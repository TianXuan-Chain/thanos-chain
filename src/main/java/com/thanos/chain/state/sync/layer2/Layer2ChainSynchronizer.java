package com.thanos.chain.state.sync.layer2;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.consensus.hotstuffbft.model.EventInfo;
import com.thanos.chain.consensus.hotstuffbft.model.EventInfoWithSignatures;
import com.thanos.chain.consensus.hotstuffbft.model.ExecutedEventOutput;
import com.thanos.chain.consensus.hotstuffbft.model.LedgerInfo;
import com.thanos.chain.consensus.hotstuffbft.model.LedgerInfoWithSignatures;
import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.executor.GlobalExecutor;
import com.thanos.chain.network.NetInvoker;
import com.thanos.chain.network.protocols.MessageDuplexDispatcher;
import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.network.protocols.base.RemotingMessageType;
import com.thanos.chain.state.sync.layer2.model.Layer2StateChainSyncCommand;
import com.thanos.chain.state.sync.layer2.model.Layer2StateSyncRequestMsg;
import com.thanos.chain.state.sync.layer2.model.Layer2StateSyncResponseMsg;
import com.thanos.chain.txpool.TxnManager;
import com.thanos.common.utils.ThanosWorker;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Layer2ChainSynchronizer.java description：
 *
 * @Author laiyiyu create on 2020-03-08 09:35:43
 */
public class Layer2ChainSynchronizer {

    private static final Logger logger = LoggerFactory.getLogger("sync-layer2");

    // 32MB
    //private static final int TRANSFER_LIMIT_SIZE = 32 * 1024 *1024;

    private static final int TRANSFER_LIMIT_EVENT_NUM = 3;

    private static final int RETRIEVAL_TIMEOUT = 6000;

    private static final int MAX_FAIL_COUNT = 2;

    ConsensusChainStore consensusChainStore;

    NetInvoker netInvoker;

    GlobalExecutor globalExecutor;

    TxnManager txnManager;

    volatile boolean syncing;

    public Layer2ChainSynchronizer(NetInvoker netInvoker, ConsensusChainStore consensusChainStore, GlobalExecutor globalExecutor, TxnManager txnManager) {
        this.netInvoker = netInvoker;
        this.consensusChainStore = consensusChainStore;
        this.globalExecutor = globalExecutor;
        this.txnManager = txnManager;
        start();
    }

    public void start() {
        new ThanosWorker("layer2_chain_sync_coordinator_thread") {
            @Override
            protected void doWork() throws Exception {
                Message msg = MessageDuplexDispatcher.getLayer2StateSyncMsg();

                switch (Layer2StateChainSyncCommand.fromByte(msg.getCode())) {
                    case LAYER2_STATE_SYNC_REQUEST:
                        Layer2StateSyncRequestMsg layer2StateSyncRequestMsg = new Layer2StateSyncRequestMsg(msg.getEncoded());
                        layer2StateSyncRequestMsg.setNodeId(msg.getNodeId());
                        layer2StateSyncRequestMsg.setRpcId(msg.getRpcId());
                        layer2StateSyncRequestMsg.setRemoteType(msg.getRemoteType());
                        processSyncReq(layer2StateSyncRequestMsg);
                        break;
                    default:
                }
            }
        }.start();
    }

    public boolean isSyncing() {
        return syncing;
    }

    private void processSyncReq(Layer2StateSyncRequestMsg request) {

        long start = System.currentTimeMillis();
        logger.info("receive sync req number:{}", request.getStartEventNumber());
        if (request.getStartEventNumber() < 0 || request.getStartEventNumber() > consensusChainStore.getLatestLedger().getLatestNumber()) {
            logger.warn("receive sync req UN_EXCEPT_NUMBER, current:[{}], except:[{}]", consensusChainStore.getLatestLedger().getLatestNumber(), request.getStartEventNumber());
            Layer2StateSyncResponseMsg responseMsg = new Layer2StateSyncResponseMsg(Layer2StateSyncResponseMsg.CommitEventRetrievalStatus.UN_EXCEPT_NUMBER, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
            responseMsg.setRpcId(request.getRpcId());
            responseMsg.setRemoteType(RemotingMessageType.RESPONSE_MESSAGE.getType());
            netInvoker.directSend(responseMsg, Arrays.asList(request.getNodeId()));
            return;
        }
        Pair<List<EventData>, List<EventInfoWithSignatures>> result = consensusChainStore.getEventDatas(request.getStartEventNumber(), request.getStartEventNumber() + TRANSFER_LIMIT_EVENT_NUM);
        long end = System.currentTimeMillis();
        Layer2StateSyncResponseMsg responseMsg = new Layer2StateSyncResponseMsg(Layer2StateSyncResponseMsg.CommitEventRetrievalStatus.SUCCESSED, result.getLeft(), result.getRight());
        logger.info("receive sync req success find, total cost[{}]", (end - start));
        responseMsg.setRpcId(request.getRpcId());
        responseMsg.setRemoteType(RemotingMessageType.RESPONSE_MESSAGE.getType());
        netInvoker.directSend(responseMsg, Arrays.asList(request.getNodeId()));
    }

    public ProcessResult<Void> syncTo(LedgerInfoWithSignatures ledgerInfo, EventData latestCommitEvent, byte[] syncPeer) {
        try {
            syncing = true;
            int failCount = 0;

            if (consensusChainStore.getLatestLedger().getLatestNumber() == ledgerInfo.getLedgerInfo().getNumber()) {
                doLatestCommitEvent(ledgerInfo);
                return ProcessResult.SUCCESSFUL;
            }

            while (true) {
                if (consensusChainStore.getLatestLedger().getLatestNumber() >= ledgerInfo.getLedgerInfo().getNumber()) {
                    return ProcessResult.SUCCESSFUL;
                }

                // self sync start number
                long currentSyncNumber = consensusChainStore.getLatestLedger().getLatestNumber() + 1;
                logger.debug("sync from number:{}, current max commit number:{}", currentSyncNumber, ledgerInfo.getLedgerInfo().getNumber());
                Layer2StateSyncRequestMsg req = new Layer2StateSyncRequestMsg(currentSyncNumber);
                req.setNodeId(syncPeer);
                Message response = netInvoker.rpcSend(req, 30000);
                while (response == null) {
                    if (failCount == MAX_FAIL_COUNT) {
                        break;
                    }
                    response = netInvoker.rpcSend(req, RETRIEVAL_TIMEOUT);
                    failCount++;
                }

                if (response == null) {
                    logger.warn("Failed to fetch commit event from {}, sync fail!", Hex.toHexString(syncPeer));
                    return ProcessResult.ofError(String.format("Retrieval peer[%s] commit event fail", Hex.toHexString(syncPeer)));
                }
                failCount = 0;

                Layer2StateSyncResponseMsg responseMsg = new Layer2StateSyncResponseMsg(response.getEncoded());
                ProcessResult<Void> undoProcess = doSync(responseMsg, ledgerInfo);
                if (!undoProcess.isSuccess()) {
                    return ProcessResult.ofError(undoProcess.getErrMsg());
                }
            }
        } catch (Exception e) {
            logger.warn("Layer2ChainSynchronizer.syncTo {} warn! {}", ledgerInfo, ExceptionUtils.getStackTrace(e));
            return ProcessResult.ofError("syncTo error:" + e.getMessage());
        } finally {
            syncing = false;
        }
    }

    private ProcessResult<Void> doSync(Layer2StateSyncResponseMsg responseMsg, LedgerInfoWithSignatures latestLedgerInfo) {
        List<EventData> eventDatas = responseMsg.getEventDatas();
        List<EventInfoWithSignatures> eventInfoWithSignatureses = responseMsg.getEventInfoWithSignatureses();

        eventDatas.sort((o1, o2) -> (int) (o1.getNumber() - o2.getNumber()));
        Map<Long, EventInfoWithSignatures> signaturesMap = eventInfoWithSignatureses.stream().collect(Collectors.toMap(EventInfoWithSignatures::getNumber, eventInfoWithSignatures -> eventInfoWithSignatures));

        for (int i = 0; i < eventDatas.size(); i++) {
            EventData currentData = eventDatas.get(i);

            EventInfoWithSignatures eventInfoWithSignatures = signaturesMap.get(currentData.getNumber());
            if (eventInfoWithSignatures == null) {
                logger.error("doSync error! EventData {} has not signatures", currentData);
                return ProcessResult.ofError("doSync error!");
            }

            ProcessResult<Void> caeRes = checkAndExecute(currentData, eventInfoWithSignatures, latestLedgerInfo);

            if (!caeRes.isSuccess()) {
                return ProcessResult.ofError(caeRes.getErrMsg());
            }
        }

        return ProcessResult.ofSuccess();
    }

    private ProcessResult<Void> checkAndExecute(EventData eventData, EventInfoWithSignatures eventInfoWithSignatures, LedgerInfoWithSignatures latestLedgerInfo) {
        if (eventData.getNumber() != consensusChainStore.getLatestLedger().getLatestNumber() + 1) {
            logger.warn("un except event number, self latest number[{}], receive number[{}]", consensusChainStore.getLatestLedger().getLatestNumber(), eventData.getNumber());
            return ProcessResult.ofError("un except event number!");
        }

        // todo :: check the event data and signature
        ProcessResult<ExecutedEventOutput> exeRes = doExecute(eventData);
        if (exeRes.isSuccess()) {
//            LedgerInfoWithSignatures latestLedger = LedgerInfoWithSignatures.build(
//                    LedgerInfo.build(
//                            EventInfo.build(eventData.getEpoch(), eventData.getRound(), eventData.getHash(), exeRes.getResult().getStateRoot(), eventData.getNumber(), eventData.getTimestamp(), exeRes.getResult().getEpochState()),
//                            eventData.getHash()
//                    ),
//                    eventInfoWithSignatures.getSignatures());
            LedgerInfoWithSignatures latestLedger;
            if (latestLedgerInfo.getLedgerInfo().getNumber() == eventData.getNumber()) {
                latestLedger = latestLedgerInfo;
            } else {
                latestLedger = LedgerInfoWithSignatures.build(
                        LedgerInfo.build(
                                EventInfo.build(eventData.getEpoch(), eventData.getRound(), eventData.getHash(), exeRes.getResult().getStateRoot(), eventData.getNumber(), eventData.getTimestamp(), exeRes.getResult().getEpochState()),
                                eventData.getHash()
                        ),
                        eventInfoWithSignatures.getSignatures());
            }


            eventData.getPayload().reDecoded();

            if (!eventData.getGlobalEvent().isEmpty()) {
                while (globalExecutor.getLatestExecuteNum() != consensusChainStore.getLatestLedger().getLatestNumber()) {
                    try {
                        Thread.sleep(1000);
                        logger.info("sync need  async exe , please await!");
                    } catch (InterruptedException e) {
                    }
                }
            }

            consensusChainStore.commit(Arrays.asList(eventData), Arrays.asList(eventInfoWithSignatures), exeRes.getResult(), latestLedger, true);
            this.txnManager.syncCommitEvent(eventData);

            logger.debug("sync commit success:" + latestLedger);

        } else {
            return ProcessResult.ofError(exeRes.getErrMsg());
        }
        return ProcessResult.ofSuccess();
    }

    private ProcessResult<ExecutedEventOutput> doExecute(EventData eventData) {
        Optional<EpochState> epochState = globalExecutor.doExecuteGlobalNodeEvents(consensusChainStore.getLatestLedger().getCurrentEpochState(), false, eventData.getHash(), eventData.getNumber(), eventData.getGlobalEvent().getGlobalNodeEvents());
        ExecutedEventOutput executedEventOutput = new ExecutedEventOutput(new HashMap(), eventData.getNumber(), consensusChainStore.getLatestLedger().getCommitExecutedEventOutput().getStateRoot(), epochState);
        return ProcessResult.ofSuccess(executedEventOutput);
    }

    private void doLatestCommitEvent(LedgerInfoWithSignatures latestLedgerInfo) {
        ExecutedEventOutput executedEventOutput = new ExecutedEventOutput(new HashMap(), consensusChainStore.getLatestLedger().getLatestNumber(), consensusChainStore.getLatestLedger().getCommitExecutedEventOutput().getStateRoot(), Optional.empty());
        consensusChainStore.fastCommit(executedEventOutput, latestLedgerInfo);

    }
}
