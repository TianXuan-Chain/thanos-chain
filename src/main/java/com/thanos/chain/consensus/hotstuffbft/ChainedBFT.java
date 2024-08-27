package com.thanos.chain.consensus.hotstuffbft;

import com.thanos.common.utils.ThanosThreadFactory;
import com.thanos.common.utils.ThanosWorker;
import com.thanos.chain.consensus.hotstuffbft.model.*;
import com.thanos.chain.consensus.hotstuffbft.model.chainConfig.OnChainConfigPayload;
import com.thanos.chain.network.protocols.MessageDuplexDispatcher;
import com.thanos.chain.network.protocols.base.Message;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 类ChainedBFT.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-11 15:40:42
 */
public class ChainedBFT {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    private static BlockingQueue<ConsensusMsg> consensusMsgQueue = new ArrayBlockingQueue<>(10000);

    private static BlockingQueue<OnChainConfigPayload> configPayloadQueue = new ArrayBlockingQueue<>(100);

    EpochManager epochManager;

    ThreadPoolExecutor decodeExecutor = new ThreadPoolExecutor(2, Runtime.getRuntime().availableProcessors(), 3, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000), new ThanosThreadFactory("chained_bft_msg_decode"));

    public ChainedBFT(EpochManager epochManager) {
        this.epochManager = epochManager;
    }

    public void start() {
        epochManager.startProcess(OnChainConfigPayload.build(this.epochManager.consensusEventExecutor.getLatestLedgerInfo().getCurrentEpochState()));

        new ThanosWorker("decode_consensus_start_thread") {
            @Override
            protected void doWork() throws Exception {
                Message msg = MessageDuplexDispatcher.getConsensusMsg();
                if (epochManager.isSyncing()) return;

                decodeExecutor.execute(() -> {
                    try {
                        ConsensusMsg consensusMsg = null;
                        switch (ConsensusCommand.fromByte(msg.getCode())) {
                            case PROPOSAL:
                                consensusMsg = new ProposalMsg(msg.getEncoded());
                                break;
                            case VOTE:
                                consensusMsg = new VoteMsg(msg.getEncoded());
                                break;
                            case HOTSTUFF_CHAIN_SYNC:
                                consensusMsg = new HotstuffChainSyncInfo(msg.getEncoded());
                                break;
                            case EVENT_RETRIEVAL_REQ:
                                consensusMsg = new EventRetrievalRequestMsg(msg.getEncoded());
                                break;
                            case LATEST_LEDGER_REQ:
                                consensusMsg = new LatestLedgerInfoRequestMsg();
                                break;
                            case LATEST_LEDGER_RESP:
                                consensusMsg = new LatestLedgerInfoResponseMsg(msg.getEncoded());
                                break;
                            default:
                                throw new RuntimeException("un except msg type!");
                                //break;
                        }

                        consensusMsg.setRemoteType(msg.getRemoteType());
                        consensusMsg.setRpcId(msg.getRpcId());
                        consensusMsg.setNodeId(msg.getNodeId());
                        //logger.debug("consensus msg:{}", consensusMsg);

                        consensusMsgQueue.put(consensusMsg);
                    } catch (Throwable e) {
                        logger.error("decode error, {}", ExceptionUtils.getStackTrace(e));
                    } finally {
                    }
                });
            }
        }.start();

        new ThanosWorker("process_consensus_msg_thread") {
            @Override
            protected void doWork() throws Exception {
                {
                    // don't wait
                    OnChainConfigPayload onChainConfigPayload = configPayloadQueue.poll();
                    if (onChainConfigPayload != null) {
                        epochManager.startProcess(onChainConfigPayload);
                    }
                    onChainConfigPayload = null;

                }
                //logger.debug("poll msg hehe!");
                {
                    // avoid cpu 100%
                    ConsensusMsg consensusMsg = consensusMsgQueue.poll(20, TimeUnit.MILLISECONDS);
                    if (consensusMsg != null) {
                        epochManager.processMessage(consensusMsg);
                    }
                    consensusMsg = null;
                }
            }
        }.start();
    }

    public static void putConsensusMsg(ConsensusMsg msg) {
        try {
            consensusMsgQueue.put(msg);
        } catch (Exception e) {
        }
    }

    public static void publishConfigPayload(OnChainConfigPayload onChainConfigPayload) {
        try {
            configPayloadQueue.put(onChainConfigPayload);
        } catch (Exception e) {
        }
    }
}
