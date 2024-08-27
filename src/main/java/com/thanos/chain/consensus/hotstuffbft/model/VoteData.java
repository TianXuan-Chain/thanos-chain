package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.crypto.CryptoHash;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.ledger.model.RLPModel;
import org.spongycastle.util.encoders.Hex;

/**
 * 类VoteData.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 14:04:16
 */
public class VoteData extends RLPModel implements CryptoHash {

    private EventInfo proposed;

    private EventInfo parent;

    private VoteData(){super(null);}

    public VoteData(byte[] encode) {
        super(encode);
    }

    public static VoteData build(EventInfo proposed, EventInfo parent) {
        VoteData voteData = new VoteData();
        voteData.proposed = proposed;
        voteData.parent = parent;
        voteData.rlpEncoded = voteData.rlpEncoded();
        return voteData;
    }

    public EventInfo getProposed() {
        return proposed;
    }

    public EventInfo getParent() {
        return parent;
    }

    public ProcessResult<Void> verify() {
        if (this.parent.getEpoch() != this.proposed.getEpoch()) {
            return ProcessResult.ofError("Parent and proposed epochs do not match");
        }

        if (this.parent.getRound() >= this.proposed.getRound()) {
            return ProcessResult.ofError("Proposed round is less than parent round");
        }

        if (this.parent.getTimestamp() > this.proposed.getTimestamp()) {
            return ProcessResult.ofError("Proposed happened before parent");
        }

        if (this.parent.getNumber() > this.proposed.getNumber()) {
            return ProcessResult.ofError("Proposed number is less than parent version");
        }

        return ProcessResult.ofSuccess();
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] proposed = this.proposed.getEncoded();
        byte[] parent = this.parent.getEncoded();
        return RLP.encodeList(proposed, parent);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.proposed = new EventInfo(rlpDecode.get(0).getRLPData());
        this.parent = new EventInfo(rlpDecode.get(1).getRLPData());
    }

    @Override
    public byte[] getHash() {
        return proposed.getHash();
    }

//    @Override
//    public String toString() {
//        return "VoteData{" +
//                "proposed id=" + Hex.toHexString(proposed.id) +
//                ", parent id=" + Hex.toHexString(parent.id) +
//                '}';
//    }


    @Override
    public String toString() {
        return "VoteData{" +
                "proposed =" + proposed +
                ", parent =" + parent +
                '}';
    }

//    public void clear() {
//        this.parent.clear();
//        this.parent = null;
//        this.proposed.clear();
//        this.proposed = null;
//    }
}
