package com.thanos.model;

import com.thanos.chain.ledger.model.BlockSign;
import com.thanos.model.common.SignatureBuilder;
import org.junit.Test;

/**
 * BlockSignTest.java description：
 *
 * @Author laiyiyu create on 2021-01-08 15:12:28
 */
public class BlockSignTest {

    @Test
    public void test1() {
        BlockSign blockSign1 = new BlockSign(1, 2, new byte[]{(byte)2, (byte)1}, SignatureBuilder.build());
        System.out.println(blockSign1);
        System.out.println(new BlockSign(blockSign1.getEncoded()));

        System.out.println("====================");

        BlockSign blockSign2 = new BlockSign(1, 2, new byte[]{(byte)2, (byte)1}, null);
        System.out.println(blockSign2);
        System.out.println(new BlockSign(blockSign2.getEncoded()));

    }

}
