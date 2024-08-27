package com.thanos.chain.executor;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.crypto.key.asymmetric.ec.ECKeyOld;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.txpool.TxnManager;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.contract.eth.solidity.SolidityType;
import com.thanos.chain.contract.eth.solidity.compiler.CompilationResult;
import com.thanos.chain.ledger.model.event.GlobalEvent;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.model.common.QuorumCertBuilder;
import com.thanos.model.common.SignatureBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;

import static com.thanos.chain.executor.ParallelStoreExecutorTest.storeTemp;
import static com.thanos.chain.executor.ParallelTokenExecutorTest.tokenComp;

/**
 * AsyncExecutorTest.java description：
 *
 * @Author laiyiyu create on 2020-09-02 10:27:50
 */

@Ignore
public class AsyncExecutorTest extends ExecutorTestBase {

    private static TreeMap<ByteArrayWrapper, Signature> signatureTreeMap = SignatureBuilder.build();

    private static final int EXE_TIME = 15;

    private static final int TRANSACTION_NUM = 120000;

    static ECKeyOld sender = ECKeyOld.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c")).compress();
    static ECKeyOld sender2 = ECKeyOld.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445a")).compress();


    @Test
    public void  test1() {
        //13-052e3614bebb47398ba3deaf890a3ec2e7364976f5ca6a232bf5050a36388e82
        //14-e4e1c8d12902d197d151171daff2444834ffa568c8f7a7677e55633fbfbb99f2
        //16-d217af3b083d6cb9ece187f551564fda1e53f4f10923dbf7cbeea9319b5a8464
        //18-304395c825d48590d9e8d3bea6359834e43d38942172bdb9639e72e2883e7a38
        //19-4ef76c4cc2621a041d5d39b5507694eaba52479452db4a0d401595c370e7646d
        TxnManager txnManager = new TxnManager(100000, 100000, consensusChainStore);

        AsyncExecutor asyncExecutor = new AsyncExecutor(consensusChainStore, stateLedger);

        mockConsensusEventSuccess();
        waitLong();
    }


    private void mockConsensusEventSuccess() {

        CompilationResult cres = null;
        try {
            cres = tokenComp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("TokensDemo").abi);

        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("TokensDemo").bin), 0);
        byte[] contractAddress = HashUtil.calcNewAddr(sender.getAddress(), new BigInteger("0").toByteArray());
        System.out.println("mockConsensusEventSuccess.address:" + Hex.toHexString(contractAddress));
        long startNumber = systemConfig.getGenesisJson().getStartEventNumber();

        // deploy contract
        QuorumCert quorumCert1 = QuorumCertBuilder.buildWithNumber(startNumber);
        EventData eventData1 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{tx}), sender.doGetPubKey(), 1, System.currentTimeMillis(), quorumCert1);
        consensusChainStore.commit(Arrays.asList(eventData1), convertEventSignaturesFrom(eventData1), buildOutputByNumber(eventData1.getNumber()), convertLedgerSignsFrom(eventData1), true);


        // set userMoney
        EthTransaction[] txs2 = new EthTransaction[TRANSACTION_NUM];
        for (int i = 0; i < TRANSACTION_NUM; i++) {
            byte[] callData = contract1.getByName("setBalance").encode(MOCK_ADDRESS[i], "5000000");
            EthTransaction temp = createTx(stateLedger, sender, contractAddress, callData, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(MOCK_ADDRESS[i].getBytes()))) );
            txs2[i] = temp;
        }

        QuorumCert quorumCert2 = QuorumCertBuilder.buildWithNumber(startNumber + 1);
        EventData eventData2 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(txs2), sender.doGetPubKey(), 1, System.currentTimeMillis(), quorumCert2);
        consensusChainStore.commit(Arrays.asList(eventData2), convertEventSignaturesFrom(eventData2), buildOutputByNumber(eventData2.getNumber()), convertLedgerSignsFrom(eventData2), true);

//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//
//        }


        // mock transfer

        new Thread(() -> {

            long startNum = startNumber + 2;
            int time = 0 ;


            EthTransaction[] txs3 = new EthTransaction[TRANSACTION_NUM - 1];
            for (int i = 0; i < TRANSACTION_NUM - 1; i++) {

                byte[] callData = contract1.getByName("transfer").encode(MOCK_ADDRESS[i], MOCK_ADDRESS[i + 1], 15);
                EthTransaction temp = createTx(stateLedger, sender, contractAddress, callData, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(MOCK_ADDRESS[i].getBytes()), new ByteArrayWrapper(MOCK_ADDRESS[i + 1].getBytes()))) );
                txs3[i] = temp;
            }


            while (true) {



                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                }




                ConsensusPayload consensusPayload = new ConsensusPayload(txs3);
                consensusPayload.setParsed();

                QuorumCert quorumCert3 = QuorumCertBuilder.buildWithNumber(startNum);
                EventData eventData3 = EventData.buildProposal(new GlobalEvent(), consensusPayload, sender.doGetPubKey(), 1, System.currentTimeMillis(), quorumCert3);
                consensusChainStore.commit(Arrays.asList(eventData3), convertEventSignaturesFrom(eventData3), buildOutputByNumber(eventData3.getNumber()), convertLedgerSignsFrom(eventData3), true);
                startNum++;

                time++;

                if (time == EXE_TIME) {
                    break;
                }

            }



            //consensusChainStore.commit();
        }, "mock_commit_thread").start();
    }


    private void waitLong() {
        try {
            Thread.sleep(1000000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static List<EventInfoWithSignatures> convertEventSignaturesFrom(EventData eventData) {
        List<EventInfoWithSignatures> result = new ArrayList<>();

        EventInfoWithSignatures signatures = EventInfoWithSignatures.build(eventData.getEpoch(), eventData.getRound(), eventData.getHash(), eventData.getHash(), eventData.getNumber(), eventData.getTimestamp(), Optional.empty(), signatureTreeMap);
        result.add(signatures);

        return result;
    }

    public static LedgerInfoWithSignatures convertLedgerSignsFrom(EventData eventData) {

        EventInfo eventInfo = EventInfo.build(eventData.getEpoch(), eventData.getRound(), eventData.getHash(), eventData.getHash(), eventData.getNumber(), eventData.getTimestamp(),  Optional.empty());
        LedgerInfoWithSignatures result = LedgerInfoWithSignatures.build(LedgerInfo.build(eventInfo, eventData.getHash()), signatureTreeMap);

        return result;
    }

    public static ExecutedEventOutput buildOutputByNumber(long number) {
        ExecutedEventOutput eventOutput = new ExecutedEventOutput(new HashMap<>(), number, HashUtil.sha3(new byte[]{2, 3,55}), Optional.empty());
        return eventOutput;

    }



    @Test

    public void query() {

        CompilationResult cres = null;
        try {
            cres = tokenComp();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
//                Hex.decode(cres.getContract("TokensDemo").bin), 0);
//        byte[] contractAddress = tx.getContractAddress();

        byte[] contractAddress = HashUtil.calcNewAddr(sender.getAddress(), new BigInteger("0").toByteArray());

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("TokensDemo").abi);

//
//
//
//        byte[] callData2 = contract1.getByName("getBalance").encode(MOCK_ADDRESS[6]);
//        EthTransaction tx2 = createTx(stateLedger, sender, contractAddress, callData2, 0l);
//        byte[] returnResult2 = executeTransaction(stateLedger, tx2).getResult().getHReturn();
//        System.out.println(SolidityType.IntType.decodeInt(returnResult2, 0).intValue());
//       // Assert.assertEquals(50000, SolidityType.IntType.decodeInt(returnResult2, 0).intValue());
//
//
//        byte[] callData3 = contract1.getByName("getBalance").encode(MOCK_ADDRESS[TRANSACTION_NUM - 1]);
//        EthTransaction tx3 = createTx(stateLedger, sender, contractAddress, callData3, 0l);
//        byte[] returnResult3 = executeTransaction(stateLedger, tx3).getResult().getHReturn();
//        System.out.println(SolidityType.IntType.decodeInt(returnResult3, 0).intValue());
//       // Assert.assertEquals(EXE_TIME, SolidityType.IntType.decodeInt(returnResult3, 0).intValue());



        for (int i = 0 ;i < TRANSACTION_NUM + 4; i++) {
            byte[] callData4 = contract1.getByName("getBalance").encode(MOCK_ADDRESS[i]);
            EthTransaction tx4 = createTx(stateLedger, sender, contractAddress, callData4, 0l);
            byte[] returnResult4 = executeTransaction(stateLedger, tx4).getResult().getHReturn();
            System.out.println(i + ":" + SolidityType.IntType.decodeInt(returnResult4, 0).intValue());
        }

        byte[] callData1 = contract1.getByName("getBalance").encode(MOCK_ADDRESS[0]);
        EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData1, 0l);
        byte[] returnResult1 = executeTransaction(stateLedger, tx1).getResult().getHReturn();
        System.out.println(SolidityType.IntType.decodeInt(returnResult1, 0).intValue());
        //Assert.assertEquals(50000 - EXE_TIME, SolidityType.IntType.decodeInt(returnResult1, 0).intValue());
    }



    @Test
    public void  test2() {
        //13-052e3614bebb47398ba3deaf890a3ec2e7364976f5ca6a232bf5050a36388e82
        //14-e4e1c8d12902d197d151171daff2444834ffa568c8f7a7677e55633fbfbb99f2
        //16-d217af3b083d6cb9ece187f551564fda1e53f4f10923dbf7cbeea9319b5a8464
        //18-304395c825d48590d9e8d3bea6359834e43d38942172bdb9639e72e2883e7a38
        //19-4ef76c4cc2621a041d5d39b5507694eaba52479452db4a0d401595c370e7646d
        TxnManager txnManager = new TxnManager(100000, 1000000, consensusChainStore);

        AsyncExecutor asyncExecutor = new AsyncExecutor(consensusChainStore, stateLedger);

        mockStoreConsensusEventSuccess();
        waitLong();
    }

    private void mockStoreConsensusEventSuccess() {

        CompilationResult cres = null;
        try {
            cres = storeTemp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("FruitStore").abi);

        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("FruitStore").bin), 0);
        byte[] contractAddress = tx.getContractAddress();

        //long startNumber = systemConfig.getGenesisJson().getStartEventNumber();
        long startNumber = 44;

        // deploy contract
        QuorumCert quorumCert1 = QuorumCertBuilder.buildWithNumber(startNumber);
        EventData eventData1 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{tx}), sender.doGetPubKey(), 1, System.currentTimeMillis(), quorumCert1);
        consensusChainStore.commit(Arrays.asList(eventData1), convertEventSignaturesFrom(eventData1), buildOutputByNumber(eventData1.getNumber()), convertLedgerSignsFrom(eventData1), true);



//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//
//        }


        // mock transfer

        new Thread(() -> {

            long startNum = startNumber + 1;
            int time = 0 ;


            EthTransaction[] txs3 = new EthTransaction[TRANSACTION_NUM - 1];
            for (int i = 0; i < TRANSACTION_NUM - 1; i++) {

                byte[] callData = contract1.getByName("setFruitStock").encode(MOCK_ADDRESS[i], (MOCK_ADDRESS[i + 1] + "马上哟与李的山分局圣诞快乐房价圣诞快乐房价看来！"));
                EthTransaction temp = createTx(stateLedger, sender, contractAddress, callData, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(MOCK_ADDRESS[i].getBytes()))) );
                txs3[i] = temp;
            }


            while (true) {



                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                }




                ConsensusPayload consensusPayload = new ConsensusPayload(txs3);
                consensusPayload.setParsed();

                QuorumCert quorumCert3 = QuorumCertBuilder.buildWithNumber(startNum);
                EventData eventData3 = EventData.buildProposal(new GlobalEvent(), consensusPayload, sender.doGetPubKey(), 1, System.currentTimeMillis(), quorumCert3);
                consensusChainStore.commit(Arrays.asList(eventData3), convertEventSignaturesFrom(eventData3), buildOutputByNumber(eventData3.getNumber()), convertLedgerSignsFrom(eventData3), true);
                startNum++;

                time++;

                if (time == EXE_TIME + 3) {
                    break;
                }

            }



            //consensusChainStore.commit();
        }, "mock_commit_thread").start();
    }


    @Test

    public void queryStore() {

        CompilationResult cres = null;
        try {
            cres = storeTemp();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
//                Hex.decode(cres.getContract("FruitStore").bin), 0);
//        byte[] contractAddress = tx.getContractAddress();

        byte[] contractAddress = HashUtil.calcNewAddr(sender.getAddress(), new BigInteger("1").toByteArray());

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("FruitStore").abi);

//
//
//
//        byte[] callData2 = contract1.getByName("getBalance").encode(MOCK_ADDRESS[6]);
//        EthTransaction tx2 = createTx(stateLedger, sender, contractAddress, callData2, 0l);
//        byte[] returnResult2 = executeTransaction(stateLedger, tx2).getResult().getHReturn();
//        System.out.println(SolidityType.IntType.decodeInt(returnResult2, 0).intValue());
//       // Assert.assertEquals(50000, SolidityType.IntType.decodeInt(returnResult2, 0).intValue());
//
//
//        byte[] callData3 = contract1.getByName("getBalance").encode(MOCK_ADDRESS[TRANSACTION_NUM - 1]);
//        EthTransaction tx3 = createTx(stateLedger, sender, contractAddress, callData3, 0l);
//        byte[] returnResult3 = executeTransaction(stateLedger, tx3).getResult().getHReturn();
//        System.out.println(SolidityType.IntType.decodeInt(returnResult3, 0).intValue());
//       // Assert.assertEquals(EXE_TIME, SolidityType.IntType.decodeInt(returnResult3, 0).intValue());



        for (int i = 0 ;i < TRANSACTION_NUM + 4; i++) {
            byte[] callData4 = contract1.getByName("getStock").encode(MOCK_ADDRESS[i]);
            EthTransaction tx4 = createTx(stateLedger, sender2, contractAddress, callData4, 0l);
            byte[] returnResult4 = executeTransaction(stateLedger, tx4).getResult().getHReturn();

            byte[] result = new byte[returnResult4.length - 64];
            System.arraycopy(returnResult4, 64, result, 0, returnResult4.length - 64);
            try {
                System.out.println(new String(result));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Assert.assertEquals(50000 - EXE_TIME, SolidityType.IntType.decodeInt(returnResult1, 0).intValue());
    }
}
