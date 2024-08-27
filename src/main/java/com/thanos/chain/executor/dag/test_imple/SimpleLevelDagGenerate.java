package com.thanos.chain.executor.dag.test_imple;


import com.thanos.chain.ledger.model.eth.EthTransaction;

import java.util.List;

/**
 * 类SimpleLevelDagGenerate.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-19 17:09:40
 */
public class SimpleLevelDagGenerate extends LevelGenerate {


    @Override
    protected int findUpdateLevelIndex(List<Level> levels, EthTransaction tx) {
        for (int i = 0; i < levels.size(); i++) {
            if (!containOne(tx.getExecuteStates(), levels.get(i).currentLevelStates)) {
                return i;
            }
        }
        return -1;
    }
}
