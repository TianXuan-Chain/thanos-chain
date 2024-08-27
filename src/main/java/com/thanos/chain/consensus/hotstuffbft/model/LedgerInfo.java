package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.chain.config.Constants;
import com.thanos.common.crypto.CryptoHash;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;

import java.util.List;
import java.util.Optional;

/**
 * 类LedgerInfo.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 14:29:16
 */
public class LedgerInfo extends RLPModel implements CryptoHash {

    private EventInfo commitEvent;

    // equals EventInfo.getHash()
    private byte[] consensusDataHash;

    private LedgerInfo(){
        super(null);
    }

    public LedgerInfo(byte[] encode){
        super(encode);
    }

    public static LedgerInfo build(EventInfo commitEvent, byte[] consensusDataHash) {
        LedgerInfo info = new LedgerInfo();
        info.commitEvent = commitEvent;
        info.consensusDataHash = consensusDataHash;
        info.rlpEncoded = info.rlpEncoded();
        return info;
    }

    public static LedgerInfo buildGenesis(byte[] genesisStateRootHash, List<ValidatorPublicKeyInfo> validatorKeys) {
        return build(EventInfo.buildGenesis(genesisStateRootHash, validatorKeys), Constants.EMPTY_HASH_BYTES);
    }

    public EventInfo getCommitEvent() {
        return commitEvent;
    }

    public void setConsensusDataHash(byte[] consensusDataHash) {
        this.consensusDataHash = consensusDataHash;
        this.rlpEncoded = rlpEncoded();
    }

    public byte[] getConsensusDataHash() {
        return consensusDataHash;
    }

    public long getEpoch() {
        return this.commitEvent.getEpoch();
    }

    public long getRound() {
        return this.commitEvent.getRound();
    }

    public byte[] getConsensusEventId() {
        return this.commitEvent.getId();
    }

    public byte[] getExecutedStateId() {
        return this.commitEvent.getExecutedStateId();
    }

    public long getNumber() {
        return this.commitEvent.getNumber();
    }

    public long getTimestamp() {
        return this.commitEvent.getTimestamp();
    }

    public Optional<EpochState> getNextEpochState() {
        return this.commitEvent.getNextEpochState();
    }

    @Override
    public byte[] getHash() {
        return this.consensusDataHash;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] commitEvent = this.commitEvent.getEncoded();
        byte[] consensusDataHash = RLP.encodeElement(this.consensusDataHash);
        return RLP.encodeList(commitEvent, consensusDataHash);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.commitEvent = new EventInfo(rlpDecode.get(0).getRLPData());

        if (rlpDecode.size() > 1) {
            this.consensusDataHash = rlpDecode.get(1).getRLPData();
        } else {
            this.consensusDataHash = Constants.EMPTY_HASH_BYTES;
        }
    }

    @Override
    public String toString() {
        return "LedgerInfo{" + commitEvent + '}';
    }

//    public void clear() {
//        this.doRemoveCheck.clear();
//        this.doRemoveCheck = null;
//        this.consensusDataHash = null;
//    }
}
