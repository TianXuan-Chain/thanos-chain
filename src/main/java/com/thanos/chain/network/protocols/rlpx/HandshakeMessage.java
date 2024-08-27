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
package com.thanos.chain.network.protocols.rlpx;

import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPElement;
import com.thanos.common.utils.rlp.RLPList;

import java.nio.charset.Charset;
import java.util.Iterator;

import static com.thanos.common.utils.ByteUtil.longToBytes;

/**
 * Created by devrandom on 2015-04-12.
 */
public class HandshakeMessage {
    long version;
    String name;
    long listenPort;
    byte[] nodeId;

    public HandshakeMessage(long version, String name, long listenPort, byte[] nodeId) {
        this.version = version;
        this.name = name;
//        this.caps = caps;
        this.listenPort = listenPort;
        this.nodeId = nodeId;
    }

    HandshakeMessage() {
    }

    static HandshakeMessage parse(byte[] wire) {
        RLPList list = (RLPList) RLP.decode2(wire).get(0);
        HandshakeMessage message = new HandshakeMessage();
        Iterator<RLPElement> iter = list.iterator();
        message.version = ByteUtil.byteArrayToInt(iter.next().getRLPData()); // FIXME long
        message.name = new String(iter.next().getRLPData(), Charset.forName("UTF-8"));
        message.listenPort = ByteUtil.byteArrayToInt(iter.next().getRLPData());
        message.nodeId = iter.next().getRLPData();
        return message;
    }

    public byte[] encode() {
        return RLP.encodeList(
                RLP.encodeElement(ByteUtil.stripLeadingZeroes(longToBytes(version))),
                RLP.encodeElement(name.getBytes()),
                RLP.encodeElement(ByteUtil.stripLeadingZeroes(longToBytes(listenPort))),
                RLP.encodeElement(nodeId)
        );
    }
}
