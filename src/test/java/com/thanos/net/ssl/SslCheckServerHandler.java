package com.thanos.net.ssl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.nio.ByteBuffer;

/**
 * SslCheckServerHandler.java description：
 *
 * @Author laiyiyu create on 2020-07-16 14:10:41
 */
public class SslCheckServerHandler extends
        SimpleChannelInboundHandler<ByteBuffer> {


    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        // Once session is secured, send a greeting and register the channel to
        // the global channel
        // list so the channel received the messages from others.
        ctx.pipeline().get(SslHandler.class).handshakeFuture()
                .addListener((GenericFutureListener<Future<Channel>>) future -> {
                    if (future.isSuccess()) {
                        System.out.println("握手成功");
                    } else {
                        System.out.println("握手失败");
                    }
                });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuffer msg)
            throws Exception {
    }

}

