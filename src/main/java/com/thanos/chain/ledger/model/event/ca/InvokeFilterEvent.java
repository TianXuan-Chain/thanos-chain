package com.thanos.chain.ledger.model.event.ca;

import com.thanos.chain.ledger.model.event.CommandEvent;
import com.thanos.chain.ledger.model.event.GlobalEventCommand;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

/**
 * InvokeFilterEvent.java description：
 *
 * @Author laiyiyu create on 2021-04-01 14:35:57
 */
public class InvokeFilterEvent extends CommandEvent {

    byte[] invokeAddr;

    byte[] invokeMethodId;

    byte[] methodInput;

    public InvokeFilterEvent(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public InvokeFilterEvent(byte[] invokeAddr, byte[] invokeMethodId, byte[] methodInput) {
        super(null);
        this.invokeAddr = invokeAddr;
        this.invokeMethodId = invokeMethodId;
        this.methodInput = methodInput;
        this.rlpEncoded = rlpEncoded();
    }

    public byte[] getInvokeAddr() {
        return invokeAddr;
    }

    public byte[] getInvokeMethodId() {
        return invokeMethodId;
    }

    public byte[] getMethodInput() {
        return methodInput;
    }

    @Override
    public GlobalEventCommand getEventCommand() {
        return GlobalEventCommand.INVOKE_FILTER;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[] invokeAddr = RLP.encodeElement(this.invokeAddr);
        byte[] invokeMethodId = RLP.encodeElement(this.invokeMethodId);
        byte[] methodInput = RLP.encodeElement(this.methodInput);
        return RLP.encodeList(invokeAddr, invokeMethodId, methodInput);
    }

    @Override
    protected void rlpDecoded() {
        RLPList rlpInfo = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.invokeAddr = rlpInfo.get(0).getRLPData();
        this.invokeMethodId = rlpInfo.get(1).getRLPData();
        this.methodInput = rlpInfo.get(2).getRLPData();
    }
}
