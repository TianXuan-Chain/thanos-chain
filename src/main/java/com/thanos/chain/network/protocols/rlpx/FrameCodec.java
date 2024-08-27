package com.thanos.chain.network.protocols.rlpx;

import com.thanos.chain.network.protocols.base.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.digests.KeccakDigest;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.modes.SICBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.util.encoders.Hex;

import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * 类FrameCodec.java的实现描述：
 *
 * @Author laiyiyu create on 2020-01-22 15:06:59
 */
public class FrameCodec {

    //private static final Logger logger = LoggerFactory.getLogger("network");
    private final StreamCipher enc;
    private final StreamCipher dec;
    private final KeccakDigest egressMac;
    private final KeccakDigest ingressMac;
    private final byte[] mac;
    private boolean isHeadRead;
    byte type;
    byte code;
    int contentSize;
    // 0:request, 1:response;
    byte remoteType = 0;
    long msgId;


    public FrameCodec(EncryptionHandshake.Secrets secrets) {
//        logger.warn("mac:" + Hex.toHexString(secrets.mac));
//        logger.warn("aes:" + Hex.toHexString(secrets.aes));
        this.mac = secrets.mac;
        BlockCipher cipher;
        enc = new SICBlockCipher(cipher = new AESEngine());
        enc.init(true, new ParametersWithIV(new KeyParameter(secrets.aes), new byte[cipher.getBlockSize()]));
        dec = new SICBlockCipher(cipher = new AESEngine());
        dec.init(false, new ParametersWithIV(new KeyParameter(secrets.aes), new byte[cipher.getBlockSize()]));
        egressMac = secrets.egressMac;
        ingressMac = secrets.ingressMac;
    }

    private AESEngine makeMacCipher() {
        // Stateless AES encryption
        AESEngine macc = new AESEngine();
        macc.init(true, new KeyParameter(mac));
        return macc;
    }

    public static class Frame {
        //totoal length = 1 + 1 + 4 + 1 + 8 + 64 = 79
        byte type;
        byte code;
        int contentSize;
        // 0:request, 1:response;
        byte remoteType = 0;

        long msgId = -1;
        //
        byte[] nodeId;
        InputStream payload;

        public Frame(byte type, byte code, byte remoteType, long msgId, int contentSize, byte[] nodeId, InputStream payload) {
            this.type = type;
            this.code = code;
            this.contentSize = contentSize;
            this.remoteType = remoteType;
            this.nodeId = nodeId;
            this.msgId = msgId;
            this.payload = payload;
        }

        public Frame(byte type, byte code, byte remoteType, long msgId, byte[] nodeId, byte[] payload) {
            this.type = type;
            this.code = code;
            this.contentSize = payload.length;
            this.remoteType = remoteType;
            this.nodeId = nodeId;
            this.msgId = msgId;
            this.payload = new ByteArrayInputStream(payload);
        }

        public Frame(byte type, byte code, byte[] nodeId,  byte[] payload) {
            this.type = type;
            this.code = code;
            // empty nodeId
            this.nodeId = nodeId;
            this.contentSize = payload.length;
            this.payload = new ByteArrayInputStream(payload);
        }

        public int getContentSize() {
            return contentSize;
        }

        public byte getType() {return  type;}

        public byte getCode() {return code;}

        public byte getRemoteType() {
            return remoteType;
        }

        public long getMsgId() {
            return msgId;
        }

        public byte[] getNodeId() {
            return nodeId;
        }

        public InputStream getStream() {
            return payload;
        }
    }

    public void writeFrame(FrameCodec.Frame frame, ByteBuf buf) throws IOException {
        writeFrame(frame, new ByteBufOutputStream(buf));
    }

    public void writeFrame(FrameCodec.Frame frame, OutputStream out) throws IOException {

        byte[] headBuffer = new byte[32];
        headBuffer[0] = (byte)(frame.contentSize >> 24);
        headBuffer[1] = (byte)(frame.contentSize >> 16);
        headBuffer[2] = (byte)(frame.contentSize >> 8);
        headBuffer[3] = (byte)(frame.contentSize);

        headBuffer[4] = frame.type;
        headBuffer[5] = frame.code;
        headBuffer[6] = frame.remoteType;

        headBuffer[7] = (byte)(frame.msgId >> 56);
        headBuffer[8] = (byte)(frame.msgId >> 48);
        headBuffer[9] = (byte)(frame.msgId >> 40);
        headBuffer[10] = (byte)(frame.msgId >> 32);
        headBuffer[11] = (byte)(frame.msgId >> 24);
        headBuffer[12] = (byte)(frame.msgId >> 16);
        headBuffer[13] = (byte)(frame.msgId >> 8);
        headBuffer[14] = (byte)(frame.msgId);

        //逐字节加密，将headBuffer[0-15] 的内容加密转成字节后，写入到headBuffer[0-15]中
        enc.processBytes(headBuffer, 0, 16, headBuffer, 0);
        // Header MAC， headBuffer[16-31] 存储的是 headBuffer[0-15] 内容的校验码
        updateMac(egressMac, headBuffer, 0, headBuffer, 16, true);

        // 将完整的加密headBuffer 内容写入out 中；
        if (frame.getType() == MessageType.CONSENSUS.getType()) {
//            System.out.println("MessageType.CONSENSUS head:" + headBuffer.length);
//            System.out.println("MessageType.CONSENSUS content size:" + frame.contentSize);
        }
        out.write(headBuffer); // 发送消息头




//        // 这里会再消息广播时，由于  frame.nodeId 传递的是引用，会修改nodeId.
//        enc.processBytes(frame.nodeId, 0, frame.nodeId.length, frame.nodeId, 0);
//        egressMac.update(frame.nodeId, 0, frame.nodeId.length);
//        out.write(frame.nodeId, 0, frame.nodeId.length);


        //int totalSize = frame.nodeId.length + frame.contentSize + padding + 16;
        byte[] nodeIdBuf = new byte[frame.nodeId.length];
        enc.processBytes(frame.nodeId, 0, frame.nodeId.length, nodeIdBuf, 0);
        egressMac.update(nodeIdBuf, 0, frame.nodeId.length);
        //System.arraycopy(frame.nodeId, 0, buff, 0, frame.nodeId.length);
        //System.out.println("send nodeid:" + Hex.toHexString(frame.nodeId));
        out.write(nodeIdBuf, 0, frame.nodeId.length); // send nodeid

        // partition send content
        byte[] partitionBuf = new byte[512];
        while (true) {
            int n = frame.payload.read(partitionBuf);
            if (n <= 0) break;
            enc.processBytes(partitionBuf, 0, n, partitionBuf, 0);
            egressMac.update(partitionBuf, 0, n);
            out.write(partitionBuf, 0, n);
        }

        // send padding
        int padding = 16 - (frame.contentSize % 16);
        if (padding < 16) {
            byte[] pad = new byte[padding];
            //System.arraycopy(pad, 0, buff, frame.nodeId.length + frame.contentSize, pad.length);
            enc.processBytes(pad, 0, padding, pad, 0);
            egressMac.update(pad, 0, padding); // absorb
            out.write(pad, 0, padding);
        }

        // send Frame mac
        byte[] macBuffer = new byte[egressMac.getDigestSize()];
        doSum(egressMac, macBuffer); // fmacseed
        updateMac(egressMac, macBuffer, 0, macBuffer,  0, true);
        out.write(macBuffer, 0, 16);

//        enc.processBytes(buff, 0, totalSize - 16, buff, 0);
//        updateMac(egressMac, buff, 0, buff,  totalSize - 16, true);
       // System.arraycopy(macBuffer, 0, buff, frame.nodeId.length + frame.contentSize + padding, 16);
//
//        System.out.println("send buff size:" + buff.length);
//        System.out.println("send buff content:");
//        for(byte t: buff) {System.out.print(t + ",");}
//        System.out.println();

//        out.write(buff, 0, totalSize);
//        System.out.println("totalSize:" + totalSize);
    }


    public List<FrameCodec.Frame> readFrames(ByteBuf buf) throws IOException {
        return readFrames(new ByteBufInputStream(buf));
    }

    public List<FrameCodec.Frame> readFrames(DataInput inp) throws IOException {
        if (!isHeadRead) {
            byte[] headBuffer = new byte[32];
            try {
                inp.readFully(headBuffer);
            } catch (EOFException e) {
                //e.printStackTrace();
                return null;
            }

            // Header MAC, 验证校验码
            updateMac(ingressMac, headBuffer, 0, headBuffer, 16, false);
            // 解密内容
            dec.processBytes(headBuffer, 0, 16, headBuffer, 0);

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

            isHeadRead = true;
        }


        int nodeIdSize = 64;

        int padding = 16 - (this.contentSize % 16);
        if (padding == 16) padding = 0;

        int macSize = 16;

        byte[] buffer = new byte[nodeIdSize + this.contentSize + padding + macSize];

        try {
            inp.readFully(buffer);
//            System.out.println("receive buffer size:" + buffer.length);
//            System.out.println("receive buffer content:");
//            for(byte t: buffer) {System.out.print(t + ",");}
//            System.out.println("");
        } catch (EOFException e) {
//            System.out.println("buffer size:" + buffer.length);
//            e.printStackTrace();
            return null;
        }

        int frameSize = buffer.length - macSize;
        //updateMac(ingressMac, buffer, 0, buffer, frameSize, false);
        ingressMac.update(buffer, 0, frameSize);
        dec.processBytes(buffer, 0, frameSize, buffer, 0);

//        System.out.println("receive decode content:");
//        for(byte t: buffer) {System.out.print(t + ",");}
//        System.out.println("");


        byte[] nodeId = new byte[64];
        System.arraycopy(buffer, 0, nodeId, 0, nodeIdSize);
//        System.out.println("receive nodeid:" + Hex.toHexString(nodeId));

        InputStream payload = new ByteArrayInputStream(buffer, nodeIdSize, this.contentSize);

        byte[] macBuffer = new byte[ingressMac.getDigestSize()];

        // Frame MAC
        doSum(ingressMac, macBuffer); // fmacseed
        updateMac(ingressMac, macBuffer, 0, buffer, frameSize, false);
        isHeadRead = false;
        FrameCodec.Frame frame = new FrameCodec.Frame(this.type, this.code, this.remoteType, this.msgId, this.contentSize, nodeId, payload);
        return Collections.singletonList(frame);
    }

    private byte[] updateMac(KeccakDigest mac, byte[] seed, int offset, byte[] out, int outOffset, boolean egress) throws IOException {
        byte[] aesBlock = new byte[mac.getDigestSize()];
        doSum(mac, aesBlock);
        makeMacCipher().processBlock(aesBlock, 0, aesBlock, 0);
        // Note that although the mac digest contentSize is 16 bytes, we only use 32 bytes in the computation
        int length = 16;
        for (int i = 0; i < length; i++) {
            aesBlock[i] ^= seed[i + offset];
        }
        mac.update(aesBlock, 0, length);
        byte[] result = new byte[mac.getDigestSize()];
        doSum(mac, result);
        if (egress) {
            System.arraycopy(result, 0, out, outOffset, length);
        } else {
            for (int i = 0; i < length; i++) {
                if (out[i + outOffset] != result[i]) {
                    throw new IOException("MAC mismatch");
                }
            }
        }
        return result;
    }

    private void doSum(KeccakDigest mac, byte[] out) {
        // doFinal without resetting the MAC by using clone of digest state
        new KeccakDigest(mac).doFinal(out, 0);
    }

}
