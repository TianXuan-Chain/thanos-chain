package com.thanos.chain.network.protocols;

import com.thanos.chain.network.protocols.base.Message;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 类ProtocolDispatcherHandler.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-27 14:52:09
 */
@ChannelHandler.Sharable
public class MessageDuplexHandler extends SimpleChannelInboundHandler<Message> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        switch (msg.getRemotingMessageType()) {
            case REQUEST_MESSAGE:
                processRequest(msg);
                break;
            case RESPONSE_MESSAGE:
                processResponse(msg);
                break;
            default:
                //do nothing
        }
    }

    private void processRequest(Message msg) {
        switch (msg.getMessageType()) {
            case P2P:
                MessageDuplexDispatcher.putP2pMsg(msg);
                break;
            case CONSENSUS:
                MessageDuplexDispatcher.putConsensusMsg(msg);
                break;
            case LAYER_2_STATE_SYNC:
                MessageDuplexDispatcher.putLayer2StateSyncMsg(msg);
                break;
            case STATE_VERIFIER:
                MessageDuplexDispatcher.putGlobalStateVerifierMsg(msg);
                break;
            default:
                //do nothing
        }
    }

    private void processResponse(Message msg) {
        MessageDuplexDispatcher.putResponseMsg(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
