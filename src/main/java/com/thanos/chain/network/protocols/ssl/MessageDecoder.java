package com.thanos.chain.network.protocols.ssl;

import com.thanos.common.utils.ByteUtil;
import com.thanos.chain.network.protocols.base.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.nio.ByteBuffer;

/**
 * MessageDecoder.java description：
 *
 * @Author laiyiyu create on 2021-01-06 09:47:21
 */
public class MessageDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger logger = LoggerFactory.getLogger("network");

    private static final int MAX_MSG_LENGTH = 1024 * 1024 * 512;

    public MessageDecoder() {
        super(MAX_MSG_LENGTH, 0, 4, 0, 4);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (null == frame) {
                return null;
            }

            ByteBuffer byteBuffer = frame.nioBuffer();

            int totalLength = byteBuffer.limit();
            int headLength = byteBuffer.getInt();;


            byte[] headBuffer = new byte[headLength];
            byteBuffer.get(headBuffer);


            int contentSize = headBuffer[0] & 0xFF;
            contentSize = (contentSize << 8) + (headBuffer[1] & 0xFF);
            contentSize = (contentSize << 8) + (headBuffer[2] & 0xFF);
            contentSize = (contentSize << 8) + (headBuffer[3] & 0xFF);

            byte type = headBuffer[4];
            byte code = headBuffer[5];
            byte remoteType = headBuffer[6];

            long msgId = headBuffer[7] & 0xFF;
            msgId = (msgId << 8) + (headBuffer[8] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[9] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[10] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[11] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[12] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[13] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[14] & 0xFF);

            boolean compress = (headBuffer[15] == 1);

            long frameCreateTime = headBuffer[16] & 0xFF;
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[17] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[18] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[19] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[20] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[21] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[22] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[23] & 0xFF);

            byte[] nodeId = new byte[64];
            for (int i = 32; i < MessageEncoder.HEAD_LENGTH; i++) {
                nodeId[i - 32] = headBuffer[i];
            }

            byte[] content = new byte[contentSize];
            byteBuffer.get(content);

            if (logger.isDebugEnabled()) {
                //ProposalMsg
                if (type == 1 && code == 0) {
                    logger.debug("receive msg ProposalMsg.size:[{}], cost[{}]ms", ByteUtil.getPrintSize(content.length), (System.currentTimeMillis() - frameCreateTime));
                }
            }

            if (compress) {
                logger.debug("receive msg need compress!");
                content = Snappy.uncompress(content);
            }

            return new Message(type, code, remoteType, msgId, nodeId, content);
        } catch (Exception e) {
            logger.error("decode exception, {}", ExceptionUtils.getStackTrace(e));
            ctx.close();
        } finally {
            if (null != frame) {
                frame.release();
            }
        }
        return null;
    }
}
