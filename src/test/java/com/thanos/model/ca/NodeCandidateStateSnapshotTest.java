package com.thanos.model.ca;

import com.thanos.chain.ledger.model.event.ca.NodeCandidate;
import com.thanos.chain.ledger.model.event.ca.NodeCandidateStateSnapshot;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.HashUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

/**
 * NodeCandidateStateSnapshotTest.java description：
 *
 * @Author laiyiyu create on 2021-04-28 14:02:00
 */
public class NodeCandidateStateSnapshotTest {


    @Test
    public void fullContent() {


        ArrayList agreeSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList rejectSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<NodeCandidate> candidate = Optional.of(createNodeCandidate());

        NodeCandidateStateSnapshot stateSnapshot1 = new NodeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        NodeCandidateStateSnapshot stateSnapshot2 = new NodeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);

        assert stateSnapshot1.equals(stateSnapshot2);


    }

    @Test
    public void withoutCandidate() {
        ArrayList agreeSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList rejectSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<NodeCandidate> candidate = Optional.empty();

        NodeCandidateStateSnapshot stateSnapshot1 = new NodeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        NodeCandidateStateSnapshot stateSnapshot2 = new NodeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }

    @Test
    public void withoutAgree() {


        ArrayList agreeSet = new ArrayList() {{
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList rejectSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<NodeCandidate> candidate = Optional.of(createNodeCandidate());

        NodeCandidateStateSnapshot stateSnapshot1 = new NodeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        NodeCandidateStateSnapshot stateSnapshot2 = new NodeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }

    @Test
    public void withoutReject() {


        ArrayList agreeSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList rejectSet = new ArrayList() {{
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<NodeCandidate> candidate = Optional.of(createNodeCandidate());

        NodeCandidateStateSnapshot stateSnapshot1 = new NodeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        NodeCandidateStateSnapshot stateSnapshot2 = new NodeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }

    @Test
    public void withoutRejectAndAgree() {


        ArrayList agreeSet = new ArrayList() {{
        }};

        ArrayList rejectSet = new ArrayList() {{
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<NodeCandidate> candidate = Optional.of(createNodeCandidate());

        NodeCandidateStateSnapshot stateSnapshot1 = new NodeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        NodeCandidateStateSnapshot stateSnapshot2 = new NodeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }


    @Test
    public void withoutRejectAndCandidate() {


        ArrayList agreeSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList rejectSet = new ArrayList() {{

        }};

        Optional<NodeCandidate> candidate = Optional.empty();

        NodeCandidateStateSnapshot stateSnapshot1 = new NodeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        NodeCandidateStateSnapshot stateSnapshot2 = new NodeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }

    @Test
    public void withoutAgreeAndCandidate() {


        ArrayList agreeSet = new ArrayList() {{

        }};

        ArrayList rejectSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<NodeCandidate> candidate = Optional.empty();

        NodeCandidateStateSnapshot stateSnapshot1 = new NodeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        NodeCandidateStateSnapshot stateSnapshot2 = new NodeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }


    private static NodeCandidate createNodeCandidate() {
        return new NodeCandidate(HashUtil.randomHash(), "nodeName", "nodeAgency", "nameHash", 1, 2, 3, HashUtil.randomHash());
    }

}
