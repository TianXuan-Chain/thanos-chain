package com.thanos.chain.contract.ca.manager;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.contract.ca.filter.GlobalFilterChain;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.CandidateStateSnapshot;
import com.thanos.chain.ledger.model.event.ca.InvokeFilterEvent;
import com.thanos.chain.ledger.model.event.ca.PlaceHolderStateSnapshot;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;

/**
 * InvokeFilterStateManager.java description：
 *
 * @Author laiyiyu create on 2021-04-26 10:14:28
 */
public class InvokeFilterStateManager  extends StateManager  {

    protected InvokeFilterStateManager(EpochState epochState, GlobalStateRepositoryImpl repository) {
        super(epochState, repository);
    }

    @Override
    protected GlobalNodeEventReceipt doExecute(GlobalNodeEvent nodeEvent) {

        InvokeFilterEvent invokeFilterEvent = (InvokeFilterEvent) nodeEvent.getCommandEvent();

        GlobalFilterChain globalFilterChain = this.repository.getGlobalFilterChain();


        ProcessResult<byte[]> processResult = globalFilterChain.invokeFilter(repository, invokeFilterEvent.getInvokeAddr(), invokeFilterEvent.getInvokeMethodId(), invokeFilterEvent.getMethodInput());

        this.finish = true;
        return new GlobalNodeEventReceipt(nodeEvent, processResult.getResult(), processResult.getErrMsg());
    }

    @Override
    protected CandidateStateSnapshot exportCurrentSnapshot() {
        return new PlaceHolderStateSnapshot();
    }
}
