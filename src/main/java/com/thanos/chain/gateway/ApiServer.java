package com.thanos.chain.gateway;

import com.thanos.chain.config.SystemConfig;
import com.thanos.common.utils.ThanosThreadFactory;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.buffer.PooledByteBufAllocator;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ApiServer.java description：
 *
 * @Author laiyiyu create on 2020-10-14 16:20:22
 */
public class ApiServer {

    private static final Logger logger = LoggerFactory.getLogger("gateway");

    private static io.grpc.Server server;

    public static void startApiServer(GatewayFacade gatewayFacade, SystemConfig systemConfig) {
        try {
            final Integer port = gatewayFacade.getSystemConfig().getGatewayLocalListenAddress();
            //System.setProperty("io.grpc.netty.shaded.io.netty.eventLoopThreads", "2");
            server = NettyServerBuilder.forPort(port)
                    .maxInboundMessageSize(1024 * 1024 * 1024)
                    .channelFactory(() -> new NioServerSocketChannel()) // do not need use epoll
                    .bossEventLoopGroup(new NioEventLoopGroup(1, new ThanosThreadFactory("api_server_netty_boss")))
                    .workerEventLoopGroup(new NioEventLoopGroup(8, new ThanosThreadFactory("api_server_netty_worker")))
                    .withChildOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .withChildOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60 * 1000)
                    //.withChildOption(ChannelOption.SO_TIMEOUT, 60 * 1000 * 60 *2) //2 hours
                    .executor(new ThreadPoolExecutor(systemConfig.getHandleReqProcessNum(), systemConfig.getHandleReqProcessNum() * 2, 10, TimeUnit.SECONDS, new ArrayBlockingQueue(10000), new ThanosThreadFactory("api_server_grpc_processor")))
                    .addService(new ApiService(gatewayFacade))
                    .build().start();

            logger.info("Listener Grpc server start port:{}", port);
            //等待客户端的连接

        } catch (Exception e) {
            logger.info("Listener Grpc server start error.", e);
            System.exit(0);
        }

        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            logger.warn("awaitTermination warn!", e);
        }
    }

}
