package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * EventDataDsCheckResult.java description：
 *
 * @Author laiyiyu create on 2021-06-09 16:05:01
 */
public class EventDataDsCheckResult extends Persistable {

    long number;

    byte[] ethTxsCheckRes;

    public EventDataDsCheckResult(long number, byte[] ethTxsCheckRes) {
        super(null);
        this.number = number;
        this.ethTxsCheckRes = ethTxsCheckRes;
        this.rlpEncoded = rlpEncoded();
    }

    public EventDataDsCheckResult(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public long getNumber() {
        return number;
    }

    public byte[] getEthTxsCheckRes() {
        return ethTxsCheckRes;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        byte[] ethTxsCheckRes = RLP.encodeElement(this.ethTxsCheckRes);
        return RLP.encodeList(number, ethTxsCheckRes);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.number = ByteUtil.byteArrayToLong(rlpDecode.get(0).getRLPData());
        this.ethTxsCheckRes = rlpDecode.get(1).getRLPData() == null? new byte[0]: rlpDecode.get(1).getRLPData();
    }

    @Override
    public String toString() {
        return "EventDataDsCheckResult{" +
                "number=" + number +
                ", ethTxsCheckRes=" + Arrays.toString(ethTxsCheckRes) +
                '}';
    }
}
