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
import com.thanos.common.crypto.key.asymmetric.SecurePublicKey;
import com.thanos.common.utils.ByteUtil;

import java.net.InetSocketAddress;

import static com.thanos.common.utils.ByteUtil.merge;
import static com.thanos.common.utils.ByteUtil.toHexString;
import static com.thanos.common.utils.HashUtil.sha3;

public abstract class DiscoveryMessage {

    byte[] wire;

    byte[] mdc;
    byte[] pk;
    byte[] type;
    byte[] data;

    //recover from sign, do not Serializer;
    private byte[] nodeId;

    //remark msg from
    private InetSocketAddress address;

    public static DiscoveryMessage decode(byte[] wire) {

        //if (wire.length < 98) throw new RuntimeException("Bad message");

        byte[] pkLengthBytes = new byte[2];
        System.arraycopy(wire, 0, pkLengthBytes, 0, 2);

        int pkLength = ByteUtil.byteArrayToShort(pkLengthBytes);

        byte[] pk = new byte[pkLength];
        System.arraycopy(wire, 2, pk, 0, pkLength);

        byte[] type = new byte[1];
        type[0] = wire[2 + pkLength];

        byte[] data = new byte[wire.length - (2 + pkLength + 1)];
        System.arraycopy(wire, 2 + pkLength + 1, data, 0, data.length);

        //byte[] mdcCheck = sha3(wire);

        //int check = FastByteComparisons.compareTo(mdc, 0, mdc.length, mdcCheck, 0, mdcCheck.length);

        //if (check != 0) throw new RuntimeException("MDC check failed");

        DiscoveryMessage msg;
        if (type[0] == 0) msg = new RegisterMessage();
        else if (type[0] == 1) msg = new PingMessage();
        else if (type[0] == 2) msg = new PongMessage();
        else if (type[0] == 3) msg = new FindShardingNodesMessage();
        else if (type[0] == 4) msg = new ShardingNodesMessage();
        else throw new RuntimeException("Unknown RLPx message: " + type[0]);

        //msg.mdc = mdc;
        msg.pk = pk;
        msg.type = type;
        msg.data = data;
        msg.wire = wire;

        msg.parse(data);

        return msg;
    }


    public DiscoveryMessage encode(byte[] type, byte[] data, SecureKey privKey) {




        // wrap all the data in to the packet
        this.pk = privKey.getPubKey();
        this.type = type;
        this.data = data;

        this.wire = merge(ByteUtil.shortToBytes((short) this.pk.length), privKey.getPubKey(),  this.type, this.data);

        return this;
    }

    public SecurePublicKey getKey() {
        return SecurePublicKey.generate(pk);
    }

    public byte[] getNodeId() {
        if (nodeId == null) {
            nodeId = getKey().getNodeId();
        }
        return nodeId;
    }

    public byte[] getPacket() {
        return wire;
    }

    public byte[] getMdc() {
        return mdc;
    }

    public byte[] getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public abstract void parse(byte[] data);

    @Override
    public String toString() {
        return "{" +
                ", type=" + toHexString(type) +
                ", data=" + toHexString(data) +
                '}';
    }
}
