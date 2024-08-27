package com.thanos.chain.consensus.hotstuffbft.store;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.TreeMap;


/**
 * 类PendingVotes.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 14:30:43
 */
public class PendingVotes {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    private HashMap<ByteArrayWrapper, LedgerInfoWithSignatures> liDigest2Votes;

    private Optional<TimeoutCertificate> maybePartialTc;

    private HashMap<ByteArrayWrapper, Vote> author2Vote;

    public PendingVotes() {
        liDigest2Votes = new HashMap<>();
        maybePartialTc = Optional.empty();
        author2Vote = new HashMap<>();
    }

    public VoteReceptionResult insertVote(Vote vote, ValidatorVerifier validatorVerifier) {

        ByteArrayWrapper author = new ByteArrayWrapper(ByteUtil.copyFrom(vote.getAuthor()));
        long round = vote.getVoteData().getProposed().getRound();
        byte[] liDigest = ByteUtil.copyFrom(vote.getLedgerInfo().getHash());
        Vote previouslySeenVote = this.author2Vote.get(author);
        if (previouslySeenVote != null) {

            if (Arrays.equals(liDigest, previouslySeenVote.getLedgerInfo().getHash())) {
                boolean newTimeoutVote = vote.isTimeout() && !previouslySeenVote.isTimeout();
                if (!newTimeoutVote) {
                    return VoteReceptionResult.ofDuplicateVote();
                }
            } else {
                logger.error("Validator {} sent two different votes for the same round {}!",
                        Hex.toHexString(author.getData()),
                        round);
                return VoteReceptionResult.ofEquivocateVote();
            }

        }

        // 2. Store new vote (or update, in case it's a new timeout vote)
        this.author2Vote.put(author, vote);

        // 3. Let's check if we can create a QC
        ByteArrayWrapper liDigestWrapper  = new ByteArrayWrapper(vote.getLedgerInfo().getHash());
        LedgerInfoWithSignatures liWithSig = liDigest2Votes.get(liDigestWrapper);
        if (liWithSig == null) {
            //logger.debug("aggregateQc build LedgerInfoWithSignatures:" + liWithSig);
            liWithSig = LedgerInfoWithSignatures.build(vote.getLedgerInfo(), new TreeMap());
            liDigest2Votes.put(liDigestWrapper, liWithSig);
        }

        liWithSig.addSignature(ByteUtil.copyFrom(vote.getAuthor()), vote.getSignature());
        // check if we have enough signatures to create a QC
        VerifyResult verifyResult = validatorVerifier.checkVotingPower(liWithSig.getSignatures().keySet());
        //logger.debug("current validatorVerifier info:" + validatorVerifier);
        long votingPower;
        switch (verifyResult.getStatus()) {
            case Success:
                liWithSig.reEncode();
                return VoteReceptionResult.ofNewQuorumCertificate(QuorumCert.build(vote.getVoteData(), liWithSig));
            case TooLittleVotingPower:
                votingPower = ((Pair<Long, Long>) verifyResult.getResult()).getLeft();
                break;
            default:
                logger.error("vote received from author[{}] could not be added: {}", Hex.toHexString(author.getData()), verifyResult.getStatus());
                //System.exit(1);
                return VoteReceptionResult.ofErrorAddingVote(verifyResult);
        }

        // 4. We couldn't form a QC, let's check if we can create a TC
        if (vote.isTimeout()) {
            Timeout timeout = vote.getTimeout();

            // if no partial TC exist, create one
            if (!maybePartialTc.isPresent()) {
                maybePartialTc = Optional.of(TimeoutCertificate.build(timeout, new TreeMap()));
            }

            // add the timeout signature
            maybePartialTc.get().addSignature(ByteUtil.copyFrom(vote.getAuthor()), vote.getTimeoutSignature().get());

            // did the TC reach a threshold?
            verifyResult = validatorVerifier.checkVotingPower(maybePartialTc.get().getSignatures().keySet());
            switch (verifyResult.getStatus()) {
                case Success:
                    return VoteReceptionResult.ofNewTimeoutCertificate(maybePartialTc.get());
                case TooLittleVotingPower:
                    break;
                default:
                    logger.error("MUST_FIX: Unexpected verification error, vote = {}", vote);
                    System.exit(1);
                    return VoteReceptionResult.ofErrorAddingVote(verifyResult);
            }

        }

        // 5. No QC (or TC) could be formed, return the QC's voting power
        return VoteReceptionResult.ofVoteAdded(votingPower);
    }
}
