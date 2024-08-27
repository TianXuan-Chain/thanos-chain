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
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;

import static com.thanos.common.utils.ByteUtil.*;
import static com.thanos.common.utils.HashUtil.sha3;


public class Node implements Serializable {
    private static final long serialVersionUID = -4267600517925770636L;

    byte[] id;
    String host;
    int port;
    short shardingNum;
    // discovery endpoint doesn't have real getCaHash for example
    private transient boolean isFakeNodeId = false;
    private transient InetSocketAddress inetSocketAddress;

    /**
     *  - create Node instance from enode if passed,
     *  - otherwise fallback to random getCaHash, if supplied with only "address:port"
     * NOTE: validation is absent as method is not heavily used
     */
    public static Node instanceOf(String addressOrEncode) {
        try {
            URI uri = new URI(addressOrEncode);
            if (uri.getScheme().equals("enode")) {
                return new Node(addressOrEncode);
            }
        } catch (URISyntaxException e) {
            // continue
        }

        final SecureKey generatedNodeKey = SecureKey.fromPrivate(SecureKey.withDefaultKeyPrefix(sha3(addressOrEncode.getBytes())));
        final String generatedNodeId = Hex.toHexString(generatedNodeKey.getNodeId());
        final Node node = new Node("enode://" + generatedNodeId + "@" + addressOrEncode);
        node.isFakeNodeId = true;
        return node;
    }

    public Node(String enodeURL) {
        try {
            URI uri = new URI(enodeURL);
            if (!uri.getScheme().equals("enode")) {
                throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT");
            }
            this.id = Hex.decode(uri.getUserInfo());
            this.host = uri.getHost();
            this.port = uri.getPort();
            this.inetSocketAddress = new InetSocketAddress(host, port);
        } catch (URISyntaxException e) {
            throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT", e);
        }
    }

    public Node(byte[] id, String host, int port) {
        this.id = ByteUtil.copyFrom(id);
        this.host = host;
        this.port = port;
        this.inetSocketAddress = new InetSocketAddress(host, port);
    }

    public Node(byte[] id, String host, int port, short shardingNum) {
        this.id = ByteUtil.copyFrom(id);
        this.host = host;
        this.port = port;
        this.shardingNum = shardingNum;
        this.inetSocketAddress = new InetSocketAddress(host, port);


    }



    /**
     * Instantiates node from RLP list containing node data.
     * @throws IllegalArgumentException if node id is not a valid EC point.
     */
    public Node(RLPList nodeRLP) {
        byte[] hostB = nodeRLP.get(0).getRLPData();
        byte[] portB = nodeRLP.get(1).getRLPData();
        byte[] shardingNumB = nodeRLP.get(3).getRLPData();
        byte[] idB;

        if (nodeRLP.size() > 4) {
            idB = nodeRLP.get(4).getRLPData();
        } else {
            idB = nodeRLP.get(3).getRLPData();
        }

        int port = byteArrayToInt(portB);

        this.host = bytesToIp(hostB);
        this.port = port;
        this.shardingNum = byteArrayToShort(shardingNumB);

        this.id = idB;
        this.inetSocketAddress = new InetSocketAddress(host, port);
    }

    public Node clone() {
        return new Node(ByteUtil.copyFrom(this.id), new String(this.host.getBytes()), this.port, this.shardingNum);
    }

    public Node(byte[] rlp) {
        this((RLPList) RLP.decode2(rlp).get(0));
    }

    /**
     * @return true if this node is endpoint for discovery loaded from config
     */
    public boolean isDiscoveryNode() {
        return isFakeNodeId;
    }


    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setShardingNum(short shardingNum) {
        this.shardingNum = shardingNum;
    }

    public short getShardingNum() {
        return shardingNum;
    }

    public void setDiscoveryNode(boolean isDiscoveryNode) {
        isFakeNodeId = isDiscoveryNode;
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    public void setInetSocketAddress() {
        this.inetSocketAddress = new InetSocketAddress(host, port);
    }

    /**
     * Full RLP
     * [host, udpPort, tcpPort,shardingNum, getCaHash]
     * @return RLP-encoded node data
     */
    public byte[] getRLP() {
        byte[] rlpHost = RLP.encodeElement(hostToBytes(host));
        byte[] rlpTCPPort = RLP.encodeInt(port);
        byte[] rlpUDPPort = RLP.encodeInt(port);
        byte[] rlpShardingNum = RLP.encodeShort(shardingNum);
        byte[] rlpId = RLP.encodeElement(id);

        return RLP.encodeList(rlpHost, rlpUDPPort, rlpTCPPort, rlpShardingNum, rlpId);
    }

    /**
     * RLP without getCaHash
     * [host, udpPort, tcpPort, shardingNum]
     * @return RLP-encoded node data
     */
    public byte[] getBriefRLP() {
        byte[] rlpHost = RLP.encodeElement(hostToBytes(host));
        byte[] rlpTCPPort = RLP.encodeInt(port);
        byte[] rlpUDPPort = RLP.encodeInt(port);
        byte[] rlpShardingNum = RLP.encodeShort(shardingNum);

        return RLP.encodeList(rlpHost, rlpUDPPort, rlpTCPPort, rlpShardingNum);
    }

    @Override
    public String toString() {
        return "Node{" +
                " host='" + host + '\'' +
                ", port=" + port +
                ", shardingNum=" + shardingNum +
                ", id=" + toHexString(id) +
                '}';
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(host, port, shardingNum);
        result = 31 * result + Arrays.hashCode(id);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return port == node.port &&
                shardingNum == node.shardingNum &&
                Arrays.equals(id, node.id) &&
                Objects.equals(host, node.host);
    }
}
