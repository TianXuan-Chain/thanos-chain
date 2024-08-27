package com.thanos.chain.executor.dag.test_imple;


import com.thanos.common.utils.ByteArrayWrapper;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * 类Level.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-19 17:11:54
 */
class Level {

    final LinkedList<Integer> parallelExecuteTxIndexes = new LinkedList<>();
    final Set<ByteArrayWrapper> currentLevelStates = new HashSet<>();

    public Level() {

    }

    public void addCurrentLevel(int index, Set<ByteArrayWrapper> states) {
        parallelExecuteTxIndexes.addLast(index);
        currentLevelStates.addAll(states);
    }

}
