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
package com.thanos.chain.contract.eth.evm.program.invoke;


import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.storage.db.Repository;
import com.thanos.chain.contract.eth.evm.DataWord;
import com.thanos.chain.contract.eth.evm.program.Program;
import com.thanos.common.utils.ByteUtil;
import com.thanos.chain.ledger.model.Block;

import java.math.BigInteger;

import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;


/**
 * @author Roman Mandeleil
 * @since 08.06.2014
 */
public class ProgramInvokeFactoryImpl implements ProgramInvokeFactory {

    // Invocation by the wire tx
    @Override
    public ProgramInvoke createProgramInvoke(EthTransaction tx, Block block, Repository repository) {


        /***         ORIGIN op       ***/
        // YP: This is the sender of original transaction; it is never a contract.
        byte[] origin = tx.getSender();



        /***         ADDRESS op       ***/
        // YP: Get address of currently executing account.
        byte[] address = null;

        if (tx.isContractCreation()) {
            byte[] nonce = repository.getNonce(origin).toByteArray();
            address = HashUtil.calcNewAddr(origin, nonce);
        } else {
            address = tx.getReceiveAddress();
        }




        /***         CALLER op       ***/
        // YP: This is the address of the account that is directly responsible for this execution.
        byte[] caller = tx.getSender();

        /***         BALANCE op       ***/
        byte[] balance = BigInteger.ZERO.toByteArray();

        /***         GASPRICE op       ***/
        byte[] gasPrice = tx.getGasPrice();

        /*** GAS op ***/
        byte[] gas = tx.getGasLimit();

        /***        CALLVALUE op      ***/
        byte[] callValue = nullToEmpty(tx.getValue());

        /***     CALLDATALOAD  op   ***/
        /***     CALLDATACOPY  op   ***/
        /***     CALLDATASIZE  op   ***/
        byte[] data = tx.isContractCreation() ? ByteUtil.EMPTY_BYTE_ARRAY : nullToEmpty(tx.getData());

        /***    PREVHASH  op  ***/
        byte[] lastHash = block.getPreEventId();

        /***   COINBASE  op ***/
        byte[] coinbase = block.getCoinbase();

        /*** TIMESTAMP  op  ***/
        long timestamp = block.getTimestamp();

        /*** NUMBER  op  ***/
        long number = block.getNumber();

        return new ProgramInvokeImpl(address, origin, caller, balance, gasPrice, gas, callValue, data,
                lastHash, coinbase, timestamp, number,
                repository);
    }

    /**
     * This invocation created for contract call contract
     */
    @Override
    public ProgramInvoke createProgramInvoke(Program program, DataWord toAddress, DataWord callerAddress,
                                             DataWord inValue, DataWord inGas,
                                             BigInteger balanceInt, byte[] dataIn,
                                             Repository repository, boolean isStaticCall, boolean byTestingSuite) {

        DataWord address = DataWord.ofQuick(toAddress.getData());
        DataWord origin = DataWord.ofQuick(program.getOriginAddress().getData());
        DataWord caller = DataWord.ofQuick(callerAddress.getData());

        DataWord balance = DataWord.of(balanceInt.toByteArray());
        DataWord gasPrice = DataWord.ofQuick(program.getGasPrice().getData());
        DataWord gas =  DataWord.ofQuick(inGas.getData());
        DataWord callValue = DataWord.ofQuick(inValue.getData());

        byte[] data = dataIn == null? null: ByteUtil.copyFrom(dataIn);
        DataWord lastHash = DataWord.ofQuick(program.getPrevHash().getData());
        DataWord coinbase = DataWord.ofQuick(program.getCoinbase().getData());
        DataWord timestamp = DataWord.ofQuick(program.getTimestamp().getData());
        DataWord number = DataWord.ofQuick(program.getNumber().getData());


        return new ProgramInvokeImpl(address, origin, caller, balance, gasPrice, gas, callValue,
                data, lastHash, coinbase, timestamp, number,
                repository, program.getCallDeep() + 1, isStaticCall, byTestingSuite);
    }
}
