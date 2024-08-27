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
package com.thanos.chain.network.protocols.ssl;

import com.thanos.chain.network.protocols.base.NettyByteToMessageCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * The Netty handler responsible for decrypting/encrypting RLPx frames
 * with the FrameCodec crated during HandshakeHandler initial work
 * <p>
 * Created by Anton Nashatyrev on 15.10.2015.
 */
public class FrameCodecHandler2 extends NettyByteToMessageCodec<FrameCodec2.Frame> {

    private static final Logger logger = LoggerFactory.getLogger("network");

    private FrameCodec2 frameCodec;

    public FrameCodecHandler2(FrameCodec2 frameCodec) {
        this.frameCodec = frameCodec;
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws IOException {
        if (in.readableBytes() == 0) {
            logger.trace("in.readableBytes() == 0");
            return;
        }

        //loggerWire.trace("Decoding frame (" + in.readableBytes() + " bytes)");
        List<FrameCodec2.Frame> frames = frameCodec.readFrames(in);
        // Check if a full frame was available.  If not, we'll try later when more bytes come in.
        if (frames == null || frames.isEmpty()) return;

        out.addAll(frames);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, FrameCodec2.Frame frame, ByteBuf out) throws Exception {
        frameCodec.writeFrame(frame, out);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("[{}]FrameCodec failed:[{}]", ctx.channel().remoteAddress(), ExceptionUtils.getStackTrace(cause));
        ctx.close();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        super.close(ctx, promise);
        frameCodec.clear();
        frameCodec = null;
    }
}
