package com.thanos.chain.executor;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.contract.ca.manager.StateManager;
import com.thanos.chain.contract.ca.manager.CandidateEventConstant;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.chain.storage.db.GlobalStateRepositoryRoot;
import com.thanos.common.utils.ByteUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GlobalNodeEventExecutor.java description：
 *
 * @Author laiyiyu create on 2021-04-16 15:07:55
 */
public class GlobalNodeEventExecutor {

    static final Logger logger = LoggerFactory.getLogger("executor");

    GlobalStateRepositoryImpl repository;

    EpochState epochState;

    GlobalNodeEvent globalNodeEvent;

    public GlobalNodeEventExecutor(GlobalStateRepositoryRoot repositoryRoot, EpochState epochState, GlobalNodeEvent globalNodeEvent) {
        this.repository = repositoryRoot.startTracking();
        this.epochState = epochState;
        this.globalNodeEvent = globalNodeEvent;
    }

    public void execute() {
        try {
            StateManager stateManager = StateManager.buildManager(epochState, this.repository);
            GlobalNodeEventReceipt globalNodeEventReceipt;
            if (!globalNodeEvent.isDsCheckValid()) {
                return;
            }

            globalNodeEvent.verify(true);
            if (!globalNodeEvent.isValid()) {
                epochState.getGlobalEventState().addGlobalNodeEventReceipts(new GlobalNodeEventReceipt(globalNodeEvent, ByteUtil.intToBytes(CandidateEventConstant.VOTE_FAILED), "un valid sign"));
                return;
            }

            try {
                globalNodeEventReceipt = stateManager.execute(globalNodeEvent);
                repository.commit();
            } catch (Throwable e) {
                logger.error("GlobalNodeEventExecutor execute inner error,{}", ExceptionUtils.getStackTrace(e));
                repository.rollback();
                globalNodeEventReceipt = new GlobalNodeEventReceipt(globalNodeEvent, ByteUtil.intToBytes(CandidateEventConstant.VOTE_FAILED), e.getMessage());
            }

            logger.info("GlobalNodeEventExecutor finish receipt: {}", globalNodeEventReceipt);
            epochState.getGlobalEventState().addGlobalNodeEventReceipts(globalNodeEventReceipt);
        } catch (Exception e) {
            logger.error("GlobalNodeEventExecutor execute outer error,{}", ExceptionUtils.getStackTrace(e));

        }
    }
}
