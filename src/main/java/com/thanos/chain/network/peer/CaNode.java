package com.thanos.chain.network.peer;

import com.thanos.common.crypto.key.asymmetric.SecurePublicKey;

/**
 * CaNode.java description：
 *
 * @Author laiyiyu create on 2020-07-15 17:34:44
 */
public class CaNode {

    public final byte[] consensusPublicKey;

    public final byte[] nodeId;

    public final String name;

    public final String agency;

    public final String caHash;

    public final int shardingNum;

    public CaNode(byte[] consensusPublicKey, String name, String agency, String caHash, int shardingNum) {
        this.consensusPublicKey = consensusPublicKey;
        this.nodeId = SecurePublicKey.generate(consensusPublicKey).getNodeId();
        this.name = name;
        this.agency = agency;
        this.caHash = caHash;
        this.shardingNum = shardingNum;
    }
}
