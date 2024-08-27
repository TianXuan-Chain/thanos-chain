package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

import java.math.BigInteger;
import java.util.Optional;

/**
 * 类ChainSyncInfo.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 11:16:51
 */
public class HotstuffChainSyncInfo extends ConsensusMsg {

    private QuorumCert highestQuorumCert;

    private QuorumCert highestCommitCert;

    private Optional<TimeoutCertificate> highestTimeoutCert;

    private HotstuffChainSyncInfo() {super(null);}

    public HotstuffChainSyncInfo(byte[] encode) {
        super(encode);
    }

    public static HotstuffChainSyncInfo build(QuorumCert highestQuorumCert, QuorumCert highestCommitCert, Optional<TimeoutCertificate> highestTimeoutCert) {
        HotstuffChainSyncInfo syncInfo = new HotstuffChainSyncInfo();
        syncInfo.highestQuorumCert = highestQuorumCert;
        syncInfo.highestCommitCert = highestCommitCert;
        syncInfo.highestTimeoutCert = highestTimeoutCert;
        syncInfo.epoch = highestQuorumCert.getCertifiedEvent().getEpoch();
        syncInfo.rlpEncoded = syncInfo.rlpEncoded();
        return syncInfo;
    }

    public static HotstuffChainSyncInfo buildWithoutEncode(QuorumCert highestQuorumCert, QuorumCert highestCommitCert, Optional<TimeoutCertificate> highestTimeoutCert) {
        HotstuffChainSyncInfo syncInfo = new HotstuffChainSyncInfo();
        syncInfo.highestQuorumCert = highestQuorumCert;
        syncInfo.highestCommitCert = highestCommitCert;
        syncInfo.highestTimeoutCert = highestTimeoutCert;
        syncInfo.epoch = highestQuorumCert.getCertifiedEvent().getEpoch();
        return syncInfo;
    }

    public QuorumCert getHighestQuorumCert() {
        return highestQuorumCert;
    }

    public QuorumCert getHighestCommitCert() {
        return highestCommitCert;
    }

    public Optional<TimeoutCertificate> getHighestTimeoutCert() {
        return highestTimeoutCert;
    }

    public long getHCCRound() {
        return this.highestCommitCert.getCommitEvent().getRound();
    }

    public long getHQCRound() {
        return this.highestQuorumCert.getCertifiedEvent().getRound();
    }

    public long getHTCRound() {
        return highestTimeoutCert.isPresent() ?
                this.highestTimeoutCert.get().getRound() : 0;
    }

    public long getHighestRound() {
        return Math.max(getHQCRound(), getHTCRound());
    }

    public long getEpoch() {
        return this.highestQuorumCert.getCertifiedEvent().getEpoch();
    }

    public ProcessResult<Void> verify(ValidatorVerifier verifier) {
        long epoch = this.highestQuorumCert.getCertifiedEvent().getEpoch();

        if (epoch != this.highestCommitCert.getCertifiedEvent().getEpoch()) {
            return ProcessResult.ofError("Multi epoch in SyncInfo - HCC and HQC");
        }

        if (this.getHighestTimeoutCert().isPresent()) {
            if (epoch != this.getHighestTimeoutCert().get().getEpoch()) {
                return ProcessResult.ofError("Multi epoch in SyncInfo - TC and HQC");

            }
        }

        if (this.highestQuorumCert.getCertifiedEvent().getRound() < this.highestCommitCert.getCertifiedEvent().getRound()) {
            return ProcessResult.ofError("HQC has lower round than HCC");
        }

        if (this.highestCommitCert.getCommitEvent().equals(EventInfo.empty())) {
            return ProcessResult.ofError("HCC has no committed block");
        }

        ProcessResult<Void> verifyRes = this.highestQuorumCert.verify(verifier);
        if (!verifyRes.isSuccess()) {
            return verifyRes.appendErrorMsg("sync info, highestQuorumCert.verify error!");
        }

        verifyRes = this.highestCommitCert.verify(verifier);
        if (!verifyRes.isSuccess()) {
            return verifyRes.appendErrorMsg("sync info, highestCommitCert.verify error!");
        }

        if (this.highestTimeoutCert.isPresent()) {
            verifyRes = this.highestTimeoutCert.get().verify(verifier);
            if (!verifyRes.isSuccess()) {
                return verifyRes.appendErrorMsg("sync info, highestTimeoutCert.verify error!");
            }
        }

        return ProcessResult.ofSuccess();
    }

    public boolean hasNewerCertificates(HotstuffChainSyncInfo syncInfo) {
        return this.getHQCRound() > syncInfo.getHQCRound()
                || this.getHTCRound() > syncInfo.getHTCRound()
                || this.getHCCRound() > syncInfo.getHCCRound();
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] epoch = RLP.encodeBigInteger(BigInteger.valueOf(this.epoch));
        byte[] highestQuorumCert = this.highestQuorumCert.getEncoded();
        byte[] highestCommitCert = this.highestCommitCert.getEncoded();
        byte[] highestTimeoutCert = this.highestTimeoutCert.isPresent()? this.highestTimeoutCert.get().getEncoded(): ByteUtil.EMPTY_BYTE_ARRAY;
        return RLP.encodeList(epoch, highestQuorumCert, highestCommitCert, highestTimeoutCert);
    }

    @Override
    protected void rlpDecoded() {
        RLPList decodeData = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.epoch = ByteUtil.byteArrayToLong(decodeData.get(0).getRLPData());
        this.highestQuorumCert = new QuorumCert(decodeData.get(1).getRLPData());
        this.highestCommitCert = new QuorumCert(decodeData.get(2).getRLPData());
        if (decodeData.size() > 3) {
            this.highestTimeoutCert = Optional.of(new TimeoutCertificate(decodeData.get(3).getRLPData()));
        } else {
            this.highestTimeoutCert = Optional.empty();
        }
    }

    @Override
    public byte getCode() {
        return ConsensusCommand.HOTSTUFF_CHAIN_SYNC.getCode();
    }

    @Override
    public ConsensusCommand getCommand() {
        return ConsensusCommand.HOTSTUFF_CHAIN_SYNC;
    }

    @Override
    public String toString() {
        return "HotstuffChainSyncInfo{" +
                "highestQuorumCert=" + highestQuorumCert +
                ", highestCommitCert=" + highestCommitCert +
                ", highestTimeoutCert=" + highestTimeoutCert +
                '}';
    }
}
