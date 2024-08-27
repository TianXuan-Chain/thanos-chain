package com.thanos.chain.ledger.model.crypto;

import com.thanos.common.crypto.key.asymmetric.SecureKey;

import java.util.Optional;

/**
 * 类ValidatorSigner.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 14:41:24
 */
public class ValidatorSigner {

    private SecureKey secureKey;

    // public key
    public byte[] getAuthor() {
        return secureKey.getPubKey();
    }

    public ValidatorSigner(SecureKey secureKey) {
        this.secureKey = secureKey;
    }

    public Optional<Signature> signMessage(byte[] id) {
        byte[] signature = secureKey.sign(id);
        return Optional.of(new Signature(signature));
    }
}
