package com.thanos.chain.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * 类NetUtil.java的实现描述：
 *
 * @Author laiyiyu create on 2020-01-14 16:37:10
 */
public class NetUtil {

    public static final String OS_NAME = System.getProperty("os.name");

    private static final Logger logger = LoggerFactory.getLogger("common");

    private static boolean IS_LINUX_PLATFORM = false;

    static {
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("linux")) {
            IS_LINUX_PLATFORM = true;
        }
    }

    public static boolean isLinuxPlatform() {
        return IS_LINUX_PLATFORM;
    }


    public static String parseChannelRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }

        return "";
    }

    //断开tcp 连接，核心通过pipeline由tail -> head 方向传播调用close方法，并在
    // head 的close 方法中，通过调用原生java nio 再通过本地调用close(fd) 执行正真tcp 连接断开
    public static void closeChannel(Channel channel) {
        final String addrRemote = parseChannelRemoteAddr(channel);
        channel.close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.info("closeChannel: close the connection to remote address[{}] result: {}", addrRemote,
                        future.isSuccess());
            }
        });
    }
}
