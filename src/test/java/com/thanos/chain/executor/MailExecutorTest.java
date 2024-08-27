package com.thanos.chain.executor;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.chain.txpool.TxnManager;
import com.thanos.chain.consensus.hotstuffbft.model.ConsensusPayload;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.consensus.hotstuffbft.model.QuorumCert;
import com.thanos.chain.contract.eth.solidity.compiler.CompilationResult;
import com.thanos.chain.contract.eth.solidity.compiler.SolidityCompiler;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.event.GlobalEvent;
import com.thanos.model.common.QuorumCertBuilder;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

import static com.thanos.chain.executor.AsyncExecutorTest.*;
import static com.thanos.chain.executor.AsyncExecutorTest.convertLedgerSignsFrom;

/**
 * 类MailExecutorTest.java的实现描述：
 *
 * @author xuhao create on 2020/12/11 11:30
 */

public class MailExecutorTest extends ExecutorTestBase {
    static SecureKey sender = SecureKey.fromPrivate(Hex.decode("010001d9694bc7257b6e11d5a6d3076b28fd9011b46fcc036fbfddf2d6f87866673480"));


    @Test
    public void test1() throws InterruptedException {

        TxnManager txnManager = new TxnManager(100000, 100000, consensusChainStore);

        AsyncExecutor asyncExecutor = new AsyncExecutor(consensusChainStore, stateLedger);

        CompilationResult cres = null;
        try {
            cres = mailCompileTemp();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        System.out.println("Controller:");
//        String bin = cres.getContract("Controller").bin;
//        System.out.println(bin);
//
//        System.out.println("NotarizationMailHandler:");
//        bin = cres.getContract("NotarizationMailHandler").bin;
//        System.out.println(bin);
//
//        System.out.println("NotarizationMailHandlerFactory:");
//        bin = cres.getContract("NotarizationMailHandlerFactory").bin;
//        System.out.println(bin);
//
//        System.out.println("NotarizationMailProxy:");
//        bin = cres.getContract("NotarizationMailProxy").bin;
//        System.out.println(bin);
//
//        System.out.println("NotarizationMailProxyFactory:");
//        bin = cres.getContract("NotarizationMailProxyFactory").bin;
//        System.out.println(bin);
//
//        System.out.println("NotarizationMailStorage:");
//        bin = cres.getContract("NotarizationMailStorage").bin;
//        System.out.println(bin);
//
//        System.out.println("NotarizationMailStorageFactory:");
//        bin = cres.getContract("NotarizationMailStorageFactory").bin;
//        System.out.println(bin);
        innerTest1(cres);
        System.out.println("finish.");

    }

    static CompilationResult mailCompileTemp() throws Exception {
        String contract = "pragma solidity ^0.4.11;\n" +
                "\n" +
                "contract ExecuteResult {\n" +
                "    event successEvent();\n" +
                "}\n" +
                "\n" +
                "contract Controller is ExecuteResult {\n" +
                "\n" +
                "    address[] public admins;\n" +
                "    mapping(address => uint) adminMap;\n" +
                "\n" +
                "    mapping(bytes32 => address) public registry;\n" +
                "\n" +
                "    function Controller() public {\n" +
                "        admins.push(tx.origin);\n" +
                "        adminMap[tx.origin] = 1;\n" +
                "    }\n" +
                "\n" +
                "    modifier onlyAdmin {\n" +
                "        require(adminMap[tx.origin] == 1);\n" +
                "        _;\n" +
                "    }\n" +
                "\n" +
                "    modifier checkAdminAmount {\n" +
                "        require(admins.length >= 2);\n" +
                "        _;\n" +
                "    }\n" +
                "\n" +
                "    function addAdmin(address account) public onlyAdmin {\n" +
                "        require(adminMap[account] == 0);\n" +
                "        admins.push(account);\n" +
                "        adminMap[account] = 1;\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    function deleteAdmin(address account) public onlyAdmin checkAdminAmount {\n" +
                "        for (uint i = 0; i < admins.length; i++) {\n" +
                "            if (admins[i] == account) {\n" +
                "                delete admins[i];\n" +
                "            }\n" +
                "        }\n" +
                "        adminMap[account] = 0;\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    function getAdminList() public constant returns (address[]){\n" +
                "        return admins;\n" +
                "    }\n" +
                "\n" +
                "    function checkAdmin(address account) public constant returns (bool){\n" +
                "        return adminMap[account] == 1;\n" +
                "    }\n" +
                "\n" +
                "    function stringToBytes32(string memory source) public returns (bytes32 result) {\n" +
                "        bytes memory tempEmptyStringTest = bytes(source);\n" +
                "        if (tempEmptyStringTest.length == 0) {\n" +
                "            return 0x0;\n" +
                "        }\n" +
                "        assembly {\n" +
                "            result := mload(add(source, 32))\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    function register(string _name, address _address) public onlyAdmin returns (bool) {\n" +
                "        bytes32 nameBtye = stringToBytes32(_name);\n" +
                "        require(registry[nameBtye] == 0);\n" +
                "        registry[nameBtye] = _address;\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function update(string _name, address _address) public onlyAdmin returns (bool) {\n" +
                "        bytes32 nameBtye = stringToBytes32(_name);\n" +
                "        registry[nameBtye] = _address;\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function lookup(string _name) public constant returns (address) {\n" +
                "        bytes32 nameBtye = stringToBytes32(_name);\n" +
                "        return registry[nameBtye];\n" +
                "    }\n" +
                "\n" +
                "    function getNotarizationMailStorage() public returns (address){\n" +
                "        return lookup(\"NotarizationMailStorage\");\n" +
                "    }\n" +
                "\n" +
                "    function getNotarizationMailHandler() public returns (address){\n" +
                "        return lookup(\"NotarizationMailHandler\");\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "contract Controlled is ExecuteResult {\n" +
                "\n" +
                "    address public controller;\n" +
                "\n" +
                "    address public owner;\n" +
                "\n" +
                "    modifier onlyAdmin(address _user){\n" +
                "        require(Controller(controller).checkAdmin(_user));\n" +
                "        _;\n" +
                "    }\n" +
                "\n" +
                "    modifier onlyCaller(string name) {\n" +
                "        require(msg.sender == Controller(controller).lookup(name));\n" +
                "        _;\n" +
                "    }\n" +
                "\n" +
                "    modifier onlyOwner {\n" +
                "        require(tx.origin == owner);\n" +
                "        _;\n" +
                "    }\n" +
                "\n" +
                "    function Controlled() public {\n" +
                "        owner = tx.origin;\n" +
                "    }\n" +
                "\n" +
                "    function getOwner() public constant returns (address) {\n" +
                "        return owner;\n" +
                "    }\n" +
                "\n" +
                "    function setController(address _controller) public onlyOwner returns (bool) {\n" +
                "        controller = _controller;\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "//公正邮存储合约\n" +
                "contract NotarizationMailStorage is Controlled {\n" +
                "\n" +
                "    struct Info {\n" +
                "        string emailNumber;\n" +
                "        string emailFingerprint;\n" +
                "        string emailHash;\n" +
                "        address createAddress;\n" +
                "        string extendInfo;\n" +
                "    }\n" +
                "\n" +
                "    mapping(string => Info) mailMap;\n" +
                "\n" +
                "    //底层存储\n" +
                "    function store(string _emailNumber, string _emailFingerprint, string _emailHash, string _extendInfo) public onlyCaller(\"NotarizationMailHandler\") returns (bool){\n" +
                "\n" +
                "        mailMap[_emailNumber] = Info(_emailNumber, _emailFingerprint, _emailHash, tx.origin, _extendInfo);\n" +
                "\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    //修改指纹信息 非必要\n" +
                "    function update(string _emailNumber, string _emailFingerprint, string _emailHash, string _extendInfo) public onlyCaller(\"NotarizationMailHandler\") returns (bool){\n" +
                "        if (mailMap[_emailNumber].createAddress == address(0x0)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "\n" +
                "        mailMap[_emailNumber].emailFingerprint = _emailFingerprint;\n" +
                "        mailMap[_emailNumber].emailHash = _emailHash;\n" +
                "        mailMap[_emailNumber].extendInfo = _extendInfo;\n" +
                "\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    //底层查询接口\n" +
                "    function query(string _emailNumber) public constant returns (string, string, string, address, string){\n" +
                "        return (mailMap[_emailNumber].emailNumber, mailMap[_emailNumber].emailFingerprint, mailMap[_emailNumber].emailHash,\n" +
                "        mailMap[_emailNumber].createAddress, mailMap[_emailNumber].extendInfo);\n" +
                "    }\n" +
                "\n" +
                "    //查询指纹\n" +
                "    function querySample(string _emailNumber) public constant returns (string, string, string){\n" +
                "        return (mailMap[_emailNumber].emailNumber, mailMap[_emailNumber].emailFingerprint, mailMap[_emailNumber].emailHash);\n" +
                "    }\n" +
                "\n" +
                "    //存证数据是否存在\n" +
                "    function exist(string _emailNumber) public constant returns (bool){\n" +
                "        return (mailMap[_emailNumber].createAddress != address(0x0));\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "//公正邮handler\n" +
                "contract NotarizationMailHandler is Controlled {\n" +
                "\n" +
                "    function store(string _emailNumber, string _emailFingerprint, string _emailHash, string _extendInfo)  returns (bool){\n" +
                "        return NotarizationMailStorage(Controller(controller).getNotarizationMailStorage()).store(_emailNumber, _emailFingerprint, _emailHash, _extendInfo);\n" +
                "    }\n" +
                "\n" +
                "    function exist(string _emailNumber) public constant returns (bool){\n" +
                "        return NotarizationMailStorage(Controller(controller).getNotarizationMailStorage()).exist(_emailNumber);\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "//公正邮代理\n" +
                "contract NotarizationMailProxy is Controlled {\n" +
                "\n" +
                "    function store(string _emailNumber, string _emailFingerprint, string _emailHash, string _extendInfo) returns (bool){\n" +
                "        return NotarizationMailHandler(Controller(controller).getNotarizationMailHandler()).store(_emailNumber, _emailFingerprint, _emailHash, _extendInfo);\n" +
                "    }\n" +
                "\n" +
                "    function exist(string _emailNumber) public constant returns (bool){\n" +
                "        return NotarizationMailHandler(Controller(controller).getNotarizationMailStorage()).exist(_emailNumber);\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "//注册or修改NotarizationMailProxy\n" +
                "contract NotarizationMailProxyFactory {\n" +
                "    event createSuccessEvent(address addr);\n" +
                "\n" +
                "    address public controller;\n" +
                "\n" +
                "    function NotarizationMailProxyFactory(address _controller){\n" +
                "        controller = _controller;\n" +
                "    }\n" +
                "\n" +
                "    function createNotarizationMailProxy() public returns (address){\n" +
                "        NotarizationMailProxy proxy = new NotarizationMailProxy();\n" +
                "        Controller(controller).register(\"NotarizationMailProxy\", proxy);\n" +
                "        proxy.setController(controller);\n" +
                "        createSuccessEvent(proxy);\n" +
                "        return proxy;\n" +
                "    }\n" +
                "\n" +
                "    function updateNotarizationMailProxy() public returns (address) {\n" +
                "        NotarizationMailProxy proxy = new NotarizationMailProxy();\n" +
                "        Controller(controller).update(\"NotarizationMailProxy\", proxy);\n" +
                "        proxy.setController(controller);\n" +
                "        createSuccessEvent(proxy);\n" +
                "        return proxy;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "//注册or修改NotarizationMailHandler\n" +
                "contract NotarizationMailHandlerFactory {\n" +
                "    event createSuccessEvent(address addr);\n" +
                "\n" +
                "    address public controller;\n" +
                "\n" +
                "    function NotarizationMailHandlerFactory(address _controller){\n" +
                "        controller = _controller;\n" +
                "    }\n" +
                "\n" +
                "    function createHandler() public returns (address){\n" +
                "        NotarizationMailHandler handler = new NotarizationMailHandler();\n" +
                "        Controller(controller).register(\"NotarizationMailHandler\", handler);\n" +
                "        handler.setController(controller);\n" +
                "        createSuccessEvent(handler);\n" +
                "        return handler;\n" +
                "    }\n" +
                "\n" +
                "    function updateHandler() public returns (address) {\n" +
                "        NotarizationMailHandler handler = new NotarizationMailHandler();\n" +
                "        Controller(controller).update(\"NotarizationMailHandler\", handler);\n" +
                "        handler.setController(controller);\n" +
                "        createSuccessEvent(handler);\n" +
                "        return handler;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n" +
                "//注册NotarizationMailStorage\n" +
                "contract NotarizationMailStorageFactory {\n" +
                "    event createSuccessEvent(address addr);\n" +
                "\n" +
                "    address public controller;\n" +
                "\n" +
                "    function NotarizationMailStorageFactory(address _controller){\n" +
                "        controller = _controller;\n" +
                "    }\n" +
                "\n" +
                "    function createMailStorage() public returns (address) {\n" +
                "        NotarizationMailStorage mailStorage = new NotarizationMailStorage();\n" +
                "        Controller(controller).register(\"NotarizationMailStorage\", mailStorage);\n" +
                "        mailStorage.setController(controller);\n" +
                "        createSuccessEvent(mailStorage);\n" +
                "        return mailStorage;\n" +
                "    }\n" +
                "}\n";


        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        //logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);
        return cres;
    }

    private void innerTest1(CompilationResult cres) {
        CallTransaction.Contract controllerContract = new CallTransaction.Contract(cres.getContract("Controller").abi);
        CallTransaction.Contract mailStorageFactoryContract = new CallTransaction.Contract(cres.getContract("NotarizationMailStorageFactory").abi);
        CallTransaction.Contract mailStorageContract = new CallTransaction.Contract(cres.getContract("NotarizationMailStorage").abi);
        CallTransaction.Contract mailHandlerFactoryContract = new CallTransaction.Contract(cres.getContract("NotarizationMailHandlerFactory").abi);
        CallTransaction.Contract mailProxyFactoryContract = new CallTransaction.Contract(cres.getContract("NotarizationMailProxyFactory").abi);
        CallTransaction.Contract mailProxyContract = new CallTransaction.Contract(cres.getContract("NotarizationMailProxy").abi);


        //byte[] contractAddress = HashUtil.calcNewAddr(sender.getAddress(), new BigInteger("0").toByteArray());
        //System.out.println("mockConsensusEventSuccess.address:" + Hex.toHexString(contractAddress));
        long startNumber = stateLedger.getLatestConsensusNumber();

        // deploy controller contract
        EthTransaction tx = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("Controller").bin), 0);
        QuorumCert quorumCert1 = QuorumCertBuilder.buildWithNumber(startNumber);
        EventData eventData1 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{tx}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert1);
        consensusChainStore.commit(Arrays.asList(eventData1), convertEventSignaturesFrom(eventData1), buildOutputByNumber(eventData1.getNumber()), convertLedgerSignsFrom(eventData1), true);
        Block deployBLock = queryBlock(eventData1.getNumber());
        byte[] controllerContractAddr = deployBLock.getReceipts().get(0).getExecutionResult();
        System.out.println("controllerContract.address:" + Hex.toHexString(controllerContractAddr));

        //==========================================

        // deploy mailStorageFactory contract
        EthTransaction tx2 = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("NotarizationMailStorageFactory").bin + "000000000000000000000000" + Hex.toHexString(controllerContractAddr)), 0);
        QuorumCert quorumCert2 = QuorumCertBuilder.buildWithNumber(eventData1.getNumber());
        EventData eventData2 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{tx2}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert2);
        consensusChainStore.commit(Arrays.asList(eventData2), convertEventSignaturesFrom(eventData2), buildOutputByNumber(eventData2.getNumber()), convertLedgerSignsFrom(eventData2), true);
        Block deployBLock2 = queryBlock(eventData2.getNumber());
        byte[] mailStoreFactoryContractAddr = deployBLock2.getReceipts().get(0).getExecutionResult();
        System.out.println("mailStorageFactoryContract.address:" + Hex.toHexString(mailStoreFactoryContractAddr));
        //create mailStorageContract
        byte[] callData = mailStorageFactoryContract.getByName("createMailStorage").encode();
        EthTransaction callTx1 = createTx(stateLedger, sender, mailStoreFactoryContractAddr, callData, 0l);
        QuorumCert quorumCert3 = QuorumCertBuilder.buildWithNumber(eventData2.getNumber());
        EventData eventData3 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{callTx1}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert3);
        consensusChainStore.commit(Arrays.asList(eventData3), convertEventSignaturesFrom(eventData3), buildOutputByNumber(eventData3.getNumber()), convertLedgerSignsFrom(eventData3), true);
        Block callBlock = queryBlock(eventData3.getNumber());
        EthTransactionReceipt ethTransactionReceipt = callBlock.getReceipts().get(0);
        byte[] mailStorageContractAddr = Hex.decode(Hex.toHexString(ethTransactionReceipt.getExecutionResult()).substring(24));
        System.out.println("mailStorageContract.address:" + Hex.toHexString(mailStorageContractAddr));

        // deploy NotarizationMailHandlerFactory contract
        EthTransaction tx3 = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("NotarizationMailHandlerFactory").bin + "000000000000000000000000" + Hex.toHexString(controllerContractAddr)), 0);
        QuorumCert quorumCert4 = QuorumCertBuilder.buildWithNumber(eventData3.getNumber());
        EventData eventData4 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{tx3}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert4);
        consensusChainStore.commit(Arrays.asList(eventData4), convertEventSignaturesFrom(eventData4), buildOutputByNumber(eventData4.getNumber()), convertLedgerSignsFrom(eventData4), true);
        Block deployBLock3 = queryBlock(eventData4.getNumber());
        byte[] mailHandlerFactoryContractAddr = deployBLock3.getReceipts().get(0).getExecutionResult();
        System.out.println("mailHandlerFactoryContract.address:" + Hex.toHexString(mailHandlerFactoryContractAddr));
        //create NotarizationMailHandler
        byte[] callData2 = mailHandlerFactoryContract.getByName("createHandler").encode();
        EthTransaction callTx2 = createTx(stateLedger, sender, mailHandlerFactoryContractAddr, callData2, 0l);
        QuorumCert quorumCert5 = QuorumCertBuilder.buildWithNumber(eventData4.getNumber());
        EventData eventData5 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{callTx2}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert5);
        consensusChainStore.commit(Arrays.asList(eventData5), convertEventSignaturesFrom(eventData5), buildOutputByNumber(eventData5.getNumber()), convertLedgerSignsFrom(eventData5), true);
        Block callBlock2 = queryBlock(eventData5.getNumber());
        EthTransactionReceipt ethTransactionReceipt2 = callBlock2.getReceipts().get(0);
        byte[] mailHandlerContractAddr = Hex.decode(Hex.toHexString(ethTransactionReceipt2.getExecutionResult()).substring(24));
        System.out.println("mailHandlerContract.address:" + Hex.toHexString(mailHandlerContractAddr));

        // deploy NotarizationMailProxyFactory contract
        EthTransaction tx4 = createTx(stateLedger, sender, new byte[0],
                Hex.decode(cres.getContract("NotarizationMailProxyFactory").bin + "000000000000000000000000" + Hex.toHexString(controllerContractAddr)), 0);
        QuorumCert quorumCert6 = QuorumCertBuilder.buildWithNumber(eventData5.getNumber());
        EventData eventData6 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{tx4}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert6);
        consensusChainStore.commit(Arrays.asList(eventData6), convertEventSignaturesFrom(eventData6), buildOutputByNumber(eventData6.getNumber()), convertLedgerSignsFrom(eventData6), true);
        Block deployBLock4 = queryBlock(eventData6.getNumber());
        byte[] mailProxyFactoryContractAddr = deployBLock4.getReceipts().get(0).getExecutionResult();
        System.out.println("mailProxyFactoryContract.address:" + Hex.toHexString(mailProxyFactoryContractAddr));
        //create NotarizationMailProxy
        byte[] callData3 = mailProxyFactoryContract.getByName("createNotarizationMailProxy").encode();
        EthTransaction callTx3 = createTx(stateLedger, sender, mailProxyFactoryContractAddr, callData3, 0l);
        QuorumCert quorumCert7 = QuorumCertBuilder.buildWithNumber(eventData6.getNumber());
        EventData eventData7 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{callTx3}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert7);
        consensusChainStore.commit(Arrays.asList(eventData7), convertEventSignaturesFrom(eventData7), buildOutputByNumber(eventData7.getNumber()), convertLedgerSignsFrom(eventData7), true);
        Block callBlock3 = queryBlock(eventData7.getNumber());
        EthTransactionReceipt ethTransactionReceipt3 = callBlock3.getReceipts().get(0);
        byte[] mailProxyContractAddr = Hex.decode(Hex.toHexString(ethTransactionReceipt3.getExecutionResult()).substring(24));
        System.out.println("mailProxyContract.address:" + Hex.toHexString(mailProxyContractAddr));

        //store
        byte[] callData4 = mailProxyContract.getByName("store").encode("111", "finger111", "hash111", "extend111");
        EthTransaction callTx4 = createTx(stateLedger, sender, mailProxyContractAddr, callData4, 0l);
        QuorumCert quorumCert8 = QuorumCertBuilder.buildWithNumber(eventData7.getNumber());
        EventData eventData8 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{callTx4}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert8);
        consensusChainStore.commit(Arrays.asList(eventData8), convertEventSignaturesFrom(eventData8), buildOutputByNumber(eventData8.getNumber()), convertLedgerSignsFrom(eventData8), true);
        Block callBlock4 = queryBlock(eventData8.getNumber());
        EthTransactionReceipt ethTransactionReceipt4 = callBlock4.getReceipts().get(0);
        System.out.println("call [store] ethTransactionReceipt:" + ethTransactionReceipt4);
        //query
        byte[] callData5 = mailStorageContract.getByName("query").encode("111");
        EthTransaction callTx5 = createTx(stateLedger, sender, mailStorageContractAddr, callData5, 0l);
        QuorumCert quorumCert9 = QuorumCertBuilder.buildWithNumber(eventData8.getNumber());
        EventData eventData9 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{callTx5}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert9);
        consensusChainStore.commit(Arrays.asList(eventData9), convertEventSignaturesFrom(eventData9), buildOutputByNumber(eventData9.getNumber()), convertLedgerSignsFrom(eventData9), true);
        Block callBlock5 = queryBlock(eventData9.getNumber());
        EthTransactionReceipt ethTransactionReceipt5 = callBlock5.getReceipts().get(0);
        assert "00000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e0000000000000000000000000000000000000000000000000000000000000012000000000000000000000000004fcf6fb1241e1dfc9a7952f687ba28cde85bffa000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000000033131310000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000966696e6765723131310000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000768617368313131000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000009657874656e643131310000000000000000000000000000000000000000000000"
                .equals(Hex.toHexString(ethTransactionReceipt5.getExecutionResult()));
        System.out.println("call [query] ethTransactionReceipt:" + ethTransactionReceipt5);


        //store
        byte[] callData6 = mailProxyContract.getByName("store").encode("111", "finger222", "hash222", "extend222");
        EthTransaction callTx6 = createTx(stateLedger, sender, mailProxyContractAddr, callData6, 0l);
        QuorumCert quorumCert10 = QuorumCertBuilder.buildWithNumber(eventData9.getNumber());
        EventData eventData10 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{callTx6}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert10);
        consensusChainStore.commit(Arrays.asList(eventData10), convertEventSignaturesFrom(eventData10), buildOutputByNumber(eventData10.getNumber()), convertLedgerSignsFrom(eventData10), true);
        Block callBlock6 = queryBlock(eventData10.getNumber());
        EthTransactionReceipt ethTransactionReceipt6 = callBlock6.getReceipts().get(0);
        System.out.println("call [store2] ethTransactionReceipt:" + ethTransactionReceipt6);
        //query
        byte[] callData7 = mailStorageContract.getByName("query").encode("111");
        EthTransaction callTx7 = createTx(stateLedger, sender, mailStorageContractAddr, callData7, 0l);
        QuorumCert quorumCert11 = QuorumCertBuilder.buildWithNumber(eventData10.getNumber());
        EventData eventData11 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{callTx7}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert11);
        consensusChainStore.commit(Arrays.asList(eventData11), convertEventSignaturesFrom(eventData9), buildOutputByNumber(eventData11.getNumber()), convertLedgerSignsFrom(eventData11), true);
        Block callBlock7 = queryBlock(eventData11.getNumber());
        EthTransactionReceipt ethTransactionReceipt7 = callBlock7.getReceipts().get(0);
        assert "00000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e0000000000000000000000000000000000000000000000000000000000000012000000000000000000000000004fcf6fb1241e1dfc9a7952f687ba28cde85bffa000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000000033131310000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000966696e6765723232320000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000768617368323232000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000009657874656e643232320000000000000000000000000000000000000000000000"
                .equals(Hex.toHexString(ethTransactionReceipt7.getExecutionResult()));
        System.out.println("call [query2] ethTransactionReceipt:" + ethTransactionReceipt7);

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
