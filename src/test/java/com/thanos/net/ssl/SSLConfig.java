package com.thanos.net.ssl;

/**
 * SSLConfig.java description：
 *
 * @Author laiyiyu create on 2020-07-16 14:04:52
 */
public class SSLConfig {

    // 是否开启ssl
    private boolean isSSL = false;
    // 是否开启双向验证
    private boolean needClientAuth = false;
    // 密匙库地址
    private String pkPath;
    // 签名证书地址
    private String caPath;
    // 证书密码
    private String pwd;

    public SSLConfig isSSL(boolean isSSL) {
        this.isSSL = isSSL;
        return this;
    }

    public SSLConfig needClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
        return this;
    }

    public SSLConfig pkPath(String pkPath) {
        this.pkPath = pkPath;
        return this;
    }

    public SSLConfig caPath(String caPath) {
        this.caPath = caPath;
        return this;
    }

    public SSLConfig pwd(String pwd) {
        this.pwd = pwd;
        return this;
    }

    public boolean isSSL() {

        return isSSL;
    }

    public boolean isNeedClientAuth() {

        return needClientAuth;
    }

    public String getPkPath() {

        return pkPath;
    }

    public String getCaPath() {

        return caPath;
    }

    public String getPwd() {
        return pwd;
    }
}
