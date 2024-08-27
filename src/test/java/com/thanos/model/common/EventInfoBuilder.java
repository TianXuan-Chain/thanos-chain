package com.thanos.model.common;

import com.thanos.chain.ledger.model.event.GlobalEventState;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.EventInfo;
import com.thanos.model.ca.GlobalEventStateBuilder;

import java.util.Optional;

/**
 * EventInfoBuilder.java description：
 *
 * @Author laiyiyu create on 2020-06-28 16:19:00
 */
public class EventInfoBuilder {


    static byte[] id = HashUtil.sha3(new byte[]{10, 20, 30});
    static byte[] exeId = HashUtil.sha3(new byte[]{1, 2, 3});

    public static EventInfo buildWithVerifierAndNumber(long number) {
        return EventInfo.build(2, 2, id, exeId, number, System.currentTimeMillis(), Optional.of(new EpochState(2, GlobalEventStateBuilder.buildFullContent())));
    }

    public static EventInfo buildWithVerifier() {
        return EventInfo.build(2, 2, id, exeId, 2, System.currentTimeMillis(), Optional.of(new EpochState(2, GlobalEventStateBuilder.buildFullContent())));
    }

    public static EventInfo buildWithoutVerifier() {
        return EventInfo.build(2, 1, id, exeId, 1, System.currentTimeMillis(), Optional.empty());
    }
}
