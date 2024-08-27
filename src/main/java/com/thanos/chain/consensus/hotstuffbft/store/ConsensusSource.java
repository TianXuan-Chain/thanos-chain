package com.thanos.chain.consensus.hotstuffbft.store;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.consensus.hotstuffbft.model.Event;
import com.thanos.chain.consensus.hotstuffbft.model.QuorumCert;
import com.thanos.chain.consensus.hotstuffbft.model.TimeoutCertificate;
import com.thanos.chain.consensus.hotstuffbft.model.Vote;
import com.thanos.chain.ledger.model.store.DefaultValueable;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.chain.storage.datasource.AbstractDbSource;
import com.thanos.chain.storage.datasource.DbSettings;
import com.thanos.chain.storage.datasource.inmem.CacheDbSource;
import com.thanos.chain.storage.datasource.rocksdb.RocksDbSource;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 类ConsensusDB.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 15:07:48
 */
public class ConsensusSource {

    private static final Map<String, Class<? extends Persistable>> COLUMN_FAMILIES = new HashMap() {{
        put("quorum_cert", QuorumCert.class);
        put("event", Event.class);
        put("single_entry", DefaultValueable.class);
    }};

    private final AbstractDbSource db;

    private SystemConfig systemConfig;

    private List<Pair<Keyable, Persistable>> flushCache = new ArrayList<>(131107);

    public ConsensusSource(boolean test,SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
        if (test)  {
            db = new CacheDbSource();
        } else {
            db = new RocksDbSource("consensus", COLUMN_FAMILIES, this.systemConfig, DbSettings.newInstance().withMaxOpenFiles(systemConfig.getConsensusMaxOpenFiles()).withMaxThreads(systemConfig.getConsensusMaxThreads()).withWriteBufferSize(systemConfig.getConsensusWriteBufferSize()));
        }
    }

    static Keyable LastVoteMsg = Keyable.ofDefault("LastVoteMsg".getBytes());
    public void saveLastVoteMsg(Vote lastVote) {
        db.put(LastVoteMsg, new DefaultValueable(lastVote.getEncoded()));
    }

    public Optional<Vote> getLastVoteMsg() {
        byte[] raw = db.getRaw(DefaultValueable.class, LastVoteMsg);
        if (raw == null) {
            return Optional.empty();
        } else {
            return Optional.of(new Vote(raw));
        }
    }

    public void deleteLastVoteMsg() {
        db.put(LastVoteMsg, new DefaultValueable(null));
    }

    static Keyable HighestTimeoutCertificate = Keyable.ofDefault("HighestTimeoutCertificate".getBytes());
    public void saveHighestTimeoutCertificate(TimeoutCertificate highestTimeoutCertificate) {
        db.put(HighestTimeoutCertificate, new DefaultValueable(highestTimeoutCertificate.getEncoded()));
    }

    public Optional<TimeoutCertificate> getHighestTimeoutCertificate() {
        byte[] raw = db.getRaw(DefaultValueable.class, HighestTimeoutCertificate);
        if (raw == null) {
            return Optional.empty();
        } else {
            return Optional.of(new TimeoutCertificate(raw));
        }
    }

    public void deleteHighestTimeoutCertificate() {
        db.put(HighestTimeoutCertificate, new DefaultValueable(null));
    }

    public void saveEventsAndQuorumCertificates(List<Event> events, List<QuorumCert> qcs) {
        List<Pair<Keyable, Persistable>> saveBatch = new ArrayList<>(events.size() + qcs.size());
        for (Event event: events) {
            saveBatch.add(Pair.of(Keyable.ofDefault(event.getId()), event));
        }

        for (QuorumCert qc: qcs) {
            saveBatch.add(Pair.of(Keyable.ofDefault(qc.getCertifiedEvent().getId()), qc));
        }

        db.updateBatch(saveBatch);
    }

    public void deleteEventsAndQuorumCertificates(List<byte[]> eventIds) {
        List<Pair<Keyable, Persistable>> saveBatch = new ArrayList<>(eventIds.size() * 2);
        for (byte[] eventId: eventIds) {
            Keyable keyable = Keyable.ofDefault(eventId);
            saveBatch.add(Pair.of(keyable, new Event()));
            saveBatch.add(Pair.of(keyable, new QuorumCert()));
        }
        db.updateBatch(saveBatch);
    }

    //get all consensus events
    public List<Event> getAllEvents() {
        return db.getAll(Event.class).stream().map(persistable -> (Event) persistable).collect(Collectors.toList());
    }

    public List<QuorumCert> getAllQuorumCerts() {
        return db.getAll(QuorumCert.class).stream().map(persistable -> (QuorumCert) persistable).collect(Collectors.toList());
    }
}
