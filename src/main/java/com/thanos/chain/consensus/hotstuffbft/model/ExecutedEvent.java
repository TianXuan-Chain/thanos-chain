package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.chain.ledger.model.store.Keyable;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * ExecutedEvent.java description：
 *
 * @Author laiyiyu create on 2020-03-04 15:48:24
 */
public class ExecutedEvent {

    Event event;

    ExecutedEventOutput executeOutput;

    TreeMap<ByteArrayWrapper, Signature> signatures;

    public ExecutedEvent(Event event, ExecutedEventOutput executeOutput) {
        this.event = event;
        this.executeOutput = executeOutput;
    }

    public Event getEvent() {
        return event;
    }

    public ExecutedEventOutput getExecutedEventOutput() { return executeOutput; }

    public Map<Keyable.DefaultKeyable, byte[]> getStateOutput() { return executeOutput.output; }

    public long getEventNumber() {
        return executeOutput.eventNumber;
    }

    public byte[] getStateRoot() {
        return executeOutput.stateRoot;
    }

    public long getEpoch() {
        return event.getEpoch();
    }

    public byte[] getId() {
        return event.getId();
    }

    public byte[] getParentId() {
        return event.getQuorumCert().getCertifiedEvent().getId();
    }

    public Optional<byte[]> getAuthor() {
        return event.getAuthor();
    }

//    public ConsensusPayload getPayload() {
//        return event.getPayload();
//    }

    public long getTimestamp() {
        return event.getTimestamp();
    }

    public long getRound() {
        return event.getRound();
    }

    public EventInfo getEventInfo() {
        return event.buildEventInfo(executeOutput.stateRoot, executeOutput.eventNumber, executeOutput.epochState);
    }

    public EventInfoWithSignatures getEventInfoWithSignatures() {
        return event.buildEventInfoWithSignatures(executeOutput.stateRoot, executeOutput.eventNumber, executeOutput.epochState, signatures);
    }

    public TreeMap<ByteArrayWrapper, Signature> getSignatures() {
        return signatures;
    }

    public void setSignatures(TreeMap<ByteArrayWrapper, Signature> signatures) {
        this.signatures = signatures;
    }

    public void clear() {
        event = null;
        //event.clear();
//        executeOutput.clear();
//        signatures = null;
        //signatures.clear();
    }

    @Override
    public String toString() {
        return "ExecutedEvent{" +
                "event=" + event +
                '}';
    }
}
