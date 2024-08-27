package com.thanos.chain.state.verifier.model;

import com.thanos.chain.ledger.model.crypto.Signature;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * LocalBlockSignMsg.java description：
 *
 * @Author laiyiyu create on 2020-09-25 15:41:47
 */
public class LocalBlockSignMsg extends GlobalStateVerifierMsg {

    public long epoch;

    public long number;

    public byte[] hash;

    public byte[] publicKey;

    public Signature signature;



    public LocalBlockSignMsg(long epoch, long number, byte[] hash, byte[] publicKey, Signature signature) {
        super(null);
        this.epoch = epoch;
        this.number = number;
        this.hash = hash;
        this.publicKey = publicKey;
        this.signature = signature;
    }

    @Override
    public byte getCode() {
        return GlobalStateVerifierCommand.LOCAL_BLOCK_SIGN.getCode();
    }

    @Override
    public GlobalStateVerifierCommand getCommand() {
        return GlobalStateVerifierCommand.LOCAL_BLOCK_SIGN;
    }

    @Override
    public String toString() {
        return "LocalBlockSignMsg{" +
                "epoch=" + epoch +
                ", number=" + number +
                ", hash=" + Hex.toHexString(hash) +
                '}';
    }

    @Override
    public void releaseReference() {
        this.hash = null;
        this.publicKey = null;
        this.signature = null;
    }
}
