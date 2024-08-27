package com.thanos.chain.consensus.hotstuffbft.model;


import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import org.junit.Assert;

import java.util.Optional;

/**
 * LatestLedger.java description：
 *
 * @Author laiyiyu create on 2020-03-06 13:35:52
 */
public class LatestLedger {

    volatile LedgerInfoWithSignatures latestLedgerInfo;

    volatile ExecutedEventOutput commitExecutedEventOutput;

    volatile EpochState currentEpoch;

    public LatestLedger() {
    }

    public LatestLedger(LedgerInfoWithSignatures latestLedgerInfo,
                        ExecutedEventOutput commitExecutedEventOutput, EpochState currentEpoch) {
        this.latestLedgerInfo = latestLedgerInfo;
        this.commitExecutedEventOutput = commitExecutedEventOutput;
        this.currentEpoch = currentEpoch;
    }

    public void reset(LedgerInfoWithSignatures latestLedgerInfo,
                        ExecutedEventOutput commitExecutedEventOutput) {
        this.latestLedgerInfo = latestLedgerInfo;
        this.commitExecutedEventOutput = commitExecutedEventOutput;
        if (commitExecutedEventOutput.hasReconfiguration()) {
            this.currentEpoch = commitExecutedEventOutput.getEpochState().get();
        }
    }

    public LedgerInfoWithSignatures getLatestLedgerInfo() {
        return latestLedgerInfo;
    }

    public ExecutedEventOutput getCommitExecutedEventOutput() { return commitExecutedEventOutput; }

    public long getLatestNumber() { return latestLedgerInfo.getLedgerInfo().getNumber(); }

    public EpochState getCurrentEpochState() {
        return this.currentEpoch;
    }

    @Override
    public String toString() {
        return "LatestLedger{" +
                "latestLedgerInfo=" + latestLedgerInfo +
                ", commitExecutedEventOutput=" + commitExecutedEventOutput +
                ", currentEpoch=" + currentEpoch +
                '}';
    }
}
