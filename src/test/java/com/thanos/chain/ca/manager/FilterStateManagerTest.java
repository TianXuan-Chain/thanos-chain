package com.thanos.chain.ca.manager;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.contract.ca.manager.CandidateEventConstant;
import com.thanos.chain.contract.ca.manager.StateManager;
import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.CaContractCode;
import com.thanos.chain.ledger.model.event.ca.VoteFilterCandidateEvent;
import com.thanos.chain.ledger.model.event.ca.VoteNodeCandidateEvent;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.model.ca.CaContractCodeBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * FilterStateManagerTest.java description：
 *
 * @Author laiyiyu create on 2021-04-30 14:29:35
 */
public class FilterStateManagerTest extends StateManagerBase {


    @Test
    public void testRegisterSuccess() {
        byte[] proposalId = HashUtil.randomPeerId();
        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        EpochState epochState = createEmptyUserSystemContractEpoch();
        SecureKey newCommittee = SecureKey.getInstance("ECDSA", 1);

        StateManager stateManager0 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent0 = create(KEY1.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, CaContractCodeBuilder.buildTest()));
        GlobalNodeEventReceipt receipt0 = stateManager0.execute(globalNodeEvent0);
        Assert.assertTrue("compile error", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt0.getExecutionResult()));
        Assert.assertTrue(epochState.getGlobalEventState().getCode() == GlobalEventCommand.VOTE_FILTER_CANDIDATE.getCode());


        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        stateManager1.execute(globalNodeEvent1);

        StateManager stateManager2_1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_1 = create(KEY2.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        stateManager2_1.execute(globalNodeEvent2_1);

        StateManager stateManager2_2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_2 = create(KEY2.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, CaContractCodeBuilder.build()));
        GlobalNodeEventReceipt receipt2_2 = stateManager2_2.execute(globalNodeEvent2_2);
        Assert.assertTrue("the VoteFilterCandidate is not same with the current state candidate", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_2.getExecutionResult()));

        StateManager stateManager2_3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_3 = create(newCommittee.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        GlobalNodeEventReceipt receipt2_3 = stateManager2_3.execute(globalNodeEvent2_3);
        Assert.assertTrue("the sender is not the committee", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_3.getExecutionResult()));

        StateManager stateManager2_4 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_4 = create(newCommittee.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        GlobalNodeEventReceipt receipt2_4 = stateManager2_4.execute(globalNodeEvent2_4);
        Assert.assertTrue("the sender is not the committee", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_4.getExecutionResult()));

        StateManager stateManager2_5 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_5 = create(KEY1.getPubKey(), new VoteNodeCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, newCommittee.getPubKey(), "name01", "agency02", "cahash01", 1, 1));
        GlobalNodeEventReceipt receipt2_5 = stateManager2_5.execute(globalNodeEvent2_5);
        Assert.assertTrue("the VoteNodeCandidateEvent is not same with the current state Event ", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_5.getExecutionResult()));


        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY3.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.REGISTER_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        GlobalNodeEventReceipt globalNodeEventReceipt = stateManager3.execute(globalNodeEvent3);
        Assert.assertTrue(CandidateEventConstant.AGREE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt.getExecutionResult()));


        CaContractCode newCode = CaContractCodeBuilder.buildTest2();
        Assert.assertTrue(epochState.getGlobalEventState().getFilterAddrs().contains(new ByteArrayWrapper(newCode.getCodeAddress())));
        Assert.assertTrue(newCode.equals(epochState.getGlobalEventState().getCaContractCode().get(new ByteArrayWrapper(newCode.getCodeAddress()))));

        epochState.reEncode(2);

        Assert.assertTrue(epochState.getGlobalEventState().getCandidateStateSnapshot().getCurrentCommand().getCode() == GlobalEventCommand.PLACEHOLDER_EMPTY.getCode());

        EpochState newEpoch = new EpochState(epochState.getEncoded());
        Assert.assertTrue(newEpoch.getGlobalEventState().equals(epochState.getGlobalEventState()));
    }


    @Test
    public void testCANCELSuccess() {
        byte[] proposalId = HashUtil.randomPeerId();

        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        EpochState epochState = createEmptyUserSystemContractEpoch();

        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.build()));
        stateManager1.execute(globalNodeEvent1);

        StateManager stateManager2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2 = create(KEY2.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.build()));
        stateManager2.execute(globalNodeEvent2);

        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY3.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.build()));
        GlobalNodeEventReceipt globalNodeEventReceipt = stateManager3.execute(globalNodeEvent3);

        Assert.assertTrue(CandidateEventConstant.AGREE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt.getExecutionResult()));

        CaContractCode removeCode = CaContractCodeBuilder.build();
        Assert.assertTrue(epochState.getGlobalEventState().getFilterAddrs().isEmpty());
        Assert.assertTrue(epochState.getGlobalEventState().getCaContractCode().get(new ByteArrayWrapper(removeCode.getCodeAddress())).valueBytes() == null);

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


        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        stateManager1.execute(globalNodeEvent1);

        StateManager stateManager2_1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_1 = create(KEY2.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        stateManager2_1.execute(globalNodeEvent2_1);

        StateManager stateManager2_2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_2 = create(KEY2.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.build()));
        GlobalNodeEventReceipt receipt2_2 = stateManager2_2.execute(globalNodeEvent2_2);
        Assert.assertTrue(CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_2.getExecutionResult()));


        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY3.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.DISAGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        GlobalNodeEventReceipt globalNodeEventReceipt = stateManager3.execute(globalNodeEvent3);

        Assert.assertTrue(CandidateEventConstant.DISAGREE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt.getExecutionResult()));

        CaContractCode newCode = CaContractCodeBuilder.buildTest2();
        Assert.assertTrue(!epochState.getGlobalEventState().getFilterAddrs().contains(new ByteArrayWrapper(newCode.getCodeAddress())));
        Assert.assertTrue(epochState.getGlobalEventState().getCaContractCode().get(new ByteArrayWrapper(newCode.getCodeAddress())) == null);


        epochState.reEncode(2);

        Assert.assertTrue(epochState.getGlobalEventState().getCandidateStateSnapshot().getCurrentCommand().getCode() == GlobalEventCommand.PLACEHOLDER_EMPTY.getCode());

        EpochState newEpoch = new EpochState(epochState.getEncoded());
        Assert.assertTrue(newEpoch.getGlobalEventState().equals(epochState.getGlobalEventState()));
    }

    @Test
    public void testRegisterRevoke() {

        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        EpochState epochState = createEmptyUserSystemContractEpoch();
        byte[] proposalId = HashUtil.randomPeerId();

        StateManager stateManager = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent = create(KEY1.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.DISAGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        GlobalNodeEventReceipt globalNodeEventReceipt = stateManager.execute(globalNodeEvent);
        Assert.assertTrue("revoke must agree_vote", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(globalNodeEventReceipt.getExecutionResult()));

        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        stateManager1.execute(globalNodeEvent1);

        StateManager stateManager2_1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_1 = create(KEY2.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.AGREE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        stateManager2_1.execute(globalNodeEvent2_1);

        StateManager stateManager2_2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_2 = create(KEY2.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.REVOKE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.build()));
        GlobalNodeEventReceipt receipt2_2 = stateManager2_2.execute(globalNodeEvent2_2);
        Assert.assertTrue("KEY2 not a revoke", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_2.getExecutionResult()));


        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY1.getPubKey(), new VoteFilterCandidateEvent(CandidateEventConstant.REVOKE_VOTE, CandidateEventConstant.CANCEL_PROCESS, proposalId, CaContractCodeBuilder.buildTest2()));
        GlobalNodeEventReceipt globalNodeEventReceipt3 = stateManager3.execute(globalNodeEvent3);

        Assert.assertTrue(CandidateEventConstant.REVOKE_FINISH == ByteUtil.byteArrayToInt(globalNodeEventReceipt3.getExecutionResult()));

        CaContractCode newCode = CaContractCodeBuilder.buildTest2();
        Assert.assertTrue(!epochState.getGlobalEventState().getFilterAddrs().contains(new ByteArrayWrapper(newCode.getCodeAddress())));
        Assert.assertTrue(epochState.getGlobalEventState().getCaContractCode().get(new ByteArrayWrapper(newCode.getCodeAddress())) == null);


        epochState.reEncode(2);

        Assert.assertTrue(epochState.getGlobalEventState().getCandidateStateSnapshot().getCurrentCommand().getCode() == GlobalEventCommand.PLACEHOLDER_EMPTY.getCode());

        EpochState newEpoch = new EpochState(epochState.getEncoded());
        Assert.assertTrue(newEpoch.getGlobalEventState().equals(epochState.getGlobalEventState()));
    }
}
