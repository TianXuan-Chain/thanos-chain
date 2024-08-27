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
package com.thanos.chain.storage.datasource;

/**
 * Defines configurable database settings
 *
 * @author Mikhail Kalinin
 * @since 26.04.2018
 */
public class DbSettings {

    public static final DbSettings DEFAULT = new DbSettings()
            .withMaxThreads(8)
            .withMaxOpenFiles(10000)
            .withWriteBufferSize(64)
            .withBloomFilterFlag(false);

    // defines a number of opened files by db instance
    // this number has significant impact on read amplification
    // on the other hand it can force exceeding of user's limit,
    // OS usually set it to 1024 for all applications
    int maxOpenFiles;
    int maxThreads;
    int writeBufferSize; //M
    boolean bloomFilterFlag;

    private DbSettings() {
    }

    public static DbSettings newInstance() {
        DbSettings settings = new DbSettings();
        settings.maxOpenFiles = DEFAULT.maxOpenFiles;
        settings.maxThreads = DEFAULT.maxThreads;
        settings.writeBufferSize = DEFAULT.writeBufferSize;
        settings.bloomFilterFlag = DEFAULT.bloomFilterFlag;
        return settings;
    }

    public int getMaxOpenFiles() {
        return maxOpenFiles;
    }

    public DbSettings withMaxOpenFiles(int maxOpenFiles) {
        this.maxOpenFiles = maxOpenFiles;
        return this;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    public DbSettings withMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    public boolean isBloomFilterFlag() {
        return bloomFilterFlag;
    }

    public DbSettings withWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
        return this;
    }

    public DbSettings withBloomFilterFlag(boolean bloomFilterFlag) {
        this.bloomFilterFlag = bloomFilterFlag;
        return this;
    }
}
