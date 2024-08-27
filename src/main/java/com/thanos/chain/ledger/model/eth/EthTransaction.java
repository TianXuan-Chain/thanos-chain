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
import com.thanos.common.utils.*;
import com.thanos.common.utils.rlp.*;
import com.thanos.chain.storage.datasource.MemSizeEstimator;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.thanos.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static com.thanos.common.utils.ByteUtil.ZERO_BYTE_ARRAY;
import static com.thanos.chain.storage.datasource.MemSizeEstimator.ByteArrayEstimator;

/**
 * A transaction (formally, T) is a single cryptographically
 * signed instruction sent by an actor external to Ethereum.
 * An external actor can be a person (via a mobile device or desktop computer)
 * or could be from a piece of automated software running on a server.
 * There are two types of transactions: those which result in message calls
 * and those which result in the creation of new contracts.
 */
public class EthTransaction extends BaseTransaction {

    private static final BigInteger DEFAULT_GAS_LIMIT = new BigInteger("21000");

    private static final BigInteger DEFAULT_GAS_PRICE = new BigInteger("10000000000000");

    public static final int HASH_LENGTH = 32;

    public static final int ADDRESS_LENGTH = 20;


    /* the amount of ether to transfer (calculated as wei) */
    protected byte[] value;

    /* the address of the destination account
     * In creation transaction the receive address is - 0 */
    protected byte[] receiveAddress;

    /* the amount of ether to pay as a transaction fee
     * to the miner for each unit of gas */
    protected byte[] gasPrice;

    /* the amount of "gas" to allow for the computation.
     * Gas is the fuel of the computational engine;
     * every computational step taken and every byte added
     * to the state or transaction list consumes some gas. */
    protected byte[] gasLimit;

    /* An unlimited size byte array specifying
     * input [data] of the message call or
     * Initialization code for a new contract */
    protected byte[] data;

    protected Set<ByteArrayWrapper> executeStates;
    protected byte[] executeStatesBytes;

    protected volatile EthTransactionReceipt ethTransactionReceipt;


    //for test
    public EthTransaction(Set<ByteArrayWrapper> executeStates) {
        super(null);
        this.executeStates = executeStates;
        this.executeStatesBytes = RLP.encodeSet(this.executeStates);
    }

    public EthTransaction(byte[] rawData) {
        super(rawData);
    }

    public EthTransaction(byte[] publicKey, byte[] nonce, long futureEventNumber, byte[] gasPrice, byte[] gasLimit, byte[] receiveAddress, byte[] value, byte[] data, Set<ByteArrayWrapper> executeStates, byte[] signature) {
        super(publicKey, futureEventNumber, nonce, signature);
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.receiveAddress = receiveAddress;
        this.value = value;
        this.data = data;
        this.executeStates = executeStates == null? new HashSet<>() : executeStates;
        this.executeStatesBytes = RLP.encodeSet(this.executeStates);
        //this.chainId = chainId;

        if (receiveAddress == null) {
            this.receiveAddress = EMPTY_BYTE_ARRAY;
        }

        this.hash = calculateHash();
        this.rlpEncoded = rlpEncoded();
    }

    //for speed up, receive from self gateway, and trust the signature
    public EthTransaction(byte[] publicKey, byte[] nonce, long futureEventNumber, byte[] gasPrice, byte[] gasLimit, byte[] receiveAddress, byte[] value, byte[] data, Set<ByteArrayWrapper> executeStates, byte[] signature, byte[] hash, byte[] rlpEncode) {
        super(publicKey, futureEventNumber, nonce, signature);
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.receiveAddress = receiveAddress;
        this.value = value;
        this.data = data;
        this.executeStates = executeStates == null? new HashSet<>() : executeStates;
        this.executeStatesBytes = RLP.encodeSet(this.executeStates);
        this.hash = hash;
        //this.chainId = chainId;
        this.valid = true;

        if (receiveAddress == null) {
            this.receiveAddress = EMPTY_BYTE_ARRAY;
        }

        this.rlpEncoded = rlpEncode;
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



    @Override
    protected boolean baseVerify() {
        if (getNonce().length > HASH_LENGTH) throw new RuntimeException("Nonce is not valid");
        if (receiveAddress != null && receiveAddress.length != 0 && receiveAddress.length != ADDRESS_LENGTH)
            return false;
        if (gasLimit.length > HASH_LENGTH)
            return false;
        if (gasPrice != null && gasPrice.length > HASH_LENGTH)
            return false;
        if (value != null  && value.length > HASH_LENGTH)
            return false;

        return true;
    }

    public byte[] getHash() {
        return hash;
    }

    @Override
    protected byte[] calculateHash() {
        return HashUtil.sha3Dynamic(this.publicKey, this.nonce, BigIntegers.asUnsignedByteArray(BigInteger.valueOf(this.futureEventNumber)), this.gasPrice, this.gasLimit, this.receiveAddress, this.value, this.data, this.executeStatesBytes);
    }

    public byte[] getValue() {
        return value == null ? ZERO_BYTE_ARRAY : value;
    }

    public byte[] getReceiveAddress() {
        return receiveAddress;
    }

    public byte[] getGasPrice() {
        return gasPrice == null ? ZERO_BYTE_ARRAY : gasPrice;
    }

    public byte[] getGasLimit() {
        return gasLimit == null ? ZERO_BYTE_ARRAY : gasLimit;
    }

    public EthTransactionReceipt getEthTransactionReceipt() {
        return ethTransactionReceipt;
    }

    public void setEthTransactionReceipt(EthTransactionReceipt ethTransactionReceipt) {
        this.ethTransactionReceipt = ethTransactionReceipt;
    }

    public void setErrEthTransactionReceipt(String err) {
        this.ethTransactionReceipt = new EthTransactionReceipt(null, this, 0, EMPTY_BYTE_ARRAY, err);
    }

    public byte[] getData() {
        return data;
    }

    public Set<ByteArrayWrapper> getExecuteStates() {
        return executeStates;
    }

    public byte[] getContractAddress() {
        if (isContractCreation()) return null;
        return receiveAddress;
    }

    public boolean isContractCreation() {
        return this.receiveAddress == null || Arrays.equals(this.receiveAddress, EMPTY_BYTE_ARRAY);
    }

    public synchronized byte[] getSender() {

        return sendAddress;
    }


    @Override
    public String toString() {
        return "EthTransaction{" +
                "value=" + Hex.toHexString(value) +
                ", receiveAddress=" + Hex.toHexString(receiveAddress) +
                ", gasPrice=" + Hex.toHexString(gasPrice) +
                ", gasLimit=" + Hex.toHexString(gasLimit) +
                ", executeStates=" + executeStates +
                ", ethTransactionReceipt=" + ethTransactionReceipt +
                ", publicKey=" + Hex.toHexString(publicKey) +
                ", nonce=" + Hex.toHexString(nonce) +
                ", futureEventNumber=" + futureEventNumber +
                ", signature=" + Hex.toHexString(signature) +
                ", hash=" + Hex.toHexString(hash) +
                '}';
    }

    @Override
    public int hashCode() {

        byte[] hash = this.getHash();
        int hashCode = 0;

        for (int i = 0; i < hash.length; ++i) {
            hashCode += hash[i] * i;
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof EthTransaction)) return false;
        EthTransaction tx = (EthTransaction) obj;

        return tx.hashCode() == this.hashCode();
    }

    public static final MemSizeEstimator<EthTransaction> MemEstimator = tx ->
            ByteArrayEstimator.estimateSize(tx.hash) +
                    ByteArrayEstimator.estimateSize(tx.nonce) +
                    ByteArrayEstimator.estimateSize(tx.value) +
                    ByteArrayEstimator.estimateSize(tx.gasPrice) +
                    ByteArrayEstimator.estimateSize(tx.gasLimit) +
                    ByteArrayEstimator.estimateSize(tx.data) +
                    ByteArrayEstimator.estimateSize(tx.sendAddress) +
                    ByteArrayEstimator.estimateSize(tx.rlpEncoded) +
                    16; // Object header + ref


    public static void main(String[] args) {
        System.out.println(Hex.toHexString(ByteUtil.longToBytes(10000000000L)));
        byte[] longbytes = BigInteger.valueOf(10000).toByteArray();
        System.out.println(longbytes.length + "-" + Hex.toHexString(longbytes));


    }
}
