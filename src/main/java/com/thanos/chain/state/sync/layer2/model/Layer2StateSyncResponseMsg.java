package com.thanos.chain.state.sync.layer2.model;

import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.chain.consensus.hotstuffbft.model.EventData;
import com.thanos.chain.consensus.hotstuffbft.model.EventInfoWithSignatures;
import com.thanos.chain.consensus.hotstuffbft.model.LedgerInfoWithSignatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Layer2StateSyncResponseMsg.java description：
 *
 * @Author laiyiyu create on 2020-07-03 16:03:52
 */
public class Layer2StateSyncResponseMsg  extends Layer2StateSyncMsg {

    public enum CommitEventRetrievalStatus {
        // Successfully fill in the request.
        SUCCESSED,
        // Can not find the event corresponding to number.
        UN_EXCEPT_NUMBER;

        static CommitEventRetrievalStatus convertFromOrdinal(int ordinal) {
            if (ordinal == 0) {
                return SUCCESSED;
            } else if (ordinal == 1) {
                return UN_EXCEPT_NUMBER;
            } else {
                throw new RuntimeException("ordinal not exit!");
            }
        }
    }

    CommitEventRetrievalStatus status;

    List<EventData> eventDatas;

    List<EventInfoWithSignatures> eventInfoWithSignatureses;

    public Layer2StateSyncResponseMsg(byte[] encode) {
        super(encode);
    }

    public Layer2StateSyncResponseMsg(CommitEventRetrievalStatus status, List<EventData> eventDatas, List<EventInfoWithSignatures> eventInfoWithSignatureses) {
        super(null);
        this.status = status;
        this.eventDatas = eventDatas;
        this.eventInfoWithSignatureses = eventInfoWithSignatureses;
        this.rlpEncoded = rlpEncoded();
    }

    public CommitEventRetrievalStatus getStatus() {
        return status;
    }

    public List<EventData> getEventDatas() {
        return eventDatas;
    }

    public List<EventInfoWithSignatures> getEventInfoWithSignatureses() {
        return eventInfoWithSignatureses;
    }

    public boolean isSuccess() {
        return this.status == CommitEventRetrievalStatus.SUCCESSED;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[][] encode = new byte[eventDatas.size() + eventInfoWithSignatureses.size() + 2][];
        encode[0] = RLP.encodeInt(status.ordinal());
        encode[1] = RLP.encodeInt(eventDatas.size());

        int i = 2;
        for (EventData event: eventDatas) {
            encode[i] = event.getEncoded();
            i++;
        }

        for (EventInfoWithSignatures eventInfoWithSignatures: eventInfoWithSignatureses) {
            encode[i] = eventInfoWithSignatures.getEncoded();
            i++;
        }

        return RLP.encodeList(encode);
    }

    @Override
    protected void rlpDecoded() {
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList resp = (RLPList) params.get(0);

        this.status = CommitEventRetrievalStatus.convertFromOrdinal(ByteUtil.byteArrayToInt(resp.get(0).getRLPData()));
        int size = ByteUtil.byteArrayToInt(resp.get(1).getRLPData());

        List<EventData> eventDatas = new ArrayList<>();
        int i = 2;
        int tempEventIndex = size + 2;
        for (; i < tempEventIndex; i++) {
            eventDatas.add(new EventData(resp.get(i).getRLPData()));
        }
        this.eventDatas = eventDatas;

        List<EventInfoWithSignatures> eventInfoWithSignatureses = new ArrayList<>();
        for (; i < resp.size(); i++) {
            eventInfoWithSignatureses.add(new EventInfoWithSignatures(resp.get(i).getRLPData()));
        }
        this.eventInfoWithSignatureses = eventInfoWithSignatureses;
    }

    @Override
    public byte getCode() {
        return Layer2StateChainSyncCommand.LAYER2_STATE_SYNC_RESPONSE.getCode();
    }

    @Override
    public Layer2StateChainSyncCommand getCommand() {
        return Layer2StateChainSyncCommand.LAYER2_STATE_SYNC_RESPONSE;
    }
}
