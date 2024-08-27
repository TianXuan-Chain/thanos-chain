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
import org.spongycastle.util.encoders.Hex;

/**
 * This class models a peer in the network
 */
public class Peer {

    private volatile String ip;
    private volatile int port;
    private volatile String peerId;
    volatile boolean setIp;

    public Peer(String ip, int port, String peerId) {
        this.ip = ip;
        this.port = port;
        this.peerId = peerId;
        this.setIp = true;

    }


    public Peer(String peerId) {
        this.peerId = peerId;
        this.setIp = false;
    }

    public void setIpInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.setIp = true;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getPeerId() {
        return peerId == null ? "" : peerId;
    }


    public byte[] getEncoded() {
        byte[] ip = RLP.encodeString(this.ip);
        byte[] port = RLP.encodeInt(this.port);
        byte[] peerId = RLP.encodeElement(Hex.decode(this.peerId));
        return RLP.encodeList(ip, port, peerId);
    }

    @Override
    public String toString() {
        return "[ip=" + getIp() +
                " port=" + getPort()
                + " peerId=" + getPeerId() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Peer)) return false;
        Peer peerData = (Peer) obj;
        return peerData.peerId.equals(this.peerId)
                || this.getIp().equals(peerData.getIp());
    }

    @Override
    public int hashCode() {
        int result = peerId.hashCode();
        result = 31 * result + ip.hashCode();
        result = 31 * result + port;
        return result;
    }
}
