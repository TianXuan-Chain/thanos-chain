package com.thanos.crypto.asymmetric;

import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.HashUtil;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

/**
 * 类.java的实现描述：SMKey测试类
 *
 * @author xuhao create on 2020/9/21 15:08
 */

public class SMKeyTest {

    @Test
    public void test_fromPrivate_when_success() {
        SecureKey smKey = SecureKey.getInstance("SM",1);
        System.out.println("smKey.privKey: " + Hex.toHexString(smKey.getPrivKeyBytes()));
        SecureKey smKey1 = SecureKey.fromPrivate(smKey.getPrivKeyBytes());
        System.out.println("smKey1.privKey: " + Hex.toHexString(smKey1.getPrivKeyBytes()));
        assert smKey.equals(smKey1);
    }

    @Test
    public void test__when_verify_success() {
        SecureKey smKey = SecureKey.getInstance("SM",1);
        String msg = "Hello,world";
        byte[] digest = HashUtil.sha3(msg.getBytes());
        //签名
        byte[] sig = smKey.sign(digest);
        //验签-静态方法
        assert smKey.verify(digest, sig);
    }

    /**
     * 方法：签名、验签
     * case：新建密钥对、签名、验签 成功
     */
    @Test
    public void test_sign_and_verify_when_success() {
        SecureKey smKey = SecureKey.getInstance("SM",1);
        String msg = "Hello,world";
        byte[] digest = HashUtil.sha3(msg.getBytes());
        //签名
        byte[] sig = smKey.sign(digest);
        //验签
        assert smKey.verify(digest, sig);
    }

}
