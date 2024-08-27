package com.thanos.chain.ca.filter;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.contract.ca.filter.SystemContractCode;
import com.thanos.chain.contract.ca.manager.StateManager;
import com.thanos.chain.ledger.model.event.CommandEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.ca.CaContractCode;
import com.thanos.chain.ledger.model.event.ca.InvokeFilterEvent;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.model.ca.CaContractCodeBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

/**
 * InvokeEthContractAuthFilterTest.java description：
 *
 * @Author laiyiyu create on 2021-05-10 10:21:52
 */
public class InvokeEthContractAuthFilterTest {

    public static SystemConfig systemConfig = SystemConfig.getDefault();

    public static ConsensusChainStore consensusChainStore = new ConsensusChainStore(systemConfig, true);

    @Test
    public void queryFilter() {
        CaContractCode caContractCodeCondition = CaContractCodeBuilder.build();
        CaContractCode caContractCodeQuery = consensusChainStore.getCaContractCode(SystemContractCode.INVOKE_ETH_CONTRACT_AUTH_FILTER_ADDR);
        Assert.assertTrue(caContractCodeCondition.equals(caContractCodeQuery));
    }

    @Test
    public void setDeployWhiteTest() {
        GlobalStateRepositoryImpl globalStateRepository = consensusChainStore.globalStateRepositoryRoot.startTracking();
        StateManager stateManager1 = StateManager.buildManager(consensusChainStore.getLatestLedger().getCurrentEpochState(), globalStateRepository);
        SecureKey random = SecureKey.getInstance("ECDSA", 1);
        SecureKey opStaff = SecureKey.fromPrivate(Hex.decode("0100016114524225cd0e1fc379d2d258392ff3b1457e42b17bd938d1b9fd80f2a8f1bf"));
                //consensusChainStore.getLatestLedger().getCurrentEpochState().getGlobalEventState().getOperationsStaffAddrs().get(0).getData();
        //setDeployWhite
        byte[] invokeAddr = SystemContractCode.INVOKE_ETH_CONTRACT_AUTH_FILTER_ADDR;
        byte[] methodId = Hex.decode("ad57ed8990f80d57879c27065fb89a85ac4f0f42d6406fd55fddb480571c5c89");
        byte[] methodInput = RLP.encodeList(RLP.encodeElement(random.getAddress()), RLP.encodeInt(0));

        CommandEvent invokeEvent = new InvokeFilterEvent(invokeAddr, methodId, methodInput);
        stateManager1.execute(create(opStaff.getPubKey(), invokeEvent));

        byte[] dbKey = HashUtil.sha3(ByteUtil.merge(invokeAddr, "com.thanos.chain.contract.ca.filter.impl.InvokeEthContractAuthFilter".getBytes(), ByteUtil.merge("DEPLOY_WHITE_KEY_MAP_PREFIX".getBytes(), random.getAddress())));
        Assert.assertTrue(globalStateRepository.getCaContractStateValue(dbKey) != null);

    }

    public static GlobalNodeEvent create(byte[] pk, CommandEvent candidateEvent) {
        GlobalNodeEvent globalNodeEvent = new GlobalNodeEvent(pk, HashUtil.randomHash(), 1, candidateEvent.getEventCommand().getCode(), candidateEvent.getEncoded(),  HashUtil.randomHash());
        return globalNodeEvent;
    }

}
