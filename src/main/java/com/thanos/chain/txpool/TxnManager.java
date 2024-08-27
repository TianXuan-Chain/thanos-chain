package com.thanos.chain.txpool;

import com.thanos.chain.txpool.EventCollector;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.consensus.hotstuffbft.model.ConsensusPayload;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.ledger.model.event.GlobalEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.txpool.TxnPool;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * TxnManager.java description：
 *
 * @Author laiyiyu create on 2020-03-02 16:44:31
 */
public class TxnManager {

    static final Logger logger = LoggerFactory.getLogger("consensus");

    public ConsensusChainStore consensusChainStore;

    public final int maxPackSize;

    public final boolean dsCheck;

    public TxnPool txnPool;

    public final EventCollector eventCollector;

    public TxnManager(int maxPackSize, int poolLimit, ConsensusChainStore consensusChainStore) {
        this(maxPackSize, poolLimit, 64, true, consensusChainStore);
    }

    public TxnManager(int maxPackSize, int poolLimit, int comingQueueSize, boolean dsCheck, ConsensusChainStore consensusChainStore) {
        this.maxPackSize = maxPackSize;
        this.dsCheck = dsCheck;
        this.consensusChainStore = consensusChainStore;
        this.txnPool = new TxnPool(this, poolLimit);
        this.eventCollector = new EventCollector(this.txnPool, comingQueueSize, consensusChainStore.test);
        logger.info("TxnManager start,dsCheck={}", dsCheck);
        //start();
    }

    public Pair<GlobalEvent, ConsensusPayload> pullEvent(boolean stateConsistent) {
        return this.txnPool.doPull(stateConsistent);
    }

    public boolean proposalDSCheck(EventData eventData) {
        return this.txnPool.proposalDSCheck(eventData);
    }

    public void doRemoveCheck(EventData eventData) {
        this.txnPool.doRemoveCheck(eventData);
    }

    public void syncCommitEvent(EventData eventData) {
        this.txnPool.doSyncCommit(eventData);
    }

    public void doImportUnCommitEvents(EventData eventData) {
        this.txnPool.doImportUnCommitEvents(eventData);
    }

    public void removeEvent(EventData eventData) {
        this.txnPool.doRemove(eventData);
    }

    public void applyImport(List<EthTransaction[]> ethTransactionArrays, LinkedHashMap<ByteArrayWrapper, EthTransaction> pendingTxs, Set<ByteArrayWrapper> pendingTxsCheck) {
        if (dsCheck) {
            consensusChainStore.doubleSpendCheck.applyImport(ethTransactionArrays, pendingTxs, pendingTxsCheck);
        } else {
            consensusChainStore.doubleSpendCheck.applyImportTest(ethTransactionArrays, pendingTxs, pendingTxsCheck);
        }
    }

    public void applyImport(GlobalNodeEvent[] events, LinkedHashMap<ByteArrayWrapper, GlobalNodeEvent> pendingEvents, Set<ByteArrayWrapper> pendingEventsCheck) {
        consensusChainStore.doubleSpendCheck.applyImport(events, pendingEvents, pendingEventsCheck);
    }

    public long getLatestNumber() {
        return consensusChainStore.getLatestLedger().getLatestNumber();
    }
}
