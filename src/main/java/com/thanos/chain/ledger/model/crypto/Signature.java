package com.thanos.chain.ledger.model.crypto;

import org.spongycastle.util.encoders.Hex;


/**
 * 类Signature.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 14:36:08
 */
public class Signature {

    final byte[] sig;

    public byte[] getSig() {
        return sig;
    }

    public Signature(byte[] sig) {
        this.sig = sig;
    }

    @Override
    public String toString() {
        return "Signature{" +
                "sig=" + Hex.toHexString(sig) +
                '}';
    }
}
