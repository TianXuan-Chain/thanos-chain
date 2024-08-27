package com.thanos.chain.storage.datasource.rocksdb;

import com.thanos.chain.config.SystemConfig;
import com.thanos.common.crypto.key.symmetric.CipherKey;
import com.thanos.chain.ledger.model.store.TestModel;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.chain.storage.datasource.AbstractDbSource;
import com.thanos.chain.storage.datasource.DbSettings;
import org.apache.commons.lang3.tuple.Pair;
import org.rocksdb.*;
import org.rocksdb.util.SizeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.thanos.common.utils.ByteUtil.toHexString;
import static org.rocksdb.WALRecoveryMode.AbsoluteConsistency;

/**
 * RocksDbSource.java description：
 *
 * @Author laiyiyu create on 2020-02-22 20:02:59
 */
public class RocksDbSource extends AbstractDbSource {


    private static final Logger logger = LoggerFactory.getLogger("db");

    String name;

    // initialized for standalone test

    ReadOptions readOpts;

    DBOptions dbOptions;

    DbSettings settings;

    CipherKey cipherKey;

    // The native RocksDB insert/update/delete are normally thread-safe
    // However close operation is not thread-safe.
    // This ReadWriteLock still permits concurrent execution of insert/delete/update operations
    // however blocks them on init/close/delete operations
    // private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();
    public RocksDbSource(String name, Map<String, Class<? extends Persistable>> columnFamilies, SystemConfig systemConfig) {
        this(name, columnFamilies, systemConfig, DbSettings.DEFAULT);
    }

    public RocksDbSource(String name, Map<String, Class<? extends Persistable>> columnFamilies, SystemConfig systemConfig, DbSettings settings) {
        this.name = name;
        config = systemConfig;
        this.settings = settings;
        this.writeBatchFactory = new WriteBatchFactory(config);
        this.cipherKey = config.getCipherKey();
        logger.debug("New RocksDbSource: " + name);
        init(columnFamilies);
    }

    private void init(Map<String, Class<? extends Persistable>> columnFamilies) {
        try {

            dbOptions = new DBOptions();
            // general options
            dbOptions.setCreateIfMissing(true)
                    // For now we set the max total WAL size to be 512M. This config can be useful when column
                    // families are updated at non-uniform frequencies.
                    .setMaxTotalWalSize(0)
                    //.setWalRecoveryMode(AbsoluteConsistency)
                    .setCreateMissingColumnFamilies(true)
                    .setMaxOpenFiles(settings.getMaxOpenFiles())
                    .setIncreaseParallelism(settings.getMaxThreads())
                    .setMaxBackgroundCompactions(settings.getMaxThreads())
                    .setMaxBackgroundFlushes(settings.getMaxThreads())
                    .setAllowConcurrentMemtableWrite(true)
                    .setEnableWriteThreadAdaptiveYield(true)
                    .setInfoLogLevel(InfoLogLevel.ERROR_LEVEL)
                    .setMaxSubcompactions(settings.getMaxThreads());


            // read options
            readOpts = new ReadOptions();
            readOpts = readOpts.setPrefixSameAsStart(true)
                    .setVerifyChecksums(false);

            // key prefix for state node lookups
            //options.useFixedLengthPrefixExtractor(NodeKeyCompositor.PREFIX_BYTES);

            //BlockBasedTable 是 SSTable 的默认表格式。
            BlockBasedTableConfig blockBasedTableConfig = new BlockBasedTableConfig();
            blockBasedTableConfig
                    .setCacheNumShardBits(2)
                    .setBlockSizeDeviation(10)
                    .setBlockRestartInterval(64)
                    .setBlockCacheSize(-1)
                    //.setBlockCacheSize(100_000 * SizeUnit.KB)
                    .setBlockCacheCompressedNumShardBits(10)
                    .setBlockCacheCompressedSize(0);
            if (settings.isBloomFilterFlag()) {
                blockBasedTableConfig.setFilter(new BloomFilter(10, false));
            }
            // .setBlockCacheCompressedSize(32 * SizeUnit.KB);

            ColumnFamilyOptions columnFamilyOptions = new ColumnFamilyOptions()
                    .setTableFormatConfig(blockBasedTableConfig)
                    .setMaxWriteBufferNumber(4)
                    .setMinWriteBufferNumberToMerge(4)
                    .setWriteBufferSize(settings.getWriteBufferSize() * SizeUnit.MB);

            List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();
            columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, columnFamilyOptions));


            for (String name : columnFamilies.keySet()) {
                columnFamilyDescriptors.add(new ColumnFamilyDescriptor(name.getBytes(), columnFamilyOptions));
            }


            db = RocksDB.open(dbOptions, getPath().toString(), columnFamilyDescriptors, columnFamilyHandles);

            initProcessTable(columnFamilies, columnFamilyDescriptors);
        } catch (Exception e) {
            close(db, dbOptions);
            throw new RuntimeException(e);
        }

    }

    private void initProcessTable(Map<String, Class<? extends Persistable>> columnFamilies, List<ColumnFamilyDescriptor> columnFamilyDescriptors) throws NoSuchMethodException {
        //skip default column
        int i = 1;
        for (; i < columnFamilyDescriptors.size(); i++) {
            String name = new String(columnFamilyDescriptors.get(i).getName());
            clazz2HandleTable.put(columnFamilies.get(name), this.columnFamilyHandles.get(i));
            clazz2ConstructorTable.put(columnFamilies.get(name), columnFamilies.get(name).getDeclaredConstructor(byte[].class));
        }
    }


    private Path getPath() {
        return Paths.get(config.databaseDir(), name);
    }


    public byte[] getRaw(Class<?> model, Keyable keyable) {
        try {
            ColumnFamilyHandle handle = clazz2HandleTable.get(model);
            byte[] encryptedKey = cipherKey.encrypt(keyable.keyBytes());
            byte[] ret = db.get(handle, readOpts, encryptedKey);
            return cipherKey.decrypt(ret);
        } catch (Exception e) {
            logger.error("Failed to get from db '{}'", name, e);
            hintOnTooManyOpenFiles(e);
            throw new RuntimeException(e);
        }
    }

    public Persistable get(Class<?> model, Keyable keyable) {
        try {
            ColumnFamilyHandle handle = clazz2HandleTable.get(model);
            byte[] encryptedKey = cipherKey.encrypt(keyable.keyBytes());
            byte[] ret = db.get(handle, readOpts, encryptedKey);
            if (ret == null) {
                return null;
            }
            return (Persistable) clazz2ConstructorTable.get(model).newInstance(cipherKey.decrypt(ret));
        } catch (Exception e) {
            logger.error("Failed to get from db '{}'", name, e);
            hintOnTooManyOpenFiles(e);
            throw new RuntimeException(e);
        }
    }


    public List<Persistable> getAll(Class<?> model) {
        ColumnFamilyHandle handle = clazz2HandleTable.get(model);
        try (RocksIterator iterator = db.newIterator(handle, readOpts)) {
            List<Persistable> result = new ArrayList<>();
            Constructor constructor = clazz2ConstructorTable.get(model);
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                result.add((Persistable) constructor.newInstance(cipherKey.decrypt(iterator.value())));
            }

            return result;
        } catch (Exception e) {
            logger.error("Failed to get from db '{}'", name, e);
            hintOnTooManyOpenFiles(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void put(Keyable keyable, Persistable persistable) {
        //WriteOptions writeOptions = null;
        try (WriteOptions writeOptions = new WriteOptions()) {


            //writeOptions = new WriteOptions();
            writeOptions.setSync(true);
            ColumnFamilyHandle handle = clazz2HandleTable.get(persistable.getClass());
            byte[] encryptedKey = cipherKey.encrypt(keyable.keyBytes());
            if (persistable.valueBytes() != null) {
                byte[] encryptedValue = cipherKey.encrypt(persistable.valueBytes());
                db.put(handle, writeOptions, encryptedKey, encryptedValue);
            } else {
                db.delete(handle, writeOptions, encryptedKey);
            }
            if (logger.isTraceEnabled())
                logger.trace("<~ RocksDbSource.put(): " + name + ", key: " + toHexString(keyable.keyBytes()) + ", " + (persistable.valueBytes() == null ? "null" : persistable.valueBytes().length));
        } catch (RocksDBException e) {
            logger.error("Failed to put into db '{}'", name, e);
            hintOnTooManyOpenFiles(e);
            throw new RuntimeException(e);
        }
    }


    public void updateBatch(List<Pair<Keyable, Persistable>> saveBatch) {

        //if (logger.isTraceEnabled()) logger.trace("~> RocksDbSource.updateBatch(): " + name + ", " + saveBatch.size());
        try {

            try (WriteBatch batch = writeBatchFactory.getInstance();
                 WriteOptions writeOptions = new WriteOptions()) {
                //For now we always use synchronous writes. This makes sure that once the operation returns
                //success, the data is persisted even if the machine crashes.
                writeOptions.setSync(true);
                for (Pair<Keyable, Persistable> pair : saveBatch) {
                    ColumnFamilyHandle handle = clazz2HandleTable.get(pair.getRight().getClass());
                    if (pair.getRight().valueBytes() == null) {
                        batch.delete(handle, pair.getLeft().keyBytes());
                    } else {
                        batch.put(handle, pair.getLeft().keyBytes(), pair.getRight().valueBytes());
                    }
                }
                //long start = System.currentTimeMillis();
                db.write(writeOptions, batch);
                //long end = System.currentTimeMillis();
                //logger.debug("write db cost:{}ms", (end - start));

            }

            //if (logger.isTraceEnabled()) logger.trace("<~ RocksDbSource.updateBatch(): " + name + ", " + saveBatch.size());
        } catch (RocksDBException e) {
            logger.error("Error in batch update on db '{}'", name, e);
            //hintOnTooManyOpenFiles(e);
            throw new RuntimeException(e);
        }
    }


    public Map<byte[], byte[]> batchGetRaw(Class<?> model, List<byte[]> keys) {
        ColumnFamilyHandle handle = clazz2HandleTable.get(model);
        try {

            List<ColumnFamilyHandle> handles = new ArrayList<>(keys.size());
            List<byte[]> encrypedKeys = new ArrayList<>(keys.size());
            keys.forEach(key -> handles.add(handle));
            keys.forEach(key -> encrypedKeys.add(cipherKey.encrypt(key)));
            Map<byte[], byte[]> result = db.multiGet(readOpts, handles, encrypedKeys);
            for (Map.Entry<byte[], byte[]> entry : result.entrySet()) {
                entry.setValue(cipherKey.decrypt(entry.getValue()));
            }
            return result;
        } catch (RocksDBException e) {
            logger.error("Failed to multiGet db [{}], error! {} ", name, e);
            hintOnTooManyOpenFiles(e);
            throw new RuntimeException(e);
        }
    }


    private void hintOnTooManyOpenFiles(Exception e) {
        if (e.getMessage() != null && e.getMessage().toLowerCase().contains("too many open files")) {
            logger.info("");
            logger.info("       Mitigating 'Too many open files':");
            logger.info("       either decrease value of database.maxOpenFiles parameter in thanos-chain.conf");
            logger.info("       or set higher limit by using 'ulimit -n' command in command line");
            logger.info("");
        }
    }

    public void shutdown() {
        for (final ColumnFamilyHandle columnFamilyHandle : columnFamilyHandles) {
            close(columnFamilyHandle);
        }
        close(db, readOpts, dbOptions);
    }

    public static void close(AutoCloseable... autoCloseables) {
        for (AutoCloseable it : autoCloseables) {
            try {
                if (it != null) {
                    it.close();
                }
            } catch (Exception ignored) {
                logger.debug("Silent exception occured", ignored);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(1 << 30);
        System.out.println(1 << 29);
        batchWrite();
        batchRead();
        readAll();
    }

    public static void doWrite() {
        RocksDbSource db = openDB();
        TestModel testModel = new TestModel("lyy", 10, Arrays.asList("name1", "name2"));
        db.put(testModel, testModel);
        db.shutdown();
    }

    public static void doRead() {
        RocksDbSource db = openDB();
        Keyable.DefaultKeyable keyable = new Keyable.DefaultKeyable("lyy".getBytes());
        TestModel testModel = (TestModel) db.get(TestModel.class, keyable);
        System.out.println(testModel);
        db.shutdown();
    }

    public static void batchWrite() {
        RocksDbSource db = openDB();
        List<Pair<Keyable, Persistable>> saveBatch = new ArrayList<>();
        TestModel testModel1 = new TestModel("hxx1", 10, Arrays.asList("name3", "name4"));
        TestModel testModel2 = new TestModel("hxx2", 10, Arrays.asList("name6", "name5"));

        saveBatch.add(Pair.of(testModel1, testModel1));
        saveBatch.add(Pair.of(testModel2, testModel2));
        db.updateBatch(saveBatch);
        db.shutdown();
    }

    public static void batchRead() {
        RocksDbSource db = openDB();
        Keyable.DefaultKeyable keyable = Keyable.ofDefault("hxx1".getBytes());
        TestModel testModel = (TestModel) db.get(TestModel.class, keyable);

        Keyable.DefaultKeyable keyable2 = Keyable.ofDefault("hxx2".getBytes());
        TestModel testModel2 = (TestModel) db.get(TestModel.class, keyable2);

        Keyable.DefaultKeyable keyable3 = Keyable.ofDefault("hxx3".getBytes());
        TestModel testModel3 = (TestModel) db.get(TestModel.class, keyable3);

        System.out.println(testModel);
        System.out.println(testModel2);
        System.out.println(testModel3);

        db.shutdown();

    }

    public static void readAll() {
        RocksDbSource db = openDB();
        List<TestModel> testModels = db.getAll(TestModel.class).stream().map(persistable -> (TestModel) persistable).collect(Collectors.toList());
        testModels.stream().forEach(System.out::println);
        db.shutdown();
    }

    public static RocksDbSource openDB() {
        Map<String, Class<? extends Persistable>> columnFamilies = new HashMap<>();
        columnFamilies.put("test_model", TestModel.class);
        RocksDbSource db = new RocksDbSource("state_b", columnFamilies, SystemConfig.getDefault());
        return db;
    }

}
