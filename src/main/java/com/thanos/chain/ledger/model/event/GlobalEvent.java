package com.thanos.chain.ledger.model.event;

import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

import java.util.Arrays;
import java.util.Optional;

/**
 * GlobalEvent.java description：
 *
 * @Author laiyiyu create on 2020-08-12 10:43:30
 */
public class GlobalEvent extends RLPModel {

    //Optional<EpochChangeEvent> epochChangeEvent;

    GlobalNodeEvent[] globalNodeEvents;

    public GlobalEvent() {
        this(new GlobalNodeEvent[0]);
    }

    public GlobalEvent(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public GlobalEvent(GlobalNodeEvent[] globalNodeEvents) {
        super(null);
        this.globalNodeEvents = globalNodeEvents;
        //this.epochChangeEvent = Optional.empty();
        this.rlpEncoded = rlpEncoded();
    }

    private boolean isGlobalNodeEventsEmpty() {
        return this.globalNodeEvents.length == 0;
    }


    public boolean isEmpty() {
        return isGlobalNodeEventsEmpty();
    }

    public GlobalNodeEvent[] getGlobalNodeEvents() {
        return globalNodeEvents;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[][] encode = new byte[1 + globalNodeEvents.length][];

        encode[0] = RLP.encodeInt(globalNodeEvents.length);
        int i = 1;
        for (GlobalNodeEvent globalNodeEvent: globalNodeEvents) {
            encode[i] = globalNodeEvent.getEncoded();
            i++;
        }

        return RLP.encodeList(encode);
    }

    @Override
    protected void rlpDecoded() {
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList payload = (RLPList) params.get(0);


        int globalNodeEventsSize = ByteUtil.byteArrayToInt(payload.get(0).getRLPData());
        GlobalNodeEvent[] globalNodeEvents = new GlobalNodeEvent[globalNodeEventsSize];
        int i = 1;
        for (; i < globalNodeEventsSize + 1; i++) {
            globalNodeEvents[i - 1] = new GlobalNodeEvent(payload.get(i).getRLPData());
        }
        this.globalNodeEvents = globalNodeEvents;
    }

    @Override
    public String toString() {
        return "GlobalEvent{" +
                ", globalNodeEvents=" + Arrays.toString(globalNodeEvents) +
                '}';
    }

    public void clear() {
        this.globalNodeEvents = null;
    }
}
