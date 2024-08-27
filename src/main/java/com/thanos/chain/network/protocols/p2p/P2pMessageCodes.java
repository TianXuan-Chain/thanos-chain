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

import java.util.HashMap;
import java.util.Map;

/**
 * A list of commands for the Ethereum network protocol.
 * <br>
 * The codes for these commands are the first byte in every packet.
 * ÐΞV
 *
 * @see <a href="https://github.com/ethereum/wiki/wiki/%C3%90%CE%9EVp2p-Wire-Protocol">
 * https://github.com/ethereum/wiki/wiki/ÐΞVp2p-Wire-Protocol</a>
 */
public enum P2pMessageCodes {

    /* P2P protocol */

    /**
     * [0x00, P2P_VERSION, CLIEND_ID, CAPS, LISTEN_PORT, CLIENT_ID] <br>
     * First packet sent over the connection, and sent once by both sides.
     * No other messages may be sent until a Hello is received.
     */
    HELLO((byte)0),

    /**
     * [0x01, REASON] <br>Inform the peer that a disconnection is imminent;
     * if received, a peer should disconnect immediately. When sending,
     * well-behaved hosts give their peers a fighting chance (read: wait 2 seconds)
     * to disconnect to before disconnecting themselves.
     */
    DISCONNECT((byte)1),

    /**
     * [0x02] <br>Requests an immediate reply of Pong from the peer.
     */
    PING((byte)2),

    /**
     * [0x03] <br>Reply to peer's Ping packet.
     */
    PONG((byte)3),

    /**
     * [0x04] <br>Request the peer to enumerate some known peers
     * for us to connect to. This should include the peer itself.
     */
    GET_PEERS((byte)4),

    /**
     * [0x05, [IP1, Port1, Id1], [IP2, Port2, Id2], ... ] <br>
     * Specifies a number of known peers. IP is a 4-byte array 'ABCD'
     * that should be interpreted as the IP address A.B.C.D.
     * Port is a 2-byte array that should be interpreted as a
     * 16-bit big-endian integer. Id is the 512-bit hash that acts
     * as the unique identifier of the node.
     */
    PEERS((byte)5),


    /**
     *
     */
    USER((byte)10);


    private final byte code;

    private static final Map<Byte, P2pMessageCodes> byteToTypeMap = new HashMap<>();

    static {
        for (P2pMessageCodes type : P2pMessageCodes.values()) {
            byteToTypeMap.put(type.code, type);
        }
    }

    private P2pMessageCodes(byte code) {
        this.code = code;
    }

    public static P2pMessageCodes fromByte(byte code) {
        return byteToTypeMap.get(code);
    }


    public byte getCode() {
        return this.code;
    }

}
