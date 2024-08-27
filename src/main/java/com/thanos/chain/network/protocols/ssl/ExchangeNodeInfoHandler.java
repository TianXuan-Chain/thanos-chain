package com.thanos.chain.network.protocols.ssl;

import com.thanos.chain.config.SystemConfig;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.chain.network.peer.PeerChannel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * ExchangeNodeInfoHandler.java description：
 *
 * @Author laiyiyu create on 2020-09-15 10:27:29
 */
public class ExchangeNodeInfoHandler extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger("network");

    PeerChannel peerChannel;

    byte[] remoteId;

    short remoteShardingNum;

    SystemConfig config;

    SecureKey myKey;

    byte[] selfNodeId;

    boolean client;

    ExchangeMessage selfExchangeMsg;

    public ExchangeNodeInfoHandler(final SystemConfig config) {
        this.config = config;
        myKey = config.getMyKey();
        this.selfExchangeMsg = new ExchangeMessage(myKey.getNodeId(), config.listenRpcPort(), config.getGenesis().getShardingNum());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("ExchangeNodeInfoHandler channelActive! {}", ctx.channel().remoteAddress());
        peerChannel.setInetSocketAddress((InetSocketAddress) ctx.channel().remoteAddress());

        selfNodeId = myKey.getNodeId();
        if (remoteId.length == 64) {
            this.client = true;
            // client handshake
            // set server's remoteId
            peerChannel.initWithRemotePeer(remoteId, remoteShardingNum);
            //logger.debug("set server remote id:" + Hex.toHexString(remoteId));
            //System.out.println("set server remote id:" + Hex.toHexString(remoteId));
            sendSelfExchangeMsgSync(ctx);
            //logger.debug("send remote id success");

        } else {
            // server handshake
            this.client = false;
            //sendSelfExchangeMsgSync(ctx);

        }
    }



    private void sendSelfExchangeMsgSync(ChannelHandlerContext ctx) throws Exception {
        final ByteBuf byteBufMsg = ctx.alloc().buffer(ExchangeMessage.TOTAL_ENCODE_LENGTH);
        byteBufMsg.writeBytes(selfExchangeMsg.encode);
        ctx.writeAndFlush(byteBufMsg).sync();
    }


    public void setRemote(String remoteId, short remoteShardingNum){
        this.remoteId = Hex.decode(remoteId);
        this.remoteShardingNum = remoteShardingNum;
    }

    public void setPeerChannel(PeerChannel peerChannel) {
        this.peerChannel = peerChannel;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        decodeExchange(ctx, in);
    }

    private void decodeExchange(ChannelHandlerContext ctx, ByteBuf buf) {
        if (client) {
            //client
            //logger.debug("client receive readableBytes():{}", buf.readableBytes());


            byte[] exchangeMsgBytes = new byte[ExchangeMessage.TOTAL_ENCODE_LENGTH];
            if (!buf.isReadable(ExchangeMessage.TOTAL_ENCODE_LENGTH))
                return;

            buf.readBytes(exchangeMsgBytes);
            ExchangeMessage serverExchangeMsg = new ExchangeMessage(exchangeMsgBytes);
            ctx.pipeline().remove(this);
            logger.warn("client peer receive server {}", serverExchangeMsg);
            this.peerChannel.doSSL(ctx, false);

        } else {

            //server

            byte[] exchangeMsgBytes = new byte[ExchangeMessage.TOTAL_ENCODE_LENGTH];
            //logger.debug("server receive readableBytes():{}", buf.readableBytes());
            if (!buf.isReadable(ExchangeMessage.TOTAL_ENCODE_LENGTH))
                return;

            buf.readBytes(exchangeMsgBytes);
            ExchangeMessage clientExchangeMsg = new ExchangeMessage(exchangeMsgBytes);
            peerChannel.initWithRemotePeer(clientExchangeMsg.nodeId, clientExchangeMsg.rpcListenPort, clientExchangeMsg.shardingNum);


            logger.warn("server peer receive client {}", clientExchangeMsg);


            // do success
            ctx.pipeline().remove(this);
            this.peerChannel.doSSL(ctx, true);

            try {
                sendSelfExchangeMsgSync(ctx);
            } catch (Exception e) {
                logger.debug("server write msg error!", e);
                ctx.close();
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            logger.debug("ExchangeNodeInfoHandler failed: " + ctx.channel().remoteAddress() + ": " + ExceptionUtils.getStackTrace(cause));
        } else {
            logger.warn("ExchangeNodeInfoHandler failed: {}", ExceptionUtils.getStackTrace(cause));
        }

        ctx.close();
    }

    public void clear() {
        this.peerChannel = null;
        this.config = null;
        this.myKey = null;
        this.remoteId = null;
        this.selfNodeId = null;
        this.selfExchangeMsg.clear();
        this.selfExchangeMsg = null;
    }
}
