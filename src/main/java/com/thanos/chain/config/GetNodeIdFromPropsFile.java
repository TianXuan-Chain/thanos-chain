/*
 * Copyright (c) [2017] [ <ether.camp> ]
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
package com.thanos.chain.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Strategy to generate the getCaHash and the nodePrivateKey from a getRemoteNodeId.properties file.
 * <p>
 *
 * @author Lucas Saldanha
 * @since 14.12.2017
 */
public class GetNodeIdFromPropsFile implements GenerateNodeIdStrategy {

    private String databaseDir;

    GetNodeIdFromPropsFile(String databaseDir) {
        this.databaseDir = databaseDir;
    }

    @Override
    public Map<String, String> getCaNodeInfoMap() {
        Properties props = new Properties();
        File file = new File(databaseDir, "nodeInfo.properties");
        if (file.canRead()) {
            try (Reader r = new FileReader(file)) {
              props.load(r);
              Map<String, String> result = new HashMap(props);
              return  result;
            } catch (IOException e) {
              throw new RuntimeException("Error reading 'nodeInfo.properties' file", e);
            }
        } else {
            throw new RuntimeException("Can't read 'nodeInfo.properties'" + " and current path:" + databaseDir);
        }
    }
}
