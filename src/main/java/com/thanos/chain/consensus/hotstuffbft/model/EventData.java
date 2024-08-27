package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.chain.config.Constants;
import com.thanos.common.crypto.CryptoHash;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.executor.dag.ExecuteRoot;
import com.thanos.chain.ledger.model.event.GlobalEvent;
import com.thanos.chain.ledger.model.store.Persistable;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * 类EventData.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 17:16:36
 */
public class EventData extends Persistable implements CryptoHash {

    public static class  EventType {

        public static final int PROPOSAL = 1;

        public static final int EMPTY_EVENT = 2;

        public static final int GENESIS = 3;
    }

    private long epoch;

    private long round;

    private long number;

    private long timestamp;

    // parent qc
    private QuorumCert quorumCert;

    GlobalEvent globalEvent;

    private ConsensusPayload payLoad;

    private int eventType;

    boolean emptyPayload;

    private Optional<byte[]> author;

    // event id
    byte[] transientHash;

    //for speed
    /* Transactions */
    private List<ExecuteRoot> dagExecuteRoots;

    /* Transactions */
    private EventDataDsCheckResult eventDataDsCheckResult;


    private EventData(){super(null);}

    public EventData(byte[] encode) {super(encode);}


    public static EventData buildWithoutDecode(byte[] encode) {
        EventData eventData = new EventData();
        eventData.rlpEncoded = encode;
        return eventData;
    }

    /**
     * 这里同时是 epoch 发生变化的根本，epoch = quorumCert.getCertifiedEvent().getEpoch() + 1
     */
    public static EventData buildGenesis(long timestamp, QuorumCert quorumCert) {
        EventData eventData = new EventData();
        eventData.epoch = quorumCert.getCertifiedEvent().getEpoch() + 1;
        eventData.round = 0;
        eventData.number = quorumCert.getCertifiedEvent().getNumber();
        eventData.timestamp = timestamp;
        eventData.quorumCert = quorumCert;
        eventData.globalEvent = new GlobalEvent();
        eventData.payLoad = new ConsensusPayload(); // empty ConsensusPayload;
        eventData.emptyPayload = true;
        eventData.eventType = EventType.GENESIS;
        eventData.author = Optional.empty();
        eventData.rlpEncoded = eventData.rlpEncoded();
        //eventData.transientHash = HashUtil.sha3(eventData.rlpEncoded);
        eventData.transientHash = HashUtil.sha3Dynamic(
                ByteUtil.longToBytes(eventData.epoch),
                ByteUtil.longToBytes(eventData.round),
                ByteUtil.longToBytes(eventData.number),
                ByteUtil.longToBytes(eventData.timestamp),
                eventData.quorumCert.getEncoded(),
                eventData.globalEvent.getEncoded(),
                HashUtil.sha3Light(eventData.payLoad.getEncoded()),
                ByteUtil.intToBytes(eventData.eventType),
                eventData.author.isPresent()? eventData.author.get(): EMPTY_BYTE_ARRAY);

        return eventData;
    }

    public static EventData buildGenesisFromLedgerInfo(LedgerInfo ledgerInfo) {
        assert ledgerInfo.getNextEpochState().isPresent();
        EventInfo ancestor = EventInfo.build(   // build parent event
                ledgerInfo.getEpoch(),
                0,
                Constants.EMPTY_HASH_BYTES,
                ledgerInfo.getExecutedStateId(),
                ledgerInfo.getNumber(),
                ledgerInfo.getTimestamp(),
                Optional.empty()
        );

        // Genesis carries a placeholder quorum certificate to its parent id with LedgerInfo
        // carrying information about version from the last LedgerInfo of previous epoch.
        QuorumCert genesisQC = QuorumCert.build(
                VoteData.build(ancestor, ancestor),
                LedgerInfoWithSignatures.build(LedgerInfo.build(ancestor, Constants.EMPTY_HASH_BYTES), new TreeMap<>())
        );

        return EventData.buildGenesis(ledgerInfo.getTimestamp(), genesisQC);
    }

    public static EventData buildEmpty(long round, QuorumCert quorumCert) {
        EventData eventData = new EventData();
        eventData.epoch = quorumCert.getCertifiedEvent().getEpoch();
        eventData.round = round;
        eventData.number = quorumCert.getCertifiedEvent().getNumber(); // empty event not change the number
        eventData.timestamp = quorumCert.getCertifiedEvent().getTimestamp();
        eventData.quorumCert = quorumCert;
        eventData.globalEvent = new GlobalEvent();
        eventData.payLoad = new ConsensusPayload(); // empty ConsensusPayload;
        eventData.emptyPayload = true;
        eventData.eventType = EventType.EMPTY_EVENT;
        eventData.author = Optional.empty();
        eventData.rlpEncoded = eventData.rlpEncoded();
        eventData.transientHash = HashUtil.sha3Dynamic(
                ByteUtil.longToBytes(eventData.epoch),
                ByteUtil.longToBytes(eventData.round),
                ByteUtil.longToBytes(eventData.number),
                ByteUtil.longToBytes(eventData.timestamp),
                eventData.quorumCert.getEncoded(),
                eventData.globalEvent.getEncoded(),
                HashUtil.sha3Light(eventData.payLoad.getEncoded()),
                ByteUtil.intToBytes(eventData.eventType),
                eventData.author.isPresent()? eventData.author.get(): EMPTY_BYTE_ARRAY);

        //eventData.transientHash = HashUtil.sha3(eventData.rlpEncoded);
        return eventData;
    }

    public static  EventData buildProposal(GlobalEvent globalEvent, ConsensusPayload payload, byte[] author, long round, long timestamp, QuorumCert quorumCert) {
        EventData eventData = new EventData();
        eventData.epoch = quorumCert.getCertifiedEvent().getEpoch();
        eventData.round = round;
        eventData.number = payload.isEmpty() && globalEvent.isEmpty()?  // empty event not change the number
                quorumCert.getCertifiedEvent().getNumber() : quorumCert.getCertifiedEvent().getNumber() + 1;
        eventData.timestamp = timestamp;
        eventData.quorumCert = quorumCert;
        eventData.globalEvent = globalEvent;
        eventData.payLoad = payload;
        eventData.emptyPayload = payload.isEmpty();
        eventData.eventType = EventType.PROPOSAL;
        eventData.author = Optional.of(ByteUtil.copyFrom(author));
        eventData.rlpEncoded = eventData.rlpEncoded();

        eventData.transientHash = HashUtil.sha3Dynamic(
                ByteUtil.longToBytes(eventData.epoch),
                ByteUtil.longToBytes(eventData.round),
                ByteUtil.longToBytes(eventData.number),
                ByteUtil.longToBytes(eventData.timestamp),
                eventData.quorumCert.getEncoded(),
                eventData.globalEvent.getEncoded(),
                HashUtil.sha3Light(eventData.payLoad.getEncoded()),
                ByteUtil.intToBytes(eventData.eventType),
                eventData.author.isPresent()? eventData.author.get(): EMPTY_BYTE_ARRAY);

        //eventData.transientHash = HashUtil.sha3(eventData.rlpEncoded);
        return eventData;
    }

    public Optional<byte[]> getAuthor() {
        if (this.eventType == EventType.PROPOSAL) {
            return this.author;
        } else {
            return Optional.empty();
        }
    }

    public GlobalEvent getGlobalEvent() {
        return globalEvent;
    }

    public boolean isEmptyPayload() {
        return emptyPayload;
    }

    public ConsensusPayload getPayload() {
        return payLoad;
    }

    public long getEpoch() {
        return epoch;
    }

    public long getRound() {
        return round;
    }

    public long getNumber() { return number; }

    public long getTimestamp() {
        return timestamp;
    }

    public QuorumCert getQuorumCert() { return quorumCert; }

    public int getEventType() {
        return eventType;
    }



    //public boolean isEmptyPayload() {return payLoad.isGlobalNodeEventsEmpty();}

    public byte[] getParentId() {
        return this.quorumCert.getCertifiedEvent().getId();
    }

    public List<ExecuteRoot> getDagExecuteRoots() {
        return dagExecuteRoots;
    }

    public void setDagExecuteRoots(List<ExecuteRoot> dagExecuteRoots) {
        this.dagExecuteRoots = dagExecuteRoots;
    }

    public boolean allEmpty() {
        return isEmptyPayload() && globalEvent.isEmpty();
    }

    public boolean isGenesisEvent() {
        return this.eventType == EventType.GENESIS;
    }

    public boolean isEmptyEvent() {
        return this.eventType == EventType.EMPTY_EVENT;
    }

    public EventDataDsCheckResult getEventDataDsCheckResult() {
        return eventDataDsCheckResult;
    }

    public void setEventDataDsCheckResult(EventDataDsCheckResult eventDataDsCheckResult) {
        this.eventDataDsCheckResult = eventDataDsCheckResult;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] epoch = RLP.encodeBigInteger(BigInteger.valueOf(this.epoch));
        byte[] round = RLP.encodeBigInteger(BigInteger.valueOf(this.round));
        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        byte[] timestamp = RLP.encodeBigInteger(BigInteger.valueOf(this.timestamp));
        byte[] quorumCert = this.quorumCert.getEncoded();
        byte[] globalEvent = this.globalEvent.getEncoded();
        byte[] payLoad = this.payLoad.getEncoded();
        byte[] emptyPayload = RLP.encodeInt(this.emptyPayload? 1 : 0);
        byte[] eventType = RLP.encodeInt(this.eventType);
        byte[] author = this.author.isPresent()? RLP.encodeElement(this.author.get()): EMPTY_BYTE_ARRAY;
        return RLP.encodeList(epoch, round, number, timestamp, quorumCert, globalEvent, payLoad, emptyPayload, eventType, author);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.epoch = ByteUtil.byteArrayToLong(rlpDecode.get(0).getRLPData());
        this.round = ByteUtil.byteArrayToLong(rlpDecode.get(1).getRLPData());
        this.number = ByteUtil.byteArrayToLong(rlpDecode.get(2).getRLPData());
        this.timestamp = ByteUtil.byteArrayToLong(rlpDecode.get(3).getRLPData());
        this.quorumCert = new QuorumCert(rlpDecode.get(4).getRLPData());
        this.globalEvent = new GlobalEvent(rlpDecode.get(5).getRLPData());
        this.payLoad = ConsensusPayload.buildNoEncode(rlpDecode.get(6).getRLPData());
        this.emptyPayload = (ByteUtil.byteArrayToInt(rlpDecode.get(7).getRLPData()) == 1);
        this.eventType = ByteUtil.byteArrayToInt(rlpDecode.get(8).getRLPData());
        if (rlpDecode.size() > 9) {
            this.author = Optional.of(rlpDecode.get(9).getRLPData());
        } else {
            this.author = Optional.empty();
        }

        this.transientHash = HashUtil.sha3Dynamic(
                ByteUtil.longToBytes(this.epoch),
                ByteUtil.longToBytes(this.round),
                ByteUtil.longToBytes(this.number),
                ByteUtil.longToBytes(this.timestamp),
                this.quorumCert.getEncoded(),
                this.globalEvent.getEncoded(),
                HashUtil.sha3Light(this.payLoad.getEncoded()),
                ByteUtil.intToBytes(this.eventType),
                this.author.isPresent()? this.author.get(): EMPTY_BYTE_ARRAY);
        //this.transientHash = HashUtil.sha3(this.rlpEncoded);
    }

    @Override
    public byte[] getHash() {
        return transientHash;
    }

    @Override
    public String toString() {
        return "EventData{" +
                "epoch=" + epoch +
                ", round=" + round +
                ", number=" + number +
                ", timestamp=" + timestamp +
                ", quorumCert for votedata=" + quorumCert.getVoteData() +
                ", quorumCert for ledger=" + quorumCert.getLedgerInfoWithSignatures() +
                ", payLoad=" + payLoad +
                ", globalEvent=" + globalEvent +
                ", eventType=" + eventType +
 //               ", author=" + Hex.toHexString(author.isPresent()?author.get(): ByteUtil.EMPTY_BYTE_ARRAY) +
                ", transientHash=" + Hex.toHexString(transientHash) +
                '}';
    }

    public void clear() {
        //payLoad = null;
        //quorumCert.clear();
//        quorumCert = null;
//        globalEvent.clear();
//        globalEvent = null;
//        payLoad.clear();
//        payLoad = null;
//        author = null;

    }
}
