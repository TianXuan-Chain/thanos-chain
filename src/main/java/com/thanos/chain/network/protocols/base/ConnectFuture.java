package com.thanos.chain.network.protocols.base;

import com.thanos.chain.network.peer.PeerChannel;

import java.nio.channels.Channel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 类ConnectFuture.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-12 19:14:27
 */
public class ConnectFuture {

    private final CountDownLatch syncCondition = new CountDownLatch(1);

    private final byte[] nodeId;

    private volatile PeerChannel channel;

    private volatile Throwable cause;

    public ConnectFuture(byte[] nodeId) {
        this.nodeId = nodeId;
    }

    public PeerChannel waitChannel(final long timeoutMillis) throws InterruptedException {
        this.syncCondition.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this.channel;
    }

    public void putConnect(final PeerChannel channel) {
        this.channel = channel;
        this.syncCondition.countDown();
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }
}
