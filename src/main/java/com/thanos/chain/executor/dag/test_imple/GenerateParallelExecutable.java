package com.thanos.chain.executor.dag.test_imple;


import com.thanos.chain.ledger.model.eth.EthTransaction;

import java.util.List;

/**
 * 类GenerateParallelExecutable.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-19 17:04:26
 */
public interface GenerateParallelExecutable {
    ParallelExecutable generate(List<EthTransaction> txs);
}
