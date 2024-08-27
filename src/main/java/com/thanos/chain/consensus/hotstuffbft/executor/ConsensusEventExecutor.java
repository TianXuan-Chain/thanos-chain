package com.thanos.chain.consensus.hotstuffbft.executor;

import com.thanos.chain.txpool.TxnManager;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.executor.GlobalExecutor;
import com.thanos.chain.network.NetInvoker;
import com.thanos.chain.state.sync.layer2.Layer2ChainSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * ConsensusEventExecutor.java description：
 *
 * @Author laiyiyu create on 2020-03-04 17:59:08
 */
public class ConsensusEventExecutor {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    ConsensusChainStore consensusChainStore;

    GlobalExecutor globalExecutor;

    Layer2ChainSynchronizer layer2ChainSynchronizer;

    public ConsensusEventExecutor(ConsensusChainStore consensusChainStore, NetInvoker netInvoker, GlobalExecutor globalExecutor,  TxnManager txnManager) {
        this.consensusChainStore = consensusChainStore;
        this.globalExecutor = globalExecutor;
        this.layer2ChainSynchronizer = new Layer2ChainSynchronizer(netInvoker, consensusChainStore, globalExecutor, txnManager);
    }

    public  ExecutedEventOutput execute(Event event, ExecutedEvent parent) {
        // execute the node event(register/ unregister),and the update the ExecutedEventOutput.validators
        // when event(register/ unregister), we should set ExecutedEventOutput.epochState, this will trigger epoch change
        // EpochState.epoch = event.getEpoch() + 1;
        //doExeDoubleSpendCheck(event);
        Optional<EpochState> epochState = globalExecutor.doExecuteGlobalNodeEvents(consensusChainStore.getLatestLedger().getCurrentEpochState(), true, event.getId(), event.getEventNumber(), event.getEventData().getGlobalEvent().getGlobalNodeEvents());
        ExecutedEventOutput executedEventOutput = new ExecutedEventOutput(new HashMap(), event.getEventNumber(), parent.getStateRoot(), epochState);
        return executedEventOutput;
    }

//    private void doExeDoubleSpendCheck(Event event) {
//        event.getPayload().reDecoded();
//        this.consensusChainStore.doubleSpendCheck.doExeDoubleSpendCheck(event);
//    }

    public void commit(List<ExecutedEvent> eventsToCommit, LedgerInfoWithSignatures finalityProof) {

        List<EventData> eventDatas = new ArrayList<>(eventsToCommit.size());
        List<EventInfoWithSignatures> eventInfoWithSignatures = new ArrayList<>(eventsToCommit.size());
        for (ExecutedEvent executedEvent: eventsToCommit) {
            if (executedEvent.getEvent().getEventData().isEmptyPayload() && executedEvent.getEvent().getEventData().getGlobalEvent().isEmpty()) continue;

            EventData eventData = executedEvent.getEvent().getEventData();

            // wait state Consistent!
            if (!eventData.getGlobalEvent().isEmpty()) {

                while (!isStateConsistent()) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                }

            }
            eventDatas.add(executedEvent.getEvent().getEventData());
            eventInfoWithSignatures.add(executedEvent.getEventInfoWithSignatures());
        }
        ExecutedEventOutput lastOutput = eventsToCommit.get(eventsToCommit.size() - 1).getExecutedEventOutput();
        consensusChainStore.commit(eventDatas, eventInfoWithSignatures, lastOutput, finalityProof, true);

//        if (lastOutput.hasReconfiguration()) {
//            ChainedBFT.publishConfigPayload(OnChainConfigPayload.build(lastOutput.getEpochState().get()));
//        }
    }

    public ProcessResult<Void> syncTo(LedgerInfoWithSignatures ledgerInfo, EventData latestCommitEvent, byte[] syncPeer) {
        return layer2ChainSynchronizer.syncTo(ledgerInfo, latestCommitEvent, syncPeer);
    }

    public LatestLedger getLatestLedgerInfo() {
        return consensusChainStore.getLatestLedger();
    }

    public boolean isSyncing() {
        return layer2ChainSynchronizer.isSyncing();
    }

    public boolean isStateConsistent() {
        return
                this.consensusChainStore.getLatestLedger().getLatestNumber()
                == this.globalExecutor.getLatestExecuteNum();
    }
}
