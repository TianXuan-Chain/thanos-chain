package com.thanos.chain.state.sync.layer2.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 类ConsensusCommand.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-26 19:40:57
 */
public enum Layer2StateChainSyncCommand {
    // used to initiate new sync
    LAYER2_STATE_SYNC_REQUEST((byte)0),

    LAYER2_STATE_SYNC_RESPONSE((byte)1),
    // used to notify about new txn commit
    COMMIT((byte)2),
    GET_STATE((byte)3),
    // used to generate epoch proof
    GET_EPOCH_PROOF((byte)4),
    // Receive a notification via a given channel when coordinator is initialized.
    WAIT_INITIALIZE((byte)5);

    private static final Map<Byte, Layer2StateChainSyncCommand> byteToCommandMap = new HashMap<>();

    static {
        for (Layer2StateChainSyncCommand consensusCommand : Layer2StateChainSyncCommand.values()) {
            byteToCommandMap.put(consensusCommand.code, consensusCommand);
        }
    }

    private byte code;

    Layer2StateChainSyncCommand(byte code) {
        this.code = code;
    }

    public static Layer2StateChainSyncCommand fromByte(byte code) {
        return byteToCommandMap.get(code);
    }

    public byte getCode() {
        return code;
    }
}
