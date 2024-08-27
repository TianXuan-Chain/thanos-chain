package com.thanos.chain.executor;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
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

/**
 * 类MailExecutorTest.java的实现描述：
 *
 * @author xuhao create on 2020/12/11 11:30
 */

public class TokenExecutorTest extends ExecutorTestBase {
    static SecureKey sender = SecureKey.fromPrivate(Hex.decode("010001d9694bc7257b6e11d5a6d3076b28fd9011b46fcc036fbfddf2d6f87866673480"));


    @Test
    public void test1() throws InterruptedException {

//        TxnManager txnManager = new TxnManager(100000, consensusChainStore);

//        AsyncExecutor asyncExecutor = new AsyncExecutor(consensusChainStore, stateLedger);


        CompilationResult cres = null;
        try {
            cres = mailCompileTemp();
            System.out.println(cres.getContract("FTBAwardPoolFactory").bin);
        } catch (Exception e) {
            e.printStackTrace();
        }


//        deployContract(cres);
//        System.out.println("finish.");

    }

    private void deployContract(CompilationResult cres) {
        CallTransaction.Contract mailStorageFactoryContract = new CallTransaction.Contract(cres.getContract("NotarizationMailStorageFactory").abi);


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
        System.out.println("call ethTransactionReceipt:" + ethTransactionReceipt);
        //store
        byte[] callData2 = mailStorageFactoryContract.getByName("store").encode("111","finger111");
        EthTransaction callTx2 = createTx(stateLedger, sender, mailStoreFactoryContractAddr, callData2, 0l);
        QuorumCert quorumCert4 = QuorumCertBuilder.buildWithNumber(eventData3.getNumber());
        EventData eventData4 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{callTx2}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert4);
        consensusChainStore.commit(Arrays.asList(eventData4), convertEventSignaturesFrom(eventData4), buildOutputByNumber(eventData4.getNumber()), convertLedgerSignsFrom(eventData4), true);
        Block callBlock2 = queryBlock(eventData3.getNumber());
        EthTransactionReceipt ethTransactionReceipt2 = callBlock2.getReceipts().get(0);
        System.out.println("call [store] ethTransactionReceipt:" + ethTransactionReceipt2);
        //query
        byte[] callData3 = mailStorageFactoryContract.getByName("query").encode("111");
        EthTransaction callTx3 = createTx(stateLedger, sender, mailStoreFactoryContractAddr, callData3, 0l);
        QuorumCert quorumCert5 = QuorumCertBuilder.buildWithNumber(eventData4.getNumber());
        EventData eventData5 = EventData.buildProposal(new GlobalEvent(), new ConsensusPayload(new EthTransaction[]{callTx3}), sender.getPubKey(), 1, System.currentTimeMillis(), quorumCert5);
        consensusChainStore.commit(Arrays.asList(eventData5), convertEventSignaturesFrom(eventData5), buildOutputByNumber(eventData5.getNumber()), convertLedgerSignsFrom(eventData5), true);
        Block callBlock3 = queryBlock(eventData5.getNumber());
        EthTransactionReceipt ethTransactionReceipt3 = callBlock3.getReceipts().get(0);
        System.out.println("call [query] ethTransactionReceipt:" + ethTransactionReceipt3);
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



    static CompilationResult mailCompileTemp() throws Exception {
        String contract = "pragma solidity ^0.4.16;\n" +
                "\n" +
                "contract ExecuteResult {\n" +
                "    event successEvent();\n" +
                "}\n" +
                "\n" +
                "library SafeMath {\n" +
                "    function add(uint a, uint b) internal returns (uint c) {\n" +
                "        c = a + b;\n" +
                "        require(c >= a);\n" +
                "    }\n" +
                "    function sub(uint a, uint b) internal returns (uint c) {\n" +
                "        require(b <= a);\n" +
                "        c = a - b;\n" +
                "    }\n" +
                "    function mul(uint a, uint b) internal returns (uint c) {\n" +
                "        c = a * b;\n" +
                "        require(a == 0 || c / a == b);\n" +
                "    }\n" +
                "    function div(uint a, uint b) internal returns (uint c) {\n" +
                "        require(b > 0);\n" +
                "        c = a / b;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "contract Controller is ExecuteResult {\n" +
                "\n" +
                "    address[] public admins;\n" +
                "    mapping(address=>uint) adminMap;\n" +
                "\n" +
                "    mapping(bytes32 => address) public registry;\n" +
                "\n" +
                "    function Controller() public {\n" +
                "        admins.push(tx.origin);\n" +
                "        adminMap[tx.origin] = 1;\n" +
                "    }\n" +
                "\n" +
                "    modifier onlyAdmin {\n" +
                "        require(adminMap[tx.origin]==1);\n" +
                "        _;\n" +
                "    }\n" +
                "\n" +
                "    modifier checkAdminAmount {\n" +
                "        require(admins.length >=2);\n" +
                "        _;\n" +
                "    }\n" +
                "\n" +
                "    function addAdmin(address account) public onlyAdmin{\n" +
                "        require(adminMap[account] == 0);\n" +
                "        admins.push(account);\n" +
                "        adminMap[account] = 1;\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    function deleteAdmin(address account) public onlyAdmin checkAdminAmount {\n" +
                "        for(uint i=0;i<admins.length;i++){\n" +
                "            if(admins[i]==account){\n" +
                "                delete admins[i];\n" +
                "            }\n" +
                "        }\n" +
                "        adminMap[account] = 0;\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    function getAdminList() public constant returns(address[]){\n" +
                "        return admins;\n" +
                "    }\n" +
                "\n" +
                "    function checkAdmin(address account) public constant returns(bool){\n" +
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
                "    function register(string _name, address _address) public onlyAdmin returns(bool) {\n" +
                "        bytes32 nameBtye = stringToBytes32(_name);\n" +
                "        require(registry[nameBtye]==0);\n" +
                "        registry[nameBtye] = _address;\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function update(string _name, address _address) public onlyAdmin returns(bool) {\n" +
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
                "    function getFTBTokenHandler() public returns(address){\n" +
                "        return lookup(\"FTBTokenHandler\");\n" +
                "    }\n" +
                "\n" +
                "    function getFTBToken() public returns(address){\n" +
                "        return lookup(\"FTBToken\");\n" +
                "    }\n" +
                "\n" +
                "    function getAwardPool() public returns(address){\n" +
                "        return lookup(\"AwardPool\");\n" +
                "    }\n" +
                "\n" +
                "    function getFreeze() public returns(address){\n" +
                "        return lookup(\"Freeze\");\n" +
                "    }\n" +
                "\n" +
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
                "    function getOwner() public constant returns(address) {\n" +
                "        return owner;\n" +
                "    }\n" +
                "\n" +
                "    function setController(address _controller) public onlyOwner returns(bool) {\n" +
                "        controller = _controller;\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "contract Freeze is Controlled {\n" +
                "\n" +
                "    address[] public freezes;\n" +
                "    mapping(address => uint8) frozedMap;\n" +
                "\n" +
                "    // 冻结账户\n" +
                "    function freeze(address account) public onlyAdmin(tx.origin) {\n" +
                "        require(frozedMap[account] == 0);\n" +
                "        freezes.push(account);\n" +
                "        frozedMap[account] = 1;\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    // 解冻账户\n" +
                "    function unFreeze(address account) public onlyAdmin(tx.origin) {\n" +
                "        for(uint i=0;i<freezes.length;i++){\n" +
                "            if(freezes[i]==account){\n" +
                "                delete freezes[i];\n" +
                "            }\n" +
                "        }\n" +
                "        frozedMap[account] = 0;\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    // 校验是否冻结账户\n" +
                "    function checkFreeze(address account) public constant returns(bool){\n" +
                "        return frozedMap[account]==1;\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "contract AwardPool is Controlled {\n" +
                "\n" +
                "    using SafeMath for uint;\n" +
                "\n" +
                "    // 每日tokens\n" +
                "    uint dayTokens;\n" +
                "    // 衰减天数\n" +
                "    uint reductionDays;\n" +
                "    // 衰减率\n" +
                "    uint reductionRate;\n" +
                "    // 已经完成分发天数\n" +
                "    uint digDays;\n" +
                "    // 挖取日期\n" +
                "    mapping(uint=> uint) recordMap;\n" +
                "\n" +
                "    // 运营-每日tokens\n" +
                "    uint operateDayTokens;\n" +
                "    // 运营-已经完成分发天数\n" +
                "    uint operateDigDays;\n" +
                "    // 运营-挖取日期\n" +
                "    mapping(uint=> uint) operateRecordMap;\n" +
                "\n" +
                "    // 玩家矿池分配账户 nb001\n" +
                "    address _distributor;\n" +
                "    // 伏羲币用户币销毁账户\n" +
                "    address _surpluser;\n" +
                "    // 伏羲币系统提币操作账户\n" +
                "    address _extractor;\n" +
                "    //伏伏羲币游戏方可支配账号,每日解封6万到该账号\n" +
                "    address _operator;\n" +
                "\n" +
                "    /**\n" +
                "     * dayTokens     每天挖取\n" +
                "     * reductionDays 每2年衰减\n" +
                "     * reductionRate 衰减1/2\n" +
                "     * digDays       挖矿天数\n" +
                "     */\n" +
                "    function AwardPool() public {\n" +
                "        dayTokens = 50000 * 100000000;\n" +
                "        reductionDays = 365 * 2;\n" +
                "        reductionRate = 2;\n" +
                "        digDays = 0;\n" +
                "        _extractor = 0xa14e6d3572df85a13ced0c7c4b8ed91240a54dd7;\n" +
                "        _distributor = 0x11b8b06a8ff4f057df6ae9754380314a5a9f476e;\n" +
                "        _surpluser = 0x71830ac09ed6de1ed44c5215fb6bde5d9ebdc820;\n" +
                "        operateDayTokens = 61000 * 100000000;\n" +
                "        operateDigDays = 0;\n" +
                "        _operator = 0x7df8e507f088b122bdad6c5bcf99c7f6f2a619af;\n" +
                "    }\n" +
                "\n" +
                "    // 计算应发tokens量\n" +
                "    function calMinerDigTokens() public returns(uint){\n" +
                "        if (digDays <= reductionDays) {\n" +
                "            return dayTokens;\n" +
                "        } else {\n" +
                "            uint t = digDays.div(reductionDays);\n" +
                "            uint rate = 1;\n" +
                "            for (uint i = 0; i < t; i++) {\n" +
                "                rate *= reductionRate;\n" +
                "            }\n" +
                "            return dayTokens.div(rate);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    // 运营-计算应发tokens量\n" +
                "    function calOperateDigTokens() public returns(uint){\n" +
                "        if (operateDigDays <= reductionDays) {\n" +
                "            return operateDayTokens;\n" +
                "        } else {\n" +
                "            uint t = operateDigDays.div(reductionDays);\n" +
                "            uint rate = 1;\n" +
                "            for (uint i = 0; i < t; i++) {\n" +
                "                rate *= reductionRate;\n" +
                "            }\n" +
                "            return operateDayTokens.div(rate);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * 仅FTBTokenhandler调用挖矿\n" +
                "     * 幂等控制，每天仅能挖取一次\n" +
                "     * 计算出今天应该挖取得量\n" +
                "     */\n" +
                "    function minerTokens(uint time) public onlyCaller(\"FTBTokenHandler\") returns(uint tokens){\n" +
                "        // 控制每天不能重复挖取\n" +
                "        require(_distributor != address(0));\n" +
                "        require(recordMap[time] == 0);\n" +
                "        digDays++;\n" +
                "        uint tokenAmount = calMinerDigTokens();\n" +
                "        recordMap[time] = tokenAmount;\n" +
                "        return tokenAmount;\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * 运营 -\n" +
                "     * 仅FTBTokenhandler调用挖矿\n" +
                "     * 幂等控制，每天仅能挖取一次\n" +
                "     * 计算出今天应该挖取得量\n" +
                "     */\n" +
                "    function minerOperatorTokens(uint time) public onlyCaller(\"FTBTokenHandler\") returns(uint tokens){\n" +
                "        // 控制每天不能重复挖取\n" +
                "        require(operateRecordMap[time] == 0);\n" +
                "        operateDigDays++;\n" +
                "        uint tokenAmount = calOperateDigTokens();\n" +
                "        operateRecordMap[time] = tokenAmount;\n" +
                "        return tokenAmount;\n" +
                "    }\n" +
                "\n" +
                "    // 获取挖矿信息\n" +
                "    function getMineInfo() public constant returns(address, address, address, uint){\n" +
                "        return (_extractor, _distributor, _surpluser, digDays);\n" +
                "    }\n" +
                "\n" +
                "    // 获取挖矿信息\n" +
                "    function getRecord(uint time) public constant returns(uint){\n" +
                "        return recordMap[time];\n" +
                "    }\n" +
                "\n" +
                "    // 分发账户\n" +
                "    function getDistributor() public constant returns(address) {\n" +
                "        return _distributor;\n" +
                "    }\n" +
                "\n" +
                "    function getExtractor() public constant returns(address){\n" +
                "        return _extractor;\n" +
                "    }\n" +
                "\n" +
                "    // 获取运营账户\n" +
                "    function getOperatorMineInfo() public constant returns (address, address, uint){\n" +
                "        return (_extractor, _operator, operateDigDays);\n" +
                "    }\n" +
                "\n" +
                "    // 获取运营挖矿信息\n" +
                "    function getOperatorRecord(uint time) public constant returns (uint) {\n" +
                "        return operateRecordMap[time];\n" +
                "    }\n" +
                "\n" +
                "    // 获取解封运营账户\n" +
                "    function getOperator() public constant returns (address){\n" +
                "        return _operator;\n" +
                "    }\n" +
                "\n" +
                "    // 校验提币账户\n" +
                "    function checkExtractor(address _user) public constant returns(bool){\n" +
                "        return _user == _extractor ? true : false;\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "contract ERC20Interface {\n" +
                "    function totalSupply() public constant returns (uint);\n" +
                "    function balanceOf(address tokenOwner) public constant returns (uint balance);\n" +
                "    function transfer(address to, uint tokens) public returns (bool success);\n" +
                "    function allowance(address tokenOwner, address spender) public constant returns (uint remaining);\n" +
                "    function approve(address spender, uint tokens) public returns (bool success);\n" +
                "    function transferFrom(address from, address to, uint tokens) public returns (bool success);\n" +
                "\n" +
                "    event Transfer(address indexed from, address indexed to, uint tokens);\n" +
                "    event Approval(address indexed tokenOwner, address indexed spender, uint tokens);\n" +
                "}\n" +
                "\n" +
                "contract IEmergency{\n" +
                "    function getEmergency() public constant returns(bool);\n" +
                "    function emergencyStop() public returns(bool);\n" +
                "    function release() public returns(bool);\n" +
                "}\n" +
                "\n" +
                "contract FTBToken is IEmergency, Controlled, ERC20Interface {\n" +
                "\n" +
                "    using SafeMath for uint;\n" +
                "\n" +
                "    string symbol;\n" +
                "    string name;\n" +
                "    uint8 decimals;\n" +
                "    uint _totalSupply;\n" +
                "\n" +
                "    mapping(address => uint) balances;\n" +
                "    mapping(address => mapping(address => uint)) allowed;\n" +
                "\n" +
                "    bool emergency = false;\n" +
                "\n" +
                "    address operator;\n" +
                "\n" +
                "    modifier checkEmergency{\n" +
                "        require(emergency == false);\n" +
                "        _;\n" +
                "    }\n" +
                "\n" +
                "    modifier checkNonOperator{\n" +
                "        require(tx.origin != operator);\n" +
                "        _;\n" +
                "    }\n" +
                "\n" +
                "    function FTBToken() public {\n" +
                "        symbol = \"FTB\";\n" +
                "        name = \"FTB tokens\";\n" +
                "        decimals = 8;\n" +
                "        _totalSupply = (73000000 + 90000000) * 100000000;\n" +
                "        balances[this] = 73000000 * 100000000;\n" +
                "        operator = 0x14b70566d9825b8849fb013bd2eb7e0d74503025;\n" +
                "        balances[operator] = 90000000 * 100000000;\n" +
                "        Transfer(address(0), tx.origin, _totalSupply);\n" +
                "    }\n" +
                "\n" +
                "    function totalSupply() public constant returns (uint) {\n" +
                "        return _totalSupply;\n" +
                "    }\n" +
                "\n" +
                "    function balanceOf(address tokenOwner) public constant returns (uint balance) {\n" +
                "        return balances[tokenOwner];\n" +
                "    }\n" +
                "\n" +
                "    function transfer(address to, uint tokens) public checkEmergency checkNonOperator onlyCaller(\"FTBTokenHandler\") returns (bool success) {\n" +
                "        balances[tx.origin] = balances[tx.origin].sub(tokens);\n" +
                "        balances[to] = balances[to].add(tokens);\n" +
                "        Transfer(tx.origin, to, tokens);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function transferWithFee(address to, uint tokens,  address feeAccount, uint fee) public checkEmergency checkNonOperator onlyCaller(\"FTBTokenHandler\") returns (bool success) {\n" +
                "        require(balances[tx.origin] > tokens + fee);\n" +
                "        balances[tx.origin] = balances[tx.origin].sub(tokens);\n" +
                "        balances[to] = balances[to].add(tokens);\n" +
                "        balances[tx.origin] = balances[tx.origin].sub(fee);\n" +
                "        balances[feeAccount] = balances[feeAccount].add(fee);\n" +
                "        Transfer(tx.origin, to, tokens);\n" +
                "        Transfer(tx.origin, feeAccount, fee);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function approve(address spender, uint tokens) public checkEmergency checkNonOperator onlyCaller(\"FTBTokenHandler\") returns (bool success) {\n" +
                "        allowed[tx.origin][spender] = tokens;\n" +
                "        Approval(tx.origin, spender, tokens);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function transferFrom(address from, address to, uint tokens) public checkEmergency onlyCaller(\"FTBTokenHandler\") returns (bool success) {\n" +
                "        balances[from] = balances[from].sub(tokens);\n" +
                "        allowed[from][tx.origin] = allowed[from][tx.origin].sub(tokens);\n" +
                "        balances[to] = balances[to].add(tokens);\n" +
                "        Transfer(from, to, tokens);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function allowance(address tokenOwner, address spender) public constant returns (uint remaining) {\n" +
                "        return allowed[tokenOwner][spender];\n" +
                "    }\n" +
                "\n" +
                "    // 提取合约内token到指定账户\n" +
                "    function minerTokens(address to, uint tokens) public checkEmergency onlyCaller(\"FTBTokenHandler\") returns (bool success) {\n" +
                "        balances[this] = balances[this].sub(tokens);\n" +
                "        balances[to] = balances[to].add(tokens);\n" +
                "        Transfer(this, to, tokens);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    // 提取运营池内token到指定账户\n" +
                "    function mineOperateTokens(address to, uint tokens) public checkEmergency onlyCaller(\"FTBTokenHandler\") returns (bool success) {\n" +
                "        balances[operator] = balances[operator].sub(tokens);\n" +
                "        balances[to] = balances[to].add(tokens);\n" +
                "        Transfer(operator, to, tokens);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    // 销毁指定账户内token\n" +
                "    function burn(address account)public checkEmergency onlyCaller(\"FTBTokenHandler\") returns (bool success){\n" +
                "        balances[address(0)] += balances[account];\n" +
                "        _totalSupply -= balances[account];\n" +
                "        balances[account] = 0;\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function getEmergency() public constant returns(bool){\n" +
                "        return emergency;\n" +
                "    }\n" +
                "\n" +
                "    function emergencyStop() public onlyCaller(\"FTBTokenHandler\") returns(bool){\n" +
                "        emergency = true;\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function release() public onlyCaller(\"FTBTokenHandler\") returns(bool){\n" +
                "        emergency = false;\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "contract FTBTokenHandler is Controlled {\n" +
                "\n" +
                "    // 校验是否冻结账户\n" +
                "    modifier checkAccountFreeze(address _user){\n" +
                "        require(!Freeze(Controller(controller).getFreeze()).checkFreeze(_user));\n" +
                "        _;\n" +
                "    }\n" +
                "\n" +
                "    // 校验是否提币账户\n" +
                "    modifier checkExtractor(address _user){\n" +
                "        require(AwardPool(Controller(controller).getAwardPool()).checkExtractor(_user));\n" +
                "        _;\n" +
                "    }\n" +
                "\n" +
                "    function totalSupply() public constant returns (uint) {\n" +
                "        return FTBToken(Controller(controller).getFTBToken()).totalSupply();\n" +
                "    }\n" +
                "\n" +
                "    function balanceOf(address tokenOwner) public constant returns (uint balance) {\n" +
                "        return FTBToken(Controller(controller).getFTBToken()).balanceOf(tokenOwner);\n" +
                "    }\n" +
                "\n" +
                "    // 转账校验双方是否冻结账户\n" +
                "    function transfer(address to, uint tokens) public  checkAccountFreeze(tx.origin) checkAccountFreeze(to) returns(bool){\n" +
                "        FTBToken(Controller(controller).getFTBToken()).transfer(to, tokens);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function batchTransfer(address[] tos, uint[] tns) public checkAccountFreeze(tx.origin) returns (bool){\n" +
                "        for (uint i = 0; i < tos.length; i++) {\n" +
                "            require(!Freeze(Controller(controller).getFreeze()).checkFreeze(tos[i]));\n" +
                "            FTBToken(Controller(controller).getFTBToken()).transfer(tos[i], tns[i]);\n" +
                "        }\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function transferWithFee(address to, uint tokens,  address feeAccount, uint fee) public  checkAccountFreeze(tx.origin) checkAccountFreeze(to) returns(bool){\n" +
                "        FTBToken(Controller(controller).getFTBToken()).transferWithFee(to, tokens, feeAccount, fee);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    // 授权校验双方是否冻结账户\n" +
                "    function approve(address spender, uint tokens) public  checkAccountFreeze(tx.origin) checkAccountFreeze(spender) returns(bool){\n" +
                "        FTBToken(Controller(controller).getFTBToken()).approve(spender, tokens);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    // 授权转账校验双方是否冻结账户\n" +
                "    function transferFrom(address from, address to, uint tokens) public  checkAccountFreeze(from) checkAccountFreeze(to) returns(bool){\n" +
                "        FTBToken(Controller(controller).getFTBToken()).transferFrom(from, to, tokens);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function allowance(address tokenOwner, address spender) public constant returns (uint remaining) {\n" +
                "        return FTBToken(Controller(controller).getFTBToken()).allowance(tokenOwner, spender);\n" +
                "    }\n" +
                "\n" +
                "    // 校验必须提币账户挖矿\n" +
                "    function minerTokens(uint time) public  checkExtractor(tx.origin) returns(bool) {\n" +
                "        AwardPool ap = AwardPool(Controller(controller).getAwardPool());\n" +
                "        uint tokens = ap.minerTokens(time);\n" +
                "        address miner = ap.getDistributor();\n" +
                "        FTBToken(Controller(controller).getFTBToken()).minerTokens(miner, tokens);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    // 校验必须提币账户挖矿\n" +
                "    function minerOperatorTokens(uint time) public  checkExtractor(tx.origin) returns(bool) {\n" +
                "        AwardPool ap = AwardPool(Controller(controller).getAwardPool());\n" +
                "        uint tokens = ap.minerOperatorTokens(time);\n" +
                "        address miner = ap.getOperator();\n" +
                "        FTBToken(Controller(controller).getFTBToken()).mineOperateTokens(miner, tokens);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    // 仅管理员销毁代币\n" +
                "    function burn(address account) public onlyAdmin(tx.origin) returns(bool) {\n" +
                "        FTBToken(Controller(controller).getFTBToken()).burn(account);\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function getEmergency() public constant returns(bool){\n" +
                "        return FTBToken(Controller(controller).getFTBToken()).getEmergency();\n" +
                "    }\n" +
                "\n" +
                "    function emergencyStop() public onlyAdmin(tx.origin) returns(bool){\n" +
                "        return FTBToken(Controller(controller).getFTBToken()).emergencyStop();\n" +
                "    }\n" +
                "\n" +
                "    function release() public onlyAdmin(tx.origin) returns(bool){\n" +
                "        return FTBToken(Controller(controller).getFTBToken()).release();\n" +
                "    }\n" +
                "\n" +
                "    // 冻结账户\n" +
                "    function freeze(address account) public onlyAdmin(tx.origin) {\n" +
                "        Freeze(Controller(controller).getFreeze()).freeze(account);\n" +
                "    }\n" +
                "\n" +
                "    // 解冻账户\n" +
                "    function unFreeze(address account) public onlyAdmin(tx.origin) {\n" +
                "        Freeze(Controller(controller).getFreeze()).unFreeze(account);\n" +
                "    }\n" +
                "\n" +
                "    // 校验是否冻结账户\n" +
                "    function checkFreeze(address account) public constant returns(bool){\n" +
                "        return Freeze(Controller(controller).getFreeze()).checkFreeze(account);\n" +
                "    }\n" +
                "\n" +
                "    // 获取挖矿信息\n" +
                "    function getMineInfo() public constant returns(address, address, address, uint){\n" +
                "        return AwardPool(Controller(controller).getAwardPool()).getMineInfo();\n" +
                "    }\n" +
                "\n" +
                "    // 获取挖矿信息\n" +
                "    function getRecord(uint time) public constant returns(uint){\n" +
                "        return AwardPool(Controller(controller).getAwardPool()).getRecord(time);\n" +
                "    }\n" +
                "\n" +
                "    function getOperatorMineInfo() public constant returns(address, address, uint){\n" +
                "        return AwardPool(Controller(controller).getAwardPool()).getOperatorMineInfo();\n" +
                "    }\n" +
                "\n" +
                "    function getOperatorRecord(uint time) public constant returns(uint){\n" +
                "        return AwardPool(Controller(controller).getAwardPool()).getOperatorRecord(time);\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "contract FTBTokenProxy is Controlled, ERC20Interface {\n" +
                "\n" +
                "    function totalSupply() public constant returns (uint) {\n" +
                "        return FTBTokenHandler(Controller(controller).getFTBTokenHandler()).totalSupply();\n" +
                "    }\n" +
                "\n" +
                "    function balanceOf(address tokenOwner) public constant returns (uint balance) {\n" +
                "        return FTBTokenHandler(Controller(controller).getFTBTokenHandler()).balanceOf(tokenOwner);\n" +
                "    }\n" +
                "\n" +
                "    function transfer(address to, uint tokens) public returns(bool){\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).transfer(to, tokens);\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function approve(address spender, uint tokens) public returns(bool){\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).approve(spender, tokens);\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function transferFrom(address from, address to, uint tokens) public returns(bool){\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).transferFrom(from, to, tokens);\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function allowance(address tokenOwner, address spender) public constant returns (uint remaining) {\n" +
                "        return FTBTokenHandler(Controller(controller).getFTBTokenHandler()).allowance(tokenOwner, spender);\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "contract OperatingTokenProxy is Controlled {\n" +
                "\n" +
                "    event mineSuccessEvent(uint amount);\n" +
                "\n" +
                "    function transfer(address to, uint tokens, string traceId, string realtime) public returns(bool){\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).transfer(to, tokens);\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function batchTransfer(address[] tos, uint[] tns, bytes32[] traceIds, bytes32[] realtimes) public returns(bool) {\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).batchTransfer(tos, tns);\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function transferWithFee(address to, uint tokens,  address feeAccount, uint fee, string traceId, string realtime) public returns(bool){\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).transferWithFee(to, tokens, feeAccount, fee);\n" +
                "        successEvent();\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    function balanceOf(address tokenOwner) public constant returns (uint balance) {\n" +
                "        return FTBTokenHandler(Controller(controller).getFTBTokenHandler()).balanceOf(tokenOwner);\n" +
                "    }\n" +
                "\n" +
                "    function minerTokens(uint time) public {\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).minerTokens(time);\n" +
                "        uint _amount = FTBTokenHandler(Controller(controller).getFTBTokenHandler()).getRecord(time);\n" +
                "        mineSuccessEvent(_amount);\n" +
                "    }\n" +
                "\n" +
                "    function minerOperatorTokens(uint time) public {\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).minerOperatorTokens(time);\n" +
                "        uint _amount = FTBTokenHandler(Controller(controller).getFTBTokenHandler()).getOperatorRecord(time);\n" +
                "        mineSuccessEvent(_amount);\n" +
                "    }\n" +
                "\n" +
                "    // 获取挖矿信息\n" +
                "    function getRecord(uint time) public constant returns(uint){\n" +
                "        return FTBTokenHandler(Controller(controller).getFTBTokenHandler()).getRecord(time);\n" +
                "    }\n" +
                "\n" +
                "    // 获取挖矿信息\n" +
                "    function getMineInfo() public constant returns(address, address, address, uint){\n" +
                "        return FTBTokenHandler(Controller(controller).getFTBTokenHandler()).getMineInfo();\n" +
                "    }\n" +
                "\n" +
                "    function getOperatorRecord(uint time) public constant returns(uint){\n" +
                "        return FTBTokenHandler(Controller(controller).getFTBTokenHandler()).getOperatorRecord(time);\n" +
                "    }\n" +
                "\n" +
                "    function getOperatorMineInfo() public constant returns(address, address, uint){\n" +
                "        return FTBTokenHandler(Controller(controller).getFTBTokenHandler()).getOperatorMineInfo();\n" +
                "    }\n" +
                "\n" +
                "    function burn(address account) public {\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).burn(account);\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    function emergencyStop() public {\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).emergencyStop();\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    function release() public {\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).release();\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    function getEmergency() public constant returns(bool){\n" +
                "        return FTBTokenHandler(Controller(controller).getFTBTokenHandler()).getEmergency();\n" +
                "    }\n" +
                "\n" +
                "    // 冻结账户\n" +
                "    function freeze(address account) public {\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).freeze(account);\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    // 解冻账户\n" +
                "    function unFreeze(address account) public {\n" +
                "        FTBTokenHandler(Controller(controller).getFTBTokenHandler()).unFreeze(account);\n" +
                "        successEvent();\n" +
                "    }\n" +
                "\n" +
                "    // 校验是否冻结账户\n" +
                "    function checkFreeze(address account) public constant returns(bool){\n" +
                "        return FTBTokenHandler(Controller(controller).getFTBTokenHandler()).checkFreeze(account);\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "contract FTBControllerFactory{\n" +
                "\n" +
                "    address public controller;\n" +
                "    event createSuccessEvent(address addr);\n" +
                "\n" +
                "    function getController() public returns(address){\n" +
                "        return controller;\n" +
                "    }\n" +
                "\n" +
                "    function createController() public returns(address){\n" +
                "        Controller ct = new Controller();\n" +
                "        controller = ct;\n" +
                "        createSuccessEvent(ct);\n" +
                "        return controller;\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "contract FTBProxyFactory {\n" +
                "\n" +
                "    address public controller;\n" +
                "    event createSuccessEvent(address addr);\n" +
                "\n" +
                "    function FTBProxyFactory(address _controller){\n" +
                "        controller = _controller;\n" +
                "    }\n" +
                "\n" +
                "    function createC2CProxy() public returns(address){\n" +
                "        FTBTokenProxy proxy = new FTBTokenProxy();\n" +
                "        Controller(controller).register(\"FTBTokenProxy\", proxy);\n" +
                "        proxy.setController(controller);\n" +
                "        createSuccessEvent(proxy);\n" +
                "        return proxy;\n" +
                "    }\n" +
                "\n" +
                "    function createB2CProxy() public returns(address){\n" +
                "        OperatingTokenProxy proxy = new OperatingTokenProxy();\n" +
                "        Controller(controller).register(\"OperatingTokenProxy\", proxy);\n" +
                "        proxy.setController(controller);\n" +
                "        createSuccessEvent(proxy);\n" +
                "        return proxy;\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "contract FTBHandlerFactory{\n" +
                "\n" +
                "    address public controller;\n" +
                "    event createSuccessEvent(address addr);\n" +
                "\n" +
                "    function FTBHandlerFactory(address _controller){\n" +
                "        controller = _controller;\n" +
                "    }\n" +
                "\n" +
                "    function createHandler() public returns(address){\n" +
                "        FTBTokenHandler handler = new FTBTokenHandler();\n" +
                "        Controller(controller).register(\"FTBTokenHandler\", handler);\n" +
                "        handler.setController(controller);\n" +
                "        createSuccessEvent(handler);\n" +
                "        return handler;\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "contract FTBTokenFactory{\n" +
                "\n" +
                "    address public controller;\n" +
                "    event createSuccessEvent(address addr);\n" +
                "\n" +
                "    function FTBTokenFactory(address _controller){\n" +
                "        controller = _controller;\n" +
                "    }\n" +
                "\n" +
                "    function createToken() public returns(address) {\n" +
                "        FTBToken token = new FTBToken();\n" +
                "        token.setController(controller);\n" +
                "        Controller(controller).register(\"FTBToken\", token);\n" +
                "        createSuccessEvent(token);\n" +
                "        return token;\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "contract FTBAwardPoolFactory {\n" +
                "\n" +
                "    address public controller;\n" +
                "    event createSuccessEvent(address addr);\n" +
                "\n" +
                "    function FTBAwardPoolFactory(address _controller){\n" +
                "        controller = _controller;\n" +
                "    }\n" +
                "\n" +
                "    function createMine() public returns(address){\n" +
                "        AwardPool ap = new AwardPool();\n" +
                "        Controller(controller).register(\"AwardPool\", ap);\n" +
                "        ap.setController(controller);\n" +
                "        createSuccessEvent(ap);\n" +
                "        return ap;\n" +
                "    }\n" +
                "\n" +
                "    function createFreeze() public returns(address){\n" +
                "        Freeze f = new Freeze();\n" +
                "        f.setController(controller);\n" +
                "        Controller(controller).register(\"Freeze\", f);\n" +
                "        createSuccessEvent(f);\n" +
                "        return f;\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "\n" +
                "\n";


        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        //logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);
        return cres;
    }
}
