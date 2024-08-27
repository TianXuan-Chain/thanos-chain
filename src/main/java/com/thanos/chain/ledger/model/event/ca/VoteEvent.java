package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.event.CommandEvent;

/**
 * VoteEvent.java description：
 *
 * @Author laiyiyu create on 2021-04-01 14:38:22
 */
public abstract class VoteEvent extends CommandEvent {

    //赞同/取消
    int voteType;

    //register/cancel
    int processType;

    byte[] proposalId;


    public VoteEvent(byte[] rlpEncoded) {
        super(rlpEncoded);
    }



    public int getVoteType() {
        return voteType;
    }

    public int getProcessType() {
        return processType;
    }

    public byte[] getProposalId() {
        return proposalId;
    }
}
