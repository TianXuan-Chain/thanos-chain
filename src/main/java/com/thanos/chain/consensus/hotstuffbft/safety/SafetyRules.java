package com.thanos.chain.consensus.hotstuffbft.safety;

import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.consensus.hotstuffbft.store.PersistentSafetyStorage;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.chain.ledger.model.crypto.ValidatorSigner;
import com.thanos.common.utils.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.thanos.chain.config.Constants.EMPTY_HASH_BYTES;

/**
 * SafetyRules.java description：
 *
 * @Author laiyiyu create on 2020-03-03 12:04:04
 */
public class SafetyRules {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    PersistentSafetyStorage safetyStorage;

    ValidatorSigner signer;

    EpochState epochState;

    ByteArrayWrapper autorWrapper;

    private ReentrantReadWriteLock safetyRulesLock = new ReentrantReadWriteLock();

    public SafetyRules(SecureKey secureKey) {

        signer = new ValidatorSigner(secureKey);
        autorWrapper = new ByteArrayWrapper(signer.getAuthor());
        safetyStorage = new PersistentSafetyStorage(true);
    }

    public void initialize(EpochState epochState) {
        try {
            this.safetyRulesLock.writeLock().lock();
            startNewEpoch(epochState);
        } finally {
            this.safetyRulesLock.writeLock().unlock();
        }
    }

    private void startNewEpoch(EpochState epochState) {
        this.epochState = epochState;

        long currentEpoch = this.safetyStorage.getEpoch();

        if (currentEpoch < epochState.getEpoch()) {
            // This is ordered specifically to avoid configuration issues:
            // * First set the waypoint to lock in the minimum restarting point,
            // * set the round information,
            // * finally, set the epoch information because once the epoch is set, this `if`
            // statement cannot be re-entered.
            safetyStorage.setEpoch(epochState.getEpoch());
            safetyStorage.setLastVotedRound(0);
            safetyStorage.setPreferredRound(0);
            //safetyStorage.setWaypoint(Waypoint.build(ledgerInfo));
        }
    }

    public byte[] getAuthor() {
        return this.signer.getAuthor();
    }

    public ByteArrayWrapper getAutorWrapper() {
        return autorWrapper;
    }

    //当生成共识Event作为签名时，无需写入deltaState。
    public Event signProposal(EventData eventData) {
        try {
            this.safetyRulesLock.writeLock().lock();

            // verify author
            Optional<byte[]> authorOp = eventData.getAuthor();
            if (!authorOp.isPresent()) {
                logger.debug("signProposal, No author found in the proposal!");
                return null;
            }
            if (!Arrays.equals(authorOp.get(), this.signer.getAuthor())) {
                logger.debug("signProposal, Proposal author is not validator signer!");
                return null;
            }

            //verify epoch
            if (eventData.getEpoch() != safetyStorage.getEpoch()) {
                logger.error("signProposal, Incorrect Epoch!");
                return null;
            }

            // last vote round
            if (eventData.getRound() <= safetyStorage.getLastVotedRound()) {
                logger.debug("Block round is older than last_voted_round ({} <= {})", eventData.getRound(), safetyStorage.getLastVotedRound());
                return null;
            }

            // verify qc
            ProcessResult<Void> verifyProcessResult = verifyQC(eventData.getQuorumCert());
            if (!verifyProcessResult.isSuccess()) {
                logger.debug("signProposal, verifyQC fail!");
                return null;
            }

            // compare Preferred Round
            ProcessResult<Void> checkRes = checkAndUpdatePreferredRound(eventData.getQuorumCert());
            if (!checkRes.isSuccess()) {
                logger.debug("signProposal, checkAndUpdatePreferredRound error.{}", checkRes.getErrMsg());
                return null;
            }

            return Event.buildProposalFromEventData(eventData, signer);
        } finally {
            this.safetyRulesLock.writeLock().unlock();
        }
    }

    public Vote constructAndSignVote(VoteProposal voteProposal) {
        try {
            this.safetyRulesLock.writeLock().lock();
            //logger.debug("Incoming vote proposal to signECDSA.");

            Event proposalEvent = voteProposal.getEvent();

            // verify epoch
            if (proposalEvent.getEpoch() != safetyStorage.getEpoch()) {
                logger.debug("constructAndSignVote Incorrect Epoch!");
                return null;
            }

            ProcessResult<Void> verifyRes = verifyQC(proposalEvent.getQuorumCert());
            if (!verifyRes.isSuccess()) {
                logger.warn("constructAndSignVote, verifyQC error.{}", verifyRes.getErrMsg());
                return null;
            }

            ProcessResult<Void> checkRes = checkAndUpdatePreferredRound(proposalEvent.getQuorumCert());
            if (!checkRes.isSuccess()) {
                logger.warn("constructAndSignVote, checkAndUpdatePreferredRound error.{}", checkRes.getErrMsg());
                return null;
            }

            // compare last vote round
            if (proposalEvent.getRound() <= safetyStorage.getLastVotedRound()) {
                logger.debug("constructAndSignVote error vote round: proposalEventRound[{}] , lastVotedRound[{}]", proposalEvent.getRound(), safetyStorage.getLastVotedRound());
                return null;
            }
            safetyStorage.setLastVotedRound(proposalEvent.getRound());

            VoteData voteData = VoteData.build(proposalEvent.buildEventInfo(voteProposal.getStateRoot(), voteProposal.getNumber(), voteProposal.getNextEpochState()), proposalEvent.getQuorumCert().getCertifiedEvent());
            return Vote.build(voteData, signer.getAuthor(), constructLedgerInfo(proposalEvent), signer);
        } finally {
            this.safetyRulesLock.writeLock().unlock();
        }
    }

   /**
    * Produces a LedgerInfo that either commits a event based upon the 3-chain commit rule
    * or an empty LedgerInfo for no commit. The 3-chain commit rule is: E0 (as well as its
    * prefix) can be committed if there exist certified events B1 and E2 that satisfy:
    * 1) E0 <- E1 <- E2 <--
    * 2) round(E0) + 1 = round(E1), and
    * 3) round(E1) + 1 = round(E2).
    */
    private LedgerInfo constructLedgerInfo(Event proposalEvent) {
        long event2 = proposalEvent.getRound();
        long event1 = proposalEvent.getQuorumCert().getCertifiedEvent().getRound();
        long event0 = proposalEvent.getQuorumCert().getParentEvent().getRound();

        boolean commit = event0 + 1 == event1 && event1 + 1 == event2;
        if (commit) {
            return LedgerInfo.build(proposalEvent.getQuorumCert().getParentEvent(), EMPTY_HASH_BYTES);
        } else {
            return LedgerInfo.build(EventInfo.empty(), EMPTY_HASH_BYTES);
        }
    }

    public Signature signTimeout(Timeout timeout) {
        try {
            this.safetyRulesLock.writeLock().lock();

            //verify epoch
            if (timeout.getEpoch() != safetyStorage.getEpoch()) {
                logger.debug("signTimeout, Incorrect Epoch!");
                return null;
            }

            // compare Preferred Round
            if (timeout.getRound() <= safetyStorage.getPreferredRound()) {
                logger.debug("timeout round does not match preferred round {} < {}", timeout.getRound(),
                        safetyStorage.getPreferredRound());
                return null;
            }

            // compare last vote Round
            if (timeout.getRound() < safetyStorage.getLastVotedRound()) {
                logger.debug("timeout round does not match last vote round {} < {}", timeout.getRound(),
                        safetyStorage.getLastVotedRound());
                return null;
            }

            if (timeout.getRound() > safetyStorage.getLastVotedRound()) {
                safetyStorage.setLastVotedRound(timeout.getRound());
            }

            return timeout.sign(signer);
        } finally {
            this.safetyRulesLock.writeLock().unlock();
        }
    }

    private ProcessResult<Void> checkAndUpdatePreferredRound(QuorumCert quorumCert) {
        long preferredRound = this.safetyStorage.getPreferredRound();
        long oneChainRound = quorumCert.getCertifiedEvent().getRound();
        long twoChainRound = quorumCert.getParentEvent().getRound();

        if (oneChainRound < preferredRound) {
            logger.debug( "QC round does not match preferred round {} < {}", oneChainRound, preferredRound);
            return ProcessResult.ofError("ProposalRoundLowerThenPreferredBlock error!");
        }

        if (twoChainRound > preferredRound) {
            this.safetyStorage.setPreferredRound(twoChainRound);
        }

        return ProcessResult.ofSuccess();
    }

    private ProcessResult<Void> verifyQC(QuorumCert qc) {
        if (this.epochState == null) {
            return ProcessResult.ofError("SafetyRules.epochState NotInitialized!");
        }

        return qc.verify(this.epochState.getValidatorVerifier());
    }
}
