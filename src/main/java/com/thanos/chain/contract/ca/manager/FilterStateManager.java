package com.thanos.chain.contract.ca.manager;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.contract.ca.resolver.CaJavaCompiler;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.*;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * FilterStateManager.java description：
 *
 * @Author laiyiyu create on 2021-03-24 17:22:49
 */
public class FilterStateManager extends StateManager  {

    int voteThreshold;

    //当前委员会(公钥)准入投票列表
    List<ByteArrayWrapper> currentAgreeVotes;

    List<ByteArrayWrapper> currentRejectVotes;

    Optional<FilterCandidate> currentCandidate;

    protected FilterStateManager(EpochState epochState, GlobalStateRepositoryImpl repository) {
        super(epochState, repository);
        this.voteThreshold = currentCommittees.size() * epochState.getGlobalEventState().getThresholdMolecular() / epochState.getGlobalEventState().getThresholdDenominator() + 1;
        FilterCandidateStateSnapshot filterCandidateStateSnapshot = (FilterCandidateStateSnapshot) epochState.getGlobalEventState().getCandidateStateSnapshot();
        this.currentAgreeVotes = filterCandidateStateSnapshot.getCurrentAgreeVotes();
        this.currentRejectVotes = filterCandidateStateSnapshot.getCurrentRejectVotes();
        this.currentCandidate = filterCandidateStateSnapshot.getCurrentCandidate();
    }

    @Override
    protected GlobalNodeEventReceipt doExecute(GlobalNodeEvent nodeEvent) {
        VoteFilterCandidateEvent candidateEvent = (VoteFilterCandidateEvent) nodeEvent.getCommandEvent();


        FilterCandidate candidate = FilterCandidate.convertFrom(candidateEvent);

        if (currentCandidate.isPresent()) {
            if (!currentCandidate.get().equals(candidate)) {
                String error = String.format("error candidate, current candidate:%s,  you candidate:%s", currentCandidate, candidate);
                return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(CandidateEventConstant.VOTE_FAILED), error);
            }

        } else {

            if (this.epochState.getGlobalEventState().getFinishProposalIds().containsKey(Keyable.ofDefault(candidate.getProposalId()))
                    || this.repository.hasProposal(candidate.getProposalId())) {
                return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(CandidateEventConstant.VOTE_FAILED), "duplicate proposal id " + Hex.toHexString(candidate.getProposalId()));
            }

            if (candidateEvent.getVoteType() != CandidateEventConstant.AGREE_VOTE) {
                return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(CandidateEventConstant.VOTE_FAILED), "fist vote must agree");
            }

            ProcessResult compileResult = new CaJavaCompiler(candidate.getCaContractCode().getCodeAddress()).compile(candidate.getCaContractCode());

            if (!compileResult.isSuccess()) {
                return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(CandidateEventConstant.VOTE_FAILED), compileResult.getErrMsg());
            }

            currentCandidate = Optional.of(candidate);
        }


        int processCode = CandidateEventConstant.VOTE_SUCCESS;
        String msg = "VOTE_SUCCESS";
        ByteArrayWrapper senderWrapper = new ByteArrayWrapper(ByteUtil.copyFrom(nodeEvent.getSendAddress()));

        if (candidateEvent.getVoteType() == CandidateEventConstant.AGREE_VOTE) {
            if (!currentAgreeVotes.contains(senderWrapper)) {
                currentAgreeVotes.add(senderWrapper);
            }

            if (currentAgreeVotes.size() >= voteThreshold) {
                if (candidate.getProcessType() == CandidateEventConstant.REGISTER_PROCESS) {
                    epochState.getGlobalEventState().addFilter(candidate);
                } else {
                    epochState.getGlobalEventState().removeFilter(candidate);
                }
                setFinish();
                processCode = CandidateEventConstant.AGREE_FINISH;
                msg = String.format("doCheck filter [%s] success!", Hex.toHexString(candidate.getCaContractCode().getCodeAddress()));
            }

        } else if (candidateEvent.getVoteType() == CandidateEventConstant.DISAGREE_VOTE) {
            if (!currentRejectVotes.contains(senderWrapper)) {
                currentRejectVotes.add(senderWrapper);
            }

            if (currentRejectVotes.size() > (currentCommittees.size() - voteThreshold)) {
                setFinish();
                processCode = CandidateEventConstant.DISAGREE_FINISH;
                msg = String.format("register filter [%s] has be reject!", candidate.getCaContractCode().getCodeAddress());
            }
        } else {
            if (senderWrapper.equals(this.currentAgreeVotes.get(0))) {
                setFinish();
                processCode = CandidateEventConstant.REVOKE_FINISH;
                msg = String.format("register filter [%s] has be revoke!", candidate.getCaContractCode().getCodeAddress());
            } else {
                processCode = CandidateEventConstant.VOTE_FAILED;
                msg = String.format("sender [%s] not a revoker!", senderWrapper);
            }
        }


        return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(processCode), msg);
    }

    @Override
    protected CandidateStateSnapshot exportCurrentSnapshot() {
        return new FilterCandidateStateSnapshot(currentAgreeVotes, currentRejectVotes, currentCandidate);
    }

    @Override
    protected void setFinish() {
        super.setFinish();
        byte[] id = ByteUtil.copyFrom(this.currentCandidate.get().getProposalId());
        this.epochState.getGlobalEventState().getFinishProposalIds().put(Keyable.ofDefault(id), new CaFinishProposalId(id));
    }

    protected void doClean() {
        this.epochState = null;
        currentAgreeVotes.clear();
        currentRejectVotes.clear();
        currentCommittees.clear();
        currentCandidate = null;
    }
}
