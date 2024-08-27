package com.thanos.chain.executor;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.WorkerPool;
import com.lmax.disruptor.dsl.Disruptor;
import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.contract.ca.filter.GlobalFilterChain;
import com.thanos.chain.executor.dag.ExecutableGenerator;
import com.thanos.chain.executor.dag.ExecutableGeneratorImpl;
import com.thanos.chain.executor.dag.ExecuteNode;
import com.thanos.chain.executor.dag.ExecuteRoot;
import com.thanos.chain.executor.dag.ReuseCountDownLatch;
import com.thanos.chain.executor.dag.test_imple.GenerateParallelExecutable;
import com.thanos.chain.executor.dag.test_imple.ParallelExecutable;
import com.thanos.chain.executor.dag.test_imple.SimpleLimitLevelDagGenerate;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.chain.storage.db.Repository;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ThanosThreadFactory;
import com.thanos.common.utils.ThanosWorker;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 类ParallelTransactionExecutor.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-20 15:32:38
 */
public class EthParallelTransactionsExecutor extends AbstractTransactionsExecutor {

    private int PROCESSOR;

    static final AtomicLong THREAD_COUNTER = new AtomicLong(0);

    private final RejectedExecutionHandler defaultHandler = (r, executor) -> logger.warn("task was rejected");

    public static final class ExecuteEvent {

        public ExecuteNode executeNode;

        public Function<Integer, Void> txExecuteFun;

        public RingBuffer<ExecuteEvent> eventRingBuffer;

        public ReuseCountDownLatch finishCondition;

        public Function<ExecuteNode.ReRunTaskContextX, Void> reExecuteFun;

    }

    static class EventExceptionHandler implements ExceptionHandler {
        @Override
        public void handleEventException(Throwable arg0, long arg1, Object arg2) {
        }

        @Override
        public void handleOnShutdownException(Throwable arg0) {
        }

        @Override
        public void handleOnStartException(Throwable arg0) {
        }
    }

    public class ExecutorEventConsumer implements WorkHandler<ExecuteEvent> {

        @Override
        public void onEvent(ExecuteEvent executeEvent) throws Exception {
            //logger.debug("ExecutorEventConsumer:{}", executeEvent.executeNode);
//            try {
//                executeEvent.txExecuteFun.apply(executeEvent.executeNode.txIndex);
//            } finally {
//                executeEvent.executeNode.finishExecuteX(executeEvent.eventRingBuffer, executeEvent.finishCondition, executeEvent.txExecuteFun, executeEvent.reExecuteFun);
//            }

            executeEvent.executeNode.executeNodeX(executeEvent.eventRingBuffer, executeEvent.finishCondition, executeEvent.txExecuteFun, executeEvent.reExecuteFun);


            // help gc
            executeEvent.eventRingBuffer = null;
            executeEvent.executeNode = null;
            executeEvent.txExecuteFun = null;
            executeEvent.finishCondition = null;
            executeEvent.reExecuteFun = null;
        }
    }

    public class ExecutorEventConsumerX implements EventHandler<ExecuteEvent> {

        @Override
        public void onEvent(ExecuteEvent executeEvent, long sequence, boolean endOfBatch) throws Exception {
            //logger.debug("ExecutorEventConsumer:{}", executeEvent.executeNode);

            executeEvent.executeNode.executeNodeX(executeEvent.eventRingBuffer, executeEvent.finishCondition, executeEvent.txExecuteFun, executeEvent.reExecuteFun);

            // help gc
            executeEvent.eventRingBuffer = null;
            executeEvent.executeNode = null;
            executeEvent.txExecuteFun = null;
            executeEvent.finishCondition = null;
            executeEvent.reExecuteFun = null;
        }
    }

    WorkerPool<ExecuteEvent> workerPool;

    RingBuffer<ExecuteEvent> eventRingBuffer;

    ExecutorEventConsumer executorEventConsumer;


    List<Disruptor<ExecuteEvent>> executorDisruptors;

    List<RingBuffer<ExecuteEvent>> executorEventRingBuffers;

    EventFactory<ExecuteEvent> eventFactory;

    ThreadFactory threadFactory;

    ExecutorEventConsumerX executorEventConsumerX;

    static {

    }


    ExecutableGenerator executableGenerator = new ExecutableGeneratorImpl();


    private ThreadPoolExecutor txExecutor;
    //private MpmcThreadExecutor txExecutor = new MpmcThreadExecutor(600000, PROCESSOR);

    ReuseCountDownLatch reuseCountDownLatch = new ReuseCountDownLatch(0);

    private final BlockingQueue<ExecuteNode.ReRunTaskContext> reactorNotify = new ArrayBlockingQueue<>(600000);

    public EthParallelTransactionsExecutor(StateLedger stateLedger) {
        super(stateLedger);
        PROCESSOR = stateLedger.systemConfig.getParallelProcessorNum();
        txExecutor = new ThreadPoolExecutor(PROCESSOR, PROCESSOR, 20, TimeUnit.SECONDS, new ArrayBlockingQueue<>(600000), new ThanosThreadFactory("eth_transactions_parallel_process"), defaultHandler);

//        eventFactory = () -> new ExecuteEvent();
//        threadFactory = r -> new Thread(r, "consensusPayload_decode_thread_" + THREAD_COUNTER.incrementAndGet());
//        executorEventConsumerX = new ExecutorEventConsumerX();
//        executorDisruptors =  new ArrayList<>(PROCESSOR);
//        executorEventRingBuffers = new ArrayList<>(PROCESSOR);
//        for (int i = 0; i < PROCESSOR; i++) {
//            Disruptor<ExecuteEvent> executeEventDisruptor = new Disruptor(eventFactory, 1024 * 512, threadFactory, ProducerType.MULTI, new YieldingWaitStrategy());
//            executorDisruptors.add(executeEventDisruptor);
//            executeEventDisruptor.handleEventsWith(executorEventConsumerX);
//            executorEventRingBuffers.add(executeEventDisruptor.getRingBuffer());
//            executeEventDisruptor.start();
//        }


//        executorEventConsumer = new ExecutorEventConsumer();
//        ExecutorEventConsumer[] consumers = new ExecutorEventConsumer[PROCESSOR];
//        for (int i = 0; i < PROCESSOR; i++) {
//            consumers[i] = executorEventConsumer;
//        }
//
//
//        eventRingBuffer = RingBuffer.create(ProducerType.MULTI,
//                () -> new ExecuteEvent(),
//                1024 * 1024,
//                new YieldingWaitStrategy());
//
//        SequenceBarrier barrier = eventRingBuffer.newBarrier();
//
//        workerPool =
//                new WorkerPool<ExecuteEvent>(eventRingBuffer,
//                        barrier,
//                        new EventExceptionHandler(),
//                        consumers);
//        //eventRingBuffer.addGatingSequences(workerPool.getWorkerSequences());
//        workerPool.start(Executors.newFixedThreadPool(PROCESSOR));


        startReactorNotify();
    }

    private void startReactorNotify() {
        new ThanosWorker("reactor_notify_executor_tx_thread") {
            @Override
            protected void doWork() throws Exception {
                ExecuteNode.ReRunTaskContext context = reactorNotify.take();
                //ExecuteNode.ReRunTaskContextX context = reactorNotify.take();
                context.getExecuteNode().getLevel().awaitLPreParallelLevelAllNodeFinish();
                //System.out.println(context.getExecuteNode().getLevel());
                if (context.getExecuteNode().getLevel().isPreParallelLevelAllNodeFinish()) {

//                    RingBuffer<ExecuteEvent> eventRingBuffer = executorEventRingBuffers.get(context.getExecuteNode().txIndex % executorEventRingBuffers.size());
//                    long seq = eventRingBuffer.next();
//                    try {
//                        EthParallelTransactionsExecutor.ExecuteEvent executeEvent = eventRingBuffer.get(seq);
//                        executeEvent.executeNode = context.getExecuteNode();
//                        executeEvent.txExecuteFun = context.getExeFun();
//                        executeEvent.eventRingBuffer = context.getEventRingBuffer();
//                        executeEvent.finishCondition = context.getFinishCondition();
//                        executeEvent.reExecuteFun = context.getReExecuteFun();
//                    } catch (Exception e) {
//                        //logger.warn("ConsensusPayload publicEvent error!", e);
//                        throw new RuntimeException("ConsensusPayload publicEvent error!", e);
//                    } finally {
//                        eventRingBuffer.publish(seq);
//                    }

                    //System.out.println(context);
                    //context.getExecuteNode().executeNodeX(context.getEventRingBuffer(), context.getFinishCondition(), context.getExeFun(), context.getReExecuteFun());

                    txExecutor.execute(() -> context.getExecuteNode().executeNode(context.getExecutor(), context.getFinishCondition(), context.getExeFun(), context.getReExecuteFun()));
                    //context.getExecuteNode().executeNode(context.getExecutor(), context.getFinishCondition(), context.getExeFun(), context.getReExecuteFun());
                } else {
                    reactorNotify.add(context);
                }
            }
        }.start();
    }

    public List<EthTransactionReceipt> execute(Block block) {

        final EthTransaction[] txs = block.getTransactionsList();
        final List<EthTransactionReceipt> result = new ArrayList<>(txs.length);
        //int i = 1;
        long dagGenStart = System.currentTimeMillis();
        List<ExecuteRoot> executeRoots = block.getDagExecuteRoots();
        //executeRoots =  executableGenerator.generate(txs).getExecuteRoots();
        if (executeRoots == null) {
            executeRoots = executableGenerator.generate(txs).getExecuteRoots();
        }
        block.setDagExecuteRoots(null);
        long dagGenEnd = System.currentTimeMillis();
//
        final GlobalFilterChain globalFilterChain = this.stateLedger.consensusChainStore.globalFilterChain;


        //long dagExeStart = System.currentTimeMillis();
        for (ExecuteRoot executeRoot : executeRoots) {

            logger.info(" all chain trace finish, current root total level:{}, total parallel serial num:{},  total serial num:{}", executeRoot.getLevelSize(), executeRoot.getTotalParallelNodeNum(), executeRoot.getSerialTransactions().size());


            for (EthTransaction executeTx : executeRoot.getSerialTransactions()) {
                try {
                    if (!executeTx.isDsCheckValid()) {
                        executeTx.setErrEthTransactionReceipt("current tx is ds tx!!!");
                        continue;
                    }

                    ProcessResult filterResult = globalFilterChain.filter(executeTx);
                    if (!filterResult.isSuccess()) {
                        executeTx.setErrEthTransactionReceipt(filterResult.getErrMsg());
                        continue;
                    }

                    Repository repository = stateLedger.rootRepository.startTracking();
                    EthTransactionExecutor txExecutor = new EthTransactionExecutor(executeTx, repository, stateLedger.programInvokeFactory, block).withConfig(this.stateLedger.systemConfig);
                    txExecutor.init();
                    txExecutor.execute();
                    txExecutor.go();
                    executeTx.setEthTransactionReceipt(txExecutor.getReceipt());
                    txExecutor.finalization();
                } catch (Exception e) {
                    String err = ExceptionUtils.getStackTrace(e);
                    executeTx.setErrEthTransactionReceipt(err);
                    logger.warn("EthParallelTransactionsExecutor execute [{}] error!, [{}]", executeTx, err);
                }
            }


            try {
                int totalSize = executeRoot.getTotalParallelNodeNum();
                reuseCountDownLatch.reuse(totalSize);

                List<ExecuteNode> parallelExecuteNodes = executeRoot.getExecuteRootNodes();
                //System.out.println("start execute: " + executeRoot);
                for (ExecuteNode executeNode : parallelExecuteNodes) {

                    Function<Integer, Void> txExecuteFun = txIndex -> {
                        EthTransaction executeTx = txs[txIndex];
//                        StringBuilder sb = new StringBuilder();
//                        sb.append("finish execute tx[" + txIndex + "], states[");
//                        for (ByteArrayWrapper byteArrayWrapper: executeTx.getExecuteStates()) {
//                            String executeState = new String(byteArrayWrapper.getData());
//                            sb.append(executeState).append(",");
//                        }
//                        sb.append("]");
                        //System.out.println("start execute tx[" + txIndex + "]");

                        try {
                            if (!executeTx.isDsCheckValid()) {
                                executeTx.setErrEthTransactionReceipt("current tx is ds tx!!!");
                                return null;
                            }

                            ProcessResult filterResult = globalFilterChain.filter(executeTx);
                            if (!filterResult.isSuccess()) {
                                executeTx.setErrEthTransactionReceipt(filterResult.getErrMsg());
                                return null;
                            }

                            Repository repository = stateLedger.rootRepository.startTracking();
                            EthTransactionExecutor txExecutor = new EthTransactionExecutor(executeTx, repository, stateLedger.programInvokeFactory, block).withConfig(this.stateLedger.systemConfig);
                            txExecutor.init();
                            txExecutor.execute();
                            txExecutor.go();
                            executeTx.setEthTransactionReceipt(txExecutor.getReceipt());
                            txExecutor.finalization();
                        } catch (Exception e) {
                            String err = ExceptionUtils.getStackTrace(e);
                            executeTx.setErrEthTransactionReceipt(err);
                            logger.warn("EthParallelTransactionsExecutor execute [{}] error!, [{}]", executeTx, err);
                        }
                        //System.out.println(sb.toString());

                        return null;
                    }; // end function

                    Function<ExecuteNode.ReRunTaskContext, Void> reExecuteFun = reRunTaskContext -> {
                        reactorNotify.add(reRunTaskContext);
                        return null;
                    };

                    txExecutor.execute(() -> executeNode.executeNode(txExecutor, reuseCountDownLatch, txExecuteFun, reExecuteFun));

                }

                try {
                    reuseCountDownLatch.await();
                } catch (Exception e) {
                    logger.warn("reuseCountDownLatch.await() error!", e);
                }
                //System.out.println("finish execute " + i + " exeRoot, total node size:" + totalSize);
                //i++;
            } catch (Exception e) {
                logger.warn(String.format("EthParallelTransactionsExecutor ExecuteRoot  %s error!", executeRoot), e);
            } finally {
                executeRoot.finishExecute();
            }
        }

        for (EthTransaction tx : txs) {
            result.add(tx.getEthTransactionReceipt());
            tx.setEthTransactionReceipt(null);
        }

        long dagExeEnd = System.currentTimeMillis();


        logger.debug("  all chain trace finish exe block[{}-{}], dag generate use:[{}], exe cost:[{}]", block.getNumber(), txs.length, (dagGenEnd - dagGenStart), (dagExeEnd - dagGenEnd));

        return result;
    }


//    public List<EthTransactionReceipt> execute(Block block) {
//
//        final EthTransaction[] txs = block.getTransactionsList();
//        final List<EthTransactionReceipt> result = new ArrayList<>(txs.length);
//        //int i = 1;
//        //long dagGenStart = System.currentTimeMillis();
//        List<ExecuteRoot> executeRoots =  executableGenerator.generate(txs).getExecuteRoots();
//        //long dagGenEnd = System.currentTimeMillis();
//        //logger.debug("block[{}], dag generate use:" + (dagGenEnd - dagGenStart));
////
//        long dagExeStart = System.currentTimeMillis();
//        for (ExecuteRoot executeRoot: executeRoots) {
//            try {
//                int totalSize = executeRoot.getTotalParallelNodeNum();
//                reuseCountDownLatch.reuse(totalSize);
//
//                List<ExecuteNode> parallelExecuteNodes = executeRoot.getExecuteRootNodes();
//                //System.out.println("start execute: " + executeRoot);
//                for (int i = 0; i < parallelExecuteNodes.size(); i++) {
//                    ExecuteNode executeNode = parallelExecuteNodes.get(i);
//                    //RingBuffer<ExecuteEvent> eventRingBuffer = eventRingBuffer.get(i % PROCESSOR);
//
//
//                    Function<Integer, Void> txExecuteFun = txIndex -> {
//                        EthTransaction executeTx = txs[txIndex];
////                        StringBuilder sb = new StringBuilder();
////                        sb.append("finish execute tx[" + txIndex + "], states[");
////                        for (ByteArrayWrapper byteArrayWrapper: executeTx.getExecuteStates()) {
////                            String executeState = new String(byteArrayWrapper.getData());
////                            sb.append(executeState).append(",");
////                        }
////                        sb.append("]");
//                        //System.out.println("start execute tx[" + txIndex + "]");
//                        Repository repository = stateLedger.rootRepository.startTracking();
//
//                        try {
//                            EthTransactionExecutor txExecutor = new EthTransactionExecutor(executeTx, repository, stateLedger.programInvokeFactory, block).withConfig(this.stateLedger.systemConfig);
//                            txExecutor.init();
//                            txExecutor.execute();
//                            txExecutor.go();
//                            txExecutor.finalization();
//                            final EthTransactionReceipt receipt = txExecutor.getReceipt();
//                            executeTx.setEthTransactionReceipt(receipt);
//                        } catch (Exception e) {
//                            logger.warn(String.format("EthParallelTransactionsExecutor execute %s error!", executeTx), e);
//                        }
//                        //System.out.println(sb.toString());
//
//                        return null;
//                    }; // end function
//
//                    Function<ExecuteNode.ReRunTaskContextX, Void> reExecuteFun = reRunTaskContext -> {
//                        reactorNotify.add(reRunTaskContext);
//                        return null;
//                    };
//
//                    long seq = eventRingBuffer.next();
//                    try {
//                        EthParallelTransactionsExecutor.ExecuteEvent executeEvent = eventRingBuffer.get(seq);
//                        executeEvent.executeNode = executeNode;
//                        executeEvent.txExecuteFun = txExecuteFun;
//                        executeEvent.eventRingBuffer = eventRingBuffer;
//                        executeEvent.finishCondition = reuseCountDownLatch;
//                        executeEvent.reExecuteFun = reExecuteFun;
//                    } catch (Exception e) {
//                        //logger.warn("ConsensusPayload publicEvent error!", e);
//                        throw new RuntimeException("ConsensusPayload publicEvent error!", e);
//                    } finally {
//                        eventRingBuffer.publish(seq);
//                    }
//
//                    //executeNode.executeNodeX(eventRingBuffer, reuseCountDownLatch, txExecuteFun, reExecuteFun);
//                }
//
//                try {
//                    reuseCountDownLatch.await();
//                } catch (Exception e) {
//                    logger.warn("reuseCountDownLatch.await() error!", e);
//                }
//                //System.out.println("finish execute " + i + " exeRoot, total node size:" + totalSize);
//                //i++;
//            } catch (Exception e) {
//                logger.warn(String.format("EthParallelTransactionsExecutor ExecuteRoot  %s error!", executeRoot), e);
//            } finally {
//                executeRoot.finishExecute();
//            }
//        }
//
//        for (EthTransaction tx: txs) {
//            result.add(tx.getEthTransactionReceipt());
//        }
//
//        //long dagExeEnd = System.currentTimeMillis();
//
//        //logger.debug("[{}]block[number={}, txs.size={}], exe total cost[{}ms],  dag generate use[{}ms], dag exe use[{}ms]" , Thread.currentThread().getName(), block.getNumber(), block.getTransactionsList().length, (dagExeEnd - dagGenStart), (dagGenEnd - dagGenStart), (dagExeEnd - dagExeStart));
//
//        return result;
//    }

//    public List<EthTransactionReceipt> execute(Block block) {
//
//        final EthTransaction[] txs = block.getTransactionsList();
//        final List<EthTransactionReceipt> result = new ArrayList<>(txs.length);
//        //int i = 1;
//        long dagGenStart = System.currentTimeMillis();
//        List<ExecuteRoot> executeRoots = block.getDagExecuteRoots();
//        if (executeRoots == null) {
//            executeRoots =  executableGenerator.generate(txs).getExecuteRoots();
//        }
//        block.setDagExecuteRoots(null);
//        long dagGenEnd = System.currentTimeMillis();
//        logger.debug(" all chain trace block[{}], dag generate use:{}" , block.getNumber(), (dagGenEnd - dagGenStart));
////
//        for (ExecuteRoot executeRoot: executeRoots) {
//            try {
//                int totalSize = executeRoot.getTotalParallelNodeNum();
//                reuseCountDownLatch.reuse(totalSize);
//
//                List<ExecuteNode> parallelExecuteNodes = executeRoot.getExecuteRootNodes();
//                System.out.println("parallelExecuteNodes size: " + parallelExecuteNodes.size());
//                for (int i = 0; i < parallelExecuteNodes.size(); i++) {
//
//                    ExecuteNode executeNode = parallelExecuteNodes.get(i);
//                    RingBuffer<ExecuteEvent> eventRingBuffer = this.executorEventRingBuffers.get(i % PROCESSOR);
//
//
//                    Function<Integer, Void> txExecuteFun = txIndex -> {
//                        EthTransaction executeTx = txs[txIndex];
////                        StringBuilder sb = new StringBuilder();
////                        sb.append("finish execute tx[" + txIndex + "], states[");
////                        for (ByteArrayWrapper byteArrayWrapper: executeTx.getExecuteStates()) {
////                            String executeState = new String(byteArrayWrapper.getData());
////                            sb.append(executeState).append(",");
////                        }
////                        sb.append("]");
//                        //System.out.println("start execute tx[" + txIndex + "]");
//                        Repository repository = stateLedger.rootRepository.startTracking();
//
//                        try {
//                            EthTransactionExecutor txExecutor = new EthTransactionExecutor(executeTx, repository, stateLedger.programInvokeFactory, block).withConfig(this.stateLedger.systemConfig);
//                            txExecutor.init();
//                            txExecutor.execute();
//                            txExecutor.go();
//                            txExecutor.finalization();
//                            final EthTransactionReceipt receipt = txExecutor.getReceipt();
//                            executeTx.setEthTransactionReceipt(receipt);
//                        } catch (Exception e) {
//                            logger.warn(String.format("EthParallelTransactionsExecutor execute %s error!", executeTx), e);
//                        }
//                        //System.out.println(sb.toString());
//
//                        return null;
//                    }; // end function
//
//                    Function<ExecuteNode.ReRunTaskContextX, Void> reExecuteFun = reRunTaskContext -> {
//                        reactorNotify.add(reRunTaskContext);
//                        return null;
//                    };
//
//
//
//                    long seq = eventRingBuffer.next();
//                    try {
//                        EthParallelTransactionsExecutor.ExecuteEvent executeEvent = eventRingBuffer.get(seq);
//                        executeEvent.executeNode = executeNode;
//                        executeEvent.txExecuteFun = txExecuteFun;
//                        executeEvent.eventRingBuffer = eventRingBuffer;
//                        executeEvent.finishCondition = reuseCountDownLatch;
//                        executeEvent.reExecuteFun = reExecuteFun;
//                    } catch (Exception e) {
//                        //logger.warn("ConsensusPayload publicEvent error!", e);
//                        throw new RuntimeException("ConsensusPayload publicEvent error!", e);
//                    } finally {
//                        eventRingBuffer.publish(seq);
//                    }
//
//
//                    //executeNode.executeNodeX(eventRingBuffer, reuseCountDownLatch, txExecuteFun, reExecuteFun);
//                }
//
//                try {
//                    reuseCountDownLatch.await();
//                } catch (Exception e) {
//                    logger.warn("reuseCountDownLatch.await() error!", e);
//                }
//                //System.out.println("finish execute " + i + " exeRoot, total node size:" + totalSize);
//                //i++;
//            } catch (Exception e) {
//                logger.warn(String.format("EthParallelTransactionsExecutor ExecuteRoot  %s error!", executeRoot), e);
//            } finally {
//                executeRoot.finishExecute();
//            }
//        }
//
//        for (EthTransaction tx: txs) {
//            result.add(tx.getEthTransactionReceipt());
//        }
//
//        //long dagExeEnd = System.currentTimeMillis();
//
//        //logger.debug("[{}]block[number={}, txs.size={}], exe total cost[{}ms],  dag generate use[{}ms], dag exe use[{}ms]" , Thread.currentThread().getName(), block.getNumber(), block.getTransactionsList().length, (dagExeEnd - dagGenStart), (dagGenEnd - dagGenStart), (dagExeEnd - dagExeStart));
//
//        return result;
//    }

    public static void DAGGenerate() {
        List<EthTransaction> ethTransactions = generate2_1();

        for (int i = 0; i < 1; i++) {
            long star = System.currentTimeMillis();
            //GenerateParallelExecutable executableDAG = new SimpleLevelDagGenerate();
            //GenerateParallelExecutable executableDAG = new ParallelLevelDagGenerate();
            GenerateParallelExecutable executableDAG = new SimpleLimitLevelDagGenerate();
            ParallelExecutable parallelExecutable = executableDAG.generate(ethTransactions);
            long end = System.currentTimeMillis();
            System.out.println(parallelExecutable.getOrderExecute().size());

            System.out.println("use:" + (end - star) + " mills");

        }
    }

    public static void main(String[] args) {
        ExecutableGeneratorImpl generator = new ExecutableGeneratorImpl();
        List<EthTransaction> ethTransactions = generate2_2();
        generator.generate(ethTransactions.toArray(new EthTransaction[ethTransactions.size()]));

        ethTransactions = generate2_3();
        generator.generate(ethTransactions.toArray(new EthTransaction[ethTransactions.size()]));
        System.out.println("hehe!");
    }

    public static List<EthTransaction> generate1() {
        List<EthTransaction> ethTransactions = new ArrayList<>(20000);

        String tempUUID = UUID.randomUUID().toString();


        for (int i = 0; i < 13000; i++) {
            Set<ByteArrayWrapper> executeStates = new HashSet<>();


            if (i == 0) {
                executeStates.add(new ByteArrayWrapper(tempUUID.getBytes()));
                executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));
                executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));
            } else {
                executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));
                executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));
                executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));
            }
            //transaction.setExecuteStates(executeStates);
            ethTransactions.add(new EthTransaction(executeStates));
        }

        for (int i = 0; i < 7000; i++) {
            Set<ByteArrayWrapper> executeStates = new HashSet<>();
            //           executeStates.add(new ByteArrayWrapper(tempUUID.toString().getBytes()));
            executeStates.add(new ByteArrayWrapper("same state".getBytes()));
            executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));
            executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));
            ethTransactions.add(new EthTransaction(executeStates));
        }
        return ethTransactions;
    }

    public static List<EthTransaction> generate2_1() {
        List<EthTransaction> ethTransactions = new ArrayList<>(20000);

        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("a".getBytes()), new ByteArrayWrapper("b".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("a".getBytes()), new ByteArrayWrapper("b".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("c".getBytes()), new ByteArrayWrapper("c".getBytes())))));
        //ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("c".getBytes()), new ByteArrayWrapper("d".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("e".getBytes()), new ByteArrayWrapper("d".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("e".getBytes()), new ByteArrayWrapper("f".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("g".getBytes()), new ByteArrayWrapper("f".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("g".getBytes()), new ByteArrayWrapper("1".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("g".getBytes()), new ByteArrayWrapper("2".getBytes())))));

        //ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("c".getBytes()), new ByteArrayWrapper("d".getBytes())))));

        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("g".getBytes()), new ByteArrayWrapper("3".getBytes())))));
        return ethTransactions;
    }

    public static List<EthTransaction> generate2_2() {
        List<EthTransaction> ethTransactions = new ArrayList<>(20000);

        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("a".getBytes()), new ByteArrayWrapper("b".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("a".getBytes()), new ByteArrayWrapper("b".getBytes())))));
        //ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("c".getBytes()), new ByteArrayWrapper("d".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("e".getBytes()), new ByteArrayWrapper("f".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>()));


        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("e".getBytes()), new ByteArrayWrapper("d".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>()));

        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("g".getBytes()), new ByteArrayWrapper("f".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("g".getBytes()), new ByteArrayWrapper("1".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("g".getBytes()), new ByteArrayWrapper("2".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("c".getBytes()), new ByteArrayWrapper("c".getBytes())))));

        //ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("c".getBytes()), new ByteArrayWrapper("d".getBytes())))));

        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("g".getBytes()), new ByteArrayWrapper("3".getBytes())))));
        return ethTransactions;
    }

    public static List<EthTransaction> generate2_3() {
        List<EthTransaction> ethTransactions = new ArrayList<>(20000);
        ethTransactions.add(new EthTransaction(new HashSet<>()));


        ethTransactions.add(new EthTransaction(new HashSet<>()));
        ethTransactions.add(new EthTransaction(new HashSet<>()));

        //ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("c".getBytes()), new ByteArrayWrapper("d".getBytes())))));

        return ethTransactions;
    }

    public static List<EthTransaction> generate3() {
        List<EthTransaction> ethTransactions = new ArrayList<>(20000);

        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("a".getBytes()), new ByteArrayWrapper("e".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("a".getBytes()), new ByteArrayWrapper("f".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("d".getBytes()), new ByteArrayWrapper("e".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("a".getBytes()), new ByteArrayWrapper("b".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("e".getBytes()), new ByteArrayWrapper("f".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("c".getBytes()), new ByteArrayWrapper("d".getBytes())))));
        ethTransactions.add(new EthTransaction(new HashSet<>(Arrays.asList(new ByteArrayWrapper("a".getBytes()), new ByteArrayWrapper("d".getBytes())))));
        return ethTransactions;
    }


    public static List<EthTransaction> generate4() {
        List<EthTransaction> ethTransactions = new ArrayList<>(20000);


        for (int i = 0; i < 15000; i++) {
            Set<ByteArrayWrapper> executeStates = new HashSet<>();
            //           executeStates.add(new ByteArrayWrapper(tempUUID.toString().getBytes()));
            executeStates.add(new ByteArrayWrapper("same1 state".getBytes()));
            executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));
            executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));
            ethTransactions.add(new EthTransaction(executeStates));
        }

        for (int i = 0; i < 10000; i++) {
            Set<ByteArrayWrapper> executeStates = new HashSet<>();

            executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));
            executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));
            executeStates.add(new ByteArrayWrapper(UUID.randomUUID().toString().getBytes()));

            //transaction.setExecuteStates(executeStates);
            ethTransactions.add(new EthTransaction(executeStates));
        }

        return ethTransactions;
    }
}
