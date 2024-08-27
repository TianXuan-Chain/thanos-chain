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
package com.thanos.chain.contract.eth.evm.program;

import com.thanos.chain.contract.eth.evm.CallCreate;
import com.thanos.chain.contract.eth.evm.LogInfo;
import com.thanos.chain.contract.eth.evm.DataWord;
import com.thanos.common.utils.ByteArraySet;
import org.apache.commons.collections4.CollectionUtils;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * @author Roman Mandeleil
 * @since 07.06.2014
 */
public class ProgramResult {

    long gasUsed;
    private byte[] hReturn = EMPTY_BYTE_ARRAY;
    private RuntimeException exception;
    private boolean revert;

    private Set<DataWord> deleteAccounts;
    private ByteArraySet touchedAccounts = new ByteArraySet();
    private List<InternalEthTransaction> internalTransactions;
    private List<LogInfo> logInfoList;
    private long futureRefund = 0;

    /*
     * for testing runs ,
     * call/create is not executed
     * but dummy recorded
     */
    private List<CallCreate> callCreateList;

    public void spendGas(long gas) {
        gasUsed += gas;
    }

    public void setRevert() {
        this.revert = true;
    }

    public boolean isRevert() {
        return revert;
    }

    public void refundGas(long gas) {
        gasUsed -= gas;
    }

    public void setHReturn(byte[] hReturn) {
        this.hReturn = hReturn;

    }

    public byte[] getHReturn() {
        return hReturn;
    }

    public RuntimeException getException() {
        return exception;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public void setException(RuntimeException exception) {
        this.exception = exception;
    }

    public Set<DataWord> getDeleteAccounts() {
        if (deleteAccounts == null) {
            deleteAccounts = new HashSet<>();
        }
        return deleteAccounts;
    }

    public void addDeleteAccount(DataWord address) {
        getDeleteAccounts().add(address);
    }

    public void addDeleteAccounts(Set<DataWord> accounts) {
        if (!CollectionUtils.isEmpty(accounts)) {
            getDeleteAccounts().addAll(accounts);
        }
    }

    public void addTouchAccount(byte[] addr) {
        touchedAccounts.add(addr);
    }

    public Set<byte[]> getTouchedAccounts() {
        return touchedAccounts;
    }

    public void addTouchAccounts(Set<byte[]> accounts) {
        if (!CollectionUtils.isEmpty(accounts)) {
            getTouchedAccounts().addAll(accounts);
        }
    }

    public List<LogInfo> getLogInfoList() {
        if (logInfoList == null) {
            logInfoList = new ArrayList<>();
        }
        return logInfoList;
    }

    public void addLogInfo(LogInfo logInfo) {
        getLogInfoList().add(logInfo);
    }

    public void addLogInfos(List<LogInfo> logInfos) {
        if (!CollectionUtils.isEmpty(logInfos)) {
            getLogInfoList().addAll(logInfos);
        }
    }

    public List<CallCreate> getCallCreateList() {
        if (callCreateList == null) {
            callCreateList = new ArrayList<>();
        }
        return callCreateList;
    }

    public void addCallCreate(byte[] data, byte[] destination, byte[] gasLimit, byte[] value) {
        getCallCreateList().add(new CallCreate(data, destination, gasLimit, value));
    }

    public List<InternalEthTransaction> getInternalTransactions() {
        if (internalTransactions == null) {
            internalTransactions = new ArrayList<>();
        }
        return internalTransactions;
    }

    public InternalEthTransaction addInternalTransaction(byte[] parentHash, int deep, byte[] nonce, DataWord gasPrice, DataWord gasLimit,
                                                         byte[] senderAddress, byte[] receiveAddress, byte[] value, byte[] data, String note) {
        InternalEthTransaction transaction = new InternalEthTransaction(parentHash, deep, getInternalTransactions().size(), nonce, gasPrice, gasLimit, senderAddress, receiveAddress, value, data, null, note);
        getInternalTransactions().add(transaction);
        return transaction;
    }

    public void addInternalTransactions(List<InternalEthTransaction> internalTransactions) {
        getInternalTransactions().addAll(internalTransactions);
    }

//    public void rejectInternalTransactions() {
//        for (InternalEthTransaction internalTx : getInternalTransactions()) {
//        }
//    }

    public void addFutureRefund(long gasValue) {
        futureRefund += gasValue;
    }

    public long getFutureRefund() {
        return futureRefund;
    }

    public void resetFutureRefund() {
        futureRefund = 0;
    }

    public void merge(ProgramResult another) {
        addInternalTransactions(another.getInternalTransactions());
        if (another.getException() == null && !another.isRevert()) {
            addDeleteAccounts(another.getDeleteAccounts());
            addLogInfos(another.getLogInfoList());
            addFutureRefund(another.getFutureRefund());
            addTouchAccounts(another.getTouchedAccounts());
        }
    }

    public void clear() {
        hReturn = null;
        exception = null;
        if (deleteAccounts != null) {
            this.deleteAccounts.clear();
        }

        touchedAccounts.clear();

        if (internalTransactions != null) {
            internalTransactions.clear();
        }

        if (logInfoList != null) {
            logInfoList.clear();
        }
    }
    
    public static ProgramResult createEmpty() {
        ProgramResult result = new ProgramResult();
        result.setHReturn(EMPTY_BYTE_ARRAY);
        return result;
    }
}
