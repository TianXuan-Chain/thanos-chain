package com.thanos.chain.executor;

import com.thanos.common.utils.ThanosWorker;
import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MpmcThreadExecutor.java description：
 *
 * @Author laiyiyu create on 2020-11-16 10:41:47
 */
public class MpmcThreadExecutor implements Executor {

    //MpmcAtomicArrayQueue<Runnable> taskQueue;
    //MpmcArrayQueue<Runnable> taskQueue;
    ArrayBlockingQueue<Runnable> taskQueue;


    ReentrantLock reentrantLock = new ReentrantLock(false);
    Condition emptyCondition = reentrantLock.newCondition();

    public MpmcThreadExecutor(int queueSize, int processorNum) {
        //taskQueue = new MpmcAtomicArrayQueue(queueSize);
        //taskQueue = new MpmcArrayQueue(queueSize);
        taskQueue = new ArrayBlockingQueue<>(queueSize);

        for(int i = 0; i < processorNum; i++) {
            new ThanosWorker("mpmc_thread_" + i) {
                @Override
                protected void doWork() throws Exception {
//                    Runnable task = taskQueue.poll();
//                    if (task == null) {
//                        Thread.yield();
//                    } else {
//                        task.run();
//                    }

                    Runnable task = taskQueue.take();
                    task.run();

                }
            }.start();
        }

    }

    public void execute(Runnable task) {
        try {
            taskQueue.offer(task);
        } catch (Exception e) {

        }
    }
}
