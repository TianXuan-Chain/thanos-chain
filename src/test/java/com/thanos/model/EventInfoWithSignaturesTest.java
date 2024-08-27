package com.thanos.model;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.EventInfoWithSignatures;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.model.ca.GlobalEventStateBuilder;
import com.thanos.model.common.SignatureBuilder;
import com.thanos.model.common.ValidatorVerifierBuilder;
import org.junit.Test;

import java.util.Optional;
import java.util.TreeMap;

/**
 * EventInfoWithSignaturesTest.java description：
 *
 * @Author laiyiyu create on 2020-07-23 11:01:53
 */
public class EventInfoWithSignaturesTest {

    @Test
    public void test1() {
        TreeMap<ByteArrayWrapper, Signature> signatureTreeMap = SignatureBuilder.build();

        EventInfoWithSignatures eventInfoWithSignatures1 = EventInfoWithSignatures.build(1,
                2,
                new byte[]{22,34,2},
                new byte[]{21,35,2},
                3,
                System.currentTimeMillis(),
                Optional.empty(),
                signatureTreeMap
        );

        System.out.println(eventInfoWithSignatures1);

        System.out.println(new EventInfoWithSignatures(eventInfoWithSignatures1.getEncoded()));

    }

    @Test
    public void test2() {
        TreeMap<ByteArrayWrapper, Signature> signatureTreeMap = SignatureBuilder.build();

        EventInfoWithSignatures eventInfoWithSignatures1 = EventInfoWithSignatures.build(3,
                4,
                new byte[]{2,4,2},
                new byte[]{1,5,2},
                6,
                System.currentTimeMillis(),
                Optional.of(new EpochState(4, GlobalEventStateBuilder.buildFullContent())),
                signatureTreeMap
        );

        System.out.println(eventInfoWithSignatures1);

        System.out.println(new EventInfoWithSignatures(eventInfoWithSignatures1.getEncoded()));
    }


    @Test
    public void test3() {

        EventInfoWithSignatures eventInfoWithSignatures1 = EventInfoWithSignatures.build(3,
                6,
                new byte[]{42,46,26},
                new byte[]{14,45,2},
                7,
                System.currentTimeMillis(),
                Optional.of(new EpochState(74, GlobalEventStateBuilder.buildFullContent())),
                new TreeMap<>()
        );

        System.out.println(eventInfoWithSignatures1);

        System.out.println(new EventInfoWithSignatures(eventInfoWithSignatures1.getEncoded()));
    }

}
