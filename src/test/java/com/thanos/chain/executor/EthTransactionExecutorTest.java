package com.thanos.chain.executor;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.contract.eth.solidity.SolidityType;
import com.thanos.chain.contract.eth.solidity.compiler.CompilationResult;
import com.thanos.chain.contract.eth.solidity.compiler.SolidityCompiler;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.chain.storage.db.Repository;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;


/**
 * EthTransactionExecutorTest.java description：
 *
 * @Author laiyiyu create on 2021-06-17 14:36:49
 */
public class EthTransactionExecutorTest {

    static SecureKey sender = SecureKey.fromPrivate(Hex.decode("010001308f761b30da0baa33457550420bb8938d040a0c6f0582d9351fd5cead86ff11"));


    static SystemConfig systemConfig = SystemConfig.getDefault();

    static ConsensusChainStore consensusChainStore = new ConsensusChainStore(systemConfig, true);

    public static StateLedger stateLedger = new StateLedger(systemConfig, null, consensusChainStore, true);





    private EthTransactionReceipt execute(EthTransaction tx) {
        Repository repository = stateLedger.rootRepository.startTracking();
        EthTransactionExecutor executor1 = new EthTransactionExecutor(tx, repository, stateLedger.programInvokeFactory, systemConfig.getGenesis()).withConfig(systemConfig);
        executor1.init();
        executor1.execute();
        executor1.go();
        EthTransactionReceipt receipt = executor1.getReceipt();
        return receipt;
    }


    static Random random = new Random();
    protected EthTransaction createTx(StateLedger stateLedger, SecureKey sender, byte[] receiveAddress,
                                      byte[] data, long value, Set<ByteArrayWrapper> executeStates) {
        BigInteger nonce = stateLedger.rootRepository.getNonce(sender.getAddress());
        byte[] hash = HashUtil.sha3(ByteUtil.longToBytesNoLeadZeroes(System.currentTimeMillis() + random.nextLong()));
        EthTransaction tx = new EthTransaction(
                sender.getPubKey(),
                ByteUtil.bigIntegerToBytes(nonce),
                1,
                ByteUtil.longToBytesNoLeadZeroes(1),
                ByteUtil.longToBytesNoLeadZeroes(3_000_000),
                receiveAddress,
                ByteUtil.longToBytesNoLeadZeroes(value),
                data,
                executeStates,
                hash
        );
        tx.setValid(true);
        //tx.signECDSA(sender);
        return tx;
    }


    @Test
    public void test1() {

        CompilationResult cres = null;
        try {
            String contract = //"pragma solidity ^0.4.25;\n" +
                    "contract TokensDemo {\n" +
                    "    event Transfer(string _from, string _to, uint256 _value);\n" +
                    "    event setMoney(string source, uint256 _value);\n" +
                    "    mapping(string => uint256) userBalance;\n" +
                    "    string spilt4Str = \"@@\";\n" +
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
                    "    function ecmul(uint ax, uint ay, uint k) public view returns(uint[2] memory p) { " +
                    "     uint[3] memory input;\n" +
                    "     input[0] = ax;\n" +
                    "     input[1] = ay;\n" +
                    "     input[2] = k;\n" +
                    "     uint len = 96;\n" +
                    "    \n" +
                    "     assembly {\n" +
                    "       if iszero(staticcall(gas, 0x07, input, len, p, 0x40)) {\n" +
                    "           revert(0,0)\n" +
                    "       }\n" +
                    "     }\n" +
                    "     return p;\n" +
                    "    }     " +
                    "    function bbS04_group_verify(string _sig, string _voteStr, string _gpkInfo, string _paramInfo) public view returns(uint[2] memory p){\n" +
                    "        bytes memory _str1ToBytes = bytes(_sig);\n" +
                    "        bytes memory _str2ToBytes = bytes(_voteStr);\n" +
                    "        bytes memory _str3ToBytes = bytes(_gpkInfo);\n" +
                    "        bytes memory _str4ToBytes = bytes(_paramInfo);\n" +
                    "        bytes memory splitStr = bytes(spilt4Str);\n" +
                    "        uint totalLen  = (_str1ToBytes.length + 2 + _str2ToBytes.length + 2 + _str3ToBytes.length + 2 + _str4ToBytes.length)* 8;\n" +
                    "        //bool success = address(0x09).call(_str1ToBytes,  _str1ToBytesLen, p, 0x40);\n" +
//                    "
//                    "        bytes memory splitBytes =  bytes(splitStr);\n" +
                    "        string memory ret = new string(totalLen);\n" +
                    "        bytes memory content = bytes(ret);\n" +
                    "        uint index = 0;\n" +
                    "        uint i = 0;\n" +
                    "        for (i = 0; i < _str1ToBytes.length; i++) content[index++] = _str1ToBytes[i];\n" +
                    "        i = 0;\n" +
                    "        for (i = 0; i < splitStr.length; i++) content[index++] = splitStr[i];\n" +
                    "        i = 0;\n" +
                    "        for (i = 0; i < _str2ToBytes.length; i++) content[index++] = _str2ToBytes[i];\n" +
                    "        i = 0;\n" +
                    "        for (i = 0; i < splitStr.length; i++) content[index++] = splitStr[i];\n" +
                    "        i = 0;\n" +
                    "        for (i = 0; i < _str3ToBytes.length; i++) content[index++] = _str3ToBytes[i];\n" +
                    "        i = 0;\n" +
                    "        for (i = 0; i < splitStr.length; i++) content[index++] = splitStr[i];\n" +
                    "        i = 0;\n" +
                    "        for (i = 0; i < _str4ToBytes.length; i++) content[index++] = _str4ToBytes[i];\n" +
                    "        assembly {\n" +
                        "       if iszero(staticcall(gas, 0x09, content, totalLen, p, 0x40)) {\n" +
                        "           revert(0,0)\n" +
                        "       }\n" +
                    "        }\n" +
                    "        return p;\n" +
                    "    }" +


                        "    function ring_sign_verify(string _sig, string _msg, string _paramInfo) public view returns(uint[2] memory p){\n" +
                        "        bytes memory _str1ToBytes = bytes(_sig);\n" +
                        "        bytes memory _str2ToBytes = bytes(_msg);\n" +
                        "        bytes memory _str3ToBytes = bytes(_paramInfo);\n" +
                        "        bytes memory splitStr = bytes(spilt4Str);\n" +
                        "        uint totalLen  = (_str1ToBytes.length + 2 + _str2ToBytes.length + 2 + _str3ToBytes.length)* 8;\n" +
//                    "
                        "        string memory ret = new string(totalLen);\n" +
                        "        bytes memory content = bytes(ret);\n" +
                        "        uint index = 0;\n" +
                        "        uint i = 0;\n" +
                        "        for (i = 0; i < _str1ToBytes.length; i++) content[index++] = _str1ToBytes[i];\n" +
                        "        i = 0;\n" +
                        "        for (i = 0; i < splitStr.length; i++) content[index++] = splitStr[i];\n" +
                        "        i = 0;\n" +
                        "        for (i = 0; i < _str2ToBytes.length; i++) content[index++] = _str2ToBytes[i];\n" +
                        "        i = 0;\n" +
                        "        for (i = 0; i < splitStr.length; i++) content[index++] = splitStr[i];\n" +
                        "        i = 0;\n" +
                        "        for (i = 0; i < _str3ToBytes.length; i++) content[index++] = _str3ToBytes[i];\n" +
                        "        assembly {\n" +
                        "       if iszero(staticcall(gas, 0x10, content, totalLen, p, 0x40)) {\n" +
                        "           revert(0,0)\n" +
                        "       }\n" +
                        "        }\n" +
                        "        return p;\n" +
                        "    }" +
                    "}";


            String contract1 = "pragma solidity ^0.4.25;\n" +
                    "contract TokensDemo {\n" +
                    "    \n" +
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
                    "    }\n " +
                    "}";
            SolidityCompiler.Result res = SolidityCompiler.compile(
                    // contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN, SolidityCompiler.Options.AST, new SolidityCompiler.CustomOption("runs", "1"));
                    contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
            //logger.info(res.errors);
            cres = CompilationResult.parse(res.output);
        } catch (Exception e) {
            e.printStackTrace();
        }

        CallTransaction.Contract contract = new CallTransaction.Contract(cres.getContract("TokensDemo").abi);

        //deploy
        EthTransaction tx1 = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("TokensDemo").bin), 0, new HashSet<>());
        EthTransactionReceipt receipt1 = execute(tx1);
        byte[] contractAddress = receipt1.getExecutionResult();

        //set user money
        byte[] callData2 = contract.getByName("setBalance").encode( Hex.toHexString(sender.getAddress()), "9000");
        EthTransaction tx2 = createTx(stateLedger, sender, contractAddress,
                callData2, 0, new HashSet<>());
        EthTransactionReceipt receipt2 = execute(tx2);

        // query user money
        byte[] callData3 = contract.getByName("getBalance").encode(Hex.toHexString(sender.getAddress()));
        EthTransaction tx3 = createTx(stateLedger, sender, contractAddress,
                callData3, 0, new HashSet<>());
        EthTransactionReceipt receipt3 = execute(tx3);
        System.out.println("sender:[" + Hex.toHexString(sender.getAddress()) + "]" + SolidityType.IntType.decodeInt(receipt3.getExecutionResult(), 0).intValue());

        byte[] callData4 = contract.getByName("ecmul").encode("1000", "2000", "90000");
        EthTransaction tx4 = createTx(stateLedger, sender, contractAddress,
                callData4, 0, new HashSet<>());
        EthTransactionReceipt receipt4 = execute(tx4);
        System.out.println("sender:[" + Hex.toHexString(sender.getAddress()) + "]" + Hex.toHexString(receipt4.getExecutionResult()));

        byte[] callData5 = contract.getByName("bbS04_group_verify").encode("sdfsd", "121", "sdf", "sdsdds");
        EthTransaction tx5 = createTx(stateLedger, sender, contractAddress,
                callData5, 0, new HashSet<>());
        EthTransactionReceipt receipt5 = execute(tx5);
        System.out.println("sender:[" + Hex.toHexString(sender.getAddress()) + "]" + Hex.toHexString(receipt5.getExecutionResult()));

        byte[] callData6 = contract.getByName("ring_sign_verify").encode("23", "sds", "111");
        EthTransaction tx6 = createTx(stateLedger, sender, contractAddress,
                callData6, 0, new HashSet<>());
        EthTransactionReceipt receipt6 = execute(tx6);
        System.out.println("sender:[" + Hex.toHexString(sender.getAddress()) + "]" + Hex.toHexString(receipt6.getExecutionResult()));


    }


    @Test
    public void test2() {

        CompilationResult cres = null;
        try {
            String contract = //"pragma solidity ^0.4.25;\n" +
                    "pragma solidity >=0.5.0 <0.5.99;\n" +
                            "// This will not compile after 0.6.0\n" +
                            "\n" +
                            "contract OtherContract {\n" +
                            "    uint x;\n" +
                            "    function f(uint y) external {\n" +
                            "        x = y;\n" +
                            "    }\n" +
                            "    function() payable external {}\n" +
                            "}\n" +
                            "\n" +
                            "contract New {\n" +
                            "    OtherContract other;\n" +
                            "    uint myNumber;\n" +
                            "\n" +
                            "    // Function mutability must be specified.\n" +
                            "    function someInteger() internal pure returns (uint) { return 2; }\n" +
                            "\n" +
                            "    // Function visibility must be specified.\n" +
                            "    // Function mutability must be specified.\n" +
                            "    function f(uint x) public returns (bytes memory) {\n" +
                            "        // The type must now be explicitly given.\n" +
                            "        uint z = someInteger();\n" +
                            "        x += z;\n" +
                            "        // Throw is now disallowed.\n" +
                            "        require(x <= 100);\n" +
                            "        int y = -3 >> 1;\n" +
                            "        require(y == -2);\n" +
                            "        do {\n" +
                            "            x += 1;\n" +
                            "            if (x > 10) continue;\n" +
                            "            // 'Continue' jumps to the condition below.\n" +
                            "        } while (x < 11);\n" +
                            "\n" +
                            "        // Call returns (bool, bytes).\n" +
                            "        // Data location must be specified.\n" +
                            "        (bool success, bytes memory data) = address(other).call(\"f\");\n" +
                            "        if (!success)\n" +
                            "            revert();\n" +
                            "        return data;\n" +
                            "    }\n" +
                            "\n" +
                            "    using address_make_payable for address;\n" +
                            "    // Data location for 'arr' must be specified\n" +
                            "    function g(uint[] memory /* arr */, bytes8 x, OtherContract otherContract, address unknownContract) public payable {\n" +
                            "        // 'otherContract.transfer' is not provided.\n" +
                            "        // Since the code of 'OtherContract' is known and has the fallback\n" +
                            "        // function, address(otherContract) has type 'address payable'.\n" +
                            "        address(otherContract).transfer(1 ether);\n" +
                            "\n" +
                            "        // 'unknownContract.transfer' is not provided.\n" +
                            "        // 'address(unknownContract).transfer' is not provided\n" +
                            "        // since 'address(unknownContract)' is not 'address payable'.\n" +
                            "        // If the function takes an 'address' which you want to send\n" +
                            "        // funds to, you can convert it to 'address payable' via 'uint160'.\n" +
                            "        // Note: This is not recommended and the explicit type\n" +
                            "        // 'address payable' should be used whenever possible.\n" +
                            "        // To increase clarity, we suggest the use of a library for\n" +
                            "        // the conversion (provided after the contract in this example).\n" +
                            "        address payable addr = unknownContract.make_payable();\n" +
                            "        require(addr.send(1 ether));\n" +
                            "\n" +
                            "        // Since uint32 (4 bytes) is smaller than bytes8 (8 bytes),\n" +
                            "        // the conversion is not allowed.\n" +
                            "        // We need to convert to a common size first:\n" +
                            "        bytes4 x4 = bytes4(x); // Padding happens on the right\n" +
                            "        uint32 y = uint32(x4); // Conversion is consistent\n" +
                            "        // 'msg.value' cannot be used in a 'non-payable' function.\n" +
                            "        // We need to make the function payable\n" +
                            "        myNumber += y + msg.value;\n" +
                            "    }\n" +
                            "}\n" +
                            "\n" +
                            "// We can define a library for explicitly converting ``address``\n" +
                            "// to ``address payable`` as a workaround.\n" +
                            "library address_make_payable {\n" +
                            "    function make_payable(address x) internal pure returns (address payable) {\n" +
                            "        return address(uint160(x));\n" +
                            "    }\n" +
                            "}";


            SolidityCompiler.Result res = SolidityCompiler.compile(
                    // contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN, SolidityCompiler.Options.AST, new SolidityCompiler.CustomOption("runs", "1"));
                    contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
            //logger.info(res.errors);
            cres = CompilationResult.parse(res.output);
        } catch (Exception e) {
            e.printStackTrace();
        }

        CallTransaction.Contract contract = new CallTransaction.Contract(cres.getContract("New").abi);

        //deploy
        EthTransaction tx1 = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("New").bin), 0, new HashSet<>());
        EthTransactionReceipt receipt1 = execute(tx1);
        byte[] contractAddress = receipt1.getExecutionResult();

        //set user money
        byte[] callData2 = contract.getByName("f").encode( "5");
        EthTransaction tx2 = createTx(stateLedger, sender, contractAddress,
                callData2, 0, new HashSet<>());
        EthTransactionReceipt receipt2 = execute(tx2);
        System.out.println(SolidityType.IntType.decodeInt(receipt2.getExecutionResult(), 0).intValue());

        // query user money
//        byte[] callData3 = contract.getByName("getBalance").encode(Hex.toHexString(sender.getAddress()));
//        EthTransaction tx3 = createTx(stateLedger, sender, contractAddress,
//                callData3, 0, new HashSet<>());
//        EthTransactionReceipt receipt3 = execute(tx3);
//        System.out.println("sender:[" + Hex.toHexString(sender.getAddress()) + "]" + SolidityType.IntType.decodeInt(receipt3.getExecutionResult(), 0).intValue());
//
//        byte[] callData4 = contract.getByName("ecmul").encode("1000", "2000", "90000");
//        EthTransaction tx4 = createTx(stateLedger, sender, contractAddress,
//                callData4, 0, new HashSet<>());
//        EthTransactionReceipt receipt4 = execute(tx4);
//        System.out.println("sender:[" + Hex.toHexString(sender.getAddress()) + "]" + Hex.toHexString(receipt4.getExecutionResult()));
//
//        byte[] callData5 = contract.getByName("bbS04_group_verify").encode("sdfsd", "121", "sdf", "sdsdds");
//        EthTransaction tx5 = createTx(stateLedger, sender, contractAddress,
//                callData5, 0, new HashSet<>());
//        EthTransactionReceipt receipt5 = execute(tx5);
//        System.out.println("sender:[" + Hex.toHexString(sender.getAddress()) + "]" + Hex.toHexString(receipt5.getExecutionResult()));
//
//        byte[] callData6 = contract.getByName("ring_sign_verify").encode("23", "sds", "111");
//        EthTransaction tx6 = createTx(stateLedger, sender, contractAddress,
//                callData6, 0, new HashSet<>());
//        EthTransactionReceipt receipt6 = execute(tx6);
//        System.out.println("sender:[" + Hex.toHexString(sender.getAddress()) + "]" + Hex.toHexString(receipt6.getExecutionResult()));


    }


}



