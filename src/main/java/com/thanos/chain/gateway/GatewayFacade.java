package com.thanos.chain.gateway;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.txpool.TxnManager;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;

import java.util.List;

/**
 * GatewayFacade.java description：
 *
 * @Author laiyiyu create on 2020-09-15 16:17:30
 */
public class GatewayFacade {

    SystemConfig systemConfig;

    StateLedger stateLedger;

    BlockPublisher blockPublisher;

    TxnManager txnManager;

    public GatewayFacade(SystemConfig systemConfig, StateLedger stateLedger, TxnManager txnManager) {
        this.systemConfig = systemConfig;
        this.stateLedger = stateLedger;
        this.txnManager = txnManager;
        this.blockPublisher = new BlockPublisher(systemConfig);
    }

    public void start() {
        //监听服务，接收网关端的请求
        new Thread(()-> ApiServer.startApiServer(this, this.systemConfig), "api_server_thread").start();
        blockPublisher.start();
    }

    public long getLatestBeExecutedNum() {
        return stateLedger.getLatestBeExecutedNum();
    }

    public long getLatestConsensusNumber() {return this.stateLedger.getLatestConsensusNumber();}

    public long getCurrentCommitRound() {return this.stateLedger.consensusChainStore.getLatestLedger().getLatestLedgerInfo().getLedgerInfo().getRound();}

    public EthTransactionReceipt getTransactionByHash(byte[] hash) {
        return stateLedger.getTransactionByHash(hash);
    }


    public GlobalNodeEvent getGlobalNodeEvent(byte[] hash) {
        return stateLedger.getGlobalNodeEvent(hash);
    }


    public GlobalNodeEventReceipt getGlobalNodeEventReceipt(byte[] hash) {
        return stateLedger.getGlobalNodeEventReceipt(hash);
    }

    public EpochState getCurrentEpoch() {
        return stateLedger.consensusChainStore.getLatestLedger().getCurrentEpochState();
    }

    //not need decode
    public List<byte[]> getTransactionsByHashes(List<byte[]> hashes) {
        return stateLedger.getTransactionsByHashes(hashes);
    }

    public Block getBlockByNumber(long number) {
        return stateLedger.getBlockByNumber(number);
    }

    // not need decode
    public EventData getEventDataByNumber(long number) {
        return stateLedger.getEventDataByNumber(number);
    }

    public EthTransactionReceipt ethCall(EthTransaction tx) {
        return stateLedger.ethCall(tx);
    }

    public SystemConfig getSystemConfig() {
        return systemConfig;
    }
}
