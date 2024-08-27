package com.thanos.chain.ledger.model.event;

import com.thanos.chain.ledger.model.BaseTransaction;
import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * GlobalNodeEvent.java description：
 *
 * @Author laiyiyu create on 2020-07-09 11:03:51
 */
public class GlobalNodeEvent extends BaseTransaction {

//    public enum NodeEventType {
//        // Successfully fill in the request.
//        REGISTER_PROCESS,
//        // Can not find the event corresponding to number.
//        DISAGREE_VOTE;
//        public static GlobalNodeEvent.NodeEventType convertFromOrdinal(int ordinal) {
//            if (ordinal == 0) {
//                return REGISTER_PROCESS;
//            } else if (ordinal == 1) {
//                return DISAGREE_VOTE;
//            } else {
//                throw new RuntimeException("ordinal not exit!");
//            }
//        }
//    }


    byte commandCode;

    byte[] data;

    CommandEvent commandEvent;

    //=====================


    public GlobalNodeEvent(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public GlobalNodeEvent(byte[] publicKey, byte[] nonce, long futureEventNumber, byte commandCode, byte[] data, byte[] signature) {
        super(publicKey, futureEventNumber, nonce, signature);

        this.commandCode = commandCode;
        this.data = data;
        this.commandEvent = CommandEvent.build(commandCode, this.data);

        this.hash = calculateHash();
        this.rlpEncoded = rlpEncoded();
    }


    //for speed
    public GlobalNodeEvent(byte[] hash, byte[] publicKey, byte[] nonce, long futureEventNumber, byte commandCode, byte[] data, byte[] signature) {
        super(publicKey, futureEventNumber, nonce, signature);

        this.commandCode = commandCode;
        this.data = data;
        this.commandEvent = CommandEvent.build(commandCode, this.data);

        this.hash = hash;
        this.valid = true;
        this.rlpEncoded = rlpEncoded();
    }

    public GlobalEventCommand getGlobalEventCommand() {
        return GlobalEventCommand.fromByte(commandCode);
    }

    public byte getCommandCode() {
        return commandCode;
    }

    public byte[] getData() {
        return data;
    }

    public CommandEvent getCommandEvent() {
        return commandEvent;
    }

    public ByteArrayWrapper getDsCheck() {
        return dsCheck;
    }

    @Override
    protected byte[] calculateHash() {
        return HashUtil.sha3Dynamic(this.publicKey, this.nonce, BigIntegers.asUnsignedByteArray(BigInteger.valueOf(this.futureEventNumber)), BigIntegers.asUnsignedByteArray(BigInteger.valueOf(this.commandCode)), this.data);
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] publicKey = RLP.encodeElement(this.publicKey);
        byte[] nonce = RLP.encodeElement(this.nonce);
        byte[] futureEventNumber = RLP.encodeBigInteger(BigInteger.valueOf(this.futureEventNumber));
        byte[] commandCode = RLP.encodeByte(this.commandCode);
        byte[] data = RLP.encodeElement(this.data);
        byte[] signature = RLP.encodeElement(this.signature);
        return RLP.encodeList(publicKey, nonce, futureEventNumber, commandCode, data, signature);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpInfo = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.publicKey = rlpInfo.get(0).getRLPData();
        this.nonce = rlpInfo.get(1).getRLPData();
        this.futureEventNumber = ByteUtil.byteArrayToLong(rlpInfo.get(2).getRLPData());


        this.commandCode = (byte) ByteUtil.byteArrayToInt(rlpInfo.get(3).getRLPData());
        this.data = rlpInfo.get(4).getRLPData();
        this.commandEvent = CommandEvent.build(commandCode, data);

        this.signature = rlpInfo.get(5).getRLPData();

        this.hash = calculateHash();
        calculateBase();

    }

    @Override
    public String toString() {
        return "GlobalNodeEvent{" +
                "commandEvent=" + commandEvent +
                ", hash=" + Hex.toHexString(hash) +
                '}';
    }
}
