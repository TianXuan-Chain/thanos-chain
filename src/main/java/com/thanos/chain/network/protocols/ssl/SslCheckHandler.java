package com.thanos.chain.network.protocols.ssl;

import com.thanos.common.crypto.ECIESCoder;
import com.thanos.chain.network.peer.PeerChannel;
import com.thanos.chain.network.protocols.rlpx.AuthResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SslCheckHandler.java description：
 *
 * @Author laiyiyu create on 2020-08-29 14:55:46
 */
public class SslCheckHandler extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger("network");

    public static final byte[] HELLO = "Hello".getBytes();

    PeerChannel peerChannel;

    boolean isServer;

    boolean isEncrypted;

    volatile boolean handleSuccess = false;


    public SslCheckHandler(PeerChannel peerChannel, boolean isServer) {
        this.peerChannel = peerChannel;
        this.isServer = isServer;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().get(SslHandler.class).handshakeFuture()
                .addListener((GenericFutureListener<Future<Channel>>) future -> {
                    if (future.isSuccess()) {
                        handleSuccess = true;
                        logger.info("handle success!");

                        if (!isServer) {
                            ctx.pipeline().remove("sslCheckHandler");

                            // notify
                            logger.debug("ssl send hello");
                            final ByteBuf byteBufMsg = ctx.alloc().buffer(HELLO.length);
                            byteBufMsg.writeBytes(HELLO);
                            ctx.writeAndFlush(byteBufMsg).sync();
                            peerChannel.publicRLPxHandshakeFinished(ctx, null, null);
                            peerChannel = null;
                        }
                    } else {
                        logger.debug("handle failed!", future.cause());
                    }
                });
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.warn("SslCheckHandler exceptionCaught", cause);
        ctx.close();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (handleSuccess && isServer) {
            byte[] responsePacket = new byte[HELLO.length];
            if (!in.isReadable(responsePacket.length))
                return;
            in.readBytes(responsePacket);
            logger.debug("receive ssl check response" + new String(responsePacket));
            ctx.pipeline().remove("sslCheckHandler");
            peerChannel.publicRLPxHandshakeFinished(ctx, null, null);
            peerChannel = null;
        }
    }
}
