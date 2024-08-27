package com.thanos.crypto.asymmetric;

import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.HashUtil;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

/**
 * 类ECKeyTest.java的实现描述：ECKey测试类
 *
 * @author xuhao create on 2020/9/21 19:57
 */

public class ECKeyTest {
    @Test
    public void test_fromPrivate_when_success() {
        SecureKey ecKey = SecureKey.getInstance("ECDSA",1);
        System.out.println("ecKey.privKey: " + Hex.toHexString(ecKey.getPrivKeyBytes()));
        SecureKey ecKey1 = SecureKey.fromPrivate(ecKey.getPrivKeyBytes());
        System.out.println("ecKey1.privKey: " + Hex.toHexString(ecKey1.getPrivKeyBytes()));
        assert ecKey.equals(ecKey1);
    }


    @Test
    public void test__when_verify_success() {
        SecureKey ecKey = SecureKey.getInstance("ECDSA",1);
        String msg = "Hello,world";
        byte[] digest = HashUtil.sha3(msg.getBytes());
        //签名
        byte[] sig = ecKey.sign(digest);
        //验签-静态方法
        assert ecKey.verify(digest, sig);
    }

    /**
     * 方法：签名、验签
     * case：新建密钥对、签名、验签 成功
     */
    @Test
    public void test_sign_and_verify_when_success() {
        SecureKey ecKey = SecureKey.getInstance("ECDSA",1);
        String msg = "Hello,world";
        byte[] digest = HashUtil.sha3(msg.getBytes());
        //签名
        byte[] sig = ecKey.sign(digest);
        //验签
        assert ecKey.verify(digest, sig);
    }

    @Test
    public void testPerformance() {
        int totalSize = 5000;
        SecureKey[] eckeys = new SecureKey[totalSize];
        byte[][] sigs = new byte[totalSize][];
        String msg = "Hello,world";
        byte[] digest = HashUtil.sha3(msg.getBytes());
        for (int i = 0; i < totalSize;i++) {
            eckeys[i] = SecureKey.getInstance("ED25519",1);
            sigs[i] = eckeys[i].sign(digest);
        }

        System.out.println("generate key success!");

        for (int i = 0; i < 50; i++) {
            long star = System.currentTimeMillis();
            doTestPerformance(eckeys, sigs, digest);
            long end = System.currentTimeMillis();
            System.out.println("use:" + (end - star));
        }
    }

    private void doTestPerformance(SecureKey[] eckeys, byte[][] sigs, byte[] digest) {
        for (int i = 0; i < eckeys.length; i++) {
            eckeys[i].verify(digest, sigs[i]);
        }
    }


}
