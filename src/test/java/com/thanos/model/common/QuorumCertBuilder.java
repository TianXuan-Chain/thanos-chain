package com.thanos.model.common;

import com.thanos.common.utils.HashUtil;
import com.thanos.chain.consensus.hotstuffbft.model.*;

/**
 * QuorumCertBuilder.java description：
 *
 * @Author laiyiyu create on 2020-06-28 16:17:36
 */
public class QuorumCertBuilder {

    public static QuorumCert buildWithNumber(long number) {

        VoteData voteData = VoteData.build(EventInfoBuilder.buildWithVerifierAndNumber(number), EventInfoBuilder.buildWithoutVerifier());
        LedgerInfo ledgerInfo = LedgerInfo.build(voteData.getProposed(), voteData.getProposed().getHash());

        LedgerInfoWithSignatures ledgerInfoWithSignatures1 = LedgerInfoWithSignatures.build(ledgerInfo, SignatureBuilder.build());
        QuorumCert highestQuorumCert = QuorumCert.build(voteData, ledgerInfoWithSignatures1);
        return highestQuorumCert;
    }

    public static QuorumCert buildHighest() {

        VoteData voteData = VoteData.build(EventInfoBuilder.buildWithVerifierAndNumber(1), EventInfoBuilder.buildWithoutVerifier());
        LedgerInfo ledgerInfo = LedgerInfo.build(voteData.getProposed(), voteData.getProposed().getHash());

        LedgerInfoWithSignatures ledgerInfoWithSignatures1 = LedgerInfoWithSignatures.build(ledgerInfo, SignatureBuilder.build());
        QuorumCert highestQuorumCert = QuorumCert.build(voteData, ledgerInfoWithSignatures1);
        return highestQuorumCert;
    }

    public static QuorumCert buildGensis() {
        LedgerInfo genesisLi = LedgerInfo.buildGenesis(HashUtil.EMPTY_TRIE_HASH, null);
        QuorumCert gensis = QuorumCert.certificateForGenesisFromLedgerInfo(genesisLi, HashUtil.EMPTY_TRIE_HASH);
        return gensis;
    }

}
