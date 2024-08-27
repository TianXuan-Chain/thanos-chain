package com.thanos.chain.executor;

import com.thanos.chain.consensus.hotstuffbft.store.DoubleSpendCheck;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * AbstractTransactionsExecutor.java description：
 *
 * @Author laiyiyu create on 2020-09-02 10:06:49
 */
public abstract class AbstractTransactionsExecutor {

    static final Logger logger = LoggerFactory.getLogger("executor");

    StateLedger stateLedger;

    DoubleSpendCheck doubleSpendCheck;

    public AbstractTransactionsExecutor(StateLedger stateLedger) {
        this.stateLedger = stateLedger;
        this.doubleSpendCheck = stateLedger.consensusChainStore.doubleSpendCheck;
    }

    public abstract List<EthTransactionReceipt> execute(Block block);
}
