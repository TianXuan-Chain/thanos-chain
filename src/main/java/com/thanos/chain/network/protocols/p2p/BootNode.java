package com.thanos.chain.network.protocols.p2p;

/**
 * BootNode.java description：
 *
 * @Author laiyiyu create on 2020-07-23 20:02:52
 */
public class BootNode {

    String host;

    int port;

    public BootNode(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
