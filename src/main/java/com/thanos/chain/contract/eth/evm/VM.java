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
package com.thanos.chain.contract.eth.evm;

import com.thanos.chain.config.ConfigResourceUtil;
import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.contract.eth.evm.hook.VMHook;
import com.thanos.chain.contract.eth.evm.program.Program;
import com.thanos.chain.contract.eth.evm.program.Stack;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.thanos.chain.contract.eth.evm.OpCode.CALL;
import static com.thanos.chain.contract.eth.evm.OpCode.PUSH1;
import static com.thanos.chain.contract.eth.evm.OpCode.REVERT;
import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static com.thanos.common.utils.HashUtil.sha3;

/**
 * The Ethereum Virtual Machine (EVM) is responsible for initialization
 * and executing a transaction on a contract.
 * <p>
 * It is a quasi-Turing-complete machine; the quasi qualification
 * comes from the fact that the computation is intrinsically bounded
 * through a parameter, gas, which limits the total amount of computation done.
 * <p>
 * The EVM is a simple stack-based architecture. The word size of the machine
 * (and thus size of stack item) is 256-bit. This was chosen to facilitate
 * the SHA3-256 getHash scheme and  elliptic-curve computations. The memory model
 * is a simple word-addressed byte array. The stack has an unlimited size.
 * The machine also has an independent storage model; this is similar in concept
 * to the memory but rather than a byte array, it is a word-addressable word array.
 * <p>
 * Unlike memory, which is volatile, storage is non volatile and is
 * maintained as part of the system state. All locations in both storage
 * and memory are well-defined initially as zero.
 * <p>
 * The machine does not follow the standard von Neumann architecture.
 * Rather than storing program code in generally-accessible memory or storage,
 * it is stored separately in a virtual ROM interactable only though
 * a specialised instruction.
 * <p>
 * The machine can have exceptional execution for several reasons,
 * including stack underflows and invalid instructions. These unambiguously
 * and validly result in immediate halting of the machine with all state changes
 * left intact. The one piece of exceptional execution that does not leave
 * state changes intact is the out-of-gas (OOG) exception.
 * <p>
 * Here, the machine halts immediately and reports the issue to
 * the execution agent (either the transaction processor or, recursively,
 * the spawning execution environment) and which will deal with it separately.
 *
 * @author Roman Mandeleil
 * @since 01.06.2014
 */
public class VM {

    private static final Logger logger = LoggerFactory.getLogger("VM");
    private static final Logger dumpLogger = LoggerFactory.getLogger("dump");
    private static BigInteger _32_ = BigInteger.valueOf(32);

    /* Keeps track of the number of steps performed in this VM */
    private int vmCounter = 0;

    private boolean vmTrace;

    private SystemConfig config;

    // deprecated field that holds VM hook. Will be removed in the future releases.
    private static VMHook deprecatedHook = VMHook.EMPTY;
    private boolean hasHooks;
    private VMHook[] hooks;

    public VM() {
        this(ConfigResourceUtil.loadSystemConfig(), VMHook.EMPTY);
    }

    public VM(SystemConfig config, VMHook hook) {
        this.config = config;
        this.vmTrace = config.vmTrace();
        this.hooks = Stream.of(deprecatedHook, hook)
                .filter(h -> !h.isEmpty())
                .toArray(VMHook[]::new);
        this.hasHooks = this.hooks.length > 0;
    }

    private void onHookEvent(Consumer<VMHook> consumer) {
        for (VMHook hook : this.hooks) {
            consumer.accept(hook);
        }
    }

    public void step(Program program) {
        try {

            OpCode op = OpCode.code(program.getCurrentOp());
            if (op == null) {
                throw Program.Exception.invalidOpCode(program.getCurrentOp());
            }

            //validateOp(op, program);

            //program.verifyStackSize(op.require());
            //program.verifyStackOverflow(op.require(), op.ret()); //Check not exceeding stack limits

            Stack stack = program.stack;
            long gasCost = op.getTier().asInt();

            program.spendGas(gasCost, op.name());

            // Execute operation
            switch (op) {
                /**
                 * Stop and Arithmetic Operations
                 */
                case STOP: {
                    program.setHReturn(EMPTY_BYTE_ARRAY);
                    program.stop();
                }
                break;
                case ADD: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord addResult = word1.add(word2);
                    program.stackPush(addResult);
                    program.step();
                }
                break;
                case MUL: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord mulResult = word1.mul(word2);
                    program.stackPush(mulResult);
                    program.step();
                }
                break;
                case SUB: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord subResult = word1.sub(word2);
                    program.stackPush(subResult);
                    program.step();
                }
                break;
                case DIV: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord divResult = word1.div(word2);
                    program.stackPush(divResult);
                    program.step();
                }
                break;
                case SDIV: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord sDivResult = word1.sDiv(word2);
                    program.stackPush(sDivResult);
                    program.step();
                }
                break;
                case MOD: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord modResult = word1.mod(word2);
                    program.stackPush(modResult);
                    program.step();
                }
                break;
                case SMOD: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord sModResult = word1.sMod(word2);
                    program.stackPush(sModResult);
                    program.step();
                }
                break;
                case EXP: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord expResult = word1.exp(word2);
                    program.stackPush(expResult);
                    program.step();
                }
                break;
                case SIGNEXTEND: {
                    DataWord word1 = program.stack.pop();
                    BigInteger k = word1.value();
                    if (k.compareTo(_32_) < 0) {
                        DataWord word2 = program.stack.pop();
                        DataWord extendResult = word2.signExtend(k.byteValue());
                        program.stackPush(extendResult);
                    }
                    program.step();
                }
                break;
                case NOT: {
                    DataWord word1 = program.stack.pop();
                    DataWord bnotWord = word1.bnot();
                    program.stackPush(bnotWord);
                    program.step();
                }
                break;
                case LT: {
                    // TODO: can be improved by not using BigInteger
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    if (word1.value().compareTo(word2.value()) == -1) {
                        program.stackPush(DataWord.ONE);
                    } else {
                        program.stackPush(DataWord.ZERO);
                    }
                    program.step();
                }
                break;
                case SLT: {
                    // TODO: can be improved by not using BigInteger
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    if (word1.sValue().compareTo(word2.sValue()) == -1) {
                        program.stackPush(DataWord.ONE);
                    } else {
                        program.stackPush(DataWord.ZERO);
                    }
                    program.step();
                }
                break;
                case SGT: {
                    // TODO: can be improved by not using BigInteger
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    if (word1.sValue().compareTo(word2.sValue()) == 1) {
                        program.stackPush(DataWord.ONE);
                    } else {
                        program.stackPush(DataWord.ZERO);
                    }
                    program.step();
                }
                break;
                case GT: {
                    // TODO: can be improved by not using BigInteger
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    if (word1.value().compareTo(word2.value()) == 1) {
                        program.stackPush(DataWord.ONE);
                    } else {
                        program.stackPush(DataWord.ZERO);
                    }
                    program.step();
                }
                break;
                case EQ: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord xorResult = word1.xor(word2);
                    if (xorResult.isZero()) {
                        program.stackPush(DataWord.ONE);
                    } else {
                        program.stackPush(DataWord.ZERO);
                    }
                    program.step();
                }
                break;
                case ISZERO: {
                    DataWord word1 = program.stack.pop();
                    if (word1.isZero()) {
                        program.stackPush(DataWord.ONE);
                    } else {
                        program.stackPush(DataWord.ZERO);
                    }

                    program.step();
                }
                break;

                /**
                 * Bitwise Logic Operations
                 */
                case AND: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord andResult = word1.and(word2);
                    program.stackPush(andResult);
                    program.step();
                }
                break;
                case OR: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord orResult = word1.or(word2);
                    program.stackPush(orResult);
                    program.step();
                }
                break;
                case XOR: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord xorResult = word1.xor(word2);
                    program.stackPush(xorResult);
                    program.step();
                }
                break;
                case BYTE: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    final DataWord result;
                    if (word1.value().compareTo(_32_) == -1) {
                        byte tmp = word2.getData()[word1.intValue()];
                        result = DataWord.of(tmp);
                    } else {
                        result = DataWord.ZERO;
                    }
                    program.stackPush(result);
                    program.step();
                }
                break;
                case SHL: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    final DataWord result = word2.shiftLeft(word1);
                    program.stackPush(result);
                    program.step();
                }
                break;
                case SHR: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    final DataWord result = word2.shiftRight(word1);
                    program.stackPush(result);
                    program.step();
                }
                break;
                case SAR: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    final DataWord result = word2.shiftRightSigned(word1);
                    program.stackPush(result);
                    program.step();
                }
                break;
                case ADDMOD: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord word3 = program.stack.pop();
                    DataWord addmodResult = word1.addmod(word2, word3);
                    program.stackPush(addmodResult);
                    program.step();
                }
                break;
                case MULMOD: {
                    DataWord word1 = program.stack.pop();
                    DataWord word2 = program.stack.pop();
                    DataWord word3 = program.stack.pop();
                    DataWord mulmodResult = word1.mulmod(word2, word3);
                    program.stackPush(mulmodResult);
                    program.step();
                }
                break;

                /**
                 * SHA3
                 */
                case SHA3: {
                    DataWord memOffsetData = program.stack.pop();
                    DataWord lengthData = program.stack.pop();
                    byte[] buffer = program.memoryChunk(memOffsetData.intValueSafe(), lengthData.intValueSafe());
                    byte[] encoded = sha3(buffer);
                    DataWord word = DataWord.of(encoded);
                    program.stackPush(word);
                    program.step();
                }
                break;

                /**
                 * Environmental Information
                 */
                case ADDRESS: {
                    DataWord address = program.getOwnerAddress();
                    program.stackPush(address);
                    program.step();
                }
                break;
                case BALANCE: {
                    DataWord address = program.stack.pop();
                    DataWord balance = program.getBalance(address);
                    program.stackPush(balance);
                    program.step();
                }
                break;
                case ORIGIN: {
                    DataWord originAddress = program.getOriginAddress();
                    program.stackPush(originAddress);
                    program.step();
                }
                break;
                case CALLER: {
                    DataWord callerAddress = program.getCallerAddress();
                    program.stackPush(callerAddress);
                    program.step();
                }
                break;
                case CALLVALUE: {
                    DataWord callValue = program.getCallValue();
                    program.stackPush(callValue);
                    program.step();
                }
                break;
                case CALLDATALOAD: {
                    DataWord dataOffs = program.stack.pop();
                    DataWord value = program.getDataValue(dataOffs);
                    program.stackPush(value);
                    program.step();
                }
                break;
                case CALLDATASIZE: {
                    DataWord dataSize = program.getDataSize();
                    program.stackPush(dataSize);
                    program.step();
                }
                break;
                case CALLDATACOPY: {
                    DataWord memOffsetData = program.stack.pop();
                    DataWord dataOffsetData = program.stack.pop();
                    DataWord lengthData = program.stack.pop();

                    byte[] msgData = program.getDataCopy(dataOffsetData, lengthData);
                    program.memorySave(memOffsetData.intValueSafe(), lengthData.intValueSafe(), msgData);
                    program.step();
                }
                break;
                case RETURNDATASIZE: {
                    DataWord dataSize = program.getReturnDataBufferSize();
                    program.stackPush(dataSize);
                    program.step();
                }
                break;
                case RETURNDATACOPY: {
                    DataWord memOffsetData = program.stack.pop();
                    DataWord dataOffsetData = program.stack.pop();
                    DataWord lengthData = program.stack.pop();
                    byte[] msgData = program.getReturnDataBufferData(dataOffsetData, lengthData);

                    if (msgData == null) {
                        throw new Program.ReturnDataCopyIllegalBoundsException(dataOffsetData, lengthData, program.getReturnDataBufferSize().longValueSafe());
                    }
                    program.memorySave(memOffsetData.intValueSafe(), lengthData.intValueSafe(), msgData);
                    program.step();
                }
                break;
                case CODESIZE:
                case EXTCODESIZE: {

                    int length;
                    if (op == OpCode.CODESIZE)
                        length = program.getCode().length;
                    else {
                        DataWord address = program.stack.pop();
                        length = program.getCodeAt(address).length;
                    }
                    DataWord codeLength = DataWord.of(length);
                    program.stackPush(codeLength);
                    program.step();
                }
                break;
                case CODECOPY:
                case EXTCODECOPY: {

                    byte[] fullCode = EMPTY_BYTE_ARRAY;
                    if (op == OpCode.CODECOPY)
                        fullCode = program.getCode();

                    if (op == OpCode.EXTCODECOPY) {
                        DataWord address = program.stack.pop();
                        fullCode = program.getCodeAt(address);
                    }

                    int memOffset = program.stack.pop().intValueSafe();
                    int codeOffset = program.stack.pop().intValueSafe();
                    int lengthData = program.stack.pop().intValueSafe();

                    int sizeToBeCopied =
                            (long) codeOffset + lengthData > fullCode.length ?
                                    (fullCode.length < codeOffset ? 0 : fullCode.length - codeOffset)
                                    : lengthData;

                    byte[] codeCopy = new byte[lengthData];

                    if (codeOffset < fullCode.length)
                        System.arraycopy(fullCode, codeOffset, codeCopy, 0, sizeToBeCopied);

                    program.memorySave(memOffset, lengthData, codeCopy);
                    program.step();
                }
                break;
                case EXTCODEHASH: {
                    DataWord address = program.stack.pop();
                    byte[] codeHash = program.getCodeHashAt(address);
                    program.stackPush(codeHash);
                    program.step();
                }
                break;
                case GASPRICE: {
                    DataWord gasPrice = program.getGasPrice();
                    program.stackPush(gasPrice);
                    program.step();
                }
                break;

                /**
                 * Block Information
                 */
                case BLOCKHASH: {

                    long blockIndex = program.stack.pop().longValueSafe();

                    DataWord blockHash = program.getBlockHash(blockIndex);
                    program.stackPush(blockHash);
                    program.step();
                }
                break;
                case COINBASE: {
                    DataWord coinbase = program.getCoinbase();
                    program.stackPush(coinbase);
                    program.step();
                }
                break;
                case TIMESTAMP: {
                    DataWord timestamp = program.getTimestamp();
                    program.stackPush(timestamp);
                    program.step();
                }
                break;
                case NUMBER: {
                    DataWord number = program.getNumber();
                    program.stackPush(number);
                    program.step();
                }
                break;
                case DIFFICULTY: {
                    DataWord difficulty = program.getDifficulty();
                    program.stackPush(difficulty);
                    program.step();
                }
                break;
                case GASLIMIT: {
                    DataWord gaslimit = program.getGasLimit();
                    program.stackPush(gaslimit);
                    program.step();
                }
                case CHAINID: {
                    throw Program.Exception.invalidOpCode(program.getCurrentOp());
                }

                case SELFBALANCE: {
                    //DataWord dataWord = program.stack.pop();
                    program.stackPush(DataWord.of(0));
                    program.step();
                }
                break;
                case BASEFEE: {
                    throw Program.Exception.invalidOpCode(program.getCurrentOp());
                }
                case POP: {
                    DataWord dataWord = program.stack.pop();
                    program.step();
                }
                break;
                case DUP1:
                case DUP2:
                case DUP3:
                case DUP4:
                case DUP5:
                case DUP6:
                case DUP7:
                case DUP8:
                case DUP9:
                case DUP10:
                case DUP11:
                case DUP12:
                case DUP13:
                case DUP14:
                case DUP15:
                case DUP16: {
                    int n = op.val() - OpCode.DUP1.val() + 1;
                    DataWord word_1 = stack.get(stack.size() - n);
                    program.stackPush(word_1);
                    program.step();
                }
                break;
                case SWAP1:
                case SWAP2:
                case SWAP3:
                case SWAP4:
                case SWAP5:
                case SWAP6:
                case SWAP7:
                case SWAP8:
                case SWAP9:
                case SWAP10:
                case SWAP11:
                case SWAP12:
                case SWAP13:
                case SWAP14:
                case SWAP15:
                case SWAP16: {
                    int n = op.val() - OpCode.SWAP1.val() + 2;

                    stack.swap(stack.size() - 1, stack.size() - n);
                    program.step();
                }
                break;
                case LOG0:
                case LOG1:
                case LOG2:
                case LOG3:
                case LOG4: {

                    if (program.isStaticCall()) throw new Program.StaticCallModificationException();
                    DataWord address = program.getOwnerAddress();

                    DataWord memStart = stack.pop();
                    DataWord memOffset = stack.pop();

                    int nTopics = op.val() - OpCode.LOG0.val();


                    List<DataWord> topics = new ArrayList<>();
                    for (int i = 0; i < nTopics; ++i) {
                        DataWord topic = stack.pop();
                        topics.add(topic);
                    }

                    byte[] data = program.memoryChunk(memStart.intValueSafe(), memOffset.intValueSafe());

                    LogInfo logInfo =
                            new LogInfo(address.getLast20Bytes(), topics, data);

                    program.getResult().addLogInfo(logInfo);
                    program.step();
                }
                break;
                case MLOAD: {
                    DataWord addr = program.stack.pop();
                    DataWord data = program.memoryLoad(addr);
                    program.stackPush(data);
                    program.step();
                }
                break;
                case MSTORE: {
                    DataWord addr = program.stack.pop();
                    DataWord value = program.stack.pop();
                    program.memorySave(addr, value);
                    program.step();
                }
                break;
                case MSTORE8: {
                    DataWord addr = program.stack.pop();
                    DataWord value = program.stack.pop();
                    byte[] byteVal = {value.getData()[31]};
                    program.memorySave(addr.intValueSafe(), byteVal);
                    program.step();
                }
                break;
                case SLOAD: {
                    DataWord key = program.stack.pop();
                    DataWord value = program.storageLoad(key);

                    if (value == null || value.data == null) {
                        value = key.and(DataWord.ZERO);
                    }
                    program.stackPush(value);
                    program.step();
                }
                break;
                case SSTORE: {
                    if (program.isStaticCall()) throw new Program.StaticCallModificationException();
                    DataWord addr = program.stack.pop();
                    DataWord value = program.stack.pop();
                    program.storageSave(addr, value);
                    program.step();
                }
                break;
                case JUMP: {
                    DataWord pos = program.stack.pop();
                    int nextPC = program.verifyJumpDest(pos);
                    program.setPC(nextPC);
                }
                break;
                case JUMPI: {
                    DataWord pos = program.stack.pop();
                    DataWord cond = program.stack.pop();
                    if (!cond.isZero()) {
                        int nextPC = program.verifyJumpDest(pos);
                        program.setPC(nextPC);
                    } else {
                        program.step();
                    }

                }
                break;
                case PC: {
                    int pc = program.getPC();
                    DataWord pcWord = DataWord.of(pc);
                    program.stackPush(pcWord);
                    program.step();
                }
                break;
                case MSIZE: {
                    int memSize = program.getMemSize();
                    DataWord wordMemSize = DataWord.of(memSize);
                    program.stackPush(wordMemSize);
                    program.step();
                }
                break;
                case GAS: {
                    DataWord gas = program.getGas();
                    program.stackPush(gas);
                    program.step();
                }
                break;

                case PUSH1:
                case PUSH2:
                case PUSH3:
                case PUSH4:
                case PUSH5:
                case PUSH6:
                case PUSH7:
                case PUSH8:
                case PUSH9:
                case PUSH10:
                case PUSH11:
                case PUSH12:
                case PUSH13:
                case PUSH14:
                case PUSH15:
                case PUSH16:
                case PUSH17:
                case PUSH18:
                case PUSH19:
                case PUSH20:
                case PUSH21:
                case PUSH22:
                case PUSH23:
                case PUSH24:
                case PUSH25:
                case PUSH26:
                case PUSH27:
                case PUSH28:
                case PUSH29:
                case PUSH30:
                case PUSH31:
                case PUSH32: {
                    program.step();
                    int nPush = op.val() - PUSH1.val() + 1;

                    byte[] data = program.sweep(nPush);
                    //System.out.println(vmCounter + "" +"PUSH:" + Hex.toHexString(data) + "-" + nPush);

                    program.stackPush(data);
                }
                break;
                case JUMPDEST: {
                    //System.out.println(vmCounter + "" +"JUMPDEST:");
                    program.step();
                }
                break;
                case CREATE: {
                    if (program.isStaticCall()) throw new Program.StaticCallModificationException();

                    DataWord value = program.stack.pop();
                    DataWord inOffset = program.stack.pop();
                    DataWord inSize = program.stack.pop();
                    //System.out.println(vmCounter + "" +"CREATE:" + Hex.toHexString(value.getData()) + " -" + Hex.toHexString(inOffset.getData())  + " -" + Hex.toHexString(inSize.getData()));

                    program.createContract(value, inOffset, inSize);

                    program.step();
                }
                break;
                case CREATE2: {
                    if (program.isStaticCall()) throw new Program.StaticCallModificationException();

                    DataWord value = program.stack.pop();
                    DataWord inOffset = program.stack.pop();
                    DataWord inSize = program.stack.pop();
                    DataWord salt = program.stack.pop();
                    //System.out.println(vmCounter + "" +"CREATE2:" + Hex.toHexString(value.getData()) + " -" + Hex.toHexString(inOffset.getData())  + " -" + Hex.toHexString(inSize.getData()) + " -" + Hex.toHexString(salt.getData()));

                    program.createContract2(value, inOffset, inSize, salt);

                    program.step();
                }
                break;
                case CALL:
                case CALLCODE:
                case DELEGATECALL:
                case STATICCALL: {
                    program.stack.pop(); // use adjustedCallGas instead of requested
                    DataWord codeAddress = program.stack.pop();
                    DataWord value = op.callHasValue() ?
                            program.stack.pop() : DataWord.ZERO;

                    if (program.isStaticCall() && op == CALL && !value.isZero())
                        throw new Program.StaticCallModificationException();

                    DataWord adjustedCallGas = DataWord.of(500000);
                    if (!value.isZero()) {
                        adjustedCallGas = adjustedCallGas.add(DataWord.of(2300));
                    }

                    DataWord inDataOffs = program.stack.pop();
                    DataWord inDataSize = program.stack.pop();

                    DataWord outDataOffs = program.stack.pop();
                    DataWord outDataSize = program.stack.pop();
                    program.memoryExpand(outDataOffs, outDataSize);
                    //System.out.println(vmCounter + "" +"CALL:" + Hex.toHexString(codeAddress.getData()) + " -" + Hex.toHexString(value.getData())  + " -" + Hex.toHexString(inDataOffs.getData()) + " -" + Hex.toHexString(inDataSize.getData()) + " -" + Hex.toHexString(outDataOffs.getData()) + " -" + Hex.toHexString(outDataSize.getData()));


                    MessageCall msg = new MessageCall(
                            op, adjustedCallGas, codeAddress, value, inDataOffs, inDataSize,
                            outDataOffs, outDataSize);

                    PrecompiledContracts.PrecompiledContract contract =
                            PrecompiledContracts.getContractForAddress(codeAddress);

                    if (!op.callIsStateless()) {
                        program.getResult().addTouchAccount(codeAddress.getLast20Bytes());
                    }

                    if (contract != null) {
                        program.callToPrecompiledAddress(msg, contract);
                    } else {
                        program.callToAddress(msg);
                    }

                    program.step();
                }
                break;
                case RETURN:
                case REVERT: {
                    DataWord offset = program.stack.pop();
                    DataWord size = program.stack.pop();

                    byte[] hReturn = program.memoryChunk(offset.intValueSafe(), size.intValueSafe());
                    program.setHReturn(hReturn);
                    program.step();
                    program.stop();

                    if (op == REVERT) {
                        program.getResult().setRevert();
                    }
                }
                break;
                case SUICIDE: {
                    if (program.isStaticCall()) throw new Program.StaticCallModificationException();
                    DataWord address = program.stack.pop();
                    program.suicide(address);
                    program.getResult().addTouchAccount(address.getLast20Bytes());
                    program.stop();
                }
                break;
                default:
                    break;
            }
            vmCounter++;
        } catch (RuntimeException e) {
            logger.error("vm play halted: {}", ExceptionUtils.getStackTrace(e));
            program.spendAllGas();
            program.resetFutureRefund();
            program.stop();
            throw e;
        } finally {
            program.fullTrace();
        }
    }

    public void play(Program program) {
        if (program.byTestingSuite()) return;

        try {
            if (hasHooks) {
                onHookEvent(hook -> hook.startPlay(program));
            }

            while (!program.isStopped()) {
                this.step(program);
            }

        } catch (RuntimeException e) {
            program.setRuntimeFailure(e);
        } catch (StackOverflowError soe) {
            logger.error("\n !!! StackOverflowError: update your java run command with -Xss2M (-Xss8M for tests) !!!\n", soe);
            System.exit(-1);
        } catch (Exception e) {
            logger.error("un know error! {}", ExceptionUtils.getStackTrace(e));
            System.exit(-1);
        } finally {
            program.afterPlay();
            if (hasHooks) {
                onHookEvent(hook -> hook.stopPlay(program));
            }
            clear();
        }
    }

    public void clear() {
        this.config = null;
        this.hooks = null;
    }

    /**
     * Utility to calculate new total memory size needed for an operation.
     * <br/> Basically just offset + size, unless size is 0, in which case the result is also 0.
     *
     * @param offset starting position of the memory
     * @param size   number of bytes needed
     * @return offset + size, unless size is 0. In that case memNeeded is also 0.
     */
    private static BigInteger memNeeded(DataWord offset, DataWord size) {
        return size.isZero() ? BigInteger.ZERO : offset.value().add(size.value());
    }
}
