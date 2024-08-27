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
package com.thanos.chain.ledger.model.eth;


import com.thanos.chain.ledger.model.BaseTransaction;
import com.thanos.chain.storage.datasource.MemSizeEstimator;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.common.utils.rlp.RLPUtil;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.thanos.chain.storage.datasource.MemSizeEstimator.ByteArrayEstimator;
import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static com.thanos.common.utils.ByteUtil.ZERO_BYTE_ARRAY;

/**
 * A transaction (formally, T) is a single cryptographically
 * signed instruction sent by an actor external to Ethereum.
 * An external actor can be a person (via a mobile device or desktop computer)
 * or could be from a piece of automated software running on a server.
 * There are two types of transactions: those which result in message calls
 * and those which result in the creation of new contracts.
 */
public class PoolableEthTransaction extends EthTransaction {



    public PoolableEthTransaction(byte[] rawData) {
        super(rawData);
    }

    public static void reset(byte[] publicKey, byte[] nonce, long futureEventNumber, byte[] gasPrice, byte[] gasLimit, byte[] receiveAddress, byte[] value, byte[] data, Set<ByteArrayWrapper> executeStates, byte[] signature, byte[] hash, byte[] rlpEncode) {

    }




    @Override
    protected byte[] rlpEncoded() {
        // parse null as 0 for nonce
        byte[] publicKey = RLP.encodeElement(this.publicKey);
        byte[] nonce = RLP.encodeElement(this.nonce);
        byte[] futureEventNumber = RLP.encodeBigInteger(BigInteger.valueOf(this.futureEventNumber));
        byte[] gasPrice = RLP.encodeElement(this.gasPrice);
        byte[] gasLimit = RLP.encodeElement(this.gasLimit);
        byte[] receiveAddress = RLP.encodeElement(this.receiveAddress);
        byte[] value = RLP.encodeElement(this.value);
        byte[] data = RLP.encodeElement(this.data);
        //byte[] executeState = RLP.encodeSet(this.executeStates);
        //this.executeStatesBytes = executeState;
        byte[] sign = RLP.encodeElement(this.signature);

        byte[] rlpEncoded = RLP.encodeList(publicKey, nonce, futureEventNumber, gasPrice, gasLimit,
                receiveAddress, value, data, this.executeStatesBytes, sign);

        return rlpEncoded;
    }

    @Override
    protected void rlpDecoded() {

        RLPList decodedTxList = RLP.decode2(rlpEncoded);
        RLPList transaction = (RLPList) decodedTxList.get(0);

        this.publicKey = transaction.get(0).getRLPData();
        this.nonce = transaction.get(1).getRLPData();
        this.futureEventNumber = ByteUtil.byteArrayToLong(transaction.get(2).getRLPData());
        this.gasPrice = transaction.get(3).getRLPData();
        this.gasLimit = transaction.get(4).getRLPData();
        this.receiveAddress = transaction.get(5).getRLPData();
        if (receiveAddress == null) receiveAddress = EMPTY_BYTE_ARRAY;
        this.value = transaction.get(6).getRLPData();
        this.data = transaction.get(7).getRLPData();

        this.executeStatesBytes = transaction.get(8).getRLPData();
        this.executeStates = RLPUtil.rlpDecodeSet(transaction.get(8));

        this.signature = transaction.get(9).getRLPData();

        calculateBase();
        this.hash = calculateHash();

    }









    public static void main(String[] args) {
        System.out.println(Hex.toHexString(ByteUtil.longToBytes(10000000000L)));
        byte[] longbytes = BigInteger.valueOf(10000).toByteArray();
        System.out.println(longbytes.length + "-" + Hex.toHexString(longbytes));


    }
}
