package com.thanos.chain.network.protocols.p2p;

import com.thanos.chain.network.peer.PeerManager;
import com.thanos.chain.network.protocols.MessageDuplexDispatcher;
import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.network.protocols.base.RemotingMessageType;
import com.thanos.common.utils.ThanosThreadFactory;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 类P2pProcessor.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-09 18:44:04
 */
public class P2pProcessor {
    private static final int P2P_PROCESSOR = 8;

    volatile boolean stop = false;

    private PeerManager peerManager;

    private Executor p2pMsgExecutor = new ThreadPoolExecutor(P2P_PROCESSOR, P2P_PROCESSOR, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000), new ThanosThreadFactory("p2p_msg_process"));

    public P2pProcessor(PeerManager peerManager) {
        this.peerManager = peerManager;
    }

    public void start() {
        new Thread(() -> process(), "p2pProcessor_thread").start();
    }

    private void process() {
        //System.out.println("p2p doCheck start success!");
        Runnable doProcessTask = () -> doProcess();
        while (true) {
            try {
                if (stop) return;
                p2pMsgExecutor.execute(doProcessTask);
            } catch (Throwable e) {

            }
        }
    }

    private void doProcess() {
        Message p2pMsg = MessageDuplexDispatcher.getP2pMsg();
        //System.out.println("doProcess msg:" + p2pMsg.toString());
        switch (P2pMessageCodes.fromByte(p2pMsg.getCode())) {
            case HELLO:
                //todo
                break;
            case DISCONNECT:
                //todo
                break;
            case PING:
                PongMsg pongMsg = new PongMsg();
                pongMsg.setRemoteType(RemotingMessageType.RESPONSE_MESSAGE.getType());
                pongMsg.setRpcId(p2pMsg.getRpcId());
                peerManager.directSend(pongMsg, Arrays.asList(p2pMsg.getNodeId()));
                break;


            case PONG:
                // donothing
                break;

            case PEERS:
                //todo
                break;
            default:
        }
    }


}
