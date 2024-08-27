package com.thanos.chain.initializer;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.consensus.hotstuffbft.ConsensusProvider;
import com.thanos.chain.txpool.TxnManager;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.gateway.GatewayFacade;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.network.NetInvoker;
import com.thanos.chain.network.peer.PeerManager;

/**
 * ComponentInitializer.java description：
 *
 * @Author laiyiyu create on 2020-09-15 15:46:13
 */
public class ComponentInitializer {

    public static void init(SystemConfig systemConfig) {
        // network
        NetInvoker netInvoker = new NetInvoker(new PeerManager(systemConfig));

        //ledger
        ConsensusChainStore consensusChainStore = new ConsensusChainStore(systemConfig, false);
        StateLedger stateLedger = new StateLedger(systemConfig, netInvoker, consensusChainStore, false);

        //txnManager
        TxnManager txnManager = new TxnManager(systemConfig.getMaxPackSize(), systemConfig.getPoolLimit(), systemConfig.comingQueueSize(), systemConfig.dsCheck(), stateLedger.consensusChainStore);

//        //gateway
        GatewayFacade gatewayFacade = new GatewayFacade(systemConfig, stateLedger, txnManager);
        gatewayFacade.start();

//        // layer2 consensus
        ConsensusProvider consensusProvider = new ConsensusProvider(systemConfig, netInvoker, stateLedger, txnManager);
        consensusProvider.start();
    }
}
