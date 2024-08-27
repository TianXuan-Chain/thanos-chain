package com.thanos.chain.ledger.model;

import com.thanos.chain.ledger.model.event.GlobalNodeEvent;
import com.thanos.common.crypto.VerifyingKey;
import com.thanos.common.crypto.key.asymmetric.SecurePublicKey;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.consensus.hotstuffbft.model.VerifyResult;
import com.thanos.chain.ledger.model.crypto.Signature;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/**
 * 类ValidatorPublicKeyInfo.java的实现描述：
 *
 * @Author laiyiyu create on 2019-12-10 14:09:55
 */
public class ValidatorPublicKeyInfo extends RLPModel {
    // The validator's account address. AccountAddresses are initially derived from the account
    // auth pubkey; however, the auth key can be rotated, so one should not rely on this
    // initial property.
    private byte[] accountAddress;
    // Voting power of this validator
    long consensusVotingPower;

    int shardingNum;
    // This key can validate messages sent from this validator, can convert to accountAddress
    VerifyingKey consensusPublicKey;

    String name;

    String agency;

    String caHash;

    public static ValidatorPublicKeyInfo convertFrom(GlobalNodeEvent globalNodeEvent) {
//        byte[] pk = ByteUtil.copyFrom(globalNodeEvent.getId());
//
//
//        ValidatorPublicKeyInfo validatorPublicKeyInfo = new ValidatorPublicKeyInfo(
//                pk,
//                globalNodeEvent.getConsensusVotingPower(),
//                globalNodeEvent.getShardingNum(),
//                new VerifyingKey(pk),
//                new String(globalNodeEvent.getName().getBytes()),
//                new String(globalNodeEvent.getAgency().getBytes()),
//                new String(globalNodeEvent.getCaHash().getBytes())
//        );


        return null;
    }

    public ValidatorPublicKeyInfo(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public ValidatorPublicKeyInfo() {
        super(null);
    }

    public ValidatorPublicKeyInfo(byte[] accountAddress, long consensusVotingPower, int shardingNum, VerifyingKey consensusPublicKey, String name, String agency, String caHash) {
        super(null);
        this.accountAddress = accountAddress;
        this.consensusVotingPower = consensusVotingPower;
        this.shardingNum = shardingNum;
        this.consensusPublicKey = consensusPublicKey;
        this.name = name;
        this.agency = agency;
        this.caHash = caHash;
        this.rlpEncoded = rlpEncoded();
    }

    public byte[] getAccountAddress() {
        return accountAddress;
    }

    public long getConsensusVotingPower() {
        return consensusVotingPower;
    }

    public VerifyingKey getConsensusPublicKey() {
        return consensusPublicKey;
    }

    public int getShardingNum() {
        return shardingNum;
    }

    public String getName() {
        return name;
    }

    public String getAgency() {
        return agency;
    }

    public String getCaHash() {
        return caHash;
    }

    public ValidatorPublicKeyInfo clone() {
        ValidatorPublicKeyInfo result = new ValidatorPublicKeyInfo(ByteUtil.copyFrom(this.rlpEncoded));
        return result;
    }

    public VerifyResult verifySignature(byte[] hash, Signature signature) {
        SecurePublicKey securePublicKey = getConsensusPublicKey().getSecurePublicKey();
        boolean res = securePublicKey.verify(hash, signature.getSig());
        if (!res) {
            return VerifyResult.ofInvalidSignature();
        }
        return VerifyResult.ofSuccess();
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] accountAddress = RLP.encodeElement(this.accountAddress);
        byte[] consensusVotingPower = RLP.encodeBigInteger(BigInteger.valueOf(this.consensusVotingPower));
        byte[] shardingNum = RLP.encodeInt(this.shardingNum);
        byte[] consensusPublicKey = RLP.encodeElement(this.consensusPublicKey.getKey());
        byte[] name = RLP.encodeString(this.name);
        byte[] agency = RLP.encodeString(this.agency);
        byte[] caHash = RLP.encodeString(this.caHash);
        return RLP.encodeList(accountAddress, consensusVotingPower, shardingNum, consensusPublicKey, name, agency, caHash);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.accountAddress = rlpDecode.get(0).getRLPData();
        this.consensusVotingPower = ByteUtil.byteArrayToLong(rlpDecode.get(1).getRLPData());
        this.shardingNum = ByteUtil.byteArrayToInt(rlpDecode.get(2).getRLPData());
        this.consensusPublicKey = new VerifyingKey(rlpDecode.get(3).getRLPData());
        this.name = new String(rlpDecode.get(4).getRLPData());
        this.agency = new String(rlpDecode.get(5).getRLPData());
        this.caHash = new String(rlpDecode.get(6).getRLPData());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidatorPublicKeyInfo that = (ValidatorPublicKeyInfo) o;
        return //consensusVotingPower == that.consensusVotingPower &&
                //shardingNum == that.shardingNum &&
                //Arrays.equals(accountAddress, that.accountAddress) &&
                Objects.equals(consensusPublicKey, that.consensusPublicKey) &&
//                Objects.equals(name, that.name) &&
//                Objects.equals(agency, that.agency) &&
                Objects.equals(caHash, that.caHash);
    }

    @Override
    public String toString() {
        return "ValidatorPublicKeyInfo{" +
                "accountAddress=" + Hex.toHexString(accountAddress) +
                ", consensusVotingPower=" + consensusVotingPower +
                ", shardingNum=" + shardingNum +
                ", consensusPublicKey=" + consensusPublicKey +
                ", name='" + name + '\'' +
                ", agency=" + agency +
                ", caHash=" + caHash +
                '}';
    }
}
