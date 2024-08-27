package com.thanos.chain.contract.ca.filter;

import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.event.ca.CaContractCode;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.chain.storage.db.GlobalStateRepositoryRoot;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GlobalFilterChain.java description：
 *
 * @Author laiyiyu create on 2021-04-09 17:34:44
 */
public class GlobalFilterChain {

    private static final Logger logger = LoggerFactory.getLogger("ca");

    private List<AbstractGlobalFilter> filters;

    private Map<ByteArrayWrapper, FilterInvoker> filterInvokerMap;

    private GlobalStateRepositoryRoot stateRepository;

    public static GlobalFilterChain build(List<ByteArrayWrapper> filterAddrs, GlobalStateRepositoryRoot stateRepository) {
        return new GlobalFilterChain(filterAddrs, stateRepository);
    }

    private GlobalFilterChain(List<ByteArrayWrapper> filterAddrs, GlobalStateRepositoryRoot stateRepository) {
        this.filters = new ArrayList<>(filterAddrs.size());
        this.filterInvokerMap = new ConcurrentHashMap<>(filterAddrs.size());
        this.stateRepository = stateRepository;

        for (ByteArrayWrapper codeAddr: filterAddrs) {
            FilterInvoker filterInvoker = buildFilterInvokerInstance(codeAddr);
            filterInvokerMap.put(codeAddr, filterInvoker);
            filters.add(filterInvoker.getFilter());
        }
    }

    public void refreshFilterState() {
        logger.debug("GlobalFilterChain will refreshFilterState!");
        for (FilterInvoker filterInvoker: filterInvokerMap.values()) {
            filterInvoker.refreshFilterState();
        }
    }

    public void refresh(Map<ByteArrayWrapper, CaContractCode> caCodeMap) {
        for (Map.Entry<ByteArrayWrapper, CaContractCode> entry: caCodeMap.entrySet()) {

            if (entry.getValue().getEncoded() != null) {
                // add
                FilterInvoker filterInvoker = buildFilterInvokerInstance(new ByteArrayWrapper(ByteUtil.copyFrom(entry.getKey().getData())));
                this.filters.add(filterInvoker.getFilter());
                filterInvokerMap.put(entry.getKey(), filterInvoker);
            } else {
                // remove
                FilterInvoker filterInvoker = filterInvokerMap.remove(entry.getKey());
                if (filterInvoker != null) {
                    this.filters.remove(filterInvoker.getFilter());
                }
            }
        }
    }

    public ProcessResult filter(EthTransaction ethTransaction) {
        ProcessResult result;
        if (CollectionUtils.isEmpty(filters)) return ProcessResult.SUCCESSFUL;

        for (AbstractGlobalFilter filter: filters) {
            result = filter.doFilter(ethTransaction);
            if (!result.isSuccess()) return result;
        }

        return  ProcessResult.ofSuccess();
    }

    public ProcessResult<byte[]> invokeFilter(GlobalStateRepositoryImpl globalStateRepositoryTrack, byte[] filterAddr, byte[] methodId, byte[] methodInput) {
        FilterInvoker filterInvoker = this.filterInvokerMap.get(new ByteArrayWrapper(filterAddr));
        if (filterInvoker == null) {
            return ProcessResult.ofError(String.format("missing the filter[%s]", Hex.toHexString(filterAddr)));
        }

        return filterInvoker.invoke(globalStateRepositoryTrack, methodId, methodInput);

    }

    private FilterInvoker buildFilterInvokerInstance(ByteArrayWrapper codeAddr) {

        FilterInvoker filterInvoker = null;
        try {
            CaContractCode caContractCode = stateRepository.getCaContractCode(codeAddr.getData());
            if (caContractCode == null) {
                logger.error("current ledger is damage, missing the filter[{}]", codeAddr);
            }

            filterInvoker = new FilterInvoker(caContractCode, stateRepository);

        } catch (Exception e) {
            logger.error("buildFilterInvokerInstance[{}] error!{}", codeAddr, ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }

        return filterInvoker;
    }
}
