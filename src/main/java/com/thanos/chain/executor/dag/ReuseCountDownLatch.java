package com.thanos.chain.executor.dag;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * ReuseCountDownLatch.java description：
 *
 * @Author laiyiyu create on 2020-02-26 17:19:14
 */
public class ReuseCountDownLatch {

    private static final class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = 4982264981922014374L;

        Sync(int count) {
            setState(count);
        }

        void resetState(int count) {
            setState(count);
        }

        int getCount() {
            return getState();
        }

        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c-1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }

        @Override
        public String toString() {
            return "" + getCount();
        }
    }

    private final Sync sync;

    public ReuseCountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new ReuseCountDownLatch.Sync(count);
    }

    public void reuse(int count) {
        //assert sync.getCount() == 0;
        sync.resetState(count);
    }

    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    public boolean releaseLast() {
        return sync.releaseShared(1);
    }

    @Override
    public String toString() {
        return "ReuseCountDownLatch{" + sync + '}';
    }

    public static void main(String[] args) {
        final ReuseCountDownLatch countDownLatch = new ReuseCountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            final int temp = 0;
            new Thread(() -> {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                boolean releaseSharedOnlyeOnce = countDownLatch.releaseLast();
                System.out.println("fist:" + temp + ", releaseSharedOnlyeOnce:" + releaseSharedOnlyeOnce);
            }).start();
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println();
        countDownLatch.reuse(8);

        for (int i = 0; i < 8; i++) {
            final int temp = 0;
            new Thread(() -> {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                boolean releaseSharedOnlyeOnce = countDownLatch.releaseLast();
                System.out.println("second:" + temp + ", releaseSharedOnlyeOnce:" + releaseSharedOnlyeOnce);
            }).start();
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
