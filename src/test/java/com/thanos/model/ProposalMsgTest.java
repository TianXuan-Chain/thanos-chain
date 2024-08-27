package com.thanos.model;

import com.thanos.common.crypto.VerifyingKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.chain.ledger.model.crypto.ValidatorSigner;
import com.thanos.model.common.EventInfoBuilder;
import com.thanos.model.common.HotstuffChainSyncInfoBuilder;
import com.thanos.model.common.SignatureBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.TreeMap;

/**
 * ProposalMsgTest.java description：
 *
 * @Author laiyiyu create on 2020-06-28 16:12:31
 */
public class ProposalMsgTest {

    @Test
    public void normal() {


        HotstuffChainSyncInfo hotstuffChainSyncInfo = HotstuffChainSyncInfoBuilder.buildCommon();


        EventData eventData = EventData.buildEmpty(3, hotstuffChainSyncInfo.getHighestQuorumCert());

        Event event = Event.buildProposalFromEventData(eventData, new ValidatorSigner(null));

        ProposalMsg proposalMsg = ProposalMsg.build(event, hotstuffChainSyncInfo);
        System.out.println(proposalMsg);

        ProposalMsg proposalMsg2 = new ProposalMsg(proposalMsg.getEncoded());
        System.out.println(proposalMsg2);


    }
}
