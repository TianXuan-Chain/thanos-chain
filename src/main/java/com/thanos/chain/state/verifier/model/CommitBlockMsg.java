package com.thanos.chain.state.verifier.model;

import com.thanos.chain.ledger.model.Block;

/**
 * CommitBlockMsg.java description：
 *
 * @Author laiyiyu create on 2020-09-27 17:41:37
 */
public class CommitBlockMsg extends GlobalStateVerifierMsg  {

    public Block block;

    public CommitBlockMsg(Block block) {
        super(null);
        this.block = block;
    }


    @Override
    public byte getCode() {
        return GlobalStateVerifierCommand.COMMIT_BLOCK.getCode();
    }

    @Override
    public GlobalStateVerifierCommand getCommand() {
        return GlobalStateVerifierCommand.COMMIT_BLOCK;
    }

    @Override
    public void releaseReference() {
        block = null;
    }
}
