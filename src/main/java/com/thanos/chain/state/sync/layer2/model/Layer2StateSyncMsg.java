package com.thanos.chain.state.sync.layer2.model;

import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.network.protocols.base.MessageType;

/**
 * Layer2StateSyncMsg.java description：
 *
 * @Author laiyiyu create on 2020-03-09 07:31:05
 */
public class Layer2StateSyncMsg extends Message {

    protected Layer2StateSyncMsg(byte[] encode) {
        super(encode);
    }

    public byte getType() {
        return MessageType.LAYER_2_STATE_SYNC.getType();
    }

    public  Layer2StateChainSyncCommand getCommand() {
        return Layer2StateChainSyncCommand.fromByte(getCode());
    }
}
