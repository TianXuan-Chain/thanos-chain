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


import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.ThanosWorker;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * CaNode.java description：
 *
 * @Author laiyiyu create on 2020-07-15 17:34:44
 */
class DiscoveryMessageProcessor {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");

//    static long PingTimeout = 15000; //NtreeOptions.REQ_TIMEOUT;
    static final int WARN_PACKET_SIZE = 1400;
    static final int MAX_OTHER_SHARDING_NUM = 3;
    static volatile int msgInCount = 0, msgOutCount = 0;
    static boolean initialLogging = true;
    NodeManager nodeManager;

    ArrayBlockingQueue<RegisterMessage> registerMessagesQueue;
    ArrayBlockingQueue<ShardingNodesMessage> shardingNodesMessageQueue;


    // gradually reducing log level for dumping discover messages
    // they are not so informative when everything is already up and running
    // but could be interesting when discovery just starts
    private void logMessage(DiscoveryMessage msg, boolean inbound) {
        String s = String.format("%s[%s (%s)] %s", inbound ? " ===>  " : "<===  ", msg.getClass().getSimpleName(),
                msg.getPacket().length, this);
        if (msgInCount > 1024) {
            logger.trace(s);
        } else {
            logger.debug(s);
        }

        if (!inbound && msg.getPacket().length > WARN_PACKET_SIZE) {
            logger.warn("Sending UDP packet exceeding safe size of {} bytes, actual: {} bytes",
                    WARN_PACKET_SIZE, msg.getPacket().length);
            logger.warn(s);
        }

//        if (initialLogging) {
//            if (msgOutCount == 0) {
//                logger.info("Pinging discovery nodes...");
//            }
//            if (msgInCount == 0 && inbound) {
//                logger.info("Received response.");
//            }
//            if (inbound && msg instanceof ShardingNodesMessage) {
//                logger.info("New peers discovered.");
//                initialLogging = false;
//            }
//        }

        if (inbound) msgInCount++; else msgOutCount++;
    }




    public DiscoveryMessageProcessor(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.registerMessagesQueue = new ArrayBlockingQueue(10000);
        this.shardingNodesMessageQueue = new ArrayBlockingQueue(10000);
    }

    static final int MAX_PROCESS_SHARDING_MSG_TIME = 5;

    public void start() {
        new ThanosWorker("process_discovery_msg_thread") {
            @Override
            protected void doWork() throws Exception {
                List<Node> newShardingNodes = doProcessShardingNodesMsgs();
                List<Node> newRegisterNodes = doProcessRegisterMsgs();
                DiscoveryMessageProcessor.this.nodeManager.notifyNewNode(newShardingNodes, newRegisterNodes);
            }
        }.start();
    }

    private List<Node> doProcessShardingNodesMsgs() {
        ArrayList<Node> newNodes = new ArrayList(16);
        ShardingNodesMessage shardingNodesMessage = this.shardingNodesMessageQueue.poll();
        int processTimes = 0;
        while (shardingNodesMessage != null && processTimes < MAX_PROCESS_SHARDING_MSG_TIME) {
            processTimes++;
            if (logger.isTraceEnabled()) {
                logger.trace("doCheck sharding:" + shardingNodesMessage);
            }
            for (Node newNode : shardingNodesMessage.getNodes()) {
                if (newNode.getShardingNum() > nodeManager.maxShardingNum ||
                        Arrays.equals(newNode.id, nodeManager.getSelfNode().id)) {
                    continue;
                }

                Node oldNode = this.nodeManager.putShardingNode(newNode.clone());

                if (oldNode == null || !oldNode.equals(newNode)) {
                    newNodes.add(newNode);
                }
            }

            shardingNodesMessage = this.shardingNodesMessageQueue.poll();
        }
        return newNodes;
    }

    private List<Node> doProcessRegisterMsgs() throws Exception {
        List<Node> newNodes = new ArrayList<>(MAX_PROCESS_SHARDING_MSG_TIME);
        List<RegisterMessage> registerMessages = new ArrayList<>(MAX_PROCESS_SHARDING_MSG_TIME);
        int processTimes = 0;
        RegisterMessage registerMessage = this.registerMessagesQueue.poll(100, TimeUnit.MILLISECONDS);
        while (registerMessage != null && processTimes < MAX_PROCESS_SHARDING_MSG_TIME) {
            processTimes++;
            if (logger.isTraceEnabled()) {
                logger.debug("doCheck register:" + registerMessage);
            }
            if (registerMessage.getShardingNum() > nodeManager.maxShardingNum ||
                    Arrays.equals(registerMessage.getNodeId(), nodeManager.getSelfNode().id)) {
                registerMessage = this.registerMessagesQueue.poll(100, TimeUnit.MILLISECONDS);
                continue;
            }
            Node newNode = new Node(registerMessage.getNodeId(), new String(registerMessage.getHost().getBytes()), registerMessage.getPort(), registerMessage.getShardingNum());
            Node oldNode = this.nodeManager.putShardingNode(newNode);
            if (oldNode == null || !oldNode.equals(newNode)) {
                newNodes.add(newNode);
            }
            // avoid memory leak;
            registerMessages.add(registerMessage);
            registerMessage = this.registerMessagesQueue.poll(100, TimeUnit.MILLISECONDS);

        }

        try {
            processAfterRegister(registerMessages);
        } catch (Exception e) {

        }


        return newNodes;
    }


    private void processAfterRegister(List<RegisterMessage> registerMessages) {
        for (RegisterMessage registerMessage: registerMessages) {
            sendShardingNodes(registerMessage.getShardingNum(), registerMessage.getAddress());
        }
    }

    public void process(DiscoveryMessage discoveryMessage) {
        //System.out.println("DiscoveryMessageProcessor doCheck msg:" + discoveryMessage);
        byte type = discoveryMessage.getType()[0];
        switch (type) {
            case 0:
                handleRegister((RegisterMessage) discoveryMessage);
                break;
            case 1:
                handlePing((PingMessage) discoveryMessage);
                break;
            case 2:
                handlePong((PongMessage) discoveryMessage);
                break;
            case 3:
                handleFindShardingNodes((FindShardingNodesMessage) discoveryMessage);
                break;
            case 4:
                handleShardingNodes((ShardingNodesMessage) discoveryMessage);
                break;
        }
    }

    private void handleRegister(RegisterMessage msg) {
        //logMessage(msg, true);
        if (msg.getShardingNum() > nodeManager.maxShardingNum) return;


        try {
            if (logger.isTraceEnabled()) {
                logger.trace("handle from {}, register {}", msg.getAddress(), msg);
            }

            this.registerMessagesQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        Node newNode = new Node(msg.getCaHash(), msg.getHost(), msg.getPort(), msg.getShardingNum());
//        this.nodeManager.putShardingNode(newNode);
//        sendShardingNodes(msg.getShardingNum());
    }

    private void handlePing(PingMessage msg) {
        logMessage(msg, true);
//        logMessage(" ===> [PING] " + this);
        if (Arrays.equals(nodeManager.getSelfNode().getId(), msg.getNodeId())) {

            Node node = new Node(msg.getNodeId(), msg.getAddress().getHostString(), msg.getAddress().getPort());

            DiscoveryMessage pong = PongMessage.create(msg.getMdc(), node, nodeManager.getKey());
            logMessage(pong, false);
            sendMessage(pong);
        }
    }
//
    private void handlePong(PongMessage msg) {
        logMessage(msg, true);
//        logMessage(" ===> [PONG] " + this);
//        if (waitForPong) {
//            waitForPong = false;
//            //changeState(State.Alive);
//        }
    }

    private void handleFindShardingNodes(FindShardingNodesMessage msg) {
        //logMessage(msg, true);
        sendShardingNodes(msg.getShardingNum(), msg.getAddress());
    }

    private void handleShardingNodes(ShardingNodesMessage msg) {
        //logMessage(msg, true);
//        logMessage(" ===> [NEIGHBOURS] " + this + ", Count: " + msg.getNodes().size());

        try {
            //logger.info("handle from {} handleShardingNodes:{}", msg.getAddress(), msg);
            this.shardingNodesMessageQueue.put(msg);
        } catch (InterruptedException e) {

        }
    }





//    public void sendPing(Node targetNode) {
//        if (waitForPong) {
//            logger.trace("<=/=  [PING] (Waiting for pong) " + this);
//        }
////        logMessage("<===  [PING] " + this);
//
//        DiscoveryMessage ping = PingMessage.create(nodeManager.getTable().getNode(), targetNode, nodeManager.getKey());
//        logMessage(ping, false);
//        waitForPong = true;
//        pingSent = System.currentTimeMillis();
//        sendMessage(ping);
////        getNodeStatistics().discoverOutPing.add();
//
////        if (nodeManager.getPongTimer().isShutdown()) return;
////        nodeManager.getPongTimer().schedule(() -> {
////            try {
////                if (waitForPong) {
////                    waitForPong = false;
////                    handleTimedOut();
////                }
////            } catch (Throwable t) {
////                logger.error("Unhandled exception", t);
////            }
////        }, PingTimeout, TimeUnit.MILLISECONDS);
//    }


    private void sendShardingNodes(short shardingNum, InetSocketAddress toAddress) {
        List<Node> findNodes = new ArrayList<>(100);

        for (Map.Entry<Short, Map<ByteArrayWrapper, Node>> entry: nodeManager.shardingNodesTable.entrySet()) {
            if (shardingNum == entry.getKey()) {
                entry.getValue().values().stream().forEach(node -> findNodes.add(node.clone()));
            } else {
                Iterator<Node> iter = entry.getValue().values().iterator();
                int i = 0;
                while (iter.hasNext() && i < MAX_OTHER_SHARDING_NUM) {
                    findNodes.add(iter.next().clone());
                    i++;
                }

            }
        }

        findNodes.add(nodeManager.selfNode.clone());

        if (CollectionUtils.isEmpty(findNodes)) return;
        ShardingNodesMessage shardingNodesMessage = ShardingNodesMessage.create(findNodes, nodeManager.getKey());
        shardingNodesMessage.setAddress(toAddress);
        //logger.debug("send to [{}] , sendShardingNodes :{}", toAddress, shardingNodesMessage);
        sendMessage(shardingNodesMessage);
    }



    private void sendMessage(DiscoveryMessage discoveryMessage) {
        nodeManager.sendOutbound(discoveryMessage);
    }
}
