package com.thanos.chain.executor.dag;


import com.thanos.chain.ledger.model.eth.EthTransaction;

/**
 * 类LevelDAGGenerator.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-20 13:44:12
 */
public interface ExecutableGenerator {

    SerialExecutables generate(EthTransaction[] txs);
}
