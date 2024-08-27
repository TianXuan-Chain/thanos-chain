package com.thanos.model.ca;

import com.thanos.chain.contract.ca.filter.SystemContractCode;
import com.thanos.chain.ledger.model.event.ca.CaContractCode;
import com.thanos.chain.ledger.model.event.ca.JavaSourceCodeEntity;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.Numeric;

import java.util.Arrays;

/**
 * CaContractCodeBuilder.java description：
 *
 * @Author laiyiyu create on 2021-04-28 14:13:08
 */
public class CaContractCodeBuilder {

    public static CaContractCode build() {

        ByteArrayWrapper filterAddr = new ByteArrayWrapper(SystemContractCode.INVOKE_ETH_CONTRACT_AUTH_FILTER_ADDR);
        JavaSourceCodeEntity javaSourceCodeEntity = new JavaSourceCodeEntity("com.thanos.chain.contract.ca.filter.impl.InvokeEthContractAuthFilter", SystemContractCode.INVOKE_ETH_CONTRACT_AUTH_FILTER_CODE);
        CaContractCode caContractCode = new CaContractCode(filterAddr.getData(), "auth_filter", "com.thanos.chain.contract.ca.filter.impl.InvokeEthContractAuthFilter", Arrays.asList(javaSourceCodeEntity));
        return caContractCode;
    }

    public static CaContractCode buildTest() {

        ByteArrayWrapper filterAddr = new ByteArrayWrapper(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000002"));
        JavaSourceCodeEntity javaSourceCodeEntity1 = new JavaSourceCodeEntity("com.thanos.chain.contract.ca.filter.impl.InvokeEthContractAuthFilterTest", SystemContractCode.TEST1_1);
        JavaSourceCodeEntity javaSourceCodeEntity2 = new JavaSourceCodeEntity("com.thanos.chain.contract.ca.filter.impl.Helper", SystemContractCode.TEST1_2);
        CaContractCode caContractCode = new CaContractCode(filterAddr.getData(), "test_filter", "com.thanos.chain.contract.ca.filter.impl.InvokeEthContractAuthFilter", Arrays.asList(javaSourceCodeEntity1, javaSourceCodeEntity2));
        return caContractCode;
    }

    public static CaContractCode buildTest2() {

        ByteArrayWrapper filterAddr = new ByteArrayWrapper(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000003"));
        JavaSourceCodeEntity javaSourceCodeEntity1 = new JavaSourceCodeEntity("com.thanos.chain.contract.ca.filter.impl.InvokeEthContractAuthFilterTest2", SystemContractCode.TEST2);
        CaContractCode caContractCode = new CaContractCode(filterAddr.getData(), "test2_filter", "com.thanos.chain.contract.ca.filter.impl.InvokeEthContractAuthFilterTest2", Arrays.asList(javaSourceCodeEntity1));
        return caContractCode;
    }



}
