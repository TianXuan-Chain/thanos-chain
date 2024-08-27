
package com.thanos.chain.consensus.hotstuffbft.store;

import com.thanos.chain.config.Constants;
import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.contract.ca.filter.GlobalFilterChain;
import com.thanos.chain.contract.ca.filter.SystemContractCode;
import com.thanos.chain.ledger.model.event.GlobalEventState;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.chain.ledger.model.event.ca.*;
import com.thanos.chain.storage.db.GlobalStateRepositoryRoot;
import com.thanos.common.crypto.VerifyingKey;
import com.thanos.common.utils.BlockingPutHashMap;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.ThanosWorker;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import com.thanos.chain.ledger.model.genesis.GenesisJson;
import com.thanos.chain.ledger.model.store.DefaultValueable;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.chain.storage.datasource.AbstractDbSource;
import com.thanos.chain.storage.datasource.DbSettings;
import com.thanos.chain.storage.datasource.inmem.CacheDbSource;
import com.thanos.chain.storage.datasource.rocksdb.RocksDbSource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * 类EventStore.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 19:26:44
 */
public class ConsensusChainStore {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    private static final int ASYNC_COMMIT_TASK = 1;

    private static final Map<String, Class<? extends Persistable>> COLUMN_FAMILIES = new HashMap() {{
        put("epoch_state", EpochState.class);
        put("event_date", EventData.class);
        put("event_date_ds_check_res", EventDataDsCheckResult.class);
        put("event_date_with_signatures", EventInfoWithSignatures.class);
        put("single_entry", DefaultValueable.class);
        put("global_state", Keyable.DefaultKeyable.class);

        put("ca_contract_state", CaContractStateValue.class);
        put("ca_contract_code", CaContractCode.class);
        put("ca_finish_proposal_id", CaFinishProposalId.class);
        put("global_node_event_receipt", GlobalNodeEventReceipt.class);

    }};

    static Keyable LatestLedgerInfo = Keyable.ofDefault("LatestLedgerInfo".getBytes());

    static Keyable LatestOutput = Keyable.ofDefault("LatestOutput".getBytes());

    static class AsyncTaskContext {

        long lastNumber;

        List<EventData> eventDatas;

        CountDownLatch awaitCondition;

        volatile boolean success;

        public AsyncTaskContext(long lastNumber, List<EventData> eventDatas, CountDownLatch awaitCondition) {
            this.lastNumber = lastNumber;
            this.eventDatas = eventDatas;
            this.awaitCondition = awaitCondition;
            this.success = true;
        }

    }

    //static ArrayBlockingQueue<AsyncTaskContext> notifyQueue = new ArrayBlockingQueue(8);



    int maxCommitEventNumInMemory;

    public final AbstractDbSource db;

    public final GlobalStateRepositoryRoot globalStateRepositoryRoot;

    SystemConfig systemConfig;

    volatile LatestLedger cacheLatestLedger;

    BlockingPutHashMap<Long, EventData> commitEventCache;

    GlobalNodeEventStateCache globalNodeEventStateCache;

    public final DoubleSpendCheck doubleSpendCheck;

    public GlobalFilterChain globalFilterChain;

    public final boolean test;

    public ConsensusChainStore(SystemConfig systemConfig, boolean test) {
        this.systemConfig = systemConfig;
        this.maxCommitEventNumInMemory = systemConfig.getMaxCommitEventNumInMemory();
        this.commitEventCache = new BlockingPutHashMap<>(maxCommitEventNumInMemory);
        this.globalNodeEventStateCache = new GlobalNodeEventStateCache(systemConfig.getConsensusChainCaContractStateCacheSize());
        this.test = test;
        if (test)  {
            db = new CacheDbSource();
        } else {
            db = new RocksDbSource("consensus_chain", COLUMN_FAMILIES, systemConfig, DbSettings.newInstance().withMaxOpenFiles(systemConfig.getConsensusChainMaxOpenFiles()).withMaxThreads(systemConfig.getConsensusChainMaxThreads()).withWriteBufferSize(systemConfig.getConsensusChainWriteBufferSize()).withBloomFilterFlag(systemConfig.getConsensusChainBloomFilterFlag()));
        }

        this.globalStateRepositoryRoot = new GlobalStateRepositoryRoot(this);
        initLedger();
        this.doubleSpendCheck = new DoubleSpendCheck(systemConfig.getGenesisJson().getStartEventNumber(), maxCommitEventNumInMemory, this);
    }

    private void initLedger() {
        byte[] liInfoBytes = db.getRaw(DefaultValueable.class, LatestLedgerInfo);
        if (liInfoBytes == null) {
            // genesis init
            List<ValidatorPublicKeyInfo> validatorPublicKeyInfos = new ArrayList<>();
            for (Map.Entry<String, GenesisJson.ValidatorInfo> entry: this.systemConfig.getGenesisJson().getValidatorVerifiers().entrySet()) {
                byte[] publicKey = Hex.decode(entry.getKey());
                GenesisJson.ValidatorInfo validatorInfo = entry.getValue();
                validatorPublicKeyInfos.add(new ValidatorPublicKeyInfo(publicKey, validatorInfo.consensusVotingPower, validatorInfo.shardingNum, new VerifyingKey(publicKey), validatorInfo.name, validatorInfo.agency, validatorInfo.caHash));
            }
            ValidatorVerifier validatorVerifier = ValidatorVerifier.convertFrom(validatorPublicKeyInfos);

            List<ByteArrayWrapper> committeeAddrs = new ArrayList<>(this.systemConfig.getGenesisJson().getCommitteeAddrs().size());
            for (String committeeAddr: this.systemConfig.getGenesisJson().getCommitteeAddrs()) {
                committeeAddrs.add(new ByteArrayWrapper(Hex.decode(committeeAddr)));
            }

            List<ByteArrayWrapper> operationsStaffAddrs = new ArrayList<>(this.systemConfig.getGenesisJson().getOperationsStaffAddrs().size());
            for (String operationsStaffAddr: this.systemConfig.getGenesisJson().getOperationsStaffAddrs()) {
                operationsStaffAddrs.add(new ByteArrayWrapper(Hex.decode(operationsStaffAddr)));
            }

            PlaceHolderStateSnapshot placeHolderStateSnapshot = new PlaceHolderStateSnapshot();

            Map<ByteArrayWrapper, CaContractCode> contractCodeMap = new HashMap<>();
            List<ByteArrayWrapper> filterAddrs = new ArrayList<>();
            if (systemConfig.getGenesisJson().isUseSystemContract()) {
                ByteArrayWrapper filterAddr = new ByteArrayWrapper(SystemContractCode.INVOKE_ETH_CONTRACT_AUTH_FILTER_ADDR);
                filterAddrs.add(filterAddr);
                JavaSourceCodeEntity javaSourceCodeEntity = new JavaSourceCodeEntity("com.thanos.chain.contract.ca.filter.impl.InvokeEthContractAuthFilter", SystemContractCode.INVOKE_ETH_CONTRACT_AUTH_FILTER_CODE);
                CaContractCode caContractCode = new CaContractCode(filterAddr.getData(), "auth_filter", "com.thanos.chain.contract.ca.filter.impl.InvokeEthContractAuthFilter", Arrays.asList(javaSourceCodeEntity));
                contractCodeMap.put(filterAddr, caContractCode);
            }


            GlobalEventState globalEventState = new GlobalEventState(systemConfig.getGenesisJson().isUseSystemContract(), systemConfig.getGenesisJson().getVoteThreshold(), placeHolderStateSnapshot.getCurrentCommand().getCode(), placeHolderStateSnapshot.getEncoded(), validatorVerifier, committeeAddrs, operationsStaffAddrs, filterAddrs, contractCodeMap, new ArrayList<>());

            EpochState newEpochState = new EpochState(1,  globalEventState);

            ExecutedEventOutput executedEventOutput = new ExecutedEventOutput(new HashMap(), this.systemConfig.getGenesisJson().getStartEventNumber(), Constants.EMPTY_HASH_BYTES, Optional.of(newEpochState));
            EventInfo eventInfo = EventInfo.build(0, 0, Constants.EMPTY_HASH_BYTES, executedEventOutput.getStateRoot(), this.systemConfig.getGenesisJson().getStartEventNumber(), 0, Optional.of(newEpochState));
            LedgerInfo ledgerInfo = LedgerInfo.build(eventInfo, eventInfo.getHash());
            LedgerInfoWithSignatures ledgerInfoWithSignatures = LedgerInfoWithSignatures.build(ledgerInfo, new TreeMap());
            this.cacheLatestLedger = new LatestLedger();
            commit(new ArrayList<>(), new ArrayList<>(), executedEventOutput, ledgerInfoWithSignatures, false);
            logger.debug("init genesis ledger:" + cacheLatestLedger);
        } else {
            LedgerInfoWithSignatures latestLedger = new LedgerInfoWithSignatures(liInfoBytes);
            long latestEpoch = latestLedger.getLedgerInfo().getNextEpochState().isPresent()? latestLedger.getLedgerInfo().getEpoch() + 1: latestLedger.getLedgerInfo().getEpoch();

            EpochState epochState = new EpochState(db.getRaw(EpochState.class, Keyable.ofDefault(ByteUtil.longToBytes(latestEpoch))));
            ExecutedEventOutput executedEventOutput = new ExecutedEventOutput(db.getRaw(DefaultValueable.class, LatestOutput));
            executedEventOutput.setEpochState(latestLedger.getLedgerInfo().getNextEpochState());
            this.cacheLatestLedger = new LatestLedger(latestLedger, executedEventOutput, epochState);
            logger.debug("init ledger:" + cacheLatestLedger);
        }
        this.globalFilterChain = GlobalFilterChain.build(this.cacheLatestLedger.getCurrentEpochState().getGlobalEventState().getFilterAddrs(), globalStateRepositoryRoot);
    }

    public void commit(List<EventData> eventDatas, List<EventInfoWithSignatures> eventInfoWithSignatures, ExecutedEventOutput latestOutput,  LedgerInfoWithSignatures latestLedgerInfo, boolean update) {
        long start = System.currentTimeMillis();

        List<Pair<Keyable, Persistable>> saveBatch = new ArrayList<>(eventDatas.size() + latestOutput.getOutput().size() + 2);

        HashMap<Long, EventData> numToEvent = new HashMap<>(4);
        int totalTransactionSzie = 0;
        for (EventData eventData: eventDatas) {
            // ensure decode
            eventData.getPayload().reDecoded();
            this.doubleSpendCheck.updateCache(eventData.getNumber(), false, eventData);
            totalTransactionSzie += eventData.getPayload().getEthTransactions().length;
            numToEvent.put(eventData.getNumber(), eventData);
            //logger.debug("[{}] all chain trace do commit event size:{}", eventData.getNumber(), eventData.getPayload().getEthTransactions().length);
            //logger.info("commit event ds check {}", eventData.getEventDataDsCheckResult().getNumber(), eventData.getEventDataDsCheckResult());
            saveBatch.add(Pair.of(Keyable.ofDefault(ByteUtil.longToBytes(eventData.getNumber())), eventData.getEventDataDsCheckResult()));
            saveBatch.add(Pair.of(Keyable.ofDefault(ByteUtil.longToBytes(eventData.getNumber())), eventData));

            logger.info("store commit event: [{}-{}]", eventData.getNumber(), Hex.toHexString(eventData.getHash()));
        }

        for (EventInfoWithSignatures infoWithSignatures: eventInfoWithSignatures) {
            saveBatch.add(Pair.of(Keyable.ofDefault(ByteUtil.longToBytes(infoWithSignatures.getNumber())), infoWithSignatures));
        }

        for (Map.Entry<Keyable.DefaultKeyable, byte[]> entry: latestOutput.getOutput().entrySet()) {
            saveBatch.add(Pair.of(entry.getKey(), new DefaultValueable(entry.getValue())));
        }

        Optional<EpochState> newEpoch = latestOutput.getEpochState();
        if (newEpoch.isPresent()) {
            GlobalEventState globalEventState = newEpoch.get().getGlobalEventState();
            saveBatch.add(Pair.of(Keyable.ofDefault(ByteUtil.longToBytes(newEpoch.get().getEpoch())), newEpoch.get()));

            Map<ByteArrayWrapper, CaContractCode> caCodeMap = globalEventState.getCaContractCode();
            for (Map.Entry<ByteArrayWrapper, CaContractCode> entry: caCodeMap.entrySet()) {
                saveBatch.add(Pair.of(Keyable.ofDefault(entry.getKey().getData()), entry.getValue()));

            }

            Map<ByteArrayWrapper, ByteArrayWrapper> stateMap = globalEventState.getGlobalEventProcessState();
            if (stateMap.size() != 0) {
                this.globalFilterChain.refreshFilterState();
            }
            for (Map.Entry<ByteArrayWrapper, ByteArrayWrapper> entry: stateMap.entrySet()) {
                logger.debug("commit process state[{}-{}]", entry.getKey(), entry.getValue());
                saveBatch.add(Pair.of(Keyable.ofDefault(entry.getKey().getData()), new CaContractStateValue(entry.getValue().getData())));
            }

            for (Map.Entry<Keyable, CaFinishProposalId> entry: globalEventState.getFinishProposalIds().entrySet()) {
                //logger.info("CaFinishProposalId[{}]", Hex.toHexString(entry.getValue().getEncoded()));
                saveBatch.add(Pair.of(entry.getKey(), entry.getValue()));
            }

            List<GlobalNodeEventReceipt> eventReceipts = globalEventState.getGlobalNodeEventReceipts();
            for (GlobalNodeEventReceipt entry: eventReceipts) {
                logger.info("store commit GlobalNodeEventReceipt {}", Hex.toHexString(entry.getHash()));
                saveBatch.add(Pair.of(Keyable.ofDefault(entry.getHash()), entry));
            }
        }

        saveBatch.add(Pair.of(LatestLedgerInfo, new DefaultValueable(latestLedgerInfo.getEncoded())));
        saveBatch.add(Pair.of(LatestOutput, new DefaultValueable(latestOutput.getEncoded())));

        for (int i = 0; i < 10; i++) {
            try {
                if (!update) {
                    db.updateBatch(saveBatch);
                    cacheLatestLedger.reset(latestLedgerInfo, latestOutput);
                } else {
                    //CountDownLatch awaitCondition = new CountDownLatch(ASYNC_COMMIT_TASK);
                    //AsyncTaskContext asyncTaskContext = new AsyncTaskContext(latestLedgerInfo.getLedgerInfo().getNumber(), eventDatas, awaitCondition);
                    updateCommitEventCache(eventDatas);
                    //asyncCommitEvent(asyncTaskContext);
                    db.updateBatch(saveBatch);

                    if (newEpoch.isPresent()) {
                        GlobalEventState globalEventState = newEpoch.get().getGlobalEventState();
                        this.globalFilterChain.refresh(globalEventState.getCaContractCode());
                        updateGlobalNodeEventStateCache(globalEventState);
                        globalEventState.cleanTransitionState();
                    }

                    cacheLatestLedger.reset(latestLedgerInfo, latestOutput);
                }
                long end = System.currentTimeMillis();

                //logger.info("do commit db coast:" + (end - start) + "ms");
                String commitInfo = totalTransactionSzie > 0?  (" all chain trace, total txs num:" + totalTransactionSzie): (" empty");
                logger.info("[{}]{} do commit cost[{}ms] , {}:", Hex.toHexString(latestLedgerInfo.getLedgerInfo().getConsensusEventId()), commitInfo, (end - start), latestLedgerInfo);
                return;
            } catch (Exception e) {
                logger.error("consensusChainStore commit error!", e);
                //e.printStackTrace();
                if (i == 9) {
                    System.exit(0);
                }
            }
        }
    }

    public void fastCommit(ExecutedEventOutput latestOutput, LedgerInfoWithSignatures latestLedgerInfo) {
        List<Pair<Keyable, Persistable>> saveBatch = new ArrayList<>(2);

        saveBatch.add(Pair.of(LatestLedgerInfo, new DefaultValueable(latestLedgerInfo.getEncoded())));
        saveBatch.add(Pair.of(LatestOutput, new DefaultValueable(latestOutput.getEncoded())));

        for (int i = 0; i < 10; i++) {
            try {
                db.updateBatch(saveBatch);
                cacheLatestLedger.reset(latestLedgerInfo, latestOutput);
                //logger.info("[{}]{} do commit cost[{}ms] , {}:", Hex.toHexString(latestLedgerInfo.getLedgerInfo().getConsensusEventId()), commitInfo, (end - start), latestLedgerInfo);
                return;
            } catch (Exception e) {
                logger.error("consensusChainStore commit error!", e);
                //e.printStackTrace();
                if (i == 9) {
                    System.exit(0);
                }
            }
        }
    }

    private void updateCommitEventCache(List<EventData> eventDates) {
        for (EventData eventData: eventDates) {
            try {
                commitEventCache.put(eventData.getNumber(), eventData);
            } catch (InterruptedException e) {

            }
        }
    }

    private void updateGlobalNodeEventStateCache(GlobalEventState globalEventState) {
        globalNodeEventStateCache.updateGlobalNodeEventStateCache(globalEventState);
    }

    /**
     * read the LedgerInfoWithSignatures from consensus commit event LedgerInfoWithSignatures which persist in db
     * @return
     */
    public LatestLedger getLatestLedger() {
        return cacheLatestLedger;
    }

    //for speed up, eventData must decode when use
    public Pair<List<EventData>, List<EventInfoWithSignatures>> getEventDatas(long startEventNumber, long endEventNumber) {
        if (endEventNumber > cacheLatestLedger.getLatestLedgerInfo().getLedgerInfo().getNumber()) {
            endEventNumber = cacheLatestLedger.getLatestLedgerInfo().getLedgerInfo().getNumber();
        }

        int totalNum = (int) (endEventNumber - startEventNumber + 1);
        List<byte[]> batchKeys = new ArrayList(totalNum);
        for (long i = startEventNumber; i <= endEventNumber; i++) {
            batchKeys.add(ByteUtil.longToBytes(i));
        }

        Map<byte[], byte[]> eventDatasRes = db.batchGetRaw(EventData.class, batchKeys);
        List<EventData> eventDatas = new ArrayList<>(totalNum);
        for (Map.Entry<byte[], byte[]> entry: eventDatasRes.entrySet()) {
            eventDatas.add(EventData.buildWithoutDecode(entry.getValue()));
        }

        Map<byte[], byte[]> eventInfosRes = db.batchGetRaw(EventInfoWithSignatures.class, batchKeys);
        List<EventInfoWithSignatures> eventInfoWithSignatures = new ArrayList<>(totalNum);
        for (Map.Entry<byte[], byte[]> entry: eventInfosRes.entrySet()) {
            eventInfoWithSignatures.add(EventInfoWithSignatures.buildWithoutDecode(entry.getValue()));
        }
        return Pair.of(eventDatas, eventInfoWithSignatures);
    }

    public EventData getEventData(long number, boolean fullDecode, boolean removeCache) {
        EventData result = null;
        //long start = System.currentTimeMillis();
        try {
            if (removeCache) {
                result = this.commitEventCache.remove(number);
            } else {
                result = this.commitEventCache.get(number);
            }

        } catch (InterruptedException e) {
        }

        //String threadName = Thread.currentThread().getName();

        if (result != null) {
            if (fullDecode) {
                result.getPayload().reDecoded();
            }
            //logger.debug("[{}]getEventData[{}] hit cache!, cost[{}]", threadName, number, (System.currentTimeMillis() - start));

        } else {
            byte[] eventDataBytes = db.getRaw(EventData.class, Keyable.ofDefault(ByteUtil.longToBytes(number)));
            if (eventDataBytes == null) {
                //logger.debug("[{}]getEventData[{}] was not exist, cost[{}] ", threadName, number, (System.currentTimeMillis() - start));
                return null;
            }
            result = new EventData(eventDataBytes);
            if (fullDecode) {
                result.getPayload().reDecoded();
            }
        }
        return result;
    }

    public EpochState getEpochState(long epoch) {
        byte[] raw = db.getRaw(EpochState.class, Keyable.ofDefault(ByteUtil.longToBytes(epoch)));
        if (raw == null) return null;
        return new EpochState(raw);
    }

    public CaContractStateValue getCaContractStateValue(byte[] key) {
        Keyable keyable = Keyable.ofDefault(key);
        CaContractStateValue cacheValue = globalNodeEventStateCache.getCaContractStateValue(keyable);
        if (cacheValue != null) return cacheValue;

        byte[] raw = db.getRaw(CaContractStateValue.class, Keyable.ofDefault(key));
        if (raw == null) return null;
        return new CaContractStateValue(raw);
    }

    public GlobalNodeEventReceipt getGlobalNodeEventReceipt(byte[] key) {
        byte[] raw = db.getRaw(GlobalNodeEventReceipt.class, Keyable.ofDefault(key));
        if (raw == null) return null;
        return new GlobalNodeEventReceipt(raw);
    }

    public CaContractCode getCaContractCode(byte[] key) {
        byte[] raw = db.getRaw(CaContractCode.class, Keyable.ofDefault(key));
        if (raw == null) return null;
        return new CaContractCode(raw);
    }

    public CaFinishProposalId getCaFinishProposalId(byte[] id) {
        byte[] raw = db.getRaw(CaFinishProposalId.class, Keyable.ofDefault(id));
        if (raw == null) return null;
        return new CaFinishProposalId(raw);
    }

    public EventDataDsCheckResult getDsCheckRes(long number) {
        byte[] dsResBytes = db.getRaw(EventDataDsCheckResult.class, Keyable.ofDefault(ByteUtil.longToBytes(number)));
        if (dsResBytes == null) {
            return null;
        }
        return new EventDataDsCheckResult(dsResBytes);
    }
}
