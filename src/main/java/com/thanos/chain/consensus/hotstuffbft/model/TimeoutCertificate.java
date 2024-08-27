package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.chain.ledger.model.crypto.Signature;

import java.util.Map;
import java.util.TreeMap;

/**
 * 类TimeoutCertificate.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 10:48:18
 */
public class TimeoutCertificate extends RLPModel {

    private Timeout timeout;

    private TreeMap<ByteArrayWrapper, Signature> signatures;

    public TimeoutCertificate(){super(null);}

    public TimeoutCertificate(byte[] encode){super(encode);}

    public static TimeoutCertificate build(Timeout timeout, TreeMap<ByteArrayWrapper, Signature> signatures) {
        TimeoutCertificate timeoutCertificate = new TimeoutCertificate();
        timeoutCertificate.timeout = timeout;
        timeoutCertificate.signatures = signatures;
        return timeoutCertificate;
    }

    public long getEpoch() {
        return timeout.getEpoch();
    }

    public long getRound() {
        return timeout.getRound();
    }

    public TreeMap<ByteArrayWrapper, Signature> getSignatures() {
        return signatures;
    }

    public void addSignature(byte[] author, Signature signature) {
        ByteArrayWrapper keyAuthor = new ByteArrayWrapper(author);
        if (!this.signatures.containsKey(keyAuthor)) {
            this.signatures.put(keyAuthor, signature);
        }
    }

    public void removeSignature(byte[] author) {
        this.signatures.remove(new ByteArrayWrapper(author));
    }

    public ProcessResult<Void> verify(ValidatorVerifier verifier) {
        byte[] hash = this.timeout.getHash();
        return verifier.verifyAggregatedSignature(hash, this.signatures);
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[][] encode = new byte[signatures.size() + 1][];
        encode[0] = this.timeout.getEncoded();
        int i = 1;
        for (Map.Entry<ByteArrayWrapper, Signature> entry: signatures.entrySet()) {
            byte[] key = RLP.encodeElement(entry.getKey().getData());
            byte[] value = RLP.encodeElement(entry.getValue().getSig());
            encode[i] = RLP.encodeList(key, value);
            i++;
        }
        return RLP.encodeList(encode);
    }

    @Override
    protected void rlpDecoded() {
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList timeoutCertificate = (RLPList) params.get(0);
        this.timeout = new Timeout(timeoutCertificate.get(0).getRLPData());

        TreeMap<ByteArrayWrapper, Signature> signatures = new TreeMap<>();
        for (int i = 1; i < timeoutCertificate.size(); i++) {
            RLPList kvBytes = (RLPList) RLP.decode2(timeoutCertificate.get(i).getRLPData()).get(0);
            signatures.put(new ByteArrayWrapper(kvBytes.get(0).getRLPData()), new Signature(kvBytes.get(1).getRLPData()));
        }
        this.signatures = signatures;
    }

    @Override
    public String toString() {
        return "TimeoutCertificate{" +
                "timeout=" + timeout +
                ", signatures=" + signatures +
                '}';
    }
}
