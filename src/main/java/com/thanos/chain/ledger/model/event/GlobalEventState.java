package com.thanos.chain.ledger.model.event;

import com.thanos.chain.consensus.hotstuffbft.model.ValidatorVerifier;
import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import com.thanos.chain.ledger.model.event.ca.*;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.thanos.common.utils.HashUtil.CRYPTO_PROVIDER;
import static com.thanos.common.utils.HashUtil.HASH_256_ALGORITHM_NAME;

/**
 * GlobalEventState.java description：
 *
 * @Author laiyiyu create on 2021-04-06 14:28:26
 */
public class GlobalEventState extends RLPModel {

    boolean useSystemContract;

    byte code;
    
    byte[] snapshotData;

    byte[] stateRoot;

    ValidatorVerifier validatorVerifier;

    String voteThreshold;

    List<ByteArrayWrapper> committeeAddrs;
    Set<ByteArrayWrapper> committeeAddrSet;

    List<ByteArrayWrapper> operationsStaffAddrs;
    Set<ByteArrayWrapper> operationsStaffAddrSet;

    List<ByteArrayWrapper> filterAddrs;

    List<ValidatorPublicKeyInfo> nodeBlackList;

    //====================

    CandidateStateSnapshot candidateStateSnapshot;
    //====================

    //transition
    int thresholdMolecular;

    int thresholdDenominator;

    List<GlobalNodeEventReceipt> globalNodeEventReceipts;

    Map<ByteArrayWrapper, CaContractCode>  caContractCode;

    Map<ByteArrayWrapper, ByteArrayWrapper> globalEventProcessState;

    Map<Keyable, CaFinishProposalId> finishProposalIds;






    public GlobalEventState(boolean useSystemContract, String voteThreshold, byte code, byte[] snapshotData, ValidatorVerifier validatorVerifier, List<ByteArrayWrapper> committeeAddrs, List<ByteArrayWrapper> operationsStaffAddrs, List<ByteArrayWrapper> filterAddrs, Map<ByteArrayWrapper, CaContractCode> contractCodeMap, List<ValidatorPublicKeyInfo> nodeBlackList) {
        super(null);
        this.useSystemContract = useSystemContract;
        this.voteThreshold = voteThreshold;
        String[] voteThresholdArr = voteThreshold.split("/");
        thresholdMolecular = Integer.parseInt(voteThresholdArr[0]);
        thresholdDenominator = Integer.parseInt(voteThresholdArr[1]);

        this.code = code;
        this.snapshotData = snapshotData;
        this.candidateStateSnapshot = CandidateStateSnapshot.build(code, snapshotData);

        this.validatorVerifier = validatorVerifier;
        this.committeeAddrs = committeeAddrs;
        this.committeeAddrSet = new HashSet<>(committeeAddrs);
        this.operationsStaffAddrs = operationsStaffAddrs;
        this.operationsStaffAddrSet = new HashSet<>(operationsStaffAddrs);
        this.filterAddrs = filterAddrs;
        this.nodeBlackList = nodeBlackList;

        this.globalNodeEventReceipts = new ArrayList<>();
        this.caContractCode = contractCodeMap;
        this.globalEventProcessState = new HashMap<>();
        this.finishProposalIds = new HashMap<>();

        calculateStateRoot();

        this.rlpEncoded = rlpEncoded();
    }

    public GlobalEventState(byte[] encode) {
        super(encode);
        this.globalNodeEventReceipts = new ArrayList<>();
        this.caContractCode = new HashMap<>();
        this.globalEventProcessState = new HashMap<>();
        this.finishProposalIds = new HashMap<>();
    }


    public byte getCode() {
        return code;
    }


    public CandidateStateSnapshot getCandidateStateSnapshot() {
        return candidateStateSnapshot;
    }

    public void resetCandidateStateSnapshot(CandidateStateSnapshot candidateStateSnapshot) {
        this.candidateStateSnapshot = candidateStateSnapshot;
        this.code = candidateStateSnapshot.getCurrentCommand().getCode();
        this.snapshotData = candidateStateSnapshot.getEncoded();

    }

    public void resetValidatorVerifier(ValidatorVerifier validatorVerifier) {
        this.validatorVerifier = validatorVerifier;
    }

    public ValidatorVerifier getValidatorVerifier() {
        return validatorVerifier;
    }

    public List<ValidatorPublicKeyInfo> getNodeBlackList() {
        return nodeBlackList;
    }

    public void addBlackNode(ValidatorPublicKeyInfo node) {
        if (!nodeBlackList.contains(node)) {
            this.nodeBlackList.add(node);
        }
    }

    public void removeBlackNode(String caHash) {
        int removeIndex = -1;
        for (int i = 0; i < nodeBlackList.size(); i++) {
            if (nodeBlackList.get(i).getCaHash().equals(caHash)) {
                removeIndex = i;
                break;
            }
        }

        if (removeIndex >= 0) {
            this.nodeBlackList.remove(removeIndex);
        }

    }

    public int getThresholdMolecular() {
        return thresholdMolecular;
    }

    public int getThresholdDenominator() {
        return thresholdDenominator;
    }

    public List<ByteArrayWrapper> getCommitteeAddrs() {
        return committeeAddrs;
    }

    public Set<ByteArrayWrapper> getCommitteeAddrSet() {
        return committeeAddrSet;
    }

    public void addCommittee(ByteArrayWrapper committeeAddr) {
        boolean exist = false;
            if (committeeAddrSet.contains(committeeAddr)) {
                exist = true;
            }


        if (!exist) {
            committeeAddrs.add(committeeAddr);
            committeeAddrSet.add(committeeAddr);
        }
    }

    public void removeCommittee(ByteArrayWrapper committeeAddr) {
        committeeAddrs.remove(committeeAddr);
        committeeAddrSet.remove(committeeAddr);
    }


    public List<ByteArrayWrapper> getOperationsStaffAddrs() {
        return operationsStaffAddrs;
    }

    public Set<ByteArrayWrapper> getOperationsStaffAddrSet() {
        return operationsStaffAddrSet;
    }

    public void addOperationsStaff(ByteArrayWrapper opStaffAddr) {
        boolean exist = false;
        if (operationsStaffAddrSet.contains(opStaffAddr)) {
            exist = true;
        }


        if (!exist) {
            operationsStaffAddrs.add(opStaffAddr);
            operationsStaffAddrSet.add(opStaffAddr);
        }
    }

    public void removeOperationsStaff(ByteArrayWrapper opStaffAddr) {
        operationsStaffAddrs.remove(opStaffAddr);
        operationsStaffAddrSet.remove(opStaffAddr);
    }


    public Map<ByteArrayWrapper, ByteArrayWrapper> getGlobalEventProcessState() {
        return globalEventProcessState;
    }

    public List<GlobalNodeEventReceipt> getGlobalNodeEventReceipts() {
        return globalNodeEventReceipts;
    }

    public List<ByteArrayWrapper> getFilterAddrs() {
        return filterAddrs;
    }

    public Map<ByteArrayWrapper, CaContractCode> getCaContractCode() {
        return caContractCode;
    }

    public void removeFilter(FilterCandidate candidate) {
        ByteArrayWrapper addr = new ByteArrayWrapper(ByteUtil.copyFrom(candidate.getCaContractCode().getCodeAddress()));
        boolean exist = filterAddrs.remove(addr);
        if (exist) {
            //do delete
            caContractCode.put(addr, new CaContractCode(null));
        }
    }

    public void addFilter(FilterCandidate candidate) {
        boolean exist = false;
        ByteArrayWrapper newAddr = new ByteArrayWrapper(ByteUtil.copyFrom(candidate.getCaContractCode().getCodeAddress()));
        for (ByteArrayWrapper addr: filterAddrs) {
            if (addr.equals(newAddr)) {
                exist = true;
            }
        }

        if (!exist) {
            filterAddrs.add(newAddr);
            caContractCode.put(newAddr, new CaContractCode(ByteUtil.copyFrom(candidate.getCaContractCode().getEncoded())));
        }


    }

    public void addGlobalNodeEventReceipts(GlobalNodeEventReceipt globalNodeEventReceipt) {
        if (globalNodeEventReceipt == null) {
            throw new RuntimeException("globalNodeEventReceipt is null");
        }
        this.globalNodeEventReceipts.add(globalNodeEventReceipt);
    }

    public Map<Keyable, CaFinishProposalId> getFinishProposalIds() {
        return finishProposalIds;
    }

    public void addGlobalEventProcessState(ByteArrayWrapper key, ByteArrayWrapper value) {
        this.globalEventProcessState.put(key, value);
    }

    public void addFinishProposal(byte[] proposalId) {
        byte[] id = ByteUtil.copyFrom(proposalId);
        this.finishProposalIds.put(Keyable.ofDefault(id), new CaFinishProposalId(id));
    }

    public void addCaContractCode(ByteArrayWrapper key, CaContractCode value) {
        this.caContractCode.put(key, value);
    }

    public void reEncode() {
        this.rlpEncoded = rlpEncoded();
    }

    public boolean isUseSystemContract() {
        return useSystemContract;
    }

    @Override
    protected byte[] rlpEncoded() {


        int committeeAddrsSize = committeeAddrs.size();
        int operationsStaffAddrsSize = operationsStaffAddrs.size();
        int filterAddrsSize = filterAddrs.size();
        int nodeBlackListSize = nodeBlackList.size();
        //int globalEventProcessStateSize = globalEventProcessState.size();
        //int totalSize =2 + 1+ receiptsSize + 1 + filterAddrsSize + 1 + globalEventProcessStateSize;
        int totalSize =6 + 1 + committeeAddrsSize + 1 + operationsStaffAddrsSize + 1 + filterAddrsSize + 1 + nodeBlackListSize;

        byte[][] encode = new byte[totalSize][];
        encode[0] = useSystemContract? RLP.encodeInt(1) : RLP.encodeInt(0);
        encode[1] = RLP.encodeByte(code);
        encode[2] = RLP.encodeElement(snapshotData);
        encode[3] = validatorVerifier.getEncoded();
        encode[4] = RLP.encodeString(this.voteThreshold);
        encode[5] = RLP.encodeElement(this.stateRoot);

        int committeeAddrsPos = 6;
        encode[committeeAddrsPos] = RLP.encodeInt(committeeAddrsSize);
        int committeeAddrsStart = committeeAddrsPos + 1;
        int committeeAddrsEnd = committeeAddrsStart + committeeAddrsSize;
        for (int i = committeeAddrsStart; i < committeeAddrsEnd; i++) {
            encode[i] = RLP.encodeElement(committeeAddrs.get(i - committeeAddrsStart).getData());
        }



        int operationsStaffAddrsSizePos = committeeAddrsEnd;
        encode[operationsStaffAddrsSizePos] = RLP.encodeInt(operationsStaffAddrsSize);
        int operationsStaffAddrsStart = operationsStaffAddrsSizePos + 1;
        int operationsStaffAddrsEnd = operationsStaffAddrsStart + operationsStaffAddrsSize;
        for (int i = operationsStaffAddrsStart; i < operationsStaffAddrsEnd; i++) {
            encode[i] = RLP.encodeElement(operationsStaffAddrs.get(i - operationsStaffAddrsStart).getData());
        }


        int filterAddrsSizePos = operationsStaffAddrsEnd;
        encode[filterAddrsSizePos] = RLP.encodeInt(filterAddrsSize);
        int filterAddrsStart = filterAddrsSizePos + 1;
        int filterAddrsEnd = filterAddrsStart + filterAddrsSize;
        for (int i = filterAddrsStart; i < filterAddrsEnd; i++) {
            encode[i] = RLP.encodeElement(filterAddrs.get(i - filterAddrsStart).getData());
        }


        int nodeBlackListSizePos = filterAddrsEnd;
        encode[nodeBlackListSizePos] = RLP.encodeInt(nodeBlackListSize);
        int nodeBlackListStart = nodeBlackListSizePos + 1;
        int nodeBlackListEnd = nodeBlackListStart + nodeBlackListSize;
        for (int i = nodeBlackListStart; i < nodeBlackListEnd; i++) {
            encode[i] = nodeBlackList.get(i - nodeBlackListStart).getEncoded();
        }

//        int globalEventProcessStateSizePos =  2 + 1 + receiptsSize + 1 + filterAddrsSize;
//        encode[globalEventProcessStateSizePos] = RLP.encodeInt(globalEventProcessStateSize);
//        int temp = globalEventProcessStateSizePos + 1;
//        for (Map.Entry<ByteArrayWrapper, ByteArrayWrapper> entry: globalEventProcessState.entrySet()) {
//            encode[temp] =
//                    RLP.encodeList(
//                            RLP.encodeElement(entry.getKey().getData()),
//                            RLP.encodeElement(entry.getValue().getData())
//                    );
//            temp++;
//        }
        return RLP.encodeList(encode);
    }

    @Override
    protected void rlpDecoded() {
        RLPList state = (RLPList)RLP.decode2(rlpEncoded).get(0);
        this.useSystemContract = ByteUtil.byteArrayToInt(state.get(0).getRLPData()) == 1? true: false;
        this.code = (byte) ByteUtil.byteArrayToInt(state.get(1).getRLPData());
        this.snapshotData = state.get(2).getRLPData();
        this.candidateStateSnapshot = CandidateStateSnapshot.build(code, snapshotData);
        this.validatorVerifier = new ValidatorVerifier(state.get(3).getRLPData());
        this.voteThreshold = new String(state.get(4).getRLPData());
        String[] voteThresholdArr = voteThreshold.split("/");
        this.thresholdMolecular = Integer.parseInt(voteThresholdArr[0]);
        this.thresholdDenominator = Integer.parseInt(voteThresholdArr[1]);
        this.stateRoot = state.get(5).getRLPData();

        int committeeAddrsSize = ByteUtil.byteArrayToInt(state.get(6).getRLPData());
        List<ByteArrayWrapper> committeeAddrs = new ArrayList<>(committeeAddrsSize);
        int startCommitteeAddrsPos = 6 + 1;
        int endCommitteeAddrsPos = startCommitteeAddrsPos + committeeAddrsSize;
        for (int i = startCommitteeAddrsPos; i < endCommitteeAddrsPos; i++) {
            committeeAddrs.add(new ByteArrayWrapper(state.get(i).getRLPData()));
        }
        this.committeeAddrs = committeeAddrs;
        this.committeeAddrSet = new HashSet<>(committeeAddrs);



        int operationsStaffAddrsSize = ByteUtil.byteArrayToInt(state.get(endCommitteeAddrsPos).getRLPData());
        List<ByteArrayWrapper> operationsStaffAddrs = new ArrayList<>(operationsStaffAddrsSize);
        int startOperationsStaffAddrsPos = endCommitteeAddrsPos + 1;
        int endOperationsStaffAddrsPos = startOperationsStaffAddrsPos + operationsStaffAddrsSize;
        for (int i = startOperationsStaffAddrsPos; i < endOperationsStaffAddrsPos; i++) {
            operationsStaffAddrs.add(new ByteArrayWrapper(state.get(i).getRLPData()));
        }
        this.operationsStaffAddrs = operationsStaffAddrs;
        this.operationsStaffAddrSet = new HashSet<>(operationsStaffAddrs);


        int filterAddrsSize = ByteUtil.byteArrayToInt(state.get(endOperationsStaffAddrsPos).getRLPData());
        List<ByteArrayWrapper> filterAddrs = new ArrayList<>(filterAddrsSize);
        int startFilterAddrsPos = endOperationsStaffAddrsPos + 1;
        int endFilterAddrsPos = startFilterAddrsPos + filterAddrsSize;
        for (int i = startFilterAddrsPos; i < endFilterAddrsPos; i++) {
            filterAddrs.add(new ByteArrayWrapper(state.get(i).getRLPData()));
        }
        this.filterAddrs = filterAddrs;


        int nodeBlackListSize = ByteUtil.byteArrayToInt(state.get(endFilterAddrsPos).getRLPData());
        List<ValidatorPublicKeyInfo> nodeBlackList = new ArrayList<>(nodeBlackListSize);
        int startNodeBlackListPos = endFilterAddrsPos + 1;
        int endNodeBlackListPos = startNodeBlackListPos + nodeBlackListSize;
        for (int i = startNodeBlackListPos; i < endNodeBlackListPos; i++) {
            nodeBlackList.add(new ValidatorPublicKeyInfo(state.get(i).getRLPData()));
        }
        this.nodeBlackList = nodeBlackList;

//        int globalEventProcessStateSize = ByteUtil.byteArrayToInt(state.get(endFilterAddrsPos).getRLPData());
//        Map<ByteArrayWrapper, ByteArrayWrapper> globalEventProcessState = new HashMap<>(globalEventProcessStateSize);
//        int startGlobalEventProcessStatePos = endFilterAddrsPos + 1;
//        int endGlobalEventProcessStatePos = startFilterAddrsPos + globalEventProcessStateSize;
//        for (int i = startGlobalEventProcessStatePos; i < endGlobalEventProcessStatePos; i++) {
//            RLPList kvBytes = (RLPList) RLP.decode2(state.get(i).getRLPData()).get(0);
//            globalEventProcessState.put(new ByteArrayWrapper(kvBytes.get(0).getRLPData()), new ByteArrayWrapper(kvBytes.get(1).getRLPData()));
//        }
//        this.globalEventProcessState = globalEventProcessState;
    }

    public void cleanTransitionState() {
        this.caContractCode.clear();
        this.globalNodeEventReceipts.clear();
        this.globalEventProcessState.clear();
        this.finishProposalIds.clear();
    }

    public void calculateStateRoot() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER);


            if (this.stateRoot != null && this.stateRoot.length != 0) {
                digest.update(stateRoot);
            }

            for (GlobalNodeEventReceipt receipt: globalNodeEventReceipts) {
                digest.update(receipt.getEncoded());
            }


            for (CaContractCode caContractCode: caContractCode.values()) {
                digest.update(caContractCode.getEncoded());
            }

            for (Map.Entry<ByteArrayWrapper, ByteArrayWrapper> entry: globalEventProcessState.entrySet()) {
                digest.update(entry.getKey().getData());
                digest.update(entry.getValue().getData());
            }

            this.stateRoot = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GlobalEventState that = (GlobalEventState) o;
        return useSystemContract == that.useSystemContract &&
                code == that.code &&
                Arrays.equals(snapshotData, that.snapshotData) &&
                Arrays.equals(stateRoot, that.stateRoot) &&
                Objects.equals(validatorVerifier, that.validatorVerifier) &&
                Objects.equals(voteThreshold, that.voteThreshold) &&
                Objects.equals(committeeAddrs, that.committeeAddrs) &&
                Objects.equals(operationsStaffAddrs, that.operationsStaffAddrs) &&
                Objects.equals(nodeBlackList, that.nodeBlackList) &&
                Objects.equals(filterAddrs, that.filterAddrs);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(useSystemContract, code, validatorVerifier, voteThreshold, committeeAddrs, operationsStaffAddrs, filterAddrs, nodeBlackList);
        result = 31 * result + Arrays.hashCode(snapshotData);
        result = 31 * result + Arrays.hashCode(stateRoot);
        return result;
    }

    @Override
    public String toString() {
        return "GlobalEventState{" +
                "useSystemContract=" + useSystemContract +
                ", stateRoot=" + Hex.toHexString(stateRoot) +
                ", validatorVerifier=" + validatorVerifier +
                ", voteThreshold='" + voteThreshold + '\'' +
                ", committeeAddrs=" + committeeAddrs +
                ", operationsStaffAddrs=" + operationsStaffAddrs +
                ", filterAddrs=" + filterAddrs +
                ", nodeBlackList=" + nodeBlackList +
                ", candidateStateSnapshot=" + candidateStateSnapshot +
                '}';
    }
}

