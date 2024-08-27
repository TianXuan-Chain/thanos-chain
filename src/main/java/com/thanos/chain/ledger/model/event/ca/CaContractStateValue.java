package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.store.Persistable;

/**
 * CaContractStateValue.java description：
 *
 * @Author laiyiyu create on 2021-04-12 15:46:04
 */
public class CaContractStateValue extends Persistable {

    public CaContractStateValue(byte[] encode) {
        super(encode);
    }

    @Override
    protected byte[] rlpEncoded() {
        return this.rlpEncoded;
    }

    @Override
    protected void rlpDecoded() {
        // do noting
    }
}
