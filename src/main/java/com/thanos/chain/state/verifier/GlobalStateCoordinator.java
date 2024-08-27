package com.thanos.chain.state.verifier;

import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.BlockSign;
import com.thanos.chain.ledger.model.crypto.Signature;
import com.thanos.chain.network.protocols.MessageDuplexDispatcher;
import com.thanos.chain.network.protocols.base.Message;
import com.thanos.chain.state.verifier.model.BlockCheckTimeoutMsg;
import com.thanos.chain.state.verifier.model.BlockSignResponseMsg;
import com.thanos.chain.state.verifier.model.CommitBlockMsg;
import com.thanos.chain.state.verifier.model.GetBlockSignRequestMsg;
import com.thanos.chain.state.verifier.model.GlobalStateVerifierCommand;
import com.thanos.chain.state.verifier.model.GlobalStateVerifierMsg;
import com.thanos.chain.state.verifier.model.LocalBlockSignMsg;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.ThanosThreadFactory;
import com.thanos.common.utils.ThanosWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * GlobalStateCoordinator.java description：
 *
 * @Author laiyiyu create on 2020-09-25 14:41:50
 */
public class GlobalStateCoordinator {

    static final Logger logger = LoggerFactory.getLogger("state-verify");

    static final boolean TRACE_ABLE = logger.isTraceEnabled();

    static final int MAX_FUTURE_BLOCK_SIGN_RESP_SIZE = 16;

    static ScheduledThreadPoolExecutor timeoutScheduled = new ScheduledThreadPoolExecutor(1, new ThanosThreadFactory("async_consensus_schedule_processor"));

    int checkTimeoutMs;

    int maxCommitBlockCache;

    GlobalStateVerifier globalStateVerifier;

    ArrayBlockingQueue<GlobalStateVerifierMsg> globalStateVerifierMsgQueue;

    Map<Long, List<BlockSignResponseMsg>> pendingBlockSignResponse;

    Map<Long, Block> commitBlocks;


    public GlobalStateCoordinator(GlobalStateVerifier globalStateVerifier) {
        this.globalStateVerifier = globalStateVerifier;
        this.globalStateVerifierMsgQueue = new ArrayBlockingQueue(10000);
        this.pendingBlockSignResponse = new HashMap<>(1024);
        this.checkTimeoutMs = globalStateVerifier.stateLedger.systemConfig.getCheckTimeoutMS();
        this.maxCommitBlockCache = globalStateVerifier.stateLedger.systemConfig.getMaxCommitBlockInMemory();
        this.commitBlocks = new HashMap<>();
    }

    public void start() {
        new ThanosWorker("global_state_coordinator_net_thread") {
            @Override
            protected void doWork() throws Exception {
                Message msg = MessageDuplexDispatcher.getGlobalStateVerifierMsg();

                GlobalStateVerifierMsg globalStateVerifierMsg = null;
                switch (GlobalStateVerifierCommand.fromByte(msg.getCode())) {
                    case GET_BLOCK_SIGN_REQ:
                        globalStateVerifierMsg = new GetBlockSignRequestMsg(msg.getEncoded());
                        break;
                    case BLOCK_SIGN_RESP:
                        globalStateVerifierMsg = new BlockSignResponseMsg(msg.getEncoded());
                        break;
                    default:
                        throw new RuntimeException("un except msg type!");
                }

                globalStateVerifierMsg.setRemoteType(msg.getRemoteType());
                globalStateVerifierMsg.setRpcId(msg.getRpcId());
                globalStateVerifierMsg.setNodeId(msg.getNodeId());
                globalStateVerifierMsgQueue.put(globalStateVerifierMsg);

            }
        }.start();

        new ThanosWorker("global_state_coordinator_process_thread") {
            @Override
            protected void doWork() throws Exception {
                GlobalStateVerifierMsg globalStateVerifierMsg = globalStateVerifierMsgQueue.take();
                switch (globalStateVerifierMsg.getCommand()) {
                    case LOCAL_BLOCK_SIGN:
                        processLocalBlockSign((LocalBlockSignMsg) globalStateVerifierMsg);
                        break;
                    case GET_BLOCK_SIGN_REQ:
                        processGetBlockSignReq((GetBlockSignRequestMsg) globalStateVerifierMsg);
                        break;
                    case BLOCK_SIGN_RESP:
                        processBlockSignResp((BlockSignResponseMsg) globalStateVerifierMsg);
                        break;
                    case BLOCK_SIGN_TIMEOUT:
                        processBlockSignTimeout((BlockCheckTimeoutMsg) globalStateVerifierMsg);
                        break;
                    case COMMIT_BLOCK:
                        processCommitBlock((CommitBlockMsg) globalStateVerifierMsg);
                        break;
                    default:
                        throw new RuntimeException("un except msg type!");
                }
                globalStateVerifierMsg.releaseReference();
            }
        }.start();
    }

    private void processLocalBlockSign(LocalBlockSignMsg localBlockSignMsg) {

        if (TRACE_ABLE) {
            logger.trace("processLocalBlockSign:{}", localBlockSignMsg);
        }


        ByteArrayWrapper publicKeyWrapper = new ByteArrayWrapper(localBlockSignMsg.publicKey);

        globalStateVerifier.currentBlockCheckContext.reset(globalStateVerifier.stateLedger, localBlockSignMsg.hash, localBlockSignMsg.epoch, localBlockSignMsg.number);
        globalStateVerifier.currentBlockCheckContext.checkSign(publicKeyWrapper, localBlockSignMsg.hash, localBlockSignMsg.signature);
        Map<ByteArrayWrapper, Signature> signatures = new HashMap<>();
        signatures.put(publicKeyWrapper, localBlockSignMsg.signature);
        globalStateVerifier.netInvoker.broadcast(new BlockSignResponseMsg(localBlockSignMsg.number, localBlockSignMsg.hash, signatures));
        if (checkFinish()) {
            return;
        }

        List<BlockSignResponseMsg> getBlockSignRequestMsgs = pendingBlockSignResponse.get(localBlockSignMsg.number);
        if (getBlockSignRequestMsgs != null) {
            for (BlockSignResponseMsg blockSignResponseMsg : getBlockSignRequestMsgs) {
                for (Map.Entry<ByteArrayWrapper, Signature> entry : blockSignResponseMsg.getSignatures().entrySet()) {
                    globalStateVerifier.currentBlockCheckContext.checkSign(entry.getKey(), blockSignResponseMsg.getHash(), entry.getValue());
                    if (checkFinish()) {
                        return;
                    }
                }
            }
        }

        timeoutScheduled.schedule(() -> {
            try {
                globalStateVerifierMsgQueue.put(new BlockCheckTimeoutMsg(localBlockSignMsg.number));
            } catch (InterruptedException e) {

            }
        }, checkTimeoutMs, TimeUnit.MILLISECONDS);
    }

    private void processGetBlockSignReq(GetBlockSignRequestMsg getBlockSignRequestMsg) {
        if (TRACE_ABLE) {
            logger.trace("processGetBlockSignReq:{}", getBlockSignRequestMsg);
        }
        if (getBlockSignRequestMsg.getNumber() > globalStateVerifier.stateLedger.getLatestBeExecutedNum() + 1) {
            return;
        }

        byte[] hash = new byte[0];
        Map<ByteArrayWrapper, Signature> signatures = new HashMap<>();

        if (getBlockSignRequestMsg.getNumber() <= globalStateVerifier.stateLedger.getLatestBeExecutedNum()) {
            Block commitBlock = commitBlocks.get(getBlockSignRequestMsg.getNumber());
            BlockSign blockSign = null;

            if (commitBlock != null && commitBlock.getBlockSign() != null) {
                blockSign = commitBlock.getBlockSign();
            } else {
                blockSign = globalStateVerifier.stateLedger.ledgerSource.getBlockSignByNumber(getBlockSignRequestMsg.getNumber());
            }

            signatures.putAll(blockSign.getSignatures());
            hash = ByteUtil.copyFrom(blockSign.getHash());

        } else if (globalStateVerifier.currentBlockCheckContext.getCurrentCheckNumber() == getBlockSignRequestMsg.getNumber()) {
            signatures.putAll(globalStateVerifier.currentBlockCheckContext.getSameHashSigns());
            hash = ByteUtil.copyFrom(globalStateVerifier.currentBlockCheckContext.getCurrentCheckHash());
        }

        BlockSignResponseMsg responseMsg = new BlockSignResponseMsg(getBlockSignRequestMsg.getNumber(), hash, signatures);
        globalStateVerifier.netInvoker.directSend(responseMsg, Arrays.asList(getBlockSignRequestMsg.getNodeId()));
    }

    private void processBlockSignResp(BlockSignResponseMsg blockSignResponseMsg) {
        //logger.debug("processBlockSignResp:{}", blockSignResponseMsg);

        if (TRACE_ABLE) {
            logger.trace("processBlockSignResp:{}", blockSignResponseMsg);
        }

        long latestBeExecutedNum = globalStateVerifier.stateLedger.ledgerSource.getLatestBeExecutedNum();

        if (blockSignResponseMsg.getNumber() <= latestBeExecutedNum
                ||
                (blockSignResponseMsg.getNumber() - latestBeExecutedNum) > MAX_FUTURE_BLOCK_SIGN_RESP_SIZE) {
            logger.debug("ignore process processBlockSignResp!");
            return;
        }

        if (globalStateVerifier.currentBlockCheckContext.getCurrentCheckNumber() == blockSignResponseMsg.getNumber()) {
            for (Map.Entry<ByteArrayWrapper, Signature> entry : blockSignResponseMsg.getSignatures().entrySet()) {
                globalStateVerifier.currentBlockCheckContext.checkSign(entry.getKey(), blockSignResponseMsg.getHash(), entry.getValue());
                if (checkFinish()) {
                    return;
                }
            }
        } else {
            List<BlockSignResponseMsg> getBlockSignRequestMsgs = pendingBlockSignResponse.get(blockSignResponseMsg.getNumber());
            if (getBlockSignRequestMsgs == null) {
                getBlockSignRequestMsgs = new ArrayList<>(16);
                pendingBlockSignResponse.put(blockSignResponseMsg.getNumber(), getBlockSignRequestMsgs);
            }
            getBlockSignRequestMsgs.add(blockSignResponseMsg);
        }
    }

    private void processBlockSignTimeout(BlockCheckTimeoutMsg blockCheckTimeoutMsg) {
        //logger.debug("processBlockSignTimeout:{}", blockCheckTimeoutMsg.number);

        if (blockCheckTimeoutMsg.number != globalStateVerifier.currentBlockCheckContext.getCurrentCheckNumber() || globalStateVerifier.currentBlockCheckContext.isFinish()) {
            //logger.debug("processBlockSignTimeout over!, context number:[{}] , request number:[{}]", globalStateVerifier.currentBlockCheckContext.getCurrentCheckNumber(), blockCheckTimeoutMsg.number);
            return;
        }

        logger.warn("doCheck BlockSign Timeout, number:[{}]", blockCheckTimeoutMsg.number);

        timeoutScheduled.schedule(() -> {
            try {
                globalStateVerifierMsgQueue.put(new BlockCheckTimeoutMsg(blockCheckTimeoutMsg.number));
            } catch (InterruptedException e) {

            }
        }, checkTimeoutMs, TimeUnit.MILLISECONDS);

        globalStateVerifier.netInvoker.broadcast(new GetBlockSignRequestMsg(blockCheckTimeoutMsg.number));
    }

    private void processCommitBlock(CommitBlockMsg commitBlockMsg) {
        commitBlocks.put(commitBlockMsg.block.getNumber(), commitBlockMsg.block);
        commitBlocks.remove(commitBlockMsg.block.getNumber() - maxCommitBlockCache);
    }

    private boolean checkFinish() {
        boolean finish = globalStateVerifier.currentBlockCheckContext.isFinish();
        long currentCheckNumber = globalStateVerifier.currentBlockCheckContext.getCurrentCheckNumber();

        if (finish) {
            List<BlockSignResponseMsg> blockSignResponseMsgs = pendingBlockSignResponse.remove(currentCheckNumber);
            if (blockSignResponseMsgs != null) {
                for (BlockSignResponseMsg blockSignResponseMsg : blockSignResponseMsgs) {
                    blockSignResponseMsg.doRelease();
                }
            }
        }
        return finish;
    }
}
