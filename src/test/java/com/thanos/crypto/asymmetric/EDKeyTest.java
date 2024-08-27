package com.thanos.crypto.asymmetric;

import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.HashUtil;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

/**
 * 类EDKeyTest.java的实现描述：EDKey测试类
 *
 * @author xuhao create on 2020/9/21 15:08
 */

public class EDKeyTest {

    @Test
    public void test_fromPrivate_when_success() {
        SecureKey edKey = SecureKey.getInstance("ED25519",1);
        System.out.println("edKey.privKey: " + Hex.toHexString(edKey.getPrivKeyBytes()));
        SecureKey edKey1 = SecureKey.fromPrivate(edKey.getPrivKeyBytes());
        System.out.println("edKey1.privKey: " + Hex.toHexString(edKey1.getPrivKeyBytes()));
        assert edKey.equals(edKey1);
    }


    /**
     * 方法：签名、验签
     * case：新建密钥对、签名、验签 成功
     */
    @Test
    public void test_sign_and_verify_when_success() {
        SecureKey edKey = SecureKey.getInstance("ED25519",1);
        String msg = "Hello,world";
        byte[] digest = HashUtil.sha3(msg.getBytes());
        //签名
        byte[] sig = edKey.sign(digest);
        //验签
        assert edKey.verify(digest, sig);
    }
}
