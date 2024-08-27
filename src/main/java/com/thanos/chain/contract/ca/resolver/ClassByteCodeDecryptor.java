package com.thanos.chain.contract.ca.resolver;

import javax.tools.JavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 类 ClassByteCodeDecryptor 的实现描述：
 *
 * @author: wanggeng  2021/6/29 20:54
 **/
public class ClassByteCodeDecryptor {

    /**
     * jni中提供的解密函数
     * 对输入的字节码进行解密，输出解密后的字节码
     * @param text
     * @return
     */
    public native static byte[] decrypt(byte[] text);

    /**
     * 如果解密库加载了，
     * 从javaFileObject中读取所有的字节码进行解密，输出解密后的字节码
     * 否则，只输出原字节码
     * 否则输出原字节码
     *
     * 如果读取中遇到IO异常，则抛出异常
     * @param javaFileObject
     * @return
     */
    public static byte[] decrypt(JavaFileObject javaFileObject) throws IOException {
        byte[] tmpBuffer = new byte[1024];

        int readLength = 0;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (InputStream inputStream = javaFileObject.openInputStream()) {
            while ((readLength=inputStream.read(tmpBuffer)) >= 0) {
                byteArrayOutputStream.write(tmpBuffer, 0, readLength);
            }
        } catch (IOException e) {
            throw e;
        }

        return decrypt(byteArrayOutputStream.toByteArray());
    }
}
