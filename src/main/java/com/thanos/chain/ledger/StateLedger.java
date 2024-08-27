
package com.thanos.chain.ledger;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.contract.eth.evm.program.invoke.ProgramInvokeFactory;
import com.thanos.chain.contract.eth.evm.program.invoke.ProgramInvokeFactoryImpl;
import com.thanos.chain.executor.EthTransactionExecutor;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.network.NetInvoker;
import com.thanos.chain.state.verifier.GlobalStateVerifier;
import com.thanos.chain.storage.datasource.LedgerSource;
import com.thanos.chain.storage.db.GlobalStateRepositoryRoot;
import com.thanos.chain.storage.db.Repository;
import com.thanos.chain.storage.db.RepositoryRoot;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * StateLedger.java description：
 *
 * @Author laiyiyu create on 2020-08-24 14:06:27
 */
public class StateLedger {

    public final ConsensusChainStore consensusChainStore;

    public final LedgerSource ledgerSource;

    public final RepositoryRoot rootRepository;

    public final SystemConfig systemConfig;

    public final ProgramInvokeFactory programInvokeFactory;

    public final GlobalStateVerifier globalStateVerifier;

    public final StateLedgerIndexer stateLedgerIndexer;

    volatile Optional<Function<Void, Void>> rollbackBlockFun = Optional.empty();

    public StateLedger(SystemConfig systemConfig, NetInvoker netInvoker, ConsensusChainStore consensusChainStore, boolean test) {
        this.consensusChainStore = consensusChainStore;
        this.ledgerSource = new LedgerSource(false, systemConfig);
        this.stateLedgerIndexer = new StateLedgerIndexer(systemConfig, ledgerSource);
        this.rootRepository = new RepositoryRoot(ledgerSource);
        this.systemConfig = systemConfig;
        this.programInvokeFactory = new ProgramInvokeFactoryImpl();
        this.globalStateVerifier = new GlobalStateVerifier(this, netInvoker, test);
    }

    public void setRollbackBlockFun(Function<Void, Void> fun) {
        this.rollbackBlockFun = Optional.of(fun);
    }

    public long getLatestBeExecutedNum() {
        return ledgerSource.getLatestBeExecutedNum();
    }

    public long getLatestBeExecutedEpoch() {
        return ledgerSource.getLatestBeExecutedEpoch();
    }

    public long getLatestConsensusNumber() {return this.consensusChainStore.getLatestLedger().getLatestNumber();}

    public EthTransactionReceipt getTransactionByHash(byte[] hash) {
        return stateLedgerIndexer.ledgerIndexSource.getTransactionReceipt(hash);
    }

    public GlobalNodeEventReceipt getGlobalNodeEventReceipt(byte[] hash) {
        return consensusChainStore.getGlobalNodeEventReceipt(hash);
    }

    public GlobalNodeEvent getGlobalNodeEvent(byte[] hash) {
        return stateLedgerIndexer.ledgerIndexSource.getGlobalNodeEvent(hash);
    }


    public List<byte[]> getTransactionsByHashes(List<byte[]> hashes) {
        return stateLedgerIndexer.ledgerIndexSource.getTransactionReceiptsByHashes(hashes);

    }

    public Block getBlockByNumber(long number) {
        return ledgerSource.getBlockByNumber(number);
    }

    public EventData getEventDataByNumber(long number) {
        return consensusChainStore.getEventData(number, false, false);
    }

    public EthTransactionReceipt ethCall(EthTransaction tx) {
        Repository track = rootRepository.getSnapshotTo(ledgerSource);
        EthTransactionExecutor executor = new EthTransactionExecutor(tx, track, programInvokeFactory, systemConfig.getGenesis()).withConfig(systemConfig);
        executor.init();
        executor.execute();
        executor.go();
        EthTransactionReceipt receipt = executor.getReceipt();
        executor.finalization();
        return receipt;
    }

    //============================================================================================


    public void doRollBackState() {
        if (rollbackBlockFun.isPresent()) {
            rollbackBlockFun.get().apply(null);
        }
    }

    public void flush() {
        rootRepository.flush();
    }


    public void persist(Block block) {
        rootRepository.persist(block);
    }
}
