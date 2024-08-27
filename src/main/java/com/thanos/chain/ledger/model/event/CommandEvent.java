package com.thanos.chain.ledger.model.event;

import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.chain.ledger.model.event.ca.*;

/**
 * CommandEvent.java description：
 *
 * @Author laiyiyu create on 2021-03-31 15:30:39
 */
public abstract class CommandEvent extends RLPModel {

    static CommandEvent build(byte code, byte[] data) {
        CommandEvent result;
        GlobalEventCommand globalEventCommand = GlobalEventCommand.fromByte(code);
        switch (globalEventCommand) {
            case VOTE_COMMITTEE_CANDIDATE:
                result = new VoteCommitteeCandidateEvent(data);
                break;
            case VOTE_NODE_CANDIDATE:
                result = new VoteNodeCandidateEvent(data);
                break;
            case VOTE_FILTER_CANDIDATE:
                result = new VoteFilterCandidateEvent(data);
                break;
            case VOTE_NODE_BLACKLIST_CANDIDATE:
                result = new VoteNodeBlackListCandidateEvent(data);
                break;
            case PROCESS_OPERATIONS_STAFF:
                result = new ProcessOperationsStaffCandidateEvent(data);
                break;
            case INVOKE_FILTER:
                result = new InvokeFilterEvent(data);
                break;
            default:
                throw new RuntimeException("un except CommandEvent type!");

        }
        return result;
    }

    public CommandEvent(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public abstract GlobalEventCommand getEventCommand();
}
