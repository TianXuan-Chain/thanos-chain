package com.thanos.chain.state.verifier.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 类ConsensusCommand.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-26 19:40:57
 */
public enum GlobalStateVerifierCommand {
    // used to initiate new sync
    LOCAL_BLOCK_SIGN((byte)0),
    GET_BLOCK_SIGN_REQ((byte)1),
    BLOCK_SIGN_RESP((byte)2),
    BLOCK_SIGN_TIMEOUT((byte)3),
    COMMIT_BLOCK((byte)4),


    WAIT_INITIALIZE((byte)5);

    private static final Map<Byte, GlobalStateVerifierCommand> byteToCommandMap = new HashMap<>();

    static {
        for (GlobalStateVerifierCommand consensusCommand : GlobalStateVerifierCommand.values()) {
            byteToCommandMap.put(consensusCommand.code, consensusCommand);
        }
    }

    private byte code;

    GlobalStateVerifierCommand(byte code) {
        this.code = code;
    }

    public static GlobalStateVerifierCommand fromByte(byte code) {
        return byteToCommandMap.get(code);
    }

    public byte getCode() {
        return code;
    }
}
