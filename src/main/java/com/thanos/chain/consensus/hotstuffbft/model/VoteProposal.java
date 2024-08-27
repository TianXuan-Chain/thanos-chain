package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * 类VoteProposal.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 14:09:33
 */
public class VoteProposal {

    private Event event;

    private Optional<EpochState>  nextEpochState;

    private byte[] stateRoot;

    private long number;

    private VoteProposal() {}

    public static  VoteProposal build(Event event, long number,  byte[] stateRoot, Optional<EpochState>  nextEpochState) {
        VoteProposal voteProposal = new VoteProposal();
        voteProposal.event = event;
        voteProposal.number = number;
        voteProposal.stateRoot = stateRoot;
        voteProposal.nextEpochState = nextEpochState;
        return voteProposal;
    }

    public Event getEvent() {
        return event;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public long getNumber() {
        return number;
    }

    public Optional<EpochState> getNextEpochState() {
        return nextEpochState;
    }
}
