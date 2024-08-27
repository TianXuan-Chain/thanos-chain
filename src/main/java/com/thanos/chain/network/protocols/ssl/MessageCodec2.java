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

import com.thanos.chain.network.protocols.base.Message;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;


/**
 * The Netty codec which encodes/decodes RPLx frames to subprotocol Messages
 */

@ChannelHandler.Sharable
public class MessageCodec2 extends MessageToMessageCodec<FrameCodec2.Frame, Message> {

    private static final Logger logger = LoggerFactory.getLogger("network");

    @Override
    protected void decode(ChannelHandlerContext ctx, FrameCodec2.Frame frame, List<Object> out) throws Exception {
        Message msg;
        //byte[] payload = new byte[frame.getContentSize()];
        //ByteStreams.read(frame.getStream(), payload, 0, frame.getContentSize());
        msg = new Message(frame.type, frame.code, frame.remoteType, frame.msgId, frame.nodeId, frame.payload);
        frame = null; //help gc
        out.add(msg);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        //System.out.println("MessageCodec send msg:" + msg);
        FrameCodec2.Frame frame = new FrameCodec2.Frame(msg.getType(), msg.getCode(), msg.getRemoteType(), msg.getRpcId(), msg.getNodeId(), msg.getEncoded());
        //System.out.println("message encode success!!");
        out.add(frame);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            logger.debug("MessageCodec failed: " + ctx.channel().remoteAddress() + ": " + cause);
        } else {
            logger.warn("MessageCodec failed: ", cause);
        }
        ctx.close();
    }
}