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

import com.thanos.chain.ledger.model.AccountState;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.contract.eth.evm.DataWord;
import com.thanos.chain.storage.datasource.LedgerSource;

import java.math.BigInteger;
import java.util.Set;

/**
 * Repository.java description：
 *
 * @Author laiyiyu create on 2020-08-24 14:06:27
 */
public interface Repository {

    /**
     * Create a new account in the database
     *
     * @param addr of the contract
     * @return newly created account state
     */
    AccountState createAccount(byte[] addr);


    /**
     * @param addr - account to check
     * @return - true if account exist,
     *           false otherwise
     */
    boolean isExist(byte[] addr);

    /**
     * Retrieve an account
     *
     * @param addr of the account
     * @return account state as stored in the database
     */
    AccountState getAccountState(byte[] addr);

    /**
     * Deletes the account
     *
     * @param addr of the account
     */
    void delete(byte[] addr);

    /**
     * Increase the account nonce of the given account by one
     *
     * @param addr of the account
     * @return new value of the nonce
     */
    BigInteger increaseNonce(byte[] addr);

    /**
     * Sets the account nonce of the given account
     *
     * @param addr of the account
     * @param nonce new nonce
     * @return new value of the nonce
     */
    BigInteger setNonce(byte[] addr, BigInteger nonce);

    /**
     * Get current nonce of a given account
     *
     * @param addr of the account
     * @return value of the nonce
     */
    BigInteger getNonce(byte[] addr);

    /**
     * Store code associated with an account
     *
     * @param addr for the account
     * @param code that will be associated with this account
     */
    void saveCode(byte[] addr, byte[] code);

    /**
     * Retrieve the code associated with an account
     *
     * @param addr of the account
     * @return code in byte-array format
     */
    byte[] getCode(byte[] addr);

    /**
     * Retrieve the code getHash associated with an account
     *
     * @param addr of the account
     * @return code getHash
     */
    byte[] getCodeHash(byte[] addr);

    /**
     * Put a value in storage of an account at a given key
     *
     * @param addr of the account
     * @param key of the data to store
     * @param value is the data to store
     */
    void addStorageRow(byte[] addr, DataWord key, DataWord value);


    /**
     * Retrieve storage value from an account for a given key
     *
     * @param addr of the account
     * @param key associated with this value
     * @return data in the form of a <code>DataWord</code>
     */
    DataWord getStorageValue(byte[] addr, DataWord key);


    /**
     * Retrieve balance of an account
     *
     * @param addr of the account
     * @return balance of the account as a <code>BigInteger</code> value
     */
    BigInteger getBalance(byte[] addr);

    /**
     * Add value to the balance of an account
     *
     * @param addr of the account
     * @param value to be added
     * @return new balance of the account
     */
    BigInteger addBalance(byte[] addr, BigInteger value);

    /**
     * @return Returns set of all the account addresses
     */
    Set<byte[]> getAccountsKeys();


    byte[] getBlockHashByNumber(long blockNumber);
    /**
     * Save a snapshot and start tracking future changes
     *
     * @return the tracker rootRepository
     */
    Repository startTracking();

    void flush();

    public void persist(Block block);


    /**
     * Store all the temporary changes made
     * to the rootRepository in the actual database
     */
    void commit();

    /**
     * Undo all the changes made so far
     * to a snapshot of the rootRepository
     */
    void rollback();

    /**
     * Close the database
     */
    void close();

    /**
     * Reset
     */
    void reset();


    byte[] getRoot();

    Repository getSnapshotTo(LedgerSource ledgerSource);

    /**
     * Clones rootRepository so changes made to this rootRepository are
     * not reflected in its clone. 
     */
    Repository clone();
}
