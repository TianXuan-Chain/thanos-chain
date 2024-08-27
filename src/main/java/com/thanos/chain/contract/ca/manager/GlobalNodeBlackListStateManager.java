package com.thanos.chain.contract.ca.manager;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.ValidatorVerifier;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.*;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.crypto.VerifyingKey;
import com.thanos.common.crypto.key.asymmetric.SecurePublicKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.List;
import java.util.Optional;

/**
 * CommitteeStateManager.java description：
 *
 * @Author laiyiyu create on 2021-03-08 09:52:09
 */
public class GlobalNodeBlackListStateManager extends StateManager {

    //VerifyingKey PALCEHOLDER_KEY = new VerifyingKey(SecurePublicKey.)

    int voteThreshold;

//    //当前最新版本,有新的候选人准入或者退出才自增1
//    int currentVersion;

    //当前委员会(公钥)准入投票列表
    List<ByteArrayWrapper> currentAgreeVotes;

    List<ByteArrayWrapper> currentRejectVotes;

    volatile Optional<NodeBlackListCandidate> currentCandidate;

    public GlobalNodeBlackListStateManager(EpochState epochState, GlobalStateRepositoryImpl repository) {
        super(epochState, repository);
        this.voteThreshold = currentCommittees.size() * epochState.getGlobalEventState().getThresholdMolecular() / epochState.getGlobalEventState().getThresholdDenominator() + 1;
        NodeBackListCandidateStateSnapshot nodeBackListCandidateStateSnapshot = (NodeBackListCandidateStateSnapshot) epochState.getGlobalEventState().getCandidateStateSnapshot();
        this.currentAgreeVotes = nodeBackListCandidateStateSnapshot.getCurrentAgreeVotes();
        this.currentRejectVotes = nodeBackListCandidateStateSnapshot.getCurrentRejectVotes();
        this.currentCandidate = nodeBackListCandidateStateSnapshot.getCurrentCandidate();
    }

    @Override
    public GlobalNodeEventReceipt doExecute(GlobalNodeEvent nodeEvent) {
        VoteNodeBlackListCandidateEvent candidateEvent = (VoteNodeBlackListCandidateEvent) nodeEvent.getCommandEvent();
        NodeBlackListCandidate candidate = NodeBlackListCandidate.convertFrom(candidateEvent);

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
                    ValidatorPublicKeyInfo validatorPublicKeyInfo = epochState.getGlobalEventState().getValidatorVerifier().getKeyByCaHash(new String(candidate.getCaHash()));
                    if (validatorPublicKeyInfo == null) {
                        // the node is not a validator node
                        byte[] publicKey = ByteUtil.copyFrom(candidate.getPublicKey());
                        validatorPublicKeyInfo = new ValidatorPublicKeyInfo(publicKey, 0, 0, new VerifyingKey(publicKey), "placeholder", "placeholder", new String(candidate.getCaHash()));
                    } else {
                        ValidatorVerifier newValidator = epochState.getGlobalEventState().getValidatorVerifier().removeOldValidator(candidate.getCaHash());
                        epochState.getGlobalEventState().resetValidatorVerifier(newValidator);
                    }

                    epochState.getGlobalEventState().addBlackNode(validatorPublicKeyInfo);
                } else {
                    epochState.getGlobalEventState().removeBlackNode(new String(candidate.getCaHash()));

                }
                setFinish();
                processCode = CandidateEventConstant.AGREE_FINISH;
                msg = String.format("process black node [%s] success!", Hex.toHexString(candidate.getCaHash()));
            }

        } else if (candidateEvent.getVoteType() == CandidateEventConstant.DISAGREE_VOTE) {
            if (!currentRejectVotes.contains(senderWrapper)) {
                currentRejectVotes.add(senderWrapper);
            }

            if (currentRejectVotes.size() > (currentCommittees.size() - voteThreshold)) {
                setFinish();
                processCode = CandidateEventConstant.DISAGREE_FINISH;
                msg = String.format("block node [%s] process has be reject!", Hex.toHexString(candidate.getCaHash()));
            }
        } else {
            if (senderWrapper.equals(this.currentAgreeVotes.get(0))) {
                setFinish();
                processCode = CandidateEventConstant.REVOKE_FINISH;
                msg = String.format("block node [%s] has be revoke!", candidate);
            } else {
                processCode = CandidateEventConstant.VOTE_FAILED;
                msg = String.format("sender [%s] not a revoker!", senderWrapper);
            }
        }


        return new GlobalNodeEventReceipt(nodeEvent, ByteUtil.intToBytes(processCode), msg);
    }

    @Override
    public CandidateStateSnapshot exportCurrentSnapshot() {
        return new NodeBackListCandidateStateSnapshot(this.currentAgreeVotes, this.currentRejectVotes, this.currentCandidate);
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
