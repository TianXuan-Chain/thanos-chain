package com.thanos.chain.ledger.model.store;

/**
 * DefaultValueable.java description：
 *
 * @Author laiyiyu create on 2020-03-05 23:09:44
 */
public class DefaultValueable extends Persistable {

    public DefaultValueable(byte[] rlpEncoded) {
        super(rlpEncoded);
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
