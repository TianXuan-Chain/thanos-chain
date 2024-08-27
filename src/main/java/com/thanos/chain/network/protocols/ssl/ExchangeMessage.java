package com.thanos.chain.network.protocols.ssl;

import com.thanos.common.utils.ByteUtil;
import org.spongycastle.util.encoders.Hex;

/**
 * ExchangeMessage.java description：
 *
 * @Author laiyiyu create on 2020-09-15 10:52:05
 */
public class ExchangeMessage {

    public static final int TOTAL_ENCODE_LENGTH = 64 + 4 + 2;

    byte[] nodeId;

    int rpcListenPort;

    short shardingNum;

    byte[] encode;

    public ExchangeMessage(byte[] nodeId, int rpcListenPort, short shardingNum) {
        this.nodeId = nodeId;
        this.rpcListenPort = rpcListenPort;
        this.shardingNum = shardingNum;
        this.encode = ByteUtil.merge(nodeId, ByteUtil.intToBytes(rpcListenPort), ByteUtil.shortToBytes(shardingNum));
    }

    public ExchangeMessage(byte[] decode) {
        this.nodeId = new byte[64];
        System.arraycopy(decode, 0, nodeId, 0, 64);
        this.rpcListenPort = ByteUtil.byteArrayToInt(new byte[]{decode[64], decode[65], decode[66], decode[67]});
        this.shardingNum = ByteUtil.byteArrayToShort(new byte[]{decode[68], decode[69]});
        this.encode = ByteUtil.copyFrom(decode);
    }

    public void clear() {
        this.encode = null;
        this.nodeId = null;
    }

    @Override
    public String toString() {
        return "ExchangeMessage{" +
                "nodeId=" + Hex.toHexString(nodeId) +
                ", rpcListenPort=" + rpcListenPort +
                ", shardingNum=" + shardingNum +
                '}';
    }
//
//    public static void main(String[] args) {
//        ECKey key = new ECKey();
//        System.out.println(key.getCaHash().length);
//        ExchangeMessage msg = new ExchangeMessage(key.getCaHash(), 4, (short)2);
//        System.out.println(msg);
//        System.out.println(new ExchangeMessage(msg.encode));
//    }
}
