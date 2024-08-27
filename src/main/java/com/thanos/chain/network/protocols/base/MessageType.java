package com.thanos.chain.network.protocols.base;

import java.util.HashMap;
import java.util.Map;

/**
 * 类MessageType.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-26 17:27:15
 */
public enum MessageType {
    P2P((byte)0),
    CONSENSUS((byte)1),
    LAYER_2_STATE_SYNC((byte)2),
    STATE_SHARDING((byte)3),
    STATE_VERIFIER((byte)4),
    TRANSACTIONS((byte)5);


    private byte type;

    private static final Map<Byte, MessageType> byteToTypeMap = new HashMap<>();

    static {
        for (MessageType messageType : MessageType.values()) {
            byteToTypeMap.put(messageType.type, messageType);
        }
    }

    private MessageType(byte type) {
        this.type = type;
    }


    public static MessageType fromByte(byte type) {
        return byteToTypeMap.get(type);
    }

    public byte getType() {
        return type;
    }
}
