package com.thanos.model.ca;

import com.thanos.chain.consensus.hotstuffbft.model.ValidatorVerifier;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import com.thanos.chain.ledger.model.event.GlobalEventState;
import com.thanos.chain.ledger.model.event.ca.CaContractCode;
import com.thanos.chain.ledger.model.event.ca.CommitteeCandidate;
import com.thanos.chain.ledger.model.event.ca.CommitteeCandidateStateSnapshot;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.HashUtil;
import com.thanos.model.common.ValidatorVerifierBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * GlobalEventStateTest.java description：
 *
 * @Author laiyiyu create on 2021-04-09 16:44:56
 */
public class GlobalEventStateTest {

    @Test
    public void fullContentWithOneFilter() {
        CommitteeCandidateStateSnapshot stateSnapshot = CommitteeCandidateStateSnapshotTest.buildFullContent();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()));
        List<ByteArrayWrapper> opStaffAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()));

        CaContractCode caContractCode = CaContractCodeBuilder.build();
        List<ByteArrayWrapper> filterAddrs = Arrays.asList(new ByteArrayWrapper(caContractCode.getCodeAddress()));


        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        contractCodeMap.put(filterAddrs.get(0), caContractCode);

        List<ValidatorPublicKeyInfo> nodeBlackList = validatorVerifier.exportPKInfos();

        GlobalEventState globalEventState1 = new GlobalEventState(true, "2/3", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, nodeBlackList);
        System.out.println(globalEventState1);
        GlobalEventState globalEventState2 = new GlobalEventState(globalEventState1.getEncoded());
        System.out.println(globalEventState2);
        Assert.assertTrue(globalEventState1.equals(globalEventState2));

    }

    @Test
    public void withoutNodeBlackList() {
        CommitteeCandidateStateSnapshot stateSnapshot = CommitteeCandidateStateSnapshotTest.buildFullContent();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()));
        List<ByteArrayWrapper> opStaffAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()));

        CaContractCode caContractCode = CaContractCodeBuilder.build();
        List<ByteArrayWrapper> filterAddrs = Arrays.asList(new ByteArrayWrapper(caContractCode.getCodeAddress()));


        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        contractCodeMap.put(filterAddrs.get(0), caContractCode);


        GlobalEventState globalEventState1 = new GlobalEventState(true, "2/3", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, new ArrayList<>());
        System.out.println(globalEventState1);
        GlobalEventState globalEventState2 = new GlobalEventState(globalEventState1.getEncoded());
        System.out.println(globalEventState2);
        Assert.assertTrue(globalEventState1.equals(globalEventState2));

    }

    @Test
    public void fullContentWithTwoFilter() {
        CommitteeCandidateStateSnapshot stateSnapshot = CommitteeCandidateStateSnapshotTest.buildFullContent();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()));
        List<ByteArrayWrapper> opStaffAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()));


        CaContractCode caContractCode1 = CaContractCodeBuilder.build();
        CaContractCode caContractCode2 = CaContractCodeBuilder.buildTest();
        List<ByteArrayWrapper> filterAddrs = Arrays.asList(new ByteArrayWrapper(caContractCode1.getCodeAddress()), new ByteArrayWrapper(caContractCode2.getCodeAddress()));


        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        contractCodeMap.put(filterAddrs.get(0), caContractCode1);
        contractCodeMap.put(filterAddrs.get(1), caContractCode2);

        List<ValidatorPublicKeyInfo> nodeBlackList = validatorVerifier.exportPKInfos();


        GlobalEventState globalEventState1 = new GlobalEventState(true, "2/3", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, nodeBlackList);
        System.out.println(globalEventState1);
        GlobalEventState globalEventState2 = new GlobalEventState(globalEventState1.getEncoded());
        System.out.println(globalEventState2);
        Assert.assertTrue(globalEventState1.equals(globalEventState2));

    }


    @Test
    public void withoutCommittees() {
        CommitteeCandidateStateSnapshot stateSnapshot = CommitteeCandidateStateSnapshotTest.buildFullContent();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = new ArrayList<>();

        List<ByteArrayWrapper> opStaffAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()));

        CaContractCode caContractCode = CaContractCodeBuilder.buildTest();
        List<ByteArrayWrapper> filterAddrs = Arrays.asList(new ByteArrayWrapper(caContractCode.getCodeAddress()));


        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        contractCodeMap.put(filterAddrs.get(0), caContractCode);
        List<ValidatorPublicKeyInfo> nodeBlackList = validatorVerifier.exportPKInfos();


        GlobalEventState globalEventState1 = new GlobalEventState(false, "2/3", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, nodeBlackList);
        System.out.println(globalEventState1);
        GlobalEventState globalEventState2 = new GlobalEventState(globalEventState1.getEncoded());
        System.out.println(globalEventState2);
        Assert.assertTrue(globalEventState1.equals(globalEventState2));
    }

    @Test
    public void withoutFilters() {
        CommitteeCandidateStateSnapshot stateSnapshot = CommitteeCandidateStateSnapshotTest.buildFullContent();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()));
        List<ByteArrayWrapper> opStaffAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()));


        List<ByteArrayWrapper> filterAddrs = new ArrayList<>();

        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        List<ValidatorPublicKeyInfo> nodeBlackList = validatorVerifier.exportPKInfos();


        GlobalEventState globalEventState1 = new GlobalEventState(true, "1/2", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, nodeBlackList);
        System.out.println(globalEventState1);
        GlobalEventState globalEventState2 = new GlobalEventState(globalEventState1.getEncoded());
        System.out.println(globalEventState2);
        Assert.assertTrue(globalEventState1.equals(globalEventState2));
    }

    @Test
    public void withoutOpStaffs() {
        CommitteeCandidateStateSnapshot stateSnapshot = CommitteeCandidateStateSnapshotTest.buildFullContent();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()));
        List<ByteArrayWrapper> opStaffAddrs = new ArrayList<>();

        CaContractCode caContractCode = CaContractCodeBuilder.buildTest();
        List<ByteArrayWrapper> filterAddrs = Arrays.asList(new ByteArrayWrapper(caContractCode.getCodeAddress()));


        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        contractCodeMap.put(filterAddrs.get(0), caContractCode);
        List<ValidatorPublicKeyInfo> nodeBlackList = validatorVerifier.exportPKInfos();

        GlobalEventState globalEventState1 = new GlobalEventState(false, "2/3", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, nodeBlackList);
        System.out.println(globalEventState1);
        GlobalEventState globalEventState2 = new GlobalEventState(globalEventState1.getEncoded());
        System.out.println(globalEventState2);
        Assert.assertTrue(globalEventState1.equals(globalEventState2));
    }

    @Test
    public void withoutCommitteesAndFilters() {
        CommitteeCandidateStateSnapshot stateSnapshot = CommitteeCandidateStateSnapshotTest.buildFullContent();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = new ArrayList<>();
        List<ByteArrayWrapper> opStaffAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()));
        List<ByteArrayWrapper> filterAddrs = new ArrayList<>();

        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        List<ValidatorPublicKeyInfo> nodeBlackList = validatorVerifier.exportPKInfos();

        GlobalEventState globalEventState1 = new GlobalEventState(true, "1/2", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, nodeBlackList);
        System.out.println(globalEventState1);
        GlobalEventState globalEventState2 = new GlobalEventState(globalEventState1.getEncoded());
        System.out.println(globalEventState2);
        Assert.assertTrue(globalEventState1.equals(globalEventState2));
    }

    @Test
    public void withoutCommitteesAndOpStaffs() {
        CommitteeCandidateStateSnapshot stateSnapshot = CommitteeCandidateStateSnapshotTest.buildFullContent();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = new ArrayList<>();
        List<ByteArrayWrapper> opStaffAddrs = new ArrayList<>();

        CaContractCode caContractCode = CaContractCodeBuilder.buildTest();
        List<ByteArrayWrapper> filterAddrs = Arrays.asList(new ByteArrayWrapper(caContractCode.getCodeAddress()));


        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        contractCodeMap.put(filterAddrs.get(0), caContractCode);
        List<ValidatorPublicKeyInfo> nodeBlackList = validatorVerifier.exportPKInfos();

        GlobalEventState globalEventState1 = new GlobalEventState(false, "2/3", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, nodeBlackList);
        System.out.println(globalEventState1);
        GlobalEventState globalEventState2 = new GlobalEventState(globalEventState1.getEncoded());
        System.out.println(globalEventState2);
        Assert.assertTrue(globalEventState1.equals(globalEventState2));
    }


    @Test
    public void withoutOpStaffsAndFilters() {
        CommitteeCandidateStateSnapshot stateSnapshot = CommitteeCandidateStateSnapshotTest.buildFullContent();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()), new ByteArrayWrapper(HashUtil.randomHash()));
        List<ByteArrayWrapper> opStaffAddrs = new ArrayList<>();
        List<ByteArrayWrapper> filterAddrs = new ArrayList<>();

        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        List<ValidatorPublicKeyInfo> nodeBlackList = validatorVerifier.exportPKInfos();

        GlobalEventState globalEventState1 = new GlobalEventState(true, "1/2", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, nodeBlackList);
        System.out.println(globalEventState1);
        GlobalEventState globalEventState2 = new GlobalEventState(globalEventState1.getEncoded());
        System.out.println(globalEventState2);
        Assert.assertTrue(globalEventState1.equals(globalEventState2));
    }

    @Test
    public void withoutCommitteesAndOpStaffsAndFilters() {
        CommitteeCandidateStateSnapshot stateSnapshot = CommitteeCandidateStateSnapshotTest.buildFullContent();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = new ArrayList<>();
        List<ByteArrayWrapper> opStaffAddrs = new ArrayList<>();
        List<ByteArrayWrapper> filterAddrs = new ArrayList<>();

        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        List<ValidatorPublicKeyInfo> nodeBlackList = validatorVerifier.exportPKInfos();

        GlobalEventState globalEventState1 = new GlobalEventState(true, "1/2", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, nodeBlackList);
        System.out.println(globalEventState1);
        GlobalEventState globalEventState2 = new GlobalEventState(globalEventState1.getEncoded());
        System.out.println(globalEventState2);
        Assert.assertTrue(globalEventState1.equals(globalEventState2));
    }


}
