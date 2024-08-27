package com.thanos.chain.network.protocols.ssl;

import com.thanos.chain.network.protocols.base.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.nio.ByteBuffer;

/**
 * MessageEncoder.java description：
 *
 * @Author laiyiyu create on 2021-01-05 19:33:16
 */
public class MessageEncoder extends MessageToByteEncoder<Message> {

    private static final Logger logger = LoggerFactory.getLogger("network");

    static final int MAX_COMPRESS_SIZE = 1024 * 1024 * 64; // 64m

    static final int HEAD_LENGTH = 32 + 64;


    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {

        boolean compress = msg.getEncoded().length > MAX_COMPRESS_SIZE;

        byte[] content;

        if (compress) {
            content = Snappy.compress(msg.getEncoded());
        } else {
            content = msg.getEncoded();
        }

        int contentSize = content.length;

        ByteBuffer header = encodeHeader(msg, compress, contentSize);
        out.writeBytes(header);
        out.writeBytes(content);
    }

    private ByteBuffer encodeHeader(Message msg, boolean compress, int contentSize) {
        byte[] headBuffer = new byte[HEAD_LENGTH];
        headBuffer[0] = (byte) (contentSize >> 24);
        headBuffer[1] = (byte) (contentSize >> 16);
        headBuffer[2] = (byte) (contentSize >> 8);
        headBuffer[3] = (byte) (contentSize);

        headBuffer[4] = msg.getType();
        headBuffer[5] = msg.getCode();
        headBuffer[6] = msg.getRemoteType();

        long msgId = msg.getRpcId();
        headBuffer[7] = (byte) (msgId >> 56);
        headBuffer[8] = (byte) (msgId >> 48);
        headBuffer[9] = (byte) (msgId >> 40);
        headBuffer[10] = (byte) (msgId >> 32);
        headBuffer[11] = (byte) (msgId >> 24);
        headBuffer[12] = (byte) (msgId >> 16);
        headBuffer[13] = (byte) (msgId >> 8);
        headBuffer[14] = (byte) (msgId);
        headBuffer[15] = (byte) (compress ? 1 : 0);

        long frameCreateTime = System.currentTimeMillis();
        headBuffer[16] = (byte) (frameCreateTime >> 56);
        headBuffer[17] = (byte) (frameCreateTime >> 48);
        headBuffer[18] = (byte) (frameCreateTime >> 40);
        headBuffer[19] = (byte) (frameCreateTime >> 32);
        headBuffer[20] = (byte) (frameCreateTime >> 24);
        headBuffer[21] = (byte) (frameCreateTime >> 16);
        headBuffer[22] = (byte) (frameCreateTime >> 8);
        headBuffer[23] = (byte) (frameCreateTime);


        byte[] nodeId = msg.getNodeId();
        for (int i = 32; i < HEAD_LENGTH; i++) {
            headBuffer[i] = nodeId[i - 32];
        }

        int totalLength = 4;
        totalLength += HEAD_LENGTH;
        totalLength += contentSize;

        ByteBuffer result = ByteBuffer.allocate(4 + totalLength - contentSize);
        result.putInt(totalLength);
        result.putInt(HEAD_LENGTH);
        result.put(headBuffer);
        result.flip();
        return result;
    }
}
