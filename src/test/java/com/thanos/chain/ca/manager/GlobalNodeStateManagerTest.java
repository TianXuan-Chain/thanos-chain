package com.thanos.chain.ca.manager;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.contract.ca.manager.CandidateEventConstant;
import com.thanos.chain.contract.ca.manager.StateManager;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.*;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.crypto.VerifyingKey;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * GlobalNodeStateManagerTest.java description：
 *
 * @Author laiyiyu create on 2021-04-29 14:43:32
 */
public class GlobalNodeStateManagerTest extends StateManagerBase {


    @Test
    public void testRegisterSuccess() {
        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        EpochState epochState = createEmptyUserSystemContractEpoch();
        byte[] proposalId = HashUtil.randomPeerId();

        SecureKey newNodePk = SecureKey.getInstance("ECDSA", 1);
        ValidatorPublicKeyInfo newVPKI = new ValidatorPublicKeyInfo(newNodePk.getPubKey(), 1, 1, new VerifyingKey(newNodePk.getPubKey()), "name01", "agency01", "ca011");

        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency01", "ca011", 1, 1));
        stateManager1.execute(globalNodeEvent1);

        StateManager stateManager2_1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_1 = create(KEY2.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency01", "ca011", 1, 1));
        stateManager2_1.execute(globalNodeEvent2_1);

        StateManager stateManager2_2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_2 = create(KEY2.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency02", "ca011", 1, 1));
        GlobalNodeEventReceipt receipt2_2 = stateManager2_2.execute(globalNodeEvent2_2);
        Assert.assertTrue("the VoteNodeCandidate is not same with the current state candidate", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_2.getExecutionResult()));

        StateManager stateManager2_3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_3 = create(newNodePk.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency02", "ca011", 1, 1));
        GlobalNodeEventReceipt receipt2_3 = stateManager2_3.execute(globalNodeEvent2_3);
        Assert.assertTrue("the sender is not the committee", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_3.getExecutionResult()));

        StateManager stateManager2_4 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_4 = create(newNodePk.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency02", "ca011", 1, 1));
        GlobalNodeEventReceipt receipt2_4 = stateManager2_4.execute(globalNodeEvent2_4);
        Assert.assertTrue("the sender is not the committee", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_4.getExecutionResult()));

        StateManager stateManager2_5 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_5 = create(KEY1.getPubKey(), new VoteCommitteeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, HashUtil.randomHash()));
        GlobalNodeEventReceipt receipt2_5 = stateManager2_5.execute(globalNodeEvent2_5);
        Assert.assertTrue("the VoteNodeCandidateEvent is not same with the current state Event ", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_5.getExecutionResult()));


        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY3.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency01", "ca011", 1, 1));
        GlobalNodeEventReceipt globalNodeEventReceipt = stateManager3.execute(globalNodeEvent3);

        Assert.assertTrue(CandidateEventConstant.AGREE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt.getExecutionResult()));

        Assert.assertTrue(newVPKI.equals(epochState.getGlobalEventState().getValidatorVerifier().getPk2ValidatorInfo().get(new ByteArrayWrapper(newNodePk.getPubKey()))));

        epochState.reEncode(2);

        Assert.assertTrue(epochState.getGlobalEventState().getCandidateStateSnapshot().getCurrentCommand().getCode() == GlobalEventCommand.PLACEHOLDER_EMPTY.getCode());

        EpochState newEpoch = new EpochState(epochState.getEncoded());
        Assert.assertTrue(newEpoch.getGlobalEventState().equals(epochState.getGlobalEventState()));
    }


    @Test
    public void testCANCELSuccess() {
        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        EpochState epochState = createEmptyUserSystemContractEpoch();
        byte[] proposalId = HashUtil.randomPeerId();


        List<ValidatorPublicKeyInfo> validatorPublicKeyInfos = epochState.getValidatorVerifier().exportPKInfos();
        ValidatorPublicKeyInfo cancelNode = validatorPublicKeyInfos.remove(0);

        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, cancelNode.getAccountAddress(), cancelNode.getName(), cancelNode.getAgency(), cancelNode.getCaHash(), cancelNode.getConsensusVotingPower(), cancelNode.getShardingNum()));
        stateManager1.execute(globalNodeEvent1);

        StateManager stateManager2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2 = create(KEY2.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, cancelNode.getAccountAddress(), cancelNode.getName(), cancelNode.getAgency(), cancelNode.getCaHash(), cancelNode.getConsensusVotingPower(), cancelNode.getShardingNum()));
        stateManager2.execute(globalNodeEvent2);

        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY3.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, cancelNode.getAccountAddress(), cancelNode.getName(), cancelNode.getAgency(), cancelNode.getCaHash(), cancelNode.getConsensusVotingPower(), cancelNode.getShardingNum()));
        GlobalNodeEventReceipt globalNodeEventReceipt = stateManager3.execute(globalNodeEvent3);

        Assert.assertTrue(CandidateEventConstant.AGREE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt.getExecutionResult()));

        Assert.assertTrue(validatorPublicKeyInfos.equals(epochState.getGlobalEventState().getValidatorVerifier().exportPKInfos()));

        epochState.reEncode(2);

        Assert.assertTrue(epochState.getGlobalEventState().getCandidateStateSnapshot().getCurrentCommand().getCode() == GlobalEventCommand.PLACEHOLDER_EMPTY.getCode());

        EpochState newEpoch = new EpochState(epochState.getEncoded());
        Assert.assertTrue(newEpoch.getGlobalEventState().equals(epochState.getGlobalEventState()));
    }


    @Test
    public void testRegisterDisagree() {
        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        EpochState epochState = createEmptyUserSystemContractEpoch();
        SecureKey newNodePk = SecureKey.getInstance("ECDSA", 1);
        byte[] proposalId = HashUtil.randomPeerId();


        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency01", "ca011", 1, 1));
        stateManager1.execute(globalNodeEvent1);

        StateManager stateManager2_1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_1 = create(KEY2.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency01", "ca011", 1, 1));
        stateManager2_1.execute(globalNodeEvent2_1);

        StateManager stateManager2_2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_2 = create(KEY2.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency02", "ca011", 1, 1));
        GlobalNodeEventReceipt receipt2_2 = stateManager2_2.execute(globalNodeEvent2_2);
        Assert.assertTrue("the VoteNodeCandidate is not same with the current state candidate", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_2.getExecutionResult()));


        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY3.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.DISAGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency01", "ca011", 1, 1));
        GlobalNodeEventReceipt globalNodeEventReceipt3 = stateManager3.execute(globalNodeEvent3);

        Assert.assertTrue(CandidateEventConstant.DISAGREE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt3.getExecutionResult()));

        Assert.assertTrue(null == epochState.getGlobalEventState().getValidatorVerifier().getPk2ValidatorInfo().get(new ByteArrayWrapper(newNodePk.getPubKey())));

        epochState.reEncode(2);

        Assert.assertTrue(epochState.getGlobalEventState().getCandidateStateSnapshot().getCurrentCommand().getCode() == GlobalEventCommand.PLACEHOLDER_EMPTY.getCode());

        EpochState newEpoch = new EpochState(epochState.getEncoded());
        Assert.assertTrue(newEpoch.getGlobalEventState().equals(epochState.getGlobalEventState()));

    }

    @Test
    public void testResisterDisagree() {
        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        EpochState epochState = createEmptyUserSystemContractEpoch();
        SecureKey newNodePk = SecureKey.getInstance("ECDSA", 1);
        byte[] proposalId = HashUtil.randomPeerId();


        StateManager stateManager = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent = create(KEY1.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.DISAGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency01", "ca011", 1, 1));
        GlobalNodeEventReceipt globalNodeEventReceipt = stateManager.execute(globalNodeEvent);
        Assert.assertTrue("revoke must agree_vote", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(globalNodeEventReceipt.getExecutionResult()));


        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency01", "ca011", 1, 1));
        stateManager1.execute(globalNodeEvent1);

        StateManager stateManager2_1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_1 = create(KEY2.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency01", "ca011", 1, 1));
        stateManager2_1.execute(globalNodeEvent2_1);

        StateManager stateManager2_2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_2 = create(KEY2.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.REVOKE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency02", "ca011", 1, 1));
        GlobalNodeEventReceipt receipt2_2 = stateManager2_2.execute(globalNodeEvent2_2);
        Assert.assertTrue("KEY2 not a revoke", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_2.getExecutionResult()));


        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY1.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.REVOKE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newNodePk.getPubKey(), "name01", "agency01", "ca011", 1, 1));
        GlobalNodeEventReceipt globalNodeEventReceipt3 = stateManager3.execute(globalNodeEvent3);

        Assert.assertTrue(CandidateEventConstant.REVOKE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt3.getExecutionResult()));

        Assert.assertTrue(null == epochState.getGlobalEventState().getValidatorVerifier().getPk2ValidatorInfo().get(new ByteArrayWrapper(newNodePk.getPubKey())));

        epochState.reEncode(2);

        Assert.assertTrue(epochState.getGlobalEventState().getCandidateStateSnapshot().getCurrentCommand().getCode() == GlobalEventCommand.PLACEHOLDER_EMPTY.getCode());

        EpochState newEpoch = new EpochState(epochState.getEncoded());
        Assert.assertTrue(newEpoch.getGlobalEventState().equals(epochState.getGlobalEventState()));

    }
}
