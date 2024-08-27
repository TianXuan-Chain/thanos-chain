package com.thanos.model;

import com.thanos.chain.consensus.hotstuffbft.model.Event;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.consensus.hotstuffbft.model.EventRetrievalResponseMsg;
import com.thanos.chain.ledger.model.crypto.ValidatorSigner;
import com.thanos.model.common.EventDataBuilder;
import org.junit.Test;

import java.util.Arrays;

/**
 * EventRetrievalResponseMsgTest.java description：
 *
 * @Author laiyiyu create on 2020-06-28 16:57:20
 */
public class EventRetrievalResponseMsgTest {

    @Test
    public void normal() {
        Event event1 = Event.buildProposalFromEventData(EventDataBuilder.buildCommon(), new ValidatorSigner(null));
        Event event2 = Event.buildProposalFromEventData(EventDataBuilder.buildGensis(), new ValidatorSigner(null));
        EventRetrievalResponseMsg eventRetrievalResponseMsg = new EventRetrievalResponseMsg(EventRetrievalResponseMsg.EventRetrievalStatus.SUCCESSED, Arrays.asList(event1, event2));
        System.out.println(eventRetrievalResponseMsg);
        System.out.println(new EventRetrievalResponseMsg(eventRetrievalResponseMsg.getEncoded()));
    }
}
