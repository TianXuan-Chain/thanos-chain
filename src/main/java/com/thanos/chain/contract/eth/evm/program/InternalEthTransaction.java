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
package com.thanos.chain.contract.eth.evm.program;

import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.rlp.RLPUtil;
import com.thanos.chain.contract.eth.evm.DataWord;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;

import static com.thanos.common.utils.ByteUtil.toHexString;
import static org.apache.commons.lang3.ArrayUtils.*;

public class InternalEthTransaction extends EthTransaction {

    private byte[] parentHash;
    private int deep;
    private int index;
    private boolean rejected = false;
    private String note;

    public InternalEthTransaction(byte[] rawData) {
        super(rawData);
    }

    public InternalEthTransaction(byte[] parentHash, int deep, int index, byte[] nonce, DataWord gasPrice, DataWord gasLimit,
                                  byte[] sendAddress, byte[] receiveAddress, byte[] value, byte[] data, Set<ByteArrayWrapper> executeStates, String note) {

        super((byte[])null);
        this.nonce = nonce;
        this.gasPrice = getData(gasPrice);
        this.gasLimit = getData(gasLimit);
        this.receiveAddress = receiveAddress;
        this.value = nullToEmpty(value);
        this.data = nullToEmpty(data);
        this.parentHash = parentHash;
        this.deep = deep;
        this.index = index;
        this.sendAddress = nullToEmpty(sendAddress);
        this.executeStates = executeStates == null? new HashSet<>() : executeStates;
        this.note = note;
        this.parsed = true;
        this.rlpEncoded = getEncoded();
    }

    private static byte[] getData(DataWord gasPrice) {
        return (gasPrice == null) ? ByteUtil.EMPTY_BYTE_ARRAY : gasPrice.getData();
    }


    public int getDeep() {
        return deep;
    }

    public int getIndex() {
        return index;
    }


    public String getNote() {
        return note;
    }

    @Override
    public byte[] getSender() {
        return sendAddress;
    }

    public byte[] getParentHash() {
        return parentHash;
    }


    @Override
    protected byte[] rlpEncoded() {
        byte[] nonce = getNonce();
        boolean isEmptyNonce = isEmpty(nonce) || (getLength(nonce) == 1 && nonce[0] == 0);

        byte[] rlpEncoded = RLP.encodeList(
                RLP.encodeElement(isEmptyNonce ? null : nonce),
                RLP.encodeElement(this.parentHash),
                RLP.encodeElement(sendAddress),
                RLP.encodeElement(receiveAddress),
                RLP.encodeElement(value),
                RLP.encodeElement(gasPrice),
                RLP.encodeElement(gasLimit),
                RLP.encodeElement(data),
                RLP.encodeSet(this.executeStates),
                RLP.encodeString(this.note),
                encodeInt(this.deep),
                encodeInt(this.index),
                encodeInt(this.rejected ? 1 : 0)
        );

        return rlpEncoded;
    }

    @Override
    protected void rlpDecoded() {
        RLPList decodedTxList = RLP.decode2(rlpEncoded);
        RLPList transaction = (RLPList) decodedTxList.get(0);

        this.nonce = transaction.get(0).getRLPData();
        this.parentHash = transaction.get(1).getRLPData();
        this.sendAddress = transaction.get(2).getRLPData();
        this.receiveAddress = transaction.get(3).getRLPData();
        this.value = transaction.get(4).getRLPData();
        this.gasPrice = transaction.get(5).getRLPData();
        this.gasLimit = transaction.get(6).getRLPData();
        this.data = transaction.get(7).getRLPData();
        this.executeStates = RLPUtil.rlpDecodeSet(transaction.get(8));
        this.note = new String(transaction.get(9).getRLPData());
        this.deep = decodeInt(transaction.get(10).getRLPData());
        this.index = decodeInt(transaction.get(11).getRLPData());
        this.rejected = decodeInt(transaction.get(12).getRLPData()) == 1;
    }


    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
    }

    private static int bytesToInt(byte[] bytes) {
        return isEmpty(bytes) ? 0 : ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static byte[] encodeInt(int value) {
        return RLP.encodeElement(intToBytes(value));
    }

    private static int decodeInt(byte[] encoded) {
        return bytesToInt(encoded);
    }


    @Override
    public String toString() {
        return "TransactionData [" +
                "  parentHash=" + toHexString(getParentHash()) +
                ", getHash=" + toHexString(getHash()) +
                ", nonce=" + toHexString(getNonce()) +
                ", gasPrice=" + toHexString(getGasPrice()) +
                ", gas=" + toHexString(getGasLimit()) +
                ", sendAddress=" + toHexString(getSender()) +
                ", receiveAddress=" + toHexString(getReceiveAddress()) +
                ", value=" + toHexString(getValue()) +
                ", data=" + toHexString(getData()) +
                ", note=" + getNote() +
                ", deep=" + getDeep() +
                ", index=" + getIndex() +
                "]";
    }
}
