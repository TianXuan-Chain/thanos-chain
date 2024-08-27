package com.thanos.chain.contract.ca.manager;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.*;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.utils.ByteUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

/**
 * PlaceholderStateManager.java description：
 *
 * @Author laiyiyu create on 2021-04-19 15:22:06
 */
public class PlaceholderStateManager extends StateManager {

    StateManager nextStateManager;

    public PlaceholderStateManager(EpochState epochState, GlobalStateRepositoryImpl repository) {
        super(epochState, repository);
    }

    @Override
    protected GlobalNodeEventReceipt doExecute(GlobalNodeEvent nodeEvent) {

        CandidateStateSnapshot nextStateSnapshot;
        GlobalEventCommand globalEventCommand = GlobalEventCommand.fromByte(nodeEvent.getCommandCode());
        switch (globalEventCommand) {
            case VOTE_COMMITTEE_CANDIDATE:
                nextStateSnapshot = new CommitteeCandidateStateSnapshot(new ArrayList<>(), new ArrayList<>(), Optional.empty());
                break;
            case VOTE_NODE_CANDIDATE:
                nextStateSnapshot = new NodeCandidateStateSnapshot(new ArrayList<>(), new ArrayList<>(), Optional.empty());
                break;
            case VOTE_FILTER_CANDIDATE:
                nextStateSnapshot = new FilterCandidateStateSnapshot(new ArrayList<>(), new ArrayList<>(), Optional.empty());
                break;
            case VOTE_NODE_BLACKLIST_CANDIDATE:
                nextStateSnapshot = new NodeBackListCandidateStateSnapshot(new ArrayList<>(), new ArrayList<>(), Optional.empty());
                break;
            case PROCESS_OPERATIONS_STAFF:
                nextStateSnapshot = new OperationsStaffStateSnapshot();
                break;
            case INVOKE_FILTER:
                nextStateSnapshot = new InvokeFilterStateSnapshot();
                break;
            default:
                return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(CandidateEventConstant.VOTE_FAILED), "un expect command!");
        }

        this.epochState.getGlobalEventState().resetCandidateStateSnapshot(nextStateSnapshot);
        nextStateManager = StateManager.buildManager(epochState, repository);
        return nextStateManager.execute(nodeEvent);
    }

    @Override
    protected CandidateStateSnapshot exportCurrentSnapshot() {
        if (nextStateManager.finish) {
            return new PlaceHolderStateSnapshot();
        } else {
            return nextStateManager.exportCurrentSnapshot();
        }
    }
}
