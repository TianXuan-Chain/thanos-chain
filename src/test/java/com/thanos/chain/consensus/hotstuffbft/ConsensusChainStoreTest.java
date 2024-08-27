package com.thanos.chain.consensus.hotstuffbft;

import com.thanos.chain.config.SystemConfig;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.model.ca.GlobalEventStateBuilder;
import com.thanos.model.common.EventDataBuilder;
import com.thanos.model.common.SignatureBuilder;
import com.thanos.model.common.ValidatorVerifierBuilder;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

/**
 * ConsensusChainStoreTest.java description：
 *
 * @Author laiyiyu create on 2020-07-01 19:31:49
 */
@Ignore
public class ConsensusChainStoreTest {



    ConsensusChainStore consensusChainStore = new ConsensusChainStore(SystemConfig.getDefault(), false);


    @Test
    public void commit() {

        Map<Keyable.DefaultKeyable, byte[]> output = new HashMap<>();
        output.put(Keyable.ofDefault(new byte[]{2, 3,55}), new byte[]{2, 3,55});


        EpochState epochState = new EpochState(3, GlobalEventStateBuilder.buildFullContent());

        ExecutedEventOutput eventOutput = new ExecutedEventOutput(output, 3, HashUtil.sha3(new byte[]{2, 3,55}), Optional.of(epochState));
        System.out.println(eventOutput);
        System.out.println(new ExecutedEventOutput(eventOutput.getEncoded()));

        List<EventData> eventDatas = Arrays.asList( EventDataBuilder.buildGensis(), EventDataBuilder.buildCommon(), EventDataBuilder.buildWithPayload());

        for (EventData eventData: eventDatas) {
            System.out.println(eventData);
        }

        EventInfoWithSignatures eventInfoWithSignatures = EventInfoWithSignatures.build(3, 1, new byte[]{2, 3,55}, new byte[]{2, 3,54}, 1, 2, Optional.empty(), new TreeMap<>());



        EventInfo eventInfo = EventInfo.build(3, 3, HashUtil.sha3(new byte[]{2, 3,55}), HashUtil.sha3(new byte[]{2, 3,55}), 1, System.currentTimeMillis(), Optional.of(epochState));
        LedgerInfo ledgerInfo = LedgerInfo.build(eventInfo, eventInfo.getHash());
//        System.out.println(ledgerInfo);
//        System.out.println(new LedgerInfo(ledgerInfo.getEncoded()));
        LedgerInfoWithSignatures ledgerInfoWithSignatures = LedgerInfoWithSignatures.build(ledgerInfo, SignatureBuilder.build());

        consensusChainStore.commit(eventDatas, Arrays.asList(eventInfoWithSignatures), eventOutput, ledgerInfoWithSignatures, false);


    }


    @Ignore
    @Test
    public void query() {

//        transientHash=802d89b06c5370514b5b2650ab82de5758dd1cb7459956fac4213a1710960629
//        transientHash=fd12edacecb7fe1392408364c340ffa6b1abe17520f982a6685e8e5e922ad2ce
//        transientHash=de6cc0391dfbae51b553cfbb39846898cbc208d1fd257ba27b7cb2ac3928f75e
        EventInfoWithSignatures signatures = new EventInfoWithSignatures(
        consensusChainStore.db.getRaw(EventInfoWithSignatures.class, Keyable.ofDefault(ByteUtil.longToBytes(1))));
        System.out.println(signatures);

        EventData eventData0 = new EventData(
                consensusChainStore.db.getRaw(EventData.class, Keyable.ofDefault(ByteUtil.longToBytes(0))));
        //eventData.getPayload().reDecoded();
        System.out.println(eventData0);

        EventData eventData1 = new EventData(
                consensusChainStore.db.getRaw(EventData.class, Keyable.ofDefault(ByteUtil.longToBytes(1))));
        //eventData.getPayload().reDecoded();
        System.out.println(eventData1);

        EventData eventData3 = new EventData(
                consensusChainStore.db.getRaw(EventData.class, Keyable.ofDefault(ByteUtil.longToBytes(3))));
        //eventData.getPayload().reDecoded();
        System.out.println(eventData3);

    }

    @Ignore
    @Test
    public void get() {
        LatestLedger latestLedger = consensusChainStore.getLatestLedger();
        System.out.println(latestLedger);
    }

}
