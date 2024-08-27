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
package com.thanos.chain.contract.eth.evm;

import com.thanos.common.utils.*;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPElement;
import com.thanos.common.utils.rlp.RLPItem;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.chain.storage.datasource.MemSizeEstimator;

import java.util.ArrayList;
import java.util.List;

import static com.thanos.common.utils.ByteUtil.toHexString;
import static com.thanos.chain.storage.datasource.MemSizeEstimator.ByteArrayEstimator;


/**
 * @author Roman Mandeleil
 * @since 19.11.2014
 */
public class LogInfo extends RLPModel {

    byte[] address;
    List<DataWord> topics;
    byte[] data;

    public LogInfo(byte[] encode) {

        super(encode);
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] addressEncoded = RLP.encodeElement(this.address);

        byte[][] topicsEncoded = null;
        if (topics != null) {
            topicsEncoded = new byte[topics.size()][];
            int i = 0;
            for (DataWord topic : topics) {
                byte[] topicData = topic.getData();
                topicsEncoded[i] = RLP.encodeElement(topicData);
                ++i;
            }
        }

        byte[] dataEncoded = RLP.encodeElement(data);
        return RLP.encodeList(addressEncoded, RLP.encodeList(topicsEncoded), dataEncoded);
    }

    @Override
    protected void rlpDecoded() {
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList logInfo = (RLPList) params.get(0);

        RLPItem address = (RLPItem) logInfo.get(0);
        RLPList topics = (RLPList) logInfo.get(1);
        RLPItem data = (RLPItem) logInfo.get(2);

        this.address = address.getRLPData() != null ? address.getRLPData() : new byte[]{};
        this.data = data.getRLPData() != null ? data.getRLPData() : new byte[]{};

        List<DataWord> topicList = new ArrayList<>(topics.size());

        for (RLPElement topic1 : topics) {
            byte[] topic = topic1.getRLPData();
            topicList.add(DataWord.of(topic));
        }
        this.topics = topicList;
    }

    public LogInfo(byte[] address, List<DataWord> topics, byte[] data) {
        super(null);
        this.address = (address != null) ? address : new byte[]{};
        this.topics = (topics != null) ? topics : new ArrayList<DataWord>();
        this.data = (data != null) ? data : new byte[]{};
        this.rlpEncoded = rlpEncoded();
    }

    public byte[] getAddress() {
        return address;
    }

    public List<DataWord> getTopics() {
        return topics;
    }

    public byte[] getData() {
        return data;
    }


    @Override
    public String toString() {

        StringBuilder topicsStr = new StringBuilder();
        topicsStr.append("[");

        for (DataWord topic : topics) {
            String topicStr = toHexString(topic.getData());
            topicsStr.append(topicStr).append(" ");
        }
        topicsStr.append("]");


        return "LogInfo{" +
                "address=" + toHexString(address) +
                ", topics=" + topicsStr +
                ", data=" + toHexString(data) +
                '}';
    }

    public static final MemSizeEstimator<LogInfo> MemEstimator = log ->
            ByteArrayEstimator.estimateSize(log.address) +
            ByteArrayEstimator.estimateSize(log.data) +
            log.topics.size() * DataWord.MEM_SIZE + 16;
}
