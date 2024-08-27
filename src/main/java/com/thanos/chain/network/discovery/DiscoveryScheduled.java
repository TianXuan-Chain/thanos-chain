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

import com.thanos.common.utils.ThanosWorker;
import com.thanos.chain.network.discovery.table.NtreeOptions;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * DiscoveryListener.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-04 14:33:57
 */
public class DiscoveryScheduled {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");

    ScheduledExecutorService discoverer = Executors.newSingleThreadScheduledExecutor();

    NodeManager nodeManager;

    public DiscoveryScheduled(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void start() {

        new ThanosWorker("discovery_scheduled") {
            @Override
            protected void doWork() throws Exception {
                doDiscovery();
                Thread.sleep(NtreeOptions.DISCOVER_SLEEP);
            }
        }.start();
    }

    private void doDiscovery() {
        RegisterMessage registerMessage = RegisterMessage.create(this.nodeManager.getSelfNode(), this.nodeManager.getKey(), this.nodeManager.getDiscoveryPort());
        for (Node node : this.nodeManager.getUnchangeDiscoveryNodes()) {
            registerMessage.setAddress(node.getInetSocketAddress());
            nodeManager.sendOutbound(registerMessage);
            if (logger.isTraceEnabled()) {
                logger.trace("do send msg:{}, to node{}" , registerMessage, node.getInetSocketAddress());
            }
        }
    }

    public void close() {
        discoverer.shutdownNow();
    }
}
