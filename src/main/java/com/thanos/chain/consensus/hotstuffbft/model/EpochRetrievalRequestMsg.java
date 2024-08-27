package com.thanos.chain.consensus.hotstuffbft.model;

/**
 * EpochRetrievalRequestMsg.java description：
 *
 * @Author laiyiyu create on 2020-03-07 19:59:12
 */
public class EpochRetrievalRequestMsg extends ConsensusMsg {

    public long startEpcoh;

    public long endEpoch;

    protected EpochRetrievalRequestMsg(byte[] encode) {
        super(encode);
    }

    public EpochRetrievalRequestMsg(long startEpcoh, long endEpoch) {
        super(null);
        this.startEpcoh = startEpcoh;
        this.endEpoch = endEpoch;
        this.rlpEncoded = rlpEncoded();
    }

    public long getStartEpcoh() {
        return startEpcoh;
    }

    public long getEndEpoch() {
        return endEpoch;
    }

    @Override
    protected byte[] rlpEncoded() {
        // todo
        return null;
    }

    @Override
    protected void rlpDecoded() {
        // todo
    }

    @Override
    public byte getCode() {
        return ConsensusCommand.EPOCH_RETRIEVAL.getCode();
    }

    @Override
    public ConsensusCommand getCommand() {
        return ConsensusCommand.EPOCH_RETRIEVAL;
    }
}
