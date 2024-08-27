package com.thanos.chain.executor;

import com.thanos.common.crypto.key.asymmetric.ec.ECKeyOld;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.chain.contract.eth.solidity.compiler.CompilationResult;
import com.thanos.chain.contract.eth.solidity.compiler.SolidityCompiler;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static com.thanos.common.utils.HashUtil.EMPTY_DATA_HASH;
import static com.thanos.common.utils.HashUtil.EMPTY_TRIE_HASH;
import static com.thanos.chain.executor.ExecutorUtil.sha3LightReceipts;

/**
 * HelloworldExecutorTest.java description：
 *
 * @Author laiyiyu create on 2020-10-27 17:42:02
 */
public class HelloworldExecutorTest extends ExecutorTestBase {

    @Test
    public void pressureTest() {
        AbstractTransactionsExecutor executor = new EthSerialTransactionsExecutor(stateLedger);


        ECKeyOld sender = ECKeyOld.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c")).compress();

        CompilationResult cres = null;
        try {
            cres = storeTemp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("HelloWorld").abi);

        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("HelloWorld").bin), 0);
        //byte[] contractAddress = tx.getContractAddress();
        //System.out.println("caddress:" + Hex.toHexString(contractAddress));
        // 先创建合约

        Block block0 = new Block(EMPTY_DATA_HASH, EMPTY_DATA_HASH,  EMPTY_BYTE_ARRAY, 1, 0, System.currentTimeMillis(), EMPTY_TRIE_HASH, EMPTY_TRIE_HASH,  new EthTransaction[]{tx});

        List<EthTransactionReceipt> receipts0 = executor.execute(block0);
        block0.setReceipts(receipts0);
        stateLedger.rootRepository.flush();
        stateLedger.persist(block0);

        block0.setReceipts(receipts0);
        block0.setStateRoot(stateLedger.rootRepository.getRoot());
        block0.setReceiptsRoot(sha3LightReceipts(block0.getReceipts()));
        //System.out.println("stateroot:" + Hex.toHexString(block.getStateRoot()));
        block0.reEncoded();
        stateLedger.persist(block0);
        //executeTransaction(stateLedger, tx);

        byte[] contractAddress = block0.getReceipts().get(0).getExecutionResult();
        System.out.println(Hex.toHexString(contractAddress));
        //=====================================




        byte[] callData = contract1.getByName("set").encode( "呵呵呵1000sadf被誉为");
        EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(("hehe").getBytes()))) );


        Block block1 = new Block(EMPTY_DATA_HASH, EMPTY_DATA_HASH,  EMPTY_BYTE_ARRAY, 1, 1, System.currentTimeMillis(), EMPTY_TRIE_HASH, EMPTY_TRIE_HASH,  new EthTransaction[]{tx1});

        List<EthTransactionReceipt> receipts1 = executor.execute(block1);
        block1.setReceipts(receipts1);
        stateLedger.rootRepository.flush();
        stateLedger.persist(block1);

        block1.setReceipts(receipts1);
        block1.setStateRoot(stateLedger.rootRepository.getRoot());
        block1.setReceiptsRoot(sha3LightReceipts(block1.getReceipts()));
        //System.out.println("stateroot:" + Hex.toHexString(block.getStateRoot()));
        block1.reEncoded();
        stateLedger.persist(block1);

        //============================



        long start = System.currentTimeMillis();
        //Block block = new Block(EMPTY_DATA_HASH, EMPTY_DATA_HASH,  EMPTY_BYTE_ARRAY, 1, 1, System.currentTimeMillis(), EMPTY_TRIE_HASH, EMPTY_TRIE_HASH,  createPressureTxs(sender, contract1, contractAddress));

        long end = System.currentTimeMillis();

        System.out.println("create block use:" + (end - start));



    }


    @Test
    public void query() {
        ECKeyOld sender = ECKeyOld.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c")).compress();

        CompilationResult cres = null;
        try {
            cres = storeTemp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("HelloWorld").bin), 0);
        byte[] contractAddress = Hex.decode("3a28851f70aebf8e5bc083098671dc500c57cfef");
        System.out.println("caddress:" + Hex.toHexString(contractAddress));

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("HelloWorld").abi);
        byte[] callData1 = contract1.getByName("get").encode();
        EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData1, 0l);
        byte[] returnResult1 = executeTransaction(stateLedger, tx1).getResult().getHReturn();
        byte[] result = new byte[returnResult1.length - 64];
        System.arraycopy(returnResult1, 64, result, 0, returnResult1.length - 64);
        try {
            System.out.println(new String(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Assert.assertEquals(50000 - EXE_TIME, SolidityType.IntType.decodeInt(returnResult1, 0).intValue());

    }

    @Test
    public void query2() {
        ECKeyOld sender = ECKeyOld.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c")).compress();

        CompilationResult cres = null;
        try {
            cres = storeTemp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("HelloWorld").bin), 0);
        byte[] contractAddress = Hex.decode("3a28851f70aebf8e5bc083098671dc500c57cfef");
        System.out.println("caddress:" + Hex.toHexString(contractAddress));

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("HelloWorld").abi);
        byte[] callData1 = contract1.getByName("get").encode();
        EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData1, 0l);
        byte[] returnResult1 = stateLedger.ethCall(tx1).getExecutionResult();
        byte[] result = new byte[returnResult1.length - 64];
        System.arraycopy(returnResult1, 64, result, 0, returnResult1.length - 64);
        try {
            System.out.println("result:" + new String(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Assert.assertEquals(50000 - EXE_TIME, SolidityType.IntType.decodeInt(returnResult1, 0).intValue());

    }



    static CompilationResult storeTemp() throws Exception {
        String contract = "pragma solidity ^0.4.0;\n" +
                "\n" +
                "contract HelloWorld {\n" +
                "    event successEvent();\n" +
                "\n" +
                "    string name;\n" +
                "\n" +
                "    uint hellNum;\n" +
                "\n" +
                "    function HelloWorld(){\n" +
                "        name = \"Hi,Welcome!\";\n" +
                "    }\n" +
                "\n" +
                "    function get() constant returns (string){\n" +
                "        return name;\n" +
                "    }\n" +
                "\n" +
                "    function set(string n){\n" +
                "        name = n;\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    function setHello(uint n){\n" +
                "        hellNum = n;\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    function getHelloNum() constant returns (uint){\n" +
                "        return hellNum;\n" +
                "    }\n" +
                "}";



        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        //logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);
        return cres;
    }
}
