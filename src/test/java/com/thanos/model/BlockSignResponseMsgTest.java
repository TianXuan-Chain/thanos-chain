package com.thanos.model;

import com.thanos.model.common.SignatureBuilder;
import com.thanos.chain.state.verifier.model.BlockSignResponseMsg;
import org.junit.Test;

/**
 * BlockSignResponseMsgTest.java description：
 *
 * @Author laiyiyu create on 2020-09-27 16:49:22
 */
public class BlockSignResponseMsgTest {

    @Test
    public void test1() {
        BlockSignResponseMsg blockSignResponseMsg = new BlockSignResponseMsg(1, new byte[]{}, SignatureBuilder.build());
        System.out.println(blockSignResponseMsg);
        System.out.println(new BlockSignResponseMsg(blockSignResponseMsg.getEncoded()).toString());
    }
}
