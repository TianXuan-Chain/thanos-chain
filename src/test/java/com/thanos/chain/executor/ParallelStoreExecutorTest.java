package com.thanos.chain.executor;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.common.crypto.key.asymmetric.ec.ECKeyOld;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.contract.eth.evm.DataWord;
import com.thanos.chain.contract.eth.solidity.compiler.CompilationResult;
import com.thanos.chain.contract.eth.solidity.compiler.SolidityCompiler;
import com.thanos.chain.ledger.model.Block;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static com.thanos.common.utils.HashUtil.EMPTY_DATA_HASH;
import static com.thanos.common.utils.HashUtil.EMPTY_TRIE_HASH;

/**
 * ParallelStoreExecutorTest.java description：
 *
 * @Author laiyiyu create on 2020-06-23 17:20:12
 */
public class ParallelStoreExecutorTest extends ExecutorTestBase {


    @Test
    public void pressureTest() {
        System.out.println(Runtime.getRuntime().availableProcessors());
        AbstractTransactionsExecutor executor = new EthParallelTransactionsExecutor(stateLedger);


        ECKeyOld sender = ECKeyOld.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c")).compress();

        CompilationResult cres = null;
        try {
            cres = storeTemp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("FruitStore").abi);

        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("FruitStore").bin), 0);
        // 先创建合约

        Block block0 = new Block(EMPTY_DATA_HASH, EMPTY_DATA_HASH,  EMPTY_BYTE_ARRAY, 1, 0, System.currentTimeMillis(), EMPTY_TRIE_HASH, EMPTY_TRIE_HASH,  new EthTransaction[]{tx});

        List<EthTransactionReceipt> receipts0 = executor.execute(block0);
        block0.setReceipts(receipts0);
        stateLedger.rootRepository.flush();
        stateLedger.persist(block0);
        //executeTransaction(stateLedger, tx);

        byte[] contractAddress = receipts0.get(0).getExecutionResult();
        System.out.println("caddress:" + Hex.toHexString(contractAddress));


        long start = System.currentTimeMillis();
        Block block = new Block(EMPTY_DATA_HASH, EMPTY_DATA_HASH,  EMPTY_BYTE_ARRAY, 1, 1, System.currentTimeMillis(), EMPTY_TRIE_HASH, EMPTY_TRIE_HASH,  createPressureTxs(sender, contract1, contractAddress));

        long end = System.currentTimeMillis();

        System.out.println("create block use:" + (end - start));


        for (int i = 0; i < 1; i++) {
            long start1 = System.currentTimeMillis();
            List<EthTransactionReceipt> receipts = executor.execute(block);
            block.setReceipts(receipts);
            stateLedger.rootRepository.flush();
            stateLedger.persist(block);
            long end1  = System.currentTimeMillis();

            System.out.println("finish " + i +" all use:" + (end1 - start1));





            System.out.println();
        }

        byte[] callData1 = contract1.getByName("getStock").encode("0");
        EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData1, 0l);
        byte[] returnResult1 = executeTransaction(stateLedger, tx1).getResult().getHReturn();
        byte[] result = new byte[returnResult1.length - 64];
        System.arraycopy(returnResult1, 64, result, 0, returnResult1.length - 64);
        try {
            System.out.println(new String(result));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public EthTransaction[] createPressureTxs(ECKeyOld sender, CallTransaction.Contract contract1, byte[] contractAddress) {

        int num = 1;

        EthTransaction[] txs = new EthTransaction[num];

        for (int i = 0; i < num; i++) {
            //byte[] callData = contract1.getByName("setFruitStock").encode( "帆帆恍恍惚惚帆恍恍惚惚帆恍恍惚惚帆恍恍惚惚帆恍恍惚惚帆恍恍惚惚帆恍恍惚惚帆恍恍惚惚帆恍恍惚惚帆恍恍惚惚夫妇SpendCheck.updat简单简单简SpendCheck.updat单恍惚惚帆恍恍惚惚帆恍恍惚惚夫妇SpendCheck.updat简单简单简SpendCheck.updat单简单简单简单简单简单简单简单简单简单简单简单hehe简单简单简单简单简单简单简单简单简单简单简单简单简单简单hehe简单简单简单简单简单简单简单vcvsfsdfd简单简单简单简单简单简单简ssdf单hehe" +  i,i + "");
            byte[] callData = contract1.getByName("setFruitStock").encode( "abc" +  i,i + "");
            EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(("hehe" + i).getBytes()))) );
            txs[i] = tx1;
            //txs.add(tx1);
        }



        return txs;
    }


    @Test
    public void testStoreAddress() {
//
//        SSTORE value:ad3228b676f7d3cd4284a5443f17f1962b36e491b30a40b2405849e597ba5fb5
//        SSTORE value:ada5013122d395ba3c54772283fb069b10426056ef8ca54750cb9bb552a59e7d
//        SSTORE value:abbb5caa7dda850e60932de0934eb1f9d0f59695050f761dc64e443e5030a569
//        SSTORE value:101e368776582e57ab3d116ffe2517c0a585cd5b23174b01e275c2d8329c3d83
//        SSTORE value:52d75039926638d3c558b2bdefb945d5be8dae29dedd1c313212a4d472d9fde5
//        SSTORE value:2b232c97452f0950c94e2539fdc7e69d21166113cf7a9bcb99b220a3fe5d720a
//        SSTORE value:62103cf3131c85df57aad364d21cba02556d3092d6cb54c298c2e7726a7870bd
//        SSTORE value:870253054e3d98b71abec8fff9ebf8a15d167f15909091a800d4acaab9266d2b
//        SSTORE value:5b8b9143058ba3a137192c563ca6541845e62f0a2f9a667aac4db2fa3c334e3c
//        SSTORE value:324fdf7bfe7bd2828491073f0b7868a9a19ee3eff384c2805040be3e426447f5
//
        System.out.println(Hex.toHexString(HashUtil.sha3(ByteUtil.merge(DataWord.of(ByteUtil.intToBytes(0)).getData(), DataWord.of(ByteUtil.intToBytes(0)).getData()))));
        System.out.println(Hex.toHexString(HashUtil.sha3(ByteUtil.merge(DataWord.of(ByteUtil.intToBytes(1)).getData(), DataWord.of(ByteUtil.intToBytes(0)).getData()))));
        System.out.println(Hex.toHexString(HashUtil.sha3(ByteUtil.merge(DataWord.of(ByteUtil.intToBytes(2)).getData(), DataWord.of(ByteUtil.intToBytes(0)).getData()))));
        System.out.println(Hex.toHexString(HashUtil.sha3(ByteUtil.merge(DataWord.of(ByteUtil.intToBytes(3)).getData(), DataWord.of(ByteUtil.intToBytes(0)).getData()))));
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
                Hex.decode(cres.getContract("FruitStore").bin), 0);
        byte[] contractAddress = tx.getContractAddress();
        System.out.println("caddress:" + Hex.toHexString(contractAddress));

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("FruitStore").abi);
        byte[] callData1 = contract1.getByName("getStock").encode("1");
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

    static CompilationResult storeTemp() throws Exception {
        String contract = "pragma solidity ^0.4.25;\n" +
                " \n" +
                "contract FruitStore {\n" +
                "    mapping(string => string) _fruitStock;\n" +
                "  \n" +
                "    function getStock(string fruit) external view returns(string) {\n" +
                "        return _fruitStock[fruit];\n" +
                "    }\n" +
                "    function setFruitStock(string stock, string fruitName)  external {\n" +
                "        _fruitStock[fruitName] = stock;\n" +
                "    }\n" +
                "}";



        String contract1 = "pragma solidity ^0.4.25;\n" +
                "                contract FruitStore {\n" +
                "                    mapping(uint => string) _fruitStock;\n" +
                "                \n" +
                "                    function getStock(uint fruit) external view returns(string) {\n" +
                "                        return _fruitStock[fruit];\n" +
                "                    }\n" +
                "                    function setFruitStock(string fruitName, uint stock)  external {\n" +
                "                       //bytes32  fruitNamebytes = bytes32(fruitName);\n" +
                "                       bytes memory fruitNamebytes = bytes(fruitName);\n" +
                "                       bytes32 position1 = keccak256(bytes32(stock), bytes32(0));\n" +
                "\n" +
                "                       assembly {\n" +
                "                           \n" +
                "                           let fruitName32 := mload(add(fruitNamebytes, 32))\n" +
                "                           sstore(position1, fruitName32)\n" +
                "                       }\n" +
                "                    }\n" +
                "                }";


        String contract2 = "pragma solidity ^0.4.25;\n" +
                "                contract FruitStore {\n" +
                "\n" +
                "                    function getStock(uint fruit) external {\n" +
                "                        //bytes32 position1 = bytes32(fruit);\n" +
                "                        assembly{\n" +
                "                            \n" +
                "                             let v := 0 // 函数风格赋值，作为变量声明的一部分\n" +
                "                             let g := add(v, 2)\n" +
                "                             sload(10)\n" +
                "                             =: v // 指令风格赋值，将sload（10）的结果放入v\n" +
                "                            \n" +
                "                        }\n" +
                "                    }\n" +
                "                    \n" +
                "                    function f(uint x) public returns (uint r) {\n" +
                "                        assembly {\n" +
                "                            r := sload(x) // 因为偏移量为 0，所以可以忽略\n" +
                "                        }\n" +
                "                    }\n" +
                "                    \n" +
                "                    function setFruitStock(string fruitName, String stock)  external {\n" +
                "                       //bytes32  fruitNamebytes = bytes32(fruitName);\n" +
                "                       bytes memory fruitNamebytes = bytes(fruitName);\n" +
                "                       bytes32 position1 = bytes32(stock);\n" +
                "\n" +
                "                       assembly {\n" +
                "                          \n" +
                "                           let fruitName32 := mload(add(fruitNamebytes, 32))\n" +
                "                           //let name := bytes32(fruitName)\n" +
                "                           sstore(position1, fruitName32)\n" +
                "                       }\n" +
                "                    }\n" +
                "                }\n" +
                "    ";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        //logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);
        return cres;
    }
}
