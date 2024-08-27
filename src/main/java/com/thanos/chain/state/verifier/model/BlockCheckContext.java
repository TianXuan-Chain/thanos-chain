package com.thanos.chain.state.verifier.model;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.VerifyResult;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.ledger.model.crypto.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.thanos.chain.state.verifier.model.BlockCheckContext.BlockCheckStatus.*;

/**
 * BlockCheckContext.java description：
 *
 * @Author laiyiyu create on 2020-09-17 10:23:04
 */
public class BlockCheckContext {

    static final Logger logger = LoggerFactory.getLogger("state-verify");

    public static enum BlockCheckStatus {
        INIT,
//        CORRECT_SIGN,
//        BAD_SIGN,
        ROLLBACK_BLOCK,
        COMMIT_BLOCK
    }

    //Block checkBlock;

    // pk to sign
    volatile Map<ByteArrayWrapper, Signature> sameHashSigns = new HashMap<>();

    // hash to (pk2Sign)
    volatile Map<ByteArrayWrapper, Signature> otherHashSigns = new HashMap<>();

    volatile CountDownLatch checkCondition;

    volatile EpochState epochState;

    volatile byte[] currentCheckHash;

    volatile long currentCheckNumber;

    volatile BlockCheckStatus currentStatus;

    public BlockCheckContext() {
        setPending();
    }

    public void resetAwaitCondition() {
        this.checkCondition = new CountDownLatch(1);
    }

    public void reset(StateLedger stateLedger, byte[] currentCheckHash, long currentCheckEpoch, long currentCheckNumber) {
        EpochState latestEpochState = stateLedger.consensusChainStore.getLatestLedger().getCurrentEpochState();
        if (latestEpochState.getEpoch() == currentCheckEpoch) {
            this.epochState = latestEpochState;
        } else {
            this.epochState = stateLedger.consensusChainStore.getEpochState(currentCheckEpoch);
        }
        this.currentCheckNumber = currentCheckNumber;
        this.currentCheckHash = currentCheckHash;
        setPending();
    }

    public void awaitCheck() {
        try {
            checkCondition.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void  checkSign(ByteArrayWrapper pk, byte[] hash, Signature sign) {
        if (this.currentStatus != INIT) return;

        if (Arrays.equals(hash, currentCheckHash)) {
            //this.checkBlock.addToSign(pk, sign);
            Signature signature = this.sameHashSigns.get(pk);
            if (signature == null) {
                VerifyResult verifySignRes = epochState.getValidatorVerifier().verifySignature(pk, hash, sign);
                if (!verifySignRes.isSuccess()) {
                    //return Pair.of(BAD_SIGN, null);
                    return;
                }

                sameHashSigns.put(pk, sign);
            } else {
                //repeat sign
                return;
            }

            VerifyResult verifyPowerRes = this.epochState.getValidatorVerifier().checkVotingPower(sameHashSigns.keySet());
            if (verifyPowerRes.isSuccess()) {
                this.currentStatus = COMMIT_BLOCK;
                checkCondition.countDown();
                return;
                //return Pair.of(COMMIT_BLOCK, new HashMap<>(sameHashSigns));
            }

        } else {
            VerifyResult verifySignRes = epochState.getValidatorVerifier().verifySignature(pk, hash, sign);
            if (!verifySignRes.isSuccess()) {
                return;
                //return Pair.of(BAD_SIGN, null);
            }
            otherHashSigns.put(pk, sign);

            VerifyResult verifyPowerRes = this.epochState.getValidatorVerifier().checkRemainingVotingPower(otherHashSigns.keySet());
            if (verifyPowerRes.isSuccess()) {
                logger.info("reach Remaining power, will rollback!");
                this.currentStatus = ROLLBACK_BLOCK;
                checkCondition.countDown();
                return;
                //return Pair.of(ROLLBACK_BLOCK, null);
            }
        }
    }

    public void setPending() {
        this.currentStatus = INIT;
        sameHashSigns.clear();
        otherHashSigns.clear();
    }

    public Map<ByteArrayWrapper, Signature> getSameHashSigns() {
        return sameHashSigns;
    }

    public BlockCheckStatus getCurrentStatus() {
        return currentStatus;
    }

    public EpochState getEpochState() {
        return epochState;
    }

    public long getCurrentCheckNumber() {
        return currentCheckNumber;
    }

    public byte[] getCurrentCheckHash() {
        return currentCheckHash;
    }

    public boolean isFinish() {
        return currentStatus != INIT;
    }

    @Override
    public String toString() {
        return "BlockCheckContext{" +
                "currentCheckNumber=" + currentCheckNumber +
                ", currentStatus=" + currentStatus +
                '}';
    }
}
