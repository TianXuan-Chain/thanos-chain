package com.thanos.model.common;

import com.thanos.common.crypto.VerifyingKey;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.consensus.hotstuffbft.model.ValidatorVerifier;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * ValidatorVerifierBuilder.java description：
 *
 * @Author laiyiyu create on 2020-06-28 16:20:37
 */
public class ValidatorVerifierBuilder {

    public static ValidatorVerifier build() {
        byte[] id = Hex.decode("010001040d3a176a1e51f68e04deda9c6437543dcd87db185b970476c611052b106a2422af7c496e09fd7f6215284ed83cb3b66bc24f9a318eded9ec7f6722fc52616e29");
        byte[] exeId = Hex.decode("010001047cbf053e81cc2cd1896fc5470c428cad1432cc53d19976a40fa70cae0e3a4415cc1648fc37dc07b5fef0f2a7da71093a88f308adb0c8ca24b0dfe983f78a04f7");
        //byte[] exeId = HashUtil.sha3(new byte[]{1, 2, 3});
        ValidatorPublicKeyInfo pk1 = new ValidatorPublicKeyInfo(id, 6, 4, new VerifyingKey(id), "hehe", "hehe1", "caca2");
        ValidatorPublicKeyInfo pk2 = new ValidatorPublicKeyInfo(exeId, 7, 4, new VerifyingKey(exeId), "hehe1", "hehe3", "caca2");
        ValidatorVerifier validatorVerifier = new ValidatorVerifier(Arrays.asList(pk1, pk2));
        return validatorVerifier;
    }
}
