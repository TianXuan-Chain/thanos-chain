package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.FastByteComparisons;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.chain.ledger.model.crypto.ValidatorSigner;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.ledger.model.store.Persistable;
import org.spongycastle.util.encoders.Hex;

import java.util.*;

/**
 * 类Event.java的实现描述：该类同时作为共识成功后，被commit 的数据模型
 *
 * @Author laiyiyu create on 2019-12-10 19:31:19
 */
public class Event extends Persistable {

    //equals EventData->getHash()
    private byte[] id;

    private EventData eventData;

    private Optional<Signature> signature;

    //@Transient
    private Map<Keyable, byte[]> deltaState;

    public Event(){super(null);}

    public Event(byte[] encode){super(encode);}

    public static  Event build(byte[] id, EventData eventData, Map<Keyable, byte[]> deltaState, Optional<Signature> signature) {
        Event event = new Event();
        event.id = id;
        event.eventData = eventData;
        event.signature = signature;
        event.deltaState = deltaState;
        event.rlpEncoded = event.rlpEncoded();
        return event;
    }

//    public static Event buildGenesisEvent() {
//        return buildGenesisEventFromLedgerInfo(LedgerInfo.buildGenesis(Constants.EMPTY_HASH_BYTES, null));
//    }

    public static Event buildGenesisEventFromLedgerInfo(LedgerInfo ledgerInfo) {
        EventData eventData = EventData.buildGenesisFromLedgerInfo(ledgerInfo);
        Event event = build(eventData.getHash(), eventData, null, Optional.empty());
        return event;
    }

    public static Event buildEmptyEvent(long round, QuorumCert qc) {
        EventData eventData = EventData.buildEmpty(round, qc);
        return build(eventData.getHash(), eventData, null, Optional.empty());
    }

//    public static  Event buildProposalEvent(ConsensusPayload payload, long round, long timestamp, QuorumCert quorumCert, ValidatorSigner signer) {
//        EventData eventData = EventData.buildProposal(payload, signer.getAuthor(), round, timestamp, quorumCert);
//        return buildProposalFromEventData(eventData, signer);
//    }

    public static  Event buildProposalFromEventData(EventData eventData, ValidatorSigner signer) {
        byte[] id = eventData.getHash();
        Optional<Signature> signature = signer.signMessage(id);
        //当生成共识Event作为签名时，无需写入deltaState。
        return  build(id, eventData, null, signature);
    }

    public EventInfo buildEventInfo(byte[] executedStateId, long number, Optional<EpochState> nextEpochState) {
        return EventInfo.build(getEpoch(), getRound(), getId(), executedStateId, number, getTimestamp(), nextEpochState);
    }

    public EventInfoWithSignatures buildEventInfoWithSignatures(byte[] executedStateId, long number, Optional<EpochState> nextEpochState, TreeMap<ByteArrayWrapper, Signature> signatures) {
        return EventInfoWithSignatures.build(getEpoch(), getRound(), getId(), executedStateId, number, getTimestamp(), nextEpochState, signatures);
    }

    public EventData getEventData() {
        return eventData;
    }

    public Optional<byte[]> getAuthor() {
        return this.eventData.getAuthor();
    }

    public long getEpoch() {
        return this.eventData.getEpoch();
    }

    public byte[] getId() {
        return id;
    }

    public boolean isParentOf(Event event) {
        return FastByteComparisons.equal(event.getId(), this.id);
    }

    public byte[] getParentId() {
        return this.eventData.getQuorumCert().getCertifiedEvent().getId();
    }

    public ConsensusPayload getPayload() {
        return this.eventData.getPayload();
    }

    public boolean hasGlobalEvents() {
        return !eventData.getGlobalEvent().isEmpty();
    }



    public QuorumCert getQuorumCert() {
        return this.eventData.getQuorumCert();
    }

    public long getRound() {
        return this.eventData.getRound();
    }

    public Optional<Signature> getSignature() {
        return signature;
    }

    public long getTimestamp() {
        return this.eventData.getTimestamp();
    }

    public long getEventNumber() {return this.eventData.getNumber(); }

    public boolean isGenesisEvent() {
        return this.eventData.getEventType() == EventData.EventType.GENESIS;
    }

    public boolean isEmptyEvent() {
        return this.eventData.getEventType() == EventData.EventType.EMPTY_EVENT;
    }


    public ProcessResult<Void> validateSignatures(ValidatorVerifier verifier) {
        switch (eventData.getEventType()) {
            case EventData.EventType.GENESIS:
                return ProcessResult.ofError("We should not accept genesis from others");
            case EventData.EventType.EMPTY_EVENT:
                return getQuorumCert().verify(verifier);
            case EventData.EventType.PROPOSAL:
                {
                    if(!signature.isPresent()) return ProcessResult.ofError("Missing signature in Proposal");
                    VerifyResult verifyResult = verifier.verifySignature(new ByteArrayWrapper(this.getAuthor().get()), id, signature.get());
                    if (!verifyResult.isSuccess()) {
                        return ProcessResult.ofError("verifier.verify error:" + verifyResult);
                    }
                    return this.getQuorumCert().verify(verifier);
                }
            default:
                return ProcessResult.ofError("un know event type");
        }
    }

    public ProcessResult<Void> verifyWellFormed() {
        if (isGenesisEvent()) {
            return ProcessResult.ofError("We must not accept genesis from others");
        }

        EventInfo parent = getQuorumCert().getCertifiedEvent();

        if (getRound() <= parent.getRound()) {
            return ProcessResult.ofError("event must have a greater round than parent's event");
        }

        if (getEpoch() != parent.getEpoch()) {
            return ProcessResult.ofError("event's parent should be in the same epoch");
        }

        if (parent.hasReconfiguration()) {
            if (!eventData.isEmptyPayload() || !eventData.getGlobalEvent().isEmpty()) {
                return ProcessResult.ofError("Reconfiguration suffix should not carry payload");
            }
        }

        if (isEmptyEvent() || parent.hasReconfiguration()) {
            if (getTimestamp() != parent.getTimestamp()) {
                return ProcessResult.ofError("empty/reconfig suffix block must have same timestamp as parent");
            }
        } else {
            if (getTimestamp() <= parent.getTimestamp()) {
                return ProcessResult.ofError("event must have strictly increasing timestamps");
            }
        }

        if (getQuorumCert().isEpochChange()) {
            return ProcessResult.ofError("Event cannot be proposed in an epoch that has Epoch Change");
        }

        if (!Arrays.equals(id, eventData.getHash())) {
            return ProcessResult.ofError("event id mismatch the hash");
        }

        return ProcessResult.ofSuccess();
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] id = RLP.encodeElement(this.id);
        byte[] eventData = this.eventData.getEncoded();
        byte[] signature = this.signature.isPresent()?RLP.encodeElement(this.signature.get().getSig()): ByteUtil.EMPTY_BYTE_ARRAY;
        return RLP.encodeList(id, eventData, signature);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.id = rlpDecode.get(0).getRLPData();
        this.eventData = new EventData(rlpDecode.get(1).getRLPData());
        if (rlpDecode.size() > 2) {
            this.signature = Optional.of(new Signature(rlpDecode.get(2).getRLPData()));
        } else {
            this.signature = Optional.empty();
        }
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + Hex.toHexString(id) +
                ", eventData=" + eventData +
                '}';
    }

    public void clear() {
        //id = null;
        eventData.clear();
        //signature = null;
    }
}
