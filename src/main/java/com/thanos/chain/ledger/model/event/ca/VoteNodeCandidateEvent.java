package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * NodeCandidate.java description：
 *
 * @Author laiyiyu create on 2021-03-24 17:39:06
 */
public class VoteNodeCandidateEvent extends VoteEvent {

    // public key
    byte[] id;

    String name;

    // byte to hex string
    String agency;
    // byte to hex string
    String caHash;

    long consensusVotingPower;
    // 分片号
    int shardingNum;

    public VoteNodeCandidateEvent(byte[] encode) {
        super(encode);
    }

    public VoteNodeCandidateEvent(int voteType, int processType, byte[] proposalId, byte[] id, String name, String agency, String caHash, long consensusVotingPower, int shardingNum) {
        super(null);
        this.voteType = voteType;
        this.processType = processType;
        this.proposalId = proposalId;
        this.id = id;
        this.name = name;
        this.agency = agency;
        this.caHash = caHash;
        this.consensusVotingPower = consensusVotingPower;
        this.shardingNum = shardingNum;
        this.rlpEncoded = rlpEncoded();
    }

    @Override
    public GlobalEventCommand getEventCommand() {
        return GlobalEventCommand.VOTE_NODE_CANDIDATE;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] voteType = RLP.encodeInt(this.voteType);
        byte[] processType = RLP.encodeInt(this.processType);
        byte[] proposalId = RLP.encodeElement(this.proposalId);
        byte[] id = RLP.encodeElement(this.id);
        byte[] name = RLP.encodeString(this.name);
        byte[] agency = RLP.encodeString(this.agency);
        byte[] caHash = RLP.encodeString(this.caHash);
        byte[] consensusVotingPower = RLP.encodeBigInteger(BigInteger.valueOf(this.consensusVotingPower));
        byte[] shardingNum = RLP.encodeInt(this.shardingNum);
        return RLP.encodeList(voteType, processType, proposalId, id, name, agency, caHash, consensusVotingPower, shardingNum);
    }


    @Override
    protected void rlpDecoded() {
        RLPList rlpInfo = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.voteType = ByteUtil.byteArrayToInt(rlpInfo.get(0).getRLPData());
        this.processType = ByteUtil.byteArrayToInt(rlpInfo.get(1).getRLPData());
        this.proposalId = rlpInfo.get(2).getRLPData();
        this.id = rlpInfo.get(3).getRLPData();
        this.name = new String(rlpInfo.get(4).getRLPData());
        this.agency = new String(rlpInfo.get(5).getRLPData());
        this.caHash = new String(rlpInfo.get(6).getRLPData());
        this.consensusVotingPower = ByteUtil.byteArrayToLong(rlpInfo.get(7).getRLPData());
        this.shardingNum = ByteUtil.byteArrayToInt(rlpInfo.get(8).getRLPData());
    }

    public int getVoteType() {
        return voteType;
    }

    public byte[] getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAgency() {
        return agency;
    }

    public String getCaHash() {
        return caHash;
    }

    public long getConsensusVotingPower() {
        return consensusVotingPower;
    }

    public int getShardingNum() {
        return shardingNum;
    }

    @Override
    public String toString() {
        return "VoteNodeCandidateEvent{" +
                "id=" + Hex.toHexString(id) +
                ", name='" + name + '\'' +
                ", agency='" + agency + '\'' +
                ", caHash='" + caHash + '\'' +
                ", consensusVotingPower=" + consensusVotingPower +
                ", shardingNum=" + shardingNum +
                '}';
    }
}
