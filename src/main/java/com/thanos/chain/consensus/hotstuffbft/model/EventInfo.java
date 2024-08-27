package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.chain.config.Constants;
import com.thanos.common.crypto.CryptoHash;
import com.thanos.common.crypto.VerifyingKey;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;

import java.math.BigInteger;
import java.util.*;
import org.spongycastle.util.encoders.Hex;

/**
 * 类EventInfo.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 14:04:39
 */
public class EventInfo extends RLPModel implements CryptoHash {

    long epoch;

    long round;
    /**
     * The identifier (hash) of the event.
     */
    byte[] id;

    long timestamp;

    //event height
    long number;
    /**
     * The accumulator root hash after executing this event.
     */
    byte[] executedStateId;

    Optional<EpochState> nextEpochState;

    // consensus hash,  different from  id, it include executedStateId、nextEpochState and so on for consensus
    byte[] transientHash;

    private EventInfo() {super(null);}

    public EventInfo(byte[] encode) {super(encode);}

    public static EventInfo build(long epoch, long round, byte[] id, byte[] executedStateId, long number, long timestamp, Optional<EpochState> nextEpochState) {
        EventInfo info = new EventInfo();
        info.epoch = epoch;
        info.round = round;
        // equals Event.id or EventData->getHash()
        info.id = id;
        info.executedStateId = executedStateId;
        info.number = number;
        info.timestamp = timestamp;
        info.nextEpochState = nextEpochState;
        info.rlpEncoded = info.rlpEncoded();
        info.transientHash = HashUtil.sha3(info.rlpEncoded);
        return info;
    }

    public static EventInfo buildGenesis(byte[] genesisStateRootHash, List<ValidatorPublicKeyInfo> validatorKeys) {
        EpochState epochState = new EpochState(1, null);
        return build(0, 0, Constants.EMPTY_HASH_BYTES, genesisStateRootHash, 0, 0, Optional.of(epochState));
    }

    public static EventInfo empty() {
        EventInfo info = new EventInfo();
        info.epoch = 0L;
        info.round = 0L;
        info.id = Constants.EMPTY_HASH_BYTES;
        info.executedStateId = Constants.EMPTY_HASH_BYTES;
        info.number = 0;
        info.timestamp = 0;
        info.nextEpochState = Optional.empty();
        info.rlpEncoded = info.rlpEncoded();
        info.transientHash = HashUtil.sha3(info.rlpEncoded);
        return info;
    }

    public long getEpoch() {
        return epoch;
    }

    public long getRound() {
        return round;
    }

    public long getNumber() { return number; }

    public byte[] getId() { return id; }

    public byte[] getExecutedStateId() {
        return executedStateId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean hasReconfiguration() {
        return this.nextEpochState.isPresent();
    }

    public Optional<EpochState> getNextEpochState() { return this.nextEpochState; }

    public long getNextEventEpoch() {
        if (nextEpochState.isPresent()) {
            return nextEpochState.get().getEpoch();
        } else {
            return this.epoch;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventInfo eventInfo = (EventInfo) o;
        return epoch == eventInfo.epoch &&
                round == eventInfo.round &&
                timestamp == eventInfo.timestamp &&
                number == eventInfo.number &&
                Arrays.equals(id, eventInfo.id) &&
                Arrays.equals(executedStateId, eventInfo.executedStateId) &&
                Objects.equals(nextEpochState, eventInfo.nextEpochState);}

    @Override
    protected byte[] rlpEncoded() {
        byte[] epoch = RLP.encodeBigInteger(BigInteger.valueOf(this.epoch));
        byte[] round = RLP.encodeBigInteger(BigInteger.valueOf(this.round));
        byte[] id = RLP.encodeElement(this.id);
        byte[] timestamp = RLP.encodeBigInteger(BigInteger.valueOf(this.timestamp));
        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        byte[] executedStateId = RLP.encodeElement(this.executedStateId);
        byte[] nextEpochState = this.nextEpochState.isPresent()?this.nextEpochState.get().getEncoded(): ByteUtil.EMPTY_BYTE_ARRAY;
        return RLP.encodeList(epoch, round, id, timestamp, number, executedStateId, nextEpochState);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.epoch = ByteUtil.byteArrayToLong(rlpDecode.get(0).getRLPData());
        this.round = ByteUtil.byteArrayToLong(rlpDecode.get(1).getRLPData());
        this.id = rlpDecode.get(2).getRLPData();
        this.timestamp = ByteUtil.byteArrayToLong(rlpDecode.get(3).getRLPData());
        this.number = ByteUtil.byteArrayToLong(rlpDecode.get(4).getRLPData());
        this.executedStateId = rlpDecode.get(5).getRLPData();

        if (rlpDecode.size() > 6) {
            this.nextEpochState = Optional.of(new EpochState(rlpDecode.get(6).getRLPData()));
        } else {
            this.nextEpochState = Optional.empty();
        }

        this.transientHash = HashUtil.sha3(rlpEncoded);
    }

    @Override
    public byte[] getHash() {
        return transientHash;
    }

    @Override
    public String toString() {
        return "EventInfo{" +
                "epoch=" + epoch +
                ", round=" + round +
                ", number=" + number +
                ", id=" + Hex.toHexString(id) +
                ", timestamp=" + timestamp +
                ", executedStateId=" + Hex.toHexString(executedStateId) +
               ", nextEpochState=" + nextEpochState +
                ", transientHash=" + Hex.toHexString(transientHash) +
                '}';
    }

    public static void main(String[] args) {
        byte[] id = HashUtil.sha3(new byte[]{11, 22, 33});
        System.out.println(Hex.toHexString(id));
        byte[] exeId = HashUtil.sha3(new byte[]{1, 2, 3});
        System.out.println(Hex.toHexString(exeId));


        ValidatorPublicKeyInfo pk1 = new ValidatorPublicKeyInfo(id, 6, 4, new VerifyingKey(id), "hehe", "hsd1", "hsd2");
        ValidatorPublicKeyInfo pk2 = new ValidatorPublicKeyInfo(exeId, 7, 4, new VerifyingKey(exeId), "hehe1", "hssd1", "hs22");

        System.out.println(new ValidatorPublicKeyInfo(pk1.getEncoded()));

        ValidatorVerifier validatorVerifier = new ValidatorVerifier(Arrays.asList(pk1, pk2));
        System.out.println(new ValidatorVerifier(validatorVerifier.getEncoded()));


        EventInfo eventInfo1 = EventInfo.build(2, 1, id, exeId, 3, System.currentTimeMillis(), Optional.of(new EpochState(4, null)));
        System.out.println(eventInfo1);
        EventInfo rlpEventInfo1 = new EventInfo(eventInfo1.rlpEncoded);
        System.out.println(rlpEventInfo1);

        EventInfo eventInfo2 = EventInfo.build(4, 6, id, exeId, 8, System.currentTimeMillis(), Optional.empty());
        System.out.println(eventInfo2);

        EventInfo rlpEventInfo2 = new EventInfo(eventInfo2.rlpEncoded);
        System.out.println(rlpEventInfo2);
    }

//    public void clear() {
//        this.id = null;
//        this.executedStateId = null;
//        this.nextEpochState = null;

//    }
}