package com.thanos.chain.consensus.hotstuffbft;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.consensus.hotstuffbft.executor.ConsensusEventExecutor;
import com.thanos.chain.consensus.hotstuffbft.store.PersistentLivenessStorage;
import com.thanos.chain.executor.GlobalExecutor;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.network.NetInvoker;
import com.thanos.chain.txpool.TxnManager;

/**
 * 类ConsensusProvider.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-04 19:32:58
 */
public class ConsensusProvider {

    SystemConfig systemConfig;

    NetInvoker netInvoker;

    StateLedger stateLedger;

    TxnManager txnManager;

    public ConsensusProvider(SystemConfig systemConfig, NetInvoker netInvoker, StateLedger stateLedger, TxnManager txnManager) {
        this.systemConfig = systemConfig;
        this.netInvoker = netInvoker;
        this.stateLedger = stateLedger;
        this.txnManager = txnManager;
    }

    public void start() {

        GlobalExecutor globalExecutor = new GlobalExecutor(stateLedger);

        ConsensusEventExecutor consensusEventExecutor = new ConsensusEventExecutor(stateLedger.consensusChainStore, this.netInvoker, globalExecutor, txnManager);

        EpochManager epochManager = new EpochManager(
                systemConfig.getMyKey(),
                new HotstuffNetInvoker(netInvoker),
                consensusEventExecutor,
                txnManager,
                new PersistentLivenessStorage(stateLedger.consensusChainStore),
                systemConfig);

        new ChainedBFT(epochManager).start();
    }
}
