package com.thanos.chain.consensus.hotstuffbft.store;

import com.thanos.chain.consensus.hotstuffbft.model.EventDataDsCheckResult;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.consensus.hotstuffbft.model.ConsensusPayload;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.common.utils.ThanosThreadFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * DoubleSpendCheck.java description：
 *
 * @Author laiyiyu create on 2020-08-17 09:52:43
 */
public class DoubleSpendCheck {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    private final static int PARALLEL_THRESHOLD = 10000;

    private final static int PROCESSOR_NUM = 4;

    private static ThreadPoolExecutor txExecutor = new ThreadPoolExecutor(PROCESSOR_NUM, PROCESSOR_NUM, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), new ThanosThreadFactory("ds_check_update"));


    volatile long maxFutureSize;

    volatile long startEventNumber;

    volatile long endEventNumber;

    volatile long gensisStartNumber;

    volatile boolean futureCheck;

    Set<ByteArrayWrapper> txNonceCache;

    Set<ByteArrayWrapper> eventNonceCache;

    ConsensusChainStore consensusChainStore;

    ReentrantReadWriteLock lock;

    public DoubleSpendCheck(long startNumber, long maxFutureSize, ConsensusChainStore consensusChainStore) {
        this.consensusChainStore = consensusChainStore;
        this.gensisStartNumber = startNumber;
        this.startEventNumber = startNumber;
        this.endEventNumber = startNumber;
        this.maxFutureSize = maxFutureSize - 2;
        this.txNonceCache = new HashSet<>(1000000);
        this.eventNonceCache = new HashSet<>(100);
        this.lock = new ReentrantReadWriteLock(true);
        this.futureCheck = consensusChainStore.systemConfig.futureCheck();
        updateCache(consensusChainStore.getLatestLedger().getLatestNumber(), true, null);
    }



    public void applyImport(List<EthTransaction[]> ethTransactionArrays, LinkedHashMap<ByteArrayWrapper, EthTransaction> pendingTxs, Set<ByteArrayWrapper> pendingTxsCheck) {
        try {
            lock.readLock().lock();
            long latestNumber = consensusChainStore.getLatestLedger().getLatestNumber();
            long maxFutureNumber = maxFutureSize + latestNumber;

            for (EthTransaction[] ethTransactions: ethTransactionArrays) {
                for (EthTransaction tx: ethTransactions) {
                    boolean containDs = txNonceCache.contains(tx.getDsCheck());
                    if (containDs
                            || latestNumber >= tx.getFutureEventNumber()
                            || tx.getFutureEventNumber() > maxFutureNumber
                            || pendingTxsCheck.contains(tx.getDsCheck())
                            ) {
                        logger.warn("applyImport tx[{}],future number[{}], hash exist!, containDs[{}], current maxFuture number[{}], latestNumber[{}] ", Hex.toHexString(tx.getHash()), tx.getFutureEventNumber(), containDs, maxFutureNumber, latestNumber);
                        continue;
                    }

                    pendingTxsCheck.add(tx.getDsCheck());
                    pendingTxs.put(tx.getDsCheck(), tx);
                }
            }


        } catch (Exception e) {

        } finally {
            lock.readLock().unlock();
        }
    }

    public void applyImportTest(List<EthTransaction[]> ethTransactionArrays, LinkedHashMap<ByteArrayWrapper, EthTransaction> pendingTxs, Set<ByteArrayWrapper> pendingTxsCheck) {
        try {
            lock.readLock().lock();

            for (EthTransaction[] ethTransactions: ethTransactionArrays) {
                for (EthTransaction tx: ethTransactions) {
                    if (pendingTxsCheck.add(tx.getDsCheck())) {
                        pendingTxs.putIfAbsent(tx.getDsCheck(), tx);
                    }

                }
            }
        } catch (Exception e) {

        } finally {
            lock.readLock().unlock();
        }
    }

    public void applyImport(GlobalNodeEvent[] events, LinkedHashMap<ByteArrayWrapper, GlobalNodeEvent> pendingEvents, Set<ByteArrayWrapper> pendingEventsCheck) {
        try {
            lock.readLock().lock();
            long latestNumber = consensusChainStore.getLatestLedger().getLatestNumber();
            long maxFutureNumber = maxFutureSize + latestNumber;
            for (GlobalNodeEvent event: events) {
                boolean containDs = eventNonceCache.contains(event.getDsCheck());
                if (containDs
                        || pendingEventsCheck.contains(event.getDsCheck())
                        || latestNumber >= event.getFutureEventNumber()
                        || event.getFutureEventNumber() > maxFutureNumber
                        ) {
                    logger.warn("applyImport event[{}],future number[{}], hash exist! containDs[{}], current maxFuture number[{}] , latestNumber[{}]", Hex.toHexString(event.getHash()), event.getFutureEventNumber(), containDs, maxFutureNumber, latestNumber);
                    continue;
                }
                pendingEventsCheck.add(event.getDsCheck());
                pendingEvents.putIfAbsent(event.getDsCheck(), event);
            }
        } catch (Exception e) {

        } finally {
            lock.readLock().unlock();
        }
    }

//    public void doReimport(List<EventData> reimportEventDatas, LinkedHashMap<ByteArrayWrapper, GlobalNodeEvent> pendingEvents, LinkedHashMap<ByteArrayWrapper, EthTransaction> pendingTxs) {
//        try {
//            lock.readLock().lock();
//
//            for (EventData eventData: reimportEventDatas) {
//                for (GlobalNodeEvent globalNodeEvent: eventData.getGlobalEvent().getGlobalNodeEvents()) {
//                    if (!eventNonceCache.contains(globalNodeEvent.getDsCheck())) {
//                        logger.info("doImportUnCommitEvents[{}-{}] globalNodeEvent[{}]:", eventData.getNumber(), Hex.toHexString(eventData.getHash()), Hex.toHexString(globalNodeEvent.getHash()));
//                        pendingEvents.put(globalNodeEvent.getDsCheck(), globalNodeEvent);
//                    }
//
//                }
//
//                eventData.getPayload().reDecoded();
//                for (EthTransaction tx: eventData.getPayload().getEthTransactions()) {
//                    if (!txNonceCache.contains(tx.getDsCheck())) {
//                        pendingTxs.put(tx.getDsCheck(), tx);
//                    }
//
//                }
//
//                if (logger.isDebugEnabled()) {
//                    for (EthTransaction tx: eventData.getPayload().getEthTransactions()) {
//                        logger.debug("doImportUnCommitEvents tx[{}]:", Hex.toHexString(tx.getHash()));
//                    }
//                }
//            }
//
//        } catch (Exception e) {
//            logger.warn("updateCache warn! {}", ExceptionUtils.getStackTrace(e));
//            throw new RuntimeException(e);
//        } finally {
//            lock.readLock().unlock();
//        }
//
//    }

    public void updateCache(long lastNumber , boolean rebuild, EventData updateEventData) {
        try {
            lock.writeLock().lock();

            long startTime = System.currentTimeMillis();
            //long lastNumber = consensusChainStore.getLatestLedger().getLatestNumber();
            long preStartNumber = this.startEventNumber;
            long preEndNumber = this.endEventNumber;

            this.endEventNumber = lastNumber;

            if (lastNumber > maxFutureSize + gensisStartNumber) {
                this.startEventNumber = lastNumber - maxFutureSize;
            } else {
                this.startEventNumber = gensisStartNumber;
            }


            long start1 = System.currentTimeMillis();
            if (rebuild) {
                eventNonceCache.clear();
                txNonceCache.clear();
                preEndNumber = gensisStartNumber;


                for (long i = Math.max(preEndNumber + 1, this.startEventNumber); i<= this.endEventNumber; i++) {
                    EventData eventData = null;
                    if (eventData == null) {
                        eventData = this.consensusChainStore.getEventData(i, true, false);
                    }

                    for (EthTransaction ethTransaction: eventData.getPayload().getEthTransactions()) {
                        txNonceCache.add(ethTransaction.getDsCheck());
                    }

                    for (GlobalNodeEvent globalNodeEvent: eventData.getGlobalEvent().getGlobalNodeEvents()) {
                        this.eventNonceCache.add(globalNodeEvent.getDsCheck());
                    }
                }
            } else {

                for (long i = preStartNumber; i < this.startEventNumber; i++) {
                    EventData eventData = this.consensusChainStore.getEventData(i, true, false);

                    if (eventData == null) {
                        logger.debug("remove number [{}] event is null", i);
                        continue;
                    }

                    ConsensusPayload consensusPayload = eventData.getPayload();
                    // ensure decode

                    for (EthTransaction tx: consensusPayload.getEthTransactions()) {
                        txNonceCache.remove(tx.getDsCheck());
                    }

                    for (GlobalNodeEvent globalNodeEvent: eventData.getGlobalEvent().getGlobalNodeEvents()) {
                        this.eventNonceCache.remove(globalNodeEvent.getDsCheck());
                    }
                }
            }

            long end1 = System.currentTimeMillis();

            long start2 = System.currentTimeMillis();

            if (updateEventData == null) {
                return;
            }

            long latestNumber = lastNumber - 1;
            long maxFutureNumber = maxFutureSize + latestNumber;
            ConsensusPayload consensusPayload = updateEventData.getPayload();
            EthTransaction[] ethTransactions = consensusPayload.getEthTransactions();
            byte[] ethCheckResBytes = new byte[ethTransactions.length];

            if (futureCheck) {
                for (int index = 0; index < ethTransactions.length; index++) {
                    EthTransaction ethTransaction = ethTransactions[index];
                    boolean containDs = txNonceCache.contains(ethTransaction.getDsCheck());
                    if (containDs
                            || latestNumber >= ethTransaction.getFutureEventNumber()
                            || ethTransaction.getFutureEventNumber() > maxFutureNumber ) {
                        logger.warn("updateCache tx[{}-{}],future number[{}], hash exist!, containDs[{}], current maxFuture number[{}], latestNumber[{}] ", index, Hex.toHexString(ethTransaction.getHash()), ethTransaction.getFutureEventNumber(), containDs, maxFutureNumber, latestNumber);
                        ethTransaction.setDsCheckUnValid();
                        ethCheckResBytes[index] = 0;
                    } else {
                        ethCheckResBytes[index] = 1;
                    }


                    txNonceCache.add(ethTransaction.getDsCheck());
                }
            } else {
                for (int index = 0; index < ethTransactions.length; index++) {
                    ethCheckResBytes[index] = 1;
                }
            }

            updateEventData.setEventDataDsCheckResult(new EventDataDsCheckResult(updateEventData.getNumber(), ethCheckResBytes));

            for (GlobalNodeEvent globalNodeEvent: updateEventData.getGlobalEvent().getGlobalNodeEvents()) {
                this.eventNonceCache.add(globalNodeEvent.getDsCheck());
            }

            long end2 = System.currentTimeMillis();

            long endTime = System.currentTimeMillis();
            String traceInfo = ethTransactions.length > 0? " all chain trace ": "empty txs";
            logger.debug("{} DoubleSpendCheck.updateCache txNonceCache size:[{}], total coast:[{}], remove cache cost[{}], add cache cost:[{}]", traceInfo, txNonceCache.size(), (endTime - startTime), (end1 - start1),(end2 - start2));
        } catch (Exception e) {
            logger.warn("updateCache warn! {}", ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
            //e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void doExeGlobalEventDoubleSpendCheck(byte[] eventId, GlobalNodeEvent[] globalNodeEvents) {
        try {
            lock.readLock().lock();

            long latestNumber = consensusChainStore.getLatestLedger().getLatestNumber();
            long maxFutureNumber = maxFutureSize + latestNumber;
            for (GlobalNodeEvent globalNodeEvent: globalNodeEvents) {
                if (eventNonceCache.contains(globalNodeEvent.getDsCheck())
                        || latestNumber >= globalNodeEvent.getFutureEventNumber()
                        || globalNodeEvent.getFutureEventNumber() > maxFutureNumber ) {
                    logger.warn("eventData[{}], has ds globalEvent[{}]", Hex.toHexString(eventId), Hex.toHexString(globalNodeEvent.getHash()));
                    globalNodeEvent.setDsCheckUnValid();
                }
            }


        } catch (Exception e) {
            logger.warn("doExeGlobalEventDoubleSpendCheck warn! {}", ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
            //e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }

//    public void doExeEthTxDoubleSpendCheck(byte[] eventId, EthTransaction[] txs) {
//        try {
//            lock.readLock().lock();
//
//            long latestNumber = consensusChainStore.getLatestLedger().getLatestNumber();
//            long maxFutureNumber = maxFutureSize + latestNumber;
//
//
//            if (txs.length < PARALLEL_THRESHOLD) {
//                doRangeExeEthTxDoubleSpendCheck(eventId, latestNumber, maxFutureNumber, 0, txs.length - 1, txs);
//            } else {
//
//                int batchSize = txs.length / PROCESSOR_NUM;
//                int remainder = txs.length % PROCESSOR_NUM;
//
//                CountDownLatch await = new CountDownLatch(PROCESSOR_NUM);
//                //System.out.println("user parallel!");
//                for (int count = 0; count < PROCESSOR_NUM; count++) {
//                    final int pos = count;
//                    final int startPos = count * (batchSize);
//                    int endPosition = (count + 1)* batchSize -1;
//
//                    if (count == PROCESSOR_NUM - 1) {
//                        endPosition += remainder;
//                    }
//
//                    final int endPos = endPosition;
//
//                    txExecutor.execute(() -> {
//                        doRangeExeEthTxDoubleSpendCheck(eventId, latestNumber, maxFutureNumber, startPos, endPos, txs);
//                        await.countDown();
//                    });
//                    //publicEvent(transactions, startPosition, endPosition, count, payloadStartOffset, payload, await);
//                    //System.out.println(startPosition + "-" + endPosition + "-" + count);
//                }
//            }
//
//
//            for (EthTransaction ethTransaction: txs) {
//                if (txNonceCache.contains(ethTransaction.getDsCheck())
//                        || latestNumber >= ethTransaction.getFutureEventNumber()
//                        || ethTransaction.getFutureEventNumber() > maxFutureNumber ) {
//                    logger.warn("eventData[{}], has ds eth tx[{}]", Hex.toHexString(eventId), Hex.toHexString(ethTransaction.getHash()));
//                    ethTransaction.setDsCheckUnValid();
//                }
//            }
//        } catch (Exception e) {
//            logger.warn("doExeEthTxDoubleSpendCheck warn! {}", ExceptionUtils.getStackTrace(e));
//            throw new RuntimeException(e);
//            //e.printStackTrace();
//        } finally {
//            lock.readLock().unlock();
//        }
//    }


//    private void doRangeExeEthTxDoubleSpendCheck(byte[] eventId, long latestNumber, long maxFutureNumber, int start, int end, EthTransaction[] txs) {
//        for (int i = start; i <= end; i++) {
//            EthTransaction ethTransaction = txs[i];
//            if (txNonceCache.contains(ethTransaction.getDsCheck())
//                    || latestNumber >= ethTransaction.getFutureEventNumber()
//                    || ethTransaction.getFutureEventNumber() > maxFutureNumber ) {
//                logger.warn("eventData[{}], has ds eth tx[{}]", Hex.toHexString(eventId), Hex.toHexString(ethTransaction.getHash()));
//                ethTransaction.setDsCheckUnValid();
//            }
//        }
//    }
}
