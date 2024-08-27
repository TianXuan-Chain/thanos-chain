package com.thanos.chain.storage.datasource.rocksdb;

import com.thanos.chain.config.SystemConfig;
import org.rocksdb.WriteBatch;

/**
 * 类WriteBatchFactory.java的实现描述：
 *
 * @author xuhao create on 2020/11/26 11:11
 */

public class WriteBatchFactory {

    private SystemConfig systemConfig;


    public WriteBatchFactory(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }

    public WriteBatch getInstance() {
        if (!systemConfig.dataNeedEncrypt()) {
            return new WriteBatch();
        } else {
            return new EncryptWriteBatch(systemConfig.getCipherKey());
        }
    }
}
