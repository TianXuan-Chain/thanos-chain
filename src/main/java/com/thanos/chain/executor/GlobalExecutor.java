package com.thanos.chain.executor;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * GlobalExecutor.java description：
 *
 * @Author laiyiyu create on 2020-07-10 14:25:21
 */
public class GlobalExecutor {

    static final Logger logger = LoggerFactory.getLogger("executor");

    //public static ArrayBlockingQueue<EventData> pendingExecuteEvent = new ArrayBlockingQueue<>(50);

    StateLedger stateLedger;

    AsyncExecutor asyncExecutor;

    public GlobalExecutor(StateLedger stateLedger) {
        this.stateLedger = stateLedger;
        this.asyncExecutor = new AsyncExecutor(stateLedger.consensusChainStore, stateLedger);
    }

    public Optional<EpochState> doExecuteGlobalNodeEvents(EpochState currentEpochState, boolean fromNormal, byte[] eventId, long number, GlobalNodeEvent[] globalNodeEvents) {

        if (globalNodeEvents.length > 0) {
            stateLedger.consensusChainStore.doubleSpendCheck.doExeGlobalEventDoubleSpendCheck(eventId, globalNodeEvents);
            EpochState newEpochState = currentEpochState.copy();
            for (GlobalNodeEvent globalNodeEvent: globalNodeEvents) {
                logger.debug(" doExecuteGlobalNodeEvent content:{}", globalNodeEvent);

                GlobalNodeEventExecutor globalNodeEventExecutor = new GlobalNodeEventExecutor(stateLedger.consensusChainStore.globalStateRepositoryRoot, newEpochState, globalNodeEvent);
                globalNodeEventExecutor.execute();
            }

            stateLedger.consensusChainStore.globalStateRepositoryRoot.flush(newEpochState);
            newEpochState.getGlobalEventState().calculateStateRoot();
            newEpochState.reEncode(newEpochState.getEpoch() + 1);
            return Optional.of(newEpochState);

        } else {
            return Optional.empty();
        }
    }

    public long getLatestExecuteNum() {
        return stateLedger.getLatestBeExecutedNum();
    }

    public long getLatestExecuteEpoch() {
        return stateLedger.getLatestBeExecutedEpoch();
    }
}
