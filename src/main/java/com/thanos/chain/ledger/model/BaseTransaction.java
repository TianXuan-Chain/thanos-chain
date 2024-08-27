package com.thanos.chain.ledger.model;

import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.common.crypto.key.asymmetric.SecurePublicKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;

import static com.thanos.common.utils.ByteUtil.ZERO_BYTE_ARRAY;

/**
 * BaseTransaction.java description：
 *
 * @Author laiyiyu create on 2021-04-14 10:05:59
 */
public abstract class BaseTransaction extends Persistable {

    protected byte[] publicKey;

    protected byte[] nonce;

    protected long futureEventNumber;

    protected byte[] signature;

    //===============================

    protected byte[] hash;

    protected SecurePublicKey securePublicKey;

    protected byte[] sendAddress;

    protected ByteArrayWrapper dsCheck;

    protected boolean valid;

    protected boolean dsCheckValid;

    public BaseTransaction(byte[] rlpEncoded) {
        super(rlpEncoded);
        this.dsCheckValid = true;
    }

    public BaseTransaction(byte[] publicKey, long futureEventNumber, byte[] nonce, byte[] signature) {
        super(null);
        this.publicKey = publicKey;
        this.futureEventNumber = futureEventNumber;
        this.nonce = nonce;
        this.signature = signature;
        this.dsCheckValid = true;
        calculateBase();
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getSignature() {
        return signature;
    }

    public long getFutureEventNumber() {
        return futureEventNumber;
    }


    public byte[] getNonce() {
        return nonce == null ? ZERO_BYTE_ARRAY : nonce;
    }

    public byte[] getHash() {
        return hash;
    }

    public byte[] getSendAddress() {
        return sendAddress;
    }

    public void setDsCheckUnValid() {
        dsCheckValid = false;
    }

    public boolean isDsCheckValid() {
        return dsCheckValid;
    }

    public boolean isValid() {
        return valid;
    }

    //for test
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void verify(boolean txVerify) {

        if (!baseVerify()) {
            valid = false;
            return;
        }

        if (txVerify && !this.securePublicKey.verify(this.hash, signature)) {
            valid = false;
            return;
        }
        valid = true;
    }

    protected boolean baseVerify() {
        return true;
    }

    public ByteArrayWrapper getDsCheck() {
        return dsCheck;
    }

    protected abstract byte[] calculateHash();

    protected void calculateBase() {
        this.securePublicKey = SecurePublicKey.generate(publicKey);
        this.sendAddress = this.securePublicKey.getAddress();
        this.dsCheck = new ByteArrayWrapper(ByteUtil.merge(sendAddress, nonce));
    }
}
