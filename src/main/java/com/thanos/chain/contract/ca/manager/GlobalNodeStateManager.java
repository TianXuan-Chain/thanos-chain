package com.thanos.chain.contract.ca.manager;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.ValidatorVerifier;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.*;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * GlobalNodeStateManager.java description：
 *
 * @Author laiyiyu create on 2021-03-24 16:09:04
 */
public class GlobalNodeStateManager extends StateManager {

    int voteThreshold;

//    //当前最新版本,有新的候选人准入或者退出才自增1
//    int currentVersion;

    //当前委员会(公钥)准入投票列表
    List<ByteArrayWrapper> currentAgreeVotes;

    List<ByteArrayWrapper> currentRejectVotes;


    volatile Optional<NodeCandidate> currentCandidate;


    public GlobalNodeStateManager(EpochState epochState, GlobalStateRepositoryImpl repository) {
        super(epochState, repository);
        this.voteThreshold = currentCommittees.size() * epochState.getGlobalEventState().getThresholdMolecular() / epochState.getGlobalEventState().getThresholdDenominator() + 1;
        NodeCandidateStateSnapshot nodeCandidateStateSnapshot = (NodeCandidateStateSnapshot) epochState.getGlobalEventState().getCandidateStateSnapshot();
        this.currentAgreeVotes = nodeCandidateStateSnapshot.getCurrentAgreeVotes();
        this.currentRejectVotes = nodeCandidateStateSnapshot.getCurrentRejectVotes();
        this.currentCandidate = nodeCandidateStateSnapshot.getCurrentCandidate();
    }

    @Override
    public GlobalNodeEventReceipt doExecute(GlobalNodeEvent nodeEvent) {

        VoteNodeCandidateEvent candidateEvent = (VoteNodeCandidateEvent) nodeEvent.getCommandEvent();

        NodeCandidate candidate = NodeCandidate.convertFrom(candidateEvent);

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

            try {
                new BigInteger(candidateEvent.getCaHash(), 16);
            } catch (Exception e) {
                return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(CandidateEventConstant.VOTE_FAILED), "caHash must hex number type");
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
                ValidatorPublicKeyInfo validatorPublicKeyInfo = candidate.convertToValidatorPublicKeyInfo();
                if (candidate.getProcessType() == CandidateEventConstant.REGISTER_PROCESS) {
                    ValidatorVerifier newValidator = epochState.getGlobalEventState().getValidatorVerifier().addNewValidator(validatorPublicKeyInfo);
                    epochState.getGlobalEventState().resetValidatorVerifier(newValidator);
                } else {
                    ValidatorVerifier newValidator = epochState.getGlobalEventState().getValidatorVerifier().removeOldValidator(validatorPublicKeyInfo);
                    epochState.getGlobalEventState().resetValidatorVerifier(newValidator);
                }
                setFinish();
                processCode = CandidateEventConstant.AGREE_FINISH;
                msg = String.format("process node [%s] success!", Hex.toHexString(candidateEvent.getId()));
            }

        } else if (candidateEvent.getVoteType() == CandidateEventConstant.DISAGREE_VOTE) {
            if (!currentRejectVotes.contains(senderWrapper)) {
                currentRejectVotes.add(senderWrapper);
            }

            if (currentRejectVotes.size() > (currentCommittees.size() - voteThreshold)) {
                setFinish();
                processCode = CandidateEventConstant.DISAGREE_FINISH;
                msg = String.format("node [%s] process has be reject!", Hex.toHexString(candidateEvent.getId()));
            }
        } else {
            if (senderWrapper.equals(this.currentAgreeVotes.get(0))) {
                setFinish();
                processCode = CandidateEventConstant.REVOKE_FINISH;
                msg = String.format("node [%s] has be revoke!", candidate);
            } else {
                processCode = CandidateEventConstant.VOTE_FAILED;
                msg = String.format("sender [%s] not a revoker!", senderWrapper);
            }
        }


        return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(processCode), msg);
    }

    @Override
    public CandidateStateSnapshot exportCurrentSnapshot() {
        return new NodeCandidateStateSnapshot(this.currentAgreeVotes, this.currentRejectVotes, this.currentCandidate);
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
