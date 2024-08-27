package com.thanos.model.common;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.ledger.model.crypto.Signature;

import java.util.TreeMap;

/**
 * SignatureBuilder.java description：
 *
 * @Author laiyiyu create on 2020-06-28 16:24:30
 */
public class SignatureBuilder {

    static byte[] sig1 = HashUtil.sha3(new byte[]{11, 12, 13});
    static byte[] sig2 = HashUtil.sha3(new byte[]{21, 22, 23});

    public static TreeMap<ByteArrayWrapper, Signature> build() {
        TreeMap<ByteArrayWrapper, Signature> signatures = new TreeMap<>();
        signatures.put(new ByteArrayWrapper(sig1), new Signature(sig1));
        signatures.put(new ByteArrayWrapper(sig2), new Signature(sig2));
        return signatures;
    }
}
