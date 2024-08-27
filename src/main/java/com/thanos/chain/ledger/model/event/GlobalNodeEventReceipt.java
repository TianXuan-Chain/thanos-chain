package com.thanos.chain.ledger.model.event;

import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * GlobalNodeEventReceipt.java description：
 *
 * @Author laiyiyu create on 2021-04-06 14:07:07
 */
public class GlobalNodeEventReceipt extends Persistable {

    static byte[] NULL_BYTES = {-128};

    private GlobalNodeEvent globalNodeEvent;

    byte[] hash;

    byte[] executionResult;

    String msg;

    public GlobalNodeEventReceipt(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public GlobalNodeEventReceipt(GlobalNodeEvent globalNodeEvent, byte[] executionResult, String msg) {
        super(null);
        this.globalNodeEvent = globalNodeEvent;
        this.hash = globalNodeEvent.getHash();
        this.executionResult = (executionResult == null || Arrays.equals(executionResult, NULL_BYTES))? ByteUtil.EMPTY_BYTE_ARRAY : executionResult;
        this.msg = msg;
        this.rlpEncoded = rlpEncoded();
    }

    public GlobalNodeEvent getGlobalNodeEvent() {
        return globalNodeEvent;
    }

    public byte[] getHash() {
        return hash;
    }

    public byte[] getExecutionResult() {
        return executionResult;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    protected byte[] rlpEncoded() {



        return RLP.encodeList(
                globalNodeEvent.getEncoded(),
                RLP.encodeElement(executionResult),
                RLP.encodeElement(msg.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(this.rlpEncoded).get(0);

        this.globalNodeEvent = new GlobalNodeEvent(rlpDecode.get(0).getRLPData());
        this.hash = this.globalNodeEvent.getHash();
        this.executionResult = rlpDecode.get(1).getRLPData() == null ? EMPTY_BYTE_ARRAY : rlpDecode.get(1).getRLPData();
        this.msg = rlpDecode.get(2).getRLPData() == null? "": new String(rlpDecode.get(2).getRLPData());
        //this.executionResult
    }


    @Override
    public String toString() {
        return "GlobalNodeEventReceipt{" +
                "globalNodeEvent=" + globalNodeEvent +
                ", executionResult=" + Hex.toHexString(executionResult) +
                ", msg='" + msg + '\'' +
                '}';
    }
}

