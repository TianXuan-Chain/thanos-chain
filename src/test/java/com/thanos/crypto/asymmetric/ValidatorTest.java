package com.thanos.crypto.asymmetric;

import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.crypto.VerifyingKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.consensus.hotstuffbft.model.ValidatorVerifier;
import com.thanos.chain.consensus.hotstuffbft.model.VerifyResult;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.chain.ledger.model.crypto.ValidatorSigner;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 类ValidatorSignerTest.java的实现描述：ValidatorSigner签名验签Test类
 *
 * @author xuhao create on 2020/9/23 10:31
 */

public class ValidatorTest {
    @Test
    public void test_sign_and_verify_when_success() {
        SecureKey secureKey = SecureKey.getInstance("ECDSA",1);
        String msg = "Hello,world";
        byte[] digest = HashUtil.sha3(msg.getBytes());
        //sign
        ValidatorSigner signer = new ValidatorSigner(secureKey);
        Signature sig = signer.signMessage(digest).get();
        //verify
        ValidatorPublicKeyInfo validatorPublicKeyInfo = new ValidatorPublicKeyInfo(secureKey.getPubKey(), 1, 1, new VerifyingKey(secureKey.getPubKey()), "Alice", "agency1", "caHash1");
        List<ValidatorPublicKeyInfo> publicKeyInfos = new ArrayList<>();
        publicKeyInfos.add(validatorPublicKeyInfo);
        ValidatorVerifier verifier = new ValidatorVerifier(publicKeyInfos);
        VerifyResult verifyRes = verifier.verifySignature(new ByteArrayWrapper(secureKey.getPubKey()), digest, sig);
        assert VerifyResult.VerifyStatus.Success.equals(verifyRes.getStatus());
    }
}
