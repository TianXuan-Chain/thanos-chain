package com.thanos.chain.consensus.hotstuffbft.liveness;

import com.thanos.chain.ledger.model.event.GlobalEventState;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.consensus.hotstuffbft.model.LedgerInfo;
import com.thanos.chain.consensus.hotstuffbft.model.LedgerInfoWithSignatures;
import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.consensus.hotstuffbft.model.ValidatorVerifier;
import com.thanos.chain.consensus.hotstuffbft.safety.Verifier;
import com.thanos.chain.ledger.model.store.Persistable;

import java.math.BigInteger;
import java.util.*;

/**
 * EpochState.java description：
 *
 * @Author laiyiyu create on 2020-06-09 10:24:46
 */
public class EpochState extends Persistable implements Verifier {

    long epoch;

    GlobalEventState globalEventState;

    public EpochState(byte[] encoded) {
        super(encoded);
    }

    public EpochState(long epoch, GlobalEventState globalEventState) {
        super(null);
        this.epoch = epoch;
        this.globalEventState = globalEventState;
        this.rlpEncoded = rlpEncoded();
    }

    public ValidatorVerifier getValidatorVerifier() {
        return globalEventState.getValidatorVerifier();
    }

    public GlobalEventState getGlobalEventState() {
        return globalEventState;
    }

    //    public EpochState() {
//        super(null);
//        this.epoch = 0;
//        this.validatorVerifier = new ValidatorVerifier(new TreeMap());
//        this.rlpEncoded = rlpEncoded();
//    }

    public ProcessResult<Void> verify(LedgerInfoWithSignatures ledgerInfo) {
        if (this.epoch != ledgerInfo.getLedgerInfo().getEpoch()) {
            ProcessResult.ofError(String.format("LedgerInfo has unexpected epoch [%d], expected [%d]", ledgerInfo.getLedgerInfo().getEpoch(), this.epoch));
        }

        return ledgerInfo.verifySignatures(globalEventState.getValidatorVerifier());
    }

    public boolean epochChangeVerificationRequired(long epoch) {
        return this.epoch < epoch;
    }

    public boolean isLedgerInfoStale(LedgerInfo ledgerInfo) {
        return ledgerInfo.getEpoch() < this.epoch;
    }

    public long getEpoch() {
        return epoch;
    }

    public List<byte[]> getOrderedPublishKeys() {
        return globalEventState.getValidatorVerifier().getOrderedPublishKeys();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EpochState that = (EpochState) o;
        return epoch == that.epoch &&
                Objects.equals(globalEventState, that.globalEventState);
    }

    @Override
    public String toString() {
        return "EpochState{" +
                "epoch=" + epoch +
                ", globalEventState=" + globalEventState +
                '}';
    }

    public void reEncode(long newEpoch) {
        this.epoch = newEpoch;
        this.globalEventState.reEncode();
        this.rlpEncoded = rlpEncoded();
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] epoch = RLP.encodeBigInteger(BigInteger.valueOf(this.epoch));
        byte[] globalEventState = this.globalEventState.getEncoded();
        return RLP.encodeList(epoch, globalEventState);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpEpochState = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.epoch = ByteUtil.byteArrayToLong(rlpEpochState.get(0).getRLPData());
        this.globalEventState = new GlobalEventState(rlpEpochState.get(1).getRLPData());
    }

    public EpochState copy() {
        return new EpochState(ByteUtil.copyFrom(this.rlpEncoded));
    }
}
