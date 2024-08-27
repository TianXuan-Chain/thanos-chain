package com.thanos.chain.consensus.hotstuffbft.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 类ConsensusCommand.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-26 19:40:57
 */
public enum ConsensusCommand {
    PROPOSAL((byte)0),
    VOTE((byte)1),
    HOTSTUFF_CHAIN_SYNC((byte)2),
    LOCAL_TIMEOUT((byte)3),
    EVENT_RETRIEVAL_REQ((byte)4),
    EVENT_RETRIEVAL_RESP((byte)5),
    LATEST_LEDGER_REQ((byte)6),
    LATEST_LEDGER_RESP((byte)7),
    EPOCH_CHANGE((byte)8),
    EPOCH_RETRIEVAL((byte)9);

    private static final Map<Byte, ConsensusCommand> byteToCommandMap = new HashMap<>();

    static {
        for (ConsensusCommand consensusCommand : ConsensusCommand.values()) {
            byteToCommandMap.put(consensusCommand.code, consensusCommand);
        }
    }

    private byte code;

    ConsensusCommand(byte code) {
        this.code = code;
    }

    public static ConsensusCommand fromByte(byte code) {
        return byteToCommandMap.get(code);
    }

    public byte getCode() {
        return code;
    }
}
