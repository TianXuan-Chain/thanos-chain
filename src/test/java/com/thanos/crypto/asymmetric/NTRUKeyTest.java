package com.thanos.crypto.asymmetric;

import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteUtil;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

/**
 * .java description：
 *
 * @Author lemon819 create on 2020-11-20 11:13:44
 */
public class NTRUKeyTest {
    @Test
    public void test_fromPrivate_when_success() {
        SecureKey ntruKey = SecureKey.getInstance("PQC", 1);
        System.out.println("ntruKey0.keypair: " + Hex.toHexString(ntruKey.getPrivKeyBytes()));
        SecureKey ntruKey1 = SecureKey.fromPrivate(ntruKey.getPrivKeyBytes());
        System.out.println("ntruKey1.keypair: " + Hex.toHexString(ntruKey1.getPrivKeyBytes()));

        System.out.println("ntruKey0.publicKey: " + Hex.toHexString(ntruKey.getPubKey()));
        System.out.println("ntruKey1.publicKey: " + Hex.toHexString(ntruKey1.getPubKey()));
        assert ntruKey.equals(ntruKey1);
    }

    @Test
    public void test__when_verify_success() {
        SecureKey ntruKey = SecureKey.getInstance("PQC", 1);
        String msg = "Hello,world";

        /*---sign---*/
        byte[] sig = ntruKey.sign(msg.getBytes());
        System.out.println("signature: " + ByteUtil.toHexString(sig));

        /*---verify---*/
        boolean flag = ntruKey.verify(msg.getBytes(), sig);
        if (!flag) {
            Assert.fail();
        }
        System.out.println("test_when_verify pass!");
    }
}
