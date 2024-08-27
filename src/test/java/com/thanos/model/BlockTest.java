package com.thanos.model;

import com.thanos.chain.ledger.model.*;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.chain.ledger.model.event.GlobalEvent;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.ledger.model.crypto.Signature;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.*;

/**
 * BlockTest.java description：
 *
 * @Author laiyiyu create on 2020-10-30 16:46:48
 */
public class BlockTest {


    private static List<EthTransactionReceipt> createTRs() {
        long currentTime = 1;
        long i = 1;
        byte[] sender =  SecureKey.getInstance("ECDSA",1).getPubKey();

        byte[] receiveAddress = HashUtil.sha3(ByteUtil.longToBytesNoLeadZeroes(currentTime + i));

        EthTransaction tx = new EthTransaction(
                sender,
                ByteUtil.longToBytesNoLeadZeroes(currentTime + i),
                2,
                ByteUtil.longToBytesNoLeadZeroes(currentTime - i),
                ByteUtil.longToBytesNoLeadZeroes(currentTime + 3_000_000 + i),
                receiveAddress,
                ByteUtil.longToBytesNoLeadZeroes(currentTime - 3_000_000 - i),
                receiveAddress,
                new HashSet<>(Arrays.asList(new ByteArrayWrapper(("hehe" + i).getBytes()))),
                sender);



        List<EthTransactionReceipt> receipts = new ArrayList();

        receipts.add(new EthTransactionReceipt(null, tx, 12, receiveAddress, ""));
        return receipts;
    }

    private static Block createBlock() {
        Block block = new Block(HashUtil.randomHash(), HashUtil.randomHash(), HashUtil.randomHash(),
                1, 2, 3, HashUtil.randomHash(), HashUtil.randomHash(), null);

        return block;
    }

    private static Block createBlockWithEvent() {

        GlobalNodeEvent[] globalNodeEvents = new GlobalNodeEvent[1];
        //globalNodeEvents[0] = new GlobalNodeEvent(HashUtil.randomHash(), HashUtil.randomHash(), 20, HashUtil.randomHash(), "#1", "#2", "#3", 4, 5, GlobalNodeEvent.NodeEventType.REGISTER_PROCESS, HashUtil.sha3(new byte[]{2, 3}));
        globalNodeEvents[0] = new GlobalNodeEvent(HashUtil.sha3(new byte[]{2, 3}), HashUtil.sha3(new byte[]{2, 3}), 1, (byte)1, HashUtil.sha3(new byte[]{2, 3}),  HashUtil.randomHash() );

        GlobalEvent globalEvent = new GlobalEvent(globalNodeEvents);
        System.out.println(globalEvent);
        System.out.println(new GlobalEvent(globalEvent.getEncoded()));

        Block block = new Block(HashUtil.randomHash(), HashUtil.randomHash(), HashUtil.randomHash(),
                1, 2, 3, HashUtil.randomHash(), HashUtil.randomHash(), globalEvent, null);

        return block;
    }

    private static Map<ByteArrayWrapper, Signature> createSignature() {

        byte[] temp = HashUtil.randomHash();
        Map<ByteArrayWrapper, Signature> result = new TreeMap<>();
        result.put(new ByteArrayWrapper(temp), new Signature(temp));
        return result;
    }

    @Test
    public void fullPayload() {

        Block block = createBlockWithEvent();
        System.out.println(block);

        block.setReceipts(createTRs());
        //block.recordSign(new BlockSign());
        block.reEncoded();
        System.out.println(block);
        System.out.println(new Block(block.getEncoded()));
        String hexBlock = Hex.toHexString(block.getEncoded());
        System.out.println(new Block(Hex.decode(hexBlock)));
        System.out.println(new Block(block.getEncoded()));
        System.out.println(hexBlock);
    }


    @Test
    public void withoutReceipts() {

        Block block = createBlock();

        //block.setReceipts(createTRs());
        //block.recordSign(createSignature());
        block.reEncoded();
        System.out.println(block);
        System.out.println(new Block(block.getEncoded()));
    }

    @Test
    public void withoutSignatures() {

        Block block = createBlock();
        block.setReceipts(createTRs());
        //block.recordSign(createSignature());
        block.reEncoded();
        System.out.println(block);
        System.out.println(new Block(block.getEncoded()));
    }
}
