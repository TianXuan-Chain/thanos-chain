package com.thanos.chain.consensus.hotstuffbft;

import com.thanos.chain.consensus.hotstuffbft.executor.ConsensusEventExecutor;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.consensus.hotstuffbft.store.LivenessStorageData;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * RecoveryMsgProcessor.java description：
 *
 * @Author laiyiyu create on 2020-03-07 18:16:05
 */
public class RecoveryMsgProcessor extends ConsensusProcessor<LivenessStorageData.RecoveryData> {

    HotstuffNetInvoker netInvoker;

    ConsensusEventExecutor consensusEventExecutor;

    HotStuffChainSyncCoordinator hotStuffChainSyncCoordinator;

    long lastCommittedRound;

    public RecoveryMsgProcessor(EpochState epochState, HotstuffNetInvoker netInvoker, ConsensusEventExecutor consensusEventExecutor, HotStuffChainSyncCoordinator hotStuffChainSyncCoordinator, long lastCommittedRound) {
        this.epochState = epochState;
        this.netInvoker = netInvoker;
        this.consensusEventExecutor = consensusEventExecutor;
        this.hotStuffChainSyncCoordinator = hotStuffChainSyncCoordinator;
        this.lastCommittedRound = lastCommittedRound;
    }

    public void releaseResource() {
        this.epochState = null;
        this.netInvoker = null;
        this.consensusEventExecutor = null;
        this.hotStuffChainSyncCoordinator.release();
        this.hotStuffChainSyncCoordinator = null;
    }

    @Override
    public ProcessResult<LivenessStorageData.RecoveryData> process(ConsensusMsg consensusMsg) {
        switch (consensusMsg.getCommand()) {
            case PROPOSAL:
                return processProposalMsg((ProposalMsg) consensusMsg);
            case VOTE:
                return processVoteMsg((VoteMsg) consensusMsg);
            default:
                break;
        }
        return ProcessResult.ofError("un know message!");
    }

    @Override
    public void saveTree(List<Event> events, List<QuorumCert> qcs) {
        this.hotStuffChainSyncCoordinator.eventTreeStore.livenessStorage.saveTree(events, qcs);
    }

    public ProcessResult<LivenessStorageData.RecoveryData> processProposalMsg(
            ProposalMsg proposalMsg) {
        byte[] nodeId = proposalMsg.getNodeId();
        HotstuffChainSyncInfo syncInfo = proposalMsg.getHotstuffChainSyncInfo();
        return syncUp(syncInfo, nodeId);
    }

    public ProcessResult<LivenessStorageData.RecoveryData> processVoteMsg(
            VoteMsg voteMsg) {
        byte[] nodeId = voteMsg.getNodeId();
        HotstuffChainSyncInfo syncInfo = voteMsg.getHotstuffChainSyncInfo();
        return syncUp(syncInfo, nodeId);
    }

    private ProcessResult<LivenessStorageData.RecoveryData> syncUp(HotstuffChainSyncInfo syncInfo, byte[] preferredPeer) {
        ProcessResult<Void> verifyRes = syncInfo.verify(this.epochState.getValidatorVerifier());
        if (!verifyRes.isSuccess()) {
            logger.warn("recover syncUp error! {}", verifyRes.getErrMsg());
            return ProcessResult.ofError("syncInfo verify error!");
        }
        Assert.assertTrue("[RecoveryMsgProcessor] Received sync info has lower round number than committed event", syncInfo.getHighestRound() > lastCommittedRound);
        Assert.assertTrue("[RecoveryMsgProcessor] Received sync info is in different epoch than committed event", syncInfo.getEpoch() == this.epochState.getEpoch());

        return hotStuffChainSyncCoordinator.fastForwardSync(syncInfo.getHighestCommitCert(), preferredPeer);
    }
}
