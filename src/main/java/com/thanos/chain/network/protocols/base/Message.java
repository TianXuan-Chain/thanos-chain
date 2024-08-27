/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.thanos.chain.network.protocols.base;

import com.thanos.common.utils.ByteUtil;
import com.thanos.chain.ledger.model.RLPModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 类PeerClient.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-26 11:09:08
 */
public class Message extends RLPModel {

    protected static final Logger logger = LoggerFactory.getLogger("network");

    private static AtomicLong messageId = new AtomicLong(0);

    /**
     * @MessageType, 消息类型，共识/p2p/分片
     */
    protected byte type;

    /**
     * 命令码
     */
    protected byte code;

    //protected byte[] encoded;

    // 0:request, 1:response;
    protected byte remoteType;

    //for invoke rpc
    protected long rpcId = -1;

    // for invoke channel
    protected byte[] nodeId;

    public Message() {
        super(null);
        this.type = getType();
        this.code = getCode();
        this.rpcId = -1;
    }

    public Message(byte[] encoded) {
        super(encoded);
        this.type = getType();
        this.code = getCode();
        this.rpcId = -1;
        //this.encoded = encoded;
    }

    @Override
    protected byte[] rlpEncoded() {
        return rlpEncoded;
    }

    @Override
    protected void rlpDecoded() {
        // do nothing
    }

    public Message(byte type, byte code, byte remoteType, long rpcId,  byte[] nodeId, byte[] encoded) {
        super(encoded);
        this.type = type;
        this.code = code;
        this.remoteType = remoteType;
        this.nodeId = nodeId;
        this.rpcId = rpcId;
        //this.encoded = encoded;
    }

    public byte getType() {
        return type;
    }

    public MessageType getMessageType() {
        return MessageType.fromByte(this.getType());
    }

    public byte getCode() {
        return code;
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    public void setNodeId(byte[] nodeId) {
        this.nodeId = ByteUtil.copyFrom(nodeId);
    }

    public long getRpcId() {
        return rpcId;
    }

    public void setRpcId() {
        if (this.rpcId == -1) {
            this.rpcId = messageId.getAndIncrement();
        }
    }

    public void setRpcId(long rpcId) {
        if (this.rpcId == -1) {
            this.rpcId = rpcId;
        }
    }

    public boolean isRpcMsg() {
        return this.rpcId != -1;
    }

    public byte getRemoteType() {
        return remoteType;
    }

    public void setRemoteType(byte remoteType) {
        this.remoteType = remoteType;
    }

    public RemotingMessageType getRemotingMessageType() {
        return RemotingMessageType.fromByte(this.getRemoteType());
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", code=" + code +
                ", remoteType=" + remoteType +
                ", rpcId=" + rpcId +
                '}';
    }

    public void clear() {
        this.nodeId = null;
        this.rlpEncoded = null;
    }
}
