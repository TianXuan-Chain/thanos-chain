package com.thanos.net;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.ledger.model.crypto.ValidatorSigner;
import com.thanos.model.common.EventDataBuilder;
import com.thanos.model.common.HotstuffChainSyncInfoBuilder;
import com.thanos.chain.network.peer.PeerClient;
import com.thanos.chain.network.peer.PeerManager;
import org.junit.Test;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PeerClientTest.java description：
 *
 * @Author laiyiyu create on 2020-06-28 19:30:32
 */
public class PeerClientTest {

    @Test
    public void test1() {

        SystemConfig systemConfig = new SystemConfig();
        PeerManager peerManager = new PeerManager(systemConfig);
        byte[] nodeId1 = systemConfig.getMyKey().getNodeId();

        PeerClient peerClient = new PeerClient(peerManager);

        Runnable connectTask = () -> peerClient.connect("localhost", 8888, nodeId1, (short) 0);
        new Thread(connectTask).start();
//        peerClient.connect("localhost", 8888, new XChainChannelInitializer(peerManager, Hex.toHexString(nodeId1)));
//        await(20000);
//        peerClient.connect("localhost", 8888, new XChainChannelInitializer(peerManager, Hex.toHexString(nodeId1)));
        await(20000000);

//        HotstuffChainSyncInfo hotstuffChainSyncInfo = HotstuffChainSyncInfoBuilder.buildCommon();
//
//
//        EventData eventData = EventData.buildEmpty(3, hotstuffChainSyncInfo.getHighestQuorumCert());
//
//        Event event = Event.buildProposalFromEventData(eventData, new ValidatorSigner(null));
//
//        ProposalMsg proposalMsg = ProposalMsg.build(event, hotstuffChainSyncInfo);
//
//        peerManager.directSend(proposalMsg, Arrays.asList(nodeId1));
//
//
//        await(40000);
    }



    @Test
    public void test2() {
        SystemConfig systemConfig = new SystemConfig();
        PeerManager peerManager = new PeerManager(systemConfig);
        byte[] nodeId1 = systemConfig.getMyKey().getNodeId();

        PeerClient peerClient = new PeerClient(peerManager);
        System.out.println("first connect!!!");


        Runnable connectTask = () -> peerClient.connect("localhost", 8888, nodeId1, (short) 0);
        new Thread(connectTask).start();
        //new Thread(connectTask).start();
//        peerClient.connect("localhost", 8888, new XChainChannelInitializer(peerManager, Hex.toHexString(nodeId1)));
//        await(20000);
//        peerClient.connect("localhost", 8888, new XChainChannelInitializer(peerManager, Hex.toHexString(nodeId1)));
        await(2000);


        HotstuffChainSyncInfo hotstuffChainSyncInfo = HotstuffChainSyncInfoBuilder.buildCommon();

        EventData eventData = EventData.buildEmpty(3, hotstuffChainSyncInfo.getHighestQuorumCert());

        Event event = Event.buildProposalFromEventData(eventData, new ValidatorSigner(null));

        ProposalMsg proposalMsg = ProposalMsg.build(event, hotstuffChainSyncInfo);
        peerManager.directSend(proposalMsg, Arrays.asList(nodeId1));
        await(20000000);
//        peerClient.connect("localhost", 8888);

        peerClient.close();
    }



    @Test
    public void test3() {
        SystemConfig systemConfig = new SystemConfig();
        PeerManager peerManager = new PeerManager(systemConfig);
        byte[] nodeId1 = systemConfig.getMyKey().getNodeId();
        PeerClient peerClient = new PeerClient(peerManager);
        peerClient.connect("localhost", 8888, nodeId1, (short) 0);
        await(2000);



        int num = 3000;
        List<Event> events = new ArrayList<>(num);

        for (int i = 0; i < num; i++) {
            events.add(Event.buildProposalFromEventData(EventDataBuilder.buildCommon(), new ValidatorSigner(null)));
        }
        EventRetrievalResponseMsg eventRetrievalResponseMsg = new EventRetrievalResponseMsg(EventRetrievalResponseMsg.EventRetrievalStatus.SUCCESSED, events);


        long start = System.currentTimeMillis();

        byte[] encode = eventRetrievalResponseMsg.getEncoded();

        long end = System.currentTimeMillis();
        System.out.println("encode cost:" + (end - start));



        start = System.currentTimeMillis();
        byte[] compressRes = null;
        try {
            compressRes = Snappy.compress(encode);

        } catch (IOException e) {
            e.printStackTrace();
        }

        end = System.currentTimeMillis();


        System.out.println("Snappy.compress cost:" + (end - start));


        //peerManager.directSend(eventRetrievalResponseMsg, Arrays.asList(nodeId1));


        await(20000000);


    }

    public static void await(long time) {
        synchronized (PeerClientTest.class) {// 类实例锁
            try {
                PeerClientTest.class.wait(time);
                System.out.println("wait after");
            } catch (InterruptedException e) {

            }
        }
    }




}
