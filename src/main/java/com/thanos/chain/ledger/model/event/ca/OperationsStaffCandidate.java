package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

import java.util.Objects;

/**
 * CommitteeCandidate.java description：
 *
 * @Author laiyiyu create on 2021-03-08 14:36:57
 */
public class OperationsStaffCandidate extends RLPModel {

    int processType;

    ByteArrayWrapper address;

    public OperationsStaffCandidate(byte[] encode) {
        super(encode);
    }

    public OperationsStaffCandidate(int processType, ByteArrayWrapper address) {
        super(null);
        this.processType = processType;
        this.address = address;
        this.rlpEncoded = rlpEncoded();
    }

    public int getProcessType() {
        return processType;
    }

    public ByteArrayWrapper getAddress() {
        return address;
    }

    public static OperationsStaffCandidate convertFrom(VoteCommitteeCandidateEvent candidateEvent) {
        OperationsStaffCandidate committeeCandidate = new OperationsStaffCandidate(candidateEvent.getProcessType(), new ByteArrayWrapper(ByteUtil.copyFrom(candidateEvent.getVoteCommitteeAddr())));
        return committeeCandidate;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] voteType = RLP.encodeInt(this.processType);
        byte[] address = RLP.encodeElement(this.address.getData());
        return RLP.encodeList(voteType, address);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpInfo = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.processType = ByteUtil.byteArrayToInt(rlpInfo.get(0).getRLPData());
        this.address = new ByteArrayWrapper(ByteUtil.copyFrom(rlpInfo.get(1).getRLPData()));

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperationsStaffCandidate that = (OperationsStaffCandidate) o;
        return processType == that.processType &&
                Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processType, address);
    }

    @Override
    public String toString() {
        return "OperationsStaffCandidate{" +
                "processType=" + processType +
                ", address=" + address +
                '}';
    }
}
