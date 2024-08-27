package com.thanos.chain.storage.datasource;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.common.utils.ByteUtil;
import com.thanos.chain.contract.eth.evm.ContractCode;
import com.thanos.chain.contract.eth.evm.DataWord;
import com.thanos.chain.ledger.model.AccountState;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.BlockSign;
import com.thanos.chain.ledger.model.store.DefaultValueable;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.chain.ledger.model.store.TestModel;
import com.thanos.chain.storage.datasource.inmem.CacheDbSource;
import com.thanos.chain.storage.datasource.rocksdb.RocksDbSource;
import org.apache.commons.lang3.tuple.Pair;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thanos.common.utils.HashUtil.CRYPTO_PROVIDER;
import static com.thanos.common.utils.HashUtil.HASH_256_LIGHT_ALGORITHM_NAME;

/**
 * LedgerSource.java description：
 *
 * @Author laiyiyu create on 2020-02-25 09:49:42
 */
public class LedgerSource {

    private static final Logger logger = LoggerFactory.getLogger("db");

    private static final Map<String, Class<? extends Persistable>> COLUMN_FAMILIES = new HashMap() {{
        put("test_model", TestModel.class);
        put("contract_state", DataWord.class);
        put("account_state", AccountState.class);
        put("contract_code", ContractCode.class);

        put("single_entry", DefaultValueable.class);
        put("block", Block.class);
        put("block_sign", BlockSign.class);
        put("transaction_receipt", EthTransactionReceipt.class);
    }};

    static final Keyable LATEST_BE_EXECUTED_NUMBER = Keyable.ofDefault("LATEST_BE_EXECUTED_NUMBER".getBytes());
    static final Keyable LATEST_BE_EXECUTED_EPOCH = Keyable.ofDefault("LATEST_BE_EXECUTED_EPOCH".getBytes());

    //private static final LedgerSource cacheSource = new LedgerSource(true, SystemConfig.getDefault());

    private LedgerLruCache ledgerLruCache;

    private final AbstractDbSource db;

    private List<Pair<Keyable, Persistable>> flushCache = new ArrayList<>(131107);

    private volatile long latestBeExecutedNum;

    private volatile long latestBeExecutedEpoch;

    public LedgerSource(boolean test, SystemConfig systemConfig) {
        if (test)  {
            db = new CacheDbSource();
        } else {
            db = new RocksDbSource("ledger", COLUMN_FAMILIES, systemConfig, DbSettings.newInstance().withMaxOpenFiles(systemConfig.getLedgerMaxOpenFiles()).withMaxThreads(systemConfig.getLedgerMaxThreads()).withWriteBufferSize(systemConfig.getLedgerWriteBufferSize()));

            byte[] latestExecuteNum = db.getRaw(DefaultValueable.class, LATEST_BE_EXECUTED_NUMBER);
            byte[] latestExecuteEpoch = db.getRaw(DefaultValueable.class, LATEST_BE_EXECUTED_EPOCH);
            if (latestExecuteNum == null) {
                Block genesis = systemConfig.getGenesis().asBlock();
                genesis.setReceipts(new ArrayList<>());
                //updateLatestBeExecutedBlock(genesis);
                doPersist(genesis);
            } else {
                this.latestBeExecutedNum = ByteUtil.byteArrayToLong(latestExecuteNum);
                this.latestBeExecutedEpoch = ByteUtil.byteArrayToLong(latestExecuteEpoch);
            }
        }
        ledgerLruCache = new LedgerLruCache(systemConfig.getLedgerContractStateCacheSize());
    }

    public long getLatestBeExecutedNum() {
        return latestBeExecutedNum;
    }

    public long getLatestBeExecutedEpoch() {
        return latestBeExecutedEpoch;
    }

    public EthTransactionReceipt getTransactionByHash(byte[] hash) {
        byte[] dbValues = db.getRaw(EthTransactionReceipt.class, Keyable.ofDefault(hash));
        if (dbValues == null) {
            return null;
        }

        return new EthTransactionReceipt(dbValues);
    }

    public Block getBlockByNumber(long number) {
        byte[] dbValues = db.getRaw(Block.class, Keyable.ofDefault(ByteUtil.longToBytes(number)));
        if (dbValues == null) {
            return null;
        }
        return new Block(dbValues);
    }

    public BlockSign getBlockSignByNumber(long number) {
        byte[] dbValues = db.getRaw(BlockSign.class, Keyable.ofDefault(ByteUtil.longToBytes(number)));
        if (dbValues == null) {
            return null;
        }
        return new BlockSign(dbValues);
    }

    public DataWord getContractState(Keyable key) {

        DataWord value = ledgerLruCache.getContractState(key);
        if (value != null) {
            return value;
        }
        //System.out.println("miss contract state cache:" + key);
        byte[] dbValues = db.getRaw(DataWord.class, key);
        if (dbValues == null) return null;
        return DataWord.of(dbValues);
    }

    public void updateContractStates(Map<Keyable, DataWord> contractStateWriteCache) {
        ledgerLruCache.updateContractStates(contractStateWriteCache);
    }

    public AccountState getAccountState(Keyable address) {
        AccountState value = ledgerLruCache.getAccountState(address);
        if (value != null) {
            return value;
        }
        return (AccountState) db.get(AccountState.class, address);
    }

    public void updateAccountStates(Map<Keyable, AccountState> addr2AccountWriteCache) {
        ledgerLruCache.updateAccountStates(addr2AccountWriteCache);
    }

    public ContractCode getContractCode(Keyable codeKey) {
        ContractCode value = ledgerLruCache.getContractCode(codeKey);
        if (value != null) {
            return value;
        }
        return (ContractCode) db.get(ContractCode.class, codeKey);
    }

    public void updateContractCodes(Map<Keyable, ContractCode> hash2CodeWriteCache) {
        ledgerLruCache.updateContractCodes(hash2CodeWriteCache);
    }

    public byte[] getCurrentStateRootHash() {
        return sha3LightStates(this.flushCache);
    }

    public void clearAllWriteCache() {
        ledgerLruCache.clearAll();
    }


    // for speed up
    volatile WriteBatch writeBatch;
    public void cacheWriteBatch(Map<Keyable, DataWord> contractStateWriteCache, Map<Keyable, AccountState> addr2AccountWriteCache, Map<Keyable, ContractCode> hash2CodeWriteCache) {
        try {

            writeBatch = db.writeBatchFactory.getInstance();
            ColumnFamilyHandle dataWordHandle = db.clazz2HandleTable.get(DataWord.class);
            for (Map.Entry<Keyable, DataWord> entry : contractStateWriteCache.entrySet()) {
                if (entry.getValue().valueBytes() == null) {
                    writeBatch.delete(dataWordHandle, entry.getKey().keyBytes());
                } else {

                    //logger.debug("key[{}]-value[{}]", Hex.toHexString(entry.getKey().keyBytes()), ByteUtil.byteArrayToLong(entry.getValue().valueBytes()));
                    writeBatch.put(dataWordHandle, entry.getKey().keyBytes(), entry.getValue().valueBytes());
                }
            }

            ColumnFamilyHandle accountStateHandle = db.clazz2HandleTable.get(AccountState.class);
            for (Map.Entry<Keyable, AccountState> entry : addr2AccountWriteCache.entrySet()) {
                if (entry.getValue().valueBytes() == null) {
                    writeBatch.delete(accountStateHandle, entry.getKey().keyBytes());
                } else {
                    writeBatch.put(accountStateHandle, entry.getKey().keyBytes(), entry.getValue().valueBytes());
                }
            }

            ColumnFamilyHandle contractCodeHandle = db.clazz2HandleTable.get(ContractCode.class);
            for (Map.Entry<Keyable, ContractCode> entry : hash2CodeWriteCache.entrySet()) {
                if (entry.getValue().valueBytes() == null) {
                    writeBatch.delete(contractCodeHandle, entry.getKey().keyBytes());
                } else {
                    writeBatch.put(contractCodeHandle, entry.getKey().keyBytes(), entry.getValue().valueBytes());
                }
            }

        } catch (Exception e) {
            logger.warn("cacheWriteBatch error!", e);
            writeBatch.close();
            throw new RuntimeException(e);
        }
    }


    public void doPersist(Block block) {
        WriteOptions writeOptions = null;
        try {
            writeOptions = new WriteOptions();
            if (writeBatch == null) {
                writeBatch = db.writeBatchFactory.getInstance();
            }

            ColumnFamilyHandle defaultValueHandle = db.clazz2HandleTable.get(DefaultValueable.class);
            writeBatch.put(defaultValueHandle, LATEST_BE_EXECUTED_NUMBER.keyBytes(), ByteUtil.longToBytes(block.getNumber()));
            writeBatch.put(defaultValueHandle, LATEST_BE_EXECUTED_EPOCH.keyBytes(), ByteUtil.longToBytes(block.getEpoch()));

            ColumnFamilyHandle blockHandle = db.clazz2HandleTable.get(Block.class);
            writeBatch.put(blockHandle, ByteUtil.longToBytes(block.getNumber()), block.getEncoded());

            if (block.getBlockSign() != null) {
                ColumnFamilyHandle blockSignHandle = db.clazz2HandleTable.get(BlockSign.class);
                writeBatch.put(blockSignHandle, ByteUtil.longToBytes(block.getNumber()), block.getBlockSign().getEncoded());
            }

            for (int i = 0; i < 10; i++) {
                try {
                    db.db.write(writeOptions, writeBatch);
                    this.latestBeExecutedNum = block.getNumber();
                    this.latestBeExecutedEpoch = block.getEpoch();
                    return;
                } catch (Exception e) {
                    logger.error("consensusChainStore commit error!", e);
                    if (i == 9) {
                        System.exit(0);
                    }
                }
            }
        } catch (RocksDBException e) {
            logger.error("Error in writeBatch update on db '{}'", e);
            throw new RuntimeException(e);
        } finally {
            if (writeBatch != null) {
                try {
                    writeBatch.close();
                } catch (Exception e) {

                } finally {
                }

            }
            writeBatch = null;

            if (writeOptions != null) {
                writeOptions.close();
            }
        }
    }

    public static byte[] sha3LightStates(List<Pair<Keyable, Persistable>> changeStates) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_LIGHT_ALGORITHM_NAME, CRYPTO_PROVIDER);
            for (Pair<Keyable, Persistable> state: changeStates) {
                //absorb
                digest.update(state.getKey().keyBytes());
                if (state.getValue().valueBytes() != null) {
                    digest.update(state.getValue().valueBytes());
                }
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
