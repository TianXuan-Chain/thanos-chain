package com.thanos.chain.gateway;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.common.utils.ThanosWorker;
import com.thanos.chain.ledger.model.Block;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * BlockPublisher.java description：
 *
 * @Author laiyiyu create on 2020-10-15 16:20:22
 */
public class BlockPublisher {

    private static final Logger logger = LoggerFactory.getLogger("gateway");

    private static BlockingQueue<Block> blockQueue = null;

    private SyncClient syncClient;

    static {
        blockQueue = new ArrayBlockingQueue<>(32);
    }

    public static void commitBlock(Block block) {
        try {
            blockQueue.put(block);
        } catch (InterruptedException e) {

        }
    }

    public BlockPublisher(SystemConfig systemConfig) {
        //构建服务
        String remoteAddress = systemConfig.getGatewayRemoteServiceAddress();
        if (StringUtils.isBlank(remoteAddress)) {
            throw new RuntimeException("Start BlockPublisher fail, remoteAddress is blank.");
        } else {
            String[] strs = remoteAddress.split(":");
            if (strs.length != 2) {
                throw new RuntimeException("Start BlockPublisher fail, remoteAddress is error.");
            }
            syncClient = new SyncClient(strs[0], Integer.valueOf(strs[1]));
        }
        //启动推送异步线程
        start();
    }

    public void start() {
        new ThanosWorker("commit_block_notify_thread") {
            @Override
            protected void doWork() throws Exception {
                Block block = null;
                try {
                    block = blockQueue.take();
//                    logger.warn("push commit block, {}", block);

                    if (logger.isTraceEnabled()) {

                        if (block.getReceipts() != null) {
                            for (EthTransactionReceipt receipt: block.getReceipts()) {
                                logger.trace("will push commit block[{}], tx:[{}]", block.getNumber(), Hex.toHexString(receipt.getEthTransaction().getHash()));
                            }
                        }


                        if (block.getGlobalEvent().getGlobalNodeEvents() != null) {
                            for (GlobalNodeEvent globalNodeEvent: block.getGlobalEvent().getGlobalNodeEvents()) {
                                logger.trace("will push commit block[{}], gne:[{}]", block.getNumber(), Hex.toHexString(globalNodeEvent.getHash()));
                            }
                        }
                    }

//                    return;
                    //todo ::remove
                    syncClient.syncBlock(block);
                } catch (Exception e) {
                    logger.error("BlockPublisher commit_block_notify_thread error, blockID:{}, error:{}", Hex.toHexString(block.getEventId()), e.getMessage());
                }
            }

        }.start();
    }

}
