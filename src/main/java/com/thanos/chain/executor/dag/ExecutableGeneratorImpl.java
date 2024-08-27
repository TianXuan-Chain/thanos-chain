package com.thanos.chain.executor.dag;


import com.thanos.chain.ledger.model.eth.EthTransaction;

import java.util.LinkedList;
import java.util.List;


/**
 * 类LevelDAGGeneratorImpl.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-20 13:45:23
 */
public class ExecutableGeneratorImpl implements ExecutableGenerator {

    private static final int LEVEL_DEEP = 100;

    //do not change the value, unless you know how to update the code
    private static final int MAX_PARALLEL_LEVEL = 1;

    @Override
    public SerialExecutables generate(EthTransaction[] txs) {
        int remainParseSize = txs.length;
        int readFromOffset = 0;
        List<ExecuteRoot> executeRoots = new LinkedList<>();
        while(true) {
            if (remainParseSize == 0) break;
            ExecuteRoot executeRoot = LevelDAG.newInstance(txs, MAX_PARALLEL_LEVEL, LEVEL_DEEP, readFromOffset).generateExecuteRoot();
            executeRoots.add(executeRoot);
            int totlaNum = executeRoot.getTotalParallelNodeNum() + executeRoot.getSerialTransactions().size();
            remainParseSize -= totlaNum;
            readFromOffset += totlaNum;
        }
        return SerialExecutables.newInstance(executeRoots);
    }
}
