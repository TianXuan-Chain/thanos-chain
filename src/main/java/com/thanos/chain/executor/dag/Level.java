package com.thanos.chain.executor.dag;

import io.netty.util.Recycler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类Level.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-20 13:59:56
 */
public class Level {

    private static final Recycler<Level> levelRECYCLER = new Recycler<Level>() {
        @Override
        protected Level newObject(Handle<Level> handle) {
            return new Level(handle, -1);
        }
    };

    static Level newInstance(int levelIndex, Level preParallelLevel) {
        Level level = levelRECYCLER.get();
        level.reuse(levelIndex, preParallelLevel);
        return level;
    }

    Level preParallelLevel;

    int levelIndex;

    ReuseCountDownLatch nextParallelLevelAwaitCondition;

    AtomicInteger exeCount;

    Recycler.Handle<Level> handle;

    public Level(Recycler.Handle<Level> handle, int levelIndex) {
        this.handle = handle;
        this.levelIndex = levelIndex;
        this.exeCount = new AtomicInteger(0);
        this.nextParallelLevelAwaitCondition = new ReuseCountDownLatch(0);

    }

    public void reuse(int levelIndex, Level preParallel) {
        this.levelIndex = levelIndex;
        this.preParallelLevel = preParallel;
        this.exeCount.set(0);
        nextParallelLevelAwaitCondition.reuse(0);
    }

//    public void resetNextNextLevelAwaitCondition() {
//        this.nextParallelLevelAwaitCondition.reuse(exeCount.get());
//    }

    public void oneNodeFinishExecute() {
        this.nextParallelLevelAwaitCondition.releaseLast();
        this.exeCount.getAndDecrement();
        //System.out.println("this level [" + levelIndex + "],exeCount = " + num + " , condition = " + nextParallelLevelAwaitCondition);
    }

    public boolean isPreParallelLevelAllNodeFinish() {
        return this.preParallelLevel == null?
                true: this.preParallelLevel.exeCount.get() == 0;
    }

    public void awaitLPreParallelLevelAllNodeFinish() {
        try {
            if (preParallelLevel == null) return;
            preParallelLevel.nextParallelLevelAwaitCondition.await(10, TimeUnit.MILLISECONDS);
            //nextParallelLevelAwaitCondition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void recycle() {
        this.preParallelLevel = null;
        handle.recycle(this);
    }

    @Override
    public String toString() {

        return "Level{[" + hashCode() + "]" +
                "levelIndex=" + levelIndex + //"\n" +
                ", nextParallelLevelAwaitCondition=" + nextParallelLevelAwaitCondition + //"\n" +
                ", exeNum=" + exeCount.get() + //"\n" +
                ", preParallelLevel=" + preParallelLevel + //"\n" +
                '}';
    }
}
