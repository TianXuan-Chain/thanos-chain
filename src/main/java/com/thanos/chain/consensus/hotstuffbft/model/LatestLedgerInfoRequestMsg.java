package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * LatestLedgerInfoRequestMsg.java description：
 *
 * @Author laiyiyu create on 2020-07-02 15:04:02
 */
public class LatestLedgerInfoRequestMsg extends ConsensusMsg {

    final static byte[] PLACEHODER = RLP.encodeList(new byte[]{1});

    public LatestLedgerInfoRequestMsg(byte[] encode) {
        super(encode);
    }

    public LatestLedgerInfoRequestMsg() {
        super(null);
        this.rlpEncoded = PLACEHODER;
    }

    protected byte[] rlpEncoded() {
        return PLACEHODER;
    }

    @Override
    protected void rlpDecoded() {
        // not need
    }

    @Override
    public byte getCode() {
        return ConsensusCommand.LATEST_LEDGER_REQ.getCode();
    }

    @Override
    public ConsensusCommand getCommand() {
        return ConsensusCommand.LATEST_LEDGER_REQ;
    }

    @Override
    public String toString() {
        return "LatestLedgerInfoRequestMsg{" +
                "epoch=" + epoch +
                ", nodeId=" + Hex.toHexString(nodeId) +
                '}';
    }
}


