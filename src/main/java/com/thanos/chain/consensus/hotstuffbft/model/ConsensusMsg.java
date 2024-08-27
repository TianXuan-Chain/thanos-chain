package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.network.protocols.base.MessageType;

/**
 * 类ConsensusMsg.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 16:43:45
 */
public class ConsensusMsg extends Message {

    protected long epoch;

    protected ConsensusMsg(byte[] encode) {
        super(encode);
    }

    public ConsensusMsg(byte type, byte code, byte remoteType, long rpcId, byte[] nodeId, byte[] encoded) {
        super(type, code, remoteType, rpcId, nodeId, encoded);
    }

    public long getEpoch() {
        return epoch;
    }

    public byte getType() {
        return MessageType.CONSENSUS.getType();
    }

    public  ConsensusCommand getCommand() {
        return ConsensusCommand.fromByte(getCode());
    }

    public ProcessResult<Void> verify(ValidatorVerifier verifier) {
        return ProcessResult.ofSuccess();
    }
}
