package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.store.Persistable;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * CaContractCode.java description：
 *
 * @Author laiyiyu create on 2021-04-12 15:47:58
 */
public class CaContractCode extends Persistable {

    byte[] codeAddress;

    String codeDescription;

    String filterMainClassName;

    List<JavaSourceCodeEntity> javaCodeSourceEntity;

    public CaContractCode(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public CaContractCode(byte[] codeAddress, String codeDescription, String filterMainClassName, List<JavaSourceCodeEntity> javaCodeSourceEntity) {
        super(null);
        this.codeAddress = codeAddress;
        this.codeDescription = codeDescription;
        this.filterMainClassName = filterMainClassName;
        this.javaCodeSourceEntity = javaCodeSourceEntity;
        this.rlpEncoded = rlpEncoded();
    }

    public byte[] getCodeAddress() {
        return codeAddress;
    }

    public String getCodeDescription() {
        return codeDescription;
    }

    public String getFilterMainClassName() {
        return filterMainClassName;
    }

    public List<JavaSourceCodeEntity> getJavaCodeSourceEntity() {
        return javaCodeSourceEntity;
    }

    @Override
    protected byte[] rlpEncoded() {
        int totalSize = 3 + 1 + javaCodeSourceEntity.size();

        byte[][] encode = new byte[totalSize][];
        encode[0] = RLP.encodeElement(codeAddress);
        encode[1] = RLP.encodeString(codeDescription);
        encode[2] = RLP.encodeString(filterMainClassName);
        encode[3] = RLP.encodeInt(javaCodeSourceEntity.size());

        int sourcesStart = 4;
        int sourcesEnd = sourcesStart + javaCodeSourceEntity.size();
        for (int i = sourcesStart; i < sourcesEnd; i++) {
            encode[i] = javaCodeSourceEntity.get(i - sourcesStart).getEncoded();
        }

        return RLP.encodeList(encode);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList)RLP.decode2(rlpEncoded).get(0);
        this.codeAddress = rlpDecode.get(0).getRLPData();
        this.codeDescription = new String(rlpDecode.get(1).getRLPData());
        this.filterMainClassName = new String(rlpDecode.get(2).getRLPData());
        int strCodeSourcesSize = ByteUtil.byteArrayToInt(rlpDecode.get(3).getRLPData());

        int sourcesStart = 4;
        List<JavaSourceCodeEntity> javaCodeSourceEntity = new ArrayList<>(strCodeSourcesSize);
        for (int i = 0; i < strCodeSourcesSize; i++) {
            javaCodeSourceEntity.add(new JavaSourceCodeEntity(rlpDecode.get(i + sourcesStart).getRLPData()));

        }
        this.javaCodeSourceEntity = javaCodeSourceEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaContractCode that = (CaContractCode) o;
        return Arrays.equals(codeAddress, that.codeAddress) &&
                Objects.equals(codeDescription, that.codeDescription) &&
                Objects.equals(filterMainClassName, that.filterMainClassName) &&
                Objects.equals(javaCodeSourceEntity, that.javaCodeSourceEntity);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(codeDescription, filterMainClassName, javaCodeSourceEntity);
        result = 31 * result + Arrays.hashCode(codeAddress);
        return result;
    }

    @Override
    public String toString() {
        return "CaContractCode{" +
                "codeAddress=" + Hex.toHexString(codeAddress) +
                ", codeDescription='" + codeDescription + '\'' +
                ", filterMainClassName='" + filterMainClassName + '\'' +
                ", javaCodeSourceEntity size=" + javaCodeSourceEntity.size() +
                '}';
    }
}
