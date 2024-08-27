package com.thanos.chain.executor.dag;

import io.netty.util.Recycler;

import java.util.List;

/**
 * 类SerialExecute.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-20 19:50:38
 */
public class SerialExecutables {

    private static final Recycler<SerialExecutables> SerialRECYCLER = new Recycler<SerialExecutables>() {
        @Override
        protected SerialExecutables newObject(Handle<SerialExecutables> handle) {
            return new SerialExecutables(handle);
        }
    };

    static SerialExecutables newInstance(List<ExecuteRoot> executeRootNodes) {
        SerialExecutables serialExecutables = SerialRECYCLER.get();
        serialExecutables.reuse(executeRootNodes);
        return serialExecutables;
    }

    List<ExecuteRoot> executeRoots;

    Recycler.Handle<SerialExecutables> handle;

    public SerialExecutables(Recycler.Handle<SerialExecutables> handle) {
        this.handle = handle;
    }

    public List<ExecuteRoot> getExecuteRoots() {
        List<ExecuteRoot> executeRoots = this.executeRoots;
        recycle();
        return executeRoots;
    }

    public void recycle() {
        //avoid memory leak
        executeRoots = null;
        handle.recycle(this);
    }

    private void reuse(List<ExecuteRoot> executeRoots) {
        this.executeRoots = executeRoots;
    }
}
