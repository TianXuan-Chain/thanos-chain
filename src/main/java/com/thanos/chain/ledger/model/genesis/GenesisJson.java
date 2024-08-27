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
package com.thanos.chain.ledger.model.genesis;


import java.util.List;
import java.util.Map;

public class GenesisJson {

    short maxShardingNum;
    short shardingNum;
    boolean useSystemContract;
    String mixhash;
    String coinbase;
    String timestamp;
    String parentHash;
    String extraData;
    String gasLimit;
    String nonce;
    String difficulty;
    String  voteThreshold;
    long startEventNumber;

    Map<String, AllocatedAccount> alloc;

    Map<String, ValidatorInfo> validatorVerifiers;

    List<String> committeeAddrs;

    List<String> operationsStaffAddrs;

    GenesisConfig config;

    public GenesisJson() {
    }

    public short getMaxShardingNum() {
        return maxShardingNum;
    }

    public void setMaxShardingNum(short maxShardingNum) {
        this.maxShardingNum = maxShardingNum;
    }

    public short getShardingNum() {
        return shardingNum;
    }

    public void setShardingNum(short shardingNum) {
        this.shardingNum = shardingNum;
    }

    public boolean isUseSystemContract() {
        return useSystemContract;
    }

    public void setUseSystemContract(boolean useSystemContract) {
        this.useSystemContract = useSystemContract;
    }

    public String getMixhash() {
        return mixhash;
    }

    public void setMixhash(String mixhash) {
        this.mixhash = mixhash;
    }

    public String getCoinbase() {
        return coinbase;
    }

    public void setCoinbase(String coinbase) {
        this.coinbase = coinbase;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getParentHash() {
        return parentHash;
    }

    public void setParentHash(String parentHash) {
        this.parentHash = parentHash;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public String getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(String gasLimit) {
        this.gasLimit = gasLimit;
    }

    public String getVoteThreshold() {
        return voteThreshold;
    }

    public void setVoteThreshold(String voteThreshold) {
        this.voteThreshold = voteThreshold;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Map<String, AllocatedAccount> getAlloc() {
        return alloc;
    }

    public void setAlloc(Map<String, AllocatedAccount> alloc) {
        this.alloc = alloc;
    }

    public GenesisConfig getConfig() {
        return config;
    }

    public void setConfig(GenesisConfig config) {
        this.config = config;
    }

    public void setStartEventNumber(long startEventNumber) {
        this.startEventNumber = startEventNumber;
    }

    public long getStartEventNumber() {
        return startEventNumber;
    }

    public Map<String, ValidatorInfo> getValidatorVerifiers() {
        return validatorVerifiers;
    }

    public void setValidatorVerifiers(Map<String, ValidatorInfo> validatorVerifiers) {
        this.validatorVerifiers = validatorVerifiers;
    }

    public List<String> getCommitteeAddrs() {
        return committeeAddrs;
    }

    public void setCommitteeAddrs(List<String> committeeAddrs) {
        this.committeeAddrs = committeeAddrs;
    }

    public List<String> getOperationsStaffAddrs() {
        return operationsStaffAddrs;
    }

    public void setOperationsStaffAddrs(List<String> operationsStaffAddrs) {
        this.operationsStaffAddrs = operationsStaffAddrs;
    }

    public static class AllocatedAccount {

        public Map<String, String> storage;
        public String nonce;
        public String code;
        public String balance;

    }

    public static class ValidatorInfo {

        public long consensusVotingPower;

        public int shardingNum;

        public String name;

        public String agency;

        public String caHash;
    }
}
