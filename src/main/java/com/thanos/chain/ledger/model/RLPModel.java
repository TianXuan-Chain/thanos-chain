package com.thanos.chain.ledger.model;

/**
 * RLPModel.java description：
 * 注意，继承该类的子类在实现时需要注意的是，
 * 不要对自身的实列属性设置默认值，因为在继承该类的
 * 的构造函数时，该构造函数会调用子类的实现类rlpDecoded()实现，
 * 这样一来，如果由默认值，由于jvm的构造函数初始化的顺序为，先完全将父类
 * 构造完成，然后才会执行子类的实列属性初始化，因此，在rlpDecoded()实现时，
 * 如果直接使用了相应的实列属性，即使设置了默认值，但由于没有初始化，因此还是为空，
 * 从而导致npe异常。
 * 另外，如果设置了默认值，即使rlpDecoded()实现中赋值了指定实例属性相应的值，
 * 也会再rlpDecoded()执行完成后，子类实列属性初始化赋值时，给默认值覆盖
 *
 * @Author laiyiyu create on 2020-02-23 15:37:04
 */
public abstract class RLPModel {

    protected byte[] rlpEncoded;

    protected volatile boolean parsed;

    public RLPModel(byte[] rlpEncoded) {
        if (rlpEncoded == null) return;
        this.rlpEncoded = rlpEncoded;
        rlpDecoded();
        parsed = true;
    }

    protected abstract byte[] rlpEncoded();

    protected abstract void rlpDecoded();

    public final byte[] getEncoded() {
        if (rlpEncoded != null) return this.rlpEncoded;
        this.rlpEncoded = rlpEncoded();
        return this.rlpEncoded;
    }
}
