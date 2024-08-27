package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.event.CommandEvent;
import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.util.Objects;

/**
 * CommitteeCandidate.java description：
 *
 * @Author laiyiyu create on 2021-03-08 14:36:57
 */
public class ProcessOperationsStaffCandidateEvent extends CommandEvent {

    int processType;

    byte[] address;

    public ProcessOperationsStaffCandidateEvent(byte[] encode) {
        super(encode);
    }

    @Override
    public GlobalEventCommand getEventCommand() {
        return GlobalEventCommand.PROCESS_OPERATIONS_STAFF;
    }

    public ProcessOperationsStaffCandidateEvent(int processType, byte[] address) {
        super(null);
        this.processType = processType;
        this.address = address;
        this.rlpEncoded = rlpEncoded();
    }

    public int getProcessType() {
        return processType;
    }

    public byte[] getAddress() {
        return address;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] voteType = RLP.encodeInt(this.processType);
        byte[] address = RLP.encodeElement(this.address);
        return RLP.encodeList(voteType, address);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpInfo = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.processType = ByteUtil.byteArrayToInt(rlpInfo.get(0).getRLPData());
        this.address = ByteUtil.copyFrom(rlpInfo.get(1).getRLPData());

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessOperationsStaffCandidateEvent that = (ProcessOperationsStaffCandidateEvent) o;
        return processType == that.processType &&
                Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processType, address);
    }

    @Override
    public String toString() {
        return "ProcessOperationsStaffCandidateEvent{" +
                "processType=" + processType +
                ", address=" + Hex.toHexString(address) +
                '}';
    }
}
