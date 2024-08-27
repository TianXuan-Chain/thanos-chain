package com.thanos.chain.consensus.hotstuffbft.liveness;

import com.thanos.chain.txpool.TxnManager;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.consensus.hotstuffbft.store.EventTreeStore;
import com.thanos.chain.ledger.model.event.GlobalEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类ProposalGenerator.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 15:12:47
 */
public class ProposalGenerator {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    private static final int MAX_EMPTY_PULL_COUNT = 50;

    // public key
    private byte[] author;

    private EventTreeStore eventTreeStore;

    private TxnManager txnManager;

    private volatile Long lastRoundGenerated = Long.valueOf(0);

    private long packageTimeSleep;

    public ProposalGenerator(byte[] author, EventTreeStore eventTreeStore, TxnManager txnManager, long packageTimeSleep) {
        this.author = author;
        this.eventTreeStore = eventTreeStore;
        this.txnManager = txnManager;
        this.packageTimeSleep = packageTimeSleep;

    }

    public byte[] getAuthor() {
        return author;
    }

    public Event generateEmptyEvent(long round) {
        QuorumCert hqc = ensureHighestQuorumCert(round);
        return Event.buildEmptyEvent(round, hqc);
    }

    public EventData generateReconfigEmptySuffix(long round) {
        QuorumCert hqc = ensureHighestQuorumCert(round);
        return EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(), this.author, round, hqc.getCertifiedEvent().getTimestamp(), hqc);
    }

    public EventData generateProposal(long round) {
        synchronized (lastRoundGenerated) {
            if (lastRoundGenerated < round) {
                this.lastRoundGenerated = round;
            } else {
                logger.warn("Already proposed in the round {}", round);
                throw new RuntimeException("Already proposed in the round " + round);
            }
        }

        QuorumCert hqc = ensureHighestQuorumCert(round);

        if (hqc.getCertifiedEvent().hasReconfiguration()) {
            logger.debug("generateProposal empty event for hasReconfiguration");
            return generateReconfigEmptySuffix(round);
        }

        long parentTimestamp = hqc.getCertifiedEvent().getTimestamp();
        long createTimestamp = System.currentTimeMillis();
        createTimestamp = createTimestamp > parentTimestamp? createTimestamp: parentTimestamp + 1;


        boolean stateConsistent = this.eventTreeStore.isStateConsistent();
        Pair<GlobalEvent, ConsensusPayload> payloadPair = this.txnManager.pullEvent(stateConsistent);
        if (payloadPair.getLeft().getGlobalNodeEvents().length == 0 && payloadPair.getRight().getEthTransactions().length == 0) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
            }
        } else if (payloadPair.getLeft().getGlobalNodeEvents().length == 0 && payloadPair.getRight().getEthTransactions().length < 100000) {
            try {
                Thread.sleep(this.packageTimeSleep);
            } catch (InterruptedException e) {
            }
        }

        return EventData.buildProposal(payloadPair.getLeft(), payloadPair.getRight(), author, round, createTimestamp, hqc);
    }

    private QuorumCert ensureHighestQuorumCert(long round) {
        QuorumCert hqc = this.eventTreeStore.getHighestQuorumCert();
        Assert.assertTrue(String.format("Given round [%d] is lower than hqc round [%d]", round, hqc.getCertifiedEvent().getRound()), hqc.getCertifiedEvent().getRound() < round);
        Assert.assertTrue("The epoch has already ended,a proposal is not allowed to generated", !hqc.isEpochChange());
        return hqc;
    }
}
