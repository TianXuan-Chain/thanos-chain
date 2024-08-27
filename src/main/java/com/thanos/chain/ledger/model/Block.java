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
package com.thanos.chain.ledger.model;

import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.chain.ledger.model.event.GlobalEvent;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.executor.dag.ExecuteRoot;
import com.thanos.chain.ledger.model.store.Persistable;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;

/**
 *
 */
public class Block extends Persistable {

    private static final Logger logger = LoggerFactory.getLogger("ledger");


    //event id, after execute transaction ,it become include receipts root hash
    private byte[] hash;

    private byte[] eventId;

    /* The SHA3 256-bit getHash of the parent event, only read for vm */
    private byte[] preEventId;
    /* The 160-bit address to which all fees collected from the
     * successful mining of this block be transferred; formally */
    private byte[] coinbase;
    /* The SHA3 256-bit getHash of the root node of the state trie,
     * after all transactions are executed and finalisations applied */
    private byte[] stateRoot;
    /* The SHA3 256-bit getHash of the root node of the trie structure
     * populated with each transaction recipe in the transaction recipes
     * list */
    private byte[] receiptsRoot;


    private long epoch;

    private long number;

    /* A scalar value equal to the reasonable output of Unix's time()
     * at this block's inception */
    private long timestamp;

    //pk to signECDSA
    private BlockSign blockSign;
    //private Map<ByteArrayWrapper, Signature> signatures;


    private List<EthTransactionReceipt> receipts;


    private GlobalEvent globalEvent;


    //Transient
    /* Transactions */
    private EthTransaction[] transactionsList;

    //for speed
    /* Transactions */
    private List<ExecuteRoot> dagExecuteRoots;


    public Block(byte[] rawData) {
        super(rawData);
    }

    public Block(byte[] eventId, byte[] preEventId, byte[] coinbase, long epoch, long number,
                 long timestamp,
                 byte[] stateRoot, byte[] receiptsRoot,
                 EthTransaction[] transactionsList) {

        this(eventId, preEventId, coinbase, epoch, number, timestamp, stateRoot, receiptsRoot, new GlobalEvent(), transactionsList);
    }

    public Block(byte[] eventId, byte[] preEventId, byte[] coinbase, long epoch, long number,
                 long timestamp,
                 byte[] stateRoot, byte[] receiptsRoot, GlobalEvent globalEvent,
                 EthTransaction[] transactionsList) {
        super(null);
        this.eventId = eventId;
        this.preEventId = preEventId;
        this.coinbase = coinbase;
        this.stateRoot = stateRoot;
        this.receiptsRoot = receiptsRoot;
        this.epoch = epoch;
        this.number = number;
        this.timestamp = timestamp;
        this.globalEvent = globalEvent;
        this.transactionsList = transactionsList;
        this.receipts = new ArrayList<>();
        this.hash = HashUtil.sha3Dynamic(eventId, preEventId, coinbase, stateRoot, receiptsRoot, ByteUtil.longToBytes(epoch), ByteUtil.longToBytes(number), ByteUtil.longToBytes(timestamp));
        this.rlpEncoded = rlpEncoded();
    }

    public byte[] getHash() {
        return hash;
    }

    public byte[] getEventId() {
        return hash;
    }

    public byte[] getPreEventId() {
        return preEventId;
    }



    public byte[] getCoinbase() {
        return coinbase;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }



    public byte[] getReceiptsRoot() {
        return receiptsRoot;
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
    }

    public void setReceiptsRoot(byte[] receiptsRoot) {
        this.receiptsRoot = receiptsRoot;
    }

    public long getEpoch() {
        return epoch;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public long getNumber() {
        return number;
    }

    public GlobalEvent getGlobalEvent() {
        return globalEvent;
    }

    public EthTransaction[] getTransactionsList() {
        return transactionsList;
    }

    public void setTransactionsList(EthTransaction[] transactionsList) {
        this.transactionsList = transactionsList;
    }

    public List<EthTransactionReceipt> getReceipts() {
        return receipts;
    }

    public void setReceipts(List<EthTransactionReceipt> receipts) {
        this.receipts = receipts;
    }

    public List<ExecuteRoot> getDagExecuteRoots() {
        return dagExecuteRoots;
    }

    public void setDagExecuteRoots(List<ExecuteRoot> dagExecuteRoots) {
        this.dagExecuteRoots = dagExecuteRoots;
    }


    public void recordSign(BlockSign blockSign) {
        this.blockSign = blockSign;
    }

    public BlockSign getBlockSign() {
        return blockSign;
    }


    public void reHash() {
        this.hash = HashUtil.sha3Dynamic(eventId, preEventId, coinbase, stateRoot, receiptsRoot, ByteUtil.longToBytes(epoch), ByteUtil.longToBytes(number), ByteUtil.longToBytes(timestamp));
    }

    public void reEncoded() {
        super.rlpEncoded = rlpEncoded();
    }

    //    public boolean isEqual(Block block) {
//        return Arrays.areEqual(this.getHash(), block.getHash());
//    }

    protected byte[] rlpEncoded() {

        int receiptsSize;
        if (CollectionUtils.isEmpty(receipts)) {
            receiptsSize = 0;
        } else {
            receiptsSize = receipts.size();
        }


        byte[][] encode = new byte[9 + 1 + receiptsSize][];
        encode[0] = RLP.encodeElement(this.eventId);
        encode[1] = RLP.encodeElement(this.preEventId);
        encode[2] = RLP.encodeElement(this.coinbase);
        encode[3] = RLP.encodeElement(this.stateRoot);
        encode[4] = RLP.encodeElement(this.receiptsRoot);
        encode[5] = RLP.encodeBigInteger(BigInteger.valueOf(this.epoch));
        encode[6] = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        encode[7] = RLP.encodeBigInteger(BigInteger.valueOf(this.timestamp));
        encode[8] = this.globalEvent.getEncoded();
        encode[9] = RLP.encodeInt(receiptsSize);

        for (int i = 0; i < receiptsSize; i++) {
            encode[i + 10] = receipts.get(i).getEncoded();
        }



//        int i = 9 + 1 + receiptsSize;
//        for (Map.Entry<ByteArrayWrapper, Signature> entry: signatures.entrySet()) {
//            encode[i] =
//                    RLP.encodeList(
//                            RLP.encodeElement(entry.getKey().getData()),
//                            RLP.encodeElement(entry.getValue().getSig())
//                    );
//            i++;
//        }
        return RLP.encodeList(encode);

    }

    protected void rlpDecoded() {
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList block = (RLPList) params.get(0);
        this.eventId = block.get(0).getRLPData();
        this.preEventId = block.get(1).getRLPData();
        this.coinbase = block.get(2).getRLPData() == null?ByteUtil.ZERO_BYTE_ARRAY: block.get(2).getRLPData();
        this.stateRoot = block.get(3).getRLPData();
        this.receiptsRoot = block.get(4).getRLPData();
        this.epoch = ByteUtil.byteArrayToLong(block.get(5).getRLPData());
        this.number = ByteUtil.byteArrayToLong(block.get(6).getRLPData());
        this.timestamp = ByteUtil.byteArrayToLong(block.get(7).getRLPData());
        this.globalEvent = new GlobalEvent(block.get(8).getRLPData());

        this.hash = HashUtil.sha3Dynamic(eventId, preEventId, coinbase, stateRoot, receiptsRoot, ByteUtil.longToBytes(epoch), ByteUtil.longToBytes(number), ByteUtil.longToBytes(timestamp));

        int receiptsSize = ByteUtil.byteArrayToInt(block.get(9).getRLPData());
        List<EthTransactionReceipt> receipts = new ArrayList<>(receiptsSize);
        //logger.info(" indexer receipts[{}] ", receiptsSize);

        int receiptEnd = receiptsSize + 10;
        for (int i = 10; i < receiptEnd; i++) {
            receipts.add(new EthTransactionReceipt(block.get(i).getRLPData()));
        }
        this.receipts = receipts;

        //logger.info(" indexer after receipts[{}] ", receiptsSize);

        //int i = 8 + 1 + receiptsSize;
//        TreeMap<ByteArrayWrapper, Signature> signatures = new TreeMap<>();
//        for (int i = 10 + receiptsSize; i < block.size(); i++) {
//            RLPList kvBytes = (RLPList) RLP.decode2(block.get(i).getRLPData()).get(0);
//            signatures.put(new ByteArrayWrapper(kvBytes.get(0).getRLPData()), new Signature(kvBytes.get(1).getRLPData()));
//        }
//        this.signatures = signatures;
    }



    @Override
    public String toString() {
        return "Block{" +
                "hash=" + Hex.toHexString(hash) +
                ", eventId=" + Hex.toHexString(eventId) +
                ", preEventId=" + Hex.toHexString(preEventId) +
                ", stateRoot=" + Hex.toHexString(stateRoot) +
                ", receiptRoot=" + Hex.toHexString(receiptsRoot) +
                ", epoch=" + epoch +
                ", number=" + number +
                ", timestamp=" + timestamp +
                ", coinbase=" + Hex.toHexString(coinbase) +
                ", globalEvent=" + this.globalEvent +
                ", receipts size=" + receipts.size() +
                '}';
    }

    public static void main(String[] args) {


    }
}
