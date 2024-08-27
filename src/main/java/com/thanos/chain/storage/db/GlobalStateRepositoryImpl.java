package com.thanos.chain.storage.db;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.contract.ca.filter.GlobalFilterChain;
import com.thanos.chain.ledger.model.event.GlobalEventState;
import com.thanos.chain.ledger.model.event.ca.CaContractCode;
import com.thanos.chain.ledger.model.event.ca.CaContractStateValue;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.common.utils.ByteArrayWrapper;

import java.util.*;

/**
 * GlobalStateRepositoryImpl.java description：
 *
 * @Author laiyiyu create on 2021-04-13 17:04:43
 */
public class GlobalStateRepositoryImpl {

    protected final boolean root;

    protected ConsensusChainStore consensusChainStore;

    protected GlobalStateRepositoryImpl parent;

    Map<Keyable, CaContractStateValue> globalEventProcessStateCache;


    protected GlobalStateRepositoryImpl() {
        this.root = false;
        this.globalEventProcessStateCache = new HashMap<>();
    }

    public GlobalStateRepositoryImpl(ConsensusChainStore consensusChainStore) {
        this.consensusChainStore = consensusChainStore;
        this.root = true;
        this.globalEventProcessStateCache = new HashMap<>();
    }

    public CaContractStateValue getCaContractStateValue(byte[] key) {
        Keyable newKey = Keyable.ofDefault(key);
        CaContractStateValue result = this.globalEventProcessStateCache.get(newKey);
        if (result != null) {
            if (result.getEncoded() != null) {
                return result;
            } else {
                return null;
            }
        }

        if (!root) {
            return parent.getCaContractStateValue(key);
        } else {
            return this.consensusChainStore.getCaContractStateValue(key);
        }
    }

    public void writeCaContractStateValue(byte[] key, byte[] value) {
        this.globalEventProcessStateCache.put(new Keyable.DefaultKeyable(key), new CaContractStateValue(value));
    }

    public void delCaContractStateValue(byte[] key) {
        this.globalEventProcessStateCache.put(new Keyable.DefaultKeyable(key), new CaContractStateValue(null));
    }

    public GlobalStateRepositoryImpl startTracking() {
        GlobalStateRepositoryImpl ret = new GlobalStateRepositoryImpl();
        ret.parent = this;
        ret.consensusChainStore = this.consensusChainStore;
        return ret;
    }

    public  GlobalStateRepositoryImpl getSnapshotTo(ConsensusChainStore consensusChainStore) {
        return new GlobalStateRepositoryImpl(consensusChainStore);
    }

    public CaContractCode getCaContractCode(byte[] addr) {
        return this.consensusChainStore.getCaContractCode(addr);
    }

    public void commit() {
        GlobalStateRepositoryImpl parentSync = parent;

        // skip the root Repository
        if (parentSync == null || root) return;

        Iterator<Map.Entry<Keyable, CaContractStateValue>> caContractStateIter = this.globalEventProcessStateCache.entrySet().iterator();
        while (caContractStateIter.hasNext()) {
            Map.Entry<Keyable, CaContractStateValue> entry = caContractStateIter.next();
            parent.globalEventProcessStateCache.put(entry.getKey(), entry.getValue());
            caContractStateIter.remove();
        }
    }

    public void rollback() {
        // nothing to do, will be GCed
        if (!root) {
            this.globalEventProcessStateCache.clear();
        }
    }

    public void flush(EpochState epochState) {
    }

    public GlobalFilterChain getGlobalFilterChain() {
        return consensusChainStore.globalFilterChain;
    }

    public boolean isManagerAuth(byte[] addr) {
        ByteArrayWrapper addrWrapper = new ByteArrayWrapper(addr);
        GlobalEventState globalEventState = consensusChainStore.getLatestLedger().getCurrentEpochState().getGlobalEventState();
        return globalEventState.getCommitteeAddrSet().contains(addrWrapper)
                || globalEventState.getOperationsStaffAddrSet().contains(addrWrapper);
    }

    public boolean hasProposal(byte[] id) {
        return this.consensusChainStore.getCaFinishProposalId(id) != null;
    }

}
