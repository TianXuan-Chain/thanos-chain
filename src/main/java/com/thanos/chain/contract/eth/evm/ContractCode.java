package com.thanos.chain.contract.eth.evm;

import com.thanos.chain.ledger.model.store.Persistable;

/**
 * ContractCode.java description：
 *
 * @Author laiyiyu create on 2020-02-25 16:42:39
 */
public class ContractCode extends Persistable {

    private final byte[] data;

    public ContractCode(byte[] data) {
        super(null);
        this.data = data;
        this.rlpEncoded = data;
    }

    @Override
    protected byte[] rlpEncoded() {
        return data;
    }

    @Override
    protected void rlpDecoded() {
    }

    public byte[] getData() {
        return data;
    }
}
