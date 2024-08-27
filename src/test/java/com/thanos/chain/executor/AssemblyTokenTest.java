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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static com.thanos.common.utils.HashUtil.EMPTY_DATA_HASH;
import static com.thanos.common.utils.HashUtil.EMPTY_TRIE_HASH;

/**
 * AssemblyTokenTest.java description：
 *
 * @Author laiyiyu create on 2021-01-12 14:24:17
 */
public class AssemblyTokenTest extends ExecutorTestBase  {
    @Test
    public void pressureTest() {

        AbstractTransactionsExecutor executor = new EthParallelTransactionsExecutor(stateLedger);


        ECKeyOld sender = ECKeyOld.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c")).compress();

        CompilationResult cres = null;
        try {
            cres = storeTemp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("AssToken").abi);

        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("AssToken").bin), 0);
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

        byte[] callData1 = contract1.getByName("getBalance").encode(Hex.decode("00000000000000000000000000000000000000000000000000000000000200ab"));
        EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData1, 0l);
        byte[] returnResult1 = executeTransaction(stateLedger, tx1).getResult().getHReturn();
//        byte[] result = new byte[returnResult1.length];
//        System.arraycopy(returnResult1, 64, result, 0, returnResult1.length - 64);
        try {
            System.out.println(SolidityType.Bytes32Type.decodeBytes32(returnResult1, 0));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public EthTransaction[] createPressureTxs(ECKeyOld sender, CallTransaction.Contract contract1, byte[] contractAddress) {

        int num = 1;

        EthTransaction[] txs = new EthTransaction[num];

        for (int i = 0; i < num; i++) {
            byte[] callData = contract1.getByName("transfer").encode( Hex.decode("00000000000000000000000000000000000000000000000000000000000200ab"),Hex.decode("00000000000000000000000000000000000000000000000000000000000200ac"), 20);
            EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData, 0l, new HashSet<>(Arrays.asList(new ByteArrayWrapper(("hehe" + i).getBytes()))) );
            txs[i] = tx1;
            //txs.add(tx1);
        }



        return txs;
    }



    static CompilationResult storeTemp() throws Exception {
        String contract = "contract AssToken {\n" +
                "    \n" +
                "    \n" +
                "    function transfer(bytes32 source, bytes32 to, int32 tokens)  public {\n" +
                "        \n" +
                "        assembly {\n" +
                "            let newsource := sub(sload(source), tokens)\n" +
                "            let newto := add(sload(to), tokens)\n" +
                "            sstore(source, newsource)\n" +
                "            sstore(to, newto)\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    function getBalance(bytes32 source)  view returns (int32 r) {\n" +
                "        assembly {\n" +
                "            r :=sload(source)\n" +
                "        }\n" +
                "    }\n" +
                "}";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        //logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);
        return cres;
    }
}
