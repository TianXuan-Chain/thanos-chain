package com.thanos.chain.consensus.hotstuffbft.safety;

import com.thanos.chain.consensus.hotstuffbft.model.LedgerInfo;
import com.thanos.chain.consensus.hotstuffbft.model.LedgerInfoWithSignatures;
import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;

/**
 * Verifier.java description：
 *
 * @Author laiyiyu create on 2020-06-11 16:38:22
 */
public interface Verifier {

    ProcessResult<Void> verify(LedgerInfoWithSignatures ledgerInfo);

    boolean epochChangeVerificationRequired(long epoch);

    boolean isLedgerInfoStale(LedgerInfo ledgerInfo);

}
