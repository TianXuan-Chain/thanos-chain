package com.thanos.chain.executor.dag;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import io.netty.util.Recycler;

import java.util.ArrayList;
import java.util.List;

/**
 * 类TopLevel.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-20 17:16:03
 */
public class ExecuteRoot {

    private static final Recycler<ExecuteRoot> rootRECYCLER = new Recycler<ExecuteRoot>() {
        @Override
        protected ExecuteRoot newObject(Handle<ExecuteRoot> handle) {
            return new ExecuteRoot(handle);
        }
    };

    static ExecuteRoot newInstance(List<EthTransaction> serialTransactions, List<ExecuteNode> executeRootNodes, List<Level> allLevel, int totalNodeNum) {
        ExecuteRoot executeRoot = rootRECYCLER.get();
        executeRoot.reuse(serialTransactions, executeRootNodes, allLevel, totalNodeNum);
        return executeRoot;
    }

    private List<ExecuteNode> executeRootNodes;

    List<EthTransaction> serialTransactions;

    private List<Level> allLevel;

    private Recycler.Handle<ExecuteRoot> handle;

    private int totalNodeNum;

    public ExecuteRoot(Recycler.Handle<ExecuteRoot> handle) {
        this.handle = handle;
    }

    public List<ExecuteNode> getExecuteRootNodes() {
        return this.executeRootNodes;
    }

    public List<EthTransaction> getSerialTransactions() {
        return serialTransactions;
    }

    public void finishExecute() {
        recycle();
    }

    private void recycle() {
        //avoid memory leak
        executeRootNodes = null;
        serialTransactions = null;

        for (Level level: allLevel) {
            level.recycle();
        }
        allLevel = null;

        handle.recycle(this);
    }

    public int getTotalParallelNodeNum() {
        return totalNodeNum;
    }

    public int getLevelSize() {
        return this.allLevel == null? -1: this.allLevel.size();
    }



    private void reuse(List<EthTransaction> serialTransactions, List<ExecuteNode> executeRootNodes, List<Level> allLevel, int totalNodeNum) {
        this.executeRootNodes = executeRootNodes;
        this.totalNodeNum = totalNodeNum;
        this.allLevel = allLevel;
        this.serialTransactions = serialTransactions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append("\n");
        for (Level level: allLevel) {
            sb.append(level).append(";\n");
        }
        sb.append("]");

        return sb.toString();
    }
}
