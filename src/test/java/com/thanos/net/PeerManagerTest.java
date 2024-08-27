package com.thanos.net;

import com.thanos.chain.config.SystemConfig;
import com.thanos.common.crypto.key.asymmetric.ec.ECKey;
import com.thanos.chain.network.peer.PeerManager;
import org.junit.Test;

import static com.thanos.common.utils.HashUtil.sha3;

/**
 * PeerManagerTest.java description：
 *
 * @Author laiyiyu create on 2020-08-02 12:17:28
 */
public class PeerManagerTest {

    @Test
    public void server1() {
        try {

            String address = "127.0.0.1";
            int port = 30303;
            SystemConfig config = new SystemConfig("thanos-chain.conf");
            ECKey generatedNodeKey = ECKey.fromPrivate(sha3((address + ":" + port).getBytes()),(short)1);
            config.setMyKey(generatedNodeKey);
            new PeerManager(config);

            synchronized (config) {
                config.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void server2() {
        try {

            String address = "127.0.0.1";
            int port = 30304;
            SystemConfig config = new SystemConfig("test-x-chain.conf");
            ECKey generatedNodeKey = ECKey.fromPrivate(sha3((address + ":" + port).getBytes()), (short) 1);
            config.setMyKey(generatedNodeKey);
            new PeerManager(config);
            synchronized (config) {
                config.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void server3() {
        try {

            String address = "127.0.0.1";
            int port = 30305;
            SystemConfig config = new SystemConfig("test2-x-chain.conf");
            ECKey generatedNodeKey = ECKey.fromPrivate(sha3((address + ":" + port).getBytes()),(short)1);
            config.setMyKey(generatedNodeKey);
            new PeerManager(config);
            synchronized (config) {
                config.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
