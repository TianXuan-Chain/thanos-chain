package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

/**
 * VoteNodeBlackListCandidateEvent.java description：
 *
 * @Author laiyiyu create on 2021-05-12 16:59:07
 */
public class VoteNodeBlackListCandidateEvent extends VoteEvent {

    byte[] publicKey;

    byte[] caHash;

    public VoteNodeBlackListCandidateEvent(int voteType, int processType, byte[] proposalId, byte[] publicKey, byte[] caHash) {
        super(null);
        this.voteType = voteType;
        this.processType = processType;
        this.proposalId = proposalId;
        this.publicKey = publicKey;
        this.caHash = caHash;
        this.rlpEncoded = rlpEncoded();
    }

    public VoteNodeBlackListCandidateEvent(byte[] encode) {
        super(encode);
    }

    @Override
    public GlobalEventCommand getEventCommand() {
        return GlobalEventCommand.VOTE_NODE_BLACKLIST_CANDIDATE;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] voteType = RLP.encodeInt(this.voteType);
        byte[] processType = RLP.encodeInt(this.processType);
        byte[] proposalId = RLP.encodeElement(this.proposalId);
        byte[] publicKey = RLP.encodeElement(this.publicKey);
        byte[] caHash = RLP.encodeElement(this.caHash);
        return RLP.encodeList(voteType, processType, proposalId, publicKey, caHash);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpInfo = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.voteType = ByteUtil.byteArrayToInt(rlpInfo.get(0).getRLPData());
        this.processType = ByteUtil.byteArrayToInt(rlpInfo.get(1).getRLPData());
        this.proposalId = ByteUtil.copyFrom(rlpInfo.get(2).getRLPData());
        this.publicKey = ByteUtil.copyFrom(rlpInfo.get(3).getRLPData());
        this.caHash = ByteUtil.copyFrom(rlpInfo.get(4).getRLPData());
    }
}
