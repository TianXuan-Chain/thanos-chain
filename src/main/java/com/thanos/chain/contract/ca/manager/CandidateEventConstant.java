package com.thanos.chain.contract.ca.manager;

/**
 * CandidateEventConstant.java description：
 *
 * @Author laiyiyu create on 2021-03-08 15:00:43
 */
public class CandidateEventConstant {

    //process type
    // 加入
    public static final int REGISTER_PROCESS = 0;
    // 退出
    public static final int CANCEL_PROCESS = 1;

    // 同意
    public static final int AGREE_VOTE = 0;
    // 拒绝
    public static final int DISAGREE_VOTE = 1;
    // 撤销
    public static final int REVOKE_VOTE = 2;

    //vote state code
    public static int VOTE_FAILED = 0;
    public static int VOTE_SUCCESS = 1;

    public static int AGREE_FINISH = 2;
    // disagree or revoke or invalid !
    public static int DISAGREE_FINISH = 3;
    public static int REVOKE_FINISH = 4;

}
