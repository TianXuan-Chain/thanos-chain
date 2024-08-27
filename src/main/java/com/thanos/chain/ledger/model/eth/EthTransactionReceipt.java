
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
package com.thanos.chain.ledger.model.eth;

import com.thanos.common.utils.*;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPElement;
import com.thanos.common.utils.rlp.RLPItem;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.chain.contract.eth.evm.LogInfo;

import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static com.thanos.common.utils.ByteUtil.toHexString;

/**
 * The ethTransaction receipt is a tuple of three items
 * comprising the ethTransaction, together with the post-ethTransaction state,
 * and the cumulative gas used in the block containing the ethTransaction receipt
 * as of immediately after the ethTransaction has happened,
 */
public class EthTransactionReceipt extends Persistable {

    private EthTransaction ethTransaction;

    private List<LogInfo> logInfoList;

    private byte[] gasUsed;
    private byte[] executionResult;
    private String error;

    public EthTransactionReceipt(byte[] rlp) {
        // 这里有一个语法点需要注意，在super(rlp) 没有调用完成前
        // 当前子类的实例属性是不会初始化默认值，例如，即使实例属性的
        // 默认值为 logInfoList = new ArrayList(); 当在super(rlp)函数中，
        // 会用到rlpDecoded() 的实现方法，但logInfoList 属于未初始化状态，即为null，
        // 如果直接使用，会发生npe.
        super(rlp);
    }

    public EthTransactionReceipt(List<LogInfo> logInfoList, EthTransaction ethTransaction, long gasUsed, byte[] executionResult, String error) {
        super(null);
        this.logInfoList = logInfoList == null? new ArrayList<>(): logInfoList;
        this.ethTransaction = ethTransaction;
        this.gasUsed = BigInteger.valueOf(gasUsed).toByteArray();
        this.executionResult = executionResult;
        this.error = error == null? "": error;
        this.rlpEncoded = rlpEncoded();
        //this.recepitHash = HashUtil.sha3Light(rlpEncoded);
    }

    public byte[] getGasUsed() {
        return gasUsed;
    }

    public byte[] getExecutionResult() {
        return executionResult;
    }

    public List<LogInfo> getLogInfoList() {
        return logInfoList;
    }

    public boolean isValid() {
        return ByteUtil.byteArrayToLong(gasUsed) > 0;
    }

    public boolean isSuccessful() {
        return error.isEmpty();
    }

    public byte[] getHashContent() {

        int count = 0;
        for (LogInfo logInfo: logInfoList)
        {
            count += logInfo.getEncoded().length;
        }

        // Create new array and copy all array contents
        byte[] logsBytes = new byte[count];
        int start = 0;
        for (LogInfo logInfo: logInfoList) {
            System.arraycopy(logInfo.getEncoded(), 0, logsBytes, start, logInfo.getEncoded().length);
            start += logInfo.getEncoded().length;
        }
        return ByteUtil.merge(logsBytes, gasUsed, executionResult, error.getBytes());
    }

    public String getError() {
        return error;
    }

    public boolean hashError() {
        return !StringUtils.isEmpty(error);
    }

    public void setEthTransaction(EthTransaction ethTransaction) {
        this.ethTransaction = ethTransaction;
    }

    public EthTransaction getEthTransaction() {
        if (ethTransaction == null) throw new NullPointerException("EthTransaction is not initialized. Use TransactionInfo and BlockStore to setup EthTransaction instance");
        return ethTransaction;
    }


    @Override
    protected byte[] rlpEncoded() {
        final byte[] logInfoListRLP;
        if (logInfoList != null) {
            byte[][] logInfoListE = new byte[logInfoList.size()][];

            int i = 0;
            for (LogInfo logInfo : logInfoList) {
                logInfoListE[i] = logInfo.getEncoded();
                ++i;
            }
            logInfoListRLP = RLP.encodeList(logInfoListE);
        } else {
            logInfoListRLP = RLP.encodeList();
        }

        return RLP.encodeList(logInfoListRLP,
                        ethTransaction.getEncoded(),
                        RLP.encodeElement(gasUsed), RLP.encodeElement(executionResult),
                        RLP.encodeElement(error.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    protected void rlpDecoded() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList receipt = (RLPList) params.get(0);

        RLPList logs = (RLPList) receipt.get(0);
        RLPList transactionRLP = (RLPList) receipt.get(1);
        RLPItem gasUsedRLP = (RLPItem) receipt.get(2);
        RLPItem result = (RLPItem) receipt.get(3);

        if (logInfoList == null) {
            logInfoList = new ArrayList<>(4);
        }

        for (RLPElement log : logs) {
            LogInfo logInfo = new LogInfo(log.getRLPData());
            logInfoList.add(logInfo);
        }
        ethTransaction = new EthTransaction(transactionRLP.getRLPData());
        gasUsed = gasUsedRLP.getRLPData();
        executionResult = (executionResult = result.getRLPData()) == null ? EMPTY_BYTE_ARRAY : executionResult;

        if (receipt.size() > 4) {
            byte[] errBytes = receipt.get(4).getRLPData();
            error = errBytes != null ? new String(errBytes, StandardCharsets.UTF_8) : "";
        }

        //this.recepitHash = HashUtil.sha3(this.rlpEncoded);
    }

    @Override
    public String toString() {

        return "EthTransactionReceipt[" +
                "\n  , hash=" + toHexString(this.ethTransaction.getHash()) +
                "\n  , gasUsed=" + toHexString(gasUsed) +
                "\n  , error=" + error +
                "\n  , executionResult=" + toHexString(executionResult) +
                "\n  , logs=" + logInfoList +
                ']';
    }

//    public long estimateMemSize() {
//        return MemEstimator.estimateSize(this);
//    }
//
//    public static final MemSizeEstimator<EthTransactionReceipt> MemEstimator = receipt -> {
//        if (receipt == null) {
//            return 0;
//        }
//        long logSize = receipt.logInfoList.stream().mapToLong(LogInfo.MemEstimator::estimateSize).sum() + 16;
//        return (receipt.ethTransaction == null ? 0 : EthTransaction.MemEstimator.estimateSize(receipt.ethTransaction)) +
//                (receipt.gasUsed == EMPTY_BYTE_ARRAY ? 0 : ByteArrayEstimator.estimateSize(receipt.gasUsed)) +
//                (receipt.executionResult == EMPTY_BYTE_ARRAY ? 0 : ByteArrayEstimator.estimateSize(receipt.executionResult)) +
//                ByteArrayEstimator.estimateSize(receipt.rlpEncoded) +
//                receipt.error.getBytes().length + 40 +
//                logSize;
//    };
}

