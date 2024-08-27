package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.chain.ledger.model.crypto.ValidatorSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.Optional;

/**
 * 类Vote.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 14:15:24
 */
public class Vote extends RLPModel {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    private VoteData voteData;

    private byte[] author;

    private LedgerInfo ledgerInfo;

    private Signature signature;

    private Optional<Signature> timeoutSignature;

    public Vote(){super(null);}

    public Vote(byte[] encode){super(encode);}

    public static Vote build(VoteData voteData, byte[] author, LedgerInfo ledgerInfo, ValidatorSigner validatorSigner) {
        ledgerInfo.setConsensusDataHash(voteData.getHash());
        Vote vote = new Vote();
        vote.voteData = voteData;
        vote.author = author;
        vote.ledgerInfo = ledgerInfo;
        vote.signature = validatorSigner.signMessage(ledgerInfo.getHash()).get();
        vote.timeoutSignature = Optional.empty();
        vote.rlpEncoded = vote.rlpEncoded();
        return vote;
    }

    public void addTimeoutSignature(Signature signature) {
        if (this.timeoutSignature.isPresent()) {
            return;
        }

        this.timeoutSignature = Optional.of(signature);
        this.rlpEncoded = rlpEncoded();
    }

    public VoteData getVoteData() {
        return voteData;
    }

    public byte[] getAuthor() {
        return author;
    }

    public LedgerInfo getLedgerInfo() {
        return ledgerInfo;
    }

    public Signature getSignature() {
        return signature;
    }

    public Optional<Signature> getTimeoutSignature() {
        return timeoutSignature;
    }

    public Timeout getTimeout() {
        return Timeout.build(voteData.getProposed().getEpoch(), voteData.getProposed().getRound());
    }

    public boolean isTimeout() {
        return timeoutSignature.isPresent();
    }

    public long getEpoch() {
        return voteData.getProposed().getEpoch();
    }

    public ProcessResult<Void> verify(ValidatorVerifier verifier) {
        if (!Arrays.equals(this.ledgerInfo.getConsensusDataHash(), voteData.getHash())) {
            return ProcessResult.ofError("Vote's hash mismatch with LedgerInfo");
        }

        VerifyResult verifyRes = verifier.verifySignature(new ByteArrayWrapper(this.author), this.ledgerInfo.getHash(), this.signature);
        if (!verifyRes.isSuccess()) {
            logger.debug("current author:" + Hex.toHexString(author));
            logger.debug("current verifier:" + verifier);
            return ProcessResult.ofError("Failed to verify Vote: " + verifyRes);
        }

        if (this.timeoutSignature.isPresent()) {
            verifyRes = verifier.verifySignature(new ByteArrayWrapper(this.author), this.getTimeout().getHash(), this.timeoutSignature.get());
            if (!verifyRes.isSuccess()) {
                return ProcessResult.ofError("Failed to verify Timeout Vote: " + verifyRes);
            }
        }

        return ProcessResult.ofSuccess();
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] voteData = this.voteData.getEncoded();
        byte[] author = RLP.encodeElement(this.author);
        byte[] ledgerInfo = this.ledgerInfo.getEncoded();
        byte[] signature = RLP.encodeElement(this.signature.getSig());
        byte[] timeoutSignature = this.timeoutSignature.isPresent()?RLP.encodeElement(this.timeoutSignature.get().getSig()): ByteUtil.EMPTY_BYTE_ARRAY;
        return RLP.encodeList(voteData, author, ledgerInfo, signature, timeoutSignature);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.voteData = new VoteData(rlpDecode.get(0).getRLPData());
        this.author = rlpDecode.get(1).getRLPData();
        this.ledgerInfo = new LedgerInfo(rlpDecode.get(2).getRLPData());
        this.signature = new Signature(rlpDecode.get(3).getRLPData());
        if (rlpDecode.size() > 4) {
            this.timeoutSignature = Optional.of(new Signature(rlpDecode.get(4).getRLPData()));
        } else {
            this.timeoutSignature = Optional.empty();
        }
    }

    @Override
    public String toString() {
        return "Vote{" +
                "voteData=" + voteData +
                ", author=" + Hex.toHexString(author) +
                ", ledgerInfo=" + ledgerInfo +
                ", signature=" + signature +
                ", timeoutSignature=" + timeoutSignature +
                '}';
    }
}
