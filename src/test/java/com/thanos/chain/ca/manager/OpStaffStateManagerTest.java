package com.thanos.chain.ca.manager;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.contract.ca.manager.CandidateEventConstant;
import com.thanos.chain.contract.ca.manager.StateManager;
import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.ProcessOperationsStaffCandidateEvent;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * OpStaffStateManagerTest.java description：
 *
 * @Author laiyiyu create on 2021-04-30 16:41:05
 */
public class OpStaffStateManagerTest extends StateManagerBase {
    
    @Test
    public void testAll() {
        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        EpochState epochState = createEmptyUserSystemContractEpoch();
        SecureKey newOperationsStaff = SecureKey.getInstance("ECDSA", 1);
        List<ByteArrayWrapper> currentOperationsStaffs = new ArrayList<>(Arrays.asList(OP_STAFF, new ByteArrayWrapper(newOperationsStaff.getAddress())));

        StateManager stateManager1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent1 = create(KEY1.getPubKey(), new ProcessOperationsStaffCandidateEvent(CandidateEventConstant.REGISTER_PROCESS, newOperationsStaff.getAddress()));
        GlobalNodeEventReceipt receipt1 = stateManager1.execute(globalNodeEvent1);
        Assert.assertTrue(CandidateEventConstant.AGREE_FINISH == ByteUtil.byteArrayToInt(receipt1.getExecutionResult()));
        Assert.assertTrue("add opStaff success", currentOperationsStaffs.equals(epochState.getGlobalEventState().getOperationsStaffAddrs()));
        Assert.assertTrue("add opStaff success", epochState.getGlobalEventState().getOperationsStaffAddrSet().contains(new ByteArrayWrapper(newOperationsStaff.getAddress())));

        StateManager stateManager2_1 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_1 = create(newOperationsStaff.getPubKey(), new ProcessOperationsStaffCandidateEvent(CandidateEventConstant.REGISTER_PROCESS, HashUtil.randomHash()));
        GlobalNodeEventReceipt receipt2_1 = stateManager2_1.execute(globalNodeEvent2_1);
        Assert.assertTrue("the sender is not the committee", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_1.getExecutionResult()));

        StateManager stateManager2_2 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent2_2 = create(newOperationsStaff.getPubKey(), new ProcessOperationsStaffCandidateEvent(CandidateEventConstant.REGISTER_PROCESS, newOperationsStaff.getAddress()));
        GlobalNodeEventReceipt receipt2_2 = stateManager2_2.execute(globalNodeEvent2_2);
        Assert.assertTrue("the sender address is not the same", CandidateEventConstant.VOTE_FAILED == ByteUtil.byteArrayToInt(receipt2_2.getExecutionResult()));

        StateManager stateManager3 = StateManager.buildManager(epochState, globalStateRepository);
        GlobalNodeEvent globalNodeEvent3 = create(KEY1.getPubKey(), new ProcessOperationsStaffCandidateEvent(CandidateEventConstant.CANCEL_PROCESS, OP_STAFF.getData()));
        GlobalNodeEventReceipt receipt3 = stateManager3.execute(globalNodeEvent3);
        Assert.assertTrue(CandidateEventConstant.AGREE_FINISH == ByteUtil.byteArrayToInt(receipt3.getExecutionResult()));
        currentOperationsStaffs.remove(OP_STAFF);
        Assert.assertTrue("remove opStaff success", currentOperationsStaffs.equals(epochState.getGlobalEventState().getOperationsStaffAddrs()));
        Assert.assertTrue("remove opStaff success", !epochState.getGlobalEventState().getOperationsStaffAddrSet().contains(OP_STAFF));

        epochState.reEncode(2);
        Assert.assertTrue(epochState.getGlobalEventState().getCandidateStateSnapshot().getCurrentCommand().getCode() == GlobalEventCommand.PLACEHOLDER_EMPTY.getCode());

        EpochState newEpoch = new EpochState(epochState.getEncoded());
        Assert.assertTrue(newEpoch.getGlobalEventState().equals(epochState.getGlobalEventState()));
    }
}
