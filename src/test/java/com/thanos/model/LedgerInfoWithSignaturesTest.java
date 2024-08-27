package com.thanos.model;

import com.thanos.common.utils.HashUtil;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.EventInfo;
import com.thanos.chain.consensus.hotstuffbft.model.LedgerInfo;
import com.thanos.chain.consensus.hotstuffbft.model.LedgerInfoWithSignatures;
import com.thanos.model.ca.GlobalEventStateBuilder;
import com.thanos.model.common.SignatureBuilder;
import com.thanos.model.common.ValidatorVerifierBuilder;
import org.junit.Test;

import java.util.Optional;

/**
 * LedgerInfoWithSignaturesTest.java description：
 *
 * @Author laiyiyu create on 2020-09-27 17:04:19
 */
public class LedgerInfoWithSignaturesTest {

    @Test
    public void test() {
        EpochState epochState = new EpochState(3, GlobalEventStateBuilder.buildFullContent());

        EventInfo eventInfo = EventInfo.build(3, 3, HashUtil.sha3(new byte[]{2, 3,55}), HashUtil.sha3(new byte[]{2, 3,55}), 1, System.currentTimeMillis(), Optional.of(epochState));
        LedgerInfo ledgerInfo = LedgerInfo.build(eventInfo, eventInfo.getHash());
//        System.out.println(ledgerInfo);
//        System.out.println(new LedgerInfo(ledgerInfo.getEncoded()));
        LedgerInfoWithSignatures ledgerInfoWithSignatures = LedgerInfoWithSignatures.build(ledgerInfo, SignatureBuilder.build());
        System.out.println(ledgerInfoWithSignatures);
        System.out.println(new LedgerInfoWithSignatures(ledgerInfoWithSignatures.getEncoded()));

    }
}
