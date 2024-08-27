package com.thanos.chain.contract.ca.manager;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.model.event.CommandEvent;
import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.*;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * StateManager.java description：
 *
 * @Author laiyiyu create on 2021-04-07 17:29:18
 */
public abstract class StateManager {

    protected boolean finish;

    //当前委员会(公钥)集合
    protected Set<ByteArrayWrapper> currentCommittees;

    protected Set<ByteArrayWrapper> currentOpStaffs;

    protected EpochState epochState;

    protected GlobalStateRepositoryImpl repository;


    public static StateManager buildManager(EpochState epochState, GlobalStateRepositoryImpl repository) {
        StateManager result;
        GlobalEventCommand globalEventCommand = GlobalEventCommand.fromByte(epochState.getGlobalEventState().getCode());
        switch (globalEventCommand) {
            case PLACEHOLDER_EMPTY:
                result = new PlaceholderStateManager(epochState, repository);
                break;
            case VOTE_COMMITTEE_CANDIDATE:
                result = new CommitteeStateManager(epochState, repository);
                break;
            case VOTE_NODE_CANDIDATE:
                result = new GlobalNodeStateManager(epochState, repository);
                break;
            case VOTE_FILTER_CANDIDATE:
                result = new FilterStateManager(epochState, repository);
                break;
            case VOTE_NODE_BLACKLIST_CANDIDATE:
                result = new GlobalNodeBlackListStateManager(epochState, repository);
                break;
            case PROCESS_OPERATIONS_STAFF:
                result = new OpStaffStateManager(epochState, repository);
                break;
            case INVOKE_FILTER:
                result = new InvokeFilterStateManager(epochState, repository);
                break;
            default:
                throw new RuntimeException("un except CommandEvent type!");

        }
        return result;
    }

    protected StateManager(EpochState epochState, GlobalStateRepositoryImpl repository) {
        this.currentCommittees = new HashSet<>(epochState.getGlobalEventState().getCommitteeAddrs());
        this.currentOpStaffs = new HashSet<>(epochState.getGlobalEventState().getOperationsStaffAddrs());
        this.epochState = epochState;
        this.repository = repository;
    }

    public GlobalNodeEventReceipt execute(GlobalNodeEvent nodeEvent) {

        CandidateStateSnapshot candidateStateSnapshot = epochState.getGlobalEventState().getCandidateStateSnapshot();
        CommandEvent commandEvent = nodeEvent.getCommandEvent();

        ByteArrayWrapper sender = new ByteArrayWrapper(nodeEvent.getSendAddress());

        if (VoteEvent.class.isAssignableFrom(commandEvent.getClass()) || commandEvent.getEventCommand().equals(GlobalEventCommand.PROCESS_OPERATIONS_STAFF)) {
            if (!currentCommittees.contains(sender)) {
                StringBuilder error = new StringBuilder();
                error.append("sender[").append(Hex.toHexString(nodeEvent.getSendAddress())).append("] is not the committee, reject the vote!");

                return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(CandidateEventConstant.VOTE_FAILED), error.toString());
            }
        } else {
            if (!currentCommittees.contains(sender) && !currentOpStaffs.contains(sender)) {
                StringBuilder error = new StringBuilder();
                error.append("sender[").append(Hex.toHexString(nodeEvent.getSendAddress())).append("] is not the committee or operation staff, reject the vote!");

                return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(CandidateEventConstant.VOTE_FAILED), error.toString());
            }
        }

        if (!candidateStateSnapshot.getCurrentCommand().equals(GlobalEventCommand.PLACEHOLDER_EMPTY) && !commandEvent.getEventCommand().equals(candidateStateSnapshot.getCurrentCommand())) {
            StringBuilder error = new StringBuilder();
            error.append("current doCheck state command is:[").append(candidateStateSnapshot.getCurrentCommand()).
                    append("], reject the command:[").append(commandEvent.getEventCommand()).append("]");

            return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(CandidateEventConstant.VOTE_FAILED), error.toString());
        }

        GlobalNodeEventReceipt receipt = doExecute(nodeEvent);
        if (finish) {
            this.epochState.getGlobalEventState().resetCandidateStateSnapshot(new PlaceHolderStateSnapshot());
            doClean();
        } else {
            this.epochState.getGlobalEventState().resetCandidateStateSnapshot(exportCurrentSnapshot());
        }

        return receipt;
    }

    protected void setFinish() {
        this.finish = true;
    }

    protected abstract GlobalNodeEventReceipt doExecute(GlobalNodeEvent nodeEvent);

    protected abstract CandidateStateSnapshot exportCurrentSnapshot();

    protected void doClean() {}
}
