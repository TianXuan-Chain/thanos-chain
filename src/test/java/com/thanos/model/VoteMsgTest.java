package com.thanos.model;

import com.thanos.common.utils.HashUtil;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.ledger.model.crypto.ValidatorSigner;
import com.thanos.model.common.EventInfoBuilder;
import com.thanos.model.common.HotstuffChainSyncInfoBuilder;
import org.junit.Test;

/**
 * VoteMsgTest.java description：
 *
 * @Author laiyiyu create on 2020-06-28 16:15:12
 */
public class VoteMsgTest {

    static byte[] author = HashUtil.sha3(new byte[]{111, 121, 122});



    @Test
    public void normal() {
        HotstuffChainSyncInfo hotstuffChainSyncInfo = HotstuffChainSyncInfoBuilder.buildCommon();
        EventInfo proposal = EventInfoBuilder.buildWithVerifier();
        EventInfo parent = EventInfoBuilder.buildWithoutVerifier();
        Vote vote = Vote.build(VoteData.build(proposal, parent), author, LedgerInfo.build(parent, parent.getHash()), new ValidatorSigner(null));
        VoteMsg voteMsg1 = VoteMsg.build(vote, hotstuffChainSyncInfo);
        System.out.println(voteMsg1);
        System.out.println(new VoteMsg(voteMsg1.getEncoded()));
    }
}
