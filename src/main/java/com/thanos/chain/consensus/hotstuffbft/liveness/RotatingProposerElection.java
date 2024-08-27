package com.thanos.chain.consensus.hotstuffbft.liveness;

import com.thanos.common.crypto.key.asymmetric.SecurePublicKey;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RotatingProposerElection.java description：
 *
 * @Author laiyiyu create on 2020-02-28 16:17:14
 */
public class RotatingProposerElection extends ProposerElection {

    List<Pair<byte[] /* public key */, byte[] /* nodeId */>> proposerInfos;

    private Map<ByteArrayWrapper, byte[]> pk2NodeMap;

    int contiguousRounds;

    public RotatingProposerElection(List<byte[]> proposers, int contiguousRounds) {
        proposerInfos = new ArrayList<>(proposers.size());
        pk2NodeMap = new HashMap<>(proposers.size());
        for (byte[] publicKey: proposers) {
            byte[] newPk = ByteUtil.copyFrom(publicKey);
            byte[] nodeId = SecurePublicKey.generate(newPk).getNodeId();
            proposerInfos.add(Pair.of(newPk, nodeId));
            pk2NodeMap.put(new ByteArrayWrapper(newPk), nodeId);
        }
        this.contiguousRounds = contiguousRounds;
    }

    @Override
    public Pair<byte[] /* public key */, byte[] /* nodeId */> getValidProposer(long round) {
        int proposalIndex = (int) ((round / contiguousRounds) % proposerInfos.size());
        return this.proposerInfos.get(proposalIndex);
    }

    @Override
    public byte[] getNodeId(ByteArrayWrapper pk) {
        return pk2NodeMap.get(pk);
    }
}
