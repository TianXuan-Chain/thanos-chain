package com.thanos.chain.state.sync.layer2.model;

import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

import java.math.BigInteger;

/**
 * Layer2StateSyncRequestMsg.java description：
 *
 * @Author laiyiyu create on 2020-03-09 07:59:05
 */
public class Layer2StateSyncRequestMsg extends Layer2StateSyncMsg {

    long startEventNumber;

    //long exceptSyncNum;

    public Layer2StateSyncRequestMsg(byte[] encode) {
        super(encode);
    }

    public Layer2StateSyncRequestMsg(long startEventNumber) {
        super(null);
        this.startEventNumber = startEventNumber;
        this.rlpEncoded = rlpEncoded();
    }


    public long getStartEventNumber() {
        return startEventNumber;
    }


    @Override
    protected byte[] rlpEncoded() {
        byte[] startEventNumber = RLP.encodeBigInteger(BigInteger.valueOf(this.startEventNumber));
        return RLP.encodeList(startEventNumber);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.startEventNumber = ByteUtil.byteArrayToLong(rlpDecode.get(0).getRLPData());
    }

    @Override
    public byte getCode() {
        return Layer2StateChainSyncCommand.LAYER2_STATE_SYNC_REQUEST.getCode();
    }

    @Override
    public Layer2StateChainSyncCommand getCommand() {
        return Layer2StateChainSyncCommand.LAYER2_STATE_SYNC_REQUEST;
    }
}
