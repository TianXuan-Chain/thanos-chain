package com.thanos.model.common;

import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.consensus.hotstuffbft.model.ConsensusPayload;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.ledger.model.event.GlobalEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.eth.EthTransaction;

import java.util.Arrays;
import java.util.HashSet;

/**
 * EventDataBuilder.java description：
 *
 * @Author laiyiyu create on 2020-06-28 16:59:11
 */
public class EventDataBuilder {

    static byte[] author = HashUtil.sha3(new byte[]{111, 121, 122});


    public static EventData buildCommon() {
        return EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(), author, 3, System.currentTimeMillis(), QuorumCertBuilder.buildHighest());
    }

    public static EventData buildGensis() {
        return EventData.buildGenesis(System.currentTimeMillis(), QuorumCertBuilder.buildGensis());
    }

    public static EventData buildWithPayload() {

        byte[] sender =  SecureKey.getInstance("ECDSA",1).getPubKey();

        int num = 3;
        EthTransaction[] ethTransactions = new EthTransaction[num];


        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < num; i++) {
            byte[] receiveAddress = HashUtil.sha3(ByteUtil.longToBytesNoLeadZeroes(currentTime + i));

            EthTransaction tx = new EthTransaction(
                    sender,
                    ByteUtil.longToBytesNoLeadZeroes(currentTime + i),
                    i,
                    ByteUtil.longToBytesNoLeadZeroes(currentTime - i),
                    ByteUtil.longToBytesNoLeadZeroes(currentTime + 3_000_000 + i),
                    receiveAddress,
                    ByteUtil.longToBytesNoLeadZeroes(currentTime - 3_000_000 - i),
                    receiveAddress,
                    new HashSet<>(Arrays.asList(new ByteArrayWrapper(("hehe" + i).getBytes()))),
                    sender
            );

            ethTransactions[i] = tx;

        }

        GlobalNodeEvent[] globalNodeEvents = new GlobalNodeEvent[]{new GlobalNodeEvent(HashUtil.sha3(new byte[]{2, 3}), HashUtil.sha3(new byte[]{2, 3}), 1, (byte)1, HashUtil.sha3(new byte[]{2, 3}),  HashUtil.randomHash() )};


        ConsensusPayload payload = new ConsensusPayload(ethTransactions);

        return EventData.buildProposal(new GlobalEvent(globalNodeEvents), payload, author, 3, System.currentTimeMillis(), QuorumCertBuilder.buildWithNumber(2));
    }
}


