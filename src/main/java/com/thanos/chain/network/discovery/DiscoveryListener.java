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

import com.thanos.chain.config.SystemConfig;
import com.thanos.common.crypto.key.asymmetric.ec.ECKey;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.net.BindException;
import java.util.concurrent.TimeUnit;

import static com.thanos.common.utils.HashUtil.sha3;


/**
 * DiscoveryListener.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-04 14:33:57
 */
public class DiscoveryListener {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");

    int port;

    String address;

    NodeManager nodeManager;

    SystemConfig config;

    Channel channel;

    volatile boolean shutdown = false;

    DiscoveryScheduled discoveryScheduled;

    public DiscoveryListener(final SystemConfig config, final NodeManager nodeManager) {
        this.config = config;
        this.nodeManager = nodeManager;

        this.address = config.bindIp();
        port = config.listenDiscoveryPortPort();
    }

    //for test
    public DiscoveryListener(String address, int port, String file) {
        this.address = address;
        this.port = port;
        this.config = new SystemConfig(file);
        final ECKey generatedNodeKey = ECKey.fromPrivate(sha3((address + ":" + port).getBytes()), (short) 1);
        config.setMyKey(generatedNodeKey);
        System.out.println("udp id:" + Hex.toHexString(generatedNodeKey.getNodeId()));
        this.nodeManager = new NodeManager(this.config);
    }

    public static Node parseNode(String s) {
        int idx1 = s.indexOf('@');
        int idx2 = s.indexOf(':');
        String id = s.substring(0, idx1);
        String host = s.substring(idx1 + 1, idx2);
        int port = Integer.parseInt(s.substring(idx2 + 1));
        return new Node(Hex.decode(id), host, port);
    }

    public void start() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(1);

        try {
            discoveryScheduled = new DiscoveryScheduled(nodeManager);
            // for test!!!

            while (!shutdown) {
                Bootstrap b = new Bootstrap();
                b.group(group)
                        .channel(NioDatagramChannel.class)
                        .handler(new ChannelInitializer<NioDatagramChannel>() {
                            @Override
                            public void initChannel(NioDatagramChannel ch)
                                    throws Exception {
                                ch.pipeline().addLast(new DiscoveryMessagePacketDecoder());
                                DiscoveryMessageHandler discoveryMessageHandler = new DiscoveryMessageHandler(ch, nodeManager);
                                nodeManager.setMessageSender(discoveryMessageHandler);
                                ch.pipeline().addLast(discoveryMessageHandler);
                            }
                        });

                channel = b.bind(address, port).sync().channel();
                discoveryScheduled.start();
                logger.info("DiscoveryListener bind to [{}:{}] success!", address, port);
                channel.closeFuture().sync();
                if (shutdown) {
                    logger.info("Shutdown discovery DiscoveryListener");
                    break;
                }
                logger.warn("UDP channel closed. Recreating after 5 sec pause...");
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            if (e instanceof BindException && e.getMessage().contains("Address already in use")) {
                logger.error("Port " + port + " is busy. Check if another instance is running with the same port.");
            } else {
                logger.error("Can't start discover: ", e);
            }
            System.exit(0);
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public void close() {
        logger.info("Closing DiscoveryListener...");
        shutdown = true;
        if (channel != null) {
            try {
                channel.close().await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Problems closing DiscoveryListener", e);
            }
        }

        if (discoveryScheduled != null) {
            try {
                discoveryScheduled.close();
            } catch (Exception e) {
                logger.warn("Problems closing DiscoveryScheduled", e);
            }
        }
    }

}
