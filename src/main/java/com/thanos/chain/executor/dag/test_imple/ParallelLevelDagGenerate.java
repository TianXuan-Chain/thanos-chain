package com.thanos.chain.executor.dag.test_imple;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.utils.ThanosThreadFactory;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

/**
 * 类ParallelLevelDagGenerate.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-19 17:21:50
 */
public class ParallelLevelDagGenerate extends LevelGenerate {

    private static final int CUT_SIZE = 50;

    //private static final int PROCESSOR_NUM = 8;
    private static final int PROCESSOR_NUM = Runtime.getRuntime().availableProcessors();

    static ThreadPoolExecutor findUpdateLevelIndexExecutor = new ThreadPoolExecutor(PROCESSOR_NUM, PROCESSOR_NUM, 2, TimeUnit.SECONDS, new LinkedTransferQueue<>(), new ThanosThreadFactory("dag_find_update_level_index"));

    @Override
    protected int findUpdateLevelIndex(List<Level> levels, EthTransaction tx) {

        if (levels.size() == 1999) {
            System.out.println();
        }

        // [startIndex, endIndex)
        List<Pair<Integer /* startIndex */, Integer /* endIndex */>> partitionIndexes = generateTaskIndexes(levels);
        List<List<Pair<Integer /* startIndex */, Integer /* endIndex */>>> parallelIndexes = generateParallelFindIndex(partitionIndexes);

        for (List<Pair<Integer /* startIndex */, Integer /* endIndex */>> executeFindPair : parallelIndexes) {
            List<Future<Integer>> futures = new ArrayList<>(executeFindPair.size());
            for (Pair<Integer /* startIndex */, Integer /* endIndex */> pair : executeFindPair) {
                Future<Integer> future = findUpdateLevelIndexExecutor.submit(() -> {
                    for (int i = pair.getLeft(); i < pair.getRight(); i++) {
                        //System.out.println(pair);
                        if (!containOne(tx.getExecuteStates(), levels.get(i).currentLevelStates)) {
                            return i;
                        }
                    }
                    return -1;
                });
                futures.add(future);
            }

//            List<CompletableFuture<Integer>> futures = executeFindPair.stream()
//                    .map(pair -> CompletableFuture.supplyAsync(() -> {
//                        for (int i = pair.getLeft(); i < pair.getRight(); i++) {
//                            if (!containOne(tx.getExecuteStates(), levels.get(i).currentLevelStates)) {
//                                return i;
//                            }
//                        }
//                        return -1;
//                    }, findUpdateLevelIndexExecutor))
//                    .collect(toList());

            for (Future<Integer> f : futures) {
                try {
                    int result = f.get();
                    if (result != -1) {
                        return result;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        return -1;
    }

    private List<Pair<Integer, Integer>> generateTaskIndexes(List<Level> levels) {
        int remainingLevelSize = levels.size();
        int start = 0;
        int end = 0;
        int partitionLevelSize;
        List<Pair<Integer, Integer>> pairs = new ArrayList<>((remainingLevelSize / CUT_SIZE) + 1);
        while (true) {
            partitionLevelSize = remainingLevelSize > CUT_SIZE? CUT_SIZE : remainingLevelSize;
            start = end;
            end = end + partitionLevelSize;
            pairs.add(Pair.of(start, end));
            remainingLevelSize = remainingLevelSize - partitionLevelSize;

            if (remainingLevelSize == 0) break;
        }
        return pairs;
    }

    private List<List<Pair<Integer /* startIndex */, Integer /* endIndex */>>> generateParallelFindIndex(List<Pair<Integer, Integer>> pairs) {
        List<List<Pair<Integer /* startIndex */, Integer /* endIndex */>>> result = new ArrayList<>((pairs.size() / PROCESSOR_NUM) + 1);
        int remainingTaskSize = pairs.size();
        Iterator<Pair<Integer /* startIndex */, Integer /* endIndex */>> iter = pairs.iterator();
        int partitionTaskSize;
        while (true) {
            partitionTaskSize = remainingTaskSize > PROCESSOR_NUM? PROCESSOR_NUM : remainingTaskSize;
            List<Pair<Integer /* startIndex */, Integer /* endIndex */>> tasks = new ArrayList<>(PROCESSOR_NUM);
            for (int i =  0; i < partitionTaskSize; i++) {
                tasks.add(iter.next());
                iter.remove();
            }
            result.add(tasks);
            remainingTaskSize -= partitionTaskSize;
            if (remainingTaskSize == 0) break;

        }
        return result;
    }
}
