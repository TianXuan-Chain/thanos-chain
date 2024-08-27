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
import org.spongycastle.util.encoders.Hex;

import static com.thanos.common.utils.ByteUtil.*;

public class RegisterMessage extends DiscoveryMessage {

    String host;

    int port;

    short shardingNum;

    long expires;

    int version;

    public static RegisterMessage create(Node selfNode, SecureKey privKey, int discoveryPort) {
        return create(selfNode, privKey, 4, discoveryPort);
    }

    public static RegisterMessage create(Node selfNode, SecureKey privKey, int version, int discoveryPort) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        /* RLP Encode data */
        byte[] tmpExp = longToBytes(expiration);
        byte[] rlpExp = RLP.encodeElement(stripLeadingZeroes(tmpExp));

        byte[] type = new byte[]{0};
        byte[] rlpVer = RLP.encodeInt(version);
        byte[] rlpSelfNodeList = selfNode.getBriefRLP();
        byte[] data = RLP.encodeList(rlpVer, rlpSelfNodeList, rlpExp);

        RegisterMessage register = new RegisterMessage();
        register.encode(type, data, privKey);

        register.expires = expiration;

        register.host = selfNode.getHost();
        register.port = discoveryPort;
        register.shardingNum = selfNode.getShardingNum();

        return register;
    }

    @Override
    public void parse(byte[] data) {
        RLPList dataList = (RLPList) RLP.decode2OneItem(data, 0);

        this.version = ByteUtil.byteArrayToInt(dataList.get(0).getRLPData());

        RLPList selfNodeList = (RLPList) dataList.get(1);
        byte[] ipF = selfNodeList.get(0).getRLPData();
        this.host = bytesToIp(ipF);
        // selfNodeList.get(1) tcp port
        // selfNodeList.get(2) udp port
        this.port = ByteUtil.byteArrayToInt(selfNodeList.get(1).getRLPData());
        this.shardingNum = ByteUtil.byteArrayToShort(selfNodeList.get(3).getRLPData());

        RLPItem expires = (RLPItem) dataList.get(2);
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }


    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public short getShardingNum() {
        return shardingNum;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public String toString() {

        long currTime = System.currentTimeMillis() / 1000;

        String out = String.format("[RegisterMessage] [ %s:%d[ , shardingNum:[%d], expires in [%d] seconds , nodeId [%s]",
                host, port, shardingNum, (expires - currTime), Hex.toHexString(getNodeId()));

        return out;
    }
}
