package com.thanos.model;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.crypto.key.asymmetric.ec.ECKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.consensus.hotstuffbft.model.ConsensusPayload;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.ledger.model.RLPModel;
import org.junit.Test;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.*;

/**
 * ConsensusPayloadTest.java description：
 *
 * @Author laiyiyu create on 2020-06-29 15:42:45
 */
public class ConsensusPayloadTest {




    @Test
    public void test1() {
        for (int i = 0; i < 20; i++) {
            create();
        }

    }


    public static void create() {

        byte[] sender =  new ECKey().getPubKey();

        int num = 300000;
        EthTransaction[] ethTransactions = new EthTransaction[num];


        long currentTime = System.currentTimeMillis();
        long start, end;
        start = System.currentTimeMillis();

        GlobalNodeEvent[] globalNodeEvents = new GlobalNodeEvent[1];
        //GlobalNodeEvent[] globalNodeEvents = new GlobalNodeEvent[0];
        globalNodeEvents[0] = new GlobalNodeEvent(HashUtil.sha3(new byte[]{2, 3}), HashUtil.sha3(new byte[]{2, 3}), 1, (byte)1, HashUtil.sha3(new byte[]{2, 3}),  HashUtil.randomHash() );


        for (int i = 0; i < num; i++) {
            byte[] receiveAddress = HashUtil.sha3(ByteUtil.longToBytesNoLeadZeroes(currentTime + i));
            EthTransaction tx = new EthTransaction(
                    sender,
                    ByteUtil.longToBytesNoLeadZeroes(currentTime + i),
                    i,
                    ByteUtil.longToBytesNoLeadZeroes(currentTime - i),
                    ByteUtil.longToBytesNoLeadZeroes(currentTime + 3_000_000 + i),
                    receiveAddress,
                    ByteUtil.longToBytesNoLeadZeroes(currentTime - 3_000_000 - i),
                    receiveAddress,
                    new HashSet<>(Arrays.asList(new ByteArrayWrapper(("hehe" + i).getBytes()))),
                    sender
            );

            ethTransactions[i] = tx;

        }
        end = System.currentTimeMillis();
        System.out.println("create ethTransactions cost:" + (end - start) + " ms");

        start = System.currentTimeMillis();
        ConsensusPayload consensusPayload = new ConsensusPayload(ethTransactions);
        System.out.println("consensusPayload:" + consensusPayload);
        end = System.currentTimeMillis();
        System.out.println("create ConsensusPayload cost:" + (end - start) + " ms");
        System.out.println("ConsensusPayload size:" + ByteUtil.getPrintSize(consensusPayload.getEncoded().length));


        //ConsensusPayload temp = new ConsensusPayload(consensusPayload.getEncoded());

        start = System.currentTimeMillis();
        byte[] compressRes = null;
        try {
            compressRes = Snappy.compress(consensusPayload.getEncoded());
            consensusPayload = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        end = System.currentTimeMillis();
        System.out.println("Snappy.compress cost:" + (end - start)  + " ms");
        System.out.println("after compress size:" + ByteUtil.getPrintSize(compressRes.length));


        start = System.currentTimeMillis();
        byte[] originContent = null;
        try {
            originContent = Snappy.uncompress(compressRes);

        } catch (IOException e) {
            e.printStackTrace();
        }
        end = System.currentTimeMillis();
        System.out.println("Snappy un compress cost:" + (end - start) + " ms");





        start = System.currentTimeMillis();
        ConsensusPayload consensusPayload1 = new ConsensusPayload(originContent);
        //consensusPayload1.reDecoded();
        System.out.println("consensusPayload1:" + consensusPayload1);
        consensusPayload1.setParallelDecode(false);
        end = System.currentTimeMillis();
        System.out.println("decode ConsensusPayload cost:" + (end - start) + " ms");


        System.out.println("=======================================");
        System.out.println("");


    }



    @Test
    public void test2() {
        for (int i = 0; i < 50; i++) {
            create2();
        }

    }

    public void create2() {
        int num = 200000;
        long start, end;
        start = System.currentTimeMillis();
        List<byte[]> hashes = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            hashes.add(HashUtil.sha3(new String("" + i).getBytes()));
        }

        end = System.currentTimeMillis();
        System.out.println("create hashes cost:" + (end - start) + " ms");

        start = System.currentTimeMillis();
        PayloadTest consensusPayload = new PayloadTest(hashes);
        end = System.currentTimeMillis();
        System.out.println("create PayloadTest cost:" + (end - start) + " ms");
        System.out.println("PayloadTest size:" + ByteUtil.getPrintSize(consensusPayload.getEncoded().length));



//        start = System.currentTimeMillis();
//        byte[] compressRes = null;
//        try {
//            compressRes = Snappy.compress(consensusPayload.getEncoded());
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        end = System.currentTimeMillis();
//        System.out.println("Snappy.compress cost:" + (end - start)  + " ms");
//        System.out.println("after PayloadTest compress size:" + getPrintSize(compressRes.length));
//
//
//        start = System.currentTimeMillis();
//        byte[] originContent = null;
//        try {
//            originContent = Snappy.uncompress(compressRes);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        end = System.currentTimeMillis();
//        System.out.println("Snappy un compress PayloadTest cost:" + (end - start) + " ms");


        start = System.currentTimeMillis();
        PayloadTest consensusPayload1 = new PayloadTest(consensusPayload.getEncoded());
        end = System.currentTimeMillis();
        System.out.println("decode PayloadTest cost:" + (end - start) + " ms");


        System.out.println("=======================================");

    }


    public static class PayloadTest extends RLPModel {
        int PLACEHOLDER = 1;

        List<byte[]> txHashs;

        public PayloadTest(byte[] encode) {
            super(encode);
        }

        public PayloadTest(List<byte[]> txHashs) {
            super(null);
            this.txHashs = txHashs;
            this.rlpEncoded = rlpEncoded();
        }

        @Override
        protected byte[] rlpEncoded() {

            byte[][] encode = new byte[txHashs.size() + 1][];
            encode[0] = RLP.encodeInt(PLACEHOLDER);
            int i = 1;
            for (byte[] hash: txHashs) {
                //System.out.println("encode:" + Hex.toHexString(hash));
                encode[i] = RLP.encodeElement(hash);
                i++;
            }

            return RLP.encodeList(encode);

        }

        @Override
        protected void rlpDecoded() {
            RLPList params = RLP.decode2(rlpEncoded);
            RLPList resp = (RLPList) params.get(0);

            this.PLACEHOLDER = ByteUtil.byteArrayToInt(resp.get(0).getRLPData());

            List<byte[]> txHashs = new ArrayList<>();
            for (int i = 1; i < resp.size(); i++) {
                byte[] hash = resp.get(i).getRLPData();
                //System.out.println("decode:" + Hex.toHexString(hash));
                txHashs.add(hash);
            }
            this.txHashs = txHashs;
        }
    }



    @Test
    public void test3() {
        for (int i = 0; i < 20; i++) {
            create3();
        }

    }

    private void create3() {

        byte[] sender =  new ECKey().getPubKey();

        int num = 300000;
        EthTransaction[] ethTransactions = new EthTransaction[num];


        long currentTime = System.currentTimeMillis();
        long start, end;
        start = System.currentTimeMillis();

        GlobalNodeEvent[] globalNodeEvents = new GlobalNodeEvent[1];
        //GlobalNodeEvent[] globalNodeEvents = new GlobalNodeEvent[0];
        globalNodeEvents[0] = new GlobalNodeEvent(HashUtil.sha3(new byte[]{2, 3}), HashUtil.sha3(new byte[]{2, 3}), 1, (byte)1, HashUtil.sha3(new byte[]{2, 3}),  HashUtil.randomHash() );


        for (int i = 0; i < num; i++) {
            byte[] receiveAddress = HashUtil.sha3(ByteUtil.longToBytesNoLeadZeroes(currentTime + i));

            EthTransaction tx = new EthTransaction(
                    sender,
                    ByteUtil.longToBytesNoLeadZeroes(currentTime + i),
                    i,
                    ByteUtil.longToBytesNoLeadZeroes(currentTime - i),
                    ByteUtil.longToBytesNoLeadZeroes(currentTime + 3_000_000 + i),
                    receiveAddress,
                    ByteUtil.longToBytesNoLeadZeroes(currentTime - 3_000_000 - i),
                    receiveAddress,
                    new HashSet<>(Arrays.asList(new ByteArrayWrapper(("hehe" + i).getBytes()))),
                    sender
            );

            ethTransactions[i] = tx;

            //System.out.println("tx.length:" + tx.getEncoded().length);


        }
        end = System.currentTimeMillis();
        System.out.println("create ethTransactions cost:" + (end - start) + " ms");

        start = System.currentTimeMillis();
        ConsensusPayload consensusPayload = new ConsensusPayload(ethTransactions);
        System.out.println("consensusPayload:" + consensusPayload);
        end = System.currentTimeMillis();
        System.out.println("create ConsensusPayload cost:" + (end - start) + " ms");
        System.out.println("ConsensusPayload size:" + ByteUtil.getPrintSize(consensusPayload.getEncoded().length));



//        start = System.currentTimeMillis();
//        byte[] compressRes = null;
//        try {
//            compressRes = Snappy.compress(consensusPayload.getEncoded());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        end = System.currentTimeMillis();
//        System.out.println("Snappy.compress cost:" + (end - start)  + " ms");
//        System.out.println("after compress size:" + ByteUtil.getPrintSize(compressRes.length));

        //ConsensusPayload temp = new ConsensusPayload(consensusPayload.getEncoded());

//
//        byte[] hash = sender;
//        for (EthTransaction transaction: ethTransactions) {
//            hash = HashUtil.sha3(hash, transaction.getHash());
//        }
//        System.out.println("root1:" + Hex.toHexString(hash));




        start = System.currentTimeMillis();
        ConsensusPayload consensusPayload1 = new ConsensusPayload(consensusPayload.getEncoded());
        //consensusPayload1.reDecoded();
        System.out.println("consensusPayload1:" + consensusPayload1);
        consensusPayload1.setParallelDecode(false);
        end = System.currentTimeMillis();
        System.out.println("decode ConsensusPayload cost:" + (end - start) + " ms");


//        byte[] hash1 = sender;
//        for (EthTransaction transaction: consensusPayload1.getEthTransactions()) {
//            hash1 = HashUtil.sha3(hash1, transaction.getHash());
//        }
//        System.out.println("root1:" + Hex.toHexString(hash1));


        System.out.println("=======================================");
        System.out.println("");



    }

}
