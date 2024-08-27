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
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.network.discovery.table.NodeTable;
import com.thanos.chain.network.peer.PeerManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NodeManager.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-04 14:34:04
 */
public class NodeManager {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");

    private static final long LISTENER_REFRESH_RATE = 1000;

    private static final long DB_COMMIT_RATE = 1 * 60 * 1000;

    static final int MAX_NODES = 2000;

    DiscoveryListener listener;

    DiscoveryMessageHandler messageSender;

    NodeTable table;

    final SecureKey key;

    final Node selfNode;

    final short maxShardingNum;

    boolean inited = false;

    Timer nodeManagerTasksTimer = new Timer("NodeManagerTasks");

    List<Node> unchangeDiscoveryNodes;

    Map<Short, Map<ByteArrayWrapper, Node>> shardingNodesTable = new ConcurrentHashMap<>();

    DiscoveryMessageProcessor discoveryMessageProcessor;

    public NodeManager(SystemConfig config) {
        //this.peerConnectionManager = peerConnectionManager;
        key = config.getMyKey();
        selfNode = new Node(config.getNodeId(), config.rpcIp(), config.listenRpcPort(), config.getGenesis().getShardingNum());
        table = new NodeTable(selfNode, true);

        this.discoveryMessageProcessor = new DiscoveryMessageProcessor(this);
//        this.pongTimer = Executors.newSingleThreadScheduledExecutor();

        String[] configPeers = config.peerDiscoveryIPList().toArray(new String[0]);
        final List<Node> discoveryPeers = new ArrayList<>();
        for (String boot: configPeers) {
            // since discover IP list has no NodeIds we will generate random but persistent
            Node node = Node.instanceOf(boot);

            //if (node.host.equals(config.get))
            //node.setInetSocketAddress();
            discoveryPeers.add(node);
        }
        unchangeDiscoveryNodes = Collections.unmodifiableList(discoveryPeers);

        this.maxShardingNum = config.getGenesis().getMaxShardingNum();
        this.listener = new DiscoveryListener(config, this);
    }

//    public ScheduledExecutorService getPongTimer() {
//        return pongTimer;
//    }

    public void start() {
        new Thread(() -> {
            try {
                listener.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void notifyNewNode(List<Node> newShardingNodes, List<Node> newRegisterNodes) {
        if (!CollectionUtils.isEmpty(newShardingNodes)) {
            logger.debug("notify new sharding node:" + newShardingNodes);
            PeerManager.notifyNewNode(newShardingNodes);
        }

        if (!CollectionUtils.isEmpty(newRegisterNodes)) {
            logger.debug("notify new register node:" + newRegisterNodes);
            PeerManager.notifyNewNode(newRegisterNodes);
        }
    }

    void channelActivated() {
        // channel activated now can send messages
        if (!inited) {
            inited = true;
            this.discoveryMessageProcessor.start();

//            dbRead();
//            nodeManagerTasksTimer.scheduleAtFixedRate(new TimerTask() {
//                @Override
//                public void run() {
//                    dbWrite();
//                }
//            }, DB_COMMIT_RATE, DB_COMMIT_RATE);
        }
    }

    private void dbRead() {
//        logger.info("Reading Node statistics from DB: " + peerSource.getNodes().size() + " nodes.");
//        for (Pair<Node, Integer> nodeElement : peerSource.getNodes()) {
//            getNodeHandler(nodeElement.getLeft());
//        }
    }

    private void dbWrite() {
        List<Pair<Node, Integer>> batch = new ArrayList<>();
        synchronized (this) {
//            for (DiscoveryMessageProcessor handler : nodeHandlerMap.values()) {
//                batch.add(Pair.of(handler.getNode(), 1));
//            }
        }
//        peerSource.clear();
//        for (Pair<Node, Integer> nodeElement : batch) {
//            peerSource.getNodes().add(nodeElement);
//        }
//        peerSource.getNodes().flush();
//        logger.info("Write Node statistics to DB: " + peerSource.getNodes().size() + " nodes.");
    }

    public void setMessageSender(DiscoveryMessageHandler messageSender) {
        this.messageSender = messageSender;
    }

    private String getKey(Node n) {
        return getKey(new InetSocketAddress(n.getHost(), n.getPort()));
    }

    private String getKey(InetSocketAddress address) {
        InetAddress addr = address.getAddress();
        // addr == null if the hostname can't be resolved
        return (addr == null ? address.getHostString() : addr.getHostAddress()) + ":" + address.getPort();
    }

    public SecureKey getKey() {
        return key;
    }


//    boolean hasNodeHandler(Node n) {
//        return nodeHandlerMap.containsKey(getKey(n));
//    }

    public NodeTable getTable() {
        return table;
    }

    public Node putShardingNode(Node node) {
        Map<ByteArrayWrapper, Node> nodeTable = shardingNodesTable.get(node.getShardingNum());
        if (nodeTable == null) {
            nodeTable = new ConcurrentHashMap();
            shardingNodesTable.put(node.shardingNum, nodeTable);
        }

        return nodeTable.put(new ByteArrayWrapper(node.getId()), node);
    }

    public List<Node> getNodesByShardingNum(short shardingNum) {
        Map<ByteArrayWrapper, Node> nodeTable = this.shardingNodesTable.get(shardingNum);
        if (nodeTable == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(nodeTable.values());
    }

    public void handleInbound(DiscoveryMessage discoveryMessage) {
        discoveryMessageProcessor.process(discoveryMessage);

    }

    public void sendOutbound(DiscoveryMessage discoveryMessage) {
        if (messageSender != null) {
            messageSender.sendPacket(discoveryMessage);
        }
    }

//    public synchronized List<DiscoveryMessageProcessor> getNodes(int minReputation) {
//        List<DiscoveryMessageProcessor> ret = new ArrayList<>();
//        for (DiscoveryMessageProcessor discoveryMessageProcessor : nodeHandlerMap.values()) {
//            ret.add(discoveryMessageProcessor);
////            if (discoveryMessageProcessor.getNodeStatistics().getReputation() >= minReputation) {
////                ret.add(discoveryMessageProcessor);
////            }
//        }
//        return ret;
//    }

    public Node getSelfNode() {

        return selfNode;
    }

    public int getDiscoveryPort() {
        return this.listener.port;
    }

    /**
     *
     * @return true, channel activated now can send messages
     */
    public boolean isInited() {
        return inited;
    }

    public List<Node> getUnchangeDiscoveryNodes() {
        return unchangeDiscoveryNodes;
    }

    public void close() {
        //peerConnectionManager.close();
        try {
            nodeManagerTasksTimer.cancel();
            try {
                //dbWrite();
            } catch (Throwable e) {     // IllegalAccessError is expected
                // NOTE: logback stops context right after shutdown initiated. It is problematic to see log output
                // System out could help
                logger.warn("Problem during NodeManager persist in close: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.warn("Problems canceling nodeManagerTasksTimer", e);
        }
        try {
            logger.info("Cancelling pongTimer");
//            pongTimer.shutdownNow();
        } catch (Exception e) {
            logger.warn("Problems cancelling pongTimer", e);
        }
    }
}
