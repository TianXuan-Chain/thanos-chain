package com.thanos.chain.consensus.hotstuffbft.store;

import com.thanos.chain.consensus.hotstuffbft.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * PersistentLivenessStorage.java description：
 *
 * @Author laiyiyu create on 2020-03-03 21:08:10
 */
public class PersistentLivenessStorage {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    ConsensusSource consensusSource;

    ConsensusChainStore chainStore;

    public PersistentLivenessStorage(ConsensusChainStore chainStore) {
        this.consensusSource = new ConsensusSource(false, chainStore.systemConfig);
        this.chainStore = chainStore;
    }

    public LivenessStorageData start() {
        logger.info("Start consensus recovery!");
        Optional<Vote> lastVote = consensusSource.getLastVoteMsg();
        Optional<TimeoutCertificate> highestTimeoutCertificate = consensusSource.getHighestTimeoutCertificate();
        List<Event> events = consensusSource.getAllEvents();
        List<QuorumCert> quorumCerts = consensusSource.getAllQuorumCerts();
        LatestLedger latestLedger = chainStore.getLatestLedger();

        // change the epoch
        LivenessStorageData.LedgerRecoveryData ledgerRecoveryData = LivenessStorageData.LedgerRecoveryData.build(latestLedger.getLatestLedgerInfo().getLedgerInfo());
        try {
            LivenessStorageData.RecoveryData recoveryData =
                    LivenessStorageData.RecoveryData
                            .build(consensusSource, lastVote,
                            ledgerRecoveryData,
                            events,
                            quorumCerts,
                            latestLedger.getCommitExecutedEventOutput(),
                            highestTimeoutCertificate);

            //logger.debug("" + recoveryData.toString());

            this.pruneTree(recoveryData.takeEventsToPrune());

            if (!recoveryData.getLastVote().isPresent()) {
                this.consensusSource.deleteLastVoteMsg();
            }

            if (!recoveryData.getHighestTimeoutCertificate().isPresent()) {
                this.consensusSource.deleteHighestTimeoutCertificate();
            }
            return recoveryData;
        } catch (Exception e) {
            logger.warn("will start RecoveryMsgProcessor!");
            return ledgerRecoveryData;
        }
    }

    public void saveTree(List<Event> events, List<QuorumCert> qcs) {
        consensusSource.saveEventsAndQuorumCertificates(events, qcs);
    }

    public void saveHighestTimeoutCertificate(TimeoutCertificate highestTimeoutCertificate) {
        consensusSource.saveHighestTimeoutCertificate(highestTimeoutCertificate);
    }

    public void pruneTree(List<byte[]> eventIds) {
        if (CollectionUtils.isEmpty(eventIds)) return;
        consensusSource.deleteEventsAndQuorumCertificates(eventIds);
    }

    public void saveVote(Vote vote) {
        consensusSource.saveLastVoteMsg(vote);
    }
}
