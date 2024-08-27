package com.thanos.crypto.asymmetric;

import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.HashUtil;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

/**
 * 类SecureKeyTest.java的实现描述：SecureKey测试类
 *
 * @author xuhao create on 2020/9/21 20:00
 */

public class SecureKeyTest {
    @Test
    public void test_fromPrivate_when_success() {
        SecureKey secureKey = SecureKey.getInstance("ECDSA", 1);
        System.out.println("secureKey.privKey: " + Hex.toHexString(secureKey.getPrivKeyBytes()));
        SecureKey secureKey1 = SecureKey.fromPrivate(secureKey.getPrivKeyBytes());
        System.out.println("secureKey1.privKey: " + Hex.toHexString(secureKey1.getPrivKeyBytes()));
        assert secureKey.equals(secureKey1);
    }


    /**
     * 方法：签名、验签
     * case：新建密钥对、签名、验签 成功
     */
    @Test
    public void test_sign_and_verify_when_success() {
        SecureKey secureKey = SecureKey.getInstance("ECDSA", 1);
        String msg = "Hello,world";
        byte[] digest = HashUtil.sha3(msg.getBytes());
        //签名
        byte[] sig = secureKey.sign(digest);
        //验签
        assert secureKey.verify(digest, sig);
    }

}
