package com.thanos.chain.executor.dag.test_imple;


import com.thanos.chain.ledger.model.eth.EthTransaction;

import java.util.List;

/**
 * 类SimpleLevelDagGenerate.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-19 17:09:40
 */
public class SimpleLimitLevelDagGenerate extends LevelGenerate {

    private static final int PARSE_LIMIT_LEVEL = 50;

    @Override
    protected int findUpdateLevelIndex(List<Level> levels, EthTransaction tx) {
        for (int i = 0; i < levels.size(); i++) {
            if (i >= PARSE_LIMIT_LEVEL) {
                return -1;
            }

            if (!containOne(tx.getExecuteStates(), levels.get(i).currentLevelStates)) {
                return i;
            }

        }
        return -1;
    }
}
