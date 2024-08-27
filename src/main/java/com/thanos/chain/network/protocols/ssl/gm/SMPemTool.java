package com.thanos.chain.network.protocols.ssl.gm;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

/**
 * SMPemTool.java description：
 *
 * @Author laiyiyu create on 2020-10-16 15:17:36
 */
public class SMPemTool {

    static {
        Security.setProperty("crypto.policy", "unlimited");
        Security.addProvider(new BouncyCastleProvider());
    }

    public static X509Certificate[] toX509Certificates(File pem)
            throws CertificateException, IOException {
        try (InputStream in = new FileInputStream(pem)) {
            return toX509Certificates(in);
        }
    }

    public static X509Certificate[] toX509Certificates(InputStream in) throws CertificateException {
        ByteBuf[] byteBufs = PemReader.readCertificates(in);

        List<X509Certificate> x509Certificates = new ArrayList<>();
        CertificateFactory certificateFactory =
                CertificateFactory.getInstance("X.509", new BouncyCastleProvider());
        for (ByteBuf byteBuf : byteBufs) {
            x509Certificates.add(
                    (X509Certificate)
                            certificateFactory.generateCertificate(new ByteBufInputStream(byteBuf)));
        }

        return x509Certificates.toArray(new X509Certificate[0]);
    }

    public static PrivateKey toPrivateKey(File pem)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException,
            IOException {

        try (InputStream in = new FileInputStream(pem)) {
            return toPrivateKey(in);
        }
    }

    public static PrivateKey toPrivateKey(InputStream in)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException,
            IOException {

        org.bouncycastle.util.io.pem.PemReader pemReader =
                new org.bouncycastle.util.io.pem.PemReader(new InputStreamReader(in));
        PemObject pem = pemReader.readPemObject();
        if (pem == null) {
            throw new IOException("The file does not represent a pem account.");
        }

        pemReader.close();

        PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(pem.getContent());
        KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);

        return keyFactory.generatePrivate(encodedKeySpec);
    }
}
