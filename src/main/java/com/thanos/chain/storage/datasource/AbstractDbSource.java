package com.thanos.chain.storage.datasource;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.chain.storage.datasource.rocksdb.WriteBatchFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AbstractDbSource.java description：
 *
 * @Author laiyiyu create on 2020-02-25 20:26:34
 */
public abstract class AbstractDbSource {

    static {
        RocksDB.loadLibrary();
    }

    public RocksDB db;

    public SystemConfig config;

    public WriteBatchFactory writeBatchFactory;

    public final List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();

    public final Map<Class<? extends Persistable>, ColumnFamilyHandle> clazz2HandleTable = new HashMap<>();

    public final Map<Class<? extends Persistable>, Constructor> clazz2ConstructorTable = new HashMap<>();

    public abstract Persistable get(Class<?> model, Keyable keyable);

    public abstract byte[] getRaw(Class<?> model, Keyable keyable);

    public abstract Map<byte[], byte[]> batchGetRaw(Class<?> model, List<byte[]> keys);

    public abstract void put(Keyable keyable, Persistable persistable);

    public abstract void updateBatch(List<Pair<Keyable, Persistable>> saveBatch);

    public abstract List<Persistable> getAll(Class<?> model);
}
