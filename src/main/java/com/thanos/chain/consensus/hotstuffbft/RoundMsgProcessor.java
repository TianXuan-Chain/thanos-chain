package com.thanos.chain.consensus.hotstuffbft;

import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.liveness.ProposalGenerator;
import com.thanos.chain.consensus.hotstuffbft.liveness.ProposerElection;
import com.thanos.chain.consensus.hotstuffbft.liveness.RoundState;
import com.thanos.chain.consensus.hotstuffbft.model.ConsensusMsg;
import com.thanos.chain.consensus.hotstuffbft.model.Event;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.consensus.hotstuffbft.model.EventRetrievalRequestMsg;
import com.thanos.chain.consensus.hotstuffbft.model.EventRetrievalResponseMsg;
import com.thanos.chain.consensus.hotstuffbft.model.ExecutedEvent;
import com.thanos.chain.consensus.hotstuffbft.model.HotstuffChainSyncInfo;
import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.consensus.hotstuffbft.model.ProposalMsg;
import com.thanos.chain.consensus.hotstuffbft.model.QuorumCert;
import com.thanos.chain.consensus.hotstuffbft.model.Timeout;
import com.thanos.chain.consensus.hotstuffbft.model.TimeoutCertificate;
import com.thanos.chain.consensus.hotstuffbft.model.Vote;
import com.thanos.chain.consensus.hotstuffbft.model.VoteMsg;
import com.thanos.chain.consensus.hotstuffbft.model.VoteProposal;
import com.thanos.chain.consensus.hotstuffbft.safety.SafetyRules;
import com.thanos.chain.consensus.hotstuffbft.store.EventTreeStore;
import com.thanos.chain.consensus.hotstuffbft.store.PersistentLivenessStorage;
import com.thanos.chain.consensus.hotstuffbft.store.VoteReceptionResult;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.network.protocols.base.RemotingMessageType;
import com.thanos.chain.txpool.TxnManager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * 类MsgProcessor.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 15:41:55
 */
public class RoundMsgProcessor extends ConsensusProcessor<Void> {

    RoundState roundState;

    EventTreeStore eventTreeStore;

    PersistentLivenessStorage livenessStorage;

    TxnManager txnManager;

    HotStuffChainSyncCoordinator hotStuffChainSyncCoordinator;

    ProposalGenerator proposalGenerator;

    ProposerElection proposerElection;

    SafetyRules safetyRules;

    HotstuffNetInvoker netInvoker;

    public RoundMsgProcessor(EpochState epochState, EventTreeStore eventTreeStore, RoundState roundState,
                             ProposerElection proposerElection, ProposalGenerator proposalGenerator,
                             SafetyRules safetyRules, HotstuffNetInvoker netInvoker,
                             TxnManager txnManager, HotStuffChainSyncCoordinator hotStuffChainSyncCoordinator,
                             PersistentLivenessStorage livenessStorage) {

        this.epochState = epochState;
        this.eventTreeStore = eventTreeStore;
        this.roundState = roundState;
        this.proposerElection = proposerElection;
        this.proposalGenerator = proposalGenerator;
        this.safetyRules = safetyRules;
        this.txnManager = txnManager;
        this.netInvoker = netInvoker;
        this.livenessStorage = livenessStorage;
        this.hotStuffChainSyncCoordinator = hotStuffChainSyncCoordinator;
    }

    public void releaseResource() {
        this.epochState = null;
        this.eventTreeStore.releaseResource();
        this.eventTreeStore = null;
        this.roundState = null;
        this.proposerElection = null;
        this.proposalGenerator = null;
        this.safetyRules = null;
        this.txnManager = null;
        this.netInvoker = null;
        this.livenessStorage = null;
        this.hotStuffChainSyncCoordinator.release();
        this.hotStuffChainSyncCoordinator = null;
    }

    public void start(Optional<Vote> lastVoteSent) {
        RoundState.NewRoundEvent newRoundEvent = roundState.processCertificates(this.eventTreeStore.getHotstuffChainSyncInfo());
        if (newRoundEvent == null) {
            logger.error("Can not jump start a round_state from existing certificates.");
            return;
        }

        if (lastVoteSent.isPresent()) {
            this.roundState.recordVote(lastVoteSent.get());
        }

        processNewRoundLocalMsg(newRoundEvent);
    }

    @Override
    public ProcessResult<Void> process(ConsensusMsg consensusMsg) {

        ProcessResult<Void> verifyRes = consensusMsg.verify(this.epochState.getValidatorVerifier());

        if (!verifyRes.isSuccess()) {
            logger.warn("verify msg {} fail, reason:{}", consensusMsg, verifyRes.getErrMsg());
            return ProcessResult.ofError("verify msg error!");
        }

        switch (consensusMsg.getCommand()) {
            case PROPOSAL:
                processProposalMsg((ProposalMsg) consensusMsg);
                break;
            case VOTE:
                processVoteMsg((VoteMsg) consensusMsg);
                break;
            case HOTSTUFF_CHAIN_SYNC:
                processChainSyncInfoMsg((HotstuffChainSyncInfo) consensusMsg);
                break;
            case LOCAL_TIMEOUT:
                processLocalTimeout((RoundState.LocalTimeoutMsg) consensusMsg);
                break;
            case EVENT_RETRIEVAL_REQ:
                processEventRetrieval((EventRetrievalRequestMsg) consensusMsg);
            default:
                break;
        }
        return ProcessResult.ofSuccess();
    }


    //====================start processNewRoundLocalMsg============================
    public void processNewRoundLocalMsg(RoundState.NewRoundEvent newRoundEvent) {
        //logger.debug("Processing {}", newRoundEvent);
        if (!proposerElection.isValidProposer(proposalGenerator.getAuthor(), newRoundEvent.getRound())) {
            return;
        }

        try {
            //long start = System.currentTimeMillis();
            ProposalMsg proposalMsg = generateProposal(newRoundEvent);
            //long end = System.currentTimeMillis();
            logger.info("processNewRoundLocalMsg, event[{}-{}-{}-parent[{}]-self[{}]]", proposalMsg.getProposal().getEpoch(), proposalMsg.getProposal().getRound(), proposalMsg.getProposal().getEventNumber(), Hex.toHexString(proposalMsg.getProposal().getParentId()), Hex.toHexString(proposalMsg.getProposal().getId()));

            if (!proposalMsg.getProposal().getEventData().isEmptyEvent()) {
                for (GlobalNodeEvent tx : proposalMsg.getProposal().getEventData().getGlobalEvent().getGlobalNodeEvents()) {
                    logger.info("EventData[{}],  tx: {}", Hex.toHexString(proposalMsg.getProposal().getId()), Hex.toHexString(tx.getHash()));
                }
            }


            if (IS_TRACE_ENABLED) {
                for (EthTransaction tx : proposalMsg.getProposal().getEventData().getPayload().getEthTransactions()) {
                    logger.trace("EventData[{}],  tx: {}", Hex.toHexString(proposalMsg.getProposal().getId()), Hex.toHexString(tx.getHash()));
                }
            }

            //logger.debug("[{}]generate proposal:{}, total size:{}, cost:{}ms", Hex.toHexString(proposalMsg.getProposal().getId()), proposalMsg, ByteUtil.getPrintSize(proposalMsg.getEncoded().length), (end - start));
            netInvoker.broadcast(proposalMsg, true);
        } catch (Exception e) {
            logger.warn("processNewRoundLocalMsg error! {}", ExceptionUtils.getStackTrace(e));
        }

    }

    private ProposalMsg generateProposal(RoundState.NewRoundEvent newRoundEvent) {

        // Proposal generator will ensure that at most one proposal is generated per round
        EventData proposal = this.proposalGenerator.generateProposal(newRoundEvent.getRound());
        Event signProposal = safetyRules.signProposal(proposal);
        if (signProposal == null) {
            throw new RuntimeException("generateProposal, safetyRules.signProposal error!");
        }

        return ProposalMsg.build(signProposal, this.eventTreeStore.getHotstuffChainSyncInfo());
    }
//====================end processNewRoundLocalMsg============================

//====================start processProposalMsg============================

    /**
     * Process a ProposalMsg, pre_process would bring all the dependencies and filter out invalid
     * proposal, processProposedEvent would execute and decide whether to vote for it.
     */
    public void processProposalMsg(ProposalMsg proposalMsg) {

        if (IS_DEBUG_ENABLED) {
            logger.debug("processProposalMsg:" + proposalMsg);
        }
        boolean stateConsistent = this.eventTreeStore.isStateConsistent();
        //ensure epoch change before stateConsistent
        if (!stateConsistent
                &&
                proposalMsg.getProposal().hasGlobalEvents()) {
            logger.warn("global state un consistent now, we should not do node event or payload , wait again! ");
            return;
        }

        if (ensureRoundAndSyncUp(proposalMsg.getProposal().getRound(), proposalMsg.getHotstuffChainSyncInfo(), proposalMsg.getNodeId(), true).isSuccess()) {
            processProposedEvent(proposalMsg.getProposal());
        }
    }

    private ProcessResult<Void> ensureRoundAndSyncUp(long msgRound, HotstuffChainSyncInfo syncInfo, byte[] nodeId, boolean helpRemote) {
        long currentRound = roundState.getCurrentRound();

        if (msgRound < currentRound) {
            logger.warn("ensureRoundAndSyncUp, Proposal round {} is less than current round {}",
                    msgRound,
                    currentRound);
            return ProcessResult.ofError();
        }

        ProcessResult<Void> syncUpRes = syncUp(syncInfo, nodeId, helpRemote);
        if (!syncUpRes.isSuccess()) {
            return syncUpRes;
        }

        // roundState may catch up with the SyncInfo, check again
        if (msgRound != roundState.getCurrentRound()) {
            logger.warn("After sync, round {} doesn't match local {}", msgRound, roundState.getCurrentRound());
            return ProcessResult.ofError();
        }

        return ProcessResult.ofSuccess();
    }

    private void processProposedEvent(Event proposal) {
        if (!this.proposerElection.isValidProposer(proposal)) {
            logger.warn("[RoundManager] Proposer {} for event {} is not a valid proposer for this round",
                    Hex.toHexString(proposal.getAuthor().get()),
                    proposal);
            return;
        }


        EventPayloadDecoder.decodePayload(proposal.getEventData());

        //logger.info("processProposedEvent executeAndVote");
        Vote vote = executeAndVote(proposal);


        if (vote == null) {
            return;
        }
        long proposalRound = proposal.getRound();
        byte[] nodeId = proposerElection.getValidProposer(proposalRound + 1).getRight();

        this.roundState.recordVote(vote);

        VoteMsg voteMsg = VoteMsg.build(vote, this.eventTreeStore.getHotstuffChainSyncInfo());
        //logger.debug("doCheck proposal msg success:" + proposal);
        this.netInvoker.directSend(voteMsg, nodeId);
    }

    private Vote executeAndVote(Event proposal) {
        ProcessResult<ExecutedEvent> executedEventRes = eventTreeStore.executeAndInsertEvent(proposal);
        if (!executedEventRes.isSuccess()) {
            throw new RuntimeException("executeAndVote, eventTreeStore.executeAndInsertEvent error!");
        }

        if (!epochState.getValidatorVerifier().containPublicKey(safetyRules.getAutorWrapper())) {
            logger.warn("current node is not a validator, ignore vote!");
            return null;
        }

        ExecutedEvent executedEvent = executedEventRes.getResult();
        Event event = executedEvent.getEvent();

        //this.txnManager.commit(event);

        // Checking round_state round again, because multiple proposed_block can now race
        // during async block retrieval
        if (event.getRound() != roundState.getCurrentRound()) {
            logger.warn("[RoundManager] Proposal [{}] rejected because round is incorrect. RoundState: [{}], proposed_block: [{}]",
                    event,
                    this.roundState.getCurrentRound(),
                    event.getRound());
            return null;
        }

//        Assert.assertTrue(String.format("[RoundManager] Proposal [%s] rejected because round is incorrect. RoundState: [%d], proposed_block: [%d]",
//                event,
//                this.roundState.getCurrentRound(),
//                event.getRound()), event.getRound() == roundState.getCurrentRound());
        // Short circuit if already voted.
        if (roundState.getVoteSent().isPresent()) {
            logger.warn("[RoundManager] Already vote on this round [{}]", this.roundState.getCurrentRound());
            return null;
        }

//        Assert.assertTrue(String.format("[RoundManager] Already vote on this round [%s]",
//                this.roundState.getCurrentRound()), !roundState.getVoteSent().isPresent());
        ExecutedEvent parentEvent = eventTreeStore.getEvent(proposal.getParentId());
        Assert.assertTrue("[RoundManager] Parent block not found after execution", parentEvent != null);

        VoteProposal voteProposal = VoteProposal.build(proposal, executedEvent.getEventNumber(), executedEvent.getStateRoot(), executedEvent.getExecutedEventOutput().getEpochState());

        Vote vote = safetyRules.constructAndSignVote(voteProposal);
        if (vote == null) {
            return null;
        }

        livenessStorage.saveVote(vote);
        return vote;
    }

//====================end processProposalMsg============================

//====================start processVoteMsg============================

    /**
     * Upon new vote:
     * 1. Ensures we're processing the vote from the same round as local round
     * 2. Filter out votes for rounds that should not be processed by this validator (to avoid
     * potential attacks).
     * 2. Add the vote to the pending votes and check whether it finishes a QC.
     * 3. Once the QC/TC successfully formed, notify the RoundState.
     */
    public void processVoteMsg(VoteMsg voteMsg) {
        if (IS_DEBUG_ENABLED) {
            logger.debug("processVoteMsg:" + voteMsg);
        }

        if (ensureRoundAndSyncUp(voteMsg.getVote().getVoteData().getProposed().getRound(), voteMsg.getHotstuffChainSyncInfo(), voteMsg.getNodeId(), true).isSuccess()) {
            processVote(voteMsg.getVote(), voteMsg.getNodeId());
        }
    }

    private void processVote(Vote vote, byte[] preferredPeer) {
        if (!vote.isTimeout()) {
            long nextRound = vote.getVoteData().getProposed().getRound() + 1;
            if (!proposerElection.isValidProposer(proposalGenerator.getAuthor(), nextRound)) {
                return;
            }
        }

        byte[] eventId = vote.getVoteData().getProposed().getId();

        // Check if the block already had a QC
        if (eventTreeStore.getQCForEvent(eventId) != null) return;

        // Sync up for timeout votes only.
        VoteReceptionResult voteReceptionResult = this.roundState.insertVote(vote, epochState.getValidatorVerifier());

        switch (voteReceptionResult.getVoteReception()) {
            case NewQuorumCertificate:
                if (IS_DEBUG_ENABLED) {
                    logger.debug("newQcAggregated.NewQuorumCertificate: " + voteReceptionResult.getResult());
                }
                newQcAggregated((QuorumCert) voteReceptionResult.getResult(), preferredPeer);
                break;
            case NewTimeoutCertificate:
                if (IS_DEBUG_ENABLED) {
                    logger.debug("newQcAggregated.NewTimeoutCertificate: " + voteReceptionResult.getResult());
                }
                newTcAggregated((TimeoutCertificate) voteReceptionResult.getResult());
                break;
            default:
                break;
        }
//        logger.debug("doCheck Vote success:" + vote);
    }

    private void newQcAggregated(QuorumCert qc, byte[] preferredPeer) {
        //logger.debug("create new QC:" + qc);
        // Process local highest commit cert should be no-op, this will sync us to the QC
        ProcessResult result = this.hotStuffChainSyncCoordinator.insertQuorumCert(qc, preferredPeer, true);

        if (!result.isSuccess()) {
            logger.error("[RoundManager] Failed to doCheck a newly aggregated QC");
            return;
        }
        //hotStuffChainSyncCoordinator.syncTo(HotstuffChainSyncInfo.build(qc, eventTreeStore.getHighestCommitCert(), Optional.empty()), author);
        processCertificates();
    }

    private void newTcAggregated(TimeoutCertificate tc) {
        ProcessResult result = eventTreeStore.insertTimeoutCertificate(tc);
        if (!result.isSuccess()) {
            logger.error("[RoundManager] Failed to doCheck a newly aggregated TC");
            return;
        }
        processCertificates();
    }
//====================end processVoteMsg============================


    //====================start processChainSyncInfoMsg============================
    public void processChainSyncInfoMsg(HotstuffChainSyncInfo hotstuffChainSyncInfo) {
        // To avoid a ping-pong cycle between two peers that move forward together.
        if (!ensureRoundAndSyncUp(hotstuffChainSyncInfo.getHighestRound() + 1, hotstuffChainSyncInfo, hotstuffChainSyncInfo.getNodeId(), false).isSuccess()) {
            logger.debug("doCheck sync info warn!");
        }
    }

    private ProcessResult<Void> syncUp(HotstuffChainSyncInfo syncInfo, byte[] nodeId, boolean helpRemote) {

        HotstuffChainSyncInfo localSyncInfo = this.eventTreeStore.getHotstuffChainSyncInfo();
        if (helpRemote && localSyncInfo.hasNewerCertificates(syncInfo)) {
            netInvoker.directSend(localSyncInfo, nodeId);
        }

        if (syncInfo.hasNewerCertificates(localSyncInfo)) {

            //logger.debug("Local state {} is stale than peer {} remote state {}", localSyncInfo, Hex.toHexString(nodeId), syncInfo);

            // Some information in SyncInfo is ahead of what we have locally.
            // First verify the SyncInfo (didn't verify it in the yet).
            if (!syncInfo.verify(this.epochState.getValidatorVerifier()).isSuccess()) {
                return ProcessResult.ofError("syncUp, syncInfo.verify error!");
            }

            ProcessResult<Void> processAddCertsResult = hotStuffChainSyncCoordinator.addCerts(syncInfo, nodeId);
            if (!processAddCertsResult.isSuccess()) {
                logger.warn("Fail to sync up to {}: {}", syncInfo, processAddCertsResult.getErrMsg());
                return processAddCertsResult;
            }

            ProcessResult<Void> proCerRes = processCertificates();
            if (!proCerRes.isSuccess()) {
                return proCerRes.appendErrorMsg("syncUp, processCertificates error!");
            }
        }

        return ProcessResult.ofSuccess();
    }
//====================end processChainSyncInfoMsg============================


    //====================start processEventRetrieval============================
    public void processEventRetrieval(EventRetrievalRequestMsg request) {
        List<Event> events = new ArrayList<>(8);
        EventRetrievalResponseMsg.EventRetrievalStatus status = EventRetrievalResponseMsg.EventRetrievalStatus.SUCCESSED;
        byte[] id = request.getEventId();

        while (events.size() < request.getEventNum()) {
            ExecutedEvent current = eventTreeStore.getEvent(id);
            if (current != null) {
                id = current.getParentId();
                events.add(current.getEvent());
            } else {
                status = EventRetrievalResponseMsg.EventRetrievalStatus.NOT_ENOUGH_EVENTS;
                break;
            }
        }

        if (events.size() == 0) {
            status = EventRetrievalResponseMsg.EventRetrievalStatus.ID_NOT_FOUND;
        }

        // do rpc response
        EventRetrievalResponseMsg eventRetrievalResponseMsg = new EventRetrievalResponseMsg(status, events);
        eventRetrievalResponseMsg.setRpcId(request.getRpcId());
        eventRetrievalResponseMsg.setRemoteType(RemotingMessageType.RESPONSE_MESSAGE.getType());
        //logger.debug("processEventRetrieval, request event[{}], resp:{}", Hex.toHexString(id), eventRetrievalResponseMsg);
        netInvoker.directSend(eventRetrievalResponseMsg, request.getNodeId());
    }

//====================end processEventRetrieval============================


    //====================start processLocalTimeout============================
    public void processLocalTimeout(RoundState.LocalTimeoutMsg localTimeoutMsg) {

        if (!epochState.getValidatorVerifier().containPublicKey(safetyRules.getAutorWrapper())) {
            logger.warn("current node is not a validator, ignore processLocalTimeout!");
            return;
        }

        long round = localTimeoutMsg.getRound();
        if (!roundState.processLocalTimeout(round)) {
            // logger.warn("[RoundManager] local timeout is stale");
            // The timeout event is late: the node has already moved to another round.
            return;
        }

        boolean useLastVote;

        Vote timeoutVote;
        if (roundState.getVoteSent().isPresent()
                && roundState.getVoteSent().get().getVoteData().getProposed().getRound() == round) {
            useLastVote = true;

            timeoutVote = roundState.getVoteSent().get();
        } else {
            useLastVote = false;
            Event emptyEvent = proposalGenerator.generateEmptyEvent(round);
            logger.debug("Planning to vote for a NIL block {}", emptyEvent);
            timeoutVote = executeAndVote(emptyEvent);
        }

        if (timeoutVote == null) {
            return;
        }

        logger.warn("Round {} timed out: {}, expected round proposer was {}, broadcasting the vote to all replicas",
                round,
                (useLastVote ? "already executed and voted at this round" : "will try to generate a backup vote"),
                Hex.toHexString(proposerElection.getValidProposer(round).getLeft()));

        if (!timeoutVote.isTimeout()) {
            Timeout timeout = timeoutVote.getTimeout();
            Signature signature = safetyRules.signTimeout(timeout);
            if (signature == null) {
                return;
            }
            timeoutVote.addTimeoutSignature(signature);
        }

        this.roundState.recordVote(timeoutVote);
        VoteMsg voteMsg = VoteMsg.build(timeoutVote, this.eventTreeStore.getHotstuffChainSyncInfo());
        netInvoker.broadcast(voteMsg, true);
    }

//====================end processLocalTimeout============================


    @Override
    public void saveTree(List<Event> events, List<QuorumCert> qcs) {
        this.livenessStorage.saveTree(events, qcs);
    }

    // This function is called only after all the dependencies of the given QC have been retrieved.
    private ProcessResult<Void> processCertificates() {
        HotstuffChainSyncInfo syncInfo = this.eventTreeStore.getHotstuffChainSyncInfo();
        RoundState.NewRoundEvent event = this.roundState.processCertificates(syncInfo);
        if (event != null) {
            processNewRoundLocalMsg(event);
        }
        return ProcessResult.ofSuccess();
    }
}
