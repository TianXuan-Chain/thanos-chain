package com.thanos.chain.consensus.hotstuffbft.safety;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanos.chain.consensus.hotstuffbft.model.LedgerInfo;
import com.thanos.chain.consensus.hotstuffbft.model.LedgerInfoWithSignatures;
import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * Waypoint.java description：
 *
 * @Author laiyiyu create on 2020-06-10 16:54:56
 */
public class Waypoint implements Verifier {
    // The number of the reconfiguration event that is being approved by this waypoint.
    long number;
    // The hash of the chosen fields of LedgerInfo.
    byte[] value;

    public static Waypoint build(LedgerInfo ledgerInfo) {
        Waypoint waypoint = new Waypoint();
        waypoint.number = ledgerInfo.getNumber();
        waypoint.value = ledgerInfo.getConsensusDataHash();
        return waypoint;
    }

    public static Waypoint build(String waypoint) {
        try {

            if (StringUtils.isEmpty(waypoint)) {
                return new Waypoint();
            }

            ObjectMapper mapper = new ObjectMapper()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
            return mapper.readValue(waypoint, Waypoint.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Waypoint() {
        this.number = 0;
        this.value = new byte[0];
    }

    public String toJSONString() {
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }



    public long getNumber() {
        return number;
    }

    public byte[] getValue() {
        return value;
    }

    public ProcessResult<Void> verify(LedgerInfo ledgerInfo) {

        if (ledgerInfo.getNumber() != this.number) {
            return ProcessResult.ofError(String.format("Waypoint version mismatch: waypoint version = %d, given version = %d", this.number, ledgerInfo.getNumber()));
        }

        if (!Arrays.equals(ledgerInfo.getConsensusDataHash(), this.value)) {
            return ProcessResult.ofError(String.format("Waypoint value mismatch: waypoint value = %s, given value = %s", Hex.toHexString(this.value), Hex.toHexString(ledgerInfo.getConsensusDataHash())));
        }

        return ProcessResult.ofSuccess();
    }

    @Override
    public ProcessResult<Void> verify(LedgerInfoWithSignatures ledgerInfoWithSignatures) {
        return verify(ledgerInfoWithSignatures.getLedgerInfo());
    }

    @Override
    public boolean epochChangeVerificationRequired(long epoch) {
        return true;
    }

    @Override
    public boolean isLedgerInfoStale(LedgerInfo ledgerInfo) {
        return ledgerInfo.getNumber() < this.number;
    }
}
