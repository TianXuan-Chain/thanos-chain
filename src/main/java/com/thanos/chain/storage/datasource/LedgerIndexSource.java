package com.thanos.chain.storage.datasource;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.common.utils.ByteUtil;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.store.DefaultValueable;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.chain.storage.datasource.inmem.CacheDbSource;
import com.thanos.chain.storage.datasource.rocksdb.RocksDbSource;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LedgerIndexSource.java description：
 *
 * @Author laiyiyu create on 2020-10-30 17:21:23
 */
public class LedgerIndexSource {

    private static final Logger logger = LoggerFactory.getLogger("db");

    private static final Map<String, Class<? extends Persistable>> COLUMN_FAMILIES = new HashMap() {{
        put("single_entry", DefaultValueable.class);
        put("transaction_receipt", EthTransactionReceipt.class);
        put("global_node_event", GlobalNodeEvent.class);
    }};

    static final Keyable LATEST_BE_INDEX_NUMBER = Keyable.ofDefault("LATEST_BE_INDEX_NUMBER".getBytes());

    private final AbstractDbSource db;

    private volatile long latestBeIndexNum;



    public LedgerIndexSource(boolean test, SystemConfig systemConfig) {
        if (test)  {
            db = new CacheDbSource();
        } else {
            db = new RocksDbSource("ledger_index", COLUMN_FAMILIES, systemConfig, DbSettings.newInstance().withMaxOpenFiles(systemConfig.getLedgerIndexMaxOpenFiles()).withMaxThreads(systemConfig.getLedgerIndexMaxThreads()).withWriteBufferSize(systemConfig.getLedgerIndexWriteBufferSize()));

            byte[] latestExecuteNum = db.getRaw(DefaultValueable.class, LATEST_BE_INDEX_NUMBER);
            if (latestExecuteNum == null) {
                Block genesis = systemConfig.getGenesis().asBlock();
                genesis.setReceipts(new ArrayList<>());
                ////updateLatestBeExecutedBlock(genesis);
                flush(genesis);
            } else {
                latestBeIndexNum = ByteUtil.byteArrayToLong(latestExecuteNum);
            }
        }
    }


    private volatile long currentProcessTotalTxNum = 0;


    public void flush(Block block) {
        WriteOptions writeOptions = null;
        WriteBatch writeBatch = null;
        try {
            long start = System.currentTimeMillis();
            currentProcessTotalTxNum += block.getReceipts().size();

            //logger.info("index block:{}", block.getNumber());
            writeOptions = new WriteOptions();
            writeBatch = db.writeBatchFactory.getInstance();

            ColumnFamilyHandle defaultValueHandle = db.clazz2HandleTable.get(DefaultValueable.class);
            writeBatch.put(defaultValueHandle, LATEST_BE_INDEX_NUMBER.keyBytes(), ByteUtil.longToBytes(block.getNumber()));
            //logger.info("index defaultValueHandle block:{}", block.getNumber());

            ColumnFamilyHandle receiptHandle = db.clazz2HandleTable.get(EthTransactionReceipt.class);
            for (EthTransactionReceipt ethTransactionReceipt : block.getReceipts()) {
                if (ethTransactionReceipt.getEthTransaction().isDsCheckValid()) {
                    //logger.info("index hash:[{}]", ethTransactionReceipt.getEthTransaction().getHash());
                    writeBatch.put(receiptHandle, ethTransactionReceipt.getEthTransaction().getHash(), ethTransactionReceipt.getEncoded());
                } else {
                    currentProcessTotalTxNum --;
                }
            }

            ColumnFamilyHandle nodeEventHandle = db.clazz2HandleTable.get(GlobalNodeEvent.class);
            if (block.getGlobalEvent().getGlobalNodeEvents() != null) {
                for (GlobalNodeEvent globalNodeEvent: block.getGlobalEvent().getGlobalNodeEvents()) {
                    writeBatch.put(nodeEventHandle, globalNodeEvent.getHash(), globalNodeEvent.getEncoded());
                }
            }

            for (int i = 0; i < 10; i++) {
                try {
                    db.db.write(writeOptions, writeBatch);
                    latestBeIndexNum = block.getNumber();
                    long end = System.currentTimeMillis();
                    //logger.debug("index block[{}], cost:{}ms", block.getNumber(), (end - start));
                    logger.info("index block[{}], cost:[{}ms] current total process tx num in memory [{}]",block.getNumber(),  (end - start), currentProcessTotalTxNum);

                    return;
                } catch (Exception e) {
                    logger.error("ConsensusChainStore commit error!", e);
                    if (i == 9) {
                        System.exit(0);
                    }
                }
            }
            //long start = System.currentTimeMillis();
        } catch (Exception e) {
            logger.error("Error in writeBatch update on db '{}'", e);
            throw new RuntimeException(e);
        } finally {
            if (writeBatch != null) {
                writeBatch.close();
            }

            if (writeOptions != null) {
                writeOptions.close();
            }
        }

    }

    public long getLatestBeIndexNumber() {
        return this.latestBeIndexNum;
    }

    public EthTransactionReceipt getTransactionReceipt(byte[] hash) {
        byte[] raw = db.getRaw(EthTransactionReceipt.class, Keyable.ofDefault(hash));
        if (raw == null) return null;
        return new EthTransactionReceipt(raw);
    }

    public List<byte[]> getTransactionReceiptsByHashes(List<byte[]> hashes) {
        Map<byte[], byte[]> eventDatasRes = db.batchGetRaw(EthTransactionReceipt.class, hashes);
        return new ArrayList<>(eventDatasRes.values());
    }

    public GlobalNodeEvent getGlobalNodeEvent(byte[] hash) {
        byte[] raw = db.getRaw(GlobalNodeEvent.class, Keyable.ofDefault(hash));
        if (raw == null) return null;
        return new GlobalNodeEvent(raw);
    }
}
