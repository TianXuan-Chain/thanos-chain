package com.thanos.chain.consensus.hotstuffbft.store;

import com.thanos.chain.consensus.hotstuffbft.model.QuorumCert;
import com.thanos.chain.consensus.hotstuffbft.model.TimeoutCertificate;
import com.thanos.chain.consensus.hotstuffbft.model.VerifyResult;
import org.apache.commons.lang3.tuple.Pair;

import static com.thanos.chain.consensus.hotstuffbft.store.VoteReceptionResult.VoteReception.*;

/**
 * VoteReceptionResult.java description：
 *
 * @Author laiyiyu create on 2020-03-02 12:26:50
 */
public class VoteReceptionResult<T> {

    final VoteReception voteReception;

    final T result;

    private VoteReceptionResult(VoteReception voteReception, T result) {
        this.voteReception = voteReception;
        this.result = result;
    }

    public static enum  VoteReception {
        // vote success!
        VoteOk,
        /// The vote has been added but QC has not been formed yet. Return the amount of voting power
        /// the given (proposal, execution) pair.
        VoteAdded,
        /// The very same vote message has been processed in past.
        DuplicateVote,
        /// The very same author has already voted for another proposal in this round (equivocation).
        EquivocateVote,
        /// This block has just been certified after adding the vote.
        NewQuorumCertificate,
        /// The vote completes a new TimeoutCertificate
        NewTimeoutCertificate,
        /// There might be some issues adding a vote
        ErrorAddingVote,

        /// The vote is not for the current round.
        UnexpectedRound,
    }

    public VoteReception getVoteReception() {
        return voteReception;
    }

    public T getResult() {
        return result;
    }

    public static VoteReceptionResult<Void> ofVoteOk() {
        return new VoteReceptionResult(VoteOk, null);
    }

    public static VoteReceptionResult<Long> ofVoteAdded(Long result) {
        return new VoteReceptionResult(VoteAdded, result);
    }

    public static VoteReceptionResult<Void> ofDuplicateVote() {
        return new VoteReceptionResult(DuplicateVote, null);
    }

    public static VoteReceptionResult<Void> ofEquivocateVote() {
        return new VoteReceptionResult(EquivocateVote, null);
    }

    public static VoteReceptionResult<QuorumCert> ofNewQuorumCertificate(QuorumCert result) {
        return new VoteReceptionResult(NewQuorumCertificate, result);
    }

    public static VoteReceptionResult<QuorumCert> ofNewTimeoutCertificate(TimeoutCertificate result) {
        return new VoteReceptionResult(NewTimeoutCertificate, result);
    }

    public static VoteReceptionResult<Pair<Long, Long>> ofUnexpectedRound(Pair<Long, Long> result) {
        return new VoteReceptionResult(UnexpectedRound, result);
    }

    public static VoteReceptionResult<VerifyResult> ofErrorAddingVote(VerifyResult result) {
        return new VoteReceptionResult(ErrorAddingVote, result);
    }

    @Override
    public String toString() {
        return "VoteReceptionResult{" +
                "voteReception=" + voteReception +
                ", result=" + result +
                '}';
    }
}
