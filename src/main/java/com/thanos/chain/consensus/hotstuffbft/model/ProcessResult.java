package com.thanos.chain.consensus.hotstuffbft.model;

/**
 * ProcessResult.java description：
 *
 * @Author laiyiyu create on 2020-03-07 14:31:59
 */
public class ProcessResult<T> {

    public final static ProcessResult<Void> SUCCESSFUL = ofSuccess(null);

    public final Status status;

    public final T result;

    public final StringBuilder errBuild;

    private ProcessResult(Status status, T result, String errMsg) {
        this.status = status;
        this.result = result;
        this.errBuild = new StringBuilder(errMsg == null? "": errMsg);
    }

    public static enum Status {
        SUCCESS,
        ERROR;
    }

    public boolean isSuccess() {
        return this.status == Status.SUCCESS;
    }

    public T getResult() {
        return result;
    }

    public String getErrMsg() {
        return this.errBuild.toString();
    }

    public ProcessResult appendErrorMsg(String errorMsg) {
        errBuild.append("\n")
                .append(errorMsg);
        return this;
    }

    public static <T> ProcessResult<T>  ofSuccess(T result) {
        return new ProcessResult<>(Status.SUCCESS, result, "");
    }

    public static <T> ProcessResult<T>  ofSuccess() {
        return new ProcessResult<>(Status.SUCCESS, null, "");
    }

    public static <T> ProcessResult<T>  ofError(String errMsg) {
        return new ProcessResult<>(Status.ERROR, null, errMsg);
    }

    public static <T> ProcessResult<T>  ofError() {
        return new ProcessResult<>(Status.ERROR, null, "");
    }
}
