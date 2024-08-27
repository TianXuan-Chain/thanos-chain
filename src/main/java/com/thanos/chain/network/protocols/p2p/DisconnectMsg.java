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

import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.network.protocols.base.ReasonCode;

import static com.thanos.chain.network.protocols.base.ReasonCode.UNKNOWN;


/**
 * Wrapper around an Ethereum Disconnect message on the network
 *
 * @see P2pMessageCodes#DISCONNECT
 */
public class DisconnectMsg extends P2pMsg {

    private ReasonCode reason;

    public DisconnectMsg(byte[] encoded) {
        super(encoded);
    }

    public DisconnectMsg(ReasonCode reason) {
        super(null);
        this.reason = reason;
        this.rlpEncoded = rlpEncoded();
    }


    @Override
    protected byte[] rlpEncoded() {
        byte[] encodedReason = RLP.encodeByte(this.reason.asByte());
        return RLP.encodeList(encodedReason);
    }

    @Override
    protected void rlpDecoded() {
        RLPList paramsList = (RLPList) RLP.decode2(rlpEncoded).get(0);

        if (paramsList.size() > 0) {
            byte[] reasonBytes = paramsList.get(0).getRLPData();
            if (reasonBytes == null)
                this.reason = UNKNOWN;
            else
                this.reason = ReasonCode.fromInt(reasonBytes[0]);
        } else {
            this.reason = UNKNOWN;
        }
    }

    @Override
    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.DISCONNECT;
    }

    @Override
    public byte getCode() {
        return P2pMessageCodes.DISCONNECT.getCode();
    }


    public ReasonCode getReason() {
        return reason;
    }

    public String toString() {
        return "[" + this.getCommand().name() + " reason=" + reason + "]";
    }
}