package com.thanos.chain.consensus.hotstuffbft;

import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.consensus.hotstuffbft.model.QuorumCert;
import com.thanos.chain.consensus.hotstuffbft.store.EventTree;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MockEventTreeTest.java description：
 *
 * @Author laiyiyu create on 2020-12-29 11:37:06
 */
public class MockEventTreeTest {

    public static class MockEvent {

        public String id;

        public String parentId;

        public String eventName;

        public MockEvent(String id, String parentId, String eventName) {
            this.id = id;
            this.parentId = parentId;
            this.eventName = eventName;
        }

        @Override
        public String toString() {
            return eventName;
        }
    }


    public static class MockLinkedEvent {

        MockEvent exeEvent;

        Set<String> children;

        public MockLinkedEvent(MockEvent exeEvent) {
            this.exeEvent = exeEvent;
            children = new HashSet<>();
        }

        public void addChild(String childEventId) {
            children.add(childEventId);
        }

        public String getEventId() {
            return exeEvent.id;
        }

    }




    public static class MockEventTree {
        String rootId;

        HashMap<String, MockLinkedEvent> id2Event;

        public MockEventTree(String rootId) {
            this.rootId = rootId;

            this.id2Event = new HashMap<>();
            this.id2Event.put(rootId, new MockLinkedEvent(makeMockEvent(rootId, rootId)));

        }

        public void insertEvent(MockEvent mockEvent) {
            if (id2Event.containsKey(mockEvent.id)) {
                return;
            }

            MockLinkedEvent parentLinkableEvent = id2Event.get(mockEvent.parentId);
            if (parentLinkableEvent == null) {
                throw new RuntimeException(String.format("Parent event [%s] not found", mockEvent.parentId));
            }

            parentLinkableEvent.addChild(mockEvent.id);
            MockLinkedEvent linkableEvent = new MockLinkedEvent(mockEvent);

            id2Event.put(mockEvent.id, linkableEvent);
        }

        public Set<String> findEventToPrune(String nextRootId) {
            if (nextRootId.equals(this.rootId)) {
                return Collections.emptySet();
            }

            Set<String> eventsPruned = new HashSet<>(8);
            eventsPruned.addAll(id2Event.keySet());

            LinkedList<MockLinkedEvent> eventsToBeKeep = new LinkedList<>();

            eventsToBeKeep.add(id2Event.get(nextRootId));

            while (eventsToBeKeep.peek() != null) {
                MockLinkedEvent eventToKeep = eventsToBeKeep.pop();
                eventsPruned.remove(eventToKeep.getEventId());

                Iterator<String> iterator = eventToKeep.children.iterator();

                while (iterator.hasNext()) {
                    String child = iterator.next();
                    eventsPruned.remove(child);

                    eventsToBeKeep.add(id2Event.get(child));
                }

            }

            return eventsPruned;
        }


        public void processPrunedEvents(String nextRootId, Set<String> newlyPrunedEvents) {
            Assert.assertTrue(this.id2Event.containsKey(rootId));

            Set<String> unCommitPathEvents = newlyPrunedEvents.stream().map(id -> (id)).collect(Collectors.toSet());

            LinkedList<String> orderCommitPathEvents = new LinkedList();


            MockLinkedEvent nextRootEvent = id2Event.get(nextRootId);
            MockLinkedEvent parentEvent = id2Event.get(nextRootEvent.exeEvent.parentId);
            while (parentEvent != null) {
                if (parentEvent == null) {
                    break;
                }

                String currentId = parentEvent.getEventId();
                orderCommitPathEvents.addLast(currentId);
                unCommitPathEvents.remove(currentId);

                if (parentEvent.exeEvent.parentId.equals(parentEvent.getEventId())) {
                    //unCommitPathEvents.remove(currentId);
                    break;
                }

                parentEvent = id2Event.get(parentEvent.exeEvent.parentId);

            }

            // Update the next root
            this.rootId = nextRootId;





        }



        public Set<String> reimportTimeoutEvent(String highestId) {

            Set<String> reimportEvents = new HashSet<>();


            LinkedList<String> eventsToBeRemove = new LinkedList<>();
            eventsToBeRemove.addAll(id2Event.get(rootId).children);
            while (eventsToBeRemove.peek() != null) {
                String id = eventsToBeRemove.poll();
                MockLinkedEvent currentEvent = id2Event.get(id);
                if (currentEvent == null) {
                    continue;
                }
                eventsToBeRemove.addAll(currentEvent.children);

                //EventData eventData = currentEvent.exeEvent.getEvent().getEventData();
                //if (!eventData.allEmpty()) {
                //logger.info("reimportTimeoutEvent normal [{}]", Hex.toHexString(eventData.getHash()));
                //this.txnManager.removeEvent(eventData);
                reimportEvents.add(id);
                //}
            }

            MockLinkedEvent currentEvent = id2Event.get(highestId);
            //currentEvent.children.clear();
            while (!rootId.equals(currentEvent.exeEvent.id)) {
                String currentId = currentEvent.exeEvent.id;
                reimportEvents.remove(currentId);
                currentEvent = id2Event.get(currentEvent.exeEvent.parentId);
//                if (currentEvent != null) {
//                    currentEvent.children.clear();
//                    currentEvent.children.add(currentId);
//                }
            }


            if (reimportEvents.size() != 0) {
                for (String id: reimportEvents) {
                    //QuorumCert quorumCert = id2QC.remove(id);
                    MockLinkedEvent removeEvent = id2Event.get(id);
                    //removeEvent.clear();
                }

            }

            return reimportEvents;

        }



    }



    public static String makeName(String rootId) {
        return "MockEvent[" + rootId +"]";
    }

    public static MockEvent makeMockEvent(String id, String parentId) {
        return new MockEvent(id, parentId, makeName(id));
    }

//    ExecutedEventOutput
//    ExecutedEvent executedEvent = new ExecutedEvent(root.rootEvent, executedEventOutput);
//
//    EventTree eventTree = new EventTree();





    @Test
    public void test1() {
        MockEventTree mockEventTree = new MockEventTree("1");

        mockEventTree.insertEvent(makeMockEvent("2-1", "1"));
        mockEventTree.insertEvent(makeMockEvent("2-2", "1"));
        mockEventTree.insertEvent(makeMockEvent("2-3", "1"));

        mockEventTree.insertEvent(makeMockEvent("2-1-1", "2-1"));
        mockEventTree.insertEvent(makeMockEvent("2-2-1", "2-2"));

        mockEventTree.insertEvent(makeMockEvent("2-3-1", "2-3"));
        mockEventTree.insertEvent(makeMockEvent("2-3-1-1", "2-3-1"));

        mockEventTree.insertEvent(makeMockEvent("2-3-1-1-1", "2-3-1-1"));
        mockEventTree.insertEvent(makeMockEvent("2-3-1-1-2", "2-3-1-1"));

        mockEventTree.reimportTimeoutEvent("2-3-1-1-1");
        mockEventTree.rootId = "2-3";
        mockEventTree.reimportTimeoutEvent("2-3-1-1");


        Set<String> id2Remove = mockEventTree.findEventToPrune("2-3-1-1");

        mockEventTree.processPrunedEvents("2-3-1-1", id2Remove);
    }



}
