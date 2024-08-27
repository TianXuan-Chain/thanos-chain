package com.thanos.chain.executor;

import com.thanos.chain.ledger.model.eth.EthTransactionReceipt;
import com.thanos.common.utils.HashUtil;
import com.thanos.common.utils.ThanosThreadFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.thanos.common.utils.HashUtil.CRYPTO_PROVIDER;
import static com.thanos.common.utils.HashUtil.HASH_256_LIGHT_ALGORITHM_NAME;

/**
 * ExecutorUtil.java description：
 *
 * @Author laiyiyu create on 2021-01-07 16:41:02
 */
public class ExecutorUtil {

    private final static int PROCESSOR_NUM = 4;

    private final static int LIGHT_HASH_LENGTH = 32;

    private final static int ABSORB_LENGTH = PROCESSOR_NUM * LIGHT_HASH_LENGTH;

    private final static int PARALLEL_THRESHOLD = 10000;

    private static ThreadPoolExecutor txExecutor = new ThreadPoolExecutor(PROCESSOR_NUM, PROCESSOR_NUM, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), new ThanosThreadFactory("executor_util_process"));

    public static byte[] calculate(List<EthTransactionReceipt> receipts) {
        if (receipts.size() > PARALLEL_THRESHOLD) {
            return calculateReceiptRoot(receipts);
        } else {
            return sha3LightReceipts(receipts);
        }
    }

    private static byte[] calculateReceiptRoot(List<EthTransactionReceipt> receipts) {

        final byte[] absorb = new byte[ABSORB_LENGTH];
        try {
            int batchSize = receipts.size() / PROCESSOR_NUM;
            int remainder = receipts.size() % PROCESSOR_NUM;
            CountDownLatch await = new CountDownLatch(PROCESSOR_NUM);
            for (int count = 0; count < PROCESSOR_NUM; count++) {
                final int pos = count;
                final int startPos = count * (batchSize);
                int endPosition = (count + 1) * batchSize - 1;

                if (count == PROCESSOR_NUM - 1) {
                    endPosition += remainder;
                }

                final int endPos = endPosition;

                txExecutor.execute(() -> {
                    byte[] hashContent = sha3LightReceipts(receipts, startPos, endPos);
                    System.arraycopy(hashContent, 0, absorb, pos * LIGHT_HASH_LENGTH, hashContent.length);
                    await.countDown();
                });
            }

            await.await();
        } catch (Throwable e) {
        }
        return HashUtil.sha3Light(absorb);
    }

    // receipts root
    public static byte[] sha3LightReceipts(List<EthTransactionReceipt> receipts) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_LIGHT_ALGORITHM_NAME, CRYPTO_PROVIDER);
            for (EthTransactionReceipt receipt : receipts) {
                //absorb
                digest.update(receipt.getHashContent());
            }

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha3LightReceipts(List<EthTransactionReceipt> receipts, int start, int end) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_LIGHT_ALGORITHM_NAME, CRYPTO_PROVIDER);
            for (int i = start; i <= end; i++) {
                //absorb
                digest.update(receipts.get(i).getHashContent());
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
