package com.thanos.chain.network.peer;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.network.protocols.MessageDuplexHandler;
import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.network.protocols.base.ReasonCode;
import com.thanos.chain.network.protocols.base.ResponseMsgFuture;
import com.thanos.chain.network.protocols.p2p.HelloMsg;
import com.thanos.chain.network.protocols.rlpx.*;
import com.thanos.chain.network.protocols.ssl.*;
import io.netty.channel.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.*;

/**
 * 类Channel.java的实现描述：
 *
 * @Author laiyiyu create on 2020-01-15 11:06:23
 */
public class PeerChannel {

    static final Logger logger = LoggerFactory.getLogger("network");

    // 这里的线程无需太多，因为writeAndFlush 是异步的
    //private static final ScheduledThreadPoolExecutor directSendTimer = new ScheduledThreadPoolExecutor(4, new ThanosThreadFactory("direct_send_msg"));

    //private static final MessageCodec messageCodec = new MessageCodec();
    private static final MessageCodec2 messageCodec2 = new MessageCodec2();

    //private static MessageDecoder messageDecoder = new MessageDecoder();

    private static final MessageDuplexHandler messageDuplexHandler = new MessageDuplexHandler();

//    static class SendMsgRunnable implements Runnable {
//
//        PeerChannel peerChannel;
//
//        public SendMsgRunnable(PeerChannel peerChannel) {
//            this.peerChannel = peerChannel;
//        }
//
//        @Override
//        public void run() {
//            try {
//                peerChannel.doDirectSend();
//            } catch (Throwable t) {
//                logger.error("Unhandled exception", ExceptionUtils.getStackTrace(t));
//            }
//        }
//    }

    Peer remotePeer;

    HandshakeHandler handshakeHandler;

    ExchangeNodeInfoHandler exchangeNodeInfoHandler;

    PeerManager peerManager;

    volatile long handShakeSuccessTimestamp;

    protected ConcurrentHashMap<Long /* rpcId */, ResponseMsgFuture> responseTable =
            new ConcurrentHashMap<Long, ResponseMsgFuture>(256);

    private ScheduledFuture<?> directSendTimerTask;

    //private final static int MAX_SENDER = Runtime.getRuntime().availableProcessors();

    private volatile Channel channel;

    private InetSocketAddress inetSocketAddress;

    private volatile boolean close;

    private volatile boolean repeatConnect;

//    private Queue<Message> messageQueue = new ArrayBlockingQueue<>(10000);

//    private SendMsgRunnable doSendTask;

    public PeerChannel() {

//        doSendTask = () -> {
//            try {
//                doDirectSend();
//            } catch (Throwable t) {
//                //t.printStackTrace();
//                logger.error("Unhandled exception", ExceptionUtils.getStackTrace(t));
//            }
//        };

//        doSendTask = new SendMsgRunnable(this);

        //todo : find magic period num for avoid netty send fail,too small period parameter for
        //todo : schedule mean fast send msg to netty channel
//        directSendTimer.setRemoveOnCancelPolicy(true);
//        directSendTimerTask = directSendTimer.scheduleAtFixedRate(doSendTask, 10, 5, TimeUnit.MILLISECONDS);
    }

//    private void doDirectSend() throws InterruptedException {
//        if (!isActive()) return;
//        Message message = this.messageQueue.poll();
//        if (message == null) return;
//        message.setNodeId(peerManager.getSystemConfig().getCaHash());
//        this.channel.writeAndFlush(message).addListener((ChannelFutureListener) f -> {
//            message.clear();
//            if (!f.isSuccess()) {
//                logger.warn("send a message to channel <" + channel.remoteAddress() + "> failed.");
//            }
//        });
//    }

    /**
     * Set node and register it in NodeManager if it is not registered yet.
     */
    public void initWithRemotePeer(byte[] nodeId, int remotePort, short remoteShardingNum) {
        this.remotePeer = new Peer(inetSocketAddress.getHostString(), remotePort, nodeId, remoteShardingNum);
    }

    public void initWithRemotePeer(byte[] nodeId, short remoteShardingNum) {
        initWithRemotePeer(nodeId, inetSocketAddress.getPort(), remoteShardingNum);
    }

    public void init(ChannelPipeline pipeline, String remoteId, short remoteShardingNum, PeerManager peerManager) {
        this.peerManager = peerManager;
//        this.handshakeHandler = new HandshakeHandler(peerManager.getSystemConfig());
//        handshakeHandler.setRemote(remoteId, remoteShardingNum);
//        handshakeHandler.setPeerChannel(this);
//        pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(peerManager.getSystemConfig().peerChannelReadTimeout(), TimeUnit.SECONDS));
//        pipeline.addLast("handshakeHandler", handshakeHandler);

        this.exchangeNodeInfoHandler = new ExchangeNodeInfoHandler(peerManager.getSystemConfig());
        exchangeNodeInfoHandler.setRemote(remoteId, remoteShardingNum);
        exchangeNodeInfoHandler.setPeerChannel(this);
        pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(peerManager.getSystemConfig().peerChannelReadTimeout(), TimeUnit.SECONDS));
        pipeline.addLast("exchangeNodeInfoHandler", exchangeNodeInfoHandler);
    }

    public Message rpcSend(Message message, long timeoutMillis) {
        try {
            if (!isActive()) throw new RuntimeException("channel is not active!");
            message.setRpcId();
            message.setNodeId(this.peerManager.getSystemConfig().getNodeId());
            final ResponseMsgFuture responseMsgFuture = new ResponseMsgFuture(message.getRpcId());
            this.responseTable.put(message.getRpcId(), responseMsgFuture);
            final SocketAddress addr = channel.remoteAddress();
            channel.writeAndFlush(message).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    return;
                }

                responseTable.remove(message.getRpcId());
                responseMsgFuture.setCause(f.cause());
                responseMsgFuture.putResponse(null);
                logger.warn("send a request command to channel <" + addr + "> failed.");
            });

            Message responseMsg = responseMsgFuture.waitResponse(timeoutMillis);
            if (null == responseMsg) {
                throw new RuntimeException("wait message :" + message.toString() + "fail!");
            }
            return responseMsg;

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            //这里是为了处理等待超时，我们任然需要移除该次请求。
            this.responseTable.remove(message.getRpcId());
        }
    }

    public void asyncDirectSend(Message message) {
        //发送之前设置nodeId
//        message.setNodeId(peerManager.getSystemConfig().getCaHash());
//        boolean success;

        this.channel.writeAndFlush(message).addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                logger.warn("send a message[{}] to channel <{}> failed , cause[{}]. try once again!", message, channel.remoteAddress(), ExceptionUtils.getStackTrace(f.cause()));
                this.channel.writeAndFlush(message);
            }
        });

//        success = messageQueue.add(message);
//
//        if (success) return;
//        try {
//            Thread.sleep(20);
//        } catch (InterruptedException e) {
//
//        }
//        for (int i = 0; i < 10; i++) {
//            success = messageQueue.add(message);
//            if (success) return;
//
//            try {
//                Thread.sleep(20);
//            } catch (InterruptedException e) {
//
//            }
//        }
        //ignore the message
    }

    public void syncDirectSend(Message msg) throws InterruptedException {
        this.channel.writeAndFlush(msg).addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                logger.warn("send a message to channel <{}> failed.", channel);
            }
        }).sync();
    }

    public ResponseMsgFuture getResponseFuture(long rpcId) {
        return this.responseTable.get(rpcId);
    }

    public void putResponse(Message responseMsg) {
        final ResponseMsgFuture responseMsgFuture = responseTable.remove(responseMsg.getRpcId());
        if (responseMsgFuture != null) {
            responseMsgFuture.putResponse(responseMsg);
        }
    }

    public void close() {
        //help gc
        //this.channel = null;

        if (close) return;
        close = true;

        //if (!repeatConnect) this.remotePeer.unActivePeer();

//        if (directSendTimerTask != null) {
//            directSendTimerTask.cancel(true);
//        }
//        this.doSendTask.peerChannel = null;
//        this.doSendTask = null;
//        messageQueue.clear();
//        messageQueue = null;
        remotePeer = null;
        peerManager = null;
        responseTable.clear();
        responseTable = null;
        directSendTimerTask = null;
//        for (String name: this.channel.pipeline().names()) {
//            logger.debug("remove handle :{}", name);
//            this.channel.pipeline().remove(name);
//        }
        channel = null;
        inetSocketAddress = null;
        exchangeNodeInfoHandler.clear();
        exchangeNodeInfoHandler = null;
        logger.debug("peer channel clear success!");

    }

    public void doSSL(ChannelHandlerContext ctx, boolean isServer) {
        SSLEngine engine;
        if (isServer) {
            // server
            try {
                engine = peerManager.selfSSLContext.createSSLEngine();
                engine.setUseClientMode(false);
                engine.setNeedClientAuth(true);

                ctx.pipeline().addLast("sslHandler", new SslHandler(engine));
                ctx.pipeline().addLast("sslCheckHandler", new SslCheckHandler(this, true));

            } catch (Exception e) {
                logger.warn("server peer init ssl error!", e);
            }
        } else {
            // client
            try {
                engine = peerManager.selfSSLContext.createSSLEngine(inetSocketAddress.getHostString(), inetSocketAddress.getPort());
                engine.setUseClientMode(true);

                ctx.pipeline().addLast("sslHandler", new SslHandler(engine));
                ctx.pipeline().addLast("sslCheckHandler", new SslCheckHandler(this, false));

            } catch (Exception e) {
                logger.warn("client peer init ssl error!", e);
            }
        }
        ctx.pipeline().fireChannelActive();
    }

    public void publicRLPxHandshakeFinished(ChannelHandlerContext ctx, FrameCodec frameCodec, HelloMsg inboundHelloMessage) {
        handShakeSuccessTimestamp = System.currentTimeMillis();
        this.channel = ctx.channel();

        if (!this.peerManager.blackNodes.isEmpty() && blackListNode(ctx)) {
            return;
        }

        if (!peerManager.systemConfig.transferDataEncrypt()) {
            logger.info("transfer data not need encrypt!");
            try {
                //ctx.pipeline().get(SslHandler.class).handlerRemoved(ctx);
                ctx.pipeline().remove("sslHandler");
            } catch (Exception e) {
                //e.printStackTrace();
                logger.warn("handlerRemoved error!", e);
                ctx.close();
                return;
            }
        }

//        ctx.pipeline().addLast("medianFrameCodec2", new FrameCodecHandler2(new FrameCodec2()));
//        ctx.pipeline().addLast("messageCodec2", messageCodec2);

        ctx.pipeline().addLast("messageEncoder", new MessageEncoder());
        ctx.pipeline().addLast("messageDecoder", new MessageDecoder());


        ctx.pipeline().addLast("messageDuplexHandler", messageDuplexHandler);



//        for (String name: ctx.pipeline().toMap().keySet()) {
//            logger.info("handle[{}]", name);
//        }


        peerManager.addActiveChannel(this);
    }

    private boolean blackListNode(ChannelHandlerContext ctx) {

        boolean isBlackListNode = false;
        try {
            SslHandler sslHandler = (SslHandler) ctx.pipeline().get("sslHandler");

            for (X509Certificate certificate: sslHandler.engine().getSession().getPeerCertificateChain()) {
                if (this.peerManager.blackListSet.contains(certificate.getSerialNumber())) {
                    isBlackListNode = true;
                    break;
                }
            }

            if (isBlackListNode) {
                logger.warn("current node[{}] is in black list, forbid connect!", Hex.toHexString(this.remotePeer.getNodeId()));
                ctx.close();
            }
        } catch (SSLPeerUnverifiedException e) {
            ctx.close();
        } catch (Exception e) {
            ctx.close();
        } finally {
            return isBlackListNode;
        }
    }

    public void disconnect(ReasonCode badProtocol) {

    }

    public byte[] getRemoteNodeId() {
        return this.remotePeer.getNodeId();
    }

    public ByteArrayWrapper getRemoteNodeIdWrapper() {
        return this.remotePeer.getNodeIdWrapper();
    }

    public short getRemoteShardingNum() {
        return this.remotePeer.getShardingNum();
    }

    public long getHandShakeSuccessTimestamp() {
        return handShakeSuccessTimestamp;
    }

    public boolean isActive() {
        return this.channel != null && this.channel.isActive();
    }

    public void closeChannel() {
        if (isActive()) {
            channel.close();
        }
    }

    public void setRepeatConnect() {
        this.repeatConnect = true;
    }

    public boolean isRepeatConnect() {
        return repeatConnect;
    }

    @Override
    public String toString() {
        return "PeerChannel{" +
                "remotePeer=" + remotePeer +
                '}';
    }

    public void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
    }

    public static void main(String[] args) {

        System.out.println(Hex.decode("").length);

        System.out.println(new BigInteger("a1c", 16));
        System.out.println(new BigInteger("A1U", 16));
    }
}
