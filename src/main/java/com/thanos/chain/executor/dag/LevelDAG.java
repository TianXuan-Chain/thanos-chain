package com.thanos.chain.executor.dag;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import io.netty.util.Recycler;

import java.util.*;


/**
 * 类Level.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-19 19:47:17
 */
class LevelDAG {

    private static int CREATE_NEW_LEVEL = -1;
    //private static int CREATE_NEW_ROOT = -2;

    private static final Recycler<LevelDAG> levelDagRECYCLER = new Recycler<LevelDAG>() {
        @Override
        protected LevelDAG newObject(Handle<LevelDAG> handle) {
            return new LevelDAG(handle);
        }
    };

    static LevelDAG newInstance(EthTransaction[] txs, int maxParallelLevel, int maxParseLevelDeep, int readFromOffset) {
        assert txs != null && txs.length > 0;
        LevelDAG levelDAG = levelDagRECYCLER.get();
        levelDAG.reuse(txs, maxParallelLevel, maxParseLevelDeep, readFromOffset);
        return levelDAG;
    }

    private void reuse(EthTransaction[] txs, int maxParallelLevel, int maxLevelDeep, int readFromOffset) {
        this.txs = txs;
        this.maxParallelLevel = maxParallelLevel;
        this.maxLevelDeep = maxLevelDeep;
        this.readFromOffset = readFromOffset;
    }

    Recycler.Handle<LevelDAG> handle;

    private EthTransaction[] txs;

    private int maxParallelLevel;

    private int maxLevelDeep;

    private int readFromOffset;

    final public Map<ByteArrayWrapper, ExecuteState> allStateTable = new HashMap(10000);

    final public List<Level> levels = new ArrayList<>(100);

    public LevelDAG(Recycler.Handle<LevelDAG> handle) {
        this.handle = handle;
    }

    ExecuteRoot generateExecuteRoot() {
        ExecuteRoot executeRoot = doGenerate();
        recycle();
        return executeRoot;
    }

    void recycle() {
        stateRecycle();
        levels.clear();
        txs = null;
        handle.recycle(this);
    }

    private ExecuteRoot doGenerate() {
        List<ExecuteNode> rootNodes = new LinkedList<>();
        //List<ExecuteNode> display = new LinkedList<>();
        List<EthTransaction> serialTransactions = new ArrayList<>(txs.length / 8);

        int parseTxsNum = 0;
        for (int i = this.readFromOffset; i < txs.length && levels.size() < maxLevelDeep; i++) {
            EthTransaction tx = txs[i];

            if (tx.getExecuteStates().size() == 0) {
                serialTransactions.add(tx);
                continue;
            }


            parseTxsNum++;

            Level level;
            int levelIndex = findAddLevelIndex(tx);

             if (levelIndex == CREATE_NEW_LEVEL) {
                int preParallelIndex = levels.size() - 1 - maxParallelLevel;
                if (preParallelIndex < 0) {
                    level = Level.newInstance(levels.size(), null);
                } else {

                    Level preParallelLevel = levels.get(preParallelIndex);
                    level = Level.newInstance(levels.size(), preParallelLevel);
                    //preParallelLevel.resetNextNextLevelAwaitCondition();
                }

                levels.add(level);
            } else {
                level = levels.get(levelIndex);
            }

            Set<ExecuteState> executeStates = new HashSet<>(tx.getExecuteStates().size());
            for (ByteArrayWrapper state: tx.getExecuteStates()) {
                ExecuteState executeState = allStateTable.get(state);
                if (executeState == null) {
                    executeState = ExecuteState.newInstance(state);
                    allStateTable.put(state, executeState);
                }
                executeStates.add(executeState);
            }

            ExecuteNode executeNode = ExecuteNode.newInstance(i, level, executeStates);
            if (level.levelIndex == 0) {
                rootNodes.add(executeNode);
            }

//            if (levelIndex == CREATE_NEW_ROOT) {
//                break;
//            }

            //display.add(executeNode);
        }
//        for (ExecuteNode executeNode: display) {
//            System.out.println(executeNode);
//        }
        return ExecuteRoot.newInstance(serialTransactions, rootNodes, new ArrayList<>(this.levels), parseTxsNum);
    }

    private int findAddLevelIndex(EthTransaction tx) {
//        if (tx.getExecuteStates().size() == 0) {
//            return CREATE_NEW_ROOT;
//        }

        int i = 0;
        retry:
        while  (i < this.levels.size()) {
            for (ByteArrayWrapper state: tx.getExecuteStates()) {
                ExecuteState executeState = this.allStateTable.get(state);
                if (executeState != null) {
                    if (executeState.levelToNodeIndexes.containsKey(i)) {
                        i++;
                        continue retry;
                    }
                }
            }
            return i;
        }
        return CREATE_NEW_LEVEL;
    }

    private void stateRecycle() {
        for (ExecuteState executeState: allStateTable.values()) {
            executeState.recycle();
        }
        allStateTable.clear();
    }
}
