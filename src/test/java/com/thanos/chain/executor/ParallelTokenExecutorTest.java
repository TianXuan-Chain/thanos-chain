package com.thanos.chain.executor;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.crypto.key.asymmetric.ec.ECKeyOld;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.contract.eth.solidity.SolidityType;
import com.thanos.chain.contract.eth.solidity.compiler.CompilationResult;
import com.thanos.chain.contract.eth.solidity.compiler.SolidityCompiler;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static com.thanos.common.utils.HashUtil.EMPTY_DATA_HASH;
import static com.thanos.common.utils.HashUtil.EMPTY_TRIE_HASH;
import static com.thanos.chain.executor.ExecutorUtil.sha3LightReceipts;

/**
 * ParallelTokenExecutorTest.java description：
 *
 * @Author laiyiyu create on 2020-02-27 09:42:25
 */
public class ParallelTokenExecutorTest extends ExecutorTestBase {



    @Test
    public void testParallelExe() {
        AbstractTransactionsExecutor executor = new EthParallelTransactionsExecutor(stateLedger);
        Block exeBlock = createBlock1();
        List<EthTransactionReceipt> receipts = executor.execute(exeBlock);
        exeBlock.setReceipts(receipts);
        stateLedger.rootRepository.flush();
        exeBlock.setStateRoot(stateLedger.rootRepository.getRoot());
        exeBlock.setReceiptsRoot(sha3LightReceipts(exeBlock.getReceipts()));
        stateLedger.persist(exeBlock);

        ECKeyOld sender = ECKeyOld.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c")).compress();
        List<EthTransaction> ethTransactions = new ArrayList<>();

        CompilationResult cres = null;
        try {
            cres = tokenComp();
        } catch (Exception e) {
            e.printStackTrace();
        }
        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("TokensDemo").bin), 0);
        byte[] contractAddress = exeBlock.getTransactionsList()[0].getReceiveAddress();

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("TokensDemo").abi);
        byte[] callData1 = contract1.getByName("getBalance").encode("aaa");
        EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData1, 0l);
        byte[] returnResult1 = executeTransaction(stateLedger, tx1).getResult().getHReturn();
        System.out.println(SolidityType.IntType.decodeInt(returnResult1, 0));
        //Assert.assertEquals(455, SolidityType.IntType.decodeInt(returnResult1, 0).intValue());

        byte[] callData2 = contract1.getByName("getBalance").encode("bbb");
        EthTransaction tx2 = createTx(stateLedger, sender, contractAddress, callData2, 0l);
        byte[] returnResult2 = executeTransaction(stateLedger, tx2).getResult().getHReturn();
        System.out.println(SolidityType.IntType.decodeInt(returnResult2, 0));
        //Assert.assertEquals(610, SolidityType.IntType.decodeInt(returnResult2, 0).intValue());

        byte[] callData3 = contract1.getByName("getBalance").encode("ccc");
        EthTransaction tx3 = createTx(stateLedger, sender, contractAddress, callData3, 0l);
        byte[] returnResult3 = executeTransaction(stateLedger, tx3).getResult().getHReturn();
        System.out.println(SolidityType.IntType.decodeInt(returnResult3, 0));
        //Assert.assertEquals(690, SolidityType.IntType.decodeInt(returnResult3, 0).intValue());

        byte[] callData4 = contract1.getByName("getBalance").encode("ddd");
        EthTransaction tx4 = createTx(stateLedger, sender, contractAddress, callData4, 0l);
        byte[] returnResult4 = executeTransaction(stateLedger, tx4).getResult().getHReturn();
        System.out.println(SolidityType.IntType.decodeInt(returnResult4, 0));
        //Assert.assertEquals(810, SolidityType.IntType.decodeInt(returnResult4, 0).intValue());

        byte[] callData5 = contract1.getByName("getBalance").encode("eee");
        EthTransaction tx5 = createTx(stateLedger, sender, contractAddress, callData5, 0l);
        byte[] returnResult5 = executeTransaction(stateLedger, tx5).getResult().getHReturn();
        System.out.println(SolidityType.IntType.decodeInt(returnResult5, 0));
        //Assert.assertEquals(810, SolidityType.IntType.decodeInt(returnResult5, 0).intValue());

        byte[] callData6 = contract1.getByName("getBalance").encode("fff");
        EthTransaction tx6 = createTx(stateLedger, sender, contractAddress, callData6, 0l);
        byte[] returnResult6 = executeTransaction(stateLedger, tx6).getResult().getHReturn();
        System.out.println(SolidityType.IntType.decodeInt(returnResult6, 0));
//        Assert.assertEquals(825, SolidityType.IntType.decodeInt(returnResult6, 0).intValue());
    }



    @Test
    public void pressureTest() {
        AbstractTransactionsExecutor executor = new EthParallelTransactionsExecutor(stateLedger);
        //AbstractTransactionsExecutor executor = new EthSerialTransactionsExecutor(stateLedger);


        ECKeyOld sender = ECKeyOld.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c")).compress();

        CompilationResult cres = null;
        try {
            cres = tokenComp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("TokensDemo").abi);

        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("TokensDemo").bin), 0);
        // 先创建合约
//        executeTransaction(stateLedger, tx);
//        stateLedger.flush();


        Block exeBlock = new Block(EMPTY_DATA_HASH, EMPTY_DATA_HASH, EMPTY_BYTE_ARRAY,  1, 1, System.currentTimeMillis(), EMPTY_TRIE_HASH, EMPTY_TRIE_HASH, new EthTransaction[]{tx});
        List<EthTransactionReceipt> receipts0 = executor.execute(exeBlock);
        exeBlock.setReceipts(receipts0);
        byte[] contractAddress = receipts0.get(0).getExecutionResult();

        stateLedger.rootRepository.flush();
        exeBlock.setStateRoot(stateLedger.ledgerSource.getCurrentStateRootHash());
        exeBlock.setReceiptsRoot(sha3LightReceipts(exeBlock.getReceipts()));
        stateLedger.persist(exeBlock);


        Block setBlock = new Block(EMPTY_DATA_HASH, EMPTY_DATA_HASH, EMPTY_BYTE_ARRAY,  1, 1, System.currentTimeMillis(), EMPTY_TRIE_HASH, EMPTY_TRIE_HASH, createSetPressureTxs(sender, contract1, contractAddress));
        List<EthTransactionReceipt> receipts1 = executor.execute(setBlock);
        setBlock.setReceipts(receipts1);
        stateLedger.rootRepository.flush();
        setBlock.setStateRoot(stateLedger.ledgerSource.getCurrentStateRootHash());
        setBlock.setReceiptsRoot(sha3LightReceipts(setBlock.getReceipts()));
        stateLedger.persist(setBlock);




        long start = System.currentTimeMillis();
        System.out.println("start time:" + start);
        Block block = new Block(EMPTY_DATA_HASH, EMPTY_DATA_HASH, EMPTY_BYTE_ARRAY,  1, 1, System.currentTimeMillis(), EMPTY_TRIE_HASH, EMPTY_TRIE_HASH, createPressureTxs(sender, contract1, contractAddress));
        long end = System.currentTimeMillis();

        System.out.println("create block use:" + (end - start));


        for (int i = 0; i < 20; i++) {
            long start1 = System.currentTimeMillis();
            List<EthTransactionReceipt> receipts = executor.execute(block);
            block.setReceipts(receipts);
            long end1  = System.currentTimeMillis();
            stateLedger.rootRepository.flush();

            long end2  = System.currentTimeMillis();
            block.setReceiptsRoot(sha3LightReceipts(block.getReceipts()));
            long end3  = System.currentTimeMillis();
            block.setStateRoot(stateLedger.rootRepository.getRoot());
            stateLedger.rootRepository.persist(block);
            long end4  = System.currentTimeMillis();


            System.out.println("finish " + i +" all use:[" + (end1 - start1) + "], sha3recepit count [" + block.getReceipts().size() + "]  use:" + (end3 - end2) + " persist cost:" + (end4 - end3));
            //System.out.println(Hex.toHexString(block.getStateRoot()));
            //System.out.println(Hex.toHexString(block.getReceiptsRoot()));


            byte[] callData1 = contract1.getByName("getBalance").encode(MOCK_ADDRESS[0]);
            EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData1, 0l);
            byte[] returnResult1 = executeTransaction(stateLedger, tx1).getResult().getHReturn();
            System.out.println(SolidityType.IntType.decodeInt(returnResult1, 0));
            //Assert.assertEquals(4990, SolidityType.IntType.decodeInt(returnResult1, 0).intValue());

            byte[] callData2 = contract1.getByName("getBalance").encode(MOCK_ADDRESS[100]);
            EthTransaction tx2 = createTx(stateLedger, sender, contractAddress, callData2, 0l);
            byte[] returnResult2 = executeTransaction(stateLedger, tx2).getResult().getHReturn();
            System.out.println(SolidityType.IntType.decodeInt(returnResult2, 0));
            //Assert.assertEquals(5000, SolidityType.IntType.decodeInt(returnResult2, 0).intValue());

            byte[] callData3 = contract1.getByName("getBalance").encode(MOCK_ADDRESS[PRESS_NUM - 1]);
            EthTransaction tx3 = createTx(stateLedger, sender, contractAddress, callData3, 0l);
            byte[] returnResult3 = executeTransaction(stateLedger, tx3).getResult().getHReturn();
            System.out.println(SolidityType.IntType.decodeInt(returnResult3, 0));
            //Assert.assertEquals(5000, SolidityType.IntType.decodeInt(returnResult3, 0).intValue());




            System.out.println();
        }



    }






    private Block createBlock1() {

        return new Block(EMPTY_DATA_HASH, EMPTY_DATA_HASH, EMPTY_BYTE_ARRAY,  1, 1, System.currentTimeMillis(), EMPTY_TRIE_HASH, EMPTY_TRIE_HASH, createTxs1());
    }




    private EthTransaction[] createTxs1() {

        ECKeyOld sender = ECKeyOld.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c")).compress();

        CompilationResult cres = null;
        try {
            cres = tokenComp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("TokensDemo").abi);

        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("TokensDemo").bin), 0);
        //byte[] contractAddress = tx.getContractAddress();
        // 先创建合约
        AbstractTransactionsExecutor executor = new EthParallelTransactionsExecutor(stateLedger);
        Block exeBlock = new Block(EMPTY_DATA_HASH, EMPTY_DATA_HASH, EMPTY_BYTE_ARRAY,  1, 1, System.currentTimeMillis(), EMPTY_TRIE_HASH, EMPTY_TRIE_HASH, new EthTransaction[]{tx});;
        List<EthTransactionReceipt> receipts = executor.execute(exeBlock);
        exeBlock.setReceipts(receipts);
        byte[] contractAddress = receipts.get(0).getExecutionResult();
        stateLedger.rootRepository.flush();
        exeBlock.setStateRoot(stateLedger.ledgerSource.getCurrentStateRootHash());
        exeBlock.setReceiptsRoot(sha3LightReceipts(exeBlock.getReceipts()));
        stateLedger.persist(exeBlock);




        List<EthTransaction> ethTransactions = new ArrayList<>();
        byte[] callData = contract1.getByName("setBalance").encode("aaa", "500");
        EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("aaa".getBytes()))) );
        ethTransactions.add(tx1);

        byte[] callData2 = contract1.getByName("setBalance").encode("bbb", "600");
        EthTransaction tx2 = createTx(stateLedger, sender, contractAddress, callData2, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("bbb".getBytes()))) );
        ethTransactions.add(tx2);


        byte[] callData3 = contract1.getByName("setBalance").encode("ccc", "700");
        EthTransaction tx3 = createTx(stateLedger, sender, contractAddress, callData3, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("ccc".getBytes()))) );
        ethTransactions.add(tx3);

        byte[] callData4 = contract1.getByName("setBalance").encode("ddd", "800");
        EthTransaction tx4 = createTx(stateLedger, sender, contractAddress, callData4, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("ddd".getBytes()))) );
        ethTransactions.add(tx4);

        byte[] callData5 = contract1.getByName("setBalance").encode("eee", "800");
        EthTransaction tx5 = createTx(stateLedger, sender, contractAddress, callData5, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("eee".getBytes()))) );
        ethTransactions.add(tx5);

        byte[] callData6 = contract1.getByName("setBalance").encode("fff", "800");
        EthTransaction tx6 = createTx(stateLedger, sender, contractAddress, callData6, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("fff".getBytes()))) );
        ethTransactions.add(tx6);

        byte[] callData7 = contract1.getByName("transfer").encode("aaa", "eee", "10");
        EthTransaction tx7 = createTx(stateLedger, sender, contractAddress, callData7, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("aaa".getBytes()), new ByteArrayWrapper("eee".getBytes()))) );
        ethTransactions.add(tx7);

        byte[] callData8 = contract1.getByName("transfer").encode("aaa", "fff", "15");
        EthTransaction tx8 = createTx(stateLedger, sender, contractAddress, callData8, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("aaa".getBytes()), new ByteArrayWrapper("fff".getBytes()))) );
        ethTransactions.add(tx8);

        byte[] callData9 = contract1.getByName("transfer").encode("ddd", "eee", "10");
        EthTransaction tx9 = createTx(stateLedger, sender, contractAddress, callData9, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("ddd".getBytes()), new ByteArrayWrapper("eee".getBytes()))) );
        ethTransactions.add(tx9);

        byte[] callData10 = contract1.getByName("transfer").encode("aaa", "bbb", "10");
        EthTransaction tx10 = createTx(stateLedger, sender, contractAddress, callData10, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("aaa".getBytes()), new ByteArrayWrapper("bbb".getBytes()))) );
        ethTransactions.add(tx10);

        byte[] callData11 = contract1.getByName("transfer").encode("eee", "fff", "10");
        EthTransaction tx11 = createTx(stateLedger, sender, contractAddress, callData11, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("eee".getBytes()), new ByteArrayWrapper("fff".getBytes()))) );
        ethTransactions.add(tx11);

        byte[] callData12 = contract1.getByName("transfer").encode("ccc", "ddd", "10");
        EthTransaction tx12 = createTx(stateLedger, sender, contractAddress, callData12, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("ccc".getBytes()), new ByteArrayWrapper("ddd".getBytes()))) );
        ethTransactions.add(tx12);

        byte[] callData13 = contract1.getByName("transfer").encode("aaa", "ddd", "10");
        EthTransaction tx13 = createTx(stateLedger, sender, contractAddress, callData13, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper("aaa".getBytes()), new ByteArrayWrapper("ddd".getBytes()))) );
        ethTransactions.add(tx13);
//
//        byte[] callData4 = contract1.getByName("getBalance").encode("a");
//        EthTransaction tx4 = createTx(stateLedger, sender, contractAddress, callData4, 0l);
//        ethTransactions.add(tx4);
//
//        byte[] callData5 = contract1.getByName("getBalance").encode("b");
//        EthTransaction tx5 = createTx(stateLedger, sender, contractAddress, callData5, 0l);
//        ethTransactions.add(tx5);


        return ethTransactions.toArray(new EthTransaction[ethTransactions.size()]);
    }

    static int PRESS_NUM = 300000;

    public EthTransaction[] createPressureTxs(ECKeyOld sender, CallTransaction.Contract contract1, byte[] contractAddress) {
        List<EthTransaction> txs = new ArrayList<>();

        for (int i = 0; i < PRESS_NUM - 1; i++) {
            byte[] callData7 = contract1.getByName("transfer").encode(MOCK_ADDRESS[i], MOCK_ADDRESS[i + 1], "10");
            EthTransaction tx7 = createTxWithSpecfiyNonce(stateLedger, sender, contractAddress, callData7, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(("a" + i).getBytes()), new ByteArrayWrapper(("a" + (i + 1)).getBytes()))) );
            txs.add(tx7);
        }


        return txs.toArray(new EthTransaction[txs.size()]);
    }


    public EthTransaction[] createSetPressureTxs(ECKeyOld sender, CallTransaction.Contract contract1, byte[] contractAddress) {



        List<EthTransaction> txs = new ArrayList<>();

        for (int i = 0; i < PRESS_NUM; i++) {
            byte[] callData = contract1.getByName("setBalance").encode(MOCK_ADDRESS[i], "50000");
            EthTransaction tx1 = createTxWithSpecfiyNonce(stateLedger, sender, contractAddress, callData, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(("aaa" + i).getBytes()))) );
            txs.add(tx1);
        }

        return txs.toArray(new EthTransaction[txs.size()]);
    }



    public static CompilationResult tokenComp() throws Exception {
        String contract = "pragma solidity ^0.4.25;\n" +
                "contract TokensDemo {\n" +
                "    event Transfer(string _from, string _to, uint256 _value);\n" +
                "    event setMoney(string source, uint256 _value);\n" +
                "    mapping(string => uint256) userBalance;\n" +
                "\n" +
                "    function transfer(string payer, string to, uint256 tokens) public {\n" +
                "        userBalance[payer] = userBalance[payer] - tokens;\n" +
                "        userBalance[to] = userBalance[to] + tokens;\n" +
                "        //emit Transfer(payer, to, tokens);\n" +
                "    }\n" +
                "    \n" +
                "    function setBalance(string source, uint256 tokens) public {\n" +
                "        userBalance[source] = tokens;\n" +
                "        emit setMoney(source, tokens);\n" +
                "    }\n" +
                "    \n" +
                "    function getBalance(string source)  view returns (uint256) {\n" +
                "        return userBalance[source];\n" +
                "    }\n" +
                "}";


        String contract1 = "pragma solidity ^0.4.25;\n" +
                "contract TokensDemo {\n" +
                "    \n" +
                "    function transfer(string source, string to, uint256 tokens) public {\n" +
                "        bytes memory sourcebytes = bytes(source);  \n" +
                "        bytes memory tobytes = bytes(to);  \n" +
                "        assembly {\n" +
                "                          \n" +
                "            let position1 := mload(add(sourcebytes, 32))\n" +
                "            let position2 := mload(add(tobytes, 32))\n" +
                "            //let name := bytes32(fruitName)\n" +
                "            let newsource := sub(sload(position1), tokens)\n" +
                "            let newto := add(sload(position2), tokens)\n" +
                "            sstore(position1, newsource)\n" +
                "            sstore(position2, newto)\n" +
                "        }\n" +
                "        \n" +
                "    }\n" +
                "    \n" +
                "    \n" +
                "    function setBalance(string source, uint256 tokens) public {\n" +
                "        bytes memory sourcebytes = bytes(source);    \n" +
                "        assembly {\n" +
                "                          \n" +
                "            let position1 := mload(add(sourcebytes, 32))\n" +
                "            //let name := bytes32(fruitName)\n" +
                "            sstore(position1, tokens)\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    \n" +
                "    function getBalance(string source)  view returns (uint256 r) {\n" +
                "        bytes memory sourcebytes = bytes(source);    \n" +
                "        assembly {\n" +
                "                          \n" +
                "            let position1 := mload(add(sourcebytes, 32))\n" +
                "            //let name := bytes32(fruitName)\n" +
                "             r :=sload(position1)\n" +
                "        }\n" +
                "        \n" +
                "    }\n" +
                "}";


        String contract2 = "pragma solidity ^0.4.25;\n" +
                "contract TokensDemo {\n" +
                "    \n" +
                "    function transfer(address source, address to, int32 tokens) public {\n" +
                "        // bytes memory sourcebytes = bytes(source);  \n" +
                "        // bytes memory tobytes = bytes(to);  \n" +
                "        assembly {\n" +
                "                          \n" +
                "            // let position1 := mload(add(sourcebytes, 32))\n" +
                "            // let position2 := mload(add(tobytes, 32))\n" +
                "            //let name := bytes32(fruitName)\n" +
                "            let newsource := sub(sload(source), tokens)\n" +
                "            let newto := add(sload(to), tokens)\n" +
                "            sstore(source, newsource)\n" +
                "            sstore(to, newto)\n" +
                "        }\n" +
                "        \n" +
                "    }\n" +
                "    \n" +
                "    \n" +
                "    function setBalance(address source, int32 tokens) public {\n" +
                "        //bytes memory sourcebytes = bytes(source);    \n" +
                "        assembly {\n" +
                "                          \n" +
                "            //let position1 := mload(add(sourcebytes, 32))\n" +
                "            //let name := bytes32(fruitName)\n" +
                "            sstore(source, tokens)\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    \n" +
                "    function getBalance(address source)  view returns (int32 r) {\n" +
                "        //bytes memory sourcebytes = bytes(source);    \n" +
                "        assembly {\n" +
                "                          \n" +
                "            //let position1 := mload(add(sourcebytes, 32))\n" +
                "            //let name := bytes32(fruitName)\n" +
                "             r :=sload(source)\n" +
                "        }\n" +
                "        \n" +
                "    }\n" +
                "}";
        SolidityCompiler.Result res = SolidityCompiler.compile(
               // contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN, SolidityCompiler.Options.AST, new SolidityCompiler.CustomOption("runs", "1"));
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        //logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);
        return cres;
    }


}
