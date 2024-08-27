package com.thanos.chain.ca.manager;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.ValidatorVerifier;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.ledger.model.event.CommandEvent;
import com.thanos.chain.ledger.model.event.GlobalEventState;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.ca.CaContractCode;
import com.thanos.chain.ledger.model.event.ca.PlaceHolderStateSnapshot;
import com.thanos.chain.storage.db.GlobalStateRepositoryRoot;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.HashUtil;
import com.thanos.model.ca.CaContractCodeBuilder;
import com.thanos.model.common.ValidatorVerifierBuilder;

import java.util.*;

/**
 * StateManagerBase.java description：
 *
 * @Author laiyiyu create on 2021-04-28 15:24:24
 */
public class StateManagerBase {

    public static SecureKey KEY1 = SecureKey.getInstance("ECDSA", 1);
    public static SecureKey KEY2 = SecureKey.getInstance("ECDSA", 1);
    public static SecureKey KEY3 = SecureKey.getInstance("ECDSA", 1);
    public static SecureKey KEY4 = SecureKey.getInstance("ECDSA", 1);

    public static ByteArrayWrapper COMMITTEE1 = new ByteArrayWrapper(KEY1.getAddress());
    public static ByteArrayWrapper COMMITTEE2 = new ByteArrayWrapper(KEY2.getAddress());
    public static ByteArrayWrapper COMMITTEE3 = new ByteArrayWrapper(KEY3.getAddress());

    public static ByteArrayWrapper OP_STAFF = new ByteArrayWrapper(KEY4.getAddress());


    public static SystemConfig systemConfig = SystemConfig.getDefault();

    public static ConsensusChainStore consensusChainStore = new ConsensusChainStore(systemConfig, true);


    public static EpochState createEmptyUserSystemContractEpoch() {
        PlaceHolderStateSnapshot stateSnapshot = new PlaceHolderStateSnapshot();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = new ArrayList<>(Arrays.asList(COMMITTEE1, COMMITTEE2, COMMITTEE3));

        List<ByteArrayWrapper> opStaffAddrs = new ArrayList<>(Arrays.asList(OP_STAFF));

        CaContractCode caContractCode = CaContractCodeBuilder.build();
        List<ByteArrayWrapper> filterAddrs = new ArrayList<>(Arrays.asList(new ByteArrayWrapper(caContractCode.getCodeAddress())));


        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
        contractCodeMap.put(filterAddrs.get(0), caContractCode);

        GlobalEventState globalEventState = new GlobalEventState(true, "2/3", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, new ArrayList<>());

        EpochState epochState = new EpochState(1, globalEventState);
        return epochState;
    }

    public static EpochState createNotUserSystemContractEpoch() {
        PlaceHolderStateSnapshot stateSnapshot = new PlaceHolderStateSnapshot();

        ValidatorVerifier validatorVerifier = ValidatorVerifierBuilder.build();

        List<ByteArrayWrapper> committeeAddrs = new ArrayList<>();

        List<ByteArrayWrapper> opStaffAddrs = new ArrayList<>();

        List<ByteArrayWrapper> filterAddrs = new ArrayList<>();


        Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();


        GlobalEventState globalEventState = new GlobalEventState(false, "2/3", stateSnapshot.getCurrentCommand().getCode(), stateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, opStaffAddrs, filterAddrs, contractCodeMap, new ArrayList<>());

        EpochState epochState = new EpochState(1, globalEventState);
        return epochState;
    }

    public static GlobalNodeEvent create(byte[] pk, CommandEvent candidateEvent) {
        GlobalNodeEvent globalNodeEvent = new GlobalNodeEvent(pk, HashUtil.randomHash(), 1, candidateEvent.getEventCommand().getCode(), candidateEvent.getEncoded(),  HashUtil.randomHash());
        return globalNodeEvent;
    }

}
