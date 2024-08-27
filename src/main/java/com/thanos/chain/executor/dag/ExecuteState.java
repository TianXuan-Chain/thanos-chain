package com.thanos.chain.executor.dag;

import com.thanos.common.utils.ByteArrayWrapper;
import io.netty.util.Recycler;

import java.util.*;

/**
 * 类ExecuteState.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-19 19:34:28
 */
class ExecuteState {

    private static final Recycler<ExecuteState> stateRECYCLER = new Recycler<ExecuteState>() {
        @Override
        protected ExecuteState newObject(Handle<ExecuteState> handle) {
            return new ExecuteState(handle);
        }
    };

    static ExecuteState newInstance(ByteArrayWrapper executeState) {
        ExecuteState state = stateRECYCLER.get();
        state.reuse(executeState);
        return state;
    }

    ByteArrayWrapper executeState;

    Recycler.Handle<ExecuteState> handle;

    final Map<Integer, Set<ExecuteNode>> levelToNodeIndexes = new HashMap<>(8);

    public ExecuteState(Recycler.Handle<ExecuteState> handle) {
        this.handle = handle;
    }

    public void addOwner(ExecuteNode newExecuteNode) {
        //this.owners.add(newNode);
        Set<ExecuteNode> owns = levelToNodeIndexes.get(newExecuteNode.level.levelIndex);
        if (owns == null) {
            owns = new HashSet<>(8);
            levelToNodeIndexes.put(newExecuteNode.level.levelIndex, owns);
        }
        owns.add(newExecuteNode);

        Set<ExecuteNode> preLevelOwns = levelToNodeIndexes.get(newExecuteNode.level.levelIndex - 1);
        if (preLevelOwns != null) {
            for (ExecuteNode executeNode : preLevelOwns) {
                newExecuteNode.addDepend(executeNode);
            }
        }

        Set<ExecuteNode> nextLevelOwns = levelToNodeIndexes.get(newExecuteNode.level.levelIndex + 1);
        if (nextLevelOwns != null) {
            for (ExecuteNode executeNode : nextLevelOwns) {
                newExecuteNode.addBeDepend(executeNode);
            }
        }
    }

    public void reuse(ByteArrayWrapper executeState) {
        this.executeState = executeState;
    }

    public void recycle() {
        // Node will be reuse, avoid memory leak;
        for (Set<ExecuteNode> executeNodes : levelToNodeIndexes.values()) {
            executeNodes.clear();
        }
        levelToNodeIndexes.clear();
        handle.recycle(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecuteState that = (ExecuteState) o;
        return Objects.equals(executeState, that.executeState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executeState);
    }

    @Override
    public String toString() {
        return new String(executeState.getData());
    }
}
