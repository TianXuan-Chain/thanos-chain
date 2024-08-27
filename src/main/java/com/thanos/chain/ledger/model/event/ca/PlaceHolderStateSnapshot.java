package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.common.utils.rlp.RLP;

import java.util.Objects;

/**
 * PlaceHolderStateSnapshot.java description：
 *
 * @Author laiyiyu create on 2021-04-08 19:39:07
 */
public class PlaceHolderStateSnapshot extends CandidateStateSnapshot {

    int placeHolder;

    public PlaceHolderStateSnapshot() {
        super(null);
        this.placeHolder = 1;
        this.rlpEncoded = rlpEncoded();
    }

    @Override
    public GlobalEventCommand getCurrentCommand() {
        return GlobalEventCommand.PLACEHOLDER_EMPTY;
    }

    @Override
    protected byte[] rlpEncoded() {
        return RLP.encodeInt(1);
    }

    @Override
    protected void rlpDecoded() {
        this.placeHolder = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaceHolderStateSnapshot that = (PlaceHolderStateSnapshot) o;
        return placeHolder == that.placeHolder;
    }

    @Override
    public int hashCode() {

        return Objects.hash(placeHolder);
    }
}
