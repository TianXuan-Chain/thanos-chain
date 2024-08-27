package com.thanos.chain.network;

import com.thanos.chain.network.peer.CaNode;
import com.thanos.chain.network.peer.PeerManager;
import com.thanos.chain.network.protocols.base.Message;
import com.thanos.common.utils.ByteArrayWrapper;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 类PeerInvoke.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-07 10:25:56
 */
public class NetInvoker {

    public PeerManager peerManager;

    public NetInvoker(PeerManager peerManager) {
        this.peerManager = peerManager;
    }

    public byte[] getSelfNodeId() {
        return this.peerManager.getSystemConfig().getMyKey().getNodeId();
    }

    public void directSend(Message message, List<byte[]> receiveNodes) {
        this.peerManager.directSend(message, receiveNodes);
    }

    // default timeout 3 sec
    public Message rpcSend(Message message) {
        return this.peerManager.rpcSend(message);
    }

    public Message rpcSend(Message message, long timeout) {
        return this.peerManager.rpcSend(message, timeout);
    }

    public void broadcast(Message message) {
        this.peerManager.broadcast(message);
    }

    public void updateEligibleNodes(Map<ByteArrayWrapper, CaNode> caNodes, Set<BigInteger> blackListSet, Map<ByteArrayWrapper, CaNode> disconnectedNodes, Function awaitCondition) {
        peerManager.updateEligibleNodes(caNodes, blackListSet, disconnectedNodes, awaitCondition);
    }
}
