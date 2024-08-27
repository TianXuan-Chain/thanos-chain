package com.thanos.chain.executor;

import com.thanos.chain.config.SystemConfig;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.crypto.key.asymmetric.ec.ECKeyOld;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import com.thanos.chain.consensus.hotstuffbft.store.ConsensusChainStore;
import com.thanos.chain.ledger.StateLedger;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.eth.EthTransaction;
import com.thanos.chain.ledger.model.genesis.GenesisLoader;
import com.thanos.chain.storage.db.Repository;

import java.math.BigInteger;
import java.util.Random;
import java.util.Set;


/**
 * ExecutorTestBase.java description：
 *
 * @Author laiyiyu create on 2020-02-27 09:43:18
 */
public class ExecutorTestBase {

    static SystemConfig systemConfig = SystemConfig.getDefault();

    static ConsensusChainStore consensusChainStore = new ConsensusChainStore(systemConfig, false);

    public static StateLedger stateLedger = new StateLedger(systemConfig, null, consensusChainStore, true);

    public static int ADDRESS_COUNT = 500000;

    public static String BASE_ADDRESS = "0xca35b7d915458ef540ade6068dfe2f44e8";

    public static final String[] MOCK_ADDRESS = new String[ADDRESS_COUNT];

    static {

        for (int i = 0; i < ADDRESS_COUNT; i++) {
            MOCK_ADDRESS[i] =  BASE_ADDRESS + FILL_PREFIX_PADDING(i);
        }
        System.out.print("");
    }

    public static String FILL_PREFIX_PADDING(int i) {
        if (i < 10) {
            return "00000" + i;
        } else if (i >=10 && i < 100) {
            return "0000" + i;
        } else if (i >=100 && i < 1000) {
            return "000" + i;
        } else if (i >=1000 && i < 10000) {
            return "00" + i;
        } else if (i >=10000 && i < 100000) {
            return "0" + i;
        } else if (i >=100000 && i <= ADDRESS_COUNT) {
            return "" + i;
        } else {
            return "aaaaaa";
        }
    }




    //for test
    public static Block gensis =  GenesisLoader.loadGenesis(
            Thread.currentThread().getClass().getResourceAsStream("/genesis/genesis.json"));


    protected EthTransaction createTx(StateLedger stateLedger, SecureKey sender, byte[] receiveAddress, byte[] data) {
        return createTx(stateLedger, sender, receiveAddress, data, 10);
    }

    protected EthTransaction createTx(StateLedger stateLedger, SecureKey sender, byte[] receiveAddress,
                                      byte[] data, long value) {
        return createTx(stateLedger, sender, receiveAddress, data, value, null);
    }


    static Random random = new Random();
    protected EthTransaction createTx(StateLedger stateLedger, SecureKey sender, byte[] receiveAddress,
                                      byte[] data, long value, Set<ByteArrayWrapper> executeStates) {
        BigInteger nonce = stateLedger.rootRepository.getNonce(sender.getAddress());
        byte[] hash = HashUtil.sha3(ByteUtil.longToBytesNoLeadZeroes(System.currentTimeMillis() + random.nextLong()));
        EthTransaction tx = new EthTransaction(
                sender.getPubKey(),
                ByteUtil.bigIntegerToBytes(nonce),
                1,
                ByteUtil.longToBytesNoLeadZeroes(1),
                ByteUtil.longToBytesNoLeadZeroes(3_000_000),
                receiveAddress,
                ByteUtil.longToBytesNoLeadZeroes(value),
                data,
                executeStates,
                hash
        );
        //tx.signECDSA(sender);
        return tx;
    }

    protected EthTransaction createTxWithSpecfiyNonce(StateLedger stateLedger, ECKeyOld sender, byte[] receiveAddress,
                                                      byte[] data, long value, Set<ByteArrayWrapper> executeStates) {
        BigInteger nonce = new BigInteger("1");
        byte[] hash = HashUtil.sha3(ByteUtil.longToBytesNoLeadZeroes(System.currentTimeMillis() + random.nextLong()));
        EthTransaction tx = new EthTransaction(
                sender.getPubKey(),
                ByteUtil.bigIntegerToBytes(nonce),
                1,
                ByteUtil.longToBytesNoLeadZeroes(1),
                ByteUtil.longToBytesNoLeadZeroes(3_000_000),
                receiveAddress,
                ByteUtil.longToBytesNoLeadZeroes(value),
                data,
                executeStates,
                hash
        );
        //tx.signECDSA(sender);
        return tx;
    }

    protected EthTransactionExecutor executeTransaction(StateLedger stateLedger, EthTransaction tx) {
        Repository track = stateLedger.rootRepository.startTracking();
        EthTransactionExecutor executor = new EthTransactionExecutor(tx, track, stateLedger.programInvokeFactory, gensis).withConfig(stateLedger.systemConfig);

        executor.init();
        executor.execute();
        executor.go();
        //executor.finalization();

        //track.commit();
        return executor;
    }

}
