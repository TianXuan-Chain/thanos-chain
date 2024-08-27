package com.thanos.chain.network.protocols.ssl;

import com.thanos.common.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.xerial.snappy.Snappy;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/**
 * 类FrameCodec.java的实现描述：
 *
 * @Author laiyiyu create on 2020-01-22 15:06:59
 */
public class FrameCodec2 {

    private static final Logger logger = LoggerFactory.getLogger("network");

    static final int HEAD_LENGTH = 32 + 64;

    private boolean isHeadRead;

    byte type;

    byte code;

    int contentSize;

    // 0:request, 1:response;
    byte remoteType = 0;

    long msgId;

    long frameCreateTime;

    boolean compress;

    byte[] nodeId;

    public static final int MAX_COMPRESS_SIZE = 1024 * 1024 * 64; // 64m

    public FrameCodec2() {
    }

    public static class Frame {
        //totoal length = 1 + 1 + 4 + 1 + 8 + 64 = 79
        byte type;
        byte code;
        int contentSize;
        // 0:request, 1:response;
        byte remoteType = 0;

        long msgId = -1;

        //long frameCreateTime;
        //
        byte[] nodeId;
        byte[] payload;

        public Frame(byte type, byte code, byte remoteType, long msgId, byte[] nodeId, byte[] payload) {
            this.type = type;
            this.code = code;
            this.contentSize = payload.length;
            this.remoteType = remoteType;
            this.nodeId = nodeId;
            this.msgId = msgId;
            this.payload = payload;
        }

        public int getContentSize() {
            return contentSize;
        }

        public byte getType() {
            return type;
        }

        public byte getCode() {
            return code;
        }

        public byte getRemoteType() {
            return remoteType;
        }

        public long getMsgId() {
            return msgId;
        }

        public byte[] getNodeId() {
            return nodeId;
        }

        public byte[] getStream() {
            return payload;
        }

        @Override
        public String toString() {
            return "Frame{" +
                    "type=" + type +
                    ", code=" + code +
                    ", contentSize=" + contentSize +
                    ", remoteType=" + remoteType +
                    ", msgId=" + msgId +
                    ", nodeId=" + (nodeId == null ? "[]" : Hex.toHexString(nodeId)) +
                    '}';
        }
    }

    public void writeFrame(FrameCodec2.Frame frame, ByteBuf buf) throws IOException {
        writeFrameWithoutEncript(frame, new ByteBufOutputStream(buf));
    }

    public void writeFrameWithoutEncript(FrameCodec2.Frame frame, OutputStream out) throws IOException {
        boolean compress = frame.contentSize > MAX_COMPRESS_SIZE;
        if (compress) {
            frame.payload = Snappy.compress(frame.payload);
            frame.contentSize = frame.payload.length;
        }

        byte[] headBuffer = new byte[HEAD_LENGTH];
        headBuffer[0] = (byte) (frame.contentSize >> 24);
        headBuffer[1] = (byte) (frame.contentSize >> 16);
        headBuffer[2] = (byte) (frame.contentSize >> 8);
        headBuffer[3] = (byte) (frame.contentSize);

        headBuffer[4] = frame.type;
        headBuffer[5] = frame.code;
        headBuffer[6] = frame.remoteType;

        headBuffer[7] = (byte) (frame.msgId >> 56);
        headBuffer[8] = (byte) (frame.msgId >> 48);
        headBuffer[9] = (byte) (frame.msgId >> 40);
        headBuffer[10] = (byte) (frame.msgId >> 32);
        headBuffer[11] = (byte) (frame.msgId >> 24);
        headBuffer[12] = (byte) (frame.msgId >> 16);
        headBuffer[13] = (byte) (frame.msgId >> 8);
        headBuffer[14] = (byte) (frame.msgId);
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


        for (int i = 32; i < HEAD_LENGTH; i++) {
            headBuffer[i] = frame.nodeId[i - 32];
        }

        out.write(headBuffer); // 写入消息头
        out.write(frame.payload, 0, frame.contentSize);// 写入消息体
    }

    public List<FrameCodec2.Frame> readFrames(ByteBuf buf) throws IOException {
        return readFramesWithoutEncript(new ByteBufInputStream(buf));
    }

    public List<FrameCodec2.Frame> readFramesWithoutEncript(DataInput inp) throws IOException {
        if (!isHeadRead) {
            byte[] headBuffer = new byte[HEAD_LENGTH];
            try {
                inp.readFully(headBuffer);
            } catch (EOFException e) {
                return null;
            }

            int contentSize = headBuffer[0] & 0xFF;
            contentSize = (contentSize << 8) + (headBuffer[1] & 0xFF);
            contentSize = (contentSize << 8) + (headBuffer[2] & 0xFF);
            contentSize = (contentSize << 8) + (headBuffer[3] & 0xFF);

            this.contentSize = contentSize;

            this.type = headBuffer[4];
            this.code = headBuffer[5];
            this.remoteType = headBuffer[6];

            long msgId = headBuffer[7] & 0xFF;
            msgId = (msgId << 8) + (headBuffer[8] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[9] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[10] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[11] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[12] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[13] & 0xFF);
            msgId = (msgId << 8) + (headBuffer[14] & 0xFF);
            this.msgId = msgId;

            this.compress = (headBuffer[15] == 1);

            long frameCreateTime = headBuffer[16] & 0xFF;
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[17] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[18] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[19] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[20] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[21] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[22] & 0xFF);
            frameCreateTime = (frameCreateTime << 8) + (headBuffer[23] & 0xFF);
            this.frameCreateTime = frameCreateTime;

            byte[] nodeId = new byte[64];
            for (int i = 32; i < HEAD_LENGTH; i++) {
                nodeId[i - 32] = headBuffer[i];
            }
            this.nodeId = nodeId;
            isHeadRead = true;
        }

        byte[] content = new byte[this.contentSize];
        try {
            inp.readFully(content);
        } catch (EOFException e) {
            return null;
        }


        if (logger.isDebugEnabled()) {
            //ProposalMsg
            if (type == 1 && code == 0) {
                logger.debug("receive ProposalMsg.size:[{}], cost[{}]ms", ByteUtil.getPrintSize(content.length), (System.currentTimeMillis() - this.frameCreateTime));
            }
        }

        isHeadRead = false;
        if (compress) {
            content = Snappy.uncompress(content);
        }

        FrameCodec2.Frame frame = new FrameCodec2.Frame(this.type, this.code, this.remoteType, this.msgId, this.nodeId, content);
        return Collections.singletonList(frame);
    }

    public void clear() {
        nodeId = null;
    }
}
