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

import static com.thanos.common.utils.ByteUtil.longToBytesNoLeadZeroes;

public class FindShardingNodesMessage extends DiscoveryMessage {

    short shardingNum;
    long expires;

    @Override
    public void parse(byte[] data) {

        RLPList list = (RLPList) RLP.decode2OneItem(data, 0);

        RLPItem shardingNum = (RLPItem) list.get(0);
        RLPItem expires = (RLPItem) list.get(1);

        this.shardingNum = ByteUtil.byteArrayToShort(shardingNum.getRLPData());
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }


    public static FindShardingNodesMessage create(short shardingNum, SecureKey privKey) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        /* RLP Encode data */
        byte[] rlpShardingNum = RLP.encodeShort(shardingNum);

        byte[] rlpExp = longToBytesNoLeadZeroes(expiration);
        rlpExp = RLP.encodeElement(rlpExp);

        byte[] type = new byte[]{3};
        byte[] data = RLP.encodeList(rlpShardingNum, rlpExp);

        FindShardingNodesMessage findNode = new FindShardingNodesMessage();
        findNode.encode(type, data, privKey);
        findNode.shardingNum = shardingNum;
        findNode.expires = expiration;

        return findNode;
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

        return String.format("[FindShardingNodesMessage] \n shardingNum: %d \n expires in %d seconds \n %s\n",
                shardingNum, (expires - currTime), super.toString());
    }

}
