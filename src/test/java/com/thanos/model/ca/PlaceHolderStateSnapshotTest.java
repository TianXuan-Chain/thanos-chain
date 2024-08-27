package com.thanos.model.ca;

import com.thanos.chain.ledger.model.event.ca.PlaceHolderStateSnapshot;
import org.junit.Test;

import java.util.Arrays;

/**
 * PlaceHolderStateSnapshotTest.java description：
 *
 * @Author laiyiyu create on 2021-04-28 14:20:55
 */
public class PlaceHolderStateSnapshotTest {

    @Test
    public void fullContent() {
        assert Arrays.equals(new PlaceHolderStateSnapshot().getEncoded(), new PlaceHolderStateSnapshot().getEncoded());
    }

}
