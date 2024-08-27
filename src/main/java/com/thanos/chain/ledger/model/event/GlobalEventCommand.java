package com.thanos.chain.ledger.model.event;


import java.util.HashMap;
import java.util.Map;

/**
 * GlobalEventCommand.java description：
 *
 * @Author laiyiyu create on 2021-03-31 10:31:28
 */
public enum GlobalEventCommand {
    PLACEHOLDER_EMPTY((byte)0),
    VOTE_COMMITTEE_CANDIDATE((byte)1),
    VOTE_NODE_CANDIDATE((byte)2),
    VOTE_FILTER_CANDIDATE((byte)3),
    VOTE_NODE_BLACKLIST_CANDIDATE((byte)4),
    PROCESS_OPERATIONS_STAFF((byte)5),
    INVOKE_FILTER((byte)6);


    private static final Map<Byte, GlobalEventCommand> byteToCommandMap = new HashMap<>();

    static {
        for (GlobalEventCommand globalEventCommand : GlobalEventCommand.values()) {
            byteToCommandMap.put(globalEventCommand.code, globalEventCommand);
        }
    }

    private byte code;

    GlobalEventCommand(byte code) {
        this.code = code;
    }

    public static GlobalEventCommand fromByte(byte code) {
        return byteToCommandMap.get(code);
    }

    public byte getCode() {
        return code;
    }
}
