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

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;


class DiscoveryMessageHandler extends SimpleChannelInboundHandler<DiscoveryMessage> {
    static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");

    public Channel channel;

    NodeManager nodeManager;

    public DiscoveryMessageHandler(NioDatagramChannel ch, NodeManager nodeManager) {
        channel = ch;
        this.nodeManager = nodeManager;
    }

    /**
     * 再udp 监听端口bind 成功下，会调用channelActive，此时，该channel 可以用于发送消息
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        nodeManager.channelActivated();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DiscoveryMessage discoveryMessage) throws Exception {
        try {
            nodeManager.handleInbound(discoveryMessage);
        } catch (Throwable t) {
            logger.info("Failed to doCheck incoming message: {}, caused by: {}", discoveryMessage, t.toString());
        }
    }

    public void sendPacket(DiscoveryMessage discoveryMessage) {
        doSendPacket(discoveryMessage.getPacket(), discoveryMessage.getAddress());
    }

    private void doSendPacket(byte[] wire, InetSocketAddress address) {
        DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(wire), address);
        channel.write(packet);
        channel.flush();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.debug("Discover channel error" + cause);
        // We don't close the channel because we can keep serving requests.
        //ctx.close();
    }
}
