package com.thanos.chain.consensus.hotstuffbft;

import com.thanos.chain.consensus.hotstuffbft.executor.ConsensusEventExecutor;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.consensus.hotstuffbft.store.EventTreeStore;
import com.thanos.chain.consensus.hotstuffbft.store.LivenessStorageData;
import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.txpool.TxnManager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.*;
import java.util.function.Function;

import static com.thanos.chain.consensus.hotstuffbft.model.EventRetrievalResponseMsg.EventRetrievalStatus.SUCCESSED;
import static com.thanos.chain.consensus.hotstuffbft.HotStuffChainSyncCoordinator.NeedFetchResult.*;

/**
 * 类ChainSyncManger.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 15:36:32
 */
public class HotStuffChainSyncCoordinator {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    public enum NeedFetchResult {
        QCRoundBeforeRoot,
        QCAlreadyExist,
        QCEventExist,
        NeedFetch,
    }

    public EventTreeStore eventTreeStore;

    public ConsensusEventExecutor consensusEventExecutor;

    public HotstuffNetInvoker netInvoker;

    public TxnManager txnManager;

    public HotStuffChainSyncCoordinator(EventTreeStore eventTreeStore, ConsensusEventExecutor consensusEventExecutor, HotstuffNetInvoker netInvoker, TxnManager txnManager) {
        this.consensusEventExecutor = consensusEventExecutor;
        this.netInvoker = netInvoker;
        this.eventTreeStore = eventTreeStore;
        this.txnManager = txnManager;
    }

//========================start  addCerts======================================================

    public ProcessResult<Void> addCerts(HotstuffChainSyncInfo syncInfo, byte[] preferredPeer) {
        try {
            ProcessResult<Void> syncRes = syncToHighestCommitCert(syncInfo.getHighestCommitCert(), preferredPeer);
            if (!syncRes.isSuccess()) {
                return syncRes;
            }

            ProcessResult<Void> insertHCCRes = insertQuorumCert(syncInfo.getHighestCommitCert(), preferredPeer, false);
            if (!insertHCCRes.isSuccess()) {
                return insertHCCRes;
            }

            ProcessResult<Void> insertHQCRes = insertQuorumCert(syncInfo.getHighestQuorumCert(), preferredPeer, false);
            if (!insertHQCRes.isSuccess()) {
                return insertHQCRes;
            }

            if (syncInfo.getHighestTimeoutCert().isPresent()) {
                return eventTreeStore.insertTimeoutCertificate(syncInfo.getHighestTimeoutCert().get());
            }
            return ProcessResult.ofSuccess();
        } catch (Exception e) {
            //e.printStackTrace();
            logger.warn("add certs warn! {}", ExceptionUtils.getStackTrace(e));
            return ProcessResult.ofError("");
        }

    }

    private ProcessResult<Void> syncToHighestCommitCert(QuorumCert highestCommitCert, byte[] preferredPeer) {
        if (!needSyncForQuorumCert(highestCommitCert)) return ProcessResult.ofSuccess();

        ProcessResult<LivenessStorageData.RecoveryData> recoveryDataRes = fastForwardSync(highestCommitCert, preferredPeer);
        if (!recoveryDataRes.isSuccess()) {
            return ProcessResult.ofError(recoveryDataRes.getErrMsg());
        }

        eventTreeStore.rebuild(recoveryDataRes.result);

//        if (highestCommitCert.isEpochChange()) {
//            EpochChangeProofMsg changeProofMsg = new EpochChangeProofMsg(Arrays.asList(highestCommitCert.getLedgerInfoWithSignatures()), false);
//            netInvoker.directSend(changeProofMsg, Arrays.asList(preferredPeer));
//        }
        return ProcessResult.ofSuccess();
    }

    private boolean needSyncForQuorumCert(QuorumCert qc) {
        //a=eventTreeStore.eventExists,
        //b=eventTreeStore.getRoot().getRound() >= qc.getCommitEvent().getRound()
        // 情况一，a 为true， b为false,总体为false，无需同步，该种情况说明自身节点有 commit qc，但没有执行commit
        // 情况二，a 为false, b为true,总体为false，无需同步，该种情况说明自身节点的commit qc 已大于 远端节点的 commit qc，但自身内存中（event tree）的commit qc 已被清除。
        // 情况三，a 为false, b为false,总体为true，需要同步
        return !(
                eventTreeStore.eventExists(qc.getCommitEvent().getId())
                || eventTreeStore.getRoot().getRound() >= qc.getCommitEvent().getRound()
        );
    }

    private NeedFetchResult needFetchForQuorumCert(QuorumCert qc) {
        if (qc.getCertifiedEvent().getRound() < eventTreeStore.getRoot().getRound()) {
            return QCRoundBeforeRoot;
        }

        if (eventTreeStore.getQCForEvent(qc.getCertifiedEvent().getId()) != null) {
            return QCAlreadyExist;
        }

        if (eventTreeStore.eventExists(qc.getCertifiedEvent().getId())) {
            return QCEventExist;
        }

        return NeedFetch;
    }

    private ProcessResult<Void> fetchQuorumCert(QuorumCert qc, byte[] preferredPeer) {
        LinkedList<Event> pending = new LinkedList();

        QuorumCert retrieveQc = qc;
        while (true) {
            if (eventTreeStore.eventExists(retrieveQc.getCertifiedEvent().getId())) {
                break;
            }

            ProcessResult<List<Event>> eventsRes = retrieveEventForQc(retrieveQc, 1, preferredPeer);
            if (!eventsRes.isSuccess()) {
                return ProcessResult.ofError(eventsRes.getErrMsg());
                //logger.warn("fetchQuorumCert error, reason: {}", eventsRes.getErrMsg());
                //throw new RuntimeException("fetchQuorumCert, retrieveEventForQc error");
            }

            // retrieve_block_for_qc guarantees that blocks has exactly 1 element
            Event event = eventsRes.result.remove(0);
            retrieveQc = event.getQuorumCert();
            pending.push(event);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("fetchQuorumCert, current qc:[{}]", qc);
            logger.debug("fetchQuorumCert, current fetch events:[{}]", pending);
        }

        while (pending.peek() != null) {
            Event event = pending.pop();
            ProcessResult<Void> insertQCRes = eventTreeStore.insertSingleQuorumCert(event.getQuorumCert());
            if (!insertQCRes.isSuccess()) {
                return ProcessResult.ofError(insertQCRes.getErrMsg());
            }

            ProcessResult<ExecutedEvent> exeAndInsertRes = eventTreeStore.executeAndInsertEvent(event);
            txnManager.removeEvent(event.getEventData());
            if (!exeAndInsertRes.isSuccess()) {
                return ProcessResult.ofError(exeAndInsertRes.getErrMsg());
            }
        }

        return eventTreeStore.insertSingleQuorumCert(qc);
    }
//========================end  addCerts======================================================


//========================start  insertQuorumCert======================================================
    public ProcessResult<Void> insertQuorumCert(QuorumCert qc, byte[] preferredPeer, boolean broadcastChange) {
        switch (needFetchForQuorumCert(qc)) {
            case NeedFetch:
                ProcessResult<Void> fetchResult = fetchQuorumCert(qc, preferredPeer);
                if (!fetchResult.isSuccess()) {
                    return fetchResult;
                }
                break;
            case QCEventExist:
                if (logger.isDebugEnabled()) {
                    logger.debug("insertQuorumCert QCEventExist:[{}]", qc);
                }
                ProcessResult<Void> insertQCRes = eventTreeStore.insertSingleQuorumCert(qc);
                if (!insertQCRes.isSuccess()) {
                    return insertQCRes;
                }
                ExecutedEvent executedEvent = eventTreeStore.getEvent(qc.getCertifiedEvent().getId());
                if (executedEvent != null && !executedEvent.getEvent().getEventData().allEmpty()) {
                    txnManager.removeEvent(executedEvent.getEvent().getEventData());
                }

                break;
            default:
                //do noting
        }

        if (this.eventTreeStore.getRoot().getRound() < qc.getCommitEvent().getRound()) {
            LedgerInfoWithSignatures finalityProof = qc.getLedgerInfoWithSignatures();

            Function<HotstuffChainSyncInfo, Void> broadcastFun = null;



            if (broadcastChange && finalityProof.getLedgerInfo().getNextEpochState().isPresent()) {
                broadcastFun = changeMsg -> {
                    changeMsg.getEncoded();
                    HotStuffChainSyncCoordinator.this.netInvoker.broadcast(changeMsg, false);
                    return null;
                };
            }


            ProcessResult<Void> commitRes = this.eventTreeStore.commit(finalityProof, broadcastFun);
            if (!commitRes.isSuccess()) {
                return commitRes.appendErrorMsg("insertQuorumCert, eventTreeStore.commit error!");
            }
//            if (qc.isEpochChange()) {
//                EpochChangeProofMsg changeProofMsg = new EpochChangeProofMsg(Arrays.asList(finalityProof), false);
//                netInvoker.directSend(changeProofMsg, Arrays.asList(preferredPeer));
//            }
        }
        return ProcessResult.ofSuccess();
    }

//========================end  insertQuorumCert======================================================

//========================start  fastForwardSync======================================================

    /**
     * this method do not rely on EventTreeStore, so it can be invoked
     * at any time
     */
    public ProcessResult<LivenessStorageData.RecoveryData> fastForwardSync(QuorumCert highestCommitCert, byte[] preferredPeer) {

        if (logger.isDebugEnabled()) {
            logger.debug("start fastForwardSync highestCommitCert![{}]", highestCommitCert);
        }

        ProcessResult<List<Event>> eventRes = retrieveEventForQc(highestCommitCert, 3, preferredPeer);
        //ProcessResult<List<Event>> eventRes = retrieveEventForQc(highestCommitCert, 3, preferredPeer);
        if (!eventRes.isSuccess()) {
            return ProcessResult.ofError(eventRes.getErrMsg());
        }

        List<Event> events = eventRes.result;

        if (logger.isDebugEnabled()) {
            logger.debug("start fastForwardSync eventRes![{}]", events);
        }

        Assert.assertTrue("should have 3-chain and equal", Arrays.equals(highestCommitCert.getCommitEvent().getId(), events.get(2).getId()));



        List<QuorumCert> quorumCerts = new ArrayList<>(3);
        quorumCerts.add(highestCommitCert);
        quorumCerts.add(events.get(0).getQuorumCert());
        quorumCerts.add(events.get(1).getQuorumCert());

        for (int i = 0; i < 3; i++) {
            Assert.assertTrue(Arrays.equals(events.get(i).getId(), quorumCerts.get(i).getCertifiedEvent().getId()));
        }

        events.forEach(event ->EventPayloadDecoder.asyncDecodePayload(event.getEventData()));

        try {
            // If a node restarts in the middle of state synchronization, it is going to try to catch up
            // to the stored quorum certs as the new root.
            this.eventTreeStore.getLivenessStorage().saveTree(events, quorumCerts);
        } catch (Exception e) {
            return ProcessResult.ofError(e.getMessage());
        }


        Event commitEvent = events.get(2);
        QuorumCert commitQC = quorumCerts.get(2);

        // 同步 commit cert 之前的event(block)
        logger.info("do sync from fastForwardSync ledger: {}", highestCommitCert.getLedgerInfoWithSignatures().getLedgerInfo());
        ProcessResult<Void> syncProcessRes = consensusEventExecutor.syncTo(highestCommitCert.getLedgerInfoWithSignatures(), commitEvent.getEventData(), preferredPeer);
        if (!syncProcessRes.isSuccess()) {
            ProcessResult.ofError(syncProcessRes.getErrMsg());
        }

        logger.debug("fastForwardSync success!");
        LivenessStorageData storageData = eventTreeStore.getLivenessStorage().start();
        if (!(storageData instanceof LivenessStorageData.RecoveryData)) {
            return ProcessResult.ofError("Failed to construct recovery data after fast forward sync");
        }

        return ProcessResult.ofSuccess((LivenessStorageData.RecoveryData)storageData);
    }



//========================end  fastForwardSync======================================================

    private ProcessResult<List<Event>> retrieveEventForQc(QuorumCert qc, int eventsNum, byte[] preferredPeer) {
        byte[] eventId = qc.getCertifiedEvent().getId();

            //ByteArrayWrapper peer = pickPeer(attempt, preferredPeer, peers);

        EventRetrievalRequestMsg eventRetrievalRequestMsg = new EventRetrievalRequestMsg(eventId, eventsNum);
        eventRetrievalRequestMsg.setNodeId(preferredPeer);
        //logger.debug("start EventRetrievalRequestMsg:" + Hex.toHexString(eventId));
        Message response = netInvoker.rpcSend(eventRetrievalRequestMsg, 5000);
        if (response == null) {
            logger.warn(
                    "Failed to fetch event {} from {}, try again",
                    Hex.toHexString(eventId), Hex.toHexString(preferredPeer));

            return ProcessResult.ofError(String.format("Failed to fetch event %s from %s", Hex.toHexString(eventId), Hex.toHexString(preferredPeer)));

        }

        EventRetrievalResponseMsg retrievalResponseMsg = new EventRetrievalResponseMsg(response.getEncoded());

        if (retrievalResponseMsg.getStatus() != SUCCESSED) {
            logger.error(
                    "Failed to fetch event {} from {} ,status: {}",
                    Hex.toHexString(eventId), Hex.toHexString(preferredPeer), retrievalResponseMsg.getStatus());

            return ProcessResult.ofError(String.format("Failed to fetch event %s from %s ,status: %s !", Hex.toHexString(eventId), Hex.toHexString(preferredPeer), retrievalResponseMsg.getStatus()));
        }

        return ProcessResult.ofSuccess(retrievalResponseMsg.getEvents());
    }

    public void release() {
        this.eventTreeStore = null;
        this.txnManager = null;
        this.consensusEventExecutor = null;
        this.netInvoker = null;
    }
}
