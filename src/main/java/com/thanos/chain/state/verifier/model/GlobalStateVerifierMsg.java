package com.thanos.chain.state.verifier.model;

import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.network.protocols.base.MessageType;

/**
 * GlobalStateVerifierMsg.java description：
 *
 * @Author laiyiyu create on 2020-09-17 11:07:42
 */
public class GlobalStateVerifierMsg extends Message {

    protected GlobalStateVerifierMsg(byte[] encode) {
        super(encode);
    }

    public byte getType() {
        return MessageType.STATE_VERIFIER.getType();
    }

    public GlobalStateVerifierCommand getCommand() {
        return GlobalStateVerifierCommand.fromByte(getCode());
    }

    public void releaseReference() {}
}
