package com.thanos.chain.storage.db;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.ledger.model.event.ca.CaContractStateValue;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;

import java.util.Iterator;
import java.util.Map;

/**
 * GlobalStateRepositoryImpl.java description：
 *
 * @Author laiyiyu create on 2021-04-13 17:04:43
 */
public class GlobalStateRepositoryRoot extends GlobalStateRepositoryImpl {

    public GlobalStateRepositoryRoot(ConsensusChainStore consensusChainStore) {
        super(consensusChainStore);
    }

    public void commit() {
    }

    public void flush(EpochState epochState) {
        Iterator<Map.Entry<Keyable, CaContractStateValue>> caContractStateIter = this.globalEventProcessStateCache.entrySet().iterator();
        while (caContractStateIter.hasNext()) {
            Map.Entry<Keyable, CaContractStateValue> entry = caContractStateIter.next();
            epochState.getGlobalEventState().addGlobalEventProcessState(new ByteArrayWrapper(ByteUtil.copyFrom(entry.getKey().keyBytes())), new ByteArrayWrapper(ByteUtil.copyFrom(entry.getValue().valueBytes())));
            caContractStateIter.remove();
        }
    }
}
