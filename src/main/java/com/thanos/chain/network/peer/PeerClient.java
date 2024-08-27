package com.thanos.chain.network.peer;

import com.thanos.chain.config.SystemConfig;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ThanosThreadFactory;
import com.thanos.chain.network.protocols.p2p.StaticMessages;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

/**
 * 类PeerClient.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-26 11:09:08
 */
public class PeerClient {

    private static final Logger logger = LoggerFactory.getLogger("network");

    private EventLoopGroup workerGroup;

    PeerManager peerManager;

    public PeerClient(PeerManager peerManager) {
        this.peerManager = peerManager;
        //this.config = systemProperties;
        // 我们的3.0的所有网络协议 统一由一个信道传输处理
        // 这里，client 端所发起channel workerGroup 实例管理
        // 不使用 一对一 的模式
        // 因此，这里线程数使用cpu，与 server 端保持一致，基于p2p 的概念,避免一个client的 大量数据传输而导致的阻塞,
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), new ThanosThreadFactory("peer_client_worker"));
    }



    /**
     *  同步连接
     *  由于XChainChannelInitializer 的上下文设计，XChainChannelInitializer 必须每次都使用新的对象
     *  所以，Bootstrap 再每次建立新的连接时，都必须使用新的对象。
     *  Connects to the node and returns only upon connection close
     */
    public void connect(String host, int port, byte[] nodeId, short shardingNum) {
        try {

            XChainChannelInitializer xChainChannelInitializer = new XChainChannelInitializer(this.peerManager, Hex.toHexString(nodeId), shardingNum);
            ChannelFuture f = connectAsync(host, port, xChainChannelInitializer);
            // 同步等待，直到tcp连接建立成功，底层使用的object wait();
            // 异步notifyAll();
            //f.sync();

            f.await(3000); // 时长等待
            logger.info("PeerClient: Can connect to " + host + ":" + port + " success!");
            System.out.println("connect success!");

        } catch (Exception e) {

            if (e instanceof IOException) {
                logger.info("PeerClient: Can't connect to " + host + ":" + port + " (" + e.getMessage() + ")");
                logger.debug("PeerClient.connect(" + host + ":" + port + ") exception:", e);
            } else {
                logger.error("Exception:", e);
            }

        }
    }

    public ChannelFuture connectAsync(String host, int port, XChainChannelInitializer xChainChannelInitializer) {

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
 //               .option(ChannelOption.TCP_NODELAY, true) // 这里无需最求高响应
//                .option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60 * 1000)
//                .option(ChannelOption.SO_SNDBUF, 1024 * 1024 * 128)
//                .option(ChannelOption.SO_RCVBUF, 1024 * 1024 * 128)
                .option(ChannelOption.SO_BACKLOG, 1024);

        //b.remoteAddress(host, port);

        if (this.peerManager.systemConfig.nettyPoolByteBuf()) {
            boolean defaultPreferDirect = PooledByteBufAllocator.defaultPreferDirect();
            PooledByteBufAllocator pooledByteBufAllocator = new PooledByteBufAllocator(defaultPreferDirect, PooledByteBufAllocator.defaultNumHeapArena(), defaultPreferDirect ? PooledByteBufAllocator.defaultNumDirectArena() : 0, PooledByteBufAllocator.defaultPageSize(), 8, PooledByteBufAllocator.defaultTinyCacheSize(), PooledByteBufAllocator.defaultSmallCacheSize(), PooledByteBufAllocator.defaultNormalCacheSize(), PooledByteBufAllocator.defaultUseCacheForAllThreads());
            bootstrap.option(ChannelOption.ALLOCATOR, pooledByteBufAllocator);
        }

        bootstrap.handler(xChainChannelInitializer);
        // Start the client.
        return bootstrap.connect(host, port);
    }

    public void close() {
        logger.info("Shutdown peerClient");
        workerGroup.shutdownGracefully();
        workerGroup.terminationFuture().syncUninterruptibly();
    }

    public static void main(String[] args) {
        test1();
    }


    public static void test2() {

    }

    public static void test1() {
        SystemConfig systemConfig = new SystemConfig("test-x-chain.conf");
        systemConfig.setMyKey(SecureKey.getInstance("ECDSA",1));
        PeerManager peerManager = new PeerManager(systemConfig);
        byte[] nodeId1 = systemConfig.getNodeId();

        PeerClient peerClient = new PeerClient(peerManager);
        System.out.println("first connect!!!");

        System.out.println("client node id:" + Hex.toHexString(nodeId1));
        Runnable connectTask = () -> peerClient.connect("localhost", 8888, Hex.decode("142abb8c8604ae81aebd56927f372efb225052833316de6e19eb121ae12de6c42c333647b3f26642dd636b8cfd7510d1444c93caf91a535b60917ecb949ddc24"), (short) 0);

        new Thread(connectTask).start();
        //new Thread(connectTask).start();
//        peerClient.connect("localhost", 8888, new XChainChannelInitializer(peerManager, Hex.toHexString(nodeId1)));
//        await(20000);
//        peerClient.connect("localhost", 8888, new XChainChannelInitializer(peerManager, Hex.toHexString(nodeId1)));
        await(2000);

        StaticMessages staticMessages = new StaticMessages(systemConfig);
        System.out.println();
//        new Thread(() -> {
//            while (true) {
//                for (int i = 0; i < 20 ; i++) {
//                    new Thread(() -> peerManager.testBroadcast(staticMessages.createHelloMessage(nodeId1, 23))).start();
//                }
//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//
//        }).start();

        new Thread(() -> {
            for (int i = 0; i < 8 ; i++) {
                new Thread(() -> {
                    while (true) {
                        for (int i1 = 0; i1 < 2; i1++) {
                            //peerManager.testBroadcast(staticMessages.createHelloMessage(nodeId1, (short)0, 23));
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }).start();
            }

        }).start();
        await(20000000);
//        peerClient.connect("localhost", 8888);

//        peerClient.close();
    }

    public static void await(long time) {
        synchronized (PeerClient.class) {// 类实例锁
            try {
                PeerClient.class.wait(time);
                System.out.println("wait after");
            } catch (InterruptedException e) {

            }
        }
    }
}
