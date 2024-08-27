package com.thanos.chain.network.peer;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * 类Peer.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-04 14:33:57
 */
public class Peer {

    private volatile String host;
    private volatile int port;
    private volatile byte[] nodeId; // public key
    private volatile ByteArrayWrapper nodeIdWrapper; // public key
    private volatile short shardingNum;




    public Peer(String host, int port, byte[] nodeId, short shardingNum) {
        this.host = new String(host.getBytes());
        this.port = port;

        assert (nodeId != null) && (nodeId.length == 64);

        this.nodeId = ByteUtil.copyFrom(nodeId);
        this.nodeIdWrapper = new ByteArrayWrapper(this.nodeId);
        this.shardingNum = shardingNum;
    }

    public short getShardingNum() {
        return shardingNum;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    public ByteArrayWrapper getNodeIdWrapper() {
        return nodeIdWrapper;
    }

//    public boolean isActive() {
//        return active;
//    }
//
//    public void activePeer(){
//        this.active = true;
//    }
//
//    public void unActivePeer() {
//        this.active = false;
//    }

    /**
     *
     * @param nodeId
     * @return 1->bigger than, 0->equals, -1->smaller than
     */
    public int compareWithOther(byte[] nodeId) {
        assert (nodeId != null) && (nodeId.length == 64);

        for (int i = 0; i < 64; i++) {
            if (this.nodeId[i] > nodeId[i]) {
                return 1;
            } else if (this.nodeId[i] < nodeId[i]) {
                return -1;
            } else {
                continue;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return "[ip=" + host +
                " port=" + getPort() + " shardingNum=" + shardingNum
                + " nodeId=" + Hex.toHexString(getNodeId()) + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Peer)) return false;
        Peer peerData = (Peer) obj;
        return Arrays.equals(peerData.getNodeId(), this.getNodeId());
    }

    @Override
    public int hashCode() {
        int result = nodeId.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + port;
        result = 31 * result + shardingNum;
        return result;
    }
}
