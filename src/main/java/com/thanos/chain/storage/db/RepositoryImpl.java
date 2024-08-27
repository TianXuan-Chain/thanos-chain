/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.thanos.chain.storage.db;

import com.thanos.chain.contract.eth.evm.ContractCode;
import com.thanos.chain.contract.eth.evm.DataWord;
import com.thanos.chain.ledger.model.AccountState;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.storage.datasource.LedgerSource;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.FastByteComparisons;
import com.thanos.common.utils.HashUtil;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import static com.thanos.common.utils.HashUtil.CRYPTO_PROVIDER;
import static com.thanos.common.utils.HashUtil.HASH_256_LIGHT_ALGORITHM_NAME;


/**
 * RepositoryImpl.java description：
 *
 * @Author laiyiyu create on 2020-08-24 14:06:27
 */
public class RepositoryImpl implements Repository {

    protected RepositoryImpl parent;

    protected LedgerSource ledgerSource;

    //protected Cache currentCache;

    protected Map<Keyable, AccountState> addr2AccountWriteCache;
    protected Map<Keyable, ContractCode> hash2CodeWriteCache;
    protected Map<Keyable, DataWord> contractStateWriteCache;

    protected final boolean root;

    protected RepositoryImpl() {
        root = false;
        init();
    }

    protected RepositoryImpl(LedgerSource ledgerSource) {
        assert ledgerSource != null;
        this.ledgerSource = ledgerSource;
        this.root = true;
        init();
    }

    protected void init() {
        addr2AccountWriteCache = new ConcurrentSkipListMap<>();
        hash2CodeWriteCache = new ConcurrentSkipListMap<>();
        contractStateWriteCache = new ConcurrentSkipListMap<>();
    }

    @Override
    public AccountState createAccount(byte[] addr) {
        AccountState state = new AccountState(BigInteger.ZERO,
                BigInteger.ZERO);
        this.addr2AccountWriteCache.put(Keyable.ofDefault(addr), state);
        return state;
    }

    @Override
    public boolean isExist(byte[] addr) {
        return getAccountState(addr) != null;
    }

    @Override
    public AccountState getAccountState(byte[] addr) {
        Keyable address = Keyable.ofDefault(addr);
        AccountState accountState = this.addr2AccountWriteCache.get(address);
        if (accountState != null) return accountState;

        if (!root) {
            return parent.getAccountState(addr);
        } else {
            return this.ledgerSource.getAccountState(address);
        }
    }

    AccountState getOrCreateAccountState(byte[] addr) {
        AccountState ret = getAccountState(addr);
        if (ret == null) {
            ret = createAccount(addr);
        }
        return ret;
    }

    @Override
    public void delete(byte[] addr) {
        this.addr2AccountWriteCache.put(Keyable.ofDefault(addr), new AccountState(null));
    }

    @Override
    public BigInteger increaseNonce(byte[] addr) {
        AccountState accountState = getOrCreateAccountState(addr);
        this.addr2AccountWriteCache.put(Keyable.ofDefault(addr), accountState.withIncrementedNonce());
        return accountState.getNonce();
    }

    @Override
    public BigInteger setNonce(byte[] addr, BigInteger nonce) {
        AccountState accountState = getOrCreateAccountState(addr);
        this.addr2AccountWriteCache.put(Keyable.ofDefault(addr), accountState.withNonce(nonce));
        return accountState.getNonce();
    }

    @Override
    public BigInteger getNonce(byte[] addr) {
        AccountState accountState = getAccountState(addr);
        return accountState == null ? BigInteger.ZERO :
                accountState.getNonce();
    }

    @Override
    public void saveCode(byte[] addr, byte[] code) {
        byte[] codeHash = HashUtil.sha3(code);
        this.hash2CodeWriteCache.put(Keyable.ofDefault(ByteUtil.merge(codeHash, addr)), new ContractCode(code));
        AccountState accountState = getOrCreateAccountState(addr);
        this.addr2AccountWriteCache.put(Keyable.ofDefault(addr), accountState.withCodeHash(codeHash));
    }

    @Override
    public byte[] getCode(byte[] addr) {
        byte[] codeHash = getCodeHash(addr);

        if (codeHash == null || FastByteComparisons.equal(codeHash, HashUtil.EMPTY_DATA_HASH)) {
            return ByteUtil.EMPTY_BYTE_ARRAY;
        }

        Keyable codeKey = Keyable.ofDefault(ByteUtil.merge(codeHash, addr));
        ContractCode contractCode = this.hash2CodeWriteCache.get(codeKey);
        if (contractCode != null) return contractCode.getData();

        if (!root) {
            return parent.getCode(addr);
        } else {
            contractCode = this.ledgerSource.getContractCode(codeKey);
            return contractCode == null ? null : contractCode.getData();
        }
    }

    @Override
    public byte[] getCodeHash(byte[] addr) {
        AccountState accountState = getAccountState(addr);
        return accountState != null ? accountState.getCodeHash() : null;
    }

    @Override
    public void addStorageRow(byte[] addr, DataWord key, DataWord value) {
        Keyable newKey = Keyable.ofDefault(ByteUtil.merge(addr, key.getData()));
        this.contractStateWriteCache.put(newKey, value.isZero() ? DataWord.buildForEmpty() : value);
    }

    @Override
    public DataWord getStorageValue(byte[] addr, DataWord key) {
        Keyable newKey = Keyable.ofDefault(ByteUtil.merge(addr, key.getData()));

        DataWord result = this.contractStateWriteCache.get(newKey);
        //this.contractStateWriteCache.put(newKey, result);
        if (result != null) return result;

        if (!root) {
            return parent.getStorageValue(addr, key);
        } else {
            return ledgerSource.getContractState(newKey);
        }
    }

    @Override
    public BigInteger getBalance(byte[] addr) {
        AccountState accountState = getAccountState(addr);
        return accountState == null ? BigInteger.ZERO : accountState.getBalance();
    }

    @Override
    public BigInteger addBalance(byte[] addr, BigInteger value) {
        AccountState accountState = getOrCreateAccountState(addr);
        this.addr2AccountWriteCache.put(Keyable.ofDefault(addr), accountState.withBalanceIncrement(value));
        return accountState.getBalance();
    }

    @Override
    public RepositoryImpl startTracking() {

        RepositoryImpl ret = new RepositoryImpl();
        ret.parent = this;
        ret.ledgerSource = this.ledgerSource;
        return ret;
    }

    @Override
    public Repository getSnapshotTo(LedgerSource ledgerSource) {
        return new RepositoryImpl(ledgerSource);
    }

    @Override
    public void commit() {
        Repository parentSync = parent;

        // skip the root Repository
        if (parentSync == null || root) return;
        //synchronized(parentSync) {

        Iterator<Map.Entry<Keyable, AccountState>> accountStateIter = this.addr2AccountWriteCache.entrySet().iterator();
        while (accountStateIter.hasNext()) {
            Map.Entry<Keyable, AccountState> entry = accountStateIter.next();
            parent.addr2AccountWriteCache.put(entry.getKey(), entry.getValue());
            accountStateIter.remove();
        }

        Iterator<Map.Entry<Keyable, ContractCode>> codeIter = this.hash2CodeWriteCache.entrySet().iterator();
        while (codeIter.hasNext()) {
            Map.Entry<Keyable, ContractCode> entry = codeIter.next();
            parent.hash2CodeWriteCache.put(entry.getKey(), entry.getValue());
            codeIter.remove();
        }

        Iterator<Map.Entry<Keyable, DataWord>> contractStateIter = this.contractStateWriteCache.entrySet().iterator();
        while (contractStateIter.hasNext()) {
            Map.Entry<Keyable, DataWord> entry = contractStateIter.next();
            parent.contractStateWriteCache.put(entry.getKey(), entry.getValue());
            //System.out.println("key->value:" + entry.getKey() + "->" + entry.getValue());
            contractStateIter.remove();
        }
    }

    @Override
    public void rollback() {
        // nothing to do, will be GCed
        if (!root) {
            this.hash2CodeWriteCache.clear();
            this.addr2AccountWriteCache.clear();
            this.contractStateWriteCache.clear();
        }
    }

    @Override
    public byte[] getRoot() {
        throw new RuntimeException("Not supported");
    }


    /**
     * As tests only implementation this hack is pretty sufficient
     */
    @Override
    public Repository clone() {
        return parent.startTracking();
    }

    @Override
    public Set<byte[]> getAccountsKeys() {
        throw new RuntimeException("Not supported");
    }

    @Override
    public byte[] getBlockHashByNumber(long blockNumber) {
        Block block = ledgerSource.getBlockByNumber(blockNumber);
        if (block != null) {
            return block.getHash();
        } else {
            return null;
        }
    }

    @Override
    public void flush() {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void persist(Block block) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void close() {
        this.parent = null;
        this.ledgerSource = null;
        this.addr2AccountWriteCache.clear();
        this.contractStateWriteCache.clear();
        this.hash2CodeWriteCache.clear();
    }

    @Override
    public void reset() {
        throw new RuntimeException("Not supported");
    }

    // state root
    public static byte[] sha3LightStates(Map<Keyable, AccountState> addr2AccountWriteCache, Map<Keyable, DataWord> contractStateWriteCache, Map<Keyable, ContractCode> hash2CodeWriteCache) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_LIGHT_ALGORITHM_NAME, CRYPTO_PROVIDER);
            for (Map.Entry<Keyable, AccountState> entry : addr2AccountWriteCache.entrySet()) {
                //absorb
                digest.update(entry.getKey().keyBytes());
                if (entry.getValue().valueBytes() != null) {
                    digest.update(entry.getValue().valueBytes());
                }
            }

            for (Map.Entry<Keyable, DataWord> entry : contractStateWriteCache.entrySet()) {
                //absorb
                digest.update(entry.getKey().keyBytes());
                if (entry.getValue().valueBytes() != null) {
                    digest.update(entry.getValue().valueBytes());
                }
            }

            for (Map.Entry<Keyable, ContractCode> entry : hash2CodeWriteCache.entrySet()) {
                //absorb
                digest.update(entry.getKey().keyBytes());
                if (entry.getValue().valueBytes() != null) {
                    digest.update(entry.getValue().valueBytes());
                }
            }

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
