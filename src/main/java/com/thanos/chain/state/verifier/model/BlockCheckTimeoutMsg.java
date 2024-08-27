package com.thanos.chain.state.verifier.model;

import com.thanos.chain.consensus.hotstuffbft.model.ConsensusCommand;

/**
 * BlockCheckTimeoutMsg.java description：
 *
 * @Author laiyiyu create on 2020-09-25 15:28:03
 */
public class BlockCheckTimeoutMsg extends GlobalStateVerifierMsg {

    public final long number;

    public BlockCheckTimeoutMsg(long number) {
        super(null);
        this.number = number;
    }


    @Override
    public byte getCode() {
        return GlobalStateVerifierCommand.BLOCK_SIGN_TIMEOUT.getCode();
    }

    @Override
    public GlobalStateVerifierCommand getCommand() {
        return GlobalStateVerifierCommand.BLOCK_SIGN_TIMEOUT;
    }
}
