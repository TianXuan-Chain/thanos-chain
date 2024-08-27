package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import com.thanos.common.crypto.VerifyingKey;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/**
 * NodeCandidate.java description：
 *
 * @Author laiyiyu create on 2021-03-24 17:39:06
 */
public class NodeCandidate extends RLPModel {

    int processType;

    byte[] proposalId;

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



    public NodeCandidate(byte[] encode) {
        super(encode);
    }

    public NodeCandidate(byte[] id, String name, String agency, String caHash, long consensusVotingPower, int shardingNum, int processType, byte[] proposalId) {
        super(null);
        this.processType = processType;
        this.id = id;
        this.name = name;
        this.agency = agency;
        this.caHash = caHash;
        this.consensusVotingPower = consensusVotingPower;
        this.shardingNum = shardingNum;
        this.processType = processType;
        this.proposalId = proposalId;
        this.rlpEncoded = rlpEncoded();
    }

    public static NodeCandidate convertFrom(VoteNodeCandidateEvent voteNodeCandidateEvent) {
        NodeCandidate nodeCandidate = new NodeCandidate(ByteUtil.copyFrom(voteNodeCandidateEvent.getId()), new String(voteNodeCandidateEvent.getName().getBytes()), new String(voteNodeCandidateEvent.getAgency().getBytes()), new String(voteNodeCandidateEvent.getCaHash().getBytes()), voteNodeCandidateEvent.getConsensusVotingPower(), voteNodeCandidateEvent.getShardingNum(), voteNodeCandidateEvent.getProcessType(), voteNodeCandidateEvent.getProposalId());
        return nodeCandidate;
    }

    public ValidatorPublicKeyInfo convertToValidatorPublicKeyInfo() {

        byte[] publicKey = ByteUtil.copyFrom(id);
        return new ValidatorPublicKeyInfo(publicKey, consensusVotingPower, shardingNum, new VerifyingKey(publicKey), new String(name.getBytes()), new String(agency.getBytes()), new String(caHash.getBytes()));
    }

    public int getProcessType() {
        return processType;
    }

    public String getCaHash() {
        return caHash;
    }

    public byte[] getProposalId() {
        return proposalId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeCandidate that = (NodeCandidate) o;
        return consensusVotingPower == that.consensusVotingPower &&
                shardingNum == that.shardingNum &&
                processType == that.processType &&
                Arrays.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(agency, that.agency) &&
                Objects.equals(caHash, that.caHash) &&
                Arrays.equals(proposalId, that.proposalId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, agency, caHash, consensusVotingPower, shardingNum, processType);
        result = 31 * result + Arrays.hashCode(id);
        return result;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] id = RLP.encodeElement(this.id);
        byte[] name = RLP.encodeString(this.name);
        byte[] agency = RLP.encodeString(this.agency);
        byte[] caHash = RLP.encodeString(this.caHash);
        byte[] consensusVotingPower = RLP.encodeBigInteger(BigInteger.valueOf(this.consensusVotingPower));
        byte[] shardingNum = RLP.encodeInt(this.shardingNum);
        byte[] voteType = RLP.encodeInt(this.processType);
        byte[] proposalId = RLP.encodeElement(this.proposalId);
        return RLP.encodeList(id, name, agency, caHash, consensusVotingPower, shardingNum, voteType, proposalId);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpInfo = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.id = rlpInfo.get(0).getRLPData();
        this.name = new String(rlpInfo.get(1).getRLPData());
        this.agency = new String(rlpInfo.get(2).getRLPData());
        this.caHash = new String(rlpInfo.get(3).getRLPData());
        this.consensusVotingPower = ByteUtil.byteArrayToLong(rlpInfo.get(4).getRLPData());
        this.shardingNum = ByteUtil.byteArrayToInt(rlpInfo.get(5).getRLPData());
        this.processType = ByteUtil.byteArrayToInt(rlpInfo.get(6).getRLPData());
        this.proposalId = rlpInfo.get(7).getRLPData();
    }

    @Override
    public String toString() {
        return "NodeCandidate{" +
                "id=" + Hex.toHexString(id) +
                ", name='" + name + '\'' +
                ", agency='" + agency + '\'' +
                ", caHash='" + caHash + '\'' +
                ", consensusVotingPower=" + consensusVotingPower +
                ", shardingNum=" + shardingNum +
                ", processType=" + processType +
                ", proposalId=" + Hex.toHexString(proposalId) +
                '}';
    }
}
