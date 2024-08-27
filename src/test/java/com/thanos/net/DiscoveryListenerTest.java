package com.thanos.net;

import com.thanos.chain.network.discovery.DiscoveryListener;
import org.junit.Test;

/**
 * DiscoveryListenerTest.java description：
 *
 * @Author laiyiyu create on 2020-07-28 10:29:16
 */
public class DiscoveryListenerTest {

    @Test
    public void server1() {
        try {
            new DiscoveryListener("127.0.0.1", 30303, "thanos-chain.conf").start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void server2() {
        try {
            new DiscoveryListener("127.0.0.1", 30304, "test-x-chain.conf").start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
