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

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.thanos.chain.config.ConfigResourceUtil;
import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.utils.*;
import com.thanos.chain.contract.eth.evm.*;
import com.thanos.chain.contract.eth.evm.PrecompiledContracts.PrecompiledContract;
import com.thanos.chain.contract.eth.evm.hook.VMHook;
import com.thanos.chain.contract.eth.evm.program.invoke.ProgramInvoke;
import com.thanos.chain.contract.eth.evm.program.invoke.ProgramInvokeFactory;
import com.thanos.chain.contract.eth.evm.program.invoke.ProgramInvokeFactoryImpl;
import com.thanos.chain.contract.eth.evm.program.listener.ProgramListenerAware;
import com.thanos.chain.ledger.model.AccountState;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.storage.db.Repository;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.*;

import static com.thanos.common.utils.BIUtil.*;
import static com.thanos.common.utils.ByteUtil.toHexString;
import static java.lang.StrictMath.min;
import static java.lang.String.format;
import static java.math.BigInteger.ZERO;
import static org.apache.commons.lang3.ArrayUtils.*;

/**
 * @author Roman Mandeleil
 * @since 01.06.2014
 */
public class Program {

    private static final Logger logger = LoggerFactory.getLogger("VM");

    /**
     * This attribute defines the number of recursive calls allowed in the EVM
     * Note: For the JVM to reach this level without a StackOverflow exception,
     * ethereumj may need to be started with a JVM argument to increase
     * the stack size. For example: -Xss10m
     */
    private static final int MAX_DEPTH = 1024;

    //Max size for stack checks
    private static final int MAX_STACKSIZE = 1024;


    private static final int PRECOMPILE_CODE_CACHE_COUNT = 1000;

    private static final EvictionListener<Keyable, ProgramPrecompile>  PRECOMPILE_REMOVE_LISTENER = (key, value) -> logger.warn("{}, remove from cache!", value);

    private static ConcurrentLinkedHashMap<Keyable, ProgramPrecompile> PRECOMPILE_CODE_CACHE = new ConcurrentLinkedHashMap.Builder<Keyable, ProgramPrecompile>()
            .maximumWeightedCapacity(PRECOMPILE_CODE_CACHE_COUNT).listener(PRECOMPILE_REMOVE_LISTENER).build();


    public static ProgramPrecompile getPrecompileCode(Keyable hash) {
        return PRECOMPILE_CODE_CACHE.get(hash);
    }

    public static void putPrecompileCode(Keyable hash, ProgramPrecompile code) {
        PRECOMPILE_CODE_CACHE.put(hash, code);
    }


    private EthTransaction ethTransaction;

    private ProgramInvoke invoke;
    private ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();

//    private ProgramOutListener listener;
//    private ProgramTraceListener traceListener;
//    private ProgramStorageChangeListener storageDiffListener = new ProgramStorageChangeListener();
//    private CompositeProgramListener programListener = new CompositeProgramListener();
    //for speed to use public
    public Stack stack;
    private Memory memory;
    //private Storage storage;
    private Repository repository;
    private byte[] returnDataBuffer;

    private ProgramResult result = new ProgramResult();
    //private ProgramTrace trace;

    private byte[] codeHash;
    private byte[] ops;
    private int pc;
    private byte previouslyExecutedOp;
    private boolean stopped;
    private ByteArraySet touchedAccounts = new ByteArraySet();

    private ProgramPrecompile programPrecompile;


    private SystemConfig config;

    private VMHook vmHook;

    public Program(byte[] ops, ProgramInvoke programInvoke) {
        this(ops, programInvoke, (EthTransaction) null);
    }

    public Program(byte[] ops, ProgramInvoke programInvoke, SystemConfig config) {
        this(ops, programInvoke, null, config, VMHook.EMPTY);
    }

    public Program(byte[] ops, ProgramInvoke programInvoke, EthTransaction ethTransaction) {
        this(ops, programInvoke, ethTransaction, ConfigResourceUtil.loadSystemConfig(), VMHook.EMPTY);
    }

    public Program(byte[] ops, ProgramInvoke programInvoke, EthTransaction ethTransaction, SystemConfig config, VMHook vmHook) {
        this(null, ops, programInvoke, ethTransaction, config, vmHook);
    }

    public Program(byte[] codeHash, byte[] ops, ProgramInvoke programInvoke, EthTransaction ethTransaction, SystemConfig config, VMHook vmHook) {
        this.config = config;
        this.invoke = programInvoke;
        this.ethTransaction = ethTransaction;

        this.codeHash = codeHash == null || FastByteComparisons.equal(HashUtil.EMPTY_DATA_HASH, codeHash) ? null : codeHash;
        this.ops = nullToEmpty(ops);

        this.vmHook = vmHook;
        //this.traceListener = new ProgramTraceListener(config.vmTrace());
        //this.memory = setupProgramListener(Memory.newInstance());
        //this.memory = Memory.newInstance();
        this.memory = new Memory();
        //this.stack = setupProgramListener(new Stack());
        this.stack = new Stack();
        //this.stack = Stack.newInstance();
        //this.originalRepo = programInvoke.getOrigRepository();
        this.repository = programInvoke.getRepository();
        //this.storage = setupProgramListener(new Storage(programInvoke));
        //this.trace = new ProgramTrace(config, programInvoke);
    }

    public ProgramPrecompile getProgramPrecompile() {
        if (programPrecompile == null) {
            if (codeHash != null) {
                programPrecompile = getPrecompileCode(Keyable.ofDefault(codeHash));
            }
            if (programPrecompile == null) {
                programPrecompile = ProgramPrecompile.compile(ops);

                if (codeHash != null) {
                    putPrecompileCode(Keyable.ofDefault(codeHash), programPrecompile);
                }
            }
        }
        return programPrecompile;
    }

    public int getCallDeep() {
        return invoke.getCallDeep();
    }

    private InternalEthTransaction addInternalTx(byte[] nonce, DataWord gasLimit, byte[] senderAddress, byte[] receiveAddress,
                                                 BigInteger value, byte[] data, String note) {

        InternalEthTransaction result = null;
        if (ethTransaction != null) {
            byte[] senderNonce = isEmpty(nonce) ? getRepository().getNonce(senderAddress).toByteArray() : nonce;

            result = getResult().addInternalTransaction(ethTransaction.getHash(), getCallDeep(), senderNonce,
                    getGasPrice(), gasLimit, senderAddress, receiveAddress, value.toByteArray(), data, note);
        }

        return result;
    }

    private <T extends ProgramListenerAware> T setupProgramListener(T programListenerAware) {
//        if (programListener.isGlobalNodeEventsEmpty()) {
//            programListener.addListener(traceListener);
//            programListener.addListener(storageDiffListener);
//        }
//
//        programListenerAware.setProgramListener(programListener);

        return programListenerAware;
    }

//    public Map<DataWord, DataWord> getStorageDiff() {
//        return storageDiffListener.getDiff();
//    }

    public byte getOp(int pc) {
        return (getLength(ops) <= pc) ? 0 : ops[pc];
    }

    public byte getCurrentOp() {
        return isEmpty(ops) ? 0 : ops[pc];
    }

    /**
     * Should be set only after the OP is fully executed.
     */
    public void setPreviouslyExecutedOp(byte op) {
        this.previouslyExecutedOp = op;
    }

    /**
     * Returns the last fully executed OP.
     */
    public byte getPreviouslyExecutedOp() {
        return this.previouslyExecutedOp;
    }

    public void stackPush(byte[] data) {
        stackPush(DataWord.of(data));
    }

    public void stackPushZero() {
        stackPush(DataWord.ZERO);
    }

    public void stackPushOne() {
        DataWord stackWord = DataWord.ONE;
        stackPush(stackWord);
    }

    public void stackPush(DataWord stackWord) {
        //verifyStackOverflow(0, 1); //Sanity Check
        //for speed
        if ((stack.size() - 0 + 1) > MAX_STACKSIZE) {
            throw new StackTooLargeException("Expected: overflow " + MAX_STACKSIZE + " elements stack limit");
        }

        stack.push(stackWord);
    }

    public Stack getStack() {
        return this.stack;
    }

    public int getPC() {
        return pc;
    }

    public void setPC(DataWord pc) {
        this.setPC(pc.intValue());
    }

    public void setPC(int pc) {
        this.pc = pc;

        if (this.pc >= ops.length) {
            stop();
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    public void stop() {
        stopped = true;
    }

    public void setHReturn(byte[] buff) {
        getResult().setHReturn(buff);
    }

    public void step() {
        //setPC(pc + 1);
        this.pc++;
        if (this.pc >= ops.length) {
            stop();
        }
    }

    public byte[] sweep(int n) {

        if (pc + n > ops.length)
            stop();

        byte[] data = Arrays.copyOfRange(ops, pc, pc + n);
        pc += n;
        if (pc >= ops.length) stop();

        return data;
    }

    public DataWord stackPop() {
        return stack.pop();
    }

    /**
     * Verifies that the stack is at least <code>stackSize</code>
     *
     * @param stackSize int
     * @throws StackTooSmallException If the stack is
     *                                smaller than <code>stackSize</code>
     */
    public void verifyStackSize(int stackSize) {
        if (stack.size() < stackSize) {
            throw Program.Exception.tooSmallStack(stackSize, stack.size());
        }
    }

    public void verifyStackOverflow(int argsReqs, int returnReqs) {
        if ((stack.size() - argsReqs + returnReqs) > MAX_STACKSIZE) {
            throw new StackTooLargeException("Expected: overflow " + MAX_STACKSIZE + " elements stack limit");
        }
    }

    public int getMemSize() {
        return memory.size();
    }

    public void memorySave(DataWord addrB, DataWord value) {
        memory.write(addrB.intValue(), value.getData(), value.getData().length, false);
    }

    public void memorySaveLimited(int addr, byte[] data, int dataSize) {
        memory.write(addr, data, dataSize, true);
    }

    public void memorySave(int addr, byte[] value) {
        memory.write(addr, value, value.length, false);
    }

    public void memoryExpand(DataWord outDataOffs, DataWord outDataSize) {
        if (!outDataSize.isZero()) {
            memory.extend(outDataOffs.intValue(), outDataSize.intValue());
        }
    }

    /**
     * Allocates a piece of memory and stores value at given offset address
     *
     * @param addr      is the offset address
     * @param allocSize size of memory needed to write
     * @param value     the data to write to memory
     */
    public void memorySave(int addr, int allocSize, byte[] value) {
        memory.extendAndWrite(addr, allocSize, value);
    }


    public DataWord memoryLoad(DataWord addr) {
        return memory.readWord(addr.intValue());
    }

    public DataWord memoryLoad(int address) {
        return memory.readWord(address);
    }

    public byte[] memoryChunk(int offset, int size) {
        return memory.read(offset, size);
    }

    /**
     * Allocates extra memory in the program for
     * a specified size, calculated from a given offset
     *
     * @param offset the memory address offset
     * @param size   the number of bytes to allocate
     */
    public void allocateMemory(int offset, int size) {
        memory.extend(offset, size);
    }


    public void suicide(DataWord obtainerAddress) {

        byte[] owner = getOwnerAddress().getLast20Bytes();
        byte[] obtainer = obtainerAddress.getLast20Bytes();
        BigInteger balance = ZERO;

        addInternalTx(null, null, owner, obtainer, balance, null, "suicide");
        getResult().addDeleteAccount(this.getOwnerAddress());
    }

    public Repository getRepository() {
        return this.repository;
    }

    /**
     * Create contract for {@link OpCode#CREATE}
     * @param value         Endowment
     * @param memStart      Code memory offset
     * @param memSize       Code memory size
     */
    public void createContract(DataWord value, DataWord memStart, DataWord memSize) {
        returnDataBuffer = null; // reset return buffer right before the call

        byte[] senderAddress = this.getOwnerAddress().getLast20Bytes();
        BigInteger endowment = value.value();
        if (!verifyCall(senderAddress, endowment))
            return;

        byte[] nonce = getRepository().getNonce(senderAddress).toByteArray();
        byte[] contractAddress = HashUtil.calcNewAddr(senderAddress, nonce);

        byte[] programCode = memoryChunk(memStart.intValue(), memSize.intValue());
        createContractImpl(value, programCode, contractAddress);
    }

    /**
     * Create contract for {@link OpCode#CREATE2}
     * @param value         Endowment
     * @param memStart      Code memory offset
     * @param memSize       Code memory size
     * @param salt          Salt, used in contract address calculation
     */
    public void createContract2(DataWord value, DataWord memStart, DataWord memSize, DataWord salt) {
        returnDataBuffer = null; // reset return buffer right before the call

        byte[] senderAddress = this.getOwnerAddress().getLast20Bytes();
        BigInteger endowment = value.value();
        if (!verifyCall(senderAddress, endowment))
            return;

        byte[] programCode = memoryChunk(memStart.intValue(), memSize.intValue());
        byte[] contractAddress = HashUtil.calcSaltAddr(senderAddress, programCode, salt.getData());

        createContractImpl(value, programCode, contractAddress);
    }


    public boolean isCreateContract() {
        return this.invoke.getDataSize().isZero();
    }

    /**
     * Verifies CREATE attempt
     */
    private boolean verifyCall(byte[] senderAddress, BigInteger endowment) {
        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            return false;
        }

//        if (isNotCovers(getRepository().getBalance(senderAddress), endowment)) {
//            stackPushZero();
//            return false;
//        }

        return true;
    }

    /**
     * All stages required to create contract on provided address after initial check
     * @param value         Endowment
     * @param programCode   Contract code
     * @param newAddress    Contract address
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void createContractImpl(DataWord value, byte[] programCode, byte[] newAddress) {

        // [1] LOG, SPEND GAS
        byte[] senderAddress = this.getOwnerAddress().getLast20Bytes();
        if (logger.isDebugEnabled())
            logger.debug("creating a new contract inside contract run: [{}]", toHexString(senderAddress));

        //  actual gas subtract
        DataWord gasLimit = getGas();
        spendGas(20, "internal call");

        // [2] CREATE THE CONTRACT ADDRESS
        AccountState existingAddr = getRepository().getAccountState(newAddress);

        //到目前为止，如果指定的地址存在于 缓存或者磁盘中，但相应的合约代码为空，并且nonce为初始化状态
        //则视 当前地址可以复用，即contractAlreadyExists 为true。
        // 3.0 架构中，不允许地址重复使用
        //boolean contractAlreadyExists = existingAddr != null && existingAddr.isContractExist(blockchainConfig);
        boolean contractAlreadyExists = existingAddr != null;

//        if (byTestingSuite()) {
//            // This keeps track of the contracts created for a test
//            getResult().addCallCreate(programCode, EMPTY_BYTE_ARRAY,
//                    gasLimit.getNoLeadZeroesData(),
//                    value.getNoLeadZeroesData());
//        }

        // [3] UPDATE THE NONCE
        // (THIS STAGE IS NOT REVERTED BY ANY EXCEPTION)
        //if (!byTestingSuite()) {
        BigInteger newNonce = getRepository().increaseNonce(senderAddress);
        //System.out.println(String.format("address[%s]-nonce:[%d]", Hex.toHexString(senderAddress), newNonce.intValue()));
        //}

        Repository track = getRepository().startTracking();

        //In case of hashing collisions, check for any balance before createAccount()
        //BigInteger oldBalance = track.getBalance(newAddress);
        track.createAccount(newAddress);
        track.increaseNonce(newAddress);

        //track.addBalance(newAddress, oldBalance);

        // [4] TRANSFER THE BALANCE
        BigInteger endowment = value.value();
//        BigInteger newBalance = ZERO;
//        if (!byTestingSuite()) {
//            track.addBalance(senderAddress, endowment.negate());
//            newBalance = track.addBalance(newAddress, endowment);
//        }


        // [5] COOK THE INVOKE AND EXECUTE
        byte[] nonce = getRepository().getNonce(senderAddress).toByteArray();
        InternalEthTransaction internalTx = addInternalTx(nonce, getGasLimit(), senderAddress, null, endowment, programCode, "create");
        //Repository originalRepo = this.invoke.getOrigRepository();
        // Some TCK tests have storage only addresses (no code, zero nonce etc) - impossible situation in the real network
        // So, we should clean up it before reuse, but as tx not always goes successful, state should be correctly
        // reverted in that case too
//        if (!contractAlreadyExists && track.hasContractDetails(newAddress)) {
//            //这里针对的情况为 创建的合约账户，其代码为空，并且nonce状态值为初始化
//            //因此，通过originalRepo.delete(newAddress) 作状态清空处理，以便复用
//            //3.0架构中，可以忽略该代码
//            originalRepo = originalRepo.clone();
//            originalRepo.delete(newAddress);
//        }
        ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
                this, DataWord.of(newAddress), getOwnerAddress(), value, gasLimit,
                ZERO, null, track, false, byTestingSuite());

        ProgramResult result = ProgramResult.createEmpty();

        //todo::remove test flag in produce env
        //boolean test = true;

        if (contractAlreadyExists) {
            result.setException(new BytecodeExecutionException("Trying to create a contract with existing contract address: 0x" + toHexString(newAddress)));
        } else if (isNotEmpty(programCode)) {
            VM vm = new VM(config, vmHook);
            Program program = new Program(programCode, programInvoke, internalTx, config, vmHook);
            // reset storage if the contract with the same address already exists
            // TCK test case only - normally this is near-impossible situation in the real network
//            ContractDetails contractDetails = program.getRepository().getContractDetails(newAddress);
//            contractDetails.deleteStorage();
            vm.play(program);
            result = program.getResult();
        }

        // 4. CREATE THE CONTRACT OUT OF RETURN
        if (!result.isRevert() && result.getException() == null) {
            byte[] code = result.getHReturn();
            // hard code;
            long createCost = 20;
            long afterSpend = programInvoke.getGas().longValue() - result.getGasUsed() - createCost;
            if (afterSpend < 0) {
                result.setException(Program.Exception.notEnoughSpendingGas("No gas to return just created contract",
                        createCost, this));
            } else if (getLength(code) > Integer.MAX_VALUE) {
                result.setException(Program.Exception.notEnoughSpendingGas("Contract size too large: " + getLength(result.getHReturn()),
                        createCost, this));
            } else {
                result.spendGas(createCost);
                track.saveCode(newAddress, code);
            }
        }

        getResult().merge(result);

        if (result.getException() != null || result.isRevert()) {
            logger.warn("createContractImpl contract run halted by Exception: contract: [{}], exception: [{}], revert:[{}]",
                    toHexString(newAddress),
                    result.getException(), result.isRevert());


            track.rollback();
            stackPushZero();

            if (result.getException() != null) {
                return;
            } else {
                returnDataBuffer = result.getHReturn();
            }
        } else {
            if (!byTestingSuite())
                track.commit();

            // IN SUCCESS PUSH THE ADDRESS INTO THE STACK
            stackPush(DataWord.of(newAddress));
        }

        // 5. REFUND THE REMAIN GAS
//        long refundGas = gasLimit.longValue() - result.getGasUsed();
//        if (refundGas > 0) {
//            refundGas(refundGas, "remain gas from the internal call");
//            if (logger.isInfoEnabled()) {
//                logger.info("The remaining gas is refunded, account: [{}], gas: [{}] ",
//                        toHexString(getOwnerAddress().getLast20Bytes()),
//                        refundGas);
//            }
//        }
        touchedAccounts.add(newAddress);
        result.clear();
    }

    /**
     * That method is for internal code invocations
     * <p/>
     * - Normal calls invoke a specified contract which updates itself
     * - Stateless calls invoke code from another contract, within the context of the caller
     *
     * @param msg is the message call object
     */
    public void callToAddress(MessageCall msg) {
        returnDataBuffer = null; // reset return buffer right before the call

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            refundGas(msg.getGas().longValue(), " call deep limit reach");
            return;
        }

        byte[] data = memoryChunk(msg.getInDataOffs().intValue(), msg.getInDataSize().intValue());

        // FETCH THE SAVED STORAGE
        byte[] codeAddress = msg.getCodeAddress().getLast20Bytes();
        byte[] senderAddress = getOwnerAddress().getLast20Bytes();
        byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;

        if (logger.isDebugEnabled())
            logger.debug(msg.getType().name() + " for existing contract: address: [{}], outDataOffs: [{}], outDataSize: [{}]  ",
                    toHexString(contextAddress), msg.getOutDataOffs().longValue(), msg.getOutDataSize().longValue());

        Repository track = getRepository().startTracking();

        // 2.1 PERFORM THE VALUE (endowment) PART
        BigInteger endowment = msg.getEndowment().value();
        //BigInteger senderBalance = track.getBalance(senderAddress);
//        if (isNotCovers(senderBalance, endowment)) {
//            stackPushZero();
//            refundGas(msg.getGas().longValue(), "refund gas from message call");
//            return;
//        }


        // FETCH THE CODE
        byte[] programCode = getRepository().isExist(codeAddress) ? getRepository().getCode(codeAddress) : EMPTY_BYTE_ARRAY;


        BigInteger contextBalance = ZERO;
        if (byTestingSuite()) {
            // This keeps track of the calls created for a test
            getResult().addCallCreate(data, contextAddress,
                    msg.getGas().getNoLeadZeroesData(),
                    msg.getEndowment().getNoLeadZeroesData());
        } else {
            //track.addBalance(senderAddress, endowment.negate());
            //contextBalance = track.addBalance(contextAddress, endowment);
        }

        // CREATE CALL INTERNAL TRANSACTION
        InternalEthTransaction internalTx = addInternalTx(null, getGasLimit(), senderAddress, contextAddress, endowment, data, "call");

        ProgramResult result = null;
        if (isNotEmpty(programCode)) {
            ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
                    this, DataWord.of(contextAddress),
                    msg.getType().callIsDelegate() ? getCallerAddress() : getOwnerAddress(),
                    msg.getType().callIsDelegate() ? getCallValue() : msg.getEndowment(),
                    msg.getGas(), contextBalance, data, track,
                    msg.getType().callIsStatic() || isStaticCall(), byTestingSuite());

            VM vm = new VM(config, vmHook);
            Program program = new Program(getRepository().getCodeHash(codeAddress), programCode, programInvoke, internalTx, config, vmHook);
            vm.play(program);
            result = program.getResult();

            //getTrace().merge(program.getTrace());
            getResult().merge(result);

            if (result.getException() != null || result.isRevert()) {
                logger.info("callToAddress contract run halted by Exception: contract: [{}], exception: [{}], revert:[{}]",
                        toHexString(contextAddress),
                        result.getException(), result.isRevert());

                //result.rejectInternalTransactions();

                track.rollback();
                stackPushZero();

                if (result.getException() != null) {
                    return;
                }
            } else {
                // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
                track.commit();
                stackPushOne();
            }

            if (byTestingSuite()) {
                logger.info("Testing run, skipping storage diff listener");
            } else if (Arrays.equals(ethTransaction.getReceiveAddress(), internalTx.getReceiveAddress())) {
                //storageDiffListener.merge(program.getStorageDiff());
            }
        } else {
            // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
            track.commit();
            stackPushOne();
        }

        // 3. APPLY RESULTS: result.getHReturn() into out_memory allocated
        if (result != null) {
            byte[] buffer = result.getHReturn();
            int offset = msg.getOutDataOffs().intValue();
            int size = msg.getOutDataSize().intValue();

            memorySaveLimited(offset, buffer, size);

            returnDataBuffer = buffer;
        }

        // 5. REFUND THE REMAIN GAS
        if (result != null) {
            BigInteger refundGas = msg.getGas().value().subtract(toBI(result.getGasUsed()));
            if (isPositive(refundGas)) {
                refundGas(refundGas.longValue(), "remaining gas from the internal call");
                if (logger.isTraceEnabled())
                    logger.trace("The remaining gas refunded, account: [{}], gas: [{}] ",
                            toHexString(senderAddress),
                            refundGas.toString());
            }
            result.clear();
        } else {
            refundGas(msg.getGas().longValue(), "remaining gas from the internal call");
        }
    }

    public void spendGas(long gasValue, String cause) {
        if ((invoke.getGasLong() - result.gasUsed) < gasValue) {
            throw Program.Exception.notEnoughSpendingGas(cause, gasValue, this);
        }
        //getResult().spendGas(gasValue);
        result.gasUsed += gasValue;
    }

    public void spendAllGas() {
        spendGas(getGas().longValue(), "Spending all remaining");
    }

    public void refundGas(long gasValue, String cause) {
        //logger.info("[{}] Refund for cause: [{}], gas: [{}]", invoke.hashCode(), cause, gasValue);
        getResult().refundGas(gasValue);
    }

    public void futureRefundGas(long gasValue) {
        logger.info("Future refund added: [{}]", gasValue);
        getResult().addFutureRefund(gasValue);
    }

    public void resetFutureRefund() {
        getResult().resetFutureRefund();
    }

    public void storageSave(DataWord word1, DataWord word2) {
        storageSave(word1.getData(), word2.getData());
    }

    public void storageSave(byte[] key, byte[] val) {
        DataWord keyWord = DataWord.of(key);
        DataWord valWord = DataWord.of(val);
        this.repository.addStorageRow(getOwnerAddress().getLast20Bytes(), keyWord, valWord);
    }

    public byte[] getCode() {
        return ops;
    }

    public byte[] getCodeAt(DataWord address) {
        byte[] code = invoke.getRepository().getCode(address.getLast20Bytes());
        return nullToEmpty(code);
    }

    public byte[] getCodeHashAt(DataWord address) {
        AccountState state = invoke.getRepository().getAccountState(address.getLast20Bytes());
        // return 0 as a code getHash of empty account (an account that would be removed by state clearing)
        if (state != null && state.isEmpty()) {
            return EMPTY_BYTE_ARRAY;
        } else {
            byte[] code = invoke.getRepository().getCodeHash(address.getLast20Bytes());
            return nullToEmpty(code);
        }
    }

    public DataWord getOwnerAddress() {
        return invoke.getOwnerAddress();
    }

    public DataWord getBlockHash(long index) {
        return index < this.getNumber().longValueSafe() && index >= Math.max(256, this.getNumber().longValueSafe()) - 256 ?
                getBlockHashByNumber(index) :
                DataWord.ZERO;
    }

    private DataWord getBlockHashByNumber(long index) {
        byte[] hash = this.invoke.getRepository().getBlockHashByNumber(index);
        int count = 0;
        while (hash == null && count < 2) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
            }
            count++;
            hash = this.invoke.getRepository().getBlockHashByNumber(index);
        }

        return DataWord.of(hash);

    }

    public DataWord getBalance(DataWord address) {
        BigInteger balance = getRepository().getBalance(address.getLast20Bytes());
        return DataWord.of(balance.toByteArray());
    }

    public DataWord getOriginAddress() {
        return invoke.getOriginAddress();
    }

    public DataWord getCallerAddress() {
        return invoke.getCallerAddress();
    }

    public DataWord getGasPrice() {
        return invoke.getMinGasPrice();
    }

    public long getGasLong() {
        //return invoke.getGasLong() - getResult().getGasUsed();
        return invoke.getGasLong() - result.gasUsed;
    }

    public DataWord getGas() {
        return DataWord.of(invoke.getGasLong() - getResult().getGasUsed());
    }

    public DataWord getCallValue() {
        return invoke.getCallValue();
    }

    public DataWord getDataSize() {
        return invoke.getDataSize();
    }

    public DataWord getDataValue(DataWord index) {
        return invoke.getDataValue(index);
    }

    public byte[] getDataCopy(DataWord offset, DataWord length) {
        return invoke.getDataCopy(offset, length);
    }

    public DataWord getReturnDataBufferSize() {
        return DataWord.of(getReturnDataBufferSizeI());
    }

    private int getReturnDataBufferSizeI() {
        return returnDataBuffer == null ? 0 : returnDataBuffer.length;
    }

    public byte[] getReturnDataBufferData(DataWord off, DataWord size) {
        if ((long) off.intValueSafe() + size.intValueSafe() > getReturnDataBufferSizeI()) return null;
        return returnDataBuffer == null ? new byte[0] :
                Arrays.copyOfRange(returnDataBuffer, off.intValueSafe(), off.intValueSafe() + size.intValueSafe());
    }

    public DataWord storageLoad(DataWord key) {
        return repository.getStorageValue(getOwnerAddress().getLast20Bytes(), key);
    }

//    /**
//     * @return current Storage data for key
//     */
//    public DataWord getCurrentValue(DataWord key) {
//        return getRepository().getStorageValue(getOwnerAddress().getLast20Bytes(), key);
//    }

    public DataWord getPrevHash() {
        return invoke.getPrevHash();
    }

    public DataWord getCoinbase() {
        return invoke.getCoinbase();
    }

    public DataWord getTimestamp() {
        return invoke.getTimestamp();
    }

    public DataWord getNumber() {
        return invoke.getNumber();
    }

    public DataWord getDifficulty() {
        return invoke.getDifficulty();
    }

    public DataWord getGasLimit() {
        return invoke.getGaslimit();
    }

    public boolean isStaticCall() {
        return invoke.isStaticCall();
    }

    public ProgramResult getResult() {
        return result;
    }

    public void setRuntimeFailure(RuntimeException e) {
        getResult().setException(e);
    }

    public String memoryToString() {
        return memory.toString();
    }

    public void fullTrace() {

        // doNothing
    }

//    public void saveOpTrace() {
//        if (this.pc < ops.length) {
//            trace.addOp(ops[pc], pc, getCallDeep(), getGas(), traceListener.resetActions());
//        }
//    }

//    public ProgramTrace getTrace() {
//        return trace;
//    }

    static String formatBinData(byte[] binData, int startPC) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < binData.length; i += 16) {
            ret.append(VMUtils.align("" + Integer.toHexString(startPC + (i)) + ":", ' ', 8, false));
            ret.append(Hex.toHexString(binData, i, min(16, binData.length - i))).append('\n');
        }
        return ret.toString();
    }

    public static String stringifyMultiline(byte[] code) {
        int index = 0;
        StringBuilder sb = new StringBuilder();
        BitSet mask = buildReachableBytecodesMask(code);
        ByteArrayOutputStream binData = new ByteArrayOutputStream();
        int binDataStartPC = -1;

        while (index < code.length) {
            final byte opCode = code[index];
            OpCode op = OpCode.code(opCode);

            if (!mask.get(index)) {
                if (binDataStartPC == -1) {
                    binDataStartPC = index;
                }
                binData.write(code[index]);
                index++;
                if (index < code.length) continue;
            }

            if (binDataStartPC != -1) {
                sb.append(formatBinData(binData.toByteArray(), binDataStartPC));
                binDataStartPC = -1;
                binData = new ByteArrayOutputStream();
                if (index == code.length) continue;
            }

            sb.append(VMUtils.align("" + Integer.toHexString(index) + ":", ' ', 8, false));

            if (op == null) {
                sb.append("<UNKNOWN>: ").append(0xFF & opCode).append("\n");
                index++;
                continue;
            }

            if (op.name().startsWith("PUSH")) {
                sb.append(' ').append(op.name()).append(' ');

                int nPush = op.val() - OpCode.PUSH1.val() + 1;
                byte[] data = Arrays.copyOfRange(code, index + 1, index + nPush + 1);
                BigInteger bi = new BigInteger(1, data);
                sb.append("0x").append(bi.toString(16));
                if (bi.bitLength() <= 32) {
                    sb.append(" (").append(new BigInteger(1, data).toString()).append(") ");
                }

                index += nPush + 1;
            } else {
                sb.append(' ').append(op.name());
                index++;
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    static class ByteCodeIterator {
        byte[] code;
        int pc;

        public ByteCodeIterator(byte[] code) {
            this.code = code;
        }

        public void setPC(int pc) {
            this.pc = pc;
        }

        public int getPC() {
            return pc;
        }

        public OpCode getCurOpcode() {
            return pc < code.length ? OpCode.code(code[pc]) : null;
        }

        public boolean isPush() {
            return getCurOpcode() != null ? getCurOpcode().name().startsWith("PUSH") : false;
        }

        public byte[] getCurOpcodeArg() {
            if (isPush()) {
                int nPush = getCurOpcode().val() - OpCode.PUSH1.val() + 1;
                byte[] data = Arrays.copyOfRange(code, pc + 1, pc + nPush + 1);
                return data;
            } else {
                return new byte[0];
            }
        }

        public boolean next() {
            pc += 1 + getCurOpcodeArg().length;
            return pc < code.length;
        }
    }

    static BitSet buildReachableBytecodesMask(byte[] code) {
        NavigableSet<Integer> gotos = new TreeSet<>();
        ByteCodeIterator it = new ByteCodeIterator(code);
        BitSet ret = new BitSet(code.length);
        int lastPush = 0;
        int lastPushPC = 0;
        do {
            ret.set(it.getPC()); // reachable bytecode
            if (it.isPush()) {
                lastPush = new BigInteger(1, it.getCurOpcodeArg()).intValue();
                lastPushPC = it.getPC();
            }
            if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.JUMPI) {
                if (it.getPC() != lastPushPC + 1) {
                    // some PC arithmetic we totally can't deal with
                    // assuming all bytecodes are reachable as a fallback
                    ret.set(0, code.length);
                    return ret;
                }
                int jumpPC = lastPush;
                if (!ret.get(jumpPC)) {
                    // code was not explored yet
                    gotos.add(jumpPC);
                }
            }
            if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.RETURN ||
                    it.getCurOpcode() == OpCode.STOP) {
                if (gotos.isEmpty()) break;
                it.setPC(gotos.pollFirst());
            }
        } while (it.next());
        return ret;
    }

    public static String stringify(byte[] code) {
        int index = 0;
        StringBuilder sb = new StringBuilder();
        BitSet mask = buildReachableBytecodesMask(code);
        String binData = "";

        while (index < code.length) {
            final byte opCode = code[index];
            OpCode op = OpCode.code(opCode);

            if (op == null) {
                sb.append(" <UNKNOWN>: ").append(0xFF & opCode).append(" ");
                index++;
                continue;
            }

            if (op.name().startsWith("PUSH")) {
                sb.append(' ').append(op.name()).append(' ');

                int nPush = op.val() - OpCode.PUSH1.val() + 1;
                byte[] data = Arrays.copyOfRange(code, index + 1, index + nPush + 1);
                BigInteger bi = new BigInteger(1, data);
                sb.append("0x").append(bi.toString(16)).append(" ");

                index += nPush + 1;
            } else {
                sb.append(' ').append(op.name());
                index++;
            }
        }

        return sb.toString();
    }


//    public void addListener(ProgramOutListener listener) {
//        this.listener = listener;
//    }

    public int verifyJumpDest(DataWord nextPC) {
        if (nextPC.bytesOccupied() > 4) {
            throw Program.Exception.badJumpDestination(-1);
        }
        int ret = nextPC.intValue();
        if (!getProgramPrecompile().hasJumpDest(ret)) {
            throw Program.Exception.badJumpDestination(ret);
        }
        return ret;
    }

    public void callToPrecompiledAddress(MessageCall msg, PrecompiledContract contract) {
        returnDataBuffer = null; // reset return buffer right before the call

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            this.refundGas(msg.getGas().longValue(), " call deep limit reach");
            return;
        }

        Repository track = getRepository().startTracking();

//        byte[] senderAddress = this.getOwnerAddress().getLast20Bytes();
//        byte[] codeAddress = msg.getCodeAddress().getLast20Bytes();
//        byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;


//        BigInteger endowment = msg.getEndowment().value();
        //BigInteger senderBalance = track.getBalance(senderAddress);
//        if (senderBalance.compareTo(endowment) < 0) {
//            stackPushZero();
//            this.refundGas(msg.getGas().longValue(), "refund gas from message call");
//            return;
//        }

        byte[] data = this.memoryChunk(msg.getInDataOffs().intValue(),
                msg.getInDataSize().intValue());

        // Charge for endowment - is not reversible by rollback
        //transfer(track, senderAddress, contextAddress, msg.getEndowment().value());

        if (byTestingSuite()) {
            // This keeps track of the calls created for a test
            this.getResult().addCallCreate(data,
                    msg.getCodeAddress().getLast20Bytes(),
                    msg.getGas().getNoLeadZeroesData(),
                    msg.getEndowment().getNoLeadZeroesData());

            stackPushOne();
            return;
        }


        long requiredGas = contract.getGasForData(data);
        if (requiredGas > msg.getGas().longValue()) {

            this.refundGas(0, "call pre-compiled"); //matches cpp logic
            this.stackPushZero();
            track.rollback();
        } else {

            if (logger.isDebugEnabled())
                logger.debug("Call {}(data = {})", contract.getClass().getSimpleName(), toHexString(data));

            Pair<Boolean, byte[]> out = contract.execute(data);

            if (out.getLeft()) { // success
                this.refundGas(msg.getGas().longValue() - requiredGas, "call pre-compiled");
                this.stackPushOne();
                returnDataBuffer = out.getRight();
                track.commit();
            } else {
                // spend all gas on failure, push zero and revert state changes
                this.refundGas(0, "call pre-compiled");
                this.stackPushZero();
                track.rollback();
            }

            this.memorySave(msg.getOutDataOffs().intValue(), msg.getOutDataSize().intValueSafe(), out.getRight());
        }
    }

    public boolean byTestingSuite() {
        return invoke.byTestingSuite();
    }

    public interface ProgramOutListener {
        void output(String out);
    }


    /**
     * Denotes problem when executing Ethereum bytecode.
     * From blockchain and peer perspective this is quite normal situation
     * and doesn't mean exceptional situation in terms of the program execution
     */
    @SuppressWarnings("serial")
    public static class BytecodeExecutionException extends RuntimeException {
        public BytecodeExecutionException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    public static class OutOfGasException extends BytecodeExecutionException {

        public OutOfGasException(String message, Object... args) {
            super(format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class IllegalOperationException extends BytecodeExecutionException {

        public IllegalOperationException(String message, Object... args) {
            super(format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class BadJumpDestinationException extends BytecodeExecutionException {

        public BadJumpDestinationException(String message, Object... args) {
            super(format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class StackTooSmallException extends BytecodeExecutionException {

        public StackTooSmallException(String message, Object... args) {
            super(format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class ReturnDataCopyIllegalBoundsException extends BytecodeExecutionException {
        public ReturnDataCopyIllegalBoundsException(DataWord off, DataWord size, long returnDataSize) {
            super(String.format("Illegal RETURNDATACOPY arguments: offset (%s) + size (%s) > RETURNDATASIZE (%d)", off, size, returnDataSize));
        }
    }

    @SuppressWarnings("serial")
    public static class StaticCallModificationException extends BytecodeExecutionException {
        public StaticCallModificationException() {
            super("Attempt to call a state modifying opcode inside STATICCALL");
        }
    }


    public static class Exception {

        public static OutOfGasException notEnoughOpGas(OpCode op, long opGas, long programGas) {
            return new OutOfGasException("Not enough gas for '%s' operation executing: opGas[%d], programGas[%d];", op, opGas, programGas);
        }

        public static OutOfGasException notEnoughOpGas(OpCode op, DataWord opGas, DataWord programGas) {
            return notEnoughOpGas(op, opGas.longValue(), programGas.longValue());
        }

        public static OutOfGasException notEnoughOpGas(OpCode op, BigInteger opGas, BigInteger programGas) {
            return notEnoughOpGas(op, opGas.longValue(), programGas.longValue());
        }

        public static OutOfGasException notEnoughSpendingGas(String cause, long gasValue, Program program) {
            return new OutOfGasException("Not enough gas for '%s' cause spending: invokeGas[%d], gas[%d], usedGas[%d];",
                    cause, program.invoke.getGas().longValue(), gasValue, program.getResult().getGasUsed());
        }

        public static OutOfGasException gasOverflow(BigInteger actualGas, BigInteger gasLimit) {
            return new OutOfGasException("Gas value overflow: actualGas[%d], gasLimit[%d];", actualGas.longValue(), gasLimit.longValue());
        }

        public static IllegalOperationException invalidOpCode(byte... opCode) {
            return new IllegalOperationException("Invalid operation code: opCode[%s];", Hex.toHexString(opCode, 0, 1));
        }

        public static IllegalOperationException invalidOperation() {
            return new IllegalOperationException("Invalid operation!");
        }

        public static BadJumpDestinationException badJumpDestination(int pc) {
            return new BadJumpDestinationException("Operation with pc isn't 'JUMPDEST': PC[%d];", pc);
        }

        public static StackTooSmallException tooSmallStack(int expectedSize, int actualSize) {
            return new StackTooSmallException("Expected stack size %d but actual %d;", expectedSize, actualSize);
        }
    }

    @SuppressWarnings("serial")
    public class StackTooLargeException extends BytecodeExecutionException {
        public StackTooLargeException(String message) {
            super(message);
        }
    }

    /**
     * used mostly for testing reasons
     */
    public byte[] getMemory() {
        return memory.read(0, memory.size());
    }

    /**
     * used mostly for testing reasons
     */
    public void initMem(byte[] data) {
        this.memory.write(0, data, data.length, false);
    }

    public void afterPlay() {
        //System.out.println(memory);
        try {
            this.ethTransaction = null;
            this.invoke = null;
            this.stack.clear();
            this.memory = null;
            this.config = null;
            this.programPrecompile = null;
            this.vmHook = null;
            this.touchedAccounts.clear();
            //this.memory.recycle();
        } catch (java.lang.Exception e) {

        }
    }

    public void clear() {
        if (result != null) {
            result.clear();
        }

    }
}
