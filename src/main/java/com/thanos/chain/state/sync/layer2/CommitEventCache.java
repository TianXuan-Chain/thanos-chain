package com.thanos.chain.state.sync.layer2;

import com.thanos.chain.consensus.hotstuffbft.model.EventData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CommitEventCache.java description：
 *
 * @Author laiyiyu create on 2020-07-06 10:27:49
 */
public class CommitEventCache {

    private static final int CACHE_SIZE = 10000;

    LinkedList<EventData> commitCache;

    ReentrantLock cacheLock;

    volatile long startEventNum;

    volatile long endEventNum;

    public CommitEventCache() {
        commitCache = new LinkedList<>();
        this.cacheLock = new ReentrantLock(false);
        startEventNum = 0;
        endEventNum = 0;
    }

    void updateLatestCommitEvent(EventData eventData) {
        try {
            cacheLock.lock();
            if (commitCache.size() > CACHE_SIZE) {
                EventData removeEvent = commitCache.removeFirst();
                startEventNum = removeEvent.getNumber() + 1;
            }
            commitCache.push(eventData);
            endEventNum = eventData.getNumber();

        } finally {
            cacheLock.unlock();
        }
    }

    List<EventData> readCache(long startNum, long endNum) {
        try {
            cacheLock.lock();
            if (startNum < this.startEventNum) {
                startNum = this.startEventNum;
            }

            if (endNum > this.endEventNum) {
                endNum = this.endEventNum;
            }

            int start = (int) (startNum - this.startEventNum);
            int end = (int) (endNum - this.startEventNum);
            List<EventData> result = new ArrayList((int) (end - start + 1));
            for (int i = start; i <= end; i++) {
                result.add(this.commitCache.get(i));
            }
            return commitCache;
        } finally {
            cacheLock.unlock();
        }
    }
}
