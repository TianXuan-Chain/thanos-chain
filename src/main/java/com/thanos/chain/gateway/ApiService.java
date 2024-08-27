package com.thanos.chain.gateway;

import com.google.protobuf.ByteString;
import com.thanos.api.proto.push.*;
import com.thanos.chain.consensus.hotstuffbft.liveness.EpochState;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.chain.ledger.model.event.GlobalNodeEventReceipt;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.ThanosThreadFactory;
import com.thanos.common.utils.ThanosWorker;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.chain.txpool.EventCollector;
import io.grpc.stub.StreamObserver;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ApiService.java description：
 *
 * @Author laiyiyu create on 2020-10-14 17:21:22
 */
public class ApiService extends PushServiceGrpc.PushServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger("gateway");

    private GatewayFacade gatewayFacade;

    //ArrayBlockingQueue<EthTransactionsPushDTO> txsPackageQueue;

    private ThreadPoolExecutor decodeExecutor;

    public ApiService(GatewayFacade gatewayFacade) {
        this.gatewayFacade = gatewayFacade;
        //this.txsPackageQueue = new ArrayBlockingQueue(gatewayFacade.systemConfig.getPushTxsQueueSize());
        //this.decodeExecutor = new ThreadPoolExecutor(gatewayFacade.systemConfig.getDecodeGatewayMsgProcessNum(), gatewayFacade.systemConfig.getDecodeGatewayMsgProcessNum(), 2L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(5), new ThanosThreadFactory("gateway_msg_decoder"));
        //asyncImportTx();
    }

    @Override
    public void pushEthTransactions(EthTransactionsPushDTO request, StreamObserver<DefaultResponse> responseObserver) {
        try {
            logger.info("ApiService pushEthTransactions ====================. num size:{}", request.getTxsList().size());
            if (CollectionUtils.isEmpty(request.getTxsList())) {
                return;
            }
            long s1 = System.currentTimeMillis();
            //处理交易
            EthTransaction[] list = new EthTransaction[request.getTxsList().size()];
            EthTransaction tx = null;
            for (int i = 0; i < request.getTxsList().size(); i++) {
                EthTransactionsPushDTO.EthTransactionPushDTO tv = request.getTxsList().get(i);
                Set<ByteArrayWrapper> byteArrayWrapperSet = new HashSet<>();
                if (CollectionUtils.isNotEmpty(tv.getExecuteStatesList())) {
                    for (ByteString bs : tv.getExecuteStatesList()) {
                        ByteArrayWrapper bw = new ByteArrayWrapper(bs.toByteArray());
                        byteArrayWrapperSet.add(bw);
                    }
                }
                tx = new EthTransaction(tv.getPublicKey().toByteArray(),
                        tv.getNonce().toByteArray(),
                        tv.getFutureEventNumber(),
                        tv.getGasPrice().toByteArray(),
                        tv.getGasLimit().toByteArray(),
                        tv.getReceiveAddress().toByteArray(),
                        tv.getValue().toByteArray(),
                        tv.getData().toByteArray(),
                        byteArrayWrapperSet,
                        tv.getSignature().toByteArray(),
                        tv.getHash().toByteArray(), tv.getRlpEncoded().toByteArray());

                if (logger.isDebugEnabled()) {
                    logger.debug("receive tx:{}!", Hex.toHexString(tx.getHash()));
                }

                list[i] = tx;
            }

            this.gatewayFacade.txnManager.eventCollector.importPayload(list, new ArrayList<>());
            long s2 = System.currentTimeMillis();
            logger.info("ApiService asyncImportTx. size: {},  time:{}", list.length, (s2 - s1));

            //txsPackageQueue.put(request);
        } catch (Exception e) {
            logger.info("ApiService pushEthTransactions. size: {}", request.getTxsList().size());
        }
        DefaultResponse response = DefaultResponse.newBuilder().setResult(true).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

//    public void asyncImportTx() {
//        new ThanosWorker("txs_package_thread") {
//            @Override
//            protected void doWork() throws Exception {
//
//                EthTransactionsPushDTO request = txsPackageQueue.take();
//                if (CollectionUtils.isEmpty(request.getTxsList())) {
//                    return;
//                }
//                long s1 = System.currentTimeMillis();
//                //处理交易
//                EthTransaction[] list = new EthTransaction[request.getTxsList().size()];
//                EthTransaction tx = null;
//                for (int i = 0; i < request.getTxsList().size(); i++) {
//                    EthTransactionsPushDTO.EthTransactionPushDTO tv = request.getTxsList().get(i);
//                    Set<ByteArrayWrapper> byteArrayWrapperSet = new HashSet<>();
//                    if (CollectionUtils.isNotEmpty(tv.getExecuteStatesList())) {
//                        for (ByteString bs : tv.getExecuteStatesList()) {
//                            ByteArrayWrapper bw = new ByteArrayWrapper(bs.toByteArray());
//                            byteArrayWrapperSet.add(bw);
//                        }
//                    }
//                    tx = new EthTransaction(tv.getPublicKey().toByteArray(),
//                            tv.getNonce().toByteArray(),
//                            tv.getFutureEventNumber(),
//                            tv.getGasPrice().toByteArray(),
//                            tv.getGasLimit().toByteArray(),
//                            tv.getReceiveAddress().toByteArray(),
//                            tv.getValue().toByteArray(),
//                            tv.getData().toByteArray(),
//                            byteArrayWrapperSet,
//                            tv.getSignature().toByteArray(),
//                            tv.getHash().toByteArray(), tv.getRlpEncoded().toByteArray());
//
//                    if (logger.isDebugEnabled()) {
//                        logger.debug("receive tx:{}!", Hex.toHexString(tx.getHash()));
//                    }
//
//                    list[i] = tx;
//                }
//                EventCollector.importPayload(list, new ArrayList<>());
//                long s2 = System.currentTimeMillis();
//                logger.info("ApiService asyncImportTx. size: {},  time:{}", list.length, (s2 - s1));
//            }
//
//            @Override
//            protected void doException(Throwable e) {
//                logger.info("ApiService asyncImportTx error.", e);
//            }
//        }.start();
//    }


    @Override
    public void pushGlobalNodeEvents(GlobalNodeEventsPushDTO request, StreamObserver<DefaultResponse> responseObserver) {
        DefaultResponse response = DefaultResponse.newBuilder().setResult(true).build();
        if (CollectionUtils.isEmpty(request.getNodesList())) {
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        try {
            List<GlobalNodeEvent> list = new ArrayList<>();
            GlobalNodeEvent nodeEvent = null;
            for (GlobalNodeEventsPushDTO.GlobalNodeEventPushDTO t : request.getNodesList()) {
                nodeEvent = new GlobalNodeEvent(t.getHash().toByteArray(),
                        t.getPublicKey().toByteArray(),
                        t.getNonce().toByteArray(),
                        t.getFutureEventNumber(),
                        (byte)t.getCommandCode(),
                        t.getData().toByteArray(),
                        t.getSignature().toByteArray());
                list.add(nodeEvent);
            }
            this.gatewayFacade.txnManager.eventCollector.importPayload(new EthTransaction[0], list);
        } catch (Exception e) {
            logger.error("ApiService pushGlobalNodeEvents error. ",  e);
        }
        // 返回
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getGlobalNodeEvent(BytesObject request, StreamObserver<BytesObject> responseObserver) {
        try {
            GlobalNodeEvent nodeEvent = gatewayFacade.getGlobalNodeEvent(request.getValue().toByteArray());
            if (nodeEvent == null) {
                responseObserver.onNext(BytesObject.newBuilder().build());
                responseObserver.onCompleted();
                return;
            }

            responseObserver.onNext(BytesObject.newBuilder().setValue(ByteString.copyFrom(nodeEvent.getEncoded())).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("ApiService getGlobalNodeEvent error. ", e);
            responseObserver.onNext(BytesObject.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getGlobalNodeEventReceipt(com.thanos.api.proto.push.BytesObject request,
                                          io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
        try {
            GlobalNodeEventReceipt nodeEventReceipt = gatewayFacade.getGlobalNodeEventReceipt(request.getValue().toByteArray());
            if (nodeEventReceipt == null) {
                responseObserver.onNext(BytesObject.newBuilder().build());
                responseObserver.onCompleted();
                return;
            }

            responseObserver.onNext(BytesObject.newBuilder().setValue(ByteString.copyFrom(nodeEventReceipt.getEncoded())).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("ApiService getGlobalNodeEventReceipt error. ", e);
            responseObserver.onNext(BytesObject.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getEpochState(com.thanos.api.proto.push.DefaultRequest request,
                              io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
        try {
            EpochState epochState = gatewayFacade.getCurrentEpoch();
            if (epochState == null) {
                responseObserver.onNext(BytesObject.newBuilder().build());
                responseObserver.onCompleted();
                return;
            }

            responseObserver.onNext(BytesObject.newBuilder().setValue(ByteString.copyFrom(epochState.getEncoded())).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("ApiService getEpochState error. ", e);
            responseObserver.onNext(BytesObject.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void saveAlive(DefaultRequest request, StreamObserver<DefaultResponse> responseObserver) {
        responseObserver.onNext(DefaultResponse.newBuilder().setResult(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getLatestBeExecutedNum(DefaultRequest request, StreamObserver<LongObject> responseObserver) {
        try {
            long num = gatewayFacade.getLatestBeExecutedNum();
            responseObserver.onNext(LongObject.newBuilder().setValue(num).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("ApiService getLatestBeExecutedNum error. ", e);
            responseObserver.onNext(LongObject.newBuilder().build());
            responseObserver.onCompleted();
        }

    }

    @Override
    public void getLatestConsensusNumber(DefaultRequest request, StreamObserver<LongObject> responseObserver) {
        try {
            long num = gatewayFacade.getLatestConsensusNumber();
            responseObserver.onNext(LongObject.newBuilder().setValue(num).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("ApiService getLatestConsensusNumber error. ", e);
            responseObserver.onNext(LongObject.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getCurrentCommitRound(DefaultRequest request, StreamObserver<LongObject> responseObserver) {
        try {
            long num = gatewayFacade.getCurrentCommitRound();
            responseObserver.onNext(LongObject.newBuilder().setValue(num).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("ApiService getCurrentCommitRound error. ", e);
            responseObserver.onNext(LongObject.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getEthTransactionByHash(BytesObject request, StreamObserver<BytesObject> responseObserver) {
        try {
            EthTransactionReceipt receipt = gatewayFacade.getTransactionByHash(request.getValue().toByteArray());
            if (receipt == null) {
                responseObserver.onNext(BytesObject.newBuilder().build());
                responseObserver.onCompleted();
                return;
            }
            responseObserver.onNext(BytesObject.newBuilder().setValue(ByteString.copyFrom(receipt.getEncoded())).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("ApiService getEthTransactionByHash error. ", e);
            responseObserver.onNext(BytesObject.newBuilder().build());
            responseObserver.onCompleted();
        }

    }

    @Override
    public void getEthTransactionsByHashes(ListBytesObject request, StreamObserver<ListBytesObject> responseObserver) {
        try {
            List<byte[]> reqList = new ArrayList<>();
            for (ByteString bs : request.getValueList()) {
                reqList.add(bs.toByteArray());
            }
            List<byte[]> res = gatewayFacade.getTransactionsByHashes(reqList);
            List<ByteString> resByteString = new ArrayList<>();
            for (byte[] bs : res) {
                resByteString.add(ByteString.copyFrom(bs));
            }
            ListBytesObject bytesObject = ListBytesObject.newBuilder().addAllValue(resByteString).build();
            responseObserver.onNext(bytesObject);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("ApiService getEthTransactionsByHashes error. ", e);
            responseObserver.onNext(ListBytesObject.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getBlockByNumber(LongObject request, StreamObserver<BlockBytesObject> responseObserver) {
        try {
            long num = request.getValue();
            Block block = gatewayFacade.getBlockByNumber(num);
            if (block == null) {
                responseObserver.onNext(BlockBytesObject.newBuilder().build());
                responseObserver.onCompleted();
                return;
            }
            byte[] blockBytes = block.getEncoded();
            BlockBytesObject blockBytesObject = BlockBytesObject.newBuilder()
                    .setBlockBaseInfo(ByteString.copyFrom(blockBytes))
                    .build();

            responseObserver.onNext(blockBytesObject);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("ApiService getBlockByNumber error. ", e);
            responseObserver.onNext(BlockBytesObject.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 暂不实现，有需要再实现
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void getEventDataByNumber(LongObject request, StreamObserver<BytesObject> responseObserver) {
//        long num = request.getValue();
//        EventData eventData = gatewayFacade.getEventDataByNumber(num);
//        responseObserver.onNext(null);
//        responseObserver.onCompleted();
    }

    @Override
    public void ethCall(EthTransactionsPushDTO.EthTransactionPushDTO tv, StreamObserver<BytesObject> responseObserver) {
        try {
            Set<ByteArrayWrapper> byteArrayWrapperSet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(tv.getExecuteStatesList())) {
                for (ByteString bs : tv.getExecuteStatesList()) {
                    ByteArrayWrapper bw = new ByteArrayWrapper(bs.toByteArray());
                    byteArrayWrapperSet.add(bw);
                }
            }
            EthTransaction tx = new EthTransaction(tv.getPublicKey().toByteArray(),
                    tv.getNonce().toByteArray(),
                    tv.getFutureEventNumber(),
                    tv.getGasPrice().toByteArray(),
                    tv.getGasLimit().toByteArray(),
                    tv.getReceiveAddress().toByteArray(),
                    tv.getValue().toByteArray(),
                    tv.getData().toByteArray(),
                    byteArrayWrapperSet,
                    tv.getSignature().toByteArray(),
                    tv.getHash().toByteArray(),
                    tv.getRlpEncoded().toByteArray());
            EthTransactionReceipt receipt = gatewayFacade.ethCall(tx);
            responseObserver.onNext(BytesObject.newBuilder().setValue(ByteString.copyFrom(receipt.getEncoded())).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("ApiService ethCall error. ", e);
            responseObserver.onNext(BytesObject.newBuilder().build());
            responseObserver.onCompleted();
        }
    }
}