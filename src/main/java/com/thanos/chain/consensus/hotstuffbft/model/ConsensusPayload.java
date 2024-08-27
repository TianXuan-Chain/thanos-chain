package com.thanos.chain.consensus.hotstuffbft.model;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.thanos.chain.config.ConfigResourceUtil;
import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.ledger.model.RLPModel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ConsensusPayload.java description：
 *
 * @Author laiyiyu create on 2020-03-08 19:02:43
 */
public class ConsensusPayload extends RLPModel {

    private static final Logger logger = LoggerFactory.getLogger("consensus");

    private static final boolean TX_VERIFY = ConfigResourceUtil.loadSystemConfig().isTXValid();
    static final int PROCESSOR_NUM = ConfigResourceUtil.loadSystemConfig().decodeProcessNum();

    static final class DecodeEvent {

        EthTransaction[] fillEthTransactions;

        int start;

        int end;

        int payloadStartOffset;

        RLPList payload;

        CountDownLatch countDownLatch;
    }

    static final class DecodeEventConsumer implements EventHandler<DecodeEvent> {

        @Override
        public void onEvent(DecodeEvent decodeEvent, long sequence, boolean endOfBatch) throws Exception {
            for (int i = decodeEvent.start; i <= decodeEvent.end; i++) {
                EthTransaction ethTransaction = new EthTransaction(decodeEvent.payload.get(i + decodeEvent.payloadStartOffset).getRLPData());
                ethTransaction.verify(TX_VERIFY);
                decodeEvent.fillEthTransactions[i] = ethTransaction;
            }

            // help gc
            decodeEvent.payload = null;
            decodeEvent.fillEthTransactions = null;
            decodeEvent.countDownLatch.countDown();
            decodeEvent.countDownLatch = null;
        }
    }

    static final int COMPRESS_SIZE_THRESHOLD = 10000;
    
    //static final int PROCESSOR_NUM = Runtime.getRuntime().availableProcessors();


    static final AtomicLong THREAD_COUNTER = new AtomicLong(0);

    static final int PARALLEL_THRESHOLD = 50;

    static List<Disruptor<DecodeEvent>> decodeDisruptors;

    static List<RingBuffer<DecodeEvent>> riskControlComputeEventRingBuffers;

    static EventFactory<DecodeEvent> eventFactory;

    static ThreadFactory threadFactory;

    static DecodeEventConsumer decodeEventConsumer;

    static {
        eventFactory = () -> new DecodeEvent();
        threadFactory = r -> new Thread(r, "consensusPayload_decode_thread_" + THREAD_COUNTER.incrementAndGet());
        decodeEventConsumer = new DecodeEventConsumer();
        decodeDisruptors =  new ArrayList<>(PROCESSOR_NUM);
        riskControlComputeEventRingBuffers = new ArrayList<>(PROCESSOR_NUM);
        for (int i = 0; i < PROCESSOR_NUM; i++) {
            Disruptor<DecodeEvent> decodeDisruptor = new Disruptor(eventFactory, 128, threadFactory);
            decodeDisruptors.add(decodeDisruptor);
            decodeDisruptor.handleEventsWith(decodeEventConsumer);
            riskControlComputeEventRingBuffers.add(decodeDisruptor.getRingBuffer());
            decodeDisruptor.start();
        }
    }

    static void publicEvent(EthTransaction[] fillEthTransactions, int start, int end, int cpuIndex, int payloadStartOffset, RLPList payload, CountDownLatch countDownLatch) {
        RingBuffer<DecodeEvent> decodeRingBuffer = riskControlComputeEventRingBuffers.get(cpuIndex);
        long seq = decodeRingBuffer.next();
        try {
            DecodeEvent decodeEvent = decodeRingBuffer.get(seq);
            decodeEvent.fillEthTransactions = fillEthTransactions;
            decodeEvent.start = start;
            decodeEvent.end = end;
            decodeEvent.payloadStartOffset = payloadStartOffset;
            decodeEvent.payload = payload;
            decodeEvent.countDownLatch = countDownLatch;
        } catch (Exception e) {
            logger.warn("ConsensusPayload publicEvent error! {}", ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("ConsensusPayload publicEvent error!", e);
        } finally {
            decodeRingBuffer.publish(seq);
        }
    }

    // for speed
    volatile EthTransaction[] ethTransactions;

    boolean parallelDecode = false;

    public ConsensusPayload() {
        this(new EthTransaction[0]);
    }

    public ConsensusPayload(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public static ConsensusPayload buildNoEncode(byte[] rlpEncoded) {
        ConsensusPayload result = new ConsensusPayload((byte[])null);
        result.rlpEncoded = rlpEncoded;
        return result;
    }

    public ConsensusPayload(EthTransaction[] ethTransactions) {
        super(null);
        assert ethTransactions != null;
        this.ethTransactions = ethTransactions;
        this.parsed = true;
        this.rlpEncoded = rlpEncoded();
    }

    public void setParallelDecode(boolean parallelDecode) {
        this.parallelDecode = parallelDecode;
    }

    public EthTransaction[] getEthTransactions() {
        return ethTransactions;
    }

    public boolean isEmpty() {
        return ethTransactions.length == 0;
    }

    public void reDecoded() {
        if (!super.parsed) {
            synchronized (this) {
                if (super.parsed) return;
                rlpDecoded();
                super.parsed = true;
            }
        }
    }

    public boolean getParsed() {
        return parsed;
    }

    // for  test
    public void setParsed() {
        super.parsed = true;
    }

    @Override
    protected byte[] rlpEncoded() {
        boolean compress = ethTransactions.length >= COMPRESS_SIZE_THRESHOLD;
        if (!compress) {
            // encode un compress
            byte[][] encode = new byte[1 + ethTransactions.length][];
            encode[0] = RLP.encodeInt(0);
            int i = 1;
            for (EthTransaction ethTransaction : ethTransactions) {
                encode[i] = ethTransaction.getEncoded();
                i++;
            }
            return RLP.encodeList(encode);
            
        } else {
            // encode compress
            byte[][] encode = new byte[2][];
            encode[0] = RLP.encodeInt(1);
            int i = 0;
            byte[][] txEncodes = new byte[ethTransactions.length][] ;
            for (EthTransaction ethTransaction : ethTransactions) {
                txEncodes[i] = ethTransaction.getEncoded();
                i++;
            }
            
            try {
                encode[1] = RLP.encodeElement(Snappy.compress(RLP.encodeList(txEncodes)));
            } catch (IOException e) {
                logger.warn("ConsensusPayload encode compress error", e);
                throw new RuntimeException("ConsensusPayload encode compress error", e);
            }

            return RLP.encodeList(encode);
        }
    }

    @Override
    protected void rlpDecoded() {
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList payload = (RLPList) params.get(0);

        boolean compress = ByteUtil.byteArrayToInt(payload.get(0).getRLPData()) == 1? true: false;
        if (!compress) {
            // uncompress decode;
            decodeTxs(payload, 1);
        } else {
            // compress decode;
            try {
                //System.out.println("user compress decode!");
                RLPList txRLPs = (RLPList) RLP.decode2(Snappy.uncompress(payload.get(1).getRLPData())).get(0);
                decodeTxs(txRLPs, 0);
            } catch (IOException e) {
                logger.warn("ConsensusPayload decode uncompress error", e);
                throw new RuntimeException("ConsensusPayload decode uncompress error", e);
            }
        }
    }
    
    private void decodeTxs(RLPList payload, int payloadStartOffset) {
        int txCounter = payloadStartOffset;
        int txsSize = payload.size() - txCounter;
        EthTransaction[] ethTransactions = new EthTransaction[txsSize];

        if (txsSize >0 && txsSize < PARALLEL_THRESHOLD) {

            for (; txCounter < payload.size(); txCounter++) {
                EthTransaction ethTransaction = new EthTransaction(payload.get(txCounter).getRLPData());
                ethTransaction.verify(TX_VERIFY);
                ethTransactions[txCounter - payloadStartOffset] = ethTransaction;
            }

        } else if (txsSize >= PARALLEL_THRESHOLD) {
            try {
                int batchSize = txsSize / PROCESSOR_NUM;
                int remainder = txsSize % PROCESSOR_NUM;

                CountDownLatch await = new CountDownLatch(PROCESSOR_NUM);
                //System.out.println("user parallel!");
                for (int count = 0; count < PROCESSOR_NUM; count++) {
                    int startPosition = count * (batchSize);
                    int endPosition = (count + 1)* batchSize -1;

                    if (count == PROCESSOR_NUM - 1) {
                        endPosition += remainder;
                    }

                    publicEvent(ethTransactions, startPosition, endPosition, count, payloadStartOffset, payload, await);
                    //System.out.println(startPosition + "-" + endPosition + "-" + count);
                }
                await.await();
            } catch (Exception e) {
                logger.warn("decodeTxs error!", e);
                throw new RuntimeException("decodeTxs error!", e);
            }
        }

        this.ethTransactions = ethTransactions;
    }

    @Override
    public String toString() {
        return "ConsensusPayload{" +
                (ethTransactions != null?
                "ethTransactions size=" + ethTransactions.length : "")
                        +
                '}';
    }

    public void clear() {
        this.ethTransactions = null;
    }
}


