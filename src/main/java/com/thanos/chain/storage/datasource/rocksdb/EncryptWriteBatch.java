package com.thanos.chain.storage.datasource.rocksdb;

import com.thanos.common.crypto.key.symmetric.CipherKey;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;

/**
 * 类EncryptWriteBatch.java的实现描述：
 *
 * @author xuhao create on 2020/11/26 11:08
 */

public class EncryptWriteBatch extends WriteBatch {

    private CipherKey cipherKey;

    public EncryptWriteBatch(CipherKey cipherKey) {
        this.cipherKey = cipherKey;
    }

    @Override
    public void put(ColumnFamilyHandle columnFamilyHandle, byte[] key,
                    byte[] value) throws RocksDBException {
        byte[] encryptedKey = cipherKey.encrypt(key);
        byte[] encryptedValue = cipherKey.encrypt(value);
        super.put(columnFamilyHandle, encryptedKey, encryptedValue);
    }

    @Override
    public void delete(ColumnFamilyHandle columnFamilyHandle, byte[] key)
            throws RocksDBException {
        byte[] encryptedKey = cipherKey.encrypt(key);
        super.delete(columnFamilyHandle, encryptedKey);
    }

}
