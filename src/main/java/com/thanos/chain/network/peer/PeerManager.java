package com.thanos.chain.network.peer;

import com.thanos.chain.config.SystemConfig;
import com.thanos.common.utils.*;
import com.thanos.chain.network.discovery.Node;
import com.thanos.chain.network.discovery.NodeManager;
import com.thanos.chain.network.protocols.MessageDuplexDispatcher;
import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.network.protocols.p2p.P2pProcessor;
import com.thanos.chain.network.protocols.p2p.StaticMessages;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.net.ssl.SSLContext;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * 类PeerManager.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-04 14:34:04
 */
public class PeerManager {

    static final Logger logger = LoggerFactory.getLogger("network");

    static final int SCHEDULED_CONNECT_STATE_CHECK_MILLS = 10000;

    static final ArrayBlockingQueue<Node> newNodeNotifyQueue = new ArrayBlockingQueue<>(10000);

    SystemConfig systemConfig;

    short selfShardingNum;

    volatile Map<ByteArrayWrapper/*shardingNum*/, CaNode> caNodes = new HashMap<>();
    Set<BigInteger> blackListSet = new HashSet<>();
    Map<ByteArrayWrapper, CaNode> blackNodes = new HashMap();

    //all same shardingNumPeer, 需要全量的长连接, 全量更新，避免并发问题
    volatile Map<ByteArrayWrapper, Peer> sameShardingNumPeers = new ConcurrentHashMap<>();
    Map<ByteArrayWrapper, PeerChannel> sameShardingNumActivePeers = new ConcurrentHashMap<>();

    // 事务分片集合，再分片合约部署时会全量更新
    volatile Map<Short /*shardingNum*/, Set<Peer>> otherShardingNumPeers = new ConcurrentHashMap();

    Map<ByteArrayWrapper /*shardingNum*/, PeerChannel> otherShardingActivePeers = new ConcurrentHashMap<>();

    PeerServer peerServer;

    PeerClient peerClient;

    P2pProcessor p2pProcessor;

    NodeManager nodeManager;

    SSLContext selfSSLContext;//安全套接字协议实现

    BlockingQueue<Function> updateFuns;

    //for speed
    public final byte[] selfNodeId;


//    static class P2PChannelManagerHandler extends ChannelDuplexHandler {
//
//        //client 端再与 server 端建立连接后，通过 AbstractNioChannel.fulfillConnectPromise(...)立马触发，且仅触发移除
//        // server 端再监听到 client 端的连接请求后，再bootstrapServer 初始化时，所注册的连接处理ServerBootstrapAcceptor 来
//        // 触发，且仅一次
//        @Override
//        public void channelActive(ChannelHandlerContext ctx) throws Exception {
//            final String remoteAddress = NetUtil.parseChannelRemoteAddr(ctx.channel());
//            logger.info("connect establish, the channel[{}]", remoteAddress);
//
//            PeerChannel peerChannel = new PeerChannel(ctx.channel());
//
//            //System.out.println(String.format("NETTY SERVER PIPELINE: channelActive, the channel[{}]", remoteAddress));
//            super.channelActive(ctx);
//        }
//
//        //不管是server or client，在调用channel.close()时（断后tcp连接后），都会触发该事件
//        @Override
//        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//            final String remoteAddress = NetUtil.parseChannelRemoteAddr(ctx.channel());
//            logger.info("disconnect, the channel[{}]", remoteAddress);
//            super.channelInactive(ctx);
//        }
//
//        @Override
//        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//            final String remoteAddress = NetUtil.parseChannelRemoteAddr(ctx.channel());
//            logger.warn("pipeline handler: exceptionCaught {}", remoteAddress);
//            logger.warn("pipeline handler: exceptionCaught exception.", cause);
//            NetUtil.closeChannel(ctx.channel());
//        }
//    }

    public PeerManager(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
        this.selfNodeId = systemConfig.getNodeId();
        this.selfSSLContext = SSLUtil.loadSSLContext(systemConfig.getKeyPath(),systemConfig.getCertsPath());
        this.selfShardingNum = systemConfig.getGenesis().getShardingNum();
        this.peerServer = new PeerServer(systemConfig, new XChainChannelInitializer(this, "", systemConfig.getGenesis().getShardingNum()));
        this.peerClient = new PeerClient(this);
        this.p2pProcessor = new P2pProcessor(this);
        this.nodeManager = new NodeManager(systemConfig);
        this.updateFuns = new ArrayBlockingQueue(10);
        start();
    }

    public void start() {
        this.nodeManager.start();
        this.peerServer.start();
        //this.p2pProcessor.start();

        // notify sync invoke!
        new ThanosWorker("do_process_response_msg_thread") {
            @Override
            protected void doWork() throws Exception {
                Message responseMsg = MessageDuplexDispatcher.getResponseMsg();
                if (responseMsg != null) {
                    PeerChannel peerChannel = sameShardingNumActivePeers.get(new ByteArrayWrapper(responseMsg.getNodeId()));

                    // do notify
                    if (peerChannel != null) peerChannel.putResponse(responseMsg);
                }
            }
        }.start();

        new ThanosWorker("process_node_connection_thread") {
            @Override
            protected void beforeLoop() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {

                }
            }

            @Override
            protected void doWork() throws Exception {
                updateNewNode();
                checkSameShardingNumConnection();
                checkOtherShardingNumConnection();
                Thread.sleep(SCHEDULED_CONNECT_STATE_CHECK_MILLS);
            }
        }.start();


        new ThanosWorker("process_update_nodes_fun_thread") {

            @Override
            protected void doWork() throws Exception {
                Function updateFun = PeerManager.this.updateFuns.take();
                updateFun.apply(null);
                for (Map.Entry<ByteArrayWrapper, CaNode> nodeEntry: PeerManager.this.blackNodes.entrySet()) {
                    try {
                        ByteArrayWrapper nodeId = nodeEntry.getKey();
                        PeerChannel peerChannel = sameShardingNumActivePeers.remove(nodeId);
                        if (peerChannel == null) {
                            peerChannel = otherShardingActivePeers.remove(nodeId);
                            //PeerManager.this.sameShardingNumPeers.remove(nodeId);
                        }

                        if (peerChannel != null) {
                            logger.info("will close channel{}", nodeId);
                            peerChannel.closeChannel();
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }.start();
    }

    public SystemConfig getSystemConfig() {
        return systemConfig;
    }


//===========msg send start===================>


    public void broadcast(Message message) {
        message.setNodeId(ByteUtil.copyFrom(this.selfNodeId));
        for (PeerChannel peerChannel : sameShardingNumActivePeers.values()) {
            peerChannel.asyncDirectSend(message);
        }
    }

    public void globalBroadcast(Message message) {
        broadcast(message);

        for (PeerChannel peerChannel : otherShardingActivePeers.values()) {
            peerChannel.asyncDirectSend(message);
        }
    }

    // receivers was nodeId
    public void directSend(Message message, List<byte[]> receiveNodes) {
//        if (CollectionUtils.isGlobalNodeEventsEmpty(receivers)) {
//            receivers = Arrays.asList(systemConfig.getCaHash());
//        }

        message.setNodeId(ByteUtil.copyFrom(this.selfNodeId));
        for (byte[] receiver : receiveNodes) {
            PeerChannel peerChannel = getChannelByNodeId(receiver);
            if (peerChannel != null) {
                peerChannel.asyncDirectSend(message);
            } else {
                logger.warn("cannot find channel for node {}", Hex.toHexString(receiver));
            }
        }
    }

    private PeerChannel getChannelByNodeId(byte[] receiver) {
        ByteArrayWrapper key = new ByteArrayWrapper(receiver);
//        CaNode caNode = caNodes.get(key);
//        if (caNode == null) return null;


        PeerChannel peerChannel = sameShardingNumActivePeers.get(key);
        if (peerChannel != null) return peerChannel;

        return otherShardingActivePeers.get(key);

        //peerChannel = transactionShardingActivePeers.get(key);
    }

    public Message rpcSend(Message message) {
        return rpcSend(message, 3000);
    }

    public Message rpcSend(Message message, long timeout) {
        PeerChannel peerChannel = sameShardingNumActivePeers.get(new ByteArrayWrapper(message.getNodeId()));
        if (peerChannel != null) {
            try {
                return peerChannel.rpcSend(message, timeout);
            } catch (Exception e) {
                logger.warn("rpcSend message error!", e);
            }
        } else {
            logger.warn("cannot find channel for node {}", Hex.toHexString(message.getNodeId()));
        }
        return null;
    }

//    public void forward(Peer peer, Message msg) {
////        if (systemConfig.getGenesis().getShardingNum() == peer.getShardingNum()
////                || transactionShardingActivePeers.containsKey(peer.getShardingNum())) {
////            logger.warn("forward an msg in transaction sharding: {}", peer);
////        }
//        try {
//
//
//            PeerChannel peerChannel = otherShardingActivePeers.get(peer.getNodeIdWrapper());
//            if (peerChannel != null) {
//                peerChannel.syncDirectSend(msg);
//                return;
//            }
//
//
//
////            ChannelFuture channelFuture = this.peerClient.connectAsync(peer.getHost(), peer.getPort(), new XChainChannelInitializer(this, Hex.toHexString(peer.getCaHash()), peer.getShardingNum()));
////
////            channelFuture.
////            PeerChannel peerChannel = connectFuture.waitChannel(5000);
////            if (peerChannel == null) {
////                logger.warn("connect {} timeout or fail", peer);
////                return;
////            }
////
////            peerChannel.syncDirectSend(msg);
////            peerChannel.closeChannel();
//        } catch (Exception e) {
//            logger.warn("forward error !", e);
//        } finally {
//           // connectFutureMap.remove(new ByteArrayWrapper(peer.getCaHash()));
//        }
//    }

//===========msg send end===================>

//===========peer or channel manager start===================>

    public void updateEligibleNodes(Map<ByteArrayWrapper, CaNode> caNodes, Set<BigInteger> blackListSet, Map<ByteArrayWrapper, CaNode> disconnectedNodes, Function awaitCondition) {
        logger.info("updateEligibleNodes:{}", blackListSet);
        this.caNodes = caNodes;
        this.blackListSet = blackListSet;
        this.blackNodes = disconnectedNodes;

        if (awaitCondition != null) {
            try {
                this.updateFuns.put(awaitCondition);
            } catch (InterruptedException e) {
            }
        }

    }

    public void addActiveChannel(PeerChannel peerChannel) {
        PeerChannel peerChannelOld = null;

        logger.debug("addActiveChannel channel:" + peerChannel);
        if (systemConfig.getGenesis().getShardingNum() == peerChannel.getRemoteShardingNum()) {
            //System.out.println("addActiveChannel channel:" + peerChannel);
            peerChannelOld = sameShardingNumActivePeers.put(peerChannel.getRemoteNodeIdWrapper(), peerChannel);
        } else {

            peerChannelOld = otherShardingActivePeers.put(peerChannel.getRemoteNodeIdWrapper(), peerChannel);

//            ConnectFuture connectFuture = connectFutureMap.remove(peerChannel.getRemoteNodeIdWrapper());
//            if (connectFuture != null) {
//                connectFuture.putConnect(peerChannel);
//            }
//
//
//            synchronized (otherShardingActivePeers) {
//                Map<ByteArrayWrapper, PeerChannel> channelMap = otherShardingActivePeers.get(caNode.shardingNum);
//                if (channelMap == null) {
//                    channelMap = new ConcurrentHashMap();
//                    otherShardingActivePeers.put((short) caNode.shardingNum, channelMap);
//                }
//            }
//            peerChannelOld = channelMap.put(peerChannel.getRemoteNodeIdWrapper(), peerChannel);
        }

        if (peerChannelOld != null) {
            logger.info("close old channel! {}", peerChannelOld);
            peerChannelOld.setRepeatConnect();
            peerChannelOld.closeChannel();
        }
    }

    public void notifyDisconnect(PeerChannel peerChannel) {
        if (peerChannel.remotePeer == null) return;
        logger.debug("notifies about disconnect Peer {}: ", peerChannel);

        if (!peerChannel.isRepeatConnect()) {

            if (systemConfig.getGenesis().getShardingNum() == peerChannel.getRemoteShardingNum()) {
                sameShardingNumActivePeers.remove(peerChannel.getRemoteNodeIdWrapper());
            } else {
                // ignore the other sharding node
                otherShardingActivePeers.remove(peerChannel.getRemoteNodeIdWrapper());
            }
        }

        peerChannel.close();
    }


//===========peer or channel manager end===================>


    private void updateNewNode() {
        Node newNode = null;

        try {
            //newNode = newNodeNotifyQueue.peek();
            while (newNodeNotifyQueue.peek() != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("peer manager receive new node:" + newNode);
                }


                newNode = newNodeNotifyQueue.poll();

                ByteArrayWrapper nodeIdWrapper = new ByteArrayWrapper(ByteUtil.copyFrom(newNode.getId()));
                CaNode caNode = caNodes.get(nodeIdWrapper);

                if (caNode != null) {
                    // register node
                    if (selfShardingNum == caNode.shardingNum) {




                        sameShardingNumPeers.put(nodeIdWrapper, new Peer(newNode.getHost(), newNode.getPort(), nodeIdWrapper.getData(), selfShardingNum));

                    } else {

                        Set<Peer> peers = otherShardingNumPeers.get(caNode.shardingNum);
                        if (peers == null) {
                            peers = new CopyOnWriteArraySet<>();
                            otherShardingNumPeers.put((short) caNode.shardingNum, peers);
                        }

                        peers.add(new Peer(newNode.getHost(), newNode.getPort(), nodeIdWrapper.getData(), (short) caNode.shardingNum));

                    }

                } else {

                    // no register node
                    if (selfShardingNum == newNode.getShardingNum()) {
                        sameShardingNumPeers.put(nodeIdWrapper, new Peer(newNode.getHost(), newNode.getPort(), nodeIdWrapper.getData(), selfShardingNum));

                    } else {
                        Set<Peer> peers = otherShardingNumPeers.get(newNode.getShardingNum());
                        if (peers == null) {
                            peers = new CopyOnWriteArraySet<>();
                            otherShardingNumPeers.put(newNode.getShardingNum(), peers);
                        }

                        peers.add(new Peer(newNode.getHost(), newNode.getPort(), nodeIdWrapper.getData(), newNode.getShardingNum()));
                    }


                }
                //newNode = newNodeNotifyQueue.poll();

            }

            if (logger.isTraceEnabled()) {
                logger.trace("current sameShardingNumPeers:" + sameShardingNumPeers.values());
            }
//

        } catch (Exception e) {

        } finally {
            newNode = null;
        }

    }

    private void checkSameShardingNumConnection() {
        List<ChannelFuture> awaits = new ArrayList<>();
        try {
            // 这里避免sameShardingNumPeers 引用被改变而引发的并发问题
            Map<ByteArrayWrapper, Peer> temp = sameShardingNumPeers;

//            if (logger.isTraceEnabled()) {
//                for (PeerChannel peerChannel : sameShardingNumActivePeers.values()) {
//                    logger.trace("checkSameShardingNumConnection, active:{}", peerChannel);
//                }
//            }

            if (temp.size() == sameShardingNumActivePeers.size()) {
                return;
            }

            for (Peer otherPeer : temp.values()) {
                if (otherPeer.compareWithOther(systemConfig.getNodeId()) == -1
                        && sameShardingNumActivePeers.get(otherPeer.getNodeIdWrapper()) == null) {

                    try {
                        logger.debug("will connect: {}", otherPeer);
                        ChannelFuture channelFuture = this.peerClient.connectAsync(otherPeer.getHost(), otherPeer.getPort(), new XChainChannelInitializer(this, Hex.toHexString(otherPeer.getNodeId()), otherPeer.getShardingNum()));
                        awaits.add(channelFuture);
                    } catch (Exception e) {
                        logger.debug("connect error!", e);
                    }

                }
            }

            for (ChannelFuture channelFuture : awaits) {
                channelFuture.await(5000);
            }

            awaits.clear();
        } catch (Exception e) {
            logger.debug("doProcessResponseMsg error!", e);
        }
    }

    private void checkOtherShardingNumConnection() {
        List<ChannelFuture> awaits = new ArrayList<>();
        try {
            int totalOtherSize = 0;

            for (Set<Peer> peers : otherShardingNumPeers.values()) {
                totalOtherSize += peers.size();
            }

            if (totalOtherSize == otherShardingActivePeers.size()) {
                return;
            }


            for (Set<Peer> peers : otherShardingNumPeers.values()) {
                for (Peer otherPeer : peers) {
                    if (otherPeer.compareWithOther(systemConfig.getNodeId()) == -1
                            && otherShardingActivePeers.get(otherPeer.getNodeIdWrapper()) == null) {
                        ChannelFuture channelFuture = this.peerClient.connectAsync(otherPeer.getHost(), otherPeer.getPort(), new XChainChannelInitializer(this, Hex.toHexString(otherPeer.getNodeId()), otherPeer.getShardingNum()));
                        awaits.add(channelFuture);
                    }
                }
            }

            for (ChannelFuture channelFuture : awaits) {
                channelFuture.await(3000);
            }

            awaits.clear();
        } catch (Exception e) {
            logger.debug("doProcessResponseMsg error!", e);
        }

    }


    public static void notifyNewNode(List<Node> newNodes) {
        newNodeNotifyQueue.addAll(newNodes);
    }


    public static void main(String[] args) {
        SystemConfig systemConfig = new SystemConfig();
        PeerManager peerManager = new PeerManager(systemConfig);

        Peer peer = new Peer("127.0.0.1", 8888, systemConfig.getMyKey().getNodeId(), (short) 1);


        StaticMessages staticMessages = new StaticMessages(systemConfig);
        // peerManager.forward(peer, staticMessages.createHelloMessage(systemConfig.getMyKey().getCaHash()));

        await(200000000);
    }

    public static void await(long time) {
        synchronized (PeerManager.class) {// 类实例锁
            try {
                PeerManager.class.wait(time);
                System.out.println("wait after");
            } catch (InterruptedException e) {

            }
        }
    }
}
