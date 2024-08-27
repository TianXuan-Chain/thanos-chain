package com.thanos.chain.ca.manager;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.contract.ca.manager.CandidateEventConstant;
import com.thanos.chain.contract.ca.manager.StateManager;
import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.VoteNodeBlackListCandidateEvent;
import com.thanos.chain.ledger.model.event.ca.VoteNodeCandidateEvent;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * GlobalNodeBlackListStateManagerTest.java description：
 *
 * @Author laiyiyu create on 2021-05-12 19:25:39
 */
public class GlobalNodeBlackListStateManagerTest extends StateManagerBase {

    @Test
    public void testRegisterAndCancelSuccess() {
        byte[] proposalId = HashUtil.randomPeerId();
        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        EpochState epochState = createEmptyUserSystemContractEpoch();
        SecureKey randomAddr = SecureKey.getInstance("ECDSA", 1);

        byte[] nodeId = epochState.getValidatorVerifier().getOrderedPublishKeys().get(0);
        byte[] caHash = epochState.getValidatorVerifier().getPk2ValidatorInfo().get(new ByteArrayWrapper(epochState.getValidatorVerifier().getOrderedPublishKeys().get(0))).getCaHash().getBytes();

        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, nodeId, caHash));
        stateManager1.execute(globalNodeEvent1);

        StateManager stateManager2_1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_1 = create(KEY2.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS , proposalId, nodeId, caHash));
        stateManager2_1.execute(globalNodeEvent2_1);

        StateManager stateManager2_2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_2 = create(KEY2.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, nodeId, HashUtil.randomHash()));
        GlobalNodeEventReceipt receipt2_2 = stateManager2_2.execute(globalNodeEvent2_2);
        Assert.assertTrue("the VoteNodeBlackListCandidateEvent is not same with the current state candidate", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_2.getExecutionResult()));

        StateManager stateManager2_3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_3 = create(randomAddr.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, nodeId, HashUtil.randomHash()));
        GlobalNodeEventReceipt receipt2_3 = stateManager2_3.execute(globalNodeEvent2_3);
        Assert.assertTrue("the sender is not the committee", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_3.getExecutionResult()));

        StateManager stateManager2_4 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_4 = create(randomAddr.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, nodeId, caHash));
        GlobalNodeEventReceipt receipt2_4 = stateManager2_4.execute(globalNodeEvent2_4);
        Assert.assertTrue("the sender is not the committee", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_4.getExecutionResult()));

        StateManager stateManager2_5 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_5 = create(KEY1.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, HashUtil.randomHash(), "name01", "agency02", "cahash01", 1, 1));
        GlobalNodeEventReceipt receipt2_5 = stateManager2_5.execute(globalNodeEvent2_5);
        Assert.assertTrue("the VoteNodeCandidateEvent is not same with the current state Event ", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_5.getExecutionResult()));


        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY3.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, nodeId, caHash));
        GlobalNodeEventReceipt globalNodeEventReceipt3 = stateManager3.execute(globalNodeEvent3);

        Assert.assertTrue(CandidateEventConstant.AGREE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt3.getExecutionResult()));
        Assert.assertTrue(!epochState.getValidatorVerifier().containPublicKey(new ByteArrayWrapper(nodeId)));
        Assert.assertTrue(epochState.getGlobalEventState().getNodeBlackList().contains(new String(caHash)));


        StateManager stateManager4 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent4 = create(KEY1.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, caHash));
        stateManager4.execute(globalNodeEvent4);

        StateManager stateManager5 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent5 = create(KEY2.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, caHash));
        stateManager5.execute(globalNodeEvent5);

        StateManager stateManager6 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent6 = create(KEY3.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, caHash));
        GlobalNodeEventReceipt globalNodeEventReceipt6 = stateManager6.execute(globalNodeEvent6);
        Assert.assertTrue(CandidateEventConstant.AGREE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt6.getExecutionResult()));
        Assert.assertTrue(!epochState.getGlobalEventState().getNodeBlackList().contains(new String(caHash)));

//        List<ByteArrayWrapper> newCommitteeAddrs = Arrays.asList(new ByteArrayWrapper(KEY1.getAddress()), new ByteArrayWrapper(KEY2.getAddress()), new ByteArrayWrapper(KEY3.getAddress()), new ByteArrayWrapper(newCommittee.getAddress()));
//        Assert.assertTrue(newCommitteeAddrs.equals(epochState.getGlobalEventState().getCommitteeAddrs()));

        epochState.reEncode(2);

        Assert.assertTrue(epochState.getGlobalEventState().getCandidateStateSnapshot().getCurrentCommand().getCode() == GlobalEventCommand.PLACEHOLDER_EMPTY.getCode());

        EpochState newEpoch = new EpochState(epochState.getEncoded());
        Assert.assertTrue(newEpoch.getGlobalEventState().equals(epochState.getGlobalEventState()));
    }



    @Test
    public void testRegisterDisagree() {
        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        EpochState epochState = createEmptyUserSystemContractEpoch();
        byte[] proposalId = HashUtil.randomPeerId();

        byte[] caHash = epochState.getValidatorVerifier().getPk2ValidatorInfo().get(new ByteArrayWrapper(epochState.getValidatorVerifier().getOrderedPublishKeys().get(0))).getCaHash().getBytes();
        byte[] nodeId = epochState.getValidatorVerifier().getOrderedPublishKeys().get(0);

        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, caHash));
        stateManager1.execute(globalNodeEvent1);

        StateManager stateManager2_1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_1 = create(KEY2.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, caHash));
        stateManager2_1.execute(globalNodeEvent2_1);

        StateManager stateManager2_2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_2 = create(KEY2.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, HashUtil.randomHash()));
        GlobalNodeEventReceipt receipt2_2 = stateManager2_2.execute(globalNodeEvent2_2);
        Assert.assertTrue(CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_2.getExecutionResult()));


        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY3.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.DISAGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, caHash));
        GlobalNodeEventReceipt globalNodeEventReceipt3 = stateManager3.execute(globalNodeEvent3);
        Assert.assertTrue(CandidateEventConstant.DISAGREE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt3.getExecutionResult()));

        epochState.reEncode(2);

        Assert.assertTrue(epochState.getGlobalEventState().getCandidateStateSnapshot().getCurrentCommand().getCode() == GlobalEventCommand.PLACEHOLDER_EMPTY.getCode());

        EpochState newEpoch = new EpochState(epochState.getEncoded());
        Assert.assertTrue(newEpoch.getGlobalEventState().equals(epochState.getGlobalEventState()));
    }

    @Test
    public void testCancelRevoke() {
        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        EpochState epochState = createEmptyUserSystemContractEpoch();
        byte[] nodeId = epochState.getValidatorVerifier().getOrderedPublishKeys().get(0);
        SecureKey newCommittee = SecureKey.getInstance("ECDSA", 1);
        byte[] proposalId = HashUtil.randomPeerId();
        byte[] caHash = epochState.getValidatorVerifier().getOrderedPublishKeys().get(0);

        StateManager stateManager = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent = create(KEY1.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.DISAGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, caHash));
        GlobalNodeEventReceipt globalNodeEventReceipt = stateManager.execute(globalNodeEvent);
        Assert.assertTrue("revoke must agree_vote", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(globalNodeEventReceipt.getExecutionResult()));

        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, caHash));
        stateManager1.execute(globalNodeEvent1);

        StateManager stateManager2_1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_1 = create(KEY2.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, caHash));
        stateManager2_1.execute(globalNodeEvent2_1);

        StateManager stateManager2_2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_2 = create(KEY2.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.REVOKE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, caHash));
        GlobalNodeEventReceipt globalNodeEventReceipt2_2 = stateManager2_2.execute(globalNodeEvent2_2);
        Assert.assertTrue("KEY2 not a revoke", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(globalNodeEventReceipt2_2.getExecutionResult()));

        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY1.getPubKey(), new VoteNodeBlackListCandidateEvent(CandidateEventConstant.REVOKE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, nodeId, caHash));
        GlobalNodeEventReceipt globalNodeEventReceipt3 = stateManager3.execute(globalNodeEvent3);

        Assert.assertTrue(CandidateEventConstant.REVOKE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt3.getExecutionResult()));

        epochState.reEncode(2);

        Assert.assertTrue(epochState.getGlobalEventState().getCandidateStateSnapshot().getCurrentCommand().getCode() == GlobalEventCommand.PLACEHOLDER_EMPTY.getCode());

        EpochState newEpoch = new EpochState(epochState.getEncoded());
        Assert.assertTrue(newEpoch.getGlobalEventState().equals(epochState.getGlobalEventState()));
    }
}
