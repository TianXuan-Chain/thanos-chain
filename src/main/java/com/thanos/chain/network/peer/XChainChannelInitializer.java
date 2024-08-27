package com.thanos.chain.network.peer;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类XChainChannelInitializer.java的实现描述：
 * 该Initializer 再initChannel完成后会被移除
 *
 * @Author laiyiyu create on 2020-01-14 10:30:34
 */
public class XChainChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger("network");

    private PeerManager peerManager;

    private String remoteId;

    private short remoteShardingNum;

    public XChainChannelInitializer(PeerManager peerManager, String remoteId, short remoteShardingNum) {
        this.peerManager = peerManager;
        this.remoteId = remoteId;
        this.remoteShardingNum = remoteShardingNum;
    }


    /**
     * 这里需要注意的是，当XChainChannelInitializer 作为ServerBootstrap 的childHandler时，
     * 每当server 端每接收到一个client 的tcp 连接，并且连接成功后，就会调用一次initChannel();
     * <p>
     * 如果作为 handler,不管是ServerBootstrap 还是Bootstrap，initChannel都
     * 会再建立连接前，initAndRegister 流程中被 调用。
     *
     * @param ch
     * @throws Exception
     */
    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        PeerChannel channel = new PeerChannel();
        channel.setInetSocketAddress(ch.remoteAddress());
        channel.init(ch.pipeline(), remoteId, remoteShardingNum, peerManager);
        //logger.debug("initChannel success!");
        ch.closeFuture().addListener((ChannelFutureListener) future -> peerManager.notifyDisconnect(channel));
    }
}
