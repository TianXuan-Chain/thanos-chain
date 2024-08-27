package com.thanos.chain.contract.ca.filter;

import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

/**
 * AbstractGlobalFilter.java description：
 *
 * @Author laiyiyu create on 2021-04-09 17:27:33
 */
public abstract class AbstractGlobalFilter extends AbstractFilterNamespace {

    protected static final Logger logger = LoggerFactory.getLogger("ca");

    public AbstractGlobalFilter(byte[] address, GlobalStateRepositoryImpl stateRepository) {
        super(address, stateRepository);
    }

    public ProcessResult doFilter(EthTransaction ethTransaction) {
        try {
            if (ethTransaction.isContractCreation()) {
                return doDeployFilter(ethTransaction);
            } else {
                //logger.info("do filter:[{}-{}]", this, Hex.toHexString(ethTransaction.getHash()));
                return doInvokeFilter(ethTransaction);
            }
        } catch (Exception e) {
            return ProcessResult.ofError(ExceptionUtils.getStackTrace(e));
        }

    }

    protected abstract ProcessResult doInvokeFilter(EthTransaction ethTransaction);

    protected abstract ProcessResult doDeployFilter(EthTransaction ethTransaction);

    public void refreshState() {}
}
