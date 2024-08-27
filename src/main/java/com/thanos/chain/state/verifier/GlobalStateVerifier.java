package com.thanos.chain.state.verifier;

import com.thanos.chain.gateway.BlockPublisher;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.ledger.StateLedgerIndexer;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.BlockSign;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.chain.network.NetInvoker;
import com.thanos.chain.state.verifier.model.BlockCheckContext;
import com.thanos.chain.state.verifier.model.CommitBlockMsg;
import com.thanos.chain.state.verifier.model.LocalBlockSignMsg;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ThanosWorker;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;

/**
 * GlobalStateVerifier.java description：
 *
 * @Author laiyiyu create on 2020-08-29 14:28:30
 */
public class GlobalStateVerifier {

    static final Logger logger = LoggerFactory.getLogger("state-verify");

    public static final ArrayBlockingQueue<Pair<Block, Function<Long, Void>>> toBeCheckBlockQueue = new ArrayBlockingQueue<>(100);

    StateLedger stateLedger;

    NetInvoker netInvoker;

    GlobalStateCoordinator coordinator;

    volatile BlockCheckContext currentBlockCheckContext;

    SecureKey secureKey;

    boolean test;

    boolean pushBlock;


    public GlobalStateVerifier(StateLedger stateLedger, NetInvoker netInvoker, boolean test) {
        this.stateLedger = stateLedger;
        this.netInvoker = netInvoker;
        this.secureKey = stateLedger.systemConfig.getMyKey();
        this.coordinator = new GlobalStateCoordinator(this);
        this.currentBlockCheckContext = new BlockCheckContext();
        this.test = test;
        this.pushBlock = stateLedger.systemConfig.pushBlock();
        start();
    }

    public void start() {

        new ThanosWorker("global_state_verifier_thread") {

            @Override
            protected void beforeLoop() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {

                }
            }

            @Override
            protected void doWork() throws Exception {
                Pair<Block, Function<Long, Void>> executedContext = toBeCheckBlockQueue.take();
                Block exeBlock = executedContext.getKey();
                if (!postCheck(exeBlock)) return;


                if (!test) {
                    awaitAsyncCheckSeq(exeBlock);
                    switch (currentBlockCheckContext.getCurrentStatus()) {
                        case COMMIT_BLOCK:
                            GlobalStateVerifier.this.doCommitBlock(exeBlock, executedContext.getValue());
                            break;
                        case ROLLBACK_BLOCK:
                            if (logger.isDebugEnabled()) {
                                logger.debug("rollback block[{}-txSize[{}]]!", exeBlock.getNumber(), exeBlock.getTransactionsList().length);
                            }

                            logger.warn("need rollback!");
                            GlobalStateVerifier.this.doRollbackBlock();
                            break;
                        default:
                    }
                } else {
                    logger.debug(" all chain trace do commit for test");
                    GlobalStateVerifier.this.doCommitBlock(exeBlock, executedContext.getValue());
                }


            }

            @Override
            protected void doException(Throwable e) {
                super.doException(e);
                GlobalStateVerifier.this.doRollbackBlock();
            }
        }.start();

        this.coordinator.start();
    }

    private boolean postCheck(Block exeBlock) {
        if (exeBlock.getNumber() != stateLedger.ledgerSource.getLatestBeExecutedNum() + 1) {
            return false;
        }
        return true;
    }

    private void awaitAsyncCheckSeq(Block exeBlock) {
        try {
            currentBlockCheckContext.resetAwaitCondition();
            this.coordinator.globalStateVerifierMsgQueue.put(new LocalBlockSignMsg(exeBlock.getEpoch(), exeBlock.getNumber(), exeBlock.getHash(), secureKey.getPubKey(), new Signature(secureKey.sign(exeBlock.getHash()))));
            currentBlockCheckContext.awaitCheck();
        } catch (InterruptedException e) {
            new RuntimeException(e);
        }
    }


    private volatile long currentCommitTimestap = 0;

    private void doCommitBlock(Block exeBlock, Function<Long, Void> afterPersist) {
        long start = System.currentTimeMillis();
        exeBlock.recordSign(new BlockSign(exeBlock.getEpoch(), exeBlock.getNumber(), exeBlock.getHash(), currentBlockCheckContext.getSameHashSigns()));
        try {
            coordinator.globalStateVerifierMsgQueue.put(new CommitBlockMsg(exeBlock));
        } catch (Exception e) {
        }
        stateLedger.persist(exeBlock);
        long end = System.currentTimeMillis();
        afterPersist.apply(exeBlock.getNumber());

        StateLedgerIndexer.commitBlock(exeBlock);
        if (this.pushBlock) {
            BlockPublisher.commitBlock(exeBlock);
        }

        if (currentCommitTimestap != 0) {

            double timeInterval = ((double) (end - currentCommitTimestap)) / 1000;
            long tps = (long) (exeBlock.getReceipts().size() / timeInterval);
            logger.info("counter, current commit event num:[{}] ,current commit block:[{}], total tx num:[{}], current tps:[{}].", stateLedger.consensusChainStore.getLatestLedger().getLatestNumber(), exeBlock.getNumber(), exeBlock.getReceipts().size(), tps);
        }

        currentCommitTimestap = end;
        logger.info(" all chain trace finish all consensus block[{}-{}-{}],total use [{}ms], persist use [{}ms]",
                exeBlock.getNumber(),
                exeBlock.getReceipts().size(),
                Hex.toHexString(exeBlock.getHash()),
                //Hex.toHexString(exeBlock.getReceiptsRoot()),
                (end - exeBlock.getTimestamp()),
                (end - start));
    }

    private void doRollbackBlock() {
        stateLedger.doRollBackState();
    }

    public static void addToStateCheck(Block block, Function<Long, Void> afterPersist) {
        try {
            toBeCheckBlockQueue.put(Pair.of(block, afterPersist));
        } catch (Exception e) {
            logger.warn("addToStateCheck error!", e);
            throw new RuntimeException(e);
        }
    }

    public static void clear() {
        toBeCheckBlockQueue.clear();
    }
}
