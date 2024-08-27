package com.thanos.chain.executor;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.crypto.key.asymmetric.ec.ECKeyOld;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.HashUtil;
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
 * AssemblyStoreTest.java description：
 *
 * @Author laiyiyu create on 2021-01-12 10:33:56
 */
public class AssemblyStoreTest extends ExecutorTestBase {

    @Test
    public void pressureTest() {

        AbstractTransactionsExecutor executor = new EthParallelTransactionsExecutor(stateLedger);


        SecureKey sender = SecureKey.fromPrivate(Hex.decode("0100013ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c"));

        CompilationResult cres = null;
        try {
            cres = storeTemp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("AssStorage").abi);

        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("AssStorage").bin), 0);
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

        byte[] callData1 = contract1.getByName("getFruit").encode(Hex.decode("00000000000000000000000000000000000000000000000000000000000200ab"));
        EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData1, 0l);
        byte[] returnResult1 = executeTransaction(stateLedger, tx1).getResult().getHReturn();
//        byte[] result = new byte[returnResult1.length];
//        System.arraycopy(returnResult1, 64, result, 0, returnResult1.length - 64);
        try {
            System.out.println(Hex.toHexString(returnResult1));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public EthTransaction[] createPressureTxs(SecureKey sender, CallTransaction.Contract contract1, byte[] contractAddress) {

        int num = 1;

        EthTransaction[] txs = new EthTransaction[num];

        for (int i = 0; i < num; i++) {
            byte[] callData = contract1.getByName("setFruit").encode( Hex.decode("00000000000000000000000000000000000000000000000000000000000200ab"),Hex.decode("00000000000000000000000000000000000000000000000000000000000200ab"));
            EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(("hehe" + i).getBytes()))) );
            txs[i] = tx1;
            //txs.add(tx1);
        }



        return txs;
    }



    static CompilationResult storeTemp() throws Exception {
        String contract = "contract AssStorage {\n" +
                "    \n" +
                "    \n" +
                "    \n" +
                "    function getFruit(bytes32 key) public returns (bytes32 r) {\n" +
                "        assembly {\n" +
                "            r := sload(key)\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    function setFruit(bytes32 key, bytes32 value) public {\n" +
                "        \n" +
                "        \n" +
                "        assembly {\n" +
                "            \n" +
                "            sstore(key, value)\n" +
                "            \n" +
                "        }\n" +
                "        \n" +
                "    }\n" +
                "    \n" +
                "    \n" +
                "}";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        //logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);
        return cres;
    }

    public static void main(String[] args) {


        byte[] input = HashUtil.sha3(new byte[]{2, 3, 5});

        String inputHex = Hex.toHexString(input);
        byte[] inputDecode = Hex.decode(inputHex);
        System.out.println(inputDecode.length);
    }
}
