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
package com.thanos.chain.ledger.model;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.storage.db.Repository;
import com.thanos.common.utils.ByteArrayWrapper;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static com.thanos.common.utils.HashUtil.EMPTY_DATA_HASH;

/**
 * The genesis block is the first block in the chain and has fixed values according to
 * the protocol specification. The genesis block is 13 items, and is specified thus:
 * <p>
 * ( zerohash_256 , SHA3 RLP () , zerohash_160 , stateRoot, 0, 2^22 , 0, 0, 1000000, 0, 0, 0, SHA3 (42) , (), () )
 * <p>
 * - Where zerohash_256 refers to the parent getHash, a 256-bit getHash which is all zeroes;
 * - zerohash_160 refers to the coinbase address, a 160-bit getHash which is all zeroes;
 * - 2^22 refers to the difficulty;
 * - 0 refers to the timestamp (the Unix epoch);
 * - the transaction trie root and extradata are both 0, being equivalent to the empty byte array.
 * - The sequences of both uncles and transactions are empty and represented by ().
 * - SHA3 (42) refers to the SHA3 getHash of a byte array of length one whose first and only byte is of value 42.
 * - SHA3 RLP () value refers to the getHash of the uncle lists in RLP, both empty lists.
 * <p>
 * See Yellow Paper: http://www.gavwood.com/Paper.pdf (Appendix I. Genesis Block)
 */
public class Genesis extends Block {

    private Map<ByteArrayWrapper, PremineAccount> premine = new HashMap<>();

    public  static byte[] ZERO_HASH_2048 = new byte[256];
    public static byte[] DIFFICULTY = BigInteger.valueOf(2).pow(17).toByteArray();
    public static long NUMBER = 0;

    private static Block instance;

    private short maxShardingNum;
    private short shardingNum;

    public Genesis(byte[] coinbase, long number,
                   long timestamp, short maxShardingNum, short shardingNum){
        super(EMPTY_DATA_HASH, EMPTY_DATA_HASH, coinbase, 0,
                number, timestamp, HashUtil.EMPTY_TRIE_HASH, HashUtil.EMPTY_TRIE_HASH,new EthTransaction[0]);
        this.maxShardingNum = maxShardingNum;
        this.shardingNum = shardingNum;
    }

    public static Block getInstance() {
        return SystemConfig.getDefault().getGenesis();
    }

    public static Genesis getInstance(SystemConfig config) {
        return config.getGenesis();
    }

    public short getMaxShardingNum() {
        return maxShardingNum;
    }

    public short getShardingNum() {
        return shardingNum;
    }

    public Map<ByteArrayWrapper, PremineAccount> getPremine() {
        return premine;
    }

    public void setPremine(Map<ByteArrayWrapper, PremineAccount> premine) {
        this.premine = premine;
    }

    public void addPremine(ByteArrayWrapper address, AccountState accountState) {
        premine.put(address, new PremineAccount(accountState));
    }

    public Block asBlock() {
        return new Block(super.getEventId(), super.getPreEventId(), super.getCoinbase(), 1, super.getNumber(), super.getTimestamp(), super.getStateRoot(), super.getReceiptsRoot(), super.getTransactionsList());
    }

    public static void populateRepository(Repository repository, Genesis genesis) {
        for (ByteArrayWrapper key : genesis.getPremine().keySet()) {
            final Genesis.PremineAccount premineAccount = genesis.getPremine().get(key);
            final AccountState accountState = premineAccount.accountState;

            repository.createAccount(key.getData());
            repository.setNonce(key.getData(), accountState.getNonce());
            repository.addBalance(key.getData(), accountState.getBalance());
            if (premineAccount.code != null) {
                repository.saveCode(key.getData(), premineAccount.code);
            }
        }
    }

    /**
     * Used to keep addition fields.
     */
    public static class PremineAccount {

        public byte[] code;

        public AccountState accountState;

        public byte[] getStateRoot() {
            return accountState.getStateRoot();
        }

        public PremineAccount(AccountState accountState) {
            this.accountState = accountState;
        }

        public PremineAccount() {
        }
    }
}
