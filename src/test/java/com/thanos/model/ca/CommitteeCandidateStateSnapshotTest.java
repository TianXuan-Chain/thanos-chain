package com.thanos.model.ca;

import com.thanos.chain.ledger.model.event.ca.CommitteeCandidate;
import com.thanos.chain.ledger.model.event.ca.CommitteeCandidateStateSnapshot;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.HashUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

/**
 * CommitteeCandidateStateSnapshotTest.java description：
 *
 * @Author laiyiyu create on 2021-04-07 11:26:44
 */
public class CommitteeCandidateStateSnapshotTest {

    @Test
    public void fullContent() {


        ArrayList<ByteArrayWrapper> agreeSet = new ArrayList() {{
                add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList rejectSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<CommitteeCandidate> candidate = Optional.of(new CommitteeCandidate(1, HashUtil.randomHash(), new ByteArrayWrapper(HashUtil.randomHash())));

        CommitteeCandidateStateSnapshot stateSnapshot1 = new CommitteeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        CommitteeCandidateStateSnapshot stateSnapshot2 = new CommitteeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);

        assert stateSnapshot1.equals(stateSnapshot2);


    }

    @Test
    public void withoutCandidate() {


        ArrayList<ByteArrayWrapper> agreeSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList<ByteArrayWrapper> rejectSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<CommitteeCandidate> candidate = Optional.empty();

        CommitteeCandidateStateSnapshot stateSnapshot1 = new CommitteeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        CommitteeCandidateStateSnapshot stateSnapshot2 = new CommitteeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }

    @Test
    public void withoutAgree() {


        ArrayList<ByteArrayWrapper> agreeSet = new ArrayList() {{
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList<ByteArrayWrapper> rejectSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<CommitteeCandidate> candidate = Optional.of(new CommitteeCandidate(1, HashUtil.randomHash(), new ByteArrayWrapper(HashUtil.randomHash())));

        CommitteeCandidateStateSnapshot stateSnapshot1 = new CommitteeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        CommitteeCandidateStateSnapshot stateSnapshot2 = new CommitteeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }

    @Test
    public void withoutReject() {


        ArrayList<ByteArrayWrapper> agreeSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList<ByteArrayWrapper> rejectSet = new ArrayList() {{
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<CommitteeCandidate> candidate = Optional.of(new CommitteeCandidate(1, HashUtil.randomHash(), new ByteArrayWrapper(HashUtil.randomHash())));

        CommitteeCandidateStateSnapshot stateSnapshot1 = new CommitteeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        CommitteeCandidateStateSnapshot stateSnapshot2 = new CommitteeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }

    @Test
    public void withoutRejectAndAgree() {


        ArrayList<ByteArrayWrapper> agreeSet = new ArrayList() {{
        }};

        ArrayList<ByteArrayWrapper> rejectSet = new ArrayList() {{
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
//            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<CommitteeCandidate> candidate = Optional.of(new CommitteeCandidate(1, HashUtil.randomHash(), new ByteArrayWrapper(HashUtil.randomHash())));

        CommitteeCandidateStateSnapshot stateSnapshot1 = new CommitteeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        CommitteeCandidateStateSnapshot stateSnapshot2 = new CommitteeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }


    @Test
    public void withoutRejectAndCandidate() {


        ArrayList<ByteArrayWrapper> agreeSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList<ByteArrayWrapper> rejectSet = new ArrayList() {{

        }};

        Optional<CommitteeCandidate> candidate = Optional.empty();

        CommitteeCandidateStateSnapshot stateSnapshot1 = new CommitteeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        CommitteeCandidateStateSnapshot stateSnapshot2 = new CommitteeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }

    @Test
    public void withoutAgreeAndCandidate() {


        ArrayList<ByteArrayWrapper> agreeSet = new ArrayList() {{

        }};

        ArrayList<ByteArrayWrapper> rejectSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<CommitteeCandidate> candidate = Optional.empty();

        CommitteeCandidateStateSnapshot stateSnapshot1 = new CommitteeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        CommitteeCandidateStateSnapshot stateSnapshot2 = new CommitteeCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }

    public static CommitteeCandidateStateSnapshot buildFullContent() {
        ArrayList<ByteArrayWrapper> agreeSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList<ByteArrayWrapper> rejectSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<CommitteeCandidate> candidate = Optional.of(new CommitteeCandidate(1, HashUtil.randomHash(), new ByteArrayWrapper(HashUtil.randomHash())));

        CommitteeCandidateStateSnapshot stateSnapshot = new CommitteeCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        return stateSnapshot;
    }

}
