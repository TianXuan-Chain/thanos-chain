package com.thanos.chain.network.protocols;

import com.thanos.chain.network.protocols.base.Message;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 类MessageDispatcher.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-27 15:02:21
 */
public class MessageDuplexDispatcher {

    private static BlockingQueue<Message> consensusMsgQueue = null;

    private static BlockingQueue<Message> layer2StateSyncMsgQueue = null;

    private static BlockingQueue<Message> globalStateVerifierMsgQueue = null;

    private static BlockingQueue<Message> p2pMsgQueue = null;

    private static BlockingQueue<Message> responseMsgQueue = null;

    static {
        consensusMsgQueue = new ArrayBlockingQueue<Message>(10000);
        layer2StateSyncMsgQueue = new ArrayBlockingQueue<Message>(10000);
        globalStateVerifierMsgQueue = new ArrayBlockingQueue<Message>(10000);
        p2pMsgQueue = new ArrayBlockingQueue<Message>(10000);

        responseMsgQueue = new ArrayBlockingQueue<Message>(10000);
    }

    public static void putResponseMsg(Message responseMsg) {
        try {
            responseMsgQueue.put(responseMsg);
        } catch (InterruptedException e) {
        }
    }

    public static Message getResponseMsg() {
        Message response = null;
        try {
            response = responseMsgQueue.take();
        } catch (InterruptedException e) {
        }
        return response;
    }

    public static void putConsensusMsg(Message consensusMsg) {
        try {
            consensusMsgQueue.put(consensusMsg);
        } catch (InterruptedException e) {

        }
    }

    public static Message getConsensusMsg() {
        Message message = null;
        try {
            return consensusMsgQueue.take();
        } catch (InterruptedException e) {

        }
        return message;
    }

    public static void putLayer2StateSyncMsg(Message layer2StateSyncMsg) {
        try {
            layer2StateSyncMsgQueue.put(layer2StateSyncMsg);
        } catch (InterruptedException e) {

        }
    }

    public static Message getLayer2StateSyncMsg() {
        Message message = null;
        try {
            return layer2StateSyncMsgQueue.take();
        } catch (InterruptedException e) {

        }
        return message;
    }

    public static void putGlobalStateVerifierMsg(Message globalStateVerifierMsg) {
        try {
            globalStateVerifierMsgQueue.put(globalStateVerifierMsg);
        } catch (InterruptedException e) {

        }
    }

    public static Message getGlobalStateVerifierMsg() {
        Message message = null;
        try {
            return globalStateVerifierMsgQueue.take();
        } catch (InterruptedException e) {

        }
        return message;
    }

    public static void putP2pMsg(Message p2pMsg) {
        try {
            p2pMsgQueue.put(p2pMsg);
        } catch (InterruptedException e) {
        }
    }

    public static Message getP2pMsg() {
        Message message = null;
        try {
            return p2pMsgQueue.take();
        } catch (InterruptedException e) {
        }
        return message;
    }
}
