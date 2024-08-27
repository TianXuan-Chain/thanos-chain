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

public class PongMessage extends DiscoveryMessage {

    byte[] token; // token is the MDC of the ping
    long expires;

    public static PongMessage create(byte[] token, Node toNode, SecureKey privKey) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        byte[] rlpToList = toNode.getBriefRLP();

        /* RLP Encode data */
        byte[] rlpToken = RLP.encodeElement(token);
        byte[] tmpExp = ByteUtil.longToBytes(expiration);
        byte[] rlpExp = RLP.encodeElement(ByteUtil.stripLeadingZeroes(tmpExp));

        byte[] type = new byte[]{2};
        byte[] data = RLP.encodeList(rlpToList, rlpToken, rlpExp);

        PongMessage pong = new PongMessage();
        pong.encode(type, data, privKey);

        pong.token = token;
        pong.expires = expiration;

        return pong;
    }

    public static PongMessage create(byte[] token, SecureKey privKey) {
        return create(token, privKey, 3 + System.currentTimeMillis() / 1000);
    }

    static PongMessage create(byte[] token, SecureKey privKey, long expiration) {

        /* RLP Encode data */
        byte[] rlpToken = RLP.encodeElement(token);
        byte[] rlpExp = RLP.encodeElement(ByteUtil.longToBytes(expiration));

        byte[] type = new byte[]{2};
        byte[] data = RLP.encodeList(rlpToken, rlpExp);

        PongMessage pong = new PongMessage();
        pong.encode(type, data, privKey);

        pong.token = token;
        pong.expires = expiration;

        return pong;
    }


    @Override
    public void parse(byte[] data) {
        RLPList list = (RLPList) RLP.decode2OneItem(data, 0);

        this.token = list.get(0).getRLPData();
        RLPItem expires = (RLPItem) list.get(1);
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }


    public byte[] getToken() {
        return token;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public String toString() {
        long currTime = System.currentTimeMillis() / 1000;

        String out = String.format("[PongMessage] \n token: %s \n expires in %d seconds \n %s\n",
                ByteUtil.toHexString(token), (expires - currTime), super.toString());

        return out;
    }
}
