package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.store.Persistable;

/**
 * CaFinishProposalId.java description：
 *
 * @Author laiyiyu create on 2021-06-08 14:10:55
 */
public class CaFinishProposalId extends Persistable {

    public CaFinishProposalId(byte[] encode) {
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
