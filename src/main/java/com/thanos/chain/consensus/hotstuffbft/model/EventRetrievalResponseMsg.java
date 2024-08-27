package com.thanos.chain.consensus.hotstuffbft.model;

import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;

import java.util.ArrayList;
import java.util.List;

/**
 * EventRetrievalResponseMsg.java description：
 *
 * @Author laiyiyu create on 2020-03-04 11:20:36
 */
public class EventRetrievalResponseMsg extends ConsensusMsg {

    public enum EventRetrievalStatus {
        // Successfully fill in the request.
        SUCCESSED,
        // Can not find the event corresponding to event_id.
        ID_NOT_FOUND,
        // Can not find enough events but find some.
        NOT_ENOUGH_EVENTS;

        static EventRetrievalStatus convertFromOrdinal(int ordinal) {
            if (ordinal == 0) {
                return SUCCESSED;
            } else if (ordinal == 1) {
                return ID_NOT_FOUND;
            } else if (ordinal == 2) {
                return NOT_ENOUGH_EVENTS;
            } else {
                throw new RuntimeException("ordinal not exit!");
            }
        }
    }

    EventRetrievalStatus status;

    List<Event> events;

    public EventRetrievalResponseMsg(byte[] encode) {
        super(encode);
    }

    public EventRetrievalResponseMsg(EventRetrievalStatus status, List<Event> events) {
        super(null);
        this.events = events;
        this.status = status;
        this.rlpEncoded = rlpEncoded();
    }

    public List<Event> getEvents() {
        return events;
    }

    public EventRetrievalStatus getStatus() {
        return status;
    }

    @Override
    protected byte[] rlpEncoded() {
        byte[][] encode = new byte[events.size() + 1][];
        encode[0] = RLP.encodeInt(status.ordinal());
        int i = 1;
        for (Event event: events) {
            encode[i] = event.getEncoded();
            i++;
        }
        return RLP.encodeList(encode);
    }

    @Override
    protected void rlpDecoded() {
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList retrievalRes = (RLPList) params.get(0);
        this.status = EventRetrievalStatus.convertFromOrdinal(ByteUtil.byteArrayToInt(retrievalRes.get(0).getRLPData()));

        List<Event> events = new ArrayList<>();
        for (int i = 1; i < retrievalRes.size(); i++) {
            events.add(new Event(retrievalRes.get(i).getRLPData()));
        }
        this.events = events;
    }


    @Override
    public byte getCode() {
        return ConsensusCommand.EVENT_RETRIEVAL_RESP.getCode();
    }

    @Override
    public ConsensusCommand getCommand() {
        return ConsensusCommand.EVENT_RETRIEVAL_RESP;
    }

    @Override
    public String toString() {
        return "EventRetrievalResponseMsg{" +
                "status=" + status +
                ", events size =" + events.size() +
                '}';
    }

    public static void main(String[] args) {
        System.out.println(EventRetrievalStatus.SUCCESSED.ordinal());
        System.out.println(EventRetrievalStatus.ID_NOT_FOUND.ordinal());
        System.out.println(EventRetrievalStatus.NOT_ENOUGH_EVENTS.ordinal());

    }
}
