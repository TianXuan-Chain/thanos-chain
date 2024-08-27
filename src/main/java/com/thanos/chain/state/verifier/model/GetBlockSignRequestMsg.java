package com.thanos.chain.state.verifier.model;

import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

import java.math.BigInteger;

/**
 * GetBlockSignRequestMsg.java description：
 *
 * @Author laiyiyu create on 2020-09-25 15:10:43
 */
public class GetBlockSignRequestMsg extends GlobalStateVerifierMsg  {

    private long number;

    public GetBlockSignRequestMsg(long number) {
        super(null);
        this.number = number;
        this.rlpEncoded = rlpEncoded();
    }

    public GetBlockSignRequestMsg(byte[] encode) {
        super(encode);
    }

    public long getNumber() {
        return number;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        return RLP.encodeList(number);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.number = ByteUtil.byteArrayToLong(rlpDecode.get(0).getRLPData());
    }

    @Override
    public byte getCode() {
        return GlobalStateVerifierCommand.GET_BLOCK_SIGN_REQ.getCode();
    }

    @Override
    public GlobalStateVerifierCommand getCommand() {
        return GlobalStateVerifierCommand.GET_BLOCK_SIGN_REQ;
    }

    @Override
    public String toString() {
        return "GetBlockSignRequestMsg{" +
                "number=" + number +
                '}';
    }
}
