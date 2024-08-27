//package com.thanos.chain.network.protocols.ssl.gm;
//
//import io.netty.buffer.ByteBufAllocator;
//import io.netty.handler.ssl.*;
//import io.netty.internal.tcnative.SSL;
//import io.netty.util.internal.StringUtil;
//import java.security.PrivateKey;
//import java.security.cert.X509Certificate;
//import javax.net.ssl.SSLException;
//import javax.net.ssl.X509ExtendedTrustManager;
//import javax.net.ssl.X509TrustManager;
//
///**
// * SMSslClientContext.java description：
// *
// * @Author laiyiyu create on 2020-10-16 15:14:01
// */
//public class SMSslClientContext extends OpenSslContext {
//
//    private final OpenSslSessionContext sessionContext;
//
//    public SMSslClientContext(
//            X509Certificate[] trustCerts,
//            X509Certificate[] encryptNodeCerts,
//            PrivateKey encryptNodeKey,
//            X509Certificate[] nodeCerts,
//            PrivateKey nodeKey)
//            throws SSLException {
//        super(
//                null,
//                IdentityCipherSuiteFilter.INSTANCE,
//                (ApplicationProtocolConfig) null,
//                0,
//                0,
//                SSL.SSL_MODE_CLIENT,
//                null,
//                ClientAuth.REQUIRE,
//                null,
//                false,
//                false);
//        boolean success = false;
//        try {
//            sessionContext =
//                    newSessionContext(
//                            this,
//                            ctx,
//                            engineMap,
//                            trustCerts,
//                            encryptNodeCerts,
//                            encryptNodeKey,
//                            nodeCerts,
//                            nodeKey);
//            success = true;
//        } finally {
//            if (!success) {
//                release();
//            }
//        }
//    }
//
//    @Override
//    public OpenSslSessionContext sessionContext() {
//        return sessionContext;
//    }
//
//    private static final class TrustManagerVerifyCallback extends AbstractCertificateVerifier {
//        private final X509TrustManager manager;
//
//        TrustManagerVerifyCallback(OpenSslEngineMap engineMap, X509TrustManager manager) {
//            super(engineMap);
//            this.manager = manager;
//        }
//
//        @Override
//        void verify(ReferenceCountedOpenSslEngine engine, X509Certificate[] peerCerts, String auth)
//                throws Exception {
//            manager.checkServerTrusted(peerCerts, auth);
//        }
//    }
//
//    private static final class ExtendedTrustManagerVerifyCallback
//            extends AbstractCertificateVerifier {
//        private final X509ExtendedTrustManager manager;
//
//        ExtendedTrustManagerVerifyCallback(
//                OpenSslEngineMap engineMap, X509ExtendedTrustManager manager) {
//            super(engineMap);
//            this.manager = manager;
//        }
//
//        @Override
//        void verify(ReferenceCountedOpenSslEngine engine, X509Certificate[] peerCerts, String auth)
//                throws Exception {
//            manager.checkServerTrusted(peerCerts, auth, engine);
//        }
//    }
//
//    static void setKeyMaterial(
//            long ctx,
//            X509Certificate[] enNodeCert,
//            PrivateKey enNodeKey,
//            X509Certificate[] nodeCert,
//            PrivateKey nodeKey)
//            throws SSLException {
//        long enNodeKeyBio = 0;
//        long nodeKeyBio = 0;
//        long enNodeCertBio = 0;
//        long nodeCertBio = 0;
//        PemEncoded encoded = null;
//        try {
//            ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
//            // Only encode one time
//            encoded = PemX509Certificate.toPEM(allocator, true, enNodeCert);
//            enNodeCertBio = toBIO(allocator, encoded.retain());
//            encoded.release();
//
//            encoded = PemX509Certificate.toPEM(allocator, true, nodeCert);
//            nodeCertBio = toBIO(allocator, encoded.retain());
//            encoded.release();
//
//            encoded = PemPrivateKey.toPEM(allocator, true, enNodeKey);
//            enNodeKeyBio = toBIO(allocator, encoded.retain());
//            encoded.release();
//
//            encoded = PemPrivateKey.toPEM(allocator, true, nodeKey);
//            nodeKeyBio = toBIO(allocator, encoded.retain());
//            encoded.release();
//
//            io.netty.internal.tcnative.SSLContext.setCipherSuite(ctx, "ALL", false);
//            io.netty.internal.tcnative.SSLContext.setCertificateExtBio(
//                    ctx, enNodeCertBio, enNodeKeyBio, nodeCertBio, nodeKeyBio, StringUtil.EMPTY_STRING);
//        } catch (SSLException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new SSLException("failed to set certificate and key", e);
//        } finally {
//            freeBio(enNodeKeyBio);
//            freeBio(nodeKeyBio);
//            freeBio(enNodeCertBio);
//            freeBio(nodeCertBio);
//            if (encoded != null && encoded.refCnt() > 0) {
//                encoded.release();
//            }
//        }
//    }
//
//    static OpenSslSessionContext newSessionContext(
//            ReferenceCountedOpenSslContext context,
//            long ctx,
//            OpenSslEngineMap engineMap,
//            X509Certificate[] trustCerts,
//            X509Certificate[] encryptNodeCerts,
//            PrivateKey enNodeKey,
//            X509Certificate[] nodeCerts,
//            PrivateKey nodeKey)
//            throws SSLException {
//        try {
//            setKeyMaterial(ctx, encryptNodeCerts, enNodeKey, nodeCerts, nodeKey);
//        } catch (Exception e) {
//            throw new SSLException("failed to set certificate and key", e);
//        }
//
//        io.netty.internal.tcnative.SSLContext.setVerify(
//                ctx, io.netty.internal.tcnative.SSL.SSL_CVERIFY_NONE, VERIFY_DEPTH);
//        try {
//            final X509TrustManager manager = new SMTrustManager(trustCerts);
//
//            // IMPORTANT: The callbacks set for verification must be static to prevent memory leak
//            // as
//            //            otherwise the context can never be collected. This is because the JNI code
//            // holds
//            //            a global reference to the callbacks.
//            //
//            //            See https://github.com/netty/netty/issues/5372
//
//            // Use this to prevent an error when running on java < 7
//            if (useExtendedTrustManager(manager)) {
//                io.netty.internal.tcnative.SSLContext.setCertVerifyCallback(
//                        ctx,
//                        new SMSslClientContext.ExtendedTrustManagerVerifyCallback(engineMap, (X509ExtendedTrustManager) manager));
//            } else {
//                io.netty.internal.tcnative.SSLContext.setCertVerifyCallback(
//                        ctx, new SMSslClientContext.TrustManagerVerifyCallback(engineMap, manager));
//            }
//        } catch (Exception e) {
//            throw new SSLException("unable to setup TrustManager", e);
//        }
//
//        return new ReferenceCountedOpenSslClientContext.OpenSslClientSessionContext(context, null);
//    }
//}
