package com.thanos.chain.executor;

import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.consensus.hotstuffbft.model.EventDataDsCheckResult;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.chain.state.verifier.GlobalStateVerifier;
import com.thanos.common.utils.ThanosWorker;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static com.thanos.common.utils.ByteUtil.ZERO_BYTE_ARRAY;

/**
 * AsyncExecutor.java description：
 *
 * @Author laiyiyu create on 2020-08-24 10:00:32
 */
public class AsyncExecutor {

    static final Logger logger = LoggerFactory.getLogger("executor");

    ConsensusChainStore consensusChainStore;

    StateLedger stateLedger;

    volatile AbstractTransactionsExecutor currentTransactionsExecutor;

    EthParallelTransactionsExecutor ethParallelTransactionsExecutor;

    EthSerialTransactionsExecutor ethSerialTransactionsExecutor;

    Map<Long, CountDownLatch> finishPersistWaitTable = new ConcurrentHashMap<>();

    Map<Long, CountDownLatch> finishFlushWaitTable = new ConcurrentHashMap<>();

    ArrayBlockingQueue<CurrentExeNumNode> nextBeExeNode = new ArrayBlockingQueue<>(1);

    volatile long currentLatestToBeExeNum;

    ThanosWorker mainExecutor;

    ThanosWorker subExecutor;

    public class CurrentExeNumNode {

        final long currentExeNum;

        final boolean notifyNext;

        public CurrentExeNumNode(long currentExeNum, boolean notifyNext) {
            this.currentExeNum = currentExeNum;
            this.notifyNext = notifyNext;
        }

        public void execute() {
            Block exeBlock = doExe();

            doFlush(exeBlock);

            toBeCheck(exeBlock);

            doNotify();
        }

        private void doFlush(Block exeBlock) {
            //CountDownLatch selfPersistAwait = new CountDownLatch(1);
            long start = System.currentTimeMillis();
            CountDownLatch awaitPrePersist = finishPersistWaitTable.get(currentExeNum - 1);
            if (awaitPrePersist == null) {

            } else {
                try {
                    awaitPrePersist.await();
                } catch (Exception e) {
                    logger.warn(String.format("waitPreNumFinishFlush[%d] error!", currentExeNum), e);
                    throw new RuntimeException(e);
                }
            }
            long end = System.currentTimeMillis();
            stateLedger.flush();
            exeBlock.setStateRoot(stateLedger.rootRepository.getRoot());
            logger.debug("[{}][{}]finish num[{}] flush, total cost:[{}], await[{}]", Thread.currentThread().getName(), Hex.toHexString(exeBlock.getEventId()), currentExeNum, (System.currentTimeMillis() - start), (end - start));
            finishFlushAwait(currentExeNum);
        }

        public void toBeCheck(Block exeBlock) {

            exeBlock.setReceiptsRoot(ExecutorUtil.calculate(exeBlock.getReceipts()));
            exeBlock.reHash();
            exeBlock.reEncoded();

            Function<Long, Void> afterPersist = num -> {
                finishPersistAwait(num);
                return null;
            };

            GlobalStateVerifier.addToStateCheck(exeBlock, afterPersist);
        }

        private void waitPreNumFinishFlush() {
            CountDownLatch awaitPre = finishFlushWaitTable.get(currentExeNum - 1);
            if (awaitPre == null) return;

            try {
                awaitPre.await();
            } catch (Exception e) {
                logger.warn(String.format("waitPreNumFinishFlush[%d] error!", currentExeNum), e);
                throw new RuntimeException(e);
            }
        }

        private Block doExe() {
            long start = System.currentTimeMillis();
            waitPreNumFinishFlush();
            long end = System.currentTimeMillis();
            //currentLatestToBeExeNum += 1;
            EventData eventData = consensusChainStore.getEventData(currentExeNum, true, true);
            if (eventData.getEventDataDsCheckResult() == null) {
                EventDataDsCheckResult dsCheckResult = consensusChainStore.getDsCheckRes(eventData.getNumber());
                byte[] dsCheckBytes = dsCheckResult.getEthTxsCheckRes();
                //logger.info("do exe block[] get from db {}", dsCheckResult);
                EthTransaction[] ethTransactions = eventData.getPayload().getEthTransactions();
                for (int i = 0; i < dsCheckBytes.length; i++) {
                    if (dsCheckBytes[i] == 0) {
                        logger.warn("doExe[{}] is un valid", Hex.toHexString(ethTransactions[i].getHash()));
                        ethTransactions[i].setDsCheckUnValid();
                    }
                }
            }

            Block block = new Block(eventData.getHash(), eventData.getParentId(), ZERO_BYTE_ARRAY, eventData.getEpoch(), eventData.getNumber(), eventData.getTimestamp(), EMPTY_BYTE_ARRAY, EMPTY_BYTE_ARRAY, eventData.getGlobalEvent(), eventData.getPayload().getEthTransactions());
            block.setDagExecuteRoots(eventData.getDagExecuteRoots());
            long end1 = System.currentTimeMillis();
            eventData.setDagExecuteRoots(null);
            List<EthTransactionReceipt> receipts = currentTransactionsExecutor.execute(block);
            block.setDagExecuteRoots(null);
            //block.setTransactionsList(null);
            block.setReceipts(receipts);
            long end2 = System.currentTimeMillis();

            logger.debug("[{}][{}] all chain trace finish doExe[{}-{}], total cost:[{}], await[{}], exe cost[{}]",
                    Thread.currentThread().getName(), Hex.toHexString(eventData.getHash()), currentExeNum, block.getReceipts().size(), (end2 - start), (end - start), (end2 - end1));

            return block;
        }

        private void doNotify() {
            if (notifyNext) {

                //currentLatestToBeExeNum++;
                createTobeExeNumAwaitCondition(true);
                logger.debug("all chain trace notify next {}", currentExeNum + 1);
                try {
                    if (consensusChainStore.getLatestLedger().getLatestNumber() >= currentExeNum + 1) {
                        nextBeExeNode.put(new CurrentExeNumNode(currentExeNum + 1, false));
                    }
                } catch (Exception e) {
                    logger.warn("doNotify error!", e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public AsyncExecutor(ConsensusChainStore consensusChainStore, StateLedger stateLedger) {
        this.consensusChainStore = consensusChainStore;
        this.stateLedger = stateLedger;

        this.ethParallelTransactionsExecutor = new EthParallelTransactionsExecutor(stateLedger);
        this.ethSerialTransactionsExecutor = new EthSerialTransactionsExecutor(stateLedger);
        // default use parallel
        this.currentTransactionsExecutor = this.ethParallelTransactionsExecutor;
        this.stateLedger.setRollbackBlockFun(aVoid -> {
            resetExecutor();
            return null;
        });
        //this.currentTransactionsExecutor = this.ethSerialTransactionsExecutor;
        start();
    }

    public void start() {
        this.currentLatestToBeExeNum = stateLedger.getLatestBeExecutedNum();
        createTobeExeNumAwaitCondition(false);
        finishFlushAwait(currentLatestToBeExeNum);
        finishPersistAwait(currentLatestToBeExeNum);
        createTobeExeNumAwaitCondition(true);

        this.mainExecutor = new ThanosWorker("event_pipeline_main_async_executor") {
            @Override
            protected void beforeLoop() {
                logger.info("event_pipeline_main_async_executor start, latestConsensusNum:[{}], currentLatestToBeExeNum;[{}]!", consensusChainStore.getLatestLedger().getLatestNumber(), stateLedger.getLatestBeExecutedNum());
            }

            @Override
            protected void doWork() throws Exception {
                long latestConsensusNum = consensusChainStore.getLatestLedger().getLatestNumber();

                //long latestExecuteEventNum = stateLedger.getLatestBeExecutedNum();

                if (latestConsensusNum < AsyncExecutor.this.currentLatestToBeExeNum) {
                    Thread.sleep(3);
                    return;
                }

                if (latestConsensusNum - AsyncExecutor.this.currentLatestToBeExeNum >= 1) {
                    exeCurrent(AsyncExecutor.this.currentLatestToBeExeNum, true);
                } else {
                    exeCurrent(AsyncExecutor.this.currentLatestToBeExeNum, false);
                }

                createTobeExeNumAwaitCondition(true);
            }

            @Override
            protected void doException(Throwable e) {
                logger.warn("event_pipeline_main_async_executor doWork error! {}", ExceptionUtils.getStackTrace(e));
                new Thread(() -> resetExecutor()).start();
            }
        };
        this.mainExecutor.start();

        this.subExecutor = new ThanosWorker("event_pipeline_sub_async_executor") {
            @Override
            protected void beforeLoop() {
                logger.info("event_pipeline_sub_async_executor start!");
            }

            @Override
            protected void doWork() throws Exception {
                //use poll with timeout avoid await all the time in resetExecutor(),when  rollback
                CurrentExeNumNode exeNumNode = nextBeExeNode.poll(200, TimeUnit.MILLISECONDS);
                if (exeNumNode != null) {
                    exeNumNode.execute();
                }
            }

            @Override
            protected void doException(Throwable e) {
                logger.warn("event_pipeline_sub_async_executor doWork error! {}", ExceptionUtils.getStackTrace(e));
                new Thread(() -> resetExecutor()).start();
            }
        };
        this.subExecutor.start();
    }

    private void finishFlushAwait(long num) {
        CountDownLatch flushAwait = finishFlushWaitTable.remove(num);
        if (flushAwait == null) return;
        flushAwait.countDown();
    }

    private void finishPersistAwait(long num) {
        CountDownLatch persistAwait = finishPersistWaitTable.remove(num);
        if (persistAwait == null) return;
        persistAwait.countDown();
    }

    private void createTobeExeNumAwaitCondition(boolean updateNext) {
        if (updateNext) {
            this.currentLatestToBeExeNum++;
        }

        finishFlushWaitTable.put(this.currentLatestToBeExeNum, new CountDownLatch(1));
        finishPersistWaitTable.put(this.currentLatestToBeExeNum, new CountDownLatch(1));
    }

    private void exeCurrent(long num, boolean notifyNext) {
        new CurrentExeNumNode(num, notifyNext).execute();
    }

    private void setParallelExecutor() {
        this.currentTransactionsExecutor = ethParallelTransactionsExecutor;
    }

    private void setSerialExecutor() {
        this.currentTransactionsExecutor = ethSerialTransactionsExecutor;
    }

    public synchronized void resetExecutor() {
        logger.info("will resetExecutor");
        this.mainExecutor.stop();
        this.subExecutor.stop();

        for (CountDownLatch countDownLatch : finishPersistWaitTable.values()) {
            countDownLatch.countDown();
        }

        for (CountDownLatch countDownLatch : finishFlushWaitTable.values()) {
            countDownLatch.countDown();
        }

        this.mainExecutor.fullAwait();
        this.subExecutor.fullAwait();
        this.nextBeExeNode.clear();
        this.finishPersistWaitTable.clear();
        this.finishFlushWaitTable.clear();
        GlobalStateVerifier.clear();
        setSerialExecutor();
        stateLedger.ledgerSource.clearAllWriteCache();
        start();
        logger.info("resetExecutor success!");
    }
}
