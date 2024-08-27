package com.thanos.chain.consensus.hotstuffbft.store;

import com.thanos.chain.ledger.model.event.GlobalEventState;
import com.thanos.chain.ledger.model.event.ca.CaContractStateValue;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GlobalNodeEventStateCache.java description：
 *
 * @Author laiyiyu create on 2021-04-13 11:14:05
 */
public class GlobalNodeEventStateCache {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    private final int CA_CONTRACT_STATE_CACHE_COUNT;

    private static Map<Keyable, CaContractStateValue> CA_CONTRACT_STATE_CACHE;

    public GlobalNodeEventStateCache(int CA_CONTRACT_STATE_CACHE_COUNT) {
        this.CA_CONTRACT_STATE_CACHE_COUNT = CA_CONTRACT_STATE_CACHE_COUNT;
        this.CA_CONTRACT_STATE_CACHE = new ConcurrentHashMap<>(CA_CONTRACT_STATE_CACHE_COUNT);
        logger.info("GlobalNodeEventStateCache CA_CONTRACT_STATE_CACHE_COUNT:{}", this.CA_CONTRACT_STATE_CACHE_COUNT);
    }

    public CaContractStateValue getCaContractStateValue(Keyable address) {
        CaContractStateValue value = CA_CONTRACT_STATE_CACHE.get(address);
        return value == null? null: new CaContractStateValue(ByteUtil.copyFrom(value.getEncoded()));
    }

    public void updateGlobalNodeEventStateCache(GlobalEventState globalEventState) {
        updateCaContractStateValues(globalEventState.getGlobalEventProcessState());
    }

    private void updateCaContractStateValues(Map<ByteArrayWrapper, ByteArrayWrapper> caContractStateWriteCache) {

        if (caContractStateWriteCache.size() >= CA_CONTRACT_STATE_CACHE_COUNT) {
            CA_CONTRACT_STATE_CACHE.clear();
            //CONTRACT_CODE_CACHE.putAll();
        } else if (caContractStateWriteCache.size() + CA_CONTRACT_STATE_CACHE.size() <= CA_CONTRACT_STATE_CACHE_COUNT) {

        } else {
            long evictSize = caContractStateWriteCache.size() + CA_CONTRACT_STATE_CACHE.size() - CA_CONTRACT_STATE_CACHE_COUNT;
            Iterator iter = CA_CONTRACT_STATE_CACHE.entrySet().iterator();
            while (evictSize > 0) {
                iter.next();
                iter.remove();
                evictSize--;
            }

        }

        for (Map.Entry<ByteArrayWrapper, ByteArrayWrapper> entry: caContractStateWriteCache.entrySet()) {
            CA_CONTRACT_STATE_CACHE.put(Keyable.ofDefault(ByteUtil.copyFrom(entry.getKey().getData())), new CaContractStateValue(ByteUtil.copyFrom(entry.getValue().getData())));
        }
    }
}
