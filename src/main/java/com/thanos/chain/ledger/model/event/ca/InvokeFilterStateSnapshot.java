package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.common.utils.rlp.RLP;

/**
 * InvokeFilterStateSnapshot.java description：
 *
 * @Author laiyiyu create on 2021-04-28 11:01:25
 */
public class InvokeFilterStateSnapshot extends CandidateStateSnapshot {

    int placeHolder;

    public InvokeFilterStateSnapshot() {
        super(null);
        this.placeHolder = 1;
        this.rlpEncoded = rlpEncoded();
    }

    @Override
    public GlobalEventCommand getCurrentCommand() {
        return GlobalEventCommand.INVOKE_FILTER;
    }

    @Override
    protected byte[] rlpEncoded() {
        return RLP.encodeInt(1);
    }

    @Override
    protected void rlpDecoded() {
        this.placeHolder = 1;
    }
}
