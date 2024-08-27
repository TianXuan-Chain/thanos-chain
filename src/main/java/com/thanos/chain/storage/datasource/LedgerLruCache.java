package com.thanos.chain.storage.datasource;

import com.thanos.common.utils.QuickHashedMap;
import com.thanos.chain.contract.eth.evm.ContractCode;
import com.thanos.chain.contract.eth.evm.DataWord;
import com.thanos.chain.ledger.model.AccountState;
import com.thanos.chain.ledger.model.store.Keyable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LedgerLruCache.java description：
 *
 * @Author laiyiyu create on 2020-02-25 12:41:32
 */
public class LedgerLruCache {

    private static final Logger logger = LoggerFactory.getLogger("db");

//===================================================================
    private final ReentrantReadWriteLock CONTRACT_STATE_RW_LOCK;

    private final int CONTRACT_STATE_CACHE_COUNT;

    private final int ACCOUNT_STATE_CACHE_COUNT = 1000;

    private final int CONTRACT_CODE_CACHE_COUNT = 1000;


    private Map<Keyable, DataWord> CONTRACT_STATE_CACHE;

    private Map<Keyable, AccountState> ACCOUNT_STATE_CACHE;

    private Map<Keyable, ContractCode> CONTRACT_CODE_CACHE;

    public LedgerLruCache(int CONTRACT_STATE_CACHE_COUNT) {
        this.CONTRACT_STATE_CACHE_COUNT = CONTRACT_STATE_CACHE_COUNT;
        this.CONTRACT_STATE_RW_LOCK = new ReentrantReadWriteLock(false);
        this.CONTRACT_STATE_CACHE = new QuickHashedMap<>(this.CONTRACT_STATE_CACHE_COUNT);
        this.ACCOUNT_STATE_CACHE = new ConcurrentHashMap<>(this.ACCOUNT_STATE_CACHE_COUNT);
        this.CONTRACT_CODE_CACHE = new ConcurrentHashMap<>(this.CONTRACT_CODE_CACHE_COUNT);
        logger.info("LedgerLruCache CONTRACT_STATE_CACHE_COUNT:{}", this.CONTRACT_STATE_CACHE_COUNT);
    }

    public  DataWord getContractState(Keyable keyable) {
        try {
            CONTRACT_STATE_RW_LOCK.readLock().lock();
            return CONTRACT_STATE_CACHE.get(keyable);
        } finally {
            CONTRACT_STATE_RW_LOCK.readLock().unlock();
        }

    }

    public void putContractState(Keyable keyable, DataWord dataWord) {
        CONTRACT_STATE_CACHE.put(keyable, dataWord);
    }

    public void updateContractStates(Map<Keyable, DataWord> contractStateWriteCache) {
        try {
            CONTRACT_STATE_RW_LOCK.writeLock().lock();


            if (contractStateWriteCache.size() >= CONTRACT_STATE_CACHE_COUNT) {
                CONTRACT_STATE_CACHE.clear();
                //CONTRACT_CODE_CACHE.putAll();
            } else if (contractStateWriteCache.size() + CONTRACT_STATE_CACHE.size() <= CONTRACT_STATE_CACHE_COUNT) {


            } else {
                long evictSize = contractStateWriteCache.size() + CONTRACT_STATE_CACHE.size() - CONTRACT_STATE_CACHE_COUNT;
                logger.debug("CONTRACT_STATE_CACHE.size[{}], contractStateWriteCache.size[{}], evictSize[{}]", CONTRACT_STATE_CACHE.size(), contractStateWriteCache.size(), evictSize);

                Iterator<Map.Entry<Keyable, DataWord>> iter = CONTRACT_STATE_CACHE.entrySet().iterator();
                while (evictSize > 0) {
                    iter.next();
                    iter.remove();
                    evictSize--;
                }
            }

            for (Map.Entry<Keyable, DataWord> entry: contractStateWriteCache.entrySet()) {
                CONTRACT_STATE_CACHE.put(entry.getKey(), entry.getValue());
            }


        } finally {
            CONTRACT_STATE_RW_LOCK.writeLock().unlock();
        }


    }
//===================================================================

    public AccountState getAccountState(Keyable address) {
        return ACCOUNT_STATE_CACHE.get(address);
    }

    public void putAccountState(Keyable address, AccountState accountState) {
        ACCOUNT_STATE_CACHE.put(address, accountState);
    }

    public void updateAccountStates(Map<Keyable, AccountState> addr2AccountWriteCache) {

        if (addr2AccountWriteCache.size() >= ACCOUNT_STATE_CACHE_COUNT) {
            ACCOUNT_STATE_CACHE.clear();
            //CONTRACT_CODE_CACHE.putAll();
        } else if (addr2AccountWriteCache.size() + ACCOUNT_STATE_CACHE.size() <= ACCOUNT_STATE_CACHE_COUNT) {


        } else {
            long evictSize = addr2AccountWriteCache.size() + ACCOUNT_STATE_CACHE.size() - ACCOUNT_STATE_CACHE_COUNT;
            Iterator iter = ACCOUNT_STATE_CACHE.entrySet().iterator();
            while (evictSize > 0) {
                iter.next();
                iter.remove();
                evictSize--;
            }

        }

        for (Map.Entry<Keyable, AccountState> entry: addr2AccountWriteCache.entrySet()) {
            ACCOUNT_STATE_CACHE.put(entry.getKey(), entry.getValue());
        }
    }


//===================================================================

    public ContractCode getContractCode(Keyable codeKey) {
        return CONTRACT_CODE_CACHE.get(codeKey);
    }

    public void putContractCode(Keyable codeKey, ContractCode code) {
        CONTRACT_CODE_CACHE.put(codeKey, code);
    }

    public void updateContractCodes(Map<Keyable, ContractCode> hash2CodeWriteCache) {

        if (hash2CodeWriteCache.size() >= CONTRACT_CODE_CACHE_COUNT) {
            CONTRACT_CODE_CACHE.clear();
            //CONTRACT_CODE_CACHE.putAll();
        } else if (hash2CodeWriteCache.size() + CONTRACT_CODE_CACHE.size() <= CONTRACT_CODE_CACHE_COUNT) {


        } else {
            long evictSize = hash2CodeWriteCache.size() + CONTRACT_CODE_CACHE.size() - CONTRACT_CODE_CACHE_COUNT;
            Iterator iter = CONTRACT_CODE_CACHE.entrySet().iterator();
            while (evictSize > 0) {
                iter.next();
                iter.remove();
                evictSize--;
            }

        }

        for (Map.Entry<Keyable, ContractCode> entry: hash2CodeWriteCache.entrySet()) {
            CONTRACT_CODE_CACHE.put(entry.getKey(), entry.getValue());
        }
    }

//===================================================================

    public void clearAll() {

        try {
            CONTRACT_STATE_RW_LOCK.writeLock().lock();
            CONTRACT_STATE_CACHE.clear();
        } finally {
            CONTRACT_STATE_RW_LOCK.writeLock().unlock();
        }

        ACCOUNT_STATE_CACHE.clear();
        CONTRACT_CODE_CACHE.clear();
    }
}
