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
package com.thanos.chain.executor;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.contract.eth.evm.LogInfo;
import com.thanos.chain.contract.eth.evm.program.invoke.ProgramInvoke;
import com.thanos.chain.ledger.model.*;
import com.thanos.chain.storage.db.Repository;
import com.thanos.chain.contract.eth.evm.DataWord;
import com.thanos.chain.contract.eth.evm.PrecompiledContracts;
import com.thanos.chain.contract.eth.evm.VM;
import com.thanos.chain.contract.eth.evm.hook.VMHook;
import com.thanos.chain.contract.eth.evm.program.Program;
import com.thanos.chain.contract.eth.evm.program.ProgramResult;
import com.thanos.chain.contract.eth.evm.program.invoke.ProgramInvokeFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import static com.thanos.common.utils.BIUtil.toBI;
import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static com.thanos.common.utils.ByteUtil.toHexString;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

/**
 * @author Roman Mandeleil
 * @since 19.12.2014
 */
public class EthTransactionExecutor {

    static final Logger logger = LoggerFactory.getLogger("execute");

    SystemConfig config;

    EthTransaction tx;

    Repository track;

    boolean readyToExecute = false;

    String execError;

    ProgramInvokeFactory programInvokeFactory;

    EthTransactionReceipt receipt;

    ProgramResult result = new ProgramResult();

    Block currentBlock;

    VM vm;

    Program program;

    PrecompiledContracts.PrecompiledContract precompiledContract;

    BigInteger m_endGas = BigInteger.ZERO;

    List<LogInfo> logs = null;

    //private ByteArraySet touchedAccounts = new ByteArraySet();

    boolean localCall = false;
    private VMHook vmHook;

    public EthTransactionExecutor(EthTransaction tx, Repository track,
                                  ProgramInvokeFactory programInvokeFactory, Block currentBlock) {

        this(tx, track, programInvokeFactory, currentBlock, VMHook.EMPTY);
    }

    public EthTransactionExecutor(EthTransaction tx, Repository track,
                                  ProgramInvokeFactory programInvokeFactory, Block currentBlock, VMHook vmHook) {

        this.tx = tx;
        //this.coinbase = coinbase;
        this.track = track;
        //this.blockStore = blockStore;
        this.programInvokeFactory = programInvokeFactory;
        this.currentBlock = currentBlock;
        this.m_endGas = toBI(tx.getGasLimit());
        this.vmHook = isNull(vmHook) ? VMHook.EMPTY : vmHook;

    }

    public EthTransactionExecutor withConfig(SystemConfig config) {
        this.config = config;
        //this.blockchainConfig = config.getBlockchainConfig().getConfigForBlock(currentBlock.getNumber());
        return this;
    }

    private void execError(String err) {
        logger.warn(err);
        execError = err;
    }

    /**
     * Do all the basic validation, if the executor
     * will be ready to run the transaction at the end
     * set readyToExecute = true
     */
    public void init() {
        //tx.verify();
        if (!tx.isValid()) {
            readyToExecute = false;
            receipt = new EthTransactionReceipt(null, tx, 0, ByteUtil.EMPTY_BYTE_ARRAY, "un valid sign!");
            return;
        }


        if (localCall) {
            readyToExecute = true;
            return;
        }
        readyToExecute = true;
    }

    public void execute() {
        if (!readyToExecute) return;

        if (tx.isContractCreation()) {
            create();
        } else {
            call();
        }
    }

    private void call() {
        if (!readyToExecute) return;

        byte[] targetAddress = tx.getReceiveAddress();
        precompiledContract = PrecompiledContracts.getContractForAddress(DataWord.of(targetAddress));

        if (precompiledContract != null) {
            long requiredGas = precompiledContract.getGasForData(tx.getData());

            BigInteger spendingGas = BigInteger.valueOf(requiredGas);

            if (!localCall && m_endGas.compareTo(spendingGas) < 0) {
                // no refund
                // no endowment
                execError("Out of Gas calling precompiled contract 0x" + toHexString(targetAddress) +
                        ", required: " + spendingGas + ", left: " + m_endGas);
                m_endGas = BigInteger.ZERO;
                return;
            } else {

                m_endGas = m_endGas.subtract(spendingGas);

                // FIXME: save return for vm trace
                Pair<Boolean, byte[]> out = precompiledContract.execute(tx.getData());

                if (!out.getLeft()) {
                    execError("Error executing precompiled contract 0x" + toHexString(targetAddress));
                    m_endGas = BigInteger.ZERO;
                    return;
                }
            }

        } else {

            byte[] code = track.getCode(targetAddress);
            if (isEmpty(code)) {
//                m_endGas = m_endGas.subtract(BigInteger.valueOf(basicTxCost));
//                result.spendGas(basicTxCost);
            } else {
                ProgramInvoke programInvoke =
                        programInvokeFactory.createProgramInvoke(tx, currentBlock, track);

                this.vm = new VM(config, vmHook);
                this.program = new Program(track.getCodeHash(targetAddress), code, programInvoke, tx, config, vmHook);
            }
        }

        //BigInteger endowment = toBI(tx.getValue());
        //transfer(cacheTrack, tx.getSender(), targetAddress, endowment);

        //touchedAccounts.add(targetAddress);
    }

    private void create() {
//        byte[] newContractAddress = tx.getContractAddress();
//
//        System.out.println("createContract.address:" + Hex.toHexString(newContractAddress));
//        System.out.println("sender.address:" + Hex.toHexString(tx.getSender())+ "-" + "nonce:" + Hex.toHexString(tx.getNonce()));
//
//        AccountState existingAddr = track.getAccountState(newContractAddress);
//        //if (existingAddr != null && existingAddr.isContractExist(blockchainConfig)) {
//        // 3.0 架构中，不允许出现 地址重复使用的情况
//        if (existingAddr != null) {
//            execError("Trying to create a contract with existing contract address: 0x" + toHexString(newContractAddress));
//            m_endGas = BigInteger.ZERO;
//            return;
//        }

        AccountState sendAccount = track.getAccountState(tx.getSender());
        if (sendAccount == null) {
            sendAccount = track.createAccount(tx.getSender());
        }





        if (isEmpty(tx.getData())) {
            // 普通账户
        } else {
            //Repository originalRepo = track;
            // 对于原以太坊来说，账户代码为空，并且nonce状态值为初始化的账户地址可以直接复用，
            // 出于mpt 架构，需要对原账户执行相应的delete 状态清空。
            // 在我们3.0架构中不允许 地址重复使用的情况，因此，可以直接去除该代码
//            if (cacheTrack.hasContractDetails(newContractAddress)) {
//                originalRepo = track.clone();
//                originalRepo.delete(newContractAddress);
//            }
            track.createAccount(HashUtil.calcNewAddr(tx.getSender(), sendAccount.getNonce().toByteArray()));
            ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(tx, currentBlock, track);

            this.vm = new VM(config, vmHook);
            this.program = new Program(tx.getData(), programInvoke, tx, config, vmHook);
        }
        //touchedAccounts.add(newContractAddress);
    }

    public void go() {
        if (!readyToExecute) return;

        try {

            if (vm != null) {

                // Charge basic cost of the transaction
                // program.spendGas(tx.transactionCost(), "TRANSACTION COST");

                //if ()
                vm.play(program);
                result = program.getResult();
                m_endGas = toBI(tx.getGasLimit()).subtract(toBI(program.getResult().getGasUsed()));

                if (tx.isContractCreation() && !result.isRevert()) {
                    int returnDataGasValue = 0;
                    if (m_endGas.compareTo(BigInteger.valueOf(returnDataGasValue)) < 0) {
                        // Not enough gas to return contract code
                        program.setRuntimeFailure(Program.Exception.notEnoughSpendingGas("No gas to return just created contract",
                                returnDataGasValue, program));
                        result = program.getResult();
                        result.setHReturn(EMPTY_BYTE_ARRAY);
                    } else if (getLength(result.getHReturn()) > Integer.MAX_VALUE) {
                        // Contract size too large
                        program.setRuntimeFailure(Program.Exception.notEnoughSpendingGas("Contract size too large: " + getLength(result.getHReturn()),
                                returnDataGasValue, program));
                        result = program.getResult();
                        result.setHReturn(EMPTY_BYTE_ARRAY);
                    } else {
                        // Contract successfully created
                        AccountState sendAccount = track.getAccountState(tx.getSender());
                        byte[] contractAddress = HashUtil.calcNewAddr(tx.getSender(), sendAccount.getNonce().toByteArray());
                        m_endGas = m_endGas.subtract(BigInteger.valueOf(returnDataGasValue));
                        track.saveCode(contractAddress, result.getHReturn());
                        result.setHReturn(contractAddress);
                        track.increaseNonce(tx.getSender());
                    }
                }

                if (result.getException() != null || result.isRevert()) {
                    result.getDeleteAccounts().clear();
                    result.getLogInfoList().clear();
                    result.resetFutureRefund();
                    rollback();

                    if (result.getException() != null) {
                        throw result.getException();
                    } else {
                        execError("REVERT opcode executed");
                    }
                } else {
                    //touchedAccounts.addAll(result.getTouchedAccounts());
                    track.commit();
                }

            } else {
                track.commit();
            }

        } catch (Throwable e) {

            // TODO: catch whatever they will throw on you !!!
//            https://github.com/ethereum/cpp-ethereum/blob/develop/libethereum/Executive.cpp#L241
            rollback();
            m_endGas = BigInteger.ZERO;
            execError(e.getMessage());
        }

        if (result != null) {
            logs = result.getLogInfoList();
            // Traverse list of suicides
            // todo: clean the state  related to the account
            for (DataWord address : result.getDeleteAccounts()) {
                track.delete(address.getLast20Bytes());
            }
        }
    }

    private void rollback() {

        track.rollback();

        // remove touched account
//        touchedAccounts.remove(
//                tx.isContractCreation() ? tx.getContractAddress() : tx.getReceiveAddress());
    }

    public void finalization() {


//        TransactionExecutionSummary.Builder summaryBuilder = TransactionExecutionSummary.builderFor(tx)
//                .gasLeftover(m_endGas)
//                .logs(result.getLogInfoList())
//                .result(result.getHReturn());

//        if (result != null) {
//            summaryBuilder
//                    .gasUsed(toBI(result.getGasUsed()))
//                    .gasRefund(toBI(0))
//                    .deletedAccounts(result.getDeleteAccounts())
//                    .internalTransactions(result.getInternalTransactions());
//
//            if (result.getException() != null) {
//                summaryBuilder.markAsFailed();
//            }
//        }

//        TransactionExecutionSummary summary = summaryBuilder.build();

//        logger.info("Pay total refund to sender: [{}], refund val: [{}]", toHexString(tx.getSender()), summary.getRefund());

        //touchedAccounts.add(coinbase);



        track.close();
        config = null;
        tx = null;
        programInvokeFactory = null;
        receipt = null;
        currentBlock = null;
        vm = null;

        if (program != null) {
            program.clear();
            program = null;
        }


        precompiledContract = null;

        m_endGas = null;
        if (logs != null) {
            logs.clear();
        }

        //private ByteArraySet touchedAccounts = new ByteArraySet();

        vmHook = null;

//        for (byte[] acctAddr : touchedAccounts) {
//            AccountState state = track.getAccountState(acctAddr);
//            if (state != null && state.isGlobalNodeEventsEmpty()) {
//                track.delete(acctAddr);
//            }
//        }


//        if (config.vmTrace() && program != null && result != null) {
//            String trace = program.getTrace()
//                    .result(result.getHReturn())
//                    .error(result.getException())
//                    .toString();


            //String txHash = toHexString(tx.getHash());
            //VMUtils.saveProgramTraceFile(config, txHash, trace);
            //listener.onVMTraceCreated(txHash, trace);
//        }
//        //return summary;
    }

    public EthTransactionReceipt getReceipt() {
        if (receipt == null) {
            receipt = new EthTransactionReceipt(getVMLogs(), tx, getGasUsed(), getResult().getHReturn(), execError);
        }
        return receipt;
    }

    public List<LogInfo> getVMLogs() {
        return logs;
    }

    public ProgramResult getResult() {
        return result;
    }

    public long getGasUsed() {
        return toBI(tx.getGasLimit()).subtract(m_endGas).longValue();
    }
}
