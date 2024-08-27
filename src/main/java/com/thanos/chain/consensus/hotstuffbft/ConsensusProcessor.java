package com.thanos.chain.consensus.hotstuffbft;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.ConsensusMsg;
import com.thanos.chain.consensus.hotstuffbft.model.Event;
import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.consensus.hotstuffbft.model.QuorumCert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ConsensusProcessor.java description：
 *
 * @Author laiyiyu create on 2020-03-07 18:42:54
 */
public abstract class ConsensusProcessor<T> {

    static final Logger logger = LoggerFactory.getLogger("consensus");

    static final boolean IS_DEBUG_ENABLED = logger.isDebugEnabled();

    static final boolean IS_TRACE_ENABLED = logger.isTraceEnabled();

    EpochState epochState;

    public abstract ProcessResult<T> process(ConsensusMsg consensusMsg);

    public EpochState getEpochState() {
        return epochState;
    }

    public abstract void saveTree(List<Event> events, List<QuorumCert> qcs);

    public void releaseResource() {}
}
