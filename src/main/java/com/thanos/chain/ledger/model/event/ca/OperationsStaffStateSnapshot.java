package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.common.utils.rlp.RLP;

/**
 * InvokeFilterStateSnapshot.java description：
 *
 * @Author laiyiyu create on 2021-04-28 11:01:25
 */
public class OperationsStaffStateSnapshot extends CandidateStateSnapshot {

    int placeHolder;

    public OperationsStaffStateSnapshot() {
        super(null);
        this.placeHolder = 1;
        this.rlpEncoded = rlpEncoded();
    }

    @Override
    public GlobalEventCommand getCurrentCommand() {
        return GlobalEventCommand.PROCESS_OPERATIONS_STAFF;
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
