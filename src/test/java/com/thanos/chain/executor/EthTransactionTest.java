/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.thanos.chain.executor;


import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.common.crypto.key.asymmetric.ec.ECKey;
import com.thanos.common.crypto.key.asymmetric.ec.ECKeyOld;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.contract.eth.evm.program.ProgramResult;
import com.thanos.chain.contract.eth.solidity.SolidityType;
import com.thanos.chain.contract.eth.solidity.compiler.CompilationResult;
import com.thanos.chain.contract.eth.solidity.compiler.SolidityCompiler;
import com.thanos.chain.ledger.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static com.thanos.common.utils.HashUtil.EMPTY_DATA_HASH;
import static com.thanos.common.utils.HashUtil.EMPTY_TRIE_HASH;
import static com.thanos.chain.executor.ExecutorUtil.sha3LightReceipts;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.slf4j.LoggerFactory.getLogger;

public class EthTransactionTest extends ExecutorTestBase {
    private final static Logger logger = getLogger(EthTransactionTest.class);

    @Test /* signECDSA transaction  https://tools.ietf.org/html/rfc6979 */
    public void test1() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, IOException {

        //python taken exact data
        String txRLPRawData = "a9e880872386f26fc1000085e8d4a510008203e89413978aee95f38490e9769c39b2773ed763d9cd5f80";
        // String txRLPRawData = "f82804881bc16d674ec8000094cd2a3d9f938e13cd947ec05abc7fe734df8dd8268609184e72a0006480";

        byte[] cowPrivKey = Hex.decode("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4");
        ECKeyOld key = ECKeyOld.fromPrivate(cowPrivKey);

        byte[] data = Hex.decode(txRLPRawData);

        // step 1: serialize + RLP encode
        // step 2: hash = keccak(step1)
        byte[] txHash = HashUtil.sha3(data);

        String signature = key.doSign(txHash).toBase64();
        logger.info(signature);
    }



    String tokensbin = "608060405234801561001057600080fd5b50610448806100206000396000f3006080604052600436106100565763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166338639f7b811461005b5780633a51d246146100b85780639b80b05014610123575b600080fd5b34801561006757600080fd5b506040805160206004803580820135601f81018490048402850184019095528484526100b694369492936024939284019190819084018382808284375094975050933594506101bc9350505050565b005b3480156100c457600080fd5b506040805160206004803580820135601f81018490048402850184019095528484526101119436949293602493928401919081908401838280828437509497506102249650505050505050565b60408051918252519081900360200190f35b34801561012f57600080fd5b506040805160206004803580820135601f81018490048402850184019095528484526100b694369492936024939284019190819084018382808284375050604080516020601f89358b018035918201839004830284018301909452808352979a999881019791965091820194509250829150840183828082843750949750509335945061028b9350505050565b806000836040518082805190602001908083835b602083106101ef5780518252601f1990920191602091820191016101d0565b51815160209384036101000a600019018019909216911617905292019485525060405193849003019092209290925550505050565b600080826040518082805190602001908083835b602083106102575780518252601f199092019160209182019101610238565b51815160209384036101000a6000190180199092169116179052920194855250604051938490030190922054949350505050565b806000846040518082805190602001908083835b602083106102be5780518252601f19909201916020918201910161029f565b51815160209384036101000a6000190180199092169116179052920194855250604051938490038101842054885195900394600094899450925082918401908083835b602083106103205780518252601f199092019160209182019101610301565b51815160209384036101000a6000190180199092169116179052920194855250604051938490038101842094909455505083518392600092869290918291908401908083835b602083106103855780518252601f199092019160209182019101610366565b51815160209384036101000a60001901801990921691161790529201948552506040519384900381018420548751950194600094889450925082918401908083835b602083106103e65780518252601f1990920191602091820191016103c7565b51815160209384036101000a600019018019909216911617905292019485525060405193849003019092209290925550505050505600a165627a7a7230582055f686ddd0f81ec3c7810fe30b70b7f9d30883d14cddaca938a1f4aa40af443f0029";


    @Test
    public void memoryRecycleTest()throws IOException, InterruptedException {
        String contract = "pragma solidity ^0.4.0;\n" +
                "\n" +
                "\n" +
                "//import \"strings.sol\";\n" +
                "\n" +
                "\n" +
                "contract HelloWorld{\n" +
                "    string name;\n" +
                "\n" +
                "    uint hellNum;\n" +
                "\n" +
                "    function HelloWorld(){\n" +
                "       name=\"Hi,Welcome!\";\n" +
                "    }\n" +
                "    function get()constant returns(string){\n" +
                "        return name;\n" +
                "    }\n" +
                "    function set(string n){\n" +
                "    \tname=n;\n" +
                "    }\n" +
                "\n" +
                "  function setHello(uint n){\n" +
                "    \thellNum=n;\n" +
                "    }\n" +
                "\n" +
                "        function getHelloNum() constant returns(uint){\n" +
                "            return hellNum;\n" +
                "        }\n" +
                "\n" +
                "\n" +
                "       function rectangle(uint w, uint h) constant returns(uint s, uint p) {\n" +
                "            s = w * h;\n" +
                "            p = 2 * (w + h);\n" +
                "        }\n" +
                "\n" +
                "\n" +
                "         function quick_sort(uint256[] input) public constant returns(uint[]) {\n" +
                "             quickSort(input, 0, input.length - 1);\n" +
                "\n" +
                "             return input;\n" +
                "         }\n" +
                "\n" +
                "        function partition(uint[] arr, uint left, uint right) returns(uint) {\n" +
                "             uint temp = arr[left];\n" +
                "             while (right > left) {\n" +
                "\n" +
                "                 while (temp <= arr[right] && left < right) {\n" +
                "                     --right;\n" +
                "                 }\n" +
                "                 if (left < right) {\n" +
                "                     arr[left] = arr[right];\n" +
                "                     ++left;\n" +
                "                 }\n" +
                "\n" +
                "                 while (temp >= arr[left] && left < right) {\n" +
                "                     ++left;\n" +
                "                 }\n" +
                "                 if (left < right) {\n" +
                "                     arr[right] = arr[left];\n" +
                "                     --right;\n" +
                "                 }\n" +
                "             }\n" +
                "             arr[left] = temp;\n" +
                "             return left;\n" +
                "         }\n" +
                "\n" +
                "         function quickSort(uint[] arr, uint left, uint right) {\n" +
                "             if (left >= right || arr.length <= 1)\n" +
                "                 return;\n" +
                "             uint mid = partition(arr, left, right);\n" +
                "             quickSort(arr, left, mid);\n" +
                "             quickSort(arr, mid + 1, right);\n" +
                "         }\n" +
                "\n" +
                "\n" +
                "        function hello(string input) constant returns(string) {\n" +
                "            return input;\n" +
                "        }\n" +
                "\n" +
                "\n" +
                "        function getArr(uint[] input) constant returns(uint[]) {\n" +
                "            return input;\n" +
                "        }\n" +
                "}\n";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);

        SecureKey sender = SecureKey.fromPrivate(Hex.decode("0100013ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c"));
        logger.info("address: " + Hex.toHexString(sender.getAddress()));

        if (cres.getContract("HelloWorld") != null) {
            CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("HelloWorld").abi);

            EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                    Hex.decode(cres.getContract("HelloWorld").bin));
            byte[] contractAddress = tx.getContractAddress();
            executeTransaction(stateLedger, tx);

            byte[] asserRes1 = new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,11,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,12,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,43,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,44,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,56};
            byte[] asserRes2 = new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,12,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,44,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,56};
            byte[] asserRes3 = new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,-88};
            System.out.println("asserRes3:" + SolidityType.IntType.decodeInt(asserRes3, 0).intValue());
            System.out.println("start test =================>");
            for (int i = 0; i < 10; i++) {
                int[] temp1 = new int[] {12,43,2,11, 44, 3, 56};
                byte[] callData1 = contract1.getByName("quick_sort").encode(temp1);
                EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData1, 0l);
                byte[] executeRuturn1 = executeTransaction(stateLedger, tx1).getResult().getHReturn();

                int[] temp2 = new int[] {12,7, 44, 3, 56};
                byte[] callData2 = contract1.getByName("quick_sort").encode(temp2);
                EthTransaction tx2 = createTx(stateLedger, sender, contractAddress, callData2, 0l);
                byte[] executeRuturn2 = executeTransaction(stateLedger, tx2).getResult().getHReturn();

                byte[] callData3 = contract1.getByName("rectangle").encode(12, 14);
                EthTransaction tx3 = createTx(stateLedger, sender, contractAddress, callData3, 0l);
                byte[] executeRuturn3 = executeTransaction(stateLedger, tx3).getResult().getHReturn();


                Assert.assertTrue(Arrays.equals(asserRes1, executeRuturn1));
                Assert.assertTrue(Arrays.equals(asserRes2, executeRuturn2));
                System.out.println();

                for (byte bytes: executeRuturn1) {
                    System.out.print(bytes + ",");
                }
                System.out.println("");
                for (byte bytes: executeRuturn2) {
                    System.out.print(bytes + ",");
                }
                System.out.println("");
                for (byte bytes: executeRuturn3) {
                    System.out.print(bytes + ",");
                }
                System.out.println("");

            }




        }
    }

    @Test
    public void tokentset() throws IOException, InterruptedException {
        String contract = "pragma solidity ^0.4.11;\n" +
                "contract TokensDemo {\n" +
                "    mapping(string => uint256) userBalance;\n" +
                "\n" +
                "    function transfer(string payer, string to, uint256 tokens) public {\n" +
                "        userBalance[payer] = userBalance[payer] - tokens;\n" +
                "        userBalance[to] = userBalance[to] + tokens;\n" +
                "    }\n" +
                "    \n" +
                "    function setBalance(string source, uint256 tokens) public {\n" +
                "        userBalance[source] = tokens;\n" +
                "    }\n" +
                "    \n" +
                "    function getBalance(string source)  view returns (uint256) {\n" +
                "        return userBalance[source];\n" +
                "    }\n" +
                "}";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);

        SecureKey sender = SecureKey.fromPrivate(Hex.decode("0100013ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c"));
        logger.info("address: " + Hex.toHexString(sender.getAddress()));

        if (cres.getContract("TokensDemo") != null) {
            CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("TokensDemo").abi);

            EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                    Hex.decode(cres.getContract("TokensDemo").bin));
            byte[] contractAddress = tx.getContractAddress();
            executeTransaction(stateLedger, tx);

            byte[] callData = contract1.getByName("setBalance").encode("a", "500");
            EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData, 0l);
            executeTransaction(stateLedger, tx1).getResult();


            byte[] callData2 = contract1.getByName("setBalance").encode("b", "14");
            EthTransaction tx2 = createTx(stateLedger, sender, contractAddress, callData2, 0l);
            executeTransaction(stateLedger, tx2).getResult();


            byte[] callData3 = contract1.getByName("transfer").encode("a", "b", "100");
            EthTransaction tx3 = createTx(stateLedger, sender, contractAddress, callData3, 0l);
            executeTransaction(stateLedger, tx3).getResult();


            byte[] callData4 = contract1.getByName("getBalance").encode("a");
            EthTransaction tx4 = createTx(stateLedger, sender, contractAddress, callData4, 0l);
            byte[] returnResulta = executeTransaction(stateLedger, tx4).getResult().getHReturn();

            byte[] callData5 = contract1.getByName("getBalance").encode("b");
            EthTransaction tx5 = createTx(stateLedger, sender, contractAddress, callData5, 0l);
            byte[] returnResultb = executeTransaction(stateLedger, tx5).getResult().getHReturn();

            System.out.println(SolidityType.IntType.decodeInt(returnResulta, 0));
            System.out.println(SolidityType.IntType.decodeInt(returnResultb, 0));
            Assert.assertEquals(400, SolidityType.IntType.decodeInt(returnResulta, 0).intValue());
            Assert.assertEquals(114, SolidityType.IntType.decodeInt(returnResultb, 0).intValue());
            stateLedger.rootRepository.flush();
        } else {
            Assert.fail();
        }


    }

    @Test
    public void multiSuicideTest() throws IOException, InterruptedException {
        String contract =
                "pragma solidity ^0.4.3;" +
                "contract PsychoKiller {" +
                "    function () payable {}" +
                "    function homicide() {" +
                "        suicide(msg.sender);" +
                "    }" +
                "    function multipleHomocide() {" +
                "        PsychoKiller k  = this;" +
                "        k.homicide.gas(10000)();" +
                "        k.homicide.gas(10000)();" +
                "        k.homicide.gas(10000)();" +
                "        k.homicide.gas(10000)();" +
                "    }" +
                "}";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);



        SecureKey sender = SecureKey.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c"));
        logger.info("address: " + Hex.toHexString(sender.getAddress()));

        if (cres.getContract("PsychoKiller") != null) {
            EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                    Hex.decode(cres.getContract("PsychoKiller").bin));
            executeTransaction(stateLedger, tx);

            byte[] contractAddress = tx.getContractAddress();

            CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("PsychoKiller").abi);
            byte[] callData = contract1.getByName("multipleHomocide").encode();

            EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData, 0l);
            ProgramResult programResult = executeTransaction(stateLedger, tx1).getResult();

            // suicide of a single account should be counted only once
            Assert.assertEquals(0, programResult.getFutureRefund());
        } else {
            Assert.fail();
        }
    }


    @Test
    public void testMap() throws IOException, InterruptedException {
        String contract =
                "pragma solidity >=0.4.0 <0.7.0;\n" +
                        "\n" +
                        "contract MappingExample {\n" +
                        "    mapping(address => uint) public balances;\n" +
                        "\n" +
                        "    function update(uint newBalance) public {\n" +
                        "        balances[msg.sender] = newBalance;\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "contract MappingUser {\n" +
                        "    function testF() public returns (uint) {\n" +
                        "        MappingExample m = new MappingExample();\n" +
                        "        m.update(100);\n" +
                        "        return m.balances(address(this));\n" +
                        "    }\n" +
                        "}";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);



        SecureKey sender = SecureKey.fromPrivate(Hex.decode("0100013ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c"));
        logger.info("address: " + Hex.toHexString(sender.getAddress()));


        AbstractTransactionsExecutor executor = new EthSerialTransactionsExecutor(stateLedger);



        if (cres.getContract("MappingUser") != null) {
            EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                    //Hex.decode(cres.getContract("MappingUser").bin));
                    Hex.decode("608060405234801561001057600080fd5b50610168806100206000396000f3fe608060405234801561001057600080fd5b5060043610610053576000357c01000000000000000000000000000000000000000000000000000000009004806327e235e31461005857806382ab890a146100b0575b600080fd5b61009a6004803603602081101561006e57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506100de565b6040518082815260200191505060405180910390f35b6100dc600480360360208110156100c657600080fd5b81019080803590602001909291905050506100f6565b005b60006020528060005260406000206000915090505481565b806000803373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055505056fea165627a7a7230582037c9172664d00de59dc8c78b2ca25bb1ca2c0d8b443787a8b2a0195f0dca971e0029"));
//            executeTransaction(stateLedger, tx);
//            stateLedger.flush();

            Block block = new Block(EMPTY_DATA_HASH, EMPTY_DATA_HASH,  EMPTY_BYTE_ARRAY, 1, 1, System.currentTimeMillis(), EMPTY_TRIE_HASH, EMPTY_TRIE_HASH,  new EthTransaction[]{tx});


            List<EthTransactionReceipt> receipts = executor.execute(block);
            block.setReceipts(receipts);
            stateLedger.rootRepository.flush();
            block.setStateRoot(stateLedger.ledgerSource.getCurrentStateRootHash());
            block.setReceiptsRoot(sha3LightReceipts(block.getReceipts()));
            stateLedger.persist(block);

            System.out.println("==============================");
            byte[] contractAddress = tx.getContractAddress();

            CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.getContract("MappingUser").abi);
            byte[] callData = contract1.getByName("testF").encode();

            EthTransaction tx1 = createTx(stateLedger, sender, contractAddress, callData, 0l);
            ProgramResult programResult = executeTransaction(stateLedger, tx1).getResult();
            System.out.println("exe return:" + Hex.toHexString(programResult.getHReturn()));

            // suicide of a single account should be counted only once
            //Assert.assertEquals(0, programResult.getFutureRefund());
        } else {
            Assert.fail();
        }
    }



    @Test
    public void testRLP() {
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


        System.out.println(tx);


        for (int k = 0; k < 1; k++) {
            testSHA(tx.getEncoded());
        }


        System.out.println(new EthTransaction(tx.getEncoded()));


        EthTransaction tx1 = new EthTransaction(
                sender,
                ByteUtil.longToBytesNoLeadZeroes(currentTime + i),
                1,
                ByteUtil.longToBytesNoLeadZeroes(currentTime - i),
                ByteUtil.longToBytesNoLeadZeroes(currentTime + 3_000_000 + i),
                null,
                ByteUtil.longToBytesNoLeadZeroes(currentTime - 3_000_000 - i),
                receiveAddress,
                new HashSet<>(Arrays.asList(new ByteArrayWrapper(("hehe" + i).getBytes()))),
                sender
                );


        System.out.println(tx1);



        System.out.println(new EthTransaction(tx1.getEncoded()));


    }



    private static void testSHA(byte[] raw) {


        long start = System.currentTimeMillis();


        for (int i = 0; i < 100000; i++) {
            HashUtil.sha3(raw);
        }
        long end = System.currentTimeMillis();
        System.out.println("testSHA use:" + (end - start) + "ms");


    }

//    {
//        "linkReferences": {},

//        "object": "608060405234801561001057600080fd5b50610168806100206000396000f3fe608060405234801561001057600080fd5b5060043610610053576000357c01000000000000000000000000000000000000000000000000000000009004806327e235e31461005857806382ab890a146100b0575b600080fd5b61009a6004803603602081101561006e57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506100de565b6040518082815260200191505060405180910390f35b6100dc600480360360208110156100c657600080fd5b81019080803590602001909291905050506100f6565b005b60006020528060005260406000206000915090505481565b806000803373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055505056fea165627a7a7230582037c9172664d00de59dc8c78b2ca25bb1ca2c0d8b443787a8b2a0195f0dca971e0029",
//        "object": "608060405234801561001057600080fd5b506103bb806100206000396000f3fe608060405234801561001057600080fd5b5060043610610048576000357c010000000000000000000000000000000000000000000000000000000090048063f0d061b41461004d575b600080fd5b61005561006b565b6040518082815260200191505060405180910390f35b6000806100766101f7565b604051809103906000f080158015610092573d6000803e3d6000fd5b5090508073ffffffffffffffffffffffffffffffffffffffff166382ab890a60646040518263ffffffff167c010000000000000000000000000000000000000000000000000000000002815260040180828152602001915050600060405180830381600087803b15801561010557600080fd5b505af1158015610119573d6000803e3d6000fd5b505050508073ffffffffffffffffffffffffffffffffffffffff166327e235e3306040518263ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060206040518083038186803b1580156101b657600080fd5b505afa1580156101ca573d6000803e3d6000fd5b505050506040513d60208110156101e057600080fd5b810190808051906020019092919050505091505090565b604051610188806102088339019056fe608060405234801561001057600080fd5b50610168806100206000396000f3fe608060405234801561001057600080fd5b5060043610610053576000357c01000000000000000000000000000000000000000000000000000000009004806327e235e31461005857806382ab890a146100b0575b600080fd5b61009a6004803603602081101561006e57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506100de565b6040518082815260200191505060405180910390f35b6100dc600480360360208110156100c657600080fd5b81019080803590602001909291905050506100f6565b005b60006020528060005260406000206000915090505481565b806000803373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055505056fea165627a7a72305820fd598bfbb466913bba0c16303880770e5cbdc477e1325376ee529b92b14d7cb70029a165627a7a723058205b54ceb8c6fcfd75d4c33d010ce1ed346c794d909975727385445b5abf6d35020029",
//            "opcodes": "PUSH1 0x80 PUSH1 0x40 MSTORE CALLVALUE DUP1 ISZERO PUSH2 0x10 JUMPI PUSH1 0x0 DUP1 REVERT JUMPDEST POP PUSH2 0x168 DUP1 PUSH2 0x20 PUSH1 0x0 CODECOPY PUSH1 0x0 RETURN INVALID PUSH1 0x80 PUSH1 0x40 MSTORE CALLVALUE DUP1 ISZERO PUSH2 0x10 JUMPI PUSH1 0x0 DUP1 REVERT JUMPDEST POP PUSH1 0x4 CALLDATASIZE LT PUSH2 0x53 JUMPI PUSH1 0x0 CALLDATALOAD PUSH29 0x100000000000000000000000000000000000000000000000000000000 SWAP1 DIV DUP1 PUSH4 0x27E235E3 EQ PUSH2 0x58 JUMPI DUP1 PUSH4 0x82AB890A EQ PUSH2 0xB0 JUMPI JUMPDEST PUSH1 0x0 DUP1 REVERT JUMPDEST PUSH2 0x9A PUSH1 0x4 DUP1 CALLDATASIZE SUB PUSH1 0x20 DUP2 LT ISZERO PUSH2 0x6E JUMPI PUSH1 0x0 DUP1 REVERT JUMPDEST DUP2 ADD SWAP1 DUP1 DUP1 CALLDATALOAD PUSH20 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF AND SWAP1 PUSH1 0x20 ADD SWAP1 SWAP3 SWAP2 SWAP1 POP POP POP PUSH2 0xDE JUMP JUMPDEST PUSH1 0x40 MLOAD DUP1 DUP3 DUP2 MSTORE PUSH1 0x20 ADD SWAP2 POP POP PUSH1 0x40 MLOAD DUP1 SWAP2 SUB SWAP1 RETURN JUMPDEST PUSH2 0xDC PUSH1 0x4 DUP1 CALLDATASIZE SUB PUSH1 0x20 DUP2 LT ISZERO PUSH2 0xC6 JUMPI PUSH1 0x0 DUP1 REVERT JUMPDEST DUP2 ADD SWAP1 DUP1 DUP1 CALLDATALOAD SWAP1 PUSH1 0x20 ADD SWAP1 SWAP3 SWAP2 SWAP1 POP POP POP PUSH2 0xF6 JUMP JUMPDEST STOP JUMPDEST PUSH1 0x0 PUSH1 0x20 MSTORE DUP1 PUSH1 0x0 MSTORE PUSH1 0x40 PUSH1 0x0 KECCAK256 PUSH1 0x0 SWAP2 POP SWAP1 POP SLOAD DUP2 JUMP JUMPDEST DUP1 PUSH1 0x0 DUP1 CALLER PUSH20 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF AND PUSH20 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF AND DUP2 MSTORE PUSH1 0x20 ADD SWAP1 DUP2 MSTORE PUSH1 0x20 ADD PUSH1 0x0 KECCAK256 DUP2 SWAP1 SSTORE POP POP JUMP INVALID LOG1 PUSH6 0x627A7A723058 KECCAK256 CALLDATACOPY 0xc9 OR 0x26 PUSH5 0xD00DE59DC8 0xc7 DUP12 0x2c LOG2 JUMPDEST 0xb1 0xca 0x2c 0xd DUP12 DIFFICULTY CALLDATACOPY DUP8 0xa8 0xb2 LOG0 NOT 0x5f 0xd 0xca SWAP8 0x1e STOP 0x29 ",
//            "sourceMap": "35:175:0:-;;;;8:9:-1;5:2;;;30:1;27;20:12;5:2;35:175:0;;;;;;;"
//    }

}
