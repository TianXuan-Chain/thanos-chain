package com.thanos.chain.consensus.hotstuffbft.model;

import com.google.common.collect.Lists;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thanos.chain.consensus.hotstuffbft.model.LatestLedgerInfoResponseMsg.LedgerRetrievalStatus.SUCCESSED;

/**
 * LatestLedgerInfoResponseMsg.java description：
 *
 * @Author laiyiyu create on 2020-07-02 14:40:42
 */
public class LatestLedgerInfoResponseMsg extends ConsensusMsg {

    public enum LedgerRetrievalStatus {
        // Successfully fill in the request.
        SUCCESSED,
        // Can not find the event corresponding to event_id.
        RETRY,
        // Can not find enough events but find some.
        CURRENT_NODE_IS_SYNCING;

        static LedgerRetrievalStatus convertFromOrdinal(int ordinal) {
            if (ordinal == 0) {
                return SUCCESSED;
            } else if (ordinal == 1) {
                return RETRY;
            } else if (ordinal == 2) {
                return CURRENT_NODE_IS_SYNCING;
            } else {
                throw new RuntimeException("ordinal not exit!");
            }
        }
    }

    LedgerRetrievalStatus status;


    LedgerInfoWithSignatures latestLedger;

    QuorumCert latestCommitQC;

    List<Event> threeChainEvents;


    public LatestLedgerInfoResponseMsg(byte[] encode) {
        super(encode);
    }

    public LatestLedgerInfoResponseMsg(LedgerRetrievalStatus status, LedgerInfoWithSignatures latestLedger, QuorumCert latestCommitQC, List<Event> threeChainEvents) {
        super(null);
        this.status = status;
        this.latestLedger = latestLedger;
        this.latestCommitQC = latestCommitQC;
        this.threeChainEvents = threeChainEvents;
        this.rlpEncoded = rlpEncoded();
    }

    public LedgerInfoWithSignatures getLatestLedger() {
        return latestLedger;
    }

    public LedgerRetrievalStatus getStatus() {
        return status;
    }

    public List<Event> getThreeChainEvents() {
        return threeChainEvents;
    }

    public QuorumCert getLatestCommitQC() {
        return latestCommitQC;
    }

    protected byte[] rlpEncoded() {
        byte[][] encode;

        if (status == SUCCESSED) {
            encode = new byte[6][];
            encode[0] = RLP.encodeInt(status.ordinal());
            encode[1] = latestLedger.getEncoded();
            encode[2] = this.latestCommitQC.getEncoded();
            encode[3] = threeChainEvents.get(0).getEncoded();
            encode[4] = threeChainEvents.get(1).getEncoded();
            encode[5] = threeChainEvents.get(2).getEncoded();

        } else {
            encode = new byte[1][];
            encode[0] = RLP.encodeInt(status.ordinal());
        }

        return RLP.encodeList(encode);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.status = LedgerRetrievalStatus.convertFromOrdinal(ByteUtil.byteArrayToInt(rlpDecode.get(0).getRLPData()));

        if (status == SUCCESSED) {
            this.latestLedger = new LedgerInfoWithSignatures(rlpDecode.get(1).getRLPData());
            this.latestCommitQC = new QuorumCert(rlpDecode.get(2).getRLPData());
            this.threeChainEvents = Arrays.asList(new Event(rlpDecode.get(3).getRLPData()), new Event(rlpDecode.get(4).getRLPData()), new Event(rlpDecode.get(5).getRLPData()));
        }
    }

    @Override
    public byte getCode() {
        return ConsensusCommand.LATEST_LEDGER_RESP.getCode();
    }

    @Override
    public ConsensusCommand getCommand() {
        return ConsensusCommand.LATEST_LEDGER_RESP;
    }

    @Override
    public String toString() {
        return "LatestLedgerInfoResponseMsg{" +
                "status=" + status +
                ", latestLedger=" + latestLedger +
                ", latestCommitQC=" + latestCommitQC +
                ", threeChainEvents=" + threeChainEvents +
                ", epoch=" + epoch +
                '}';
    }
}
