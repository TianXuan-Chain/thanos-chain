package com.thanos.chain.executor.dag.test_imple;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.utils.ByteArrayWrapper;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 类LevelGenerate.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-19 17:14:09
 */
public abstract class LevelGenerate implements GenerateParallelExecutable {

    @Override
    public ParallelExecutable generate(List<EthTransaction> txs) {
        assert txs.size() != 0;
        List<Level> orderLevels = generateOrderLevels(txs);
        return generateParallelExecutable(orderLevels, txs);
    }

    private List<Level> generateOrderLevels(List<EthTransaction> ethTransactions) {
        List<Level> levels = new LinkedList<>();

        for (int i = 0; i < ethTransactions.size(); i++) {
            boolean needCreate = true;
            EthTransaction tx = ethTransactions.get(i);

            if (!CollectionUtils.isEmpty(levels)) {

                int updateLevelIndex = findUpdateLevelIndex(levels, tx);

                if (updateLevelIndex != -1) {
                    Level level = levels.get(updateLevelIndex);
                    level.addCurrentLevel(i, tx.getExecuteStates());
                    needCreate = false;
                }
            }

            if (needCreate) {
                Level level = new Level();
                level.addCurrentLevel(i, tx.getExecuteStates());
                levels.add(level);
            }
        }
        return levels;
    }

    protected abstract int findUpdateLevelIndex(List<Level> levels, EthTransaction tx);

    private ParallelExecutable generateParallelExecutable(List<Level> orderLevels, List<EthTransaction> txs) {
        List<ParallelExecutable.ParallelTX> orderExecute = new ArrayList<>(orderLevels.size());
        for (Level level : orderLevels) {
            List<EthTransaction> parallelTXs = new ArrayList<>(level.parallelExecuteTxIndexes.size());
            for (Integer index : level.parallelExecuteTxIndexes) {
                parallelTXs.add(txs.get(index));
            }

            ParallelExecutable.ParallelTX parallelTX = new ParallelExecutable.ParallelTX(parallelTXs);
            orderExecute.add(parallelTX);
        }
        return new ParallelExecutable(orderExecute);
    }


    public static boolean containOne(Set<ByteArrayWrapper> firstStates, Set<ByteArrayWrapper> secondStates) {
        for (ByteArrayWrapper first : firstStates) {
            if (secondStates.contains(first)) return true;
        }
        return false;
    }
}
