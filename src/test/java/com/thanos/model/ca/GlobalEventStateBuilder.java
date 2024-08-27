package com.thanos.model.ca;

import com.thanos.chain.consensus.hotstuffbft.model.ValidatorVerifier;
import com.thanos.chain.contract.ca.filter.SystemContractCode;
import com.thanos.chain.ledger.model.event.GlobalEventState;
import com.thanos.chain.ledger.model.event.ca.CaContractCode;
import com.thanos.chain.ledger.model.event.ca.JavaSourceCodeEntity;
import com.thanos.chain.ledger.model.event.ca.PlaceHolderStateSnapshot;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.HashUtil;
import com.thanos.model.common.ValidatorVerifierBuilder;

import java.util.*;

/**
 * GlobalEventStateBuilder.java description：
 *
 * @Author laiyiyu create on 2021-04-20 16:01:56
 */
public class GlobalEventStateBuilder {


    public static GlobalEventState buildFullContent() {
        String voteThreshold = "1/2";
        PlaceHolderStateSnapshot placeHolderStateSnapshot = new PlaceHolderStateSnapshot();
        byte code = placeHolderStateSnapshot.getCurrentCommand().getCode();
        byte[] snapshot = placeHolderStateSnapshot.getEncoded();
        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()));
        List<ByteArrayWrapper> opStaffAddrs = Arrays.asList(new ByteArrayWrapper(HashUtil.randomHash()));

        ByteArrayWrapper codeAddr = new ByteArrayWrapper(HashUtil.randomHash());
        List<ByteArrayWrapper> filterAddrs = Arrays.asList(codeAddr);
        JavaSourceCodeEntity javaSourceCodeEntity = new JavaSourceCodeEntity("com.thanos.chain.contract.ca.filter.impl.InvokeEthContractAuthFilter", SystemContractCode.INVOKE_ETH_CONTRACT_AUTH_FILTER_CODE);
        CaContractCode caContractCode = new CaContractCode(codeAddr.getData(), "test_filter", "com.thanos.chain.contract.ca.filter.impl.InvokeEthContractAuthFilter", Arrays.asList(javaSourceCodeEntity));
        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        contractCodeMap.put(codeAddr, caContractCode);
        return new GlobalEventState(true, voteThreshold, code, snapshot, validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, new ArrayList<>());
    }
}
