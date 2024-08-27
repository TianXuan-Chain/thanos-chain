package com.thanos.chain.ledger;

import com.thanos.chain.config.SystemConfig;
import com.thanos.common.utils.ThanosWorker;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.storage.datasource.LedgerIndexSource;
import com.thanos.chain.storage.datasource.LedgerSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * StateLedgerIndexer.java description：
 *
 * @Author laiyiyu create on 2020-11-01 12:19:22
 */
public class StateLedgerIndexer {

    private static final Logger logger = LoggerFactory.getLogger("ledger");

    private static BlockingQueue<Block> blockQueue = null;


    static {
        blockQueue = new ArrayBlockingQueue<>(8);
    }


    public static void commitBlock(Block block) {
        try {
            blockQueue.put(block);
        } catch (InterruptedException e) {

        }
    }

    public final LedgerIndexSource ledgerIndexSource;

    public final LedgerSource ledgerSource;


    public StateLedgerIndexer(SystemConfig systemConfig, LedgerSource ledgerSource) {
        this.ledgerIndexSource = new LedgerIndexSource(false, systemConfig);
        this.ledgerSource = ledgerSource;
        start();
    }

    private void start() {
        new ThanosWorker("block_indexer_thread") {
            @Override
            protected void beforeLoop() {

                long latestNumber = ledgerIndexSource.getLatestBeIndexNumber();

                long latestBeExecutedNum = ledgerSource.getLatestBeExecutedNum();

                logger.info("block_indexer start latestBeIndexNum:[{}], latestBeExecutedNum:[{}]", latestNumber, latestBeExecutedNum);

                if (latestNumber < latestBeExecutedNum) {

                    for (long i = latestNumber + 1; i <= latestBeExecutedNum; i++) {
                        //logger.info("indexer will query block [{}]", i);
                        Block block = ledgerSource.getBlockByNumber(i);
                        //logger.info("indexer  query block [{}] success!", i);
                        if (block == null) {
                            logger.error("block[{}] not exist!", i);
                            System.exit(0);
                        }
                        ledgerIndexSource.flush(block);
                    }

                }


            }

            @Override
            protected void doWork() throws Exception {
                Block block = blockQueue.take();
                long currentLatestNum = block.getNumber();
                long beIndexNumber = ledgerIndexSource.getLatestBeIndexNumber();

                if (currentLatestNum == beIndexNumber + 1) {
                    ledgerIndexSource.flush(block);
                } else if (currentLatestNum > beIndexNumber + 1) {

                    for (long i = beIndexNumber + 1; i <= currentLatestNum; i++) {
                        Block persistBlock = ledgerSource.getBlockByNumber(i);
                        ledgerIndexSource.flush(persistBlock);
                    }

                } else {
                    // do nothing
                }

            }

        }.start();
    }

}
