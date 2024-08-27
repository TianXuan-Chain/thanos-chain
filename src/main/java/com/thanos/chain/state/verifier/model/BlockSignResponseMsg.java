package com.thanos.chain.state.verifier.model;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.ledger.model.crypto.Signature;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * BlockSignResponseMsg.java description：
 *
 * @Author laiyiyu create on 2020-09-17 11:04:55
 */
public class BlockSignResponseMsg extends GlobalStateVerifierMsg {

    private long number;

    private byte[] hash;

    private Map<ByteArrayWrapper, Signature> signatures;

    public BlockSignResponseMsg(long number, byte[] hash, Map<ByteArrayWrapper, Signature> signatures) {
        super(null);
        this.number = number;
        this.hash = hash;
        this.signatures = new TreeMap<>(signatures);
        this.rlpEncoded = rlpEncoded();
    }

    public BlockSignResponseMsg(byte[] encode) {
        super(encode);
    }

    public long getNumber() {
        return number;
    }

    public byte[] getHash() {
        return hash;
    }

    public Map<ByteArrayWrapper, Signature> getSignatures() {
        return signatures;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[][] encode = new byte[2 + signatures.size()][];

        encode[0] = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        encode[1] = RLP.encodeElement(this.hash);
        int i = 2;
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
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList block = (RLPList) params.get(0);
        this.number = ByteUtil.byteArrayToLong(block.get(0).getRLPData());
        this.hash = block.get(1).getRLPData() == null? new byte[0]: block.get(1).getRLPData();
        TreeMap<ByteArrayWrapper, Signature> signatures = new TreeMap<>();
        for (int i = 2; i < block.size(); i++) {
            RLPList kvBytes = (RLPList) RLP.decode2(block.get(i).getRLPData()).get(0);
            signatures.put(new ByteArrayWrapper(kvBytes.get(0).getRLPData()), new Signature(kvBytes.get(1).getRLPData()));
        }
        this.signatures = signatures;
    }

    @Override
    public byte getCode() {
        return GlobalStateVerifierCommand.BLOCK_SIGN_RESP.getCode();
    }

    @Override
    public GlobalStateVerifierCommand getCommand() {
        return GlobalStateVerifierCommand.BLOCK_SIGN_RESP;
    }

    @Override
    public String toString() {
        return "BlockSignResponseMsg{" +
                "number=" + number +
                ", hash=" + Hex.toHexString(hash) +
                ", signatures size=" + signatures.size() +
                '}';
    }

    public void releaseReference() {

    }

    public void doRelease() {
        hash = null;
        signatures.clear();
        signatures = null;
    }
}
