package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

/**
 * VoteFilterCandidateEvent.java description：
 *
 * @Author laiyiyu create on 2021-03-31 16:18:36
 */
public class VoteFilterCandidateEvent extends VoteEvent  {

    CaContractCode caContractCode;

    public VoteFilterCandidateEvent(byte[] encode) {
        super(encode);
    }

    public VoteFilterCandidateEvent(int voteType, int processType, byte[]proposalId, CaContractCode caContractCode) {
        super(null);
        this.voteType = voteType;
        this.processType = processType;
        this.proposalId = proposalId;
        this.caContractCode = caContractCode;
        this.rlpEncoded = rlpEncoded();
    }

    @Override
    public GlobalEventCommand getEventCommand() {
        return GlobalEventCommand.VOTE_FILTER_CANDIDATE;
    }


    public CaContractCode getCaContractCode() {
        return caContractCode;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] voteType = RLP.encodeInt(this.voteType);
        byte[] processType = RLP.encodeInt(this.processType);
        byte[] proposalId = RLP.encodeElement(this.proposalId);
        byte[] voteCommitteeAddr = this.caContractCode.getEncoded();
        return RLP.encodeList(voteType, processType, proposalId, voteCommitteeAddr);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpInfo = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.voteType = ByteUtil.byteArrayToInt(rlpInfo.get(0).getRLPData());
        this.processType = ByteUtil.byteArrayToInt(rlpInfo.get(1).getRLPData());
        this.proposalId = ByteUtil.copyFrom(rlpInfo.get(2).getRLPData());
        this.caContractCode = new CaContractCode(ByteUtil.copyFrom(rlpInfo.get(3).getRLPData()));
    }
}
