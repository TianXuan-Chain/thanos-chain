package com.thanos.chain.executor;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.crypto.key.asymmetric.ec.ECKeyOld;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.txpool.TxnManager;
import com.thanos.chain.consensus.hotstuffbft.model.ConsensusPayload;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.consensus.hotstuffbft.model.QuorumCert;
import com.thanos.chain.contract.eth.solidity.SolidityType;
import com.thanos.chain.contract.eth.solidity.compiler.CompilationResult;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.event.GlobalEvent;
import com.thanos.model.common.QuorumCertBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.HashSet;

import static com.thanos.chain.executor.AsyncExecutorTest.buildOutputByNumber;
import static com.thanos.chain.executor.AsyncExecutorTest.convertEventSignaturesFrom;
import static com.thanos.chain.executor.AsyncExecutorTest.convertLedgerSignsFrom;
import static com.thanos.chain.executor.ParallelTokenExecutorTest.tokenComp;

/**
 * SimpleTokenTest.java description：
 *
 * @Author laiyiyu create on 2020-11-30 10:48:19
 */
@Ignore
public class SimpleTokenTest extends ExecutorTestBase {

    static SecureKey sender = SecureKey.fromPrivate(Hex.decode("010001d9694bc7257b6e11d5a6d3076b28fd9011b46fcc036fbfddf2d6f87866673480"));


    @Test
    public void test1() throws InterruptedException {

        TxnManager txnManager = new TxnManager(100000, 10000, consensusChainStore);

        AsyncExecutor asyncExecutor = new AsyncExecutor(consensusChainStore, stateLedger);


        CompilationResult cres = null;
        try {
            cres = tokenComp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("TokensDemo").abi);


        for (int i = 0; i < 1; i++) {
            innerTest1(cres, contract1);
            System.out.println("finish :" + i);
        }

    }

    private void innerTest1(CompilationResult cres, CallTransaction.Contract contract1) {

        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("TokensDemo").bin), 0);
        //byte[] contractAddress = HashUtil.calcNewAddr(sender.getAddress(), new BigInteger("0").toByteArray());
        //System.out.println("mockConsensusEventSuccess.address:" + Hex.toHexString(contractAddress));
        long startNumber = stateLedger.getLatestConsensusNumber();

        // deploy contract
        QuorumCert quorumCert1 = QuorumCertBuilder.buildWithNumber(startNumber);
        EventData eventData1 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{tx}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert1);
        consensusChainStore.commit(Arrays.asList(eventData1), convertEventSignaturesFrom(eventData1), buildOutputByNumber(eventData1.getNumber()), convertLedgerSignsFrom(eventData1), true);
        Block deployBLock = queryBlock(eventData1.getNumber());
        byte[] contractAddress = deployBLock.getReceipts().get(0).getExecutionResult();
        System.out.println("mockConsensusEventSuccess.address:" + Hex.toHexString(contractAddress));

        //==========================================

        //set user1

        EthTransaction[] txs2 = new EthTransaction[1];
        ECKeyOld user1 = new ECKeyOld();
        byte[] callData = contract1.getByName("setBalance").encode( Hex.toHexString(user1.getAddress()), "9000");
        txs2[0] = createTx(stateLedger, sender, contractAddress, callData, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(user1.getAddress()))) );
        QuorumCert quorumCert2 = QuorumCertBuilder.buildWithNumber(eventData1.getNumber());
        EventData eventData2 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(txs2), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert2);
        consensusChainStore.commit(Arrays.asList(eventData2), convertEventSignaturesFrom(eventData2), buildOutputByNumber(eventData2.getNumber()), convertLedgerSignsFrom(eventData2), true);
        queryBlock(eventData2.getNumber());

        //query
        {
            byte[] callData4 = contract1.getByName("getBalance").encode(Hex.toHexString(user1.getAddress()));
            EthTransaction tx4 = createTx(stateLedger, sender, contractAddress, callData4, 0l);
            byte[] returnResult4 = executeTransaction(stateLedger, tx4).getResult().getHReturn();
            System.out.println("user1:[" + Hex.toHexString(user1.getAddress()) + "]" + SolidityType.IntType.decodeInt(returnResult4, 0).intValue());
        }


        //==========================================

        //set user2

        EthTransaction[] txs3 = new EthTransaction[1];
        ECKeyOld user2 = new ECKeyOld();
        byte[] callData1 = contract1.getByName("setBalance").encode(Hex.toHexString(user2.getAddress()), "9000");
        txs3[0] = createTx(stateLedger, sender, contractAddress, callData1, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(user2.getAddress()))) );
        QuorumCert quorumCert3 = QuorumCertBuilder.buildWithNumber(eventData2.getNumber());
        EventData eventData3 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(txs3), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert3);
        consensusChainStore.commit(Arrays.asList(eventData3), convertEventSignaturesFrom(eventData3), buildOutputByNumber(eventData3.getNumber()), convertLedgerSignsFrom(eventData3), true);
        queryBlock(eventData3.getNumber());

        {
            //query
            byte[] callData5 = contract1.getByName("getBalance").encode(Hex.toHexString(user2.getAddress()));
            EthTransaction tx5 = createTx(stateLedger, sender, contractAddress, callData5, 0l);
            byte[] returnResult5 = executeTransaction(stateLedger, tx5).getResult().getHReturn();
            System.out.println("user2:[" + Hex.toHexString(user2.getAddress()) + "]" + SolidityType.IntType.decodeInt(returnResult5, 0).intValue());
        }



        //==========================================

        // transfer
        EthTransaction[] txs4 = new EthTransaction[1];
        byte[] callData2 = contract1.getByName("transfer").encode(Hex.toHexString(user1.getAddress()), Hex.toHexString(user2.getAddress()), "1");
        txs4[0] = createTx(stateLedger, sender, contractAddress, callData2, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(user2.getAddress()))) );
        QuorumCert quorumCert4 = QuorumCertBuilder.buildWithNumber(eventData3.getNumber());
        EventData eventData4 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(txs4), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert4);
        consensusChainStore.commit(Arrays.asList(eventData4), convertEventSignaturesFrom(eventData4), buildOutputByNumber(eventData4.getNumber()), convertLedgerSignsFrom(eventData4), true);
        queryBlock(eventData4.getNumber());

        {
            byte[] callData4 = contract1.getByName("getBalance").encode(Hex.toHexString(user1.getAddress()));
            EthTransaction tx4 = createTx(stateLedger, sender, contractAddress, callData4, 0l);
            byte[] returnResult4 = executeTransaction(stateLedger, tx4).getResult().getHReturn();
            System.out.println("user1:[" + Hex.toHexString(user1.getAddress()) + "]" + SolidityType.IntType.decodeInt(returnResult4, 0).intValue());
            assert SolidityType.IntType.decodeInt(returnResult4, 0).intValue() == 8999;

        }

        {
            //query
            byte[] callData5 = contract1.getByName("getBalance").encode(Hex.toHexString(user2.getAddress()));
            EthTransaction tx5 = createTx(stateLedger, sender, contractAddress, callData5, 0l);
            byte[] returnResult5 = executeTransaction(stateLedger, tx5).getResult().getHReturn();
            System.out.println("user2:[" + Hex.toHexString(user2.getAddress()) + "]" + SolidityType.IntType.decodeInt(returnResult5, 0).intValue());
            assert SolidityType.IntType.decodeInt(returnResult5, 0).intValue() == 9001;
        }



    }

    private Block queryBlock(long number) {
        System.out.println("current query number:" + number);
        Block deployBLock = null;

        while (true) {

            deployBLock = stateLedger.getBlockByNumber(number);
            if (deployBLock == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }

        }
        return deployBLock;
    }

}
