package com.thanos.model;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.ledger.model.RLPModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * EthEthTransactionReceiptTest.java description：
 *
 * @Author laiyiyu create on 2020-10-27 14:49:32
 */
public class EthEthTransactionReceiptTest {


    public static class TRTestModel extends RLPModel {

        List<EthTransactionReceipt> receiptList;

        public TRTestModel(byte[] rlpEncoded) {
            super(rlpEncoded);
        }

        public TRTestModel(List<EthTransactionReceipt> receiptList) {
            super(null);
            this.receiptList = receiptList;
            this.rlpEncoded = rlpEncoded();

        }

        @Override
        protected byte[] rlpEncoded() {

            byte[][] encode = new byte[1 + receiptList.size()][];
            encode[0] = RLP.encodeInt(0);
            int i = 1;
            for (EthTransactionReceipt transaction: receiptList) {
                encode[i] = transaction.getEncoded();
                i++;
            }
            return RLP.encodeList(encode);


        }

        @Override
        protected void rlpDecoded() {
            RLPList params = RLP.decode2(rlpEncoded);
            RLPList payload = (RLPList) params.get(0);
            List<EthTransactionReceipt> transactions = new ArrayList<>(payload.size() - 1);

            for (int i = 1; i < payload.size(); i++) {
                transactions.add(new EthTransactionReceipt(payload.get(i).getRLPData()));
            }
        }
    }



    @Test
    public void test1() {

        byte[] sender =  SecureKey.getInstance("ECDSA",1).getPubKey();

        int i = 1;
        long currentTime = System.currentTimeMillis();

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
                sender
        );


        EthTransactionReceipt receipt = new EthTransactionReceipt(null, tx, 12, receiveAddress, "");
        System.out.println(receipt);
        EthTransactionReceipt receipt1 = new EthTransactionReceipt(receipt.getEncoded());
        System.out.println(receipt1);

    }

    @Test
    public void test2() {

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
                sender
        );


        EthTransactionReceipt receipt = new EthTransactionReceipt(null, tx, 12, receiveAddress, "");


        System.out.println(receipt);

        int count = 400000;

        ArrayList<EthTransactionReceipt> ethTransactionReceipts = new ArrayList<>(count);
        for (int j = 0; j < count; j++) {
            ethTransactionReceipts.add(receipt);
        }

        for (int k = 0; k < 50; k++) {
            long start = System.currentTimeMillis();
            TRTestModel trTestModel = new TRTestModel(ethTransactionReceipts);
            long end = System.currentTimeMillis();
            System.out.println("encode use:" + (end - start));

//            long start1 = System.currentTimeMillis();
//            TRTestModel trTestModel1 = new TRTestModel(trTestModel.rlpEncoded());
//            long end1 = System.currentTimeMillis();
//            System.out.println("decode use:" + (end1 - start1));
        }

    }

}
