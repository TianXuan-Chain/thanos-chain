package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.ledger.model.store.Persistable;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;

/**
 * ExecutedEventOutput.java description：
 *
 * @Author laiyiyu create on 2020-03-04 17:10:19
 */
public class ExecutedEventOutput extends Persistable {

    long eventNumber;

    byte[] stateHash;

    byte[] stateRoot;

    //变化增量
    Map<Keyable.DefaultKeyable, byte[]> output;

    //Transient
    Optional<EpochState> epochState;


    public ExecutedEventOutput(byte[] encode) {
        super(encode);
    }



    public ExecutedEventOutput(Map<Keyable.DefaultKeyable, byte[]> output, long eventNumber, byte[] parentStateRoot, Optional<EpochState> epochState) {
        super(null);
        this.output = output;
        this.eventNumber = eventNumber;
        this.epochState = epochState;
        byte[] mergedArray = merge(output);
        if (output.size() == 0) {
            this.stateHash = HashUtil.EMPTY_DATA_HASH;
            this.stateRoot = parentStateRoot;
        } else {
            this.stateHash = HashUtil.sha3(mergedArray);
            this.stateRoot = HashUtil.sha3(parentStateRoot, stateHash);
        }

        if (epochState.isPresent()) {
            this.stateHash = HashUtil.sha3(stateHash, epochState.get().getEncoded());
            this.stateRoot = HashUtil.sha3(stateRoot, stateHash);
        }

        // lazy
        this.rlpEncoded = rlpEncoded();
    }


    public ExecutedEventOutput(long eventNumber, byte[] stateHash, byte[] stateRoot, Map<Keyable.DefaultKeyable, byte[]> output, Optional<EpochState> epochState) {
        super(null);
        this.eventNumber = eventNumber;
        this.stateHash = stateHash;
        this.stateRoot = stateRoot;
        this.output = output;
        this.epochState = epochState;
        this.rlpEncoded = rlpEncoded();
    }

    public Map<Keyable.DefaultKeyable, byte[]> getOutput() {
        return output;
    }

    public Optional<EpochState> getEpochState() {
        return epochState;
    }

//    public void resetEpcohState() {
//        this.epochState = Optional.empty();
//    }


    public void setEpochState(Optional<EpochState> epochState) {
        this.epochState = epochState;
    }

    public boolean hasReconfiguration() { return epochState.isPresent(); }

    public long getEventNumber() {
        return eventNumber;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public byte[] getStateHash() {
        return stateHash;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[][] encode = new byte[3 + output.size()][];

        encode[0] = RLP.encodeBigInteger(BigInteger.valueOf(this.eventNumber));
        encode[1] = RLP.encodeElement(this.stateHash);
        encode[2] = RLP.encodeElement(this.stateRoot);
        int i = 3;
        for (Map.Entry<Keyable.DefaultKeyable, byte[]> entry: output.entrySet()) {
            encode[i] =
                    RLP.encodeList(
                            RLP.encodeElement(entry.getKey().keyBytes()),
                            RLP.encodeElement(entry.getValue())
                    );
            i++;
        }
        return RLP.encodeList(encode);
    }

    @Override
    protected void rlpDecoded() {
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList exeOutput = (RLPList) params.get(0);
        this.eventNumber = ByteUtil.byteArrayToLong(exeOutput.get(0).getRLPData());
        this.stateHash = exeOutput.get(1).getRLPData();
        this.stateRoot = exeOutput.get(2).getRLPData();

        Map<Keyable.DefaultKeyable, byte[]> output = new HashMap<>();
        for (int i = 3; i < exeOutput.size(); i++) {
            RLPList kvBytes = (RLPList) RLP.decode2(exeOutput.get(i).getRLPData()).get(0);
            output.put(Keyable.ofDefault(kvBytes.get(0).getRLPData()), kvBytes.get(1).getRLPData());
        }
        this.output = output;
    }

    @Override
    public String toString() {
        return "ExecutedEventOutput{" +
                "eventNumber=" + eventNumber +
                ", stateHash=" + Hex.toHexString(stateHash) +
                ", stateRoot=" + Hex.toHexString(stateRoot) +
                ", output=" + output +
                ", epochState=" + epochState +
                '}';
    }

    public static byte[] merge(Map<Keyable.DefaultKeyable, byte[]> input) {
        int count = 0;
        for (Map.Entry<Keyable.DefaultKeyable, byte[]> entry: input.entrySet())
        {
            count += (entry.getKey().keyBytes().length + entry.getValue().length);
        }
        byte[] mergedArray = new byte[count];
        int start = 0;
        for (Map.Entry<Keyable.DefaultKeyable, byte[]> entry: input.entrySet())
        {
            System.arraycopy(entry.getKey().keyBytes(), 0, mergedArray, start, entry.getKey().keyBytes().length);
            start += entry.getKey().keyBytes().length;
            System.arraycopy(entry.getValue(), 0, mergedArray, start, entry.getValue().length);
            start += entry.getValue().length;
        }
        return mergedArray;
    }

    public ExecutedEventOutput copy() {
        ExecutedEventOutput output = new ExecutedEventOutput(ByteUtil.copyFrom(this.rlpEncoded));
        if (epochState.isPresent()) {
            output.setEpochState(Optional.of(new EpochState(ByteUtil.copyFrom(this.epochState.get().getEncoded()))));
        } else {
            output.setEpochState(Optional.empty());
        }

        return output;
    }

//    public void clear() {
//        stateHash = null;
//        stateRoot = null;
//        output.clear();
//        epochState = null;
//    }
}
