package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.chain.ledger.model.event.GlobalEventCommand;

/**
 * CandidateStateSnapshot.java description：
 *
 * @Author laiyiyu create on 2021-04-07 15:03:53
 */
public abstract class CandidateStateSnapshot extends RLPModel {

    public CandidateStateSnapshot(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public static CandidateStateSnapshot build(byte code, byte[] data) {
        CandidateStateSnapshot result;
        GlobalEventCommand globalEventCommand = GlobalEventCommand.fromByte(code);
        switch (globalEventCommand) {
            case PLACEHOLDER_EMPTY:
                result = new PlaceHolderStateSnapshot();
                break;
            case VOTE_COMMITTEE_CANDIDATE:
                result = new CommitteeCandidateStateSnapshot(data);
                break;
            case VOTE_NODE_CANDIDATE:
                result = new NodeCandidateStateSnapshot(data);
                break;
            case VOTE_FILTER_CANDIDATE:
                result = new FilterCandidateStateSnapshot(data);
                break;
            case VOTE_NODE_BLACKLIST_CANDIDATE:
                result = new NodeBackListCandidateStateSnapshot(data);
                break;
            default:
                throw new RuntimeException("un except CommandEvent type!");

        }
        return result;
    }

    public abstract GlobalEventCommand getCurrentCommand();
}
