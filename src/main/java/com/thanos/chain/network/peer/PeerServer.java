package com.thanos.chain.network.peer;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.network.NetUtil;
import com.thanos.common.utils.ThanosThreadFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

/**
 * 类PeerServer.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-26 11:06:37
 */
public class PeerServer {

    private static final Logger logger = LoggerFactory.getLogger("network");
    ChannelFuture channelFuture;
    private final SystemConfig config;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup connectGroup;
    private EventLoopGroup rwGroup;
    private final XChainChannelInitializer xChainChannelInitializer;
    private boolean listening;

    public PeerServer(SystemConfig config, XChainChannelInitializer xChainChannelInitializer) {
        this.config = config;
        this.xChainChannelInitializer = xChainChannelInitializer;
    }

    public static void main(String[] args) {


        SystemConfig systemConfig = new SystemConfig();
        PeerManager peerManager = new PeerManager(systemConfig);
        System.out.println("server node id:" + Hex.toHexString(systemConfig.getNodeId()));
    }

    public ChannelFuture start() {
        //logger.info("start peer server Listening!");
        connectGroup = new NioEventLoopGroup(1, new ThanosThreadFactory("peer_netty_boss"));

        int processNum = Runtime.getRuntime().availableProcessors() > 64 ? 64 : Runtime.getRuntime().availableProcessors();
        if (NetUtil.isLinuxPlatform() && config.epollSupport()) {
            // default num:Runtime.getRuntime().availableProcessors() * 2
            rwGroup = new EpollEventLoopGroup(processNum, new ThanosThreadFactory("peer_netty_worker"));
            //rwGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), new ThanosThreadFactory("peer_netty_worker"));

        } else {
            // 使用默认处理数，Runtime.getRuntime().availableProcessors() * 2
            rwGroup = new NioEventLoopGroup(processNum, new ThanosThreadFactory("peer_netty_worker"));
        }


        try {
            serverBootstrap = new ServerBootstrap();

            serverBootstrap.group(connectGroup, rwGroup)
                    .channel(NioServerSocketChannel.class)
                    // option 再init 流程中会设置到channel 中
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    //.option(ChannelOption.TCP_NODELAY, true) // 这里需最求高响应
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60 * 1000)
                    .option(ChannelOption.SO_BACKLOG, 1024)
//                    .option(ChannelOption.SO_SNDBUF, 1024 * 1024 * 128)
//                    .option(ChannelOption.SO_RCVBUF, 1024 * 1024 * 128)
                    .childHandler(xChainChannelInitializer);

            if (this.config.nettyPoolByteBuf()) {
                boolean defaultPreferDirect = PooledByteBufAllocator.defaultPreferDirect();
                logger.info("defaultPreferDirect:{}", defaultPreferDirect);
                PooledByteBufAllocator pooledByteBufAllocator = new PooledByteBufAllocator(defaultPreferDirect, PooledByteBufAllocator.defaultNumHeapArena(), defaultPreferDirect ? PooledByteBufAllocator.defaultNumDirectArena() : 0, PooledByteBufAllocator.defaultPageSize(), 8, PooledByteBufAllocator.defaultTinyCacheSize(), PooledByteBufAllocator.defaultSmallCacheSize(), PooledByteBufAllocator.defaultNormalCacheSize(), PooledByteBufAllocator.defaultUseCacheForAllThreads());
                serverBootstrap.option(ChannelOption.ALLOCATOR, pooledByteBufAllocator);
                serverBootstrap.childOption(ChannelOption.ALLOCATOR, pooledByteBufAllocator);
            }


            ChannelFuture channelFuture = this.serverBootstrap.bind(config.bindIp(), config.listenRpcPort()).sync();

            logger.info("Listening for incoming connections, address:[{}], port: [{}] ", config.bindIp(), config.listenRpcPort());
            listening = true;

            // 让出线程，底层使用wait()，直到调用 close() 方法;
            //channelFuture.channel().closeFuture().sync();
            //logger.info("Connection is closed");
            return channelFuture;

        } catch (Exception e) {
            logger.error("PeerServer bind failed: {}", ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e);
        }
    }

    public void close() {
        if (listening && channelFuture != null && channelFuture.channel().isOpen()) {
            try {
                logger.info("Closing PeerServer...");
                channelFuture.channel().close().sync();
                logger.info("PeerServer closed.");
            } catch (Exception e) {
                logger.warn("Problems closing server channel", e);
            }
        }
    }

    public boolean isListening() {
        return listening;
    }
}
