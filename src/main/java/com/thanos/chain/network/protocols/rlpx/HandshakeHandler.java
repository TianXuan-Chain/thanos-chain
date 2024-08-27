/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.thanos.chain.network.protocols.rlpx;

import com.google.common.io.ByteStreams;
import com.thanos.chain.config.SystemConfig;
import com.thanos.common.crypto.ECIESCoder;
import com.thanos.common.crypto.key.asymmetric.ec.ECKeyOld;
import com.thanos.chain.network.peer.PeerChannel;
import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.network.protocols.base.MessageType;
import com.thanos.chain.network.protocols.p2p.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static com.thanos.common.utils.ByteUtil.bigEndianToShort;


/**
 * The Netty handler which manages initial negotiation with peer
 * (when either we initiating connection or remote peer initiates)
 *
 * The initial handshake includes:
 * - first AuthInitiate -> AuthResponse messages when peers exchange with secrets
 * - second P2P Hello messages when P2P protocol and subprotocol capabilities are negotiated
 *
 * After the handshake is done this handler reports secrets and other data to the Channel
 * which installs further handlers depending on the protocol parameters.
 * This handler is finally removed from the pipeline.
 */

public class HandshakeHandler extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger("network");

    private FrameCodec frameCodec;

    private final ECKeyOld myKey;

    private byte[] selfNodeId;

    private byte[] remoteId;

    private short remoteShardingNum;

    private EncryptionHandshake handshake;

    private byte[] initiatePacket;

    private PeerChannel peerChannel;

    private boolean isHandshakeDone;

    private StaticMessages staticMessages;

    private final SystemConfig config;

    public HandshakeHandler(final SystemConfig config) {
        this.config = config;
        staticMessages = new StaticMessages(config);
        myKey = (ECKeyOld) config.getMyKey();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        peerChannel.setInetSocketAddress((InetSocketAddress) ctx.channel().remoteAddress());

        selfNodeId = myKey.getNodeId();
        if (remoteId.length == 64) {
            // client handshake
            // set server's remoteId
            peerChannel.initWithRemotePeer(remoteId, remoteShardingNum);
            //logger.warn("set server remote id:" + Hex.toHexString(remoteId));
            //System.out.println("set server remote id:" + Hex.toHexString(remoteId));
            initiate(ctx);
        } else {
            // server handshake
            handshake = new EncryptionHandshake();
        }
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //loggerWire.debug("Decoding handshake... (" + in.readableBytes() + " bytes available)");
        decodeHandshake(ctx, in);

    }

    public void initiate(ChannelHandlerContext ctx) throws Exception {
        //logger.info("RLPX protocol activated");
        handshake = new EncryptionHandshake(ECKeyOld.fromNodeId(this.remoteId).getPubKeyPoint());

        AuthInitiateMessageV4 initiateMessage = handshake.createAuthInitiateV4(myKey);
        initiatePacket = handshake.encryptAuthInitiateV4(initiateMessage);


        final ByteBuf byteBufMsg = ctx.alloc().buffer(initiatePacket.length);
        byteBufMsg.writeBytes(initiatePacket);
        ctx.writeAndFlush(byteBufMsg).sync();
    }

    // consume handshake, producing no resulting message to upper layers
    private void decodeHandshake(final ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {

        if (handshake.isInitiator()) {
            // client handshake
            if (frameCodec == null) {

                byte[] responsePacket = new byte[AuthResponseMessage.getLength() + ECIESCoder.getOverhead()];
                if (!buffer.isReadable(responsePacket.length))
                    return;
                buffer.readBytes(responsePacket);

                try {
                    // trying to decode as pre-EIP-8
                    AuthResponseMessage response = handshake.handleAuthResponse(myKey, initiatePacket, responsePacket);
                    //logger.info("From: {}    Recv:  {}", ctx.channel().remoteAddress(), response);

                } catch (Throwable t) {

                    // it must be format defined by EIP-8 then
                    responsePacket = readEIP8Packet(buffer, responsePacket);
                    if (responsePacket == null) return;

                    AuthResponseMessageV4 response = handshake.handleAuthResponseV4(myKey, initiatePacket, responsePacket);
                    //logger.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), response);
                }

                EncryptionHandshake.Secrets secrets = this.handshake.getSecrets();
                this.frameCodec = new FrameCodec(secrets);

                //logger.info("auth exchange done");
                sendHelloMessage(ctx, frameCodec, selfNodeId);
            } else {
                //logger.info("MessageCodec: Buffer bytes: " + buffer.readableBytes());
                List<FrameCodec.Frame> frames = frameCodec.readFrames(buffer);
                if (frames == null || frames.isEmpty())
                    return;
                FrameCodec.Frame frame = frames.get(0);
                byte[] payload = ByteStreams.toByteArray(frame.getStream());
                if (frame.getType() == MessageType.P2P.getType() && frame.getCode() == P2pMessageCodes.HELLO.getCode()) {
                    HelloMsg helloMessage = new HelloMsg(payload);
                    isHandshakeDone = true;
                    ctx.pipeline().remove(this);
                    //logger.warn("client peer receive server node id: {}", Hex.toHexString(remoteId));
                    this.peerChannel.doSSL(ctx, false);
                    //this.peerChannel.publicRLPxHandshakeFinished(ctx, frameCodec, helloMessage);
                }
            }
        } else {
            // server handshake
            //logger.info("Not initiator.");
            if (frameCodec == null) {
                //logger.debug("FrameCodec.FrameCodec == null");
                byte[] authInitPacket = new byte[AuthInitiateMessage.getLength() + ECIESCoder.getOverhead()];
                if (!buffer.isReadable(authInitPacket.length))
                    return;
                buffer.readBytes(authInitPacket);

                this.handshake = new EncryptionHandshake();

                byte[] responsePacket;

                try {
                    // trying to decode as pre-EIP-8
                    AuthInitiateMessage initiateMessage = handshake.decryptAuthInitiate(authInitPacket, myKey);
                    //logger.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), initiateMessage);

                    AuthResponseMessage response = handshake.makeAuthInitiate(initiateMessage, myKey);
                    //logger.debug("To:   {}    Send:  {}", ctx.channel().remoteAddress(), response);
                    responsePacket = handshake.encryptAuthResponse(response);

                } catch (Throwable t) {

                    // it must be format defined by EIP-8 then
                    try {

                        authInitPacket = readEIP8Packet(buffer, authInitPacket);

                        if (authInitPacket == null) return;

                        AuthInitiateMessageV4 initiateMessage = handshake.decryptAuthInitiateV4(authInitPacket, myKey);
                        //logger.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), initiateMessage);

                        AuthResponseMessageV4 response = handshake.makeAuthInitiateV4(initiateMessage, myKey);
                        //logger.debug("To:   {}    Send:  {}", ctx.channel().remoteAddress(), response);
                        responsePacket = handshake.encryptAuthResponseV4(response);

                    } catch (InvalidCipherTextException ce) {
                        logger.warn("Can't decrypt AuthInitiateMessage from " + ctx.channel().remoteAddress() +
                                ". Most likely the remote peer used wrong public key (NodeID) to encrypt message.");
                        return;
                    }
                }

                handshake.agreeSecret(authInitPacket, responsePacket);

                EncryptionHandshake.Secrets secrets = this.handshake.getSecrets();
                this.frameCodec = new FrameCodec(secrets);

                ECPoint remotePubKey = this.handshake.getRemotePublicKey();

                byte[] compressed = remotePubKey.getEncoded();
                //set client's remoteId
                this.remoteId = new byte[compressed.length - 1];
                System.arraycopy(compressed, 1, this.remoteId, 0, this.remoteId.length);
                //System.out.println("set client remote id:" + Hex.toHexString(remoteId));

                final ByteBuf byteBufMsg = ctx.alloc().buffer(responsePacket.length);
                byteBufMsg.writeBytes(responsePacket);
                ctx.writeAndFlush(byteBufMsg).sync();
            } else {
                List<FrameCodec.Frame> frames = frameCodec.readFrames(buffer);
                if (frames == null || frames.isEmpty())
                    return;
                FrameCodec.Frame frame = frames.get(0);

                Message message = new P2pMessageFactory().create((byte) frame.getType(),
                        ByteStreams.toByteArray(frame.getStream()));

                if (frame.getType() == MessageType.P2P.getType() && frame.getCode() == P2pMessageCodes.DISCONNECT.getCode()) {
                    logger.debug("Active remote peer disconnected right after handshake.");
                    return;
                }

                if (!(frame.getType() == MessageType.P2P.getType() && frame.getCode() == P2pMessageCodes.HELLO.getCode())) {
                    throw new RuntimeException("The message type is not HELLO or DISCONNECT: " + message);
                }

                final HelloMsg inboundHelloMessage = (HelloMsg) message;
                // now we know both remote getCaHash and port
                // let's set node, that will cause registering node in NodeManager
                //logger.warn("server peer receive client node id: {}", Hex.toHexString(remoteId));

                peerChannel.initWithRemotePeer(remoteId, inboundHelloMessage.getListenPort(), inboundHelloMessage.getShardingNum());
                // Secret authentication finish here
                sendHelloMessage(ctx, frameCodec, selfNodeId);
                isHandshakeDone = true;
                ctx.pipeline().remove(this);
                this.peerChannel.doSSL(ctx, true);
                //this.peerChannel.publicRLPxHandshakeFinished(ctx, frameCodec, inboundHelloMessage);
            }
        }
    }

    private byte[] readEIP8Packet(ByteBuf buffer, byte[] plainPacket) {

        int size = bigEndianToShort(plainPacket);
        if (size < plainPacket.length)
            throw new IllegalArgumentException("AuthResponse packet contentSize is too low");

        int bytesLeft = size - plainPacket.length + 2;
        byte[] restBytes = new byte[bytesLeft];

        if (!buffer.isReadable(restBytes.length))
            return null;

        buffer.readBytes(restBytes);

        byte[] fullResponse = new byte[size + 2];
        System.arraycopy(plainPacket, 0, fullResponse, 0, plainPacket.length);
        System.arraycopy(restBytes, 0, fullResponse, plainPacket.length, restBytes.length);

        return fullResponse;
    }

    public void setRemote(String remoteId, short remoteShardingNum){
        this.remoteId = Hex.decode(remoteId);
        this.remoteShardingNum = remoteShardingNum;
    }

    public void setPeerChannel(PeerChannel peerChannel) {
        this.peerChannel = peerChannel;
    }

    public byte[] getRemoteId() {
        return remoteId;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("Handshake failed: ", cause);
        ctx.close();
    }

    private void sendHelloMessage(ChannelHandlerContext ctx, FrameCodec frameCodec, byte[] nodeId) throws IOException, InterruptedException {
        final HelloMsg helloMessage = staticMessages.createHelloMessage(nodeId);
        ByteBuf byteBufMsg = ctx.alloc().buffer();
        frameCodec.writeFrame(new FrameCodec.Frame(MessageType.P2P.getType(), P2pMessageCodes.HELLO.getCode() , nodeId, helloMessage.getEncoded()), byteBufMsg);
        ctx.writeAndFlush(byteBufMsg).sync();
    }
}
