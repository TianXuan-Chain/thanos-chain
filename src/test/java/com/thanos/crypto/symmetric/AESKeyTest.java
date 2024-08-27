package com.thanos.crypto.symmetric;

import com.thanos.common.crypto.key.symmetric.CipherKey;
import org.junit.Test;

import java.util.Arrays;

/**
 * 类AESKeyTest.java的实现描述：
 *
 * @author xuhao create on 2020/11/26 15:38
 */

public class AESKeyTest {

    /**
     * 方法：加密、解密
     * case：新建密钥对、签名、验签 成功
     */
    @Test
    public void test_encrypt_and_decrypt_when_success() {
        CipherKey aesKey = CipherKey.getInstance("AES");
        byte[] data = "Hello,world".getBytes();
        //加密
        byte[] encryptData = aesKey.encrypt(data);
        //解密
        byte[] plainText = aesKey.decrypt(encryptData);
        assert Arrays.equals(data, plainText);
    }
}
