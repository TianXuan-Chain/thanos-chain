package com.thanos.chain.contract.ca.filter;

import com.thanos.chain.ledger.model.event.ca.CaContractStateValue;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

/**
 * AbstractFilterNamespace.java description：
 *
 * @Author laiyiyu create on 2021-04-15 11:09:21
 */
public  class AbstractFilterNamespace {

    private static final Logger logger = LoggerFactory.getLogger("ca");

    final protected byte[] address;

    final protected String namespace;

    final protected GlobalStateRepositoryImpl stateRepository;

    public AbstractFilterNamespace(byte[] address, GlobalStateRepositoryImpl stateRepository) {
        this.address = address;
        this.stateRepository = stateRepository;
        this.namespace = this.getClass().getName();
    }

    public CaContractStateValue getCaContractStateValue(byte[] key) {
        byte[] dbKey = HashUtil.sha3(ByteUtil.merge(this.address, this.namespace.getBytes(), key));
        CaContractStateValue result = this.stateRepository.getCaContractStateValue(dbKey);
        //logger.info("getCaContractStateValue[{}-{}-{}]", Hex.toHexString(dbKey), Hex.toHexString(key), (result == null? "": Hex.toHexString(result.valueBytes())));
        return result;
    }

    public void writeCaContractStateValue(byte[] key, byte[] value) {
        byte[] dbKey = HashUtil.sha3(ByteUtil.merge(this.address, this.namespace.getBytes(), key));
        this.stateRepository.writeCaContractStateValue(dbKey, value);
        //logger.info("writeCaContractStateValue[{}-{}-{}]", Hex.toHexString(dbKey), Hex.toHexString(key),Hex.toHexString(value));

    }

    public void deleteCaContractStateValue(byte[] key) {
        byte[] dbKey = HashUtil.sha3(ByteUtil.merge(this.address, this.namespace.getBytes(), key));
        this.stateRepository.delCaContractStateValue(dbKey);
        //logger.info("deleteCaContractStateValue[{}-{}]",Hex.toHexString(dbKey), Hex.toHexString(key));

    }

    public boolean isAuthManager(byte[] addr) {
        return this.stateRepository.isManagerAuth(addr);
    }
}
