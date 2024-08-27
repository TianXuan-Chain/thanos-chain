package com.thanos.model.common;

import com.thanos.common.utils.HashUtil;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import java.util.Optional;

/**
 * HotstuffChainSyncInfoBuilder.java description：
 *
 * @Author laiyiyu create on 2020-06-28 16:29:50
 */
public class HotstuffChainSyncInfoBuilder {

    public static HotstuffChainSyncInfo buildCommon() {
        EventInfo proposal = EventInfoBuilder.buildWithVerifier();
        EventInfo parent = EventInfoBuilder.buildWithoutVerifier();

        VoteData voteData = VoteData.build(proposal, parent);

        LedgerInfo ledgerInfo = LedgerInfo.build(proposal, proposal.getHash());

        LedgerInfoWithSignatures ledgerInfoWithSignatures1 = LedgerInfoWithSignatures.build(ledgerInfo, SignatureBuilder.build());
        QuorumCert highestQuorumCert = QuorumCert.build(voteData, ledgerInfoWithSignatures1);

        LedgerInfo genesisLi = LedgerInfo.buildGenesis(HashUtil.EMPTY_TRIE_HASH, null);
        QuorumCert commitQuorumCert = QuorumCert.certificateForGenesisFromLedgerInfo(genesisLi, HashUtil.EMPTY_TRIE_HASH);


        HotstuffChainSyncInfo hotstuffChainSyncInfo = HotstuffChainSyncInfo.build(highestQuorumCert, commitQuorumCert, Optional.of(TimeoutCertificate.build(Timeout.build(2, 3), SignatureBuilder.build())));


        return hotstuffChainSyncInfo;
    }
}
