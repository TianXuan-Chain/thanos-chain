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
package com.thanos.chain.network.discovery;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.thanos.common.utils.ByteUtil.toHexString;


@ChannelHandler.Sharable
public class DiscoveryMessagePacketDecoder extends MessageToMessageDecoder<DatagramPacket> {

    @Override
    public void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {
        ByteBuf buf = packet.content();
        byte[] encoded = new byte[buf.readableBytes()];
        buf.readBytes(encoded);
        try {
            DiscoveryMessage msg = DiscoveryMessage.decode(encoded);
            msg.setAddress(packet.sender());
            out.add(msg);
        } catch (Exception e) {
            //System.out.println("Exception processing inbound message from " + ctx.channel().remoteAddress() + ": " + toHexString(encoded) + "-");
            e.printStackTrace();
            throw new RuntimeException("Exception processing inbound message from " + ctx.channel().remoteAddress() + ": " + toHexString(encoded), e);
        }
    }
}
