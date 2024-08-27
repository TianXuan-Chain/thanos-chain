package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.chain.ledger.model.store.Persistable;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;

/**
 * EventInfoWithSignatures.java description：
 *
 * @Author laiyiyu create on 2020-07-22 21:45:16
 */
public class EventInfoWithSignatures extends Persistable {

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

    TreeMap<ByteArrayWrapper, Signature> signatures;

    Optional<EpochState> nextEpochState;

    // consensus hash,  different from  id, it include executedStateId、nextEpochState and so on for consensus
    byte[] transientHash;

    private EventInfoWithSignatures() {super(null);}

    public EventInfoWithSignatures(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public static EventInfoWithSignatures buildWithoutDecode(byte[] encode) {
        EventInfoWithSignatures eventData = new EventInfoWithSignatures();
        eventData.rlpEncoded = encode;
        return eventData;
    }

    public static EventInfoWithSignatures build(long epoch, long round, byte[] id, byte[] executedStateId, long number, long timestamp, Optional<EpochState> nextEpochState, TreeMap<ByteArrayWrapper, Signature> signatures) {
        EventInfoWithSignatures info = new EventInfoWithSignatures(null);
        info.epoch = epoch;
        info.round = round;
        // equals Event.id or EventData->getHash()
        info.id = id;
        info.executedStateId = executedStateId;
        info.number = number;
        info.timestamp = timestamp;
        info.signatures = signatures;
        info.nextEpochState = nextEpochState;
        info.rlpEncoded = info.rlpEncoded();
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

    public Optional<EpochState> getNextEpochState() { return this.nextEpochState; }

    public TreeMap<ByteArrayWrapper, Signature> getSignatures() {
        return signatures;
    }

    public byte[] getTransientHash() {
        return transientHash;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] epoch = RLP.encodeBigInteger(BigInteger.valueOf(this.epoch));
        byte[] round = RLP.encodeBigInteger(BigInteger.valueOf(this.round));
        byte[] id = RLP.encodeElement(this.id);
        byte[] timestamp = RLP.encodeBigInteger(BigInteger.valueOf(this.timestamp));
        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        byte[] executedStateId = RLP.encodeElement(this.executedStateId);
        byte[] nextEpochState = this.nextEpochState.isPresent()?this.nextEpochState.get().getEncoded(): ByteUtil.EMPTY_BYTE_ARRAY;
        byte[] info = RLP.encodeList(epoch, round, id, timestamp, number, executedStateId, nextEpochState);
        this.transientHash = HashUtil.sha3(info);

        byte[][] encode = new byte[signatures.size() + 1][];
        encode[0] = info;
        int i = 1;
        for (Map.Entry<ByteArrayWrapper, Signature> entry: signatures.entrySet()) {
            encode[i] =
                    RLP.encodeList(
                            RLP.encodeElement(entry.getKey().getData()),
                            RLP.encodeElement(entry.getValue().getSig())
                    );
            i++;
        }
        return RLP.encodeList(encode);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        RLPList info = (RLPList) RLP.decode2(rlpDecode.get(0).getRLPData()).get(0);

        this.epoch = ByteUtil.byteArrayToLong(info.get(0).getRLPData());
        this.round = ByteUtil.byteArrayToLong(info.get(1).getRLPData());
        this.id = info.get(2).getRLPData();
        this.timestamp = ByteUtil.byteArrayToLong(info.get(3).getRLPData());
        this.number = ByteUtil.byteArrayToLong(info.get(4).getRLPData());
        this.executedStateId = info.get(5).getRLPData();

        if (info.size() > 6) {
            this.nextEpochState = Optional.of(new EpochState(info.get(6).getRLPData()));
        } else {
            this.nextEpochState = Optional.empty();
        }
        this.transientHash = HashUtil.sha3(rlpDecode.get(0).getRLPData());

        TreeMap<ByteArrayWrapper, Signature> signatures = new TreeMap<>();
        for (int i = 1; i < rlpDecode.size(); i++) {
            RLPList kvBytes = (RLPList) RLP.decode2(rlpDecode.get(i).getRLPData()).get(0);
            signatures.put(new ByteArrayWrapper(kvBytes.get(0).getRLPData()), new Signature(kvBytes.get(1).getRLPData()));
        }
        this.signatures = signatures;
    }

    @Override
    public String toString() {
        return "EventInfoWithSignatures{" +
                "epoch=" + epoch +
                ", round=" + round +
                ", id=" + Hex.toHexString(id) +
                ", timestamp=" + timestamp +
                ", number=" + number +
                ", executedStateId=" + Hex.toHexString(executedStateId) +
                ", nextEpochState=" + nextEpochState +
                ", signatures=" + signatures +
                ", transientHash=" + Hex.toHexString(transientHash) +
                '}';
    }
}
