package com.thanos.chain.consensus.hotstuffbft.store;

import com.thanos.chain.consensus.hotstuffbft.ChainedBFT;
import com.thanos.chain.consensus.hotstuffbft.model.chainConfig.OnChainConfigPayload;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.txpool.TxnManager;
import com.thanos.chain.consensus.hotstuffbft.executor.ConsensusEventExecutor;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 类EventTreeStore.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 15:27:06
 */
public class EventTreeStore {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    private EventTree eventTree;

    private ConsensusEventExecutor consensusEventExecutor;

    public PersistentLivenessStorage livenessStorage;

    int maxPrunedEventsInMemory;

    boolean reimportUnCommitEvent;

    private TxnManager txnManager;

    //private ReentrantReadWriteLock eventTreeLock = new ReentrantReadWriteLock();

    public EventTreeStore(PersistentLivenessStorage livenessStorage, LivenessStorageData.RecoveryData initialData, ConsensusEventExecutor consensusEventExecutor, TxnManager txnManager, int maxPrunedEventsInMemory, boolean reimportUnCommitEvent) {
        this.consensusEventExecutor = consensusEventExecutor;
        this.livenessStorage = livenessStorage;
        this.maxPrunedEventsInMemory = maxPrunedEventsInMemory;
        this.txnManager = txnManager;
        this.reimportUnCommitEvent = reimportUnCommitEvent;
        if (initialData != null) {
            Optional<TimeoutCertificate> highestTc = initialData.getHighestTimeoutCertificate();
            this.eventTree = buildEventTree(initialData, highestTc);
            doRecovery(initialData);
        }
    }

    public void releaseResource() {
        eventTree.txnManager = null;
        eventTree = null;
        txnManager = null;
        consensusEventExecutor = null;
        livenessStorage = null;
    }

    private EventTree buildEventTree(LivenessStorageData.RecoveryData initialData, Optional<TimeoutCertificate> highestTc) {
        LivenessStorageData.RecoveryData.RootInfo root = initialData.getRoot();
        ExecutedEventOutput executedEventOutput = initialData.getExecutedEventOutput();
        Assert.assertTrue(String.format("root qc version [%d] doesn't match committed trees [%d]", root.rootQc.getCertifiedEvent().getNumber(), executedEventOutput.getEventNumber())
                , root.rootQc.getCertifiedEvent().getNumber() == executedEventOutput.getEventNumber());

        Assert.assertTrue(String.format("root qc state id [%s] doesn't match committed trees [%s]", Hex.toHexString(root.rootQc.getCertifiedEvent().getExecutedStateId()), Hex.toHexString(executedEventOutput.getStateRoot()))
                , Arrays.equals(root.rootQc.getCertifiedEvent().getExecutedStateId(), executedEventOutput.getStateRoot()));

        ExecutedEventOutput rootExecutedOutput = new ExecutedEventOutput(root.rootQc.getNumber(), executedEventOutput.getStateHash(), executedEventOutput.getStateRoot(), executedEventOutput.getOutput(), root.rootQc.getCertifiedEvent().getNextEpochState());
        //executedEventOutput.resetEpcohState();
        ExecutedEvent executedEvent = new ExecutedEvent(root.rootEvent, rootExecutedOutput);

        EventTree eventTree = new EventTree(this.txnManager, executedEvent, root.rootQc, root.rootLi, highestTc, this.maxPrunedEventsInMemory, this.reimportUnCommitEvent);

        return eventTree;
    }

    public void rebuild(LivenessStorageData.RecoveryData initialData) {
        // Rollover the previous highest TC from the old tree to the new one.
        Optional<TimeoutCertificate> preHtc = this.getHighestTimeoutCert();
        EventTree tree = buildEventTree(initialData, preHtc);
        this.eventTree = tree;
        doRecovery(initialData);

        // If we fail to commit B_i via state computer and crash, after restart our highest commit cert
        // will not match the latest commit B_j(j<i) of state computer.
        // This introduces an inconsistent state if we send out SyncInfo and others try to sync to
        // B_i and figure out we only have B_j.
        // Here we commit up to the highest_commit_cert to maintain highest_commit_cert == state_computer.committed_trees.
        if (getHighestCommitCert().getCommitEvent().getRound() > getRoot().getRound()) {
            LedgerInfoWithSignatures finalityProof = getHighestCommitCert().getLedgerInfoWithSignatures();
            commit(finalityProof, null);
        }
    }

    private void doRecovery(LivenessStorageData.RecoveryData initialData) {
        LivenessStorageData.RecoveryData.RootInfo root = initialData.getRoot();
        for (Event event: initialData.events) {
            // root event has been execute ,ignore
            if (Arrays.equals(event.getId(), root.getRootEvent().getId())) continue;

            ProcessResult<ExecutedEvent> exeAndInsertRes = executeAndInsertEvent(event);
            if (!exeAndInsertRes.isSuccess()) {
                logger.error("[BlockStore] failed to insert event during build error :{}", exeAndInsertRes.getErrMsg());
                System.exit(-1);
            }
        }

        for (QuorumCert qc: initialData.quorumCerts) {
            if (Arrays.equals(qc.getCertifiedEvent().getId(), root.getRootEvent().getId())) continue;

            ProcessResult<Void> insertQCRes = insertSingleQuorumCert(qc);
            if (!insertQCRes.isSuccess()) {
                logger.error("[BlockStore] failed to insert qc during build error :{}", insertQCRes.getErrMsg());
                System.exit(-1);
            }
        }
    }

    public ProcessResult<ExecutedEvent> executeAndInsertEvent(Event event) {
        try {
            //eventTreeLock.writeLock().lock();
            ExecutedEvent oldExecutedEvent = getEvent(event.getId());
            if (oldExecutedEvent != null) return ProcessResult.ofSuccess(oldExecutedEvent);

            ExecutedEvent executedEvent = executeEvent(event);

            livenessStorage.saveTree(Arrays.asList(event), Collections.emptyList());
            this.eventTree.insertEvent(executedEvent);
            return ProcessResult.ofSuccess(executedEvent);
        } catch (Exception e) {
            logger.error("executeAndInsertEvent error!{}", ExceptionUtils.getStackTrace(e));
            return ProcessResult.ofError(e.getMessage());
        } finally {
            //eventTreeLock.writeLock().unlock();
        }
    }

    private ExecutedEvent executeEvent(Event event) {
        Assert.assertTrue("Event with old round", getRoot().getRound() < event.getRound());
        ExecutedEvent parentEvent = getEvent(event.getParentId());

        Assert.assertTrue(String.format("Event with missing parent [%s]", Hex.toHexString(event.getParentId())), parentEvent != null);

        // Reconfiguration rule - if a block is a child of pending reconfiguration, it needs to be empty
        // So we roll over the executed state until it's committed and we start new epoch.
        ExecutedEventOutput executedOutput;
        if ((parentEvent.getExecutedEventOutput().hasReconfiguration() && !Arrays.equals(parentEvent.getId(), this.eventTree.rootId))) {
            executedOutput = parentEvent.getExecutedEventOutput();
            //executedOutput = new ExecutedEventOutput(parentEvent.getStateOutput(), parentEvent.getEventNumber(), parentEvent.getStateRoot(), parentEvent.getExecutedEventOutput().getEpochState());
        } else {
            // 这里，需要处理empty event
            // Although NIL events don't have payload, we still send a T::default() to compute
            // because we may inject a event prologue transaction.
            executedOutput = consensusEventExecutor.execute(event, parentEvent);
        }
        return new ExecutedEvent(event, executedOutput);
    }

    public ProcessResult<Void> insertSingleQuorumCert(QuorumCert qc) {
        try {
            //eventTreeLock.writeLock().lock();
            // If the parent event is not the root event (i.e not None), ensure the executed state
            // of a event is consistent with its QuorumCert, otherwise persist the QuorumCert's
            // state and on restart, a new execution will agree with it.  A new execution will match
            // the QuorumCert's state on the next restart will work if there is a memory
            // corruption, for example.
            ExecutedEvent executedEvent = getEvent(qc.getCertifiedEvent().getId());
            Assert.assertTrue(
                    String.format("QuorumCert for event [%s] not found", Hex.toHexString(qc.getCertifiedEvent().getId())),
                    executedEvent != null);

            Assert.assertTrue(
                    String.format("QC for event {%s} has different {%s} than local {%s}", Hex.toHexString(qc.getCertifiedEvent().getId()), qc.getCertifiedEvent(), executedEvent.getEventInfo()),
                    executedEvent.getEventInfo().equals(qc.getCertifiedEvent()));

            executedEvent.setSignatures(qc.getLedgerInfoWithSignatures().getSignatures());


            this.livenessStorage.saveTree(Collections.emptyList(), Arrays.asList(qc));

            this.eventTree.insertQC(qc);
            return ProcessResult.ofSuccess();
        } catch (Exception e) {
            logger.error("insertSingleQuorumCert {} warn!{}", qc, ExceptionUtils.getStackTrace(e));
            return ProcessResult.ofError(e.getMessage());
        } finally {
            //eventTreeLock.writeLock().unlock();
        }
    }

    public ProcessResult<Void> insertTimeoutCertificate(TimeoutCertificate timeoutCertificate) {
        try {
            //eventTreeLock.writeLock().lock();
            Optional<TimeoutCertificate> timeoutCertificateOptional = this.getHighestTimeoutCert();
            long currentTcRound = timeoutCertificateOptional.isPresent()? timeoutCertificateOptional.get().getRound(): 0;

            //Assert.assertTrue(timeoutCertificate.getRound() > currentTcRound);
            if (timeoutCertificate.getRound() <= currentTcRound) return ProcessResult.ofSuccess();

            this.livenessStorage.saveHighestTimeoutCertificate(timeoutCertificate);

            this.eventTree.replaceTimeoutCert(timeoutCertificate);
            this.eventTree.reimportTimeoutEvent();
            //this.livenessStorage.consensusSource.deleteEventsAndQuorumCertificates(deleteEvents);
            return ProcessResult.ofSuccess();
        } catch (Exception e) {
            logger.error("insertTimeoutCertificate {} error!{}", timeoutCertificate, ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        } finally {
            //eventTreeLock.writeLock().unlock();
        }
    }

    public ProcessResult<Void> commit(LedgerInfoWithSignatures finalityProof, Function<HotstuffChainSyncInfo, Void> broadcastFun) {
        try {
            byte[] eventIdToCommit = finalityProof.getLedgerInfo().getConsensusEventId();
            ExecutedEvent eventToCommit = getEvent(eventIdToCommit);

            if (eventToCommit == null) {
                String errorInfo = String.format("Committed event [%s] not found", Hex.toHexString(eventIdToCommit));
                logger.error(errorInfo);
                throw new RuntimeException(errorInfo);
            }

            List<ExecutedEvent> eventsToCommit = pathFromRoot(eventIdToCommit);
            consensusEventExecutor.commit(eventsToCommit, finalityProof);


            pruneTree(eventToCommit.getId());
            //ExecutedEventOutput lastOutput = eventsToCommit.get(eventsToCommit.size() - 1).getExecutedEventOutput();
            if (finalityProof.getLedgerInfo().getNextEpochState().isPresent()) {
                if (broadcastFun != null) {
                    logger.info("will broadcast epoch change!");
                    HotstuffChainSyncInfo hotstuffChainSyncInfo = this.getHotstuffChainSyncInfo();
                    broadcastFun.apply(hotstuffChainSyncInfo);
                }
                //ChainedBFT.publishConfigPayload(OnChainConfigPayload.build(lastOutput.getEpochState().get()));
                ChainedBFT.publishConfigPayload(OnChainConfigPayload.build(finalityProof.getLedgerInfo().getNextEpochState().get()));
            }
            return ProcessResult.ofSuccess();
        } catch (Exception e) {
            logger.error("EventTreeStore.commit {} error!{}", finalityProof, ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
            //return ProcessResult.ofError(e.getMessage());
        }
    }

    public void pruneTree(byte[] nextRootId) {
        Set<ByteArrayWrapper> id2Remove;
        id2Remove = this.eventTree.findEventToPrune(nextRootId);

        if (logger.isDebugEnabled()) {
            logger.debug("event trace trace, pruneTree id2Remove [{}]", id2Remove);
        }

        this.livenessStorage.pruneTree(id2Remove.stream().map(id -> id.getData()).collect(Collectors.toList()));
        this.eventTree.processPrunedEvents(nextRootId, id2Remove);
    }


    //================================start do read
    public Pair<QuorumCert, List<Event>> getThreeChainCommitPair() {
        return eventTree.getThreeChainCommitPair();
    }

    public ExecutedEvent getRoot() {
        return eventTree.getRootEvent();
    }

    public ExecutedEvent getEvent(byte[] eventId) {
        return eventTree.getEvent(eventId);
    }

    public boolean eventExists(byte[] eventId) {
        return eventTree.eventExists(eventId);
    }

    public QuorumCert getQCForEvent(byte[] eventId) {
        return eventTree.getQCForEvent(eventId);
    }

    public List<ExecutedEvent> pathFromRoot(byte[] eventId) {
        return eventTree.pathFromRoot(eventId);
    }

    public ExecutedEvent getHeighestCertifiedEvent() {
        return eventTree.getHighestCertifiedEvent();
    }

    public QuorumCert getHighestQuorumCert() {
        return eventTree.getHighestQuorumCert();
    }

    public QuorumCert getHighestCommitCert() {
        return eventTree.getHighestCommitCert();
    }

    public Optional<TimeoutCertificate> getHighestTimeoutCert() {
        return eventTree.getHighestTimeoutCert();
    }

    public HotstuffChainSyncInfo getHotstuffChainSyncInfo() {
        return HotstuffChainSyncInfo.buildWithoutEncode(this.getHighestQuorumCert(),
                this.getHighestCommitCert(),
                this.getHighestTimeoutCert());
    }
    //================================end do read


    //===================for RecoveryMsgProcessor==============
    public EventTreeStore(PersistentLivenessStorage livenessStorage, int maxPrunedEventsInMemory, boolean reimportUnCommitEvent) {
        this.livenessStorage = livenessStorage;
        this.maxPrunedEventsInMemory = maxPrunedEventsInMemory;
        this.reimportUnCommitEvent = reimportUnCommitEvent;
    }

    public PersistentLivenessStorage getLivenessStorage() {
        return livenessStorage;
    }

    public boolean isStateConsistent() {
        return this.consensusEventExecutor.isStateConsistent();
    }
}
