package com.thanos.chain.txpool;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.ThanosWorker;
import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * EventCollector.java description：
 *
 * @Author laiyiyu create on 2020-08-19 09:50:26
 */
public class EventCollector {

    private static final Logger logger = LoggerFactory.getLogger("tx-pool");

    public static Object IN_COMING_MONITOR = new Object();

    public final LinkedList<GlobalNodeEvent> IN_COMING_EVENTS;

    public final ArrayBlockingQueue<EthTransaction[]> IN_COMING_TXS;

    public final TxnPool txnPool;

    public EventCollector(TxnPool txnPool, int comingQueueSize, boolean test) {
        this.txnPool = txnPool;
        IN_COMING_EVENTS = new LinkedList();
        IN_COMING_TXS = new ArrayBlockingQueue<>(comingQueueSize);
        if (!test) {
            start();
        }
        //
    }


    public void start() {
        new ThanosWorker("do_import_thread") {

            @Override
            protected void beforeLoop() {
                try {
                    logger.info("do_import_thread start success!");
                    Thread.sleep(3000);
                } catch (InterruptedException e) {

                }
            }

            @Override
            protected void doWork() throws Exception {
                List<EthTransaction[]> ethTransactionArrays = new ArrayList<>(30);
                int i = 0;
                GlobalNodeEvent[] globalNodeEvents;
                //EthTransaction[] txs;
                synchronized (EventCollector.IN_COMING_MONITOR) {
                    globalNodeEvents = new GlobalNodeEvent[EventCollector.this.IN_COMING_EVENTS.size()];
                    for (i = 0; i < EventCollector.this.IN_COMING_EVENTS.size(); i++) {
                        globalNodeEvents[i] = EventCollector.this.IN_COMING_EVENTS.pop();
                    }
                }

                if (IN_COMING_TXS.size() > 0) {
                    EventCollector.this.IN_COMING_TXS.drainTo(ethTransactionArrays);
                } else {
                    Thread.sleep(1000);
                }
                EventCollector.this.txnPool.doImport(globalNodeEvents, ethTransactionArrays);
            }
        }.start();
    }

    public boolean importPayload(EthTransaction[] ethTransactions, List<GlobalNodeEvent> globalNodeEvents) {
        if (!CollectionUtils.isEmpty(globalNodeEvents)) {
            synchronized (IN_COMING_MONITOR) {
                IN_COMING_EVENTS.addAll(globalNodeEvents);
                //if (IN_COMING_TXS.size() > MAX_IN_COMING_SIZE) return false;
                //IN_COMING_TXS.addAll(ethTransactions);
            }
        }

        if (ethTransactions.length != 0) {
            try {
                IN_COMING_TXS.put(ethTransactions);
            } catch (InterruptedException e) {

            }
        }
        return true;
    }
}
