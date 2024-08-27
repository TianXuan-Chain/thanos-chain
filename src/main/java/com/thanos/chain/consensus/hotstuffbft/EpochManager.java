package com.thanos.chain.consensus.hotstuffbft;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.consensus.hotstuffbft.executor.ConsensusEventExecutor;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.liveness.ProposalGenerator;
import com.thanos.chain.consensus.hotstuffbft.liveness.ProposerElection;
import com.thanos.chain.consensus.hotstuffbft.liveness.RotatingProposerElection;
import com.thanos.chain.consensus.hotstuffbft.liveness.RoundState;
import com.thanos.chain.consensus.hotstuffbft.model.ConsensusMsg;
import com.thanos.chain.consensus.hotstuffbft.model.Event;
import com.thanos.chain.consensus.hotstuffbft.model.LatestLedgerInfoRequestMsg;
import com.thanos.chain.consensus.hotstuffbft.model.LatestLedgerInfoResponseMsg;
import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.consensus.hotstuffbft.model.QuorumCert;
import com.thanos.chain.consensus.hotstuffbft.model.ValidatorVerifier;
import com.thanos.chain.consensus.hotstuffbft.model.Vote;
import com.thanos.chain.consensus.hotstuffbft.model.chainConfig.OnChainConfigPayload;
import com.thanos.chain.consensus.hotstuffbft.safety.SafetyRules;
import com.thanos.chain.consensus.hotstuffbft.store.EventTreeStore;
import com.thanos.chain.consensus.hotstuffbft.store.LivenessStorageData;
import com.thanos.chain.consensus.hotstuffbft.store.PersistentLivenessStorage;
import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.network.protocols.base.RemotingMessageType;
import com.thanos.chain.txpool.TxnManager;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 类EpochManager.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 15:36:33
 */
public class EpochManager {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    SystemConfig config;

    HotstuffNetInvoker netInvoker;

    ConsensusEventExecutor consensusEventExecutor;

    TxnManager txnManager;

    PersistentLivenessStorage livenessStorage;

    ConsensusProcessor msgProcessor;

    SafetyRules safetyRules;

    //author-> public key
    public EpochManager(SecureKey secureKey, HotstuffNetInvoker netInvoker, ConsensusEventExecutor consensusEventExecutor, TxnManager txnManager, PersistentLivenessStorage livenessStorage, SystemConfig systemConfig) {
        this.netInvoker = netInvoker;
        this.consensusEventExecutor = consensusEventExecutor;
        this.txnManager = txnManager;
        this.livenessStorage = livenessStorage;
        this.config = systemConfig;
        this.safetyRules = new SafetyRules(secureKey);
    }

    //========================start startProcess===============================
    public void startProcess(OnChainConfigPayload payload) {
        EpochState epochState = payload.getEpochState();
        LivenessStorageData livenessStorageData = this.livenessStorage.start();

        if (msgProcessor != null) {
            msgProcessor.releaseResource();
        }

        if (livenessStorageData instanceof LivenessStorageData.RecoveryData) {
            startRoundProcess((LivenessStorageData.RecoveryData) livenessStorageData, epochState);
        } else if (livenessStorageData instanceof LivenessStorageData.LedgerRecoveryData) {
            startSyncProcessor((LivenessStorageData.LedgerRecoveryData) livenessStorageData, epochState);
        } else {
            logger.error("un expect type");
        }
    }

    private void startRoundProcess(LivenessStorageData.RecoveryData recoveryData, EpochState epochState) {

        logger.info("Starting {} with genesis {}", recoveryData, epochState);

        logger.info("Create EventTreeStore");
        Optional<Vote> lastVote = recoveryData.getLastVote();
        EventTreeStore eventTreeStore = new EventTreeStore(livenessStorage, recoveryData, consensusEventExecutor, txnManager, config.getMaxPrunedEventsInMemory(), config.reimportUnCommitEvent());


        logger.info("Update SafetyRules");
        //SafetyRules safetyRules = new SafetyRules(author);
        //ConsensusState consensusState = safetyRules.getConsensusState();
        //Waypoint waypoint = consensusState.getWaypoint();
        //EpochChangeProofMsg epochChangeProof = livenessStorage.retrieveEpochChangeProof(waypoint.getNumber());
        safetyRules.initialize(epochState);

        logger.info("Create ProposalGenerator");
        ProposalGenerator proposalGenerator = new ProposalGenerator(safetyRules.getAuthor(), eventTreeStore, txnManager, config.packageTimeSleep());

        logger.info("Create RoundState");
        RoundState roundState = new RoundState(config.getRoundTimeoutBaseMS(), 1.5, 4);

        logger.info("Create ProposerElection");
        ProposerElection proposerElection = createProposerElection(epochState);

        logger.info("Update NetInvoker");
        //NetInvoker netInvoker = new NetInvoker(new PeerManager(this.config));
        netInvoker.updateEligibleNodes(epochState, consensusEventExecutor);

        RoundMsgProcessor roundMsgProcessor = new RoundMsgProcessor(
                epochState,
                eventTreeStore,
                roundState,
                proposerElection,
                proposalGenerator,
                safetyRules,
                netInvoker,
                txnManager,
                new HotStuffChainSyncCoordinator(eventTreeStore, consensusEventExecutor, netInvoker, this.txnManager),
                livenessStorage);
        roundMsgProcessor.start(lastVote);
        this.msgProcessor = roundMsgProcessor;
        logger.info("RoundManager started");
    }

    private ProposerElection createProposerElection(EpochState epochState) {
        switch (config.getProposerType()) {
            case ProposerElection.MultipleOrderedProposers:
                return null;
            case ProposerElection.RotatingProposer:
                return new RotatingProposerElection(epochState.getOrderedPublishKeys(), config.getContiguousRounds());
            default:
                return null;
        }
    }

    private void startSyncProcessor(LivenessStorageData.LedgerRecoveryData ledgerRecoveryData, EpochState epochState) {
        this.netInvoker.updateEligibleNodes(epochState, consensusEventExecutor);
        this.msgProcessor = new RecoveryMsgProcessor(epochState, netInvoker, consensusEventExecutor, new HotStuffChainSyncCoordinator(new EventTreeStore(this.livenessStorage, config.getMaxPrunedEventsInMemory(), config.reimportUnCommitEvent()), consensusEventExecutor, netInvoker, this.txnManager), ledgerRecoveryData.getCommitRound());
    }
//========================end startProcess===============================


    //========================start processMessage===============================
    public void processMessage(ConsensusMsg consensusMsg) {
        if (processEpoch(consensusMsg)) {
            ProcessResult processResult = msgProcessor.process(consensusMsg);
            if ((msgProcessor instanceof RecoveryMsgProcessor) && processResult.isSuccess()) {
                logger.info("Recovered from RecoveryMsgProcessor");
                msgProcessor.releaseResource();
                startRoundProcess((LivenessStorageData.RecoveryData) processResult.getResult(), consensusEventExecutor.getLatestLedgerInfo().getCurrentEpochState());
            }
        }
    }

    private boolean processEpoch(ConsensusMsg consensusMsg) {
        switch (consensusMsg.getCommand()) {
            case PROPOSAL:
            case VOTE:
            case HOTSTUFF_CHAIN_SYNC:
                if (this.getEpoch() == consensusMsg.getEpoch()) {
                    return true;
                } else {
                    if (consensusMsg.getNodeId() == null) {
                        //if means peer node has bug or self msg, we can ignore
                        return false;
                    }
                    processDifferentEpoch(consensusMsg.getNodeId(), consensusMsg.getEpoch());
                }
                break;
            case LOCAL_TIMEOUT:
            case EVENT_RETRIEVAL_REQ:
                return true;
            case LATEST_LEDGER_REQ:
                LatestLedgerInfoRequestMsg request = (LatestLedgerInfoRequestMsg) consensusMsg;
                processLatestLedgerReq(request);
                break;
            case LATEST_LEDGER_RESP:
                LatestLedgerInfoResponseMsg response = (LatestLedgerInfoResponseMsg) consensusMsg;
                doSync(response);
                break;
            default:
                logger.warn("Unexpected messages: {}", consensusMsg);
        }
        return false;
    }

    private void processDifferentEpoch(byte[] peerId, long differentEpoch) {
        if (differentEpoch < getEpoch()) {
            netInvoker.directSend(genLatestLedgerInfoResponseMsg(), peerId);
            //processEpochRetrieval(new EpochRetrievalRequestMsg(differentEpoch, getEpoch()));
        } else if (differentEpoch > getEpoch()) {
            ledgerSync(peerId);
        } else {
            logger.warn("Same epoch should not come to process_different_epoch");
        }
    }

    private void ledgerSync(byte[] peerId) {
        LatestLedgerInfoRequestMsg requestMsg = new LatestLedgerInfoRequestMsg();
        requestMsg.setNodeId(peerId);
        Message response = netInvoker.rpcSend(requestMsg);
        if (response == null) {
            logger.warn("ledgerSync error!");
            return;
        }
        LatestLedgerInfoResponseMsg latestLedgerInfoResponseMsg = new LatestLedgerInfoResponseMsg(response.getEncoded());
        latestLedgerInfoResponseMsg.setNodeId(peerId);
        //latestLedgerInfoResponseMsg.setNodeId(peerId);
        doSync(latestLedgerInfoResponseMsg);
    }

    private void processLatestLedgerReq(LatestLedgerInfoRequestMsg request) {
        LatestLedgerInfoResponseMsg response = genLatestLedgerInfoResponseMsg();
        response.setRpcId(request.getRpcId());
        response.setRemoteType(RemotingMessageType.RESPONSE_MESSAGE.getType());
        netInvoker.directSend(response, request.getNodeId());
    }

    private void doSync(LatestLedgerInfoResponseMsg response) {
        if (logger.isDebugEnabled()) {
            logger.debug("receive LatestLedgerInfoResponseMsg:" + response);
        }
        if (response.getStatus() != LatestLedgerInfoResponseMsg.LedgerRetrievalStatus.SUCCESSED) {
            return;
        }

        if (response.getLatestLedger().getLedgerInfo().getNumber() <= consensusEventExecutor.getLatestLedgerInfo().getLatestLedgerInfo().getLedgerInfo().getNumber()) {
            return;
        }

        try {
            // todo :: check the LatestLedgerInfoResponseMsg for signature
            QuorumCert highestCommitCert = response.getLatestCommitQC();
            List<Event> events = response.getThreeChainEvents();

            List<Event> validEvents = new ArrayList<>();
            validEvents.add(events.get(0));

            List<QuorumCert> quorumCerts = new ArrayList<>(3);
            quorumCerts.add(highestCommitCert);

            // avoid epoch change case
            if (!Arrays.equals(events.get(0).getId(), events.get(1).getId())) {
                validEvents.add(events.get(1));
                quorumCerts.add(events.get(0).getQuorumCert());
            }

            if (!Arrays.equals(events.get(1).getId(), events.get(2).getId())) {
                validEvents.add(events.get(2));
                quorumCerts.add(events.get(1).getQuorumCert());
            }

            for (int i = 0; i < validEvents.size(); i++) {
                Assert.assertTrue(Arrays.equals(events.get(i).getId(), quorumCerts.get(i).getCertifiedEvent().getId()));
            }

            events.forEach(event -> EventPayloadDecoder.decodePayload(event.getEventData()));

            this.msgProcessor.saveTree(validEvents, quorumCerts);
            consensusEventExecutor.syncTo(response.getLatestLedger(), events.get(2).getEventData(), response.getNodeId());
        } catch (Exception e) {
            logger.warn("epoch manager doSync error!{}", ExceptionUtils.getStackTrace(e));
        }
        startProcess(OnChainConfigPayload.build(consensusEventExecutor.getLatestLedgerInfo().getCurrentEpochState()));
    }

    private LatestLedgerInfoResponseMsg genLatestLedgerInfoResponseMsg() {

        if (msgProcessor == null || msgProcessor instanceof RecoveryMsgProcessor) {
            return new LatestLedgerInfoResponseMsg(LatestLedgerInfoResponseMsg.LedgerRetrievalStatus.CURRENT_NODE_IS_SYNCING, null, null, null);
        }

        Pair<QuorumCert, List<Event>> threeChainPair = ((RoundMsgProcessor) msgProcessor).eventTreeStore.getThreeChainCommitPair();

        return new LatestLedgerInfoResponseMsg(LatestLedgerInfoResponseMsg.LedgerRetrievalStatus.SUCCESSED, consensusEventExecutor.getLatestLedgerInfo().getLatestLedgerInfo(), threeChainPair.getKey(), threeChainPair.getValue());


        //new LatestLedgerInfoResponseMsg(consensusEventExecutor.getLatestLedgerInfo().getLatestLedgerInfo());
    }
//========================end processMessage===============================

    public boolean isSyncing() {
        return this.consensusEventExecutor.isSyncing();
    }

    public ValidatorVerifier getVerifier() {
        return getEpochState().getValidatorVerifier();
    }

    private long getEpoch() {
        return this.getEpochState().getEpoch();
    }

    private EpochState getEpochState() {
        if (this.msgProcessor == null) {
            logger.error("EpochManager not started yet");

            throw new RuntimeException("EpochManager not started yet");
        }

        return this.msgProcessor.getEpochState();
    }
}
