package com.thanos.chain.txpool;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.consensus.hotstuffbft.model.ConsensusPayload;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.ledger.model.event.GlobalEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 类TxnPool.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 15:17:21
 */
public class TxnPool {

    private static final Logger logger = LoggerFactory.getLogger("tx-pool");

    LinkedHashMap<ByteArrayWrapper, GlobalNodeEvent> pendingEvents = new LinkedHashMap<>(8);

    Set<ByteArrayWrapper> pendingEventsCheck = new HashSet<>(1024 * 64);

    LinkedHashMap<ByteArrayWrapper, EthTransaction> pendingTxs;

    Set<ByteArrayWrapper> pendingTxsCheck;

    TxnManager txnManager;

    ReentrantLock lock = new ReentrantLock(true);

    Condition txsFullCondition = lock.newCondition();

    final int maxPackSize;

    final int poolLimit;

    public TxnPool(TxnManager txnManager, int poolLimit) {
        this.txnManager = txnManager;
        this.maxPackSize = txnManager.maxPackSize;
        this.poolLimit = poolLimit;
        this.pendingTxs = new LinkedHashMap<>(poolLimit);
        this.pendingTxsCheck = new HashSet<>(poolLimit);
        logger.info("TxnPool init success!");
    }

    public void doImport(GlobalNodeEvent[] globalNodeEvents, List<EthTransaction[]> ethTransactionArrays) {
        try {
            lock.lock();
            if (globalNodeEvents != null && globalNodeEvents.length != 0) {
                txnManager.applyImport(globalNodeEvents, pendingEvents, pendingEventsCheck);
            }

            if (pendingTxsCheck.size() > poolLimit) {
                logger.warn("current tx pool is full, size={}", pendingTxsCheck.size());
                txsFullCondition.await();
            }

            if (ethTransactionArrays.size() != 0) {
                txnManager.applyImport(ethTransactionArrays, pendingTxs, pendingTxsCheck);
            }
        } catch (Exception e) {
            logger.warn("doImport error!", e);
            //e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void doImportUnCommitEvents(EventData eventData) {
        try {
            lock.lock();

            //for (EventData eventData: eventDatas.values()) {
            for (GlobalNodeEvent globalNodeEvent: eventData.getGlobalEvent().getGlobalNodeEvents()) {
                logger.info("doImportUnCommitEvents[{}-{}] globalNodeEvent[{}]:", eventData.getNumber(), Hex.toHexString(eventData.getHash()), Hex.toHexString(globalNodeEvent.getHash()));
                pendingEvents.put(globalNodeEvent.getDsCheck(), globalNodeEvent);
            }

            eventData.getPayload().reDecoded();
            for (EthTransaction tx: eventData.getPayload().getEthTransactions()) {
                pendingTxs.put(tx.getDsCheck(), tx);
            }

            if (logger.isDebugEnabled()) {
                for (EthTransaction tx: eventData.getPayload().getEthTransactions()) {
                    logger.debug("doImportUnCommitEvents tx[{}]:", Hex.toHexString(tx.getHash()));
                }
            }
            //}

            //this.txnManager.consensusChainStore.doubleSpendCheck.doReimport(eventDatas, this.pendingEvents, this.pendingTxs);

        } catch (Exception e) {
            logger.warn("doImport error! {}", ExceptionUtils.getStackTrace(e));
            //e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public boolean proposalDSCheck(EventData eventData) {
        try {
            lock.lock();
            // todo :: check event data ds
            return true;

        } catch (Exception e) {
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void doRemoveCheck(EventData eventData) {
        try {
            lock.lock();
            //for (EventData eventData: eventDatas) {
            for (GlobalNodeEvent event: eventData.getGlobalEvent().getGlobalNodeEvents()) {
                logger.info("doCommit remove event:[{}-{}]", Hex.toHexString(eventData.getHash()), Hex.toHexString(event.getHash()));
                pendingEventsCheck.remove(event.getDsCheck());
                //pendingEvents.remove(event.getDsCheck());
            }

            eventData.getPayload().reDecoded();
            for (EthTransaction tx: eventData.getPayload().getEthTransactions()) {
                pendingTxsCheck.remove(tx.getDsCheck());
                //pendingTxs.remove(tx.getDsCheck());
            }
        } finally {
            lock.unlock();
        }
    }

    public void doSyncCommit(EventData eventData) {
        try {
            lock.lock();
            //for (EventData eventData: eventDatas) {
            for (GlobalNodeEvent event: eventData.getGlobalEvent().getGlobalNodeEvents()) {
                logger.info("doSyncCommit remove event:[{}-{}]", Hex.toHexString(eventData.getHash()), Hex.toHexString(event.getHash()));
                pendingEventsCheck.remove(event.getDsCheck());
                pendingEvents.remove(event.getDsCheck());
            }

            eventData.getPayload().reDecoded();
            for (EthTransaction tx: eventData.getPayload().getEthTransactions()) {
                pendingTxsCheck.remove(tx.getDsCheck());
                pendingTxs.remove(tx.getDsCheck());
            }
        } finally {
            lock.unlock();
        }
    }

    public void doRemove(EventData eventData) {
        try {
            lock.lock();
            for (GlobalNodeEvent event: eventData.getGlobalEvent().getGlobalNodeEvents()) {
                pendingEventsCheck.add(event.getDsCheck());
                pendingEvents.remove(event.getDsCheck());
            }

            eventData.getPayload().reDecoded();
            for (EthTransaction tx: eventData.getPayload().getEthTransactions()) {
                pendingTxsCheck.add(tx.getDsCheck());
                pendingTxs.remove(tx.getDsCheck());
            }

            if (pendingTxsCheck.size() < poolLimit) {
                txsFullCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    public Pair<GlobalEvent, ConsensusPayload> doPull(boolean stateConsistent) {
        try {
            lock.lock();
            if (!stateConsistent && pendingEvents.size() > 0) {
                return Pair.of(new GlobalEvent(), new ConsensusPayload());
            }

            GlobalNodeEvent[] globalNodeEvents = new GlobalNodeEvent[pendingEvents.size()];
            Iterator<Map.Entry<ByteArrayWrapper, GlobalNodeEvent>> eventsIter = pendingEvents.entrySet().iterator();
            int count = 0;
            while (eventsIter.hasNext()) {
                globalNodeEvents[count] = eventsIter.next().getValue();
                eventsIter.remove();
                count++;
            }

            int max = Math.min(maxPackSize, pendingTxs.size());
            EthTransaction[] txs = new EthTransaction[max];
            Iterator<Map.Entry<ByteArrayWrapper, EthTransaction>> txsIter = pendingTxs.entrySet().iterator();
            for (int i = 0; i < max; i++) {
                txs[i] = txsIter.next().getValue();
                txsIter.remove();
            }

            if (pendingTxsCheck.size() < poolLimit) {
                txsFullCondition.signalAll();
            }

            return Pair.of(new GlobalEvent(globalNodeEvents), new ConsensusPayload(txs));
        } finally {
            lock.unlock();
        }
    }

    public long getLatestNumber() {
        return this.txnManager.getLatestNumber();
    }
}
