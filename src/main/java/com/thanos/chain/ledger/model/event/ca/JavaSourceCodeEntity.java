package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.RLPModel;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

import java.util.Objects;

/**
 * JavaSourceCodeEntity.java description：
 *
 * @Author laiyiyu create on 2021-04-15 17:33:25
 */
public class JavaSourceCodeEntity extends RLPModel {

    String clazzName;

    String sourceCode;

    public JavaSourceCodeEntity(byte[] encode) {
        super(encode);
    }

    public JavaSourceCodeEntity(String clazzName, String sourceCode) {
        super(null);
        this.clazzName = clazzName;
        this.sourceCode = sourceCode;
        this.rlpEncoded = rlpEncoded();
    }

    public String getClazzName() {
        return clazzName;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] className = RLP.encodeString(this.clazzName);
        byte[] sourceCode = RLP.encodeString(this.sourceCode);
        return RLP.encodeList(className, sourceCode);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpDecode = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.clazzName = new String(rlpDecode.get(0).getRLPData());
        this.sourceCode = new String(rlpDecode.get(1).getRLPData());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaSourceCodeEntity that = (JavaSourceCodeEntity) o;
        return Objects.equals(clazzName, that.clazzName) &&
                Objects.equals(sourceCode, that.sourceCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazzName, sourceCode);
    }
}
