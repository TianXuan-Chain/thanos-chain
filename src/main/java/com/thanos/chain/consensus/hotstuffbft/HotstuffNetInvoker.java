package com.thanos.chain.consensus.hotstuffbft;

import com.thanos.chain.consensus.hotstuffbft.executor.ConsensusEventExecutor;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.chain.consensus.hotstuffbft.model.ConsensusMsg;
import com.thanos.chain.consensus.hotstuffbft.model.ValidatorVerifier;
import com.thanos.chain.ledger.model.ValidatorPublicKeyInfo;
import com.thanos.chain.network.NetInvoker;
import com.thanos.chain.network.peer.CaNode;
import com.thanos.chain.network.protocols.base.Message;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

/**
 * HotstuffNetInvoker.java description：
 *
 * @Author laiyiyu create on 2020-07-10 11:06:15
 */
public class HotstuffNetInvoker {

    private static final Logger logger = LoggerFactory.getLogger("consensus");


    NetInvoker netInvoker;

    public HotstuffNetInvoker(NetInvoker netInvoker) {
        this.netInvoker = netInvoker;
    }

    public void directSend(ConsensusMsg message, byte[] nodeId) {
        if (Arrays.equals(nodeId, netInvoker.getSelfNodeId())) {
            message.setNodeId(ByteUtil.copyFrom(netInvoker.getSelfNodeId()));
            ChainedBFT.putConsensusMsg(message);
        } else {
            this.netInvoker.directSend(message, Arrays.asList(nodeId));
        }
    }

    // default timeout 3 sec
    public Message rpcSend(ConsensusMsg message) {
        return this.netInvoker.rpcSend(message);
    }

    public Message rpcSend(ConsensusMsg message, long timeout) {
        return this.netInvoker.rpcSend(message, timeout);
    }

    public void broadcast(ConsensusMsg message, boolean includeSelf) {
        if (includeSelf) {
            //message.setNodeId(ByteUtil.copyFrom(netInvoker.peerManager.selfNodeId));
            ChainedBFT.putConsensusMsg(message); // self
        }

        this.netInvoker.broadcast(message);
    }

    public void updateEligibleNodes(EpochState epochState, ConsensusEventExecutor consensusEventExecutor) {
        ValidatorVerifier validators = epochState.getValidatorVerifier();
        Map<ByteArrayWrapper, CaNode> caNodes = new HashMap<>(validators.getPk2ValidatorInfo().size());
        Map<ByteArrayWrapper, CaNode> disconnectedNodes = new HashMap<>();
        List<ValidatorPublicKeyInfo> blackList = epochState.getGlobalEventState().getNodeBlackList();
        Set<BigInteger> blackListSet = new HashSet<>(blackList.size());
        blackList.stream().forEach(validatorPublicKeyInfo -> {

            blackListSet.add(new BigInteger(validatorPublicKeyInfo.getCaHash(), 16));
            logger.info("updateEligibleNodes cahash:{}", validatorPublicKeyInfo.getCaHash());
            CaNode caNode = new CaNode(
                    ByteUtil.copyFrom(validatorPublicKeyInfo.getAccountAddress()),
                    new String(validatorPublicKeyInfo.getName().getBytes()),
                    new String(validatorPublicKeyInfo.getAgency().getBytes()),
                    new String(validatorPublicKeyInfo.getCaHash().getBytes()),
                    validatorPublicKeyInfo.getShardingNum()
            );

            disconnectedNodes.put(new ByteArrayWrapper(caNode.nodeId), caNode);
        });




        for (Map.Entry<ByteArrayWrapper, ValidatorPublicKeyInfo> entry: validators.getPk2ValidatorInfo().entrySet()) {
            CaNode caNode = new CaNode(
                    ByteUtil.copyFrom(entry.getKey().getData()),
                    new String(entry.getValue().getName().getBytes()),
                    new String(entry.getValue().getAgency().getBytes()),
                    new String(entry.getValue().getCaHash().getBytes()),
                    entry.getValue().getShardingNum()
            );
            caNodes.put(new ByteArrayWrapper(caNode.nodeId), caNode);
        }


        Function function = null;
        if (!CollectionUtils.isEmpty(blackList)) {
            function = o -> {
                while (!consensusEventExecutor.isStateConsistent()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                }

                return null;
            };
        }

        this.netInvoker.updateEligibleNodes(caNodes, blackListSet, disconnectedNodes, function);
    }
}
