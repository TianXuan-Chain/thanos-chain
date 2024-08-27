package com.thanos.chain.consensus.hotstuffbft.liveness;

import com.thanos.chain.consensus.hotstuffbft.ChainedBFT;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.consensus.hotstuffbft.store.PendingVotes;
import com.thanos.chain.consensus.hotstuffbft.store.VoteReceptionResult;
import com.thanos.common.utils.ThanosThreadFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.thanos.chain.consensus.hotstuffbft.liveness.RoundState.NewRoundReason.*;

/**
 * 类Pacemaker.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 19:29:05
 */
public class RoundState {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    public final int roundTimeoutBaseMS;

    public enum NewRoundReason {
        QC_READY,TIMEOUT;
    }

    static ScheduledThreadPoolExecutor timeoutScheduled = new ScheduledThreadPoolExecutor(1, new ThanosThreadFactory("round_state_timeout_schedule_processor"));

    public static class NewRoundEvent {
        long round;

        long timeout;

        NewRoundReason reason;

        public NewRoundEvent(long round, long timeout, NewRoundReason reason) {
            this.round = round;
            this.timeout = timeout;
            this.reason = reason;
        }


        public long getRound() {
            return round;
        }

        public long getTimeout() {
            return timeout;
        }

        public NewRoundReason getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return "NewRoundEvent{" +
                    "round=" + round +
                    ", timeout=" + timeout +
                    ", reason=" + reason +
                    '}';
        }
    }

    public static class LocalTimeoutMsg extends ConsensusMsg {
        long round;

        public LocalTimeoutMsg(long round) {
            super(null);
            this.round = round;
        }

        @Override
        public byte getCode() {
            return ConsensusCommand.LOCAL_TIMEOUT.getCode();
        }

        public long getRound() {
            return round;
        }

        @Override
        public ConsensusCommand getCommand() {
            return ConsensusCommand.LOCAL_TIMEOUT;
        }
    }

    private double exponentBase;

    private int maxExponent;

    volatile long highestCommittedRound;

    volatile long currentRound;

    // Votes received fot the current round.
    PendingVotes pendingVotes;

    // Vote sent locally for the current round.
    volatile Optional<Vote> voteSent;

    public RoundState(int roundTimeoutBaseMS, double exponentBase, int maxExponent) {
        this.roundTimeoutBaseMS = roundTimeoutBaseMS;
        this.highestCommittedRound = 0;
        this.currentRound = 0;
        this.exponentBase = exponentBase;
        this.maxExponent = maxExponent;
        this.pendingVotes = new PendingVotes();
       // this.currentRoundDeadline = Instant.now();
        this.voteSent = Optional.empty();
    }

    public long getCurrentRound() {
        return currentRound;
    }

    public boolean processLocalTimeout(long round) {
        if (round != currentRound) {
            return false;
        }
        //logger.warn("Local timeout for round {}", round);
        setUpTimeout();
        return true;
    }

    public NewRoundEvent processCertificates(HotstuffChainSyncInfo syncInfo) {
        if (syncInfo.getHCCRound() > this.highestCommittedRound) {
            this.highestCommittedRound = syncInfo.getHCCRound();
        }

        long newRound = syncInfo.getHighestRound() + 1;
        if (newRound > this.currentRound) {
            // start new round
            this.currentRound = newRound;
            this.voteSent = Optional.empty();
            pendingVotes = new PendingVotes();
            long timeout = this.setUpTimeout();

            NewRoundReason newRoundReason = (!syncInfo.getHighestTimeoutCert().isPresent())? QC_READY : TIMEOUT;
            NewRoundEvent newRoundEvent = new NewRoundEvent(newRound, timeout, newRoundReason);
            //logger.debug("Starting new round: {}", newRoundEvent);
            return newRoundEvent;
        }
        return null;
    }

    public VoteReceptionResult insertVote(Vote vote, ValidatorVerifier verifier) {
        if (vote.getVoteData().getProposed().getRound() == this.currentRound) {
            return this.pendingVotes.insertVote(vote, verifier);
        } else {
            return VoteReceptionResult.ofUnexpectedRound(Pair.of(vote.getVoteData().getProposed().getRound(), this.currentRound));
        }
    }

    public void recordVote(Vote vote) {
        if (vote.getVoteData().getProposed().getRound() == this.currentRound) {
            this.voteSent = Optional.of(vote);
        }
    }

    public Optional<Vote> getVoteSent() {
        return this.voteSent;
    }

    private long setUpTimeout() {
        long timeout = setUpDeadline();
        //logger.debug("setUpTimeout:" + timeout);
        final long currentRound = this.currentRound;
        timeoutScheduled.schedule(() -> ChainedBFT.putConsensusMsg(new LocalTimeoutMsg(currentRound)), timeout, TimeUnit.MILLISECONDS);
        return timeout;
    }

    private long setUpDeadline() {

        long roundIndexAfterCommittedRound = 0;

        if (highestCommittedRound == 0) {
            // Genesis doesn't require the 3-chain rule for commit, hence start the index at
            // the round after genesis.
            roundIndexAfterCommittedRound = currentRound - 1;
        } else  if (currentRound < highestCommittedRound + 3) {
            roundIndexAfterCommittedRound = 0;
        } else {
            roundIndexAfterCommittedRound = currentRound - highestCommittedRound - 3;
        }

        if (roundIndexAfterCommittedRound == 0) {
            roundIndexAfterCommittedRound = 1;
        }

        long timeout = getRoundDuration(roundIndexAfterCommittedRound);

        //this.currentRoundDeadline = Instant.now().plusMillis(timeout);
        return timeout * roundTimeoutBaseMS;
    }

    private long getRoundDuration(long roundIndexAfterCommittedRound) {
        int pow = Math.min((int)roundIndexAfterCommittedRound, maxExponent);
        //double baseMultiplier = Math.pow(exponentBase, pow);
        return (long) Math.min((int)roundIndexAfterCommittedRound, maxExponent);
    }
}
