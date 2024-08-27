package com.thanos.chain.network.protocols.ssl.gm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * SMTrustManager.java description：
 *
 * @Author laiyiyu create on 2020-10-16 15:15:46
 */
public class SMTrustManager implements X509TrustManager {

    private static final Logger logger = LoggerFactory.getLogger(SMTrustManager.class);

    private X509Certificate[] trustCerts;

    public SMTrustManager(X509Certificate[] trustCerts) {
        this.trustCerts = trustCerts;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String auth)
            throws CertificateException {
        // Here: Add additional certificate validation logic
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String auth)
            throws CertificateException {
        // Here: Add additional certificate validation logic
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
