package com.thanos.chain.contract.ca.manager;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.CandidateStateSnapshot;
import com.thanos.chain.ledger.model.event.ca.PlaceHolderStateSnapshot;
import com.thanos.chain.ledger.model.event.ca.ProcessOperationsStaffCandidateEvent;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;

/**
 * OpStaffStateManager.java description：
 *
 * @Author laiyiyu create on 2021-04-29 11:16:57
 */
public class OpStaffStateManager extends StateManager {

    protected OpStaffStateManager(EpochState epochState, GlobalStateRepositoryImpl repository) {
        super(epochState, repository);
    }

    @Override
    protected GlobalNodeEventReceipt doExecute(GlobalNodeEvent nodeEvent) {
        ProcessOperationsStaffCandidateEvent candidateEvent = (ProcessOperationsStaffCandidateEvent) nodeEvent.getCommandEvent();

        if (candidateEvent.getProcessType() == CandidateEventConstant.REGISTER_PROCESS) {
            epochState.getGlobalEventState().addOperationsStaff(new ByteArrayWrapper(ByteUtil.copyFrom(candidateEvent.getAddress())));
        } else {
            epochState.getGlobalEventState().removeOperationsStaff(new ByteArrayWrapper(ByteUtil.copyFrom(candidateEvent.getAddress())));
        }
        this.finish = true;
        return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(CandidateEventConstant.AGREE_FINISH), "PROCESS_SUCCESS");
    }

    @Override
    protected CandidateStateSnapshot exportCurrentSnapshot() {
        return new PlaceHolderStateSnapshot();
    }
}
