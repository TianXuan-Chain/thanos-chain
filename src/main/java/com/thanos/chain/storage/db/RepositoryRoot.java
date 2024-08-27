/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.thanos.chain.storage.db;

import com.thanos.common.utils.HashUtil;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.storage.datasource.*;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RepositoryRoot.java description：
 *
 * @Author laiyiyu create on 2020-08-24 14:06:27
 */
public class RepositoryRoot extends RepositoryImpl {

    private static final Logger logger = LoggerFactory.getLogger("ledger");

    private volatile byte[] stateRoot = ArrayUtils.EMPTY_BYTE_ARRAY;

    public RepositoryRoot(LedgerSource ledgerSource) {
        super(ledgerSource);
    }

    @Override
    public void commit() {
        //do nothing
    }

    @Override
    public void flush() {
        ledgerSource.updateAccountStates(this.addr2AccountWriteCache);
        ledgerSource.updateContractCodes(this.hash2CodeWriteCache);
        long start0 = System.currentTimeMillis();
        ledgerSource.updateContractStates(this.contractStateWriteCache);
        long start = System.currentTimeMillis();
        this.stateRoot = sha3LightStates(this.addr2AccountWriteCache, this.contractStateWriteCache, this.hash2CodeWriteCache);
        long end = System.currentTimeMillis();
        ledgerSource.cacheWriteBatch(this.contractStateWriteCache, this.addr2AccountWriteCache, this.hash2CodeWriteCache);
        long end1 = System.currentTimeMillis();

        this.addr2AccountWriteCache.clear();
        this.contractStateWriteCache.clear();
        this.hash2CodeWriteCache.clear();
        logger.debug(" all chain trace flush total cost[{}], update contract state:[{}], stateRoot cost:[{}], cacheWriteBatch cost:[{}]", (end1 - start0),(start - start0), (end - start), (end1 - end));
    }


    public void persist(Block block) {
        ledgerSource.doPersist(block);
    }

    @Override
    public byte[] getRoot() {
        return stateRoot;
    }

    @Override
    public Repository getSnapshotTo(LedgerSource ledgerSource) {
        return new RepositoryRoot(ledgerSource);
    }

    @Override
    public Repository clone() {
        return getSnapshotTo(ledgerSource);
    }
}
