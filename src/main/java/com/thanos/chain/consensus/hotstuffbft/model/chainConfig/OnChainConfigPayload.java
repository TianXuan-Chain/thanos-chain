package com.thanos.chain.consensus.hotstuffbft.model.chainConfig;


import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import com.thanos.common.utils.ByteUtil;

import java.util.HashMap;
import java.util.List;

/**
 * OnChainConfigPayload.java description：
 *
 * @Author laiyiyu create on 2020-06-11 14:15:13
 */
public class OnChainConfigPayload {

    public static String EPOCH_INFO = "EPOCH_INFO";

    long epoch;

    // configId 2 content
    HashMap<String, Object> configs;

    public OnChainConfigPayload(long epoch, HashMap<String, Object> configs) {
        this.epoch = epoch;
        this.configs = configs;
    }

    public static OnChainConfigPayload build(EpochState epochState) {
        HashMap<String, Object> configs = new HashMap<>();
        EpochState newEpoch = new EpochState(ByteUtil.copyFrom(epochState.getEncoded()));
        configs.put(EPOCH_INFO, newEpoch);
        return new OnChainConfigPayload(epochState.getEpoch(), configs);
    }

    public long getEpoch() {
        return epoch;
    }

    public EpochState getEpochState() {
        return (EpochState) configs.get(EPOCH_INFO);
    }


}
