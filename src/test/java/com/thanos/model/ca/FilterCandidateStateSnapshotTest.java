package com.thanos.model.ca;

import com.thanos.chain.contract.ca.manager.CandidateEventConstant;
import com.thanos.chain.ledger.model.event.ca.FilterCandidate;
import com.thanos.chain.ledger.model.event.ca.FilterCandidateStateSnapshot;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.HashUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

/**
 * FilterCandidateStateSnapshotTest.java description：
 *
 * @Author laiyiyu create on 2021-04-28 14:10:12
 */
public class FilterCandidateStateSnapshotTest {


    @Test
    public void fullContent() {


        ArrayList agreeSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList rejectSet = new ArrayList() {{
            add(new ByteArrayWrapper(HashUtil.randomHash()));
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        Optional<FilterCandidate> candidate = Optional.of(createFilterCandidate());

        FilterCandidateStateSnapshot stateSnapshot1 = new FilterCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        FilterCandidateStateSnapshot stateSnapshot2 = new FilterCandidateStateSnapshot(stateSnapshot1.getEncoded());
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

        Optional<FilterCandidate> candidate = Optional.empty();

        FilterCandidateStateSnapshot stateSnapshot1 = new FilterCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        FilterCandidateStateSnapshot stateSnapshot2 = new FilterCandidateStateSnapshot(stateSnapshot1.getEncoded());
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

        Optional<FilterCandidate> candidate = Optional.of(createFilterCandidate());

        FilterCandidateStateSnapshot stateSnapshot1 = new FilterCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        FilterCandidateStateSnapshot stateSnapshot2 = new FilterCandidateStateSnapshot(stateSnapshot1.getEncoded());
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

        Optional<FilterCandidate> candidate = Optional.of(createFilterCandidate());

        FilterCandidateStateSnapshot stateSnapshot1 = new FilterCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        FilterCandidateStateSnapshot stateSnapshot2 = new FilterCandidateStateSnapshot(stateSnapshot1.getEncoded());
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

        Optional<FilterCandidate> candidate = Optional.of(createFilterCandidate());

        FilterCandidateStateSnapshot stateSnapshot1 = new FilterCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        FilterCandidateStateSnapshot stateSnapshot2 = new FilterCandidateStateSnapshot(stateSnapshot1.getEncoded());
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
            add(new ByteArrayWrapper(HashUtil.randomHash()));
        }};

        ArrayList rejectSet = new ArrayList() {{

        }};

        Optional<FilterCandidate> candidate = Optional.empty();

        FilterCandidateStateSnapshot stateSnapshot1 = new FilterCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        FilterCandidateStateSnapshot stateSnapshot2 = new FilterCandidateStateSnapshot(stateSnapshot1.getEncoded());
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
        }};

        Optional<FilterCandidate> candidate = Optional.empty();

        FilterCandidateStateSnapshot stateSnapshot1 = new FilterCandidateStateSnapshot(agreeSet, rejectSet, candidate);
        System.out.println(stateSnapshot1);

        FilterCandidateStateSnapshot stateSnapshot2 = new FilterCandidateStateSnapshot(stateSnapshot1.getEncoded());
        System.out.println(stateSnapshot2);


        assert stateSnapshot1.equals(stateSnapshot2);
    }


    private static FilterCandidate createFilterCandidate() {
        return new FilterCandidate(CandidateEventConstant.AGREE_VOTE, HashUtil.randomHash(), CaContractCodeBuilder.build());
    }
}
