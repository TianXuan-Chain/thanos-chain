package com.thanos.chain.network.protocols.base;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 类ResponseMsgFuture.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-08 10:14:33
 */
public class ResponseMsgFuture {

    private final CountDownLatch syncCondition = new CountDownLatch(1);

    private final long rpcId;

    private volatile Message responseMsg;

    private volatile Throwable cause;

    public ResponseMsgFuture(long rpcId) {
        this.rpcId = rpcId;
    }

    public Message waitResponse(final long timeoutMillis) throws InterruptedException {
        this.syncCondition.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this.responseMsg;
    }

    public void putResponse(final Message responseMsg) {
        this.responseMsg = responseMsg;
        this.syncCondition.countDown();
    }

    public long getRpcId() {
        return rpcId;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }
}
