package com.thanos.chain.executor.dag.test_imple;


import com.thanos.chain.ledger.model.eth.EthTransaction;

import java.util.List;

/**
 * 类ParallelExecutable.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-17 16:40:49
 */
public class ParallelExecutable {

    final private List<ParallelTX> orderExecute;

    public ParallelExecutable(List<ParallelTX> orderExecute) {
        this.orderExecute = orderExecute;
    }

    static public class ParallelTX {
        final List<EthTransaction> parallelTXs;


        public ParallelTX(List<EthTransaction> parallelTXs) {
            this.parallelTXs = parallelTXs;
        }

//        @Override
//        public String toString() {
//            StringBuilder content = new StringBuilder("######start").append("\r\n");
//            for (EthTransaction transaction : parallelTXs) {
//                for (ByteArrayWrapper byteArrayWrapper : transaction.getExecuteStates()) {
//                    content.append(byteArrayWrapper).append(",");
//                }
//                content.append("\r\n").append("-------").append("\r\n");
//            }
//            content.append("######end").append("\r\n");
//            return content.toString();
//        }

    }

    public List<ParallelTX> getOrderExecute() {
        return orderExecute;
    }

//    @Override
//    public String toString() {
//        StringBuilder content = new StringBuilder();
//        for (ParallelTX parallelTX : orderExecute) {
//            content.append(parallelTX).append("\r\n");
//        }
//
//        return content.toString();
//    }
}
