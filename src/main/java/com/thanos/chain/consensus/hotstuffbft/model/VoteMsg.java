package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

import java.beans.Transient;
import java.math.BigInteger;

/**
 * 类VoteMsg.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 14:05:15
 */
public class VoteMsg extends ConsensusMsg {

    private Vote vote;

    private HotstuffChainSyncInfo hotstuffChainSyncInfo;

    private VoteMsg() {super(null);}

    public VoteMsg(byte[] encode) {super(encode);}

    public static VoteMsg build(Vote vote, HotstuffChainSyncInfo hotstuffChainSyncInfo) {
        VoteMsg voteMsg = new VoteMsg();
        voteMsg.epoch = vote.getEpoch();
        voteMsg.hotstuffChainSyncInfo = hotstuffChainSyncInfo;
        voteMsg.vote = vote;
        voteMsg.rlpEncoded = voteMsg.rlpEncoded();
        return voteMsg;
    }

    public Vote getVote() {
        return vote;
    }

    public HotstuffChainSyncInfo getHotstuffChainSyncInfo() {
        return hotstuffChainSyncInfo;
    }

    public long getEpoch() {
        return this.vote.getEpoch();
    }

    @Override
    public ProcessResult<Void> verify(ValidatorVerifier verifier) {
        if (this.vote.getEpoch() != this.hotstuffChainSyncInfo.getEpoch()) {
            return ProcessResult.ofError("VoteMsg has different epoch");
        }

        return this.vote.verify(verifier);
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] epoch = RLP.encodeBigInteger(BigInteger.valueOf(this.epoch));
        byte[] vote = this.vote.getEncoded();
        byte[] hotstuffChainSyncInfo = this.hotstuffChainSyncInfo.getEncoded();
        return RLP.encodeList(epoch, vote, hotstuffChainSyncInfo);
    }

    @Override
    protected void rlpDecoded() {
        RLPList decodeData = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.epoch = ByteUtil.byteArrayToLong(decodeData.get(0).getRLPData());
        this.vote = new Vote(decodeData.get(1).getRLPData());
        this.hotstuffChainSyncInfo = new HotstuffChainSyncInfo(decodeData.get(2).getRLPData());
    }

    @Override
    public byte getCode() {
        return ConsensusCommand.VOTE.getCode();
    }

    @Override
    public ConsensusCommand getCommand() {
        return ConsensusCommand.VOTE;
    }

    @Override
    public String toString() {
        return "VoteMsg{" +
                "vote=" + vote +
                ", hotstuffChainSyncInfo=" + hotstuffChainSyncInfo +
                '}';
    }
}
