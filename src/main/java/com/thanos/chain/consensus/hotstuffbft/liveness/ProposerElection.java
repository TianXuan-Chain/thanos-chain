package com.thanos.chain.consensus.hotstuffbft.liveness;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.consensus.hotstuffbft.model.Event;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 类LeaderElection.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 19:30:54
 */
public abstract class ProposerElection {

    public final static int MultipleOrderedProposers = 1;

    public final static int RotatingProposer = 2;

    public boolean isValidProposer(byte[] author, long round) {
        return Arrays.equals(author, getValidProposer(round).getLeft());
    }

    public abstract Pair<byte[] /* public key */, byte[] /* nodeId */> getValidProposer(long round);

    public boolean isValidProposer(Event event) {
        return event.getAuthor().isPresent() ?
                isValidProposer(event.getAuthor().get(), event.getRound()): false;
    }

    public abstract byte[] getNodeId(ByteArrayWrapper pk);
}
