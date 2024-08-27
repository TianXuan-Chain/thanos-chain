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
package com.thanos.chain.network.protocols.p2p;

import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.encoders.Hex;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * Wrapper around an Ethereum HelloMsg on the network
 *
 * @see P2pMessageCodes#HELLO
 */
public class HelloMsg extends P2pMsg {

    /**
     * The implemented version of the P2P protocol.
     */
    private byte p2pVersion;
    /**
     * The underlying client. A user-readable string.
     */
    private String clientId;

    /**
     * The port on which the peer is listening for an incoming connection
     */
    private int listenPort;

    /**
     * state sharding num
     */
    private short shardingNum;

    public HelloMsg(byte[] encoded) {
        super(encoded);
    }

    public HelloMsg(byte p2pVersion, String clientId, short shardingNum,  int listenPort, byte[] nodeId) {
        super(null);
        this.p2pVersion = p2pVersion;
        this.clientId = clientId;
        this.listenPort = listenPort;
        this.nodeId = nodeId;
        this.shardingNum = shardingNum;
        this.rlpEncoded = rlpEncoded();
    }

    protected byte[] rlpEncoded() {
        byte[] p2pVersion = RLP.encodeByte(this.p2pVersion);
        byte[] clientId = RLP.encodeString(this.clientId);
        byte[] shardingNum = RLP.encodeShort(this.shardingNum);
        byte[] peerPort = RLP.encodeInt(this.listenPort);

        return RLP.encodeList(p2pVersion, clientId, shardingNum, peerPort);
    }

    @Override
    protected void rlpDecoded() {
        RLPList paramsList = (RLPList) RLP.decode2(rlpEncoded).get(0);

        byte[] p2pVersionBytes = paramsList.get(0).getRLPData();
        this.p2pVersion = p2pVersionBytes != null ? p2pVersionBytes[0] : 0;

        byte[] clientIdBytes = paramsList.get(1).getRLPData();
        this.clientId = new String(clientIdBytes != null ? clientIdBytes : EMPTY_BYTE_ARRAY);

//        RLPList capabilityList = (RLPList) paramsList.get(2);

        byte[] shardingNumBytes = paramsList.get(2).getRLPData();
        this.shardingNum = ByteUtil.byteArrayToShort(shardingNumBytes);

        byte[] peerPortBytes = paramsList.get(3).getRLPData();
        this.listenPort = ByteUtil.byteArrayToInt(peerPortBytes);
    }



    public byte getP2PVersion() {
        return p2pVersion;
    }

    public String getClientId() {
        return clientId;
    }

    public short getShardingNum() {
        return shardingNum;
    }

    public int getListenPort() {
        return listenPort;
    }

    @Override
    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.HELLO;
    }

    @Override
    public byte getCode() {
        return P2pMessageCodes.HELLO.getCode();
    }



    @Override
    public String toString() {
        return "HelloMsg{" +
                "p2pVersion=" + p2pVersion +
                ", clientId='" + clientId + '\'' +
                ", shardingNum=" + shardingNum +
                ", listenDiscoveryPortPort=" + listenPort +
                ", type=" + type +
                ", code=" + code +
                ", remoteType=" + remoteType +
                ", rpcId=" + rpcId +
                ", nodeId=" + Hex.toHexString(this.nodeId) +
                '}';
    }
}