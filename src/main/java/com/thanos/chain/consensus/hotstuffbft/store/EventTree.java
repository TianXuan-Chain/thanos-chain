package com.thanos.chain.consensus.hotstuffbft.store;

import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.txpool.TxnManager;
import com.thanos.common.utils.ByteUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.*;

/**
 * 类EventTree.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 14:02:49
 */
public class EventTree {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    protected static class LinkableEvent {

        public LinkableEvent(ExecutedEvent executedEvent) {
            this.executedEvent = executedEvent;
            this.children = new HashSet<>();
        }

        ExecutedEvent executedEvent;

        Set<ByteArrayWrapper> children;

        boolean reimport = false;

        public void addChild(byte[] childEventId) {
            children.add(new ByteArrayWrapper(childEventId));
        }

        public byte[] getEventId() {
            return this.executedEvent.getId();
        }

        public ExecutedEvent getExecutedEvent() {
            return executedEvent;
        }

        public boolean hasReimport() {
            return reimport;
        }

        public void setImport() {
            this.reimport = true;
        }

        public void clear() {
//            children.clear();
            executedEvent.clear();
//            executedEvent = null;
//            children = null;
        }

        @Override
        public String toString() {
            return "LinkableEvent{" +
                    "self =" + Hex.toHexString(executedEvent.getId()) +
                    ", parent =" + Hex.toHexString(executedEvent.getParentId()) +
                    ", children=" + children +
                    '}';
        }
    }

    TxnManager txnManager;

    HashMap<ByteArrayWrapper, LinkableEvent> id2Event;

    byte[] rootId;

    byte[] highestCertifiedEventId;

    QuorumCert highestQuorumCert;

    Optional<TimeoutCertificate> highestTimeoutCert;

    QuorumCert highestCommitCert;

    HashMap<ByteArrayWrapper, QuorumCert> id2QC;

    LinkedList<byte[]> prunedEventIds;

    // 这里需要依据系统的内存大小定，不能太大，如果太大
    // 并且每一个event的占用的内存太大的情况下，会应无法及时gc而导致oom。
    int max_pruned_events_in_mem;

    boolean reimportUnCommitEvent;

    public EventTree(TxnManager txnManager, ExecutedEvent rootEvent, QuorumCert rootQC, QuorumCert rootLeaderInfo, Optional<TimeoutCertificate> highestTimeoutCert, int max_pruned_events_in_mem, boolean reimportUnCommitEvent) {
        Assert.assertTrue("inconsistent root and ledger info", Arrays.equals(rootEvent.getId(), rootLeaderInfo.getCommitEvent().getId()));
        this.max_pruned_events_in_mem = max_pruned_events_in_mem < 10? 10: max_pruned_events_in_mem;
        this.reimportUnCommitEvent = reimportUnCommitEvent;
        this.txnManager = txnManager;
        this.rootId = rootEvent.getId();
        this.highestCertifiedEventId = rootEvent.getId();

        this.id2Event = new HashMap<>();
        this.id2Event.put(new ByteArrayWrapper(rootEvent.getId()), new LinkableEvent(rootEvent));

        this.highestQuorumCert = rootQC;
        this.highestCommitCert = rootLeaderInfo;
        this.highestTimeoutCert = highestTimeoutCert;

        this.id2QC = new HashMap<>();
        id2QC.put(new ByteArrayWrapper(rootQC.getCertifiedEvent().getId()), rootQC);

        prunedEventIds = new LinkedList<>();

        logger.info("event tree build success! max_pruned_events_in_mem:{}, reimportUnCommitEvent:{}", this.max_pruned_events_in_mem, this.reimportUnCommitEvent);
    }

    public QuorumCert getHighestQuorumCert() {
        return highestQuorumCert;
    }

    public Optional<TimeoutCertificate> getHighestTimeoutCert() {
        return highestTimeoutCert;
    }

    public QuorumCert getHighestCommitCert() {
        return highestCommitCert;
    }

    public ExecutedEvent getHighestCertifiedEvent() {
        return getEvent(this.highestCertifiedEventId);
    }

    private LinkableEvent getLinkableEvent(byte[] eventId) {
        return id2Event.get(new ByteArrayWrapper(eventId));
    }

    public LinkableEvent getRootLinkableEvent() {
        return getLinkableEvent(rootId);
    }

    public ExecutedEvent getRootEvent() {
        return getEvent(this.rootId);
    }

    public ExecutedEvent getEvent(byte[] eventId) {
        LinkableEvent linkableEvent = getLinkableEvent(eventId);
        return linkableEvent == null? null: linkableEvent.executedEvent;
    }

    public void doRemoveCommitEvent(LinkedList<ByteArrayWrapper> orderCommitPathEvents) {
        int leftSize = orderCommitPathEvents.size() - this.max_pruned_events_in_mem;
        if (leftSize > 0) {
            for (int i = 0; i < leftSize; i++) {
                ByteArrayWrapper id = orderCommitPathEvents.pollLast();
                LinkableEvent removeEvent = id2Event.remove(id);
                removeEvent.clear();
                // help gc
                QuorumCert quorumCert = id2QC.remove(id);

                //quorumCert.clear();
            }
        }
    }

    public void doRemoveUnCommitEvent(Set<ByteArrayWrapper> unCommitPathEvents) {
        if (CollectionUtils.isEmpty(unCommitPathEvents)) return;

        //Map<ByteArrayWrapper, EventData> unCommitEvents = new HashMap<>(8);
        for (ByteArrayWrapper id: unCommitPathEvents) {
            QuorumCert quorumCert = id2QC.remove(id);
            LinkableEvent removeEvent = id2Event.remove(id);
            Event event = removeEvent.executedEvent.getEvent();
            removeEvent.clear();
            if ((!event.getEventData().isEmptyPayload() || event.hasGlobalEvents())) {

                if (!reimportUnCommitEvent) {
                    this.txnManager.doRemoveCheck(event.getEventData());
                    continue;
                }

                if (!removeEvent.hasReimport()) {
                    logger.info("reimport un commit event[{}]", id);
                    this.txnManager.doImportUnCommitEvents(event.getEventData());
                }
            }
        }
    }


    public boolean eventExists(byte[] eventId) {
        return id2Event.containsKey(new ByteArrayWrapper(eventId));
    }

    public void replaceTimeoutCert(TimeoutCertificate timeoutCertificate) {
        this.highestTimeoutCert = Optional.of(timeoutCertificate);
    }

    public void reimportTimeoutEvent() {
        if (!reimportUnCommitEvent) return;

        ByteArrayWrapper highestId = new ByteArrayWrapper(ByteUtil.copyFrom(highestQuorumCert.getCertifiedEvent().getId()));
        Map<ByteArrayWrapper, EventData> reimportEvents = new HashMap();

        LinkedList<ByteArrayWrapper> eventsToBeRemove = new LinkedList<>();
        eventsToBeRemove.addAll(getLinkableEvent(this.rootId).children);
        while (eventsToBeRemove.peek() != null) {
            ByteArrayWrapper id = eventsToBeRemove.poll();
            LinkableEvent currentEvent = id2Event.get(id);
            if (currentEvent == null) {
                continue;
            }
            eventsToBeRemove.addAll(currentEvent.children);

            EventData eventData = currentEvent.executedEvent.getEvent().getEventData();
            //if (!eventData.allEmpty()) {
                //logger.info("reimportTimeoutEvent normal [{}]", Hex.toHexString(eventData.getHash()));
                //this.txnManager.removeEvent(eventData);
            reimportEvents.put(new ByteArrayWrapper(eventData.getHash()), eventData);
            //}
        }

        LinkableEvent currentEvent = id2Event.get(highestId);
        //currentEvent.children.clear();
        while (!Arrays.equals(currentEvent.executedEvent.getEvent().getId(), rootId)) {
            ByteArrayWrapper currentId = new ByteArrayWrapper(currentEvent.executedEvent.getEvent().getId());
            reimportEvents.remove(currentId);
            currentEvent = id2Event.get(new ByteArrayWrapper(currentEvent.executedEvent.getEvent().getParentId()));
        }


        if (reimportEvents.size() != 0) {

            for (ByteArrayWrapper id: reimportEvents.keySet()) {
                //QuorumCert quorumCert = id2QC.remove(id);
                LinkableEvent reimportEvent = id2Event.get(id);
                if (reimportEvent.hasReimport()) {
                    continue;
                }
                logger.info("do reimport timeout event[{}]!", id);
                reimportEvent.setImport();
                this.txnManager.doImportUnCommitEvents(reimportEvent.executedEvent.getEvent().getEventData());
                //removeEvent.clear();
            }
        }
    }

    public QuorumCert getQCForEvent(byte[] eventId) {
        return this.id2QC.get(new ByteArrayWrapper(eventId));
    }

    public Pair<QuorumCert, List<Event>> getThreeChainCommitPair() {
        Event rootEvent = getRootEvent().getEvent();
        QuorumCert highestCommitCert = null;
        for (QuorumCert quorumCert: id2QC.values()) {
            if (Arrays.equals(rootEvent.getId(), quorumCert.getCommitEvent().getId())) {
                highestCommitCert = quorumCert;
                break;
            }
        }

        Event highestCommitCertOneEvent = getEvent(highestCommitCert.getCertifiedEvent().getId()).getEvent();
        Event highestCommitCertTwoEvent = getEvent(highestCommitCert.getParentEvent().getId()).getEvent();
        return Pair.of(highestCommitCert, Arrays.asList(highestCommitCertOneEvent, highestCommitCertTwoEvent, rootEvent));
    }

    public ExecutedEvent insertEvent(ExecutedEvent event) {
        ExecutedEvent existingEvent = this.getEvent(event.getId());
        if (existingEvent != null) {
            logger.debug("Already had event {} when trying to add another event {} for the same id", existingEvent, event);
            return existingEvent;
        }

        LinkableEvent parentLinkableEvent = getLinkableEvent(event.getParentId());

        Assert.assertTrue(String.format("Parent event [%s] not found", Hex.toHexString(event.getParentId())), parentLinkableEvent != null);
        parentLinkableEvent.addChild(event.getId());

        LinkableEvent linkableEvent = new LinkableEvent(event);
        id2Event.put(new ByteArrayWrapper(event.getId()), linkableEvent);
        return event;
    }

    public void insertQC(QuorumCert qc) {
        byte[] eventId = qc.getCertifiedEvent().getId();

        // Safety invariant: For any two quorum certificates qc1, qc2 in the block store,
        // qc1 == qc2 || qc1.round != qc2.round
        // The invariant is quadratic but can be maintained in linear time by the check
        // below.
        Assert.assertTrue
                (this.id2QC.entrySet().stream()
                .allMatch(entry ->
                        Arrays.equals(entry.getValue().getLedgerInfoWithSignatures().getLedgerInfo().getConsensusDataHash(), qc.getLedgerInfoWithSignatures().getLedgerInfo().getConsensusDataHash())
                                || entry.getValue().getCertifiedEvent().getRound() != qc.getCertifiedEvent().getRound()
                        )
                );

        ExecutedEvent event = this.getEvent(eventId);

        Assert.assertTrue(String.format("event [%s] not found", Hex.toHexString(eventId)), event != null);

        if (event.getRound() > this.getHighestCertifiedEvent().getRound()) {
            this.highestCertifiedEventId = event.getId();
            this.highestQuorumCert = qc;
        }

        ByteArrayWrapper eventIdWrapper = new ByteArrayWrapper(eventId);
        if (!this.id2QC.containsKey(eventIdWrapper)) {
            this.id2QC.put(new ByteArrayWrapper(eventId), qc);
        }

        if (this.highestCommitCert.getCommitEvent().getRound() < qc.getCommitEvent().getRound()) {
            this.highestCommitCert = qc;
        }
    }

    public Set<ByteArrayWrapper> findEventToPrune(byte[] nextRootId) {
        if (Arrays.equals(nextRootId, this.rootId)) {
            return Collections.emptySet();
        }

        Set<ByteArrayWrapper> eventsPruned = new HashSet<ByteArrayWrapper>(id2Event.size());
        eventsPruned.addAll(id2Event.keySet());

        LinkedList<LinkableEvent> eventsToBeKeep = new LinkedList<>();

        eventsToBeKeep.add(getLinkableEvent(nextRootId));

        while (eventsToBeKeep.peek() != null) {
            LinkableEvent eventToKeep = eventsToBeKeep.pop();
            eventsPruned.remove(new ByteArrayWrapper(eventToKeep.executedEvent.getId()));
            Iterator<ByteArrayWrapper> iterator = eventToKeep.children.iterator();

            while (iterator.hasNext()) {
                ByteArrayWrapper child = iterator.next();
                eventsPruned.remove(child);
                //if (Arrays.equals(nextRootId, child.getData())) continue;
                eventsToBeKeep.add(this.getLinkableEvent(child.getData()));
            }
        }

        return eventsPruned;
    }

    public void processPrunedEvents(byte[] nextRootId, Set<ByteArrayWrapper> newlyPrunedEvents) {
        Assert.assertTrue(this.eventExists(rootId));
        Set<ByteArrayWrapper> unCommitPathEvents = newlyPrunedEvents;
        LinkedList<ByteArrayWrapper> orderCommitPathEvents = new LinkedList();

        LinkableEvent nextRootEvent = getLinkableEvent(nextRootId);
        LinkableEvent parentEvent = getLinkableEvent(nextRootEvent.executedEvent.getParentId());
        while (true) {
            if (parentEvent == null) {
                break;
            }

            ByteArrayWrapper currentId = new ByteArrayWrapper(parentEvent.getEventId());
            orderCommitPathEvents.addLast(currentId);
            unCommitPathEvents.remove(currentId);

            if (Arrays.equals(parentEvent.getEventId(), parentEvent.executedEvent.getParentId())) {
                break;
            }
            parentEvent = getLinkableEvent(parentEvent.executedEvent.getParentId());
        }

        if (logger.isTraceEnabled()) {
            printTrace(nextRootId, orderCommitPathEvents, unCommitPathEvents);
        }

        doRemoveTxnPoolCommitEvent(nextRootId);
        // Update the next root
        this.rootId = nextRootId;

        //this.prunedEventIds.addAll(newlyPrunedEvents);
        this.doRemoveUnCommitEvent(unCommitPathEvents);
        this.doRemoveCommitEvent(orderCommitPathEvents);
    }

    private void doRemoveTxnPoolCommitEvent(byte[] nextRootId) {
        LinkableEvent nextRootEvent = getLinkableEvent(nextRootId);
        if (!nextRootEvent.executedEvent.getEvent().getEventData().allEmpty()) {
            this.txnManager.doRemoveCheck(nextRootEvent.executedEvent.getEvent().getEventData());
        }

        LinkableEvent parentEvent = getLinkableEvent(nextRootEvent.executedEvent.getParentId());
        while (parentEvent != null && !Arrays.equals(parentEvent.executedEvent.getId(), this.rootId)) {
            if (!parentEvent.executedEvent.getEvent().getEventData().allEmpty()) {
                this.txnManager.doRemoveCheck(parentEvent.executedEvent.getEvent().getEventData());
            }
            parentEvent = getLinkableEvent(parentEvent.executedEvent.getParentId());
        }
    }

    public List<ExecutedEvent> pathFromRoot(byte[] eventId) {
        byte[] currentId = eventId;
        List<ExecutedEvent> result = new ArrayList<>(8);
        while (true) {
            ExecutedEvent event = this.getEvent(currentId);
            if (event == null) {
                return Collections.emptyList();
            }

            if (event.getRound() <= this.getRootEvent().getRound()) break;

            currentId = event.getParentId();
            result.add(event);
        }

        // At this point cur_block.round() <= self.root.round()
        if (!Arrays.equals(rootId, currentId)) return Collections.emptyList();

        Collections.reverse(result);
        return result;
    }

    public Set<ByteArrayWrapper> getAllEventId() {
        return this.id2Event.keySet();
    }

    public int getLinkableEventsSize() {
        int result = 0;
        LinkedList<LinkableEvent> toVisit = new LinkedList<>();
        toVisit.push(getRootLinkableEvent());
        while (toVisit.peek() != null) {
            result++;
            LinkableEvent current = toVisit.pop();
            for (ByteArrayWrapper childId: current.children) {
                toVisit.push(getLinkableEvent(childId.getData()));
            }
        }
        return result;
    }

    public int getChildLinkableEventsSize() {
        return this.getLinkableEventsSize() - 1;
    }

    public int getPrunedEventsInMemSize() {
        return this.prunedEventIds.size();
    }

    public void printTrace(byte[] nextRootId, LinkedList<ByteArrayWrapper> orderCommitPathEvents, Set<ByteArrayWrapper> unCommitPathEvents) {
        logger.trace("event trace trace, total countL[{}],  current rootId:[{}], nextRootId[{}]", id2Event.size(), Hex.toHexString(rootId), Hex.toHexString(nextRootId));
        for (LinkableEvent linkableEvent: id2Event.values()) {
            logger.trace("event trace trace, event tree element:[{}]", linkableEvent);
        }

        StringBuilder orderCommitPathEventsContent = new StringBuilder("event trace trace, current commit event[");
        for (ByteArrayWrapper commitId: orderCommitPathEvents)  {
            orderCommitPathEventsContent.append(commitId).append(",");
        }
        orderCommitPathEventsContent.append("]");
        logger.trace(orderCommitPathEventsContent.toString());

        StringBuilder unCommitPathEventsContent = new StringBuilder("event trace trace, current un commit event[");
        for (ByteArrayWrapper unCommitId: unCommitPathEvents)  {
            unCommitPathEventsContent.append(unCommitId).append(",");
        }
        unCommitPathEventsContent.append("]");
        logger.trace(unCommitPathEventsContent.toString());

    }
}
