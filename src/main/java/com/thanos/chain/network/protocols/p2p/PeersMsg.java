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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper around an Ethereum Peers message on the network
 *
 * @see com.thanos.chain.network.protocols.p2p.P2pMessageCodes#PEERS
 */
public class PeersMsg extends P2pMsg {


    private Set<Peer> peers;

    public PeersMsg(byte[] payload) {
        super(payload);
    }

    public PeersMsg(Set<Peer> peers) {
        super(null);
        this.peers = peers;
        this.rlpEncoded = rlpEncoded();
    }


    @Override
    protected byte[] rlpEncoded() {
        byte[][] encodedByteArrays = new byte[this.peers.size() + 1][];
        encodedByteArrays[0] = RLP.encodeByte(this.getCommand().getCode());
        List<Peer> peerList = new ArrayList<>(this.peers);
        for (int i = 0; i < peerList.size(); i++) {
            encodedByteArrays[i + 1] = peerList.get(i).getEncoded();
        }
        return RLP.encodeList(encodedByteArrays);
    }

    @Override
    protected void rlpDecoded() {
        RLPList paramsList = (RLPList) RLP.decode2(rlpEncoded).get(0);

        peers = new LinkedHashSet<>();
        for (int i = 1; i < paramsList.size(); ++i) {
            RLPList peerParams = (RLPList) paramsList.get(i);
            byte[] ipBytes = peerParams.get(0).getRLPData();
            byte[] portBytes = peerParams.get(1).getRLPData();
            byte[] peerIdRaw = peerParams.get(2).getRLPData();

            try {
                int peerPort = ByteUtil.byteArrayToInt(portBytes);
                String ip = new String(ipBytes);

                String peerId = peerIdRaw == null ? "" : Hex.toHexString(peerIdRaw);
                Peer peer = new Peer(ip, peerPort, peerId);
                peers.add(peer);
            } catch (Exception e) {
                throw new RuntimeException("Malformed ip", e);
            }
        }
    }

    public Set<Peer> getPeers() {
        return peers;
    }

    @Override
    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.PEERS;
    }

    @Override
    public byte getCode() {
        return P2pMessageCodes.PEERS.getCode();
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Peer peerData : peers) {
            sb.append("\n       ").append(peerData);
        }
        return "[" + this.getCommand().name() + sb.toString() + "]";
    }
}