package com.thanos.chain.consensus.hotstuffbft;

import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.executor.dag.ExecutableGenerator;
import com.thanos.chain.executor.dag.ExecutableGeneratorImpl;
import com.thanos.chain.executor.dag.ExecuteRoot;
import com.thanos.common.utils.ThanosThreadFactory;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * EventPayloadDecoder.java description：
 *
 * @Author laiyiyu create on 2020-08-17 14:41:02
 */
public class EventPayloadDecoder {

    static ThreadPoolExecutor decodeExecutor = new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new ThanosThreadFactory("event_payload_decode_consensus"));

    static ExecutableGenerator executableGenerator = new ExecutableGeneratorImpl();

    public static void asyncDecodePayload(EventData eventData) {
        decodeExecutor.execute(() -> {
            eventData.getPayload().reDecoded();
            List<ExecuteRoot> executeRoots = executableGenerator.generate(eventData.getPayload().getEthTransactions()).getExecuteRoots();
            eventData.setDagExecuteRoots(executeRoots);
        });
    }

    public static void decodePayload(EventData eventData) {
        eventData.getPayload().reDecoded();
        List<ExecuteRoot> executeRoots = executableGenerator.generate(eventData.getPayload().getEthTransactions()).getExecuteRoots();
        eventData.setDagExecuteRoots(executeRoots);
    }
}
