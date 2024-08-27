package com.thanos.vm;

import com.thanos.chain.contract.eth.solidity.compiler.CompilationResult;
import com.thanos.chain.contract.eth.solidity.compiler.SolidityCompiler;
import org.junit.Test;

import java.io.IOException;

/**
 * TestCompiler.java description：
 *
 * @Author laiyiyu create on 2021-01-15 10:25:37
 */
public class TestCompiler {



    @Test
    public void test() {

        CompilationResult cres = null;
        try {
            cres = demeTest();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private CompilationResult demeTest() throws IOException {

        String contract = "pragma solidity ^0.4.25;\n" +
                "\n" +
                "contract SimpleAuction {\n" +
                "    // 拍卖的参数。\n" +
                "    address public beneficiary;\n" +
                "    // 时间是unix的绝对时间戳（自1970-01-01以来的秒数）\n" +
                "    // 或以秒为单位的时间段。\n" +
                "    uint public auctionEnd;\n" +
                "\n" +
                "    // 拍卖的当前状态\n" +
                "    address public highestBidder;\n" +
                "    uint public highestBid;\n" +
                "\n" +
                "    //可以取回的之前的出价\n" +
                "    mapping(address => uint) pendingReturns;\n" +
                "\n" +
                "    // 拍卖结束后设为 true，将禁止所有的变更\n" +
                "    bool ended;\n" +
                "\n" +
                "    // 变更触发的事件\n" +
                "    event HighestBidIncreased(address bidder, uint amount);\n" +
                "    event AuctionEnded(address winner, uint amount);\n" +
                "\n" +
                "    // 以下是所谓的 natspec 注释，可以通过三个斜杠来识别。\n" +
                "    // 当用户被要求确认交易时将显示。\n" +
                "\n" +
                "    /// 以受益者地址 `_beneficiary` 的名义，\n" +
                "    /// 创建一个简单的拍卖，拍卖时间为 `_biddingTime` 秒。\n" +
                "    constructor (\n" +
                "        uint _biddingTime,\n" +
                "        address _beneficiary\n" +
                "    ) public {\n" +
                "        beneficiary = _beneficiary;\n" +
                "        auctionEnd = now + _biddingTime;\n" +
                "    }\n" +
                "\n" +
                "    /// 对拍卖进行出价，具体的出价随交易一起发送。\n" +
                "    /// 如果没有在拍卖中胜出，则返还出价。\n" +
                "    function bid() public payable {\n" +
                "        // 参数不是必要的。因为所有的信息已经包含在了交易中。\n" +
                "        // 对于能接收以太币的函数，关键字 payable 是必须的。\n" +
                "\n" +
                "        // 如果拍卖已结束，撤销函数的调用。\n" +
                "        require(\n" +
                "            now <= auctionEnd,\n" +
                "            \"Auction already ended.\"\n" +
                "        );\n" +
                "\n" +
                "        // 如果出价不够高，返还你的钱\n" +
                "        require(\n" +
                "            msg.value > highestBid,\n" +
                "            \"There already is a higher bid.\"\n" +
                "        );\n" +
                "\n" +
                "        if (highestBid != 0) {\n" +
                "            // 返还出价时，简单地直接调用 highestBidder.send(highestBid) 函数，\n" +
                "            // 是有安全风险的，因为它有可能执行一个非信任合约。\n" +
                "            // 更为安全的做法是让接收方自己提取金钱。\n" +
                "            pendingReturns[highestBidder] += highestBid;\n" +
                "        }\n" +
                "        highestBidder = msg.sender;\n" +
                "        highestBid = msg.value;\n" +
                "        emit HighestBidIncreased(msg.sender, msg.value);\n" +
                "    }\n" +
                "\n" +
                "    /// 取回出价（当该出价已被超越）\n" +
                "    function withdraw() public returns (bool) {\n" +
                "        uint amount = pendingReturns[msg.sender];\n" +
                "        if (amount > 0) {\n" +
                "            // 这里很重要，首先要设零值。\n" +
                "            // 因为，作为接收调用的一部分，\n" +
                "            // 接收者可以在 `send` 返回之前，重新调用该函数。\n" +
                "            pendingReturns[msg.sender] = 0;\n" +
                "\n" +
                "            if (!msg.sender.send(amount)) {\n" +
                "                // 这里不需抛出异常，只需重置未付款\n" +
                "                pendingReturns[msg.sender] = amount;\n" +
                "                return false;\n" +
                "            }\n" +
                "        }\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    /// 结束拍卖，并把最高的出价发送给受益人\n" +
                "    function auctionEnd() public {\n" +
                "        // 对于可与其他合约交互的函数（意味着它会调用其他函数或发送以太币），\n" +
                "        // 一个好的指导方针是将其结构分为三个阶段：\n" +
                "        // 1. 检查条件\n" +
                "        // 2. 执行动作 (可能会改变条件)\n" +
                "        // 3. 与其他合约交互\n" +
                "        // 如果这些阶段相混合，其他的合约可能会回调当前合约并修改状态，\n" +
                "        // 或者导致某些效果（比如支付以太币）多次生效。\n" +
                "        // 如果合约内调用的函数包含了与外部合约的交互，\n" +
                "        // 则它也会被认为是与外部合约有交互的。\n" +
                "\n" +
                "        // 1. 条件\n" +
                "        require(now >= auctionEnd, \"Auction not yet ended.\");\n" +
                "        require(!ended, \"auctionEnd has already been called.\");\n" +
                "\n" +
                "        // 2. 生效\n" +
                "        ended = true;\n" +
                "        emit AuctionEnded(highestBidder, highestBid);\n" +
                "\n" +
                "        // 3. 交互\n" +
                "        beneficiary.transfer(highestBid);\n" +
                "    }\n" +
                "}";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        //logger.info(res.errors);
        CompilationResult cres = CompilationResult.parse(res.output);
        return cres;
    }


}
