package com.thanos.chain.gateway;

import com.google.protobuf.ByteString;
import com.thanos.api.proto.sync.BlockBytesObject;
import com.thanos.api.proto.sync.SyncServiceGrpc;
import com.thanos.common.utils.ThanosThreadFactory;
import com.thanos.chain.ledger.model.Block;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.Channel;
import io.grpc.netty.shaded.io.netty.channel.ChannelFactory;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.buffer.PooledByteBufAllocator;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * SyncClient.java description：
 *
 * @Author laiyiyu create on 2020-10-15 12:30:32
 */
public class SyncClient {

    private ManagedChannel managedChannel;

    private SyncServiceGrpc.SyncServiceBlockingStub syncServiceBlockingStub;

    private final static int PROCESS_TIME_OUT = 1000 * 60;

    public SyncClient(String ip, int port) {
        managedChannel = NettyChannelBuilder
                .forAddress(ip, port)
                .channelFactory(() -> new NioSocketChannel())
                .eventLoopGroup(new NioEventLoopGroup(1, new ThanosThreadFactory("sync_client_netty_worker")))
                .withOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                //.withOption(ChannelOption.SO_TIMEOUT, 60 * 1000 *10)
                .usePlaintext().build();

        syncServiceBlockingStub = SyncServiceGrpc
                .newBlockingStub(managedChannel)
                .withExecutor(new ThreadPoolExecutor(2, 2, 1, TimeUnit.SECONDS, new ArrayBlockingQueue(10), new ThanosThreadFactory("sync_service_block_netty_process")));
    }

    public void syncBlock(Block block){
        if(block==null){
            return;
        }
        byte[] blockBytes = block.getEncoded();

        BlockBytesObject blockBytesObject = BlockBytesObject.newBuilder()
                .setBlockBaseInfo(ByteString.copyFrom(blockBytes))
                .build();
        syncServiceBlockingStub.withDeadlineAfter(PROCESS_TIME_OUT, TimeUnit.MILLISECONDS).syncBlock(blockBytesObject);
    }
}
