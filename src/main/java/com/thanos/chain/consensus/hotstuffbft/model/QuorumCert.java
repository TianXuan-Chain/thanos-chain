package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.ledger.model.store.Persistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 类QuorumCert.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 14:03:53
 */
public class QuorumCert extends Persistable {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    private VoteData voteData;

    private LedgerInfoWithSignatures ledgerInfoWithSignatures;

    public QuorumCert() {super(null);}

    public QuorumCert(byte[] encode) {super(encode);}

    public static QuorumCert build(VoteData voteData, LedgerInfoWithSignatures ledgerInfoWithSignatures) {
        QuorumCert quorumCert = new QuorumCert();
        quorumCert.voteData = voteData;
        quorumCert.ledgerInfoWithSignatures = ledgerInfoWithSignatures;
        quorumCert.rlpEncoded = quorumCert.rlpEncoded();
        return quorumCert;
    }

    public static QuorumCert certificateForGenesisFromLedgerInfo(LedgerInfo ledgerInfo, byte[] genesisId) {
        EventInfo ancestor = EventInfo.build(ledgerInfo.getEpoch() + 1, 0, genesisId, ledgerInfo.getExecutedStateId(), ledgerInfo.getNumber(), ledgerInfo.getTimestamp(), Optional.empty());
        VoteData voteData = VoteData.build(ancestor, ancestor);
        LedgerInfo li = LedgerInfo.build(ancestor, voteData.getHash());
        return build(voteData, LedgerInfoWithSignatures.build(li, new TreeMap<>()));
    }

    public VoteData getVoteData() {
        return voteData;
    }

    public LedgerInfoWithSignatures getLedgerInfoWithSignatures() {
        return ledgerInfoWithSignatures;
    }

    public EventInfo getCertifiedEvent() {
        return this.voteData.getProposed();
    }

    public long getNumber() {return this.voteData.getProposed().getNumber();}

    public EventInfo getParentEvent() {
        return this.voteData.getParent();
    }

    public EventInfo getCommitEvent() {
        return this.ledgerInfoWithSignatures.getLedgerInfo().getCommitEvent();
    }

    public boolean isEpochChange() {
        return this.ledgerInfoWithSignatures.getLedgerInfo().getNextEpochState().isPresent();
    }

    public ProcessResult<Void> verify(ValidatorVerifier verifier) {
        byte[] voteHash = this.voteData.getHash();
        if (!Arrays.equals(getLedgerInfoWithSignatures().getLedgerInfo().getConsensusDataHash(), voteHash)) {
            logger.warn("Quorum Cert's hash mismatch LedgerInfo," + voteData + ", " + getLedgerInfoWithSignatures().getLedgerInfo());
            return ProcessResult.ofError("Quorum Cert's hash mismatch LedgerInfo");
        }

        // round 为0 的event均为Genesis event, Genesis event 的parent event 为自身
        // Genesis's QC is implicitly agreed upon, it doesn't have real signatures.
        // If someone sends us a QC on a fake genesis, it'll fail to insert into BlockStore
        // because of the round constraint.
        if (getCertifiedEvent().getRound() == 0) {
            if (!getCertifiedEvent().equals(this.getParentEvent())) {
                return ProcessResult.ofError("Genesis QC has inconsistent parent block with certified block");
            }

            if (!getCertifiedEvent().equals(getLedgerInfoWithSignatures().getLedgerInfo().getCommitEvent())) {
                return ProcessResult.ofError("Genesis QC has inconsistent commit block with certified block");
            }

            if (!this.getLedgerInfoWithSignatures().getSignatures().isEmpty()) {
                return ProcessResult.ofError("Genesis QC should not carry signatures");
            }

            return ProcessResult.ofSuccess();
        }

        ProcessResult<Void> verifySignaturesResult = this.getLedgerInfoWithSignatures().verifySignatures(verifier);

        if (!verifySignaturesResult.isSuccess()) {
            return verifySignaturesResult.appendErrorMsg("Fail to verify QuorumCert");
        }

        return voteData.verify();
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] voteData = this.voteData.getEncoded();
        byte[] ledgerInfoWithSignatures = this.ledgerInfoWithSignatures.getEncoded();
        return RLP.encodeList(voteData, ledgerInfoWithSignatures);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.voteData = new VoteData(rlpDecode.get(0).getRLPData());
        this.ledgerInfoWithSignatures = new LedgerInfoWithSignatures(rlpDecode.get(1).getRLPData());
    }

    @Override
    public String toString() {
        return "QuorumCert{" +
                "voteData=" + voteData +
                ", ledgerInfoWithSignatures=" + ledgerInfoWithSignatures +
                '}';
    }

    public void clear() {
//        this.voteData.clear();
//        this.voteData = null;
//        this.ledgerInfoWithSignatures.clear();
//        this.ledgerInfoWithSignatures = null;

    }
}
