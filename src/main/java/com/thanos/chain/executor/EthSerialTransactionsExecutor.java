package com.thanos.chain.executor;

import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.contract.ca.filter.GlobalFilterChain;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.chain.storage.db.Repository;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * EthSerialTransactionsExecutor.java description：
 *
 * @Author laiyiyu create on 2020-08-24 10:59:34
 */
public class EthSerialTransactionsExecutor extends AbstractTransactionsExecutor {

    public EthSerialTransactionsExecutor(StateLedger stateLedger) {
        super(stateLedger);
    }

    public List<EthTransactionReceipt> execute(Block block) {

        logger.debug("EthSerialTransactionsExecutor start execute::{}", block);

        List<EthTransactionReceipt> receipts = new ArrayList<>(block.getTransactionsList().length);
        final GlobalFilterChain globalFilterChain = this.stateLedger.consensusChainStore.globalFilterChain;

        //int i = 0;
        for (EthTransaction tx : block.getTransactionsList()) {

            if (!tx.isDsCheckValid()) {
                tx.setErrEthTransactionReceipt("current tx is ds tx!!!");
                receipts.add(tx.getEthTransactionReceipt());
                continue;
            }

            ProcessResult filterResult = globalFilterChain.filter(tx);
            if (!filterResult.isSuccess()) {
                tx.setErrEthTransactionReceipt(filterResult.getErrMsg());
                receipts.add(tx.getEthTransactionReceipt());
                continue;
            }

            Repository txTrack =  stateLedger.rootRepository.startTracking();
            EthTransactionExecutor executor = new EthTransactionExecutor(
                    tx, txTrack, stateLedger.programInvokeFactory, block)
                    .withConfig(this.stateLedger.systemConfig);

            executor.init();
            executor.execute();
            executor.go();
            final EthTransactionReceipt receipt = executor.getReceipt();
            tx.setEthTransactionReceipt(receipt);
            receipts.add(receipt);
            executor.finalization();

            //i++;
        }

        return receipts;


    }
}
