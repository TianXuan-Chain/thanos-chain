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


import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPItem;
import com.thanos.common.utils.rlp.RLPList;

import java.util.ArrayList;
import java.util.List;

import static com.thanos.common.utils.ByteUtil.longToBytesNoLeadZeroes;

/**
 * ShardingNodesMessage.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-04 14:34:04
 */
public class ShardingNodesMessage extends DiscoveryMessage {

    List<Node> nodes;
    long expires;

    @Override
    public void parse(byte[] data) {
        RLPList list = (RLPList) RLP.decode2OneItem(data, 0);

        RLPList nodesRLP = (RLPList) list.get(0);
        RLPItem expires = (RLPItem) list.get(1);

        nodes = new ArrayList<>();

        for (int i = 0; i < nodesRLP.size(); ++i) {
            RLPList nodeRLP = (RLPList) nodesRLP.get(i);
            Node node = new Node(nodeRLP);
            nodes.add(node);
        }
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }

    public static ShardingNodesMessage create(List<Node> nodes, SecureKey privKey) {
        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        byte[][] nodeRLPs = null;

        if (nodes != null) {
            nodeRLPs = new byte[nodes.size()][];
            int i = 0;
            for (Node node : nodes) {
                nodeRLPs[i] = node.getRLP();
                ++i;
            }
        }

        byte[] rlpListNodes = RLP.encodeList(nodeRLPs);
        byte[] rlpExp = longToBytesNoLeadZeroes(expiration);
        rlpExp = RLP.encodeElement(rlpExp);

        byte[] type = new byte[]{4};
        byte[] data = RLP.encodeList(rlpListNodes, rlpExp);

        ShardingNodesMessage shardingNodesMessage = new ShardingNodesMessage();
        shardingNodesMessage.encode(type, data, privKey);
        shardingNodesMessage.nodes = nodes;
        shardingNodesMessage.expires = expiration;

        return shardingNodesMessage;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public String toString() {

        long currTime = System.currentTimeMillis() / 1000;

        String out = String.format("[ShardingNodesMessage] nodes [%d]: %s \n expires in %d seconds",
                this.getNodes().size(), this.getNodes(), (expires - currTime));

        return out;
    }
}
