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


import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.network.protocols.base.MessageType;

public class P2pMsg extends Message {

    public P2pMsg() {
        super();
    }

    public P2pMsg(byte[] content) {
        super(content);
    }

    public P2pMsg(byte type, byte code, byte remoteType, long rpcId, byte[] nodeId, byte[] encoded) {
        super(type, code, remoteType, rpcId, nodeId, encoded);
    }

    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.fromByte(this.code);
    }


    public byte getType() {
        return MessageType.P2P.getType();
    }
}
