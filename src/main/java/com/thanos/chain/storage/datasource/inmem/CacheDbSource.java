package com.thanos.chain.storage.datasource.inmem;

import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.chain.storage.datasource.AbstractDbSource;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CacheDbSource.java description：
 *
 * @Author laiyiyu create on 2020-02-25 20:33:42
 */
public class CacheDbSource extends AbstractDbSource {

    private ConcurrentMap<Keyable, Persistable> cache = new ConcurrentHashMap<>();

    public CacheDbSource() {
        config = SystemConfig.getDefault();
    }

    @Override
    public Persistable get(Class<?> model, Keyable keyable) {
        return this.cache.get(keyable);
    }

    @Override
    public byte[] getRaw(Class<?> model, Keyable keyable) {
        Persistable persistable = this.cache.get(keyable);
        if (persistable != null) {
            return persistable.valueBytes();
        }
        return null;
    }

    @Override
    public Map<byte[], byte[]> batchGetRaw(Class<?> model, List<byte[]> keys) {
        Map<byte[], byte[]> result = new HashMap<>();
        for (byte[] key: keys) {
            Keyable keyable = Keyable.ofDefault(key);
            Persistable persistable = cache.get(keyable);
            if (persistable == null) {
                result.put(key, null);
            } else {
                result.put(key, persistable.valueBytes());
            }

        }
        return result;
    }

    @Override
    public void put(Keyable keyable, Persistable persistable) {
        this.cache.put(keyable, persistable);
    }

    @Override
    public void updateBatch(List<Pair<Keyable, Persistable>> saveBatch) {
        for (Pair<Keyable, Persistable> pair: saveBatch) {
            this.cache.put(pair.getLeft(), pair.getRight());
        }
    }

    @Override
    public List<Persistable> getAll(Class<?> model) {
        List<Persistable> result = new ArrayList<>();
        for (Map.Entry<Keyable, Persistable> entry: cache.entrySet()) {
            if (entry.getValue().getClass().isAssignableFrom(model)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }
}
