package com.thanos.chain.ledger.model.store;


import com.thanos.chain.ledger.model.RLPModel;

/**
 * Persistable.java description：
 *
 * @Author laiyiyu create on 2020-02-23 13:47:43
 */
public abstract class Persistable extends RLPModel implements Valueable {

    public Persistable(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    @Override
    public byte[] valueBytes() {
        return this.rlpEncoded;
    }

    protected abstract byte[] rlpEncoded();

    protected abstract void rlpDecoded();
}
