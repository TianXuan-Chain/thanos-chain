package com.thanos.api.proto.push;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 *服务接口定义，服务端和客户端都要遵循该接口进行通信
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.30.2)",
    comments = "Source: push.proto")
public final class PushServiceGrpc {

  private PushServiceGrpc() {}

  public static final String SERVICE_NAME = "com.thanos.api.proto.push.PushService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest,
      com.thanos.api.proto.push.DefaultResponse> getSaveAliveMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "saveAlive",
      requestType = com.thanos.api.proto.push.DefaultRequest.class,
      responseType = com.thanos.api.proto.push.DefaultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest,
      com.thanos.api.proto.push.DefaultResponse> getSaveAliveMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest, com.thanos.api.proto.push.DefaultResponse> getSaveAliveMethod;
    if ((getSaveAliveMethod = PushServiceGrpc.getSaveAliveMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getSaveAliveMethod = PushServiceGrpc.getSaveAliveMethod) == null) {
          PushServiceGrpc.getSaveAliveMethod = getSaveAliveMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.DefaultRequest, com.thanos.api.proto.push.DefaultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "saveAlive"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.DefaultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.DefaultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("saveAlive"))
              .build();
        }
      }
    }
    return getSaveAliveMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.EthTransactionsPushDTO,
      com.thanos.api.proto.push.DefaultResponse> getPushEthTransactionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "pushEthTransactions",
      requestType = com.thanos.api.proto.push.EthTransactionsPushDTO.class,
      responseType = com.thanos.api.proto.push.DefaultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.EthTransactionsPushDTO,
      com.thanos.api.proto.push.DefaultResponse> getPushEthTransactionsMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.EthTransactionsPushDTO, com.thanos.api.proto.push.DefaultResponse> getPushEthTransactionsMethod;
    if ((getPushEthTransactionsMethod = PushServiceGrpc.getPushEthTransactionsMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getPushEthTransactionsMethod = PushServiceGrpc.getPushEthTransactionsMethod) == null) {
          PushServiceGrpc.getPushEthTransactionsMethod = getPushEthTransactionsMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.EthTransactionsPushDTO, com.thanos.api.proto.push.DefaultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "pushEthTransactions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.EthTransactionsPushDTO.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.DefaultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("pushEthTransactions"))
              .build();
        }
      }
    }
    return getPushEthTransactionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.GlobalNodeEventsPushDTO,
      com.thanos.api.proto.push.DefaultResponse> getPushGlobalNodeEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "pushGlobalNodeEvents",
      requestType = com.thanos.api.proto.push.GlobalNodeEventsPushDTO.class,
      responseType = com.thanos.api.proto.push.DefaultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.GlobalNodeEventsPushDTO,
      com.thanos.api.proto.push.DefaultResponse> getPushGlobalNodeEventsMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.GlobalNodeEventsPushDTO, com.thanos.api.proto.push.DefaultResponse> getPushGlobalNodeEventsMethod;
    if ((getPushGlobalNodeEventsMethod = PushServiceGrpc.getPushGlobalNodeEventsMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getPushGlobalNodeEventsMethod = PushServiceGrpc.getPushGlobalNodeEventsMethod) == null) {
          PushServiceGrpc.getPushGlobalNodeEventsMethod = getPushGlobalNodeEventsMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.GlobalNodeEventsPushDTO, com.thanos.api.proto.push.DefaultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "pushGlobalNodeEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.GlobalNodeEventsPushDTO.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.DefaultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("pushGlobalNodeEvents"))
              .build();
        }
      }
    }
    return getPushGlobalNodeEventsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.BytesObject,
      com.thanos.api.proto.push.BytesObject> getGetGlobalNodeEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getGlobalNodeEvent",
      requestType = com.thanos.api.proto.push.BytesObject.class,
      responseType = com.thanos.api.proto.push.BytesObject.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.BytesObject,
      com.thanos.api.proto.push.BytesObject> getGetGlobalNodeEventMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.BytesObject, com.thanos.api.proto.push.BytesObject> getGetGlobalNodeEventMethod;
    if ((getGetGlobalNodeEventMethod = PushServiceGrpc.getGetGlobalNodeEventMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getGetGlobalNodeEventMethod = PushServiceGrpc.getGetGlobalNodeEventMethod) == null) {
          PushServiceGrpc.getGetGlobalNodeEventMethod = getGetGlobalNodeEventMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.BytesObject, com.thanos.api.proto.push.BytesObject>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getGlobalNodeEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.BytesObject.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.BytesObject.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("getGlobalNodeEvent"))
              .build();
        }
      }
    }
    return getGetGlobalNodeEventMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.BytesObject,
      com.thanos.api.proto.push.BytesObject> getGetGlobalNodeEventReceiptMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getGlobalNodeEventReceipt",
      requestType = com.thanos.api.proto.push.BytesObject.class,
      responseType = com.thanos.api.proto.push.BytesObject.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.BytesObject,
      com.thanos.api.proto.push.BytesObject> getGetGlobalNodeEventReceiptMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.BytesObject, com.thanos.api.proto.push.BytesObject> getGetGlobalNodeEventReceiptMethod;
    if ((getGetGlobalNodeEventReceiptMethod = PushServiceGrpc.getGetGlobalNodeEventReceiptMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getGetGlobalNodeEventReceiptMethod = PushServiceGrpc.getGetGlobalNodeEventReceiptMethod) == null) {
          PushServiceGrpc.getGetGlobalNodeEventReceiptMethod = getGetGlobalNodeEventReceiptMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.BytesObject, com.thanos.api.proto.push.BytesObject>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getGlobalNodeEventReceipt"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.BytesObject.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.BytesObject.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("getGlobalNodeEventReceipt"))
              .build();
        }
      }
    }
    return getGetGlobalNodeEventReceiptMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest,
      com.thanos.api.proto.push.BytesObject> getGetEpochStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getEpochState",
      requestType = com.thanos.api.proto.push.DefaultRequest.class,
      responseType = com.thanos.api.proto.push.BytesObject.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest,
      com.thanos.api.proto.push.BytesObject> getGetEpochStateMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest, com.thanos.api.proto.push.BytesObject> getGetEpochStateMethod;
    if ((getGetEpochStateMethod = PushServiceGrpc.getGetEpochStateMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getGetEpochStateMethod = PushServiceGrpc.getGetEpochStateMethod) == null) {
          PushServiceGrpc.getGetEpochStateMethod = getGetEpochStateMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.DefaultRequest, com.thanos.api.proto.push.BytesObject>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getEpochState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.DefaultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.BytesObject.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("getEpochState"))
              .build();
        }
      }
    }
    return getGetEpochStateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest,
      com.thanos.api.proto.push.LongObject> getGetLatestBeExecutedNumMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getLatestBeExecutedNum",
      requestType = com.thanos.api.proto.push.DefaultRequest.class,
      responseType = com.thanos.api.proto.push.LongObject.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest,
      com.thanos.api.proto.push.LongObject> getGetLatestBeExecutedNumMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest, com.thanos.api.proto.push.LongObject> getGetLatestBeExecutedNumMethod;
    if ((getGetLatestBeExecutedNumMethod = PushServiceGrpc.getGetLatestBeExecutedNumMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getGetLatestBeExecutedNumMethod = PushServiceGrpc.getGetLatestBeExecutedNumMethod) == null) {
          PushServiceGrpc.getGetLatestBeExecutedNumMethod = getGetLatestBeExecutedNumMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.DefaultRequest, com.thanos.api.proto.push.LongObject>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getLatestBeExecutedNum"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.DefaultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.LongObject.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("getLatestBeExecutedNum"))
              .build();
        }
      }
    }
    return getGetLatestBeExecutedNumMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest,
      com.thanos.api.proto.push.LongObject> getGetLatestConsensusNumberMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getLatestConsensusNumber",
      requestType = com.thanos.api.proto.push.DefaultRequest.class,
      responseType = com.thanos.api.proto.push.LongObject.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest,
      com.thanos.api.proto.push.LongObject> getGetLatestConsensusNumberMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest, com.thanos.api.proto.push.LongObject> getGetLatestConsensusNumberMethod;
    if ((getGetLatestConsensusNumberMethod = PushServiceGrpc.getGetLatestConsensusNumberMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getGetLatestConsensusNumberMethod = PushServiceGrpc.getGetLatestConsensusNumberMethod) == null) {
          PushServiceGrpc.getGetLatestConsensusNumberMethod = getGetLatestConsensusNumberMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.DefaultRequest, com.thanos.api.proto.push.LongObject>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getLatestConsensusNumber"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.DefaultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.LongObject.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("getLatestConsensusNumber"))
              .build();
        }
      }
    }
    return getGetLatestConsensusNumberMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest,
      com.thanos.api.proto.push.LongObject> getGetCurrentCommitRoundMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getCurrentCommitRound",
      requestType = com.thanos.api.proto.push.DefaultRequest.class,
      responseType = com.thanos.api.proto.push.LongObject.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest,
      com.thanos.api.proto.push.LongObject> getGetCurrentCommitRoundMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.DefaultRequest, com.thanos.api.proto.push.LongObject> getGetCurrentCommitRoundMethod;
    if ((getGetCurrentCommitRoundMethod = PushServiceGrpc.getGetCurrentCommitRoundMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getGetCurrentCommitRoundMethod = PushServiceGrpc.getGetCurrentCommitRoundMethod) == null) {
          PushServiceGrpc.getGetCurrentCommitRoundMethod = getGetCurrentCommitRoundMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.DefaultRequest, com.thanos.api.proto.push.LongObject>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getCurrentCommitRound"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.DefaultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.LongObject.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("getCurrentCommitRound"))
              .build();
        }
      }
    }
    return getGetCurrentCommitRoundMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.BytesObject,
      com.thanos.api.proto.push.BytesObject> getGetEthTransactionByHashMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getEthTransactionByHash",
      requestType = com.thanos.api.proto.push.BytesObject.class,
      responseType = com.thanos.api.proto.push.BytesObject.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.BytesObject,
      com.thanos.api.proto.push.BytesObject> getGetEthTransactionByHashMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.BytesObject, com.thanos.api.proto.push.BytesObject> getGetEthTransactionByHashMethod;
    if ((getGetEthTransactionByHashMethod = PushServiceGrpc.getGetEthTransactionByHashMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getGetEthTransactionByHashMethod = PushServiceGrpc.getGetEthTransactionByHashMethod) == null) {
          PushServiceGrpc.getGetEthTransactionByHashMethod = getGetEthTransactionByHashMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.BytesObject, com.thanos.api.proto.push.BytesObject>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getEthTransactionByHash"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.BytesObject.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.BytesObject.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("getEthTransactionByHash"))
              .build();
        }
      }
    }
    return getGetEthTransactionByHashMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.ListBytesObject,
      com.thanos.api.proto.push.ListBytesObject> getGetEthTransactionsByHashesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getEthTransactionsByHashes",
      requestType = com.thanos.api.proto.push.ListBytesObject.class,
      responseType = com.thanos.api.proto.push.ListBytesObject.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.ListBytesObject,
      com.thanos.api.proto.push.ListBytesObject> getGetEthTransactionsByHashesMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.ListBytesObject, com.thanos.api.proto.push.ListBytesObject> getGetEthTransactionsByHashesMethod;
    if ((getGetEthTransactionsByHashesMethod = PushServiceGrpc.getGetEthTransactionsByHashesMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getGetEthTransactionsByHashesMethod = PushServiceGrpc.getGetEthTransactionsByHashesMethod) == null) {
          PushServiceGrpc.getGetEthTransactionsByHashesMethod = getGetEthTransactionsByHashesMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.ListBytesObject, com.thanos.api.proto.push.ListBytesObject>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getEthTransactionsByHashes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.ListBytesObject.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.ListBytesObject.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("getEthTransactionsByHashes"))
              .build();
        }
      }
    }
    return getGetEthTransactionsByHashesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.LongObject,
      com.thanos.api.proto.push.BlockBytesObject> getGetBlockByNumberMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getBlockByNumber",
      requestType = com.thanos.api.proto.push.LongObject.class,
      responseType = com.thanos.api.proto.push.BlockBytesObject.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.LongObject,
      com.thanos.api.proto.push.BlockBytesObject> getGetBlockByNumberMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.LongObject, com.thanos.api.proto.push.BlockBytesObject> getGetBlockByNumberMethod;
    if ((getGetBlockByNumberMethod = PushServiceGrpc.getGetBlockByNumberMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getGetBlockByNumberMethod = PushServiceGrpc.getGetBlockByNumberMethod) == null) {
          PushServiceGrpc.getGetBlockByNumberMethod = getGetBlockByNumberMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.LongObject, com.thanos.api.proto.push.BlockBytesObject>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getBlockByNumber"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.LongObject.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.BlockBytesObject.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("getBlockByNumber"))
              .build();
        }
      }
    }
    return getGetBlockByNumberMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.LongObject,
      com.thanos.api.proto.push.BytesObject> getGetEventDataByNumberMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getEventDataByNumber",
      requestType = com.thanos.api.proto.push.LongObject.class,
      responseType = com.thanos.api.proto.push.BytesObject.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.LongObject,
      com.thanos.api.proto.push.BytesObject> getGetEventDataByNumberMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.LongObject, com.thanos.api.proto.push.BytesObject> getGetEventDataByNumberMethod;
    if ((getGetEventDataByNumberMethod = PushServiceGrpc.getGetEventDataByNumberMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getGetEventDataByNumberMethod = PushServiceGrpc.getGetEventDataByNumberMethod) == null) {
          PushServiceGrpc.getGetEventDataByNumberMethod = getGetEventDataByNumberMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.LongObject, com.thanos.api.proto.push.BytesObject>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getEventDataByNumber"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.LongObject.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.BytesObject.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("getEventDataByNumber"))
              .build();
        }
      }
    }
    return getGetEventDataByNumberMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO,
      com.thanos.api.proto.push.BytesObject> getEthCallMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ethCall",
      requestType = com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO.class,
      responseType = com.thanos.api.proto.push.BytesObject.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO,
      com.thanos.api.proto.push.BytesObject> getEthCallMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO, com.thanos.api.proto.push.BytesObject> getEthCallMethod;
    if ((getEthCallMethod = PushServiceGrpc.getEthCallMethod) == null) {
      synchronized (PushServiceGrpc.class) {
        if ((getEthCallMethod = PushServiceGrpc.getEthCallMethod) == null) {
          PushServiceGrpc.getEthCallMethod = getEthCallMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO, com.thanos.api.proto.push.BytesObject>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ethCall"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.push.BytesObject.getDefaultInstance()))
              .setSchemaDescriptor(new PushServiceMethodDescriptorSupplier("ethCall"))
              .build();
        }
      }
    }
    return getEthCallMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PushServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PushServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PushServiceStub>() {
        @java.lang.Override
        public PushServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PushServiceStub(channel, callOptions);
        }
      };
    return PushServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PushServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PushServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PushServiceBlockingStub>() {
        @java.lang.Override
        public PushServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PushServiceBlockingStub(channel, callOptions);
        }
      };
    return PushServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PushServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PushServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PushServiceFutureStub>() {
        @java.lang.Override
        public PushServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PushServiceFutureStub(channel, callOptions);
        }
      };
    return PushServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   *服务接口定义，服务端和客户端都要遵循该接口进行通信
   * </pre>
   */
  public static abstract class PushServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void saveAlive(com.thanos.api.proto.push.DefaultRequest request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.DefaultResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSaveAliveMethod(), responseObserver);
    }

    /**
     */
    public void pushEthTransactions(com.thanos.api.proto.push.EthTransactionsPushDTO request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.DefaultResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPushEthTransactionsMethod(), responseObserver);
    }

    /**
     */
    public void pushGlobalNodeEvents(com.thanos.api.proto.push.GlobalNodeEventsPushDTO request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.DefaultResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPushGlobalNodeEventsMethod(), responseObserver);
    }

    /**
     */
    public void getGlobalNodeEvent(com.thanos.api.proto.push.BytesObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnimplementedUnaryCall(getGetGlobalNodeEventMethod(), responseObserver);
    }

    /**
     */
    public void getGlobalNodeEventReceipt(com.thanos.api.proto.push.BytesObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnimplementedUnaryCall(getGetGlobalNodeEventReceiptMethod(), responseObserver);
    }

    /**
     */
    public void getEpochState(com.thanos.api.proto.push.DefaultRequest request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnimplementedUnaryCall(getGetEpochStateMethod(), responseObserver);
    }

    /**
     */
    public void getLatestBeExecutedNum(com.thanos.api.proto.push.DefaultRequest request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.LongObject> responseObserver) {
      asyncUnimplementedUnaryCall(getGetLatestBeExecutedNumMethod(), responseObserver);
    }

    /**
     */
    public void getLatestConsensusNumber(com.thanos.api.proto.push.DefaultRequest request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.LongObject> responseObserver) {
      asyncUnimplementedUnaryCall(getGetLatestConsensusNumberMethod(), responseObserver);
    }

    /**
     */
    public void getCurrentCommitRound(com.thanos.api.proto.push.DefaultRequest request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.LongObject> responseObserver) {
      asyncUnimplementedUnaryCall(getGetCurrentCommitRoundMethod(), responseObserver);
    }

    /**
     */
    public void getEthTransactionByHash(com.thanos.api.proto.push.BytesObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnimplementedUnaryCall(getGetEthTransactionByHashMethod(), responseObserver);
    }

    /**
     */
    public void getEthTransactionsByHashes(com.thanos.api.proto.push.ListBytesObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.ListBytesObject> responseObserver) {
      asyncUnimplementedUnaryCall(getGetEthTransactionsByHashesMethod(), responseObserver);
    }

    /**
     */
    public void getBlockByNumber(com.thanos.api.proto.push.LongObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BlockBytesObject> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByNumberMethod(), responseObserver);
    }

    /**
     */
    public void getEventDataByNumber(com.thanos.api.proto.push.LongObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnimplementedUnaryCall(getGetEventDataByNumberMethod(), responseObserver);
    }

    /**
     */
    public void ethCall(com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnimplementedUnaryCall(getEthCallMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSaveAliveMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.DefaultRequest,
                com.thanos.api.proto.push.DefaultResponse>(
                  this, METHODID_SAVE_ALIVE)))
          .addMethod(
            getPushEthTransactionsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.EthTransactionsPushDTO,
                com.thanos.api.proto.push.DefaultResponse>(
                  this, METHODID_PUSH_ETH_TRANSACTIONS)))
          .addMethod(
            getPushGlobalNodeEventsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.GlobalNodeEventsPushDTO,
                com.thanos.api.proto.push.DefaultResponse>(
                  this, METHODID_PUSH_GLOBAL_NODE_EVENTS)))
          .addMethod(
            getGetGlobalNodeEventMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.BytesObject,
                com.thanos.api.proto.push.BytesObject>(
                  this, METHODID_GET_GLOBAL_NODE_EVENT)))
          .addMethod(
            getGetGlobalNodeEventReceiptMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.BytesObject,
                com.thanos.api.proto.push.BytesObject>(
                  this, METHODID_GET_GLOBAL_NODE_EVENT_RECEIPT)))
          .addMethod(
            getGetEpochStateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.DefaultRequest,
                com.thanos.api.proto.push.BytesObject>(
                  this, METHODID_GET_EPOCH_STATE)))
          .addMethod(
            getGetLatestBeExecutedNumMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.DefaultRequest,
                com.thanos.api.proto.push.LongObject>(
                  this, METHODID_GET_LATEST_BE_EXECUTED_NUM)))
          .addMethod(
            getGetLatestConsensusNumberMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.DefaultRequest,
                com.thanos.api.proto.push.LongObject>(
                  this, METHODID_GET_LATEST_CONSENSUS_NUMBER)))
          .addMethod(
            getGetCurrentCommitRoundMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.DefaultRequest,
                com.thanos.api.proto.push.LongObject>(
                  this, METHODID_GET_CURRENT_COMMIT_ROUND)))
          .addMethod(
            getGetEthTransactionByHashMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.BytesObject,
                com.thanos.api.proto.push.BytesObject>(
                  this, METHODID_GET_ETH_TRANSACTION_BY_HASH)))
          .addMethod(
            getGetEthTransactionsByHashesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.ListBytesObject,
                com.thanos.api.proto.push.ListBytesObject>(
                  this, METHODID_GET_ETH_TRANSACTIONS_BY_HASHES)))
          .addMethod(
            getGetBlockByNumberMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.LongObject,
                com.thanos.api.proto.push.BlockBytesObject>(
                  this, METHODID_GET_BLOCK_BY_NUMBER)))
          .addMethod(
            getGetEventDataByNumberMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.LongObject,
                com.thanos.api.proto.push.BytesObject>(
                  this, METHODID_GET_EVENT_DATA_BY_NUMBER)))
          .addMethod(
            getEthCallMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO,
                com.thanos.api.proto.push.BytesObject>(
                  this, METHODID_ETH_CALL)))
          .build();
    }
  }

  /**
   * <pre>
   *服务接口定义，服务端和客户端都要遵循该接口进行通信
   * </pre>
   */
  public static final class PushServiceStub extends io.grpc.stub.AbstractAsyncStub<PushServiceStub> {
    private PushServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PushServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PushServiceStub(channel, callOptions);
    }

    /**
     */
    public void saveAlive(com.thanos.api.proto.push.DefaultRequest request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.DefaultResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSaveAliveMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void pushEthTransactions(com.thanos.api.proto.push.EthTransactionsPushDTO request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.DefaultResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPushEthTransactionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void pushGlobalNodeEvents(com.thanos.api.proto.push.GlobalNodeEventsPushDTO request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.DefaultResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPushGlobalNodeEventsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getGlobalNodeEvent(com.thanos.api.proto.push.BytesObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetGlobalNodeEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getGlobalNodeEventReceipt(com.thanos.api.proto.push.BytesObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetGlobalNodeEventReceiptMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getEpochState(com.thanos.api.proto.push.DefaultRequest request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetEpochStateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getLatestBeExecutedNum(com.thanos.api.proto.push.DefaultRequest request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.LongObject> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetLatestBeExecutedNumMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getLatestConsensusNumber(com.thanos.api.proto.push.DefaultRequest request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.LongObject> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetLatestConsensusNumberMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getCurrentCommitRound(com.thanos.api.proto.push.DefaultRequest request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.LongObject> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetCurrentCommitRoundMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getEthTransactionByHash(com.thanos.api.proto.push.BytesObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetEthTransactionByHashMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getEthTransactionsByHashes(com.thanos.api.proto.push.ListBytesObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.ListBytesObject> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetEthTransactionsByHashesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getBlockByNumber(com.thanos.api.proto.push.LongObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BlockBytesObject> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByNumberMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getEventDataByNumber(com.thanos.api.proto.push.LongObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetEventDataByNumberMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void ethCall(com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEthCallMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   *服务接口定义，服务端和客户端都要遵循该接口进行通信
   * </pre>
   */
  public static final class PushServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<PushServiceBlockingStub> {
    private PushServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PushServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PushServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.thanos.api.proto.push.DefaultResponse saveAlive(com.thanos.api.proto.push.DefaultRequest request) {
      return blockingUnaryCall(
          getChannel(), getSaveAliveMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.DefaultResponse pushEthTransactions(com.thanos.api.proto.push.EthTransactionsPushDTO request) {
      return blockingUnaryCall(
          getChannel(), getPushEthTransactionsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.DefaultResponse pushGlobalNodeEvents(com.thanos.api.proto.push.GlobalNodeEventsPushDTO request) {
      return blockingUnaryCall(
          getChannel(), getPushGlobalNodeEventsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.BytesObject getGlobalNodeEvent(com.thanos.api.proto.push.BytesObject request) {
      return blockingUnaryCall(
          getChannel(), getGetGlobalNodeEventMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.BytesObject getGlobalNodeEventReceipt(com.thanos.api.proto.push.BytesObject request) {
      return blockingUnaryCall(
          getChannel(), getGetGlobalNodeEventReceiptMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.BytesObject getEpochState(com.thanos.api.proto.push.DefaultRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetEpochStateMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.LongObject getLatestBeExecutedNum(com.thanos.api.proto.push.DefaultRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetLatestBeExecutedNumMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.LongObject getLatestConsensusNumber(com.thanos.api.proto.push.DefaultRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetLatestConsensusNumberMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.LongObject getCurrentCommitRound(com.thanos.api.proto.push.DefaultRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetCurrentCommitRoundMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.BytesObject getEthTransactionByHash(com.thanos.api.proto.push.BytesObject request) {
      return blockingUnaryCall(
          getChannel(), getGetEthTransactionByHashMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.ListBytesObject getEthTransactionsByHashes(com.thanos.api.proto.push.ListBytesObject request) {
      return blockingUnaryCall(
          getChannel(), getGetEthTransactionsByHashesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.BlockBytesObject getBlockByNumber(com.thanos.api.proto.push.LongObject request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByNumberMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.BytesObject getEventDataByNumber(com.thanos.api.proto.push.LongObject request) {
      return blockingUnaryCall(
          getChannel(), getGetEventDataByNumberMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.thanos.api.proto.push.BytesObject ethCall(com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO request) {
      return blockingUnaryCall(
          getChannel(), getEthCallMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   *服务接口定义，服务端和客户端都要遵循该接口进行通信
   * </pre>
   */
  public static final class PushServiceFutureStub extends io.grpc.stub.AbstractFutureStub<PushServiceFutureStub> {
    private PushServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PushServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PushServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.DefaultResponse> saveAlive(
        com.thanos.api.proto.push.DefaultRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSaveAliveMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.DefaultResponse> pushEthTransactions(
        com.thanos.api.proto.push.EthTransactionsPushDTO request) {
      return futureUnaryCall(
          getChannel().newCall(getPushEthTransactionsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.DefaultResponse> pushGlobalNodeEvents(
        com.thanos.api.proto.push.GlobalNodeEventsPushDTO request) {
      return futureUnaryCall(
          getChannel().newCall(getPushGlobalNodeEventsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.BytesObject> getGlobalNodeEvent(
        com.thanos.api.proto.push.BytesObject request) {
      return futureUnaryCall(
          getChannel().newCall(getGetGlobalNodeEventMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.BytesObject> getGlobalNodeEventReceipt(
        com.thanos.api.proto.push.BytesObject request) {
      return futureUnaryCall(
          getChannel().newCall(getGetGlobalNodeEventReceiptMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.BytesObject> getEpochState(
        com.thanos.api.proto.push.DefaultRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetEpochStateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.LongObject> getLatestBeExecutedNum(
        com.thanos.api.proto.push.DefaultRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetLatestBeExecutedNumMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.LongObject> getLatestConsensusNumber(
        com.thanos.api.proto.push.DefaultRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetLatestConsensusNumberMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.LongObject> getCurrentCommitRound(
        com.thanos.api.proto.push.DefaultRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetCurrentCommitRoundMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.BytesObject> getEthTransactionByHash(
        com.thanos.api.proto.push.BytesObject request) {
      return futureUnaryCall(
          getChannel().newCall(getGetEthTransactionByHashMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.ListBytesObject> getEthTransactionsByHashes(
        com.thanos.api.proto.push.ListBytesObject request) {
      return futureUnaryCall(
          getChannel().newCall(getGetEthTransactionsByHashesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.BlockBytesObject> getBlockByNumber(
        com.thanos.api.proto.push.LongObject request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByNumberMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.BytesObject> getEventDataByNumber(
        com.thanos.api.proto.push.LongObject request) {
      return futureUnaryCall(
          getChannel().newCall(getGetEventDataByNumberMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.push.BytesObject> ethCall(
        com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO request) {
      return futureUnaryCall(
          getChannel().newCall(getEthCallMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SAVE_ALIVE = 0;
  private static final int METHODID_PUSH_ETH_TRANSACTIONS = 1;
  private static final int METHODID_PUSH_GLOBAL_NODE_EVENTS = 2;
  private static final int METHODID_GET_GLOBAL_NODE_EVENT = 3;
  private static final int METHODID_GET_GLOBAL_NODE_EVENT_RECEIPT = 4;
  private static final int METHODID_GET_EPOCH_STATE = 5;
  private static final int METHODID_GET_LATEST_BE_EXECUTED_NUM = 6;
  private static final int METHODID_GET_LATEST_CONSENSUS_NUMBER = 7;
  private static final int METHODID_GET_CURRENT_COMMIT_ROUND = 8;
  private static final int METHODID_GET_ETH_TRANSACTION_BY_HASH = 9;
  private static final int METHODID_GET_ETH_TRANSACTIONS_BY_HASHES = 10;
  private static final int METHODID_GET_BLOCK_BY_NUMBER = 11;
  private static final int METHODID_GET_EVENT_DATA_BY_NUMBER = 12;
  private static final int METHODID_ETH_CALL = 13;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PushServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PushServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SAVE_ALIVE:
          serviceImpl.saveAlive((com.thanos.api.proto.push.DefaultRequest) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.DefaultResponse>) responseObserver);
          break;
        case METHODID_PUSH_ETH_TRANSACTIONS:
          serviceImpl.pushEthTransactions((com.thanos.api.proto.push.EthTransactionsPushDTO) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.DefaultResponse>) responseObserver);
          break;
        case METHODID_PUSH_GLOBAL_NODE_EVENTS:
          serviceImpl.pushGlobalNodeEvents((com.thanos.api.proto.push.GlobalNodeEventsPushDTO) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.DefaultResponse>) responseObserver);
          break;
        case METHODID_GET_GLOBAL_NODE_EVENT:
          serviceImpl.getGlobalNodeEvent((com.thanos.api.proto.push.BytesObject) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject>) responseObserver);
          break;
        case METHODID_GET_GLOBAL_NODE_EVENT_RECEIPT:
          serviceImpl.getGlobalNodeEventReceipt((com.thanos.api.proto.push.BytesObject) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject>) responseObserver);
          break;
        case METHODID_GET_EPOCH_STATE:
          serviceImpl.getEpochState((com.thanos.api.proto.push.DefaultRequest) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject>) responseObserver);
          break;
        case METHODID_GET_LATEST_BE_EXECUTED_NUM:
          serviceImpl.getLatestBeExecutedNum((com.thanos.api.proto.push.DefaultRequest) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.LongObject>) responseObserver);
          break;
        case METHODID_GET_LATEST_CONSENSUS_NUMBER:
          serviceImpl.getLatestConsensusNumber((com.thanos.api.proto.push.DefaultRequest) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.LongObject>) responseObserver);
          break;
        case METHODID_GET_CURRENT_COMMIT_ROUND:
          serviceImpl.getCurrentCommitRound((com.thanos.api.proto.push.DefaultRequest) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.LongObject>) responseObserver);
          break;
        case METHODID_GET_ETH_TRANSACTION_BY_HASH:
          serviceImpl.getEthTransactionByHash((com.thanos.api.proto.push.BytesObject) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject>) responseObserver);
          break;
        case METHODID_GET_ETH_TRANSACTIONS_BY_HASHES:
          serviceImpl.getEthTransactionsByHashes((com.thanos.api.proto.push.ListBytesObject) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.ListBytesObject>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_NUMBER:
          serviceImpl.getBlockByNumber((com.thanos.api.proto.push.LongObject) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BlockBytesObject>) responseObserver);
          break;
        case METHODID_GET_EVENT_DATA_BY_NUMBER:
          serviceImpl.getEventDataByNumber((com.thanos.api.proto.push.LongObject) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject>) responseObserver);
          break;
        case METHODID_ETH_CALL:
          serviceImpl.ethCall((com.thanos.api.proto.push.EthTransactionsPushDTO.EthTransactionPushDTO) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.push.BytesObject>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class PushServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PushServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.thanos.api.proto.push.PushGrpcTransport.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PushService");
    }
  }

  private static final class PushServiceFileDescriptorSupplier
      extends PushServiceBaseDescriptorSupplier {
    PushServiceFileDescriptorSupplier() {}
  }

  private static final class PushServiceMethodDescriptorSupplier
      extends PushServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PushServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PushServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PushServiceFileDescriptorSupplier())
              .addMethod(getSaveAliveMethod())
              .addMethod(getPushEthTransactionsMethod())
              .addMethod(getPushGlobalNodeEventsMethod())
              .addMethod(getGetGlobalNodeEventMethod())
              .addMethod(getGetGlobalNodeEventReceiptMethod())
              .addMethod(getGetEpochStateMethod())
              .addMethod(getGetLatestBeExecutedNumMethod())
              .addMethod(getGetLatestConsensusNumberMethod())
              .addMethod(getGetCurrentCommitRoundMethod())
              .addMethod(getGetEthTransactionByHashMethod())
              .addMethod(getGetEthTransactionsByHashesMethod())
              .addMethod(getGetBlockByNumberMethod())
              .addMethod(getGetEventDataByNumberMethod())
              .addMethod(getEthCallMethod())
              .build();
        }
      }
    }
    return result;
  }
}
