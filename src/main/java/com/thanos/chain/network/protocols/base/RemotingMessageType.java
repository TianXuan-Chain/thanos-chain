/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thanos.chain.network.protocols.base;

import java.util.HashMap;
import java.util.Map;

public enum RemotingMessageType {

    REQUEST_MESSAGE((byte)0),

    RESPONSE_MESSAGE((byte)1);

    private byte type;

    private static final Map<Byte, RemotingMessageType> byteToTypeMap = new HashMap<>();

    static {
        for (RemotingMessageType remotingMessageType : RemotingMessageType.values()) {
            byteToTypeMap.put(remotingMessageType.type, remotingMessageType);
        }
    }

    RemotingMessageType(byte type) {
        this.type = type;
    }


    public static RemotingMessageType fromByte(byte type) {
        return byteToTypeMap.get(type);
    }

    public byte getType() {
        return type;
    }
}
