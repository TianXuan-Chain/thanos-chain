package com.thanos.chain.executor.dag;

import com.lmax.disruptor.RingBuffer;
import com.thanos.chain.executor.EthParallelTransactionsExecutor;
import io.netty.util.Recycler;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * 类Node.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-19 19:32:57
 */
public class ExecuteNode implements Comparable<ExecuteNode> {

    private static final Recycler<ExecuteNode> nodeRECYCLER = new Recycler<ExecuteNode>() {
        @Override
        protected ExecuteNode newObject(Handle<ExecuteNode> handle) {
            return new ExecuteNode(handle);
        }
    };

    static ExecuteNode newInstance(int selfIndex, Level level, Set<ExecuteState> ownStates) {
        ExecuteNode executeNode = nodeRECYCLER.get();
        executeNode.reuse(selfIndex, level, ownStates);
        return executeNode;
    }

    public volatile int txIndex;

    Level level;

    // 依赖，上一层级： this.level.levelIndex - 1
    final Set<ExecuteNode> dependExecuteNodes = new HashSet<>();

    // 被依赖，下一层级： this.level.levelIndex + 1
    final Set<ExecuteNode> beDependExecuteNodes = new HashSet<>();

    // for display test
    // Set<ExecuteState> ownStates = new HashSet<>();

    Recycler.Handle<ExecuteNode> handle;

    ReRunTaskContext reRunTaskContext;
    ReRunTaskContextX reRunTaskContextX;

    volatile boolean hasExecute;

    public ExecuteNode(Recycler.Handle<ExecuteNode> handle) {
        this.handle = handle;
        this.reRunTaskContext = new ReRunTaskContext();
        this.reRunTaskContextX = new ReRunTaskContextX();
    }

    public void reuse(int selfIndex, Level level, Set<ExecuteState> ownStates) {
        this.txIndex = selfIndex;
        //this.ownStates = ownStates;
        this.level = level;
        this.level.exeCount.getAndIncrement();
        this.level.nextParallelLevelAwaitCondition.reuse(this.level.exeCount.get());
        this.hasExecute = false;
        //level.addNode(this);
        for (ExecuteState executeState: ownStates) {
            executeState.addOwner(this);
        }
    }

    public void addDepend(ExecuteNode depend) {
        assert this.level.levelIndex - depend.level.levelIndex == 1;
        dependExecuteNodes.add(depend);
        depend.beDependExecuteNodes.add(this);
    }

    public void addBeDepend(ExecuteNode depend) {
        assert depend.level.levelIndex - this.level.levelIndex == 1;
        beDependExecuteNodes.add(depend);
        depend.dependExecuteNodes.add(this);
    }

    //thread unsafe
    public void executeNode(Executor executor, ReuseCountDownLatch finishCondition, Function<Integer, Void> txExecuteFun, Function<ReRunTaskContext, Void> reExecuteFun) {
        if (!hasExecute) {
            hasExecute = true;
            doExecute(executor, finishCondition, txExecuteFun, reExecuteFun);
        }
    }

    public void  executeNodeX(RingBuffer<EthParallelTransactionsExecutor.ExecuteEvent> eventRingBuffer, ReuseCountDownLatch finishCondition, Function<Integer, Void> txExecuteFun, Function<ReRunTaskContextX, Void> reExecuteFun) {
        if (!hasExecute) {
            hasExecute = true;
            doExecuteX(eventRingBuffer, finishCondition, txExecuteFun, reExecuteFun);
        }
    }

    public void finishExecute(Executor executor, ReuseCountDownLatch finishCondition, Function<Integer, Void> txExecuteFun, Function<ReRunTaskContext, Void> reExecuteFun) {
        // 触发被依赖的的节点
        finishCondition.releaseLast();
        this.level.oneNodeFinishExecute();
        Iterator<ExecuteNode> iterator = beDependExecuteNodes.iterator();
        while (iterator.hasNext()) {
            ExecuteNode beDependNode = iterator.next();
            //System.out.println(" beDependNode [" + beDependNode.txIndex + "] was remove by node[" + this.txIndex + "]");
            removeTrigger(beDependNode, executor, finishCondition, txExecuteFun, reExecuteFun);
            iterator.remove();
        }
        recycle();
    }

    public void finishExecuteX(RingBuffer<EthParallelTransactionsExecutor.ExecuteEvent> eventRingBuffer, ReuseCountDownLatch finishCondition, Function<Integer, Void> txExecuteFun, Function<ReRunTaskContextX, Void> reExecuteFun) {
        // 触发被依赖的的节点
        finishCondition.releaseLast();
        this.level.oneNodeFinishExecute();
        Iterator<ExecuteNode> iterator = beDependExecuteNodes.iterator();
        while (iterator.hasNext()) {
            ExecuteNode beDependNode = iterator.next();
            //System.out.println(" beDependNode [" + beDependNode.txIndex + "] was remove by node[" + this.txIndex + "]");
            removeTriggerX(eventRingBuffer, beDependNode, finishCondition, txExecuteFun, reExecuteFun);
            iterator.remove();
        }
        recycle();
    }

    private void removeTrigger(ExecuteNode beDependNode, Executor executor, ReuseCountDownLatch finishCondition, Function<Integer, Void> txExecuteFun, Function<ReRunTaskContext, Void> reExecuteFun) {
        synchronized (beDependNode.dependExecuteNodes) {
            beDependNode.dependExecuteNodes.remove(this);
            if (beDependNode.dependExecuteNodes.size() == 0 && !beDependNode.hasExecute) {
                //System.out.println(" beDependNode [" + beDependNode.txIndex + "] was trigger by node[" + this.txIndex + "]");


                //beDependNode.executeNode(executor, finishCondition, txExecuteFun, reExecuteFun);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        beDependNode.executeNode(executor, finishCondition, txExecuteFun, reExecuteFun);
                    }
                });
            }
        }
    }

    private void removeTriggerX(RingBuffer<EthParallelTransactionsExecutor.ExecuteEvent> eventRingBuffer, ExecuteNode beDependNode, ReuseCountDownLatch finishCondition, Function<Integer, Void> txExecuteFun, Function<ReRunTaskContextX, Void> reExecuteFun) {
        synchronized (beDependNode.dependExecuteNodes) {
            beDependNode.dependExecuteNodes.remove(this);
            if (beDependNode.dependExecuteNodes.size() == 0 && !beDependNode.hasExecute) {
                //System.out.println(" beDependNode [" + beDependNode.txIndex + "] was trigger by node[" + this.txIndex + "]");


                long seq = eventRingBuffer.next();
                try {
                    EthParallelTransactionsExecutor.ExecuteEvent executeEvent = eventRingBuffer.get(seq);
                    executeEvent.executeNode = beDependNode;
                    executeEvent.txExecuteFun = txExecuteFun;
                    executeEvent.eventRingBuffer = eventRingBuffer;
                    executeEvent.finishCondition = finishCondition;
                    executeEvent.reExecuteFun = reExecuteFun;
                } catch (Exception e) {
                    //logger.warn("ConsensusPayload publicEvent error!", e);
                    throw new RuntimeException("ConsensusPayload publicEvent error!", e);
                } finally {
                    eventRingBuffer.publish(seq);
                }
                //beDependNode.executeNodeX(eventRingBuffer, finishCondition, txExecuteFun, reExecuteFun);
            }
        }
    }

    private void recycle() {
        //dependExecuteNodes.clear();
        this.reRunTaskContext.reset(null,  null,null, null);
        handle.recycle(this);
    }

    private void doExecute(Executor executor, ReuseCountDownLatch finishCondition, Function<Integer, Void> txExecuteFun, Function<ReRunTaskContext, Void> reExecuteFun) {
        if (this.level.isPreParallelLevelAllNodeFinish()) {
//            Runnable task = new PriorityTask(this.level.levelIndex) {
//                @Override
//                public void run() {
//                    try {
//                        txExecuteFun.apply(txIndex);
//                    } finally {
//                        finishExecute(executor, finishCondition, txExecuteFun, reExecuteFun);
//                    }
//                }
//            };
//
//            executor.execute(task);

            try {
                txExecuteFun.apply(txIndex);
            } finally {
                finishExecute(executor, finishCondition, txExecuteFun, reExecuteFun);
            }
        } else {
            this.hasExecute = false;
            this.reRunTaskContext.reset(executor, finishCondition, txExecuteFun, reExecuteFun);
            reExecuteFun.apply(this.reRunTaskContext);
        }
    }

    private void doExecuteX(RingBuffer<EthParallelTransactionsExecutor.ExecuteEvent> eventRingBuffer, ReuseCountDownLatch finishCondition, Function<Integer, Void> txExecuteFun, Function<ReRunTaskContextX, Void> reExecuteFun) {
        if (this.level.isPreParallelLevelAllNodeFinish()) {
            try {
                txExecuteFun.apply(txIndex);
            } finally {
                finishExecuteX(eventRingBuffer, finishCondition, txExecuteFun, reExecuteFun);
            }


//            Runnable task = new PriorityTask(this.level.levelIndex) {
//                @Override
//                public void run() {
//                    try {
//                        txExecuteFun.apply(txIndex);
//                    } finally {
//                        finishExecuteX(eventRingBuffer, finishCondition, txExecuteFun, reExecuteFun);
//                    }
//                }
//            };
//
//            executor.execute(task);




        } else {
            this.hasExecute = false;
            this.reRunTaskContextX.reset(eventRingBuffer, finishCondition, txExecuteFun, reExecuteFun);
            reExecuteFun.apply(this.reRunTaskContextX);
        }
    }

    public Level getLevel() {
        return level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecuteNode executeNode = (ExecuteNode) o;
        return txIndex == executeNode.txIndex;
    }

    @Override
    public int hashCode() {

        return Objects.hash(txIndex);
    }

    @Override
    public int compareTo(ExecuteNode o) {
        return txIndex - o.txIndex;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("node[index=").append(txIndex).append(", level= ").append(level.levelIndex).append("] depend nodes[");
//
//
//        int i = 0;
//        for (ExecuteNode executeNode: this.dependExecuteNodes) {
//
//
//            sb.append(executeNode.txIndex);
//
//            if (dependExecuteNodes.size() - 1 != i)   sb.append(",");
//            i++;
//        }
//
//        sb.append("] and beDepend by nodes[");
//
//        int j = 0;
//        for (ExecuteNode executeNode: this.beDependExecuteNodes) {
//            sb.append(executeNode.txIndex);
//
//            if (beDependExecuteNodes.size() - 1 != j)   sb.append(",");
//            j++;
//        }

       sb.append("]");


       return sb.toString();
    }

    public class ReRunTaskContext implements Comparable<ReRunTaskContext> {

        Executor executor;

        Function<Integer, Void> exeFun;

        Function<ReRunTaskContext, Void> reExecuteFun;

        ReuseCountDownLatch finishCondition;

        ExecuteNode executeNode;

        ReRunTaskContext() {
            executeNode = ExecuteNode.this;
        }

        public Executor getExecutor() {
            return executor;
        }

        public Function<Integer, Void> getExeFun() {
            return exeFun;
        }

        public Function<ReRunTaskContext, Void> getReExecuteFun() {
            return reExecuteFun;
        }

        public ExecuteNode getExecuteNode() {
            return executeNode;
        }

        public ReuseCountDownLatch getFinishCondition() {
            return finishCondition;
        }

        public void reset(Executor executor, ReuseCountDownLatch finishCondition, Function<Integer, Void> exeFun, Function<ReRunTaskContext, Void> reExecuteFun) {
            this.executor = executor;
            this.finishCondition = finishCondition;
            this.exeFun = exeFun;
            this.reExecuteFun = reExecuteFun;
        }

        // 小堆排序
        @Override
        public int compareTo(ReRunTaskContext o) {
            return executeNode.level.levelIndex - o.executeNode.level.levelIndex;
        }

        @Override
        public String toString() {
            return "ReRunTaskContext{" +
                    "executeNode=" + executeNode +
                    '}' + " was re execute";
        }
    }

    public class ReRunTaskContextX {

        RingBuffer<EthParallelTransactionsExecutor.ExecuteEvent> eventRingBuffer;

        Function<Integer, Void> exeFun;

        Function<ReRunTaskContextX, Void> reExecuteFun;

        ReuseCountDownLatch finishCondition;

        ExecuteNode executeNode;

        ReRunTaskContextX() {
            executeNode = ExecuteNode.this;
        }

        public RingBuffer<EthParallelTransactionsExecutor.ExecuteEvent> getEventRingBuffer() {
            return eventRingBuffer;
        }

        public Function<Integer, Void> getExeFun() {
            return exeFun;
        }

        public Function<ReRunTaskContextX, Void> getReExecuteFun() {
            return reExecuteFun;
        }

        public ExecuteNode getExecuteNode() {
            return executeNode;
        }

        public ReuseCountDownLatch getFinishCondition() {
            return finishCondition;
        }

        public void reset(RingBuffer<EthParallelTransactionsExecutor.ExecuteEvent> eventRingBuffer, ReuseCountDownLatch finishCondition, Function<Integer, Void> exeFun, Function<ReRunTaskContextX, Void> reExecuteFun) {
            this.eventRingBuffer = eventRingBuffer;
            this.finishCondition = finishCondition;
            this.exeFun = exeFun;
            this.reExecuteFun = reExecuteFun;
        }



        @Override
        public String toString() {
            return "ReRunTaskContextX{" +
                    "executeNode=" + executeNode +
                    '}' + " was re execute";
        }
    }

    static abstract class PriorityTask implements Runnable, Comparable<PriorityTask> {

        private Integer levelIndex;

        public PriorityTask(Integer levelIndex) {
            this.levelIndex = levelIndex;
        }

        @Override
        public int compareTo(PriorityTask o) {
            return this.levelIndex - o.levelIndex;
        }
    }
}
