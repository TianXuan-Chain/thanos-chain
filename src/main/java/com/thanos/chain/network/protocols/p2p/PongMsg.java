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

import org.spongycastle.util.encoders.Hex;

/**
 * Wrapper around an Ethereum Pong message on the network
 *
 * @see P2pMessageCodes#PONG
 */
public class PongMsg extends P2pMsg {

    /**
     * Pong message is always a the same single command payload
     */
    private final static byte[] FIXED_PAYLOAD = Hex.decode("C0");

    @Override
    protected byte[] rlpEncoded() {
        return FIXED_PAYLOAD;
    }


    @Override
    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.PONG;
    }

    @Override
    public byte getCode() {
        return P2pMessageCodes.PONG.getCode();
    }

    @Override
    public String toString() {
        return "[" + this.getCommand().name() + "]";
    }
}