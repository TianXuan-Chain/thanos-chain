package com.thanos.api.proto.sync;

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
    comments = "Source: sync.proto")
public final class SyncServiceGrpc {

  private SyncServiceGrpc() {}

  public static final String SERVICE_NAME = "com.thanos.api.proto.sync.SyncService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.thanos.api.proto.sync.BlockBytesObject,
      com.thanos.api.proto.sync.DefaultResponse> getSyncBlockMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "syncBlock",
      requestType = com.thanos.api.proto.sync.BlockBytesObject.class,
      responseType = com.thanos.api.proto.sync.DefaultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.thanos.api.proto.sync.BlockBytesObject,
      com.thanos.api.proto.sync.DefaultResponse> getSyncBlockMethod() {
    io.grpc.MethodDescriptor<com.thanos.api.proto.sync.BlockBytesObject, com.thanos.api.proto.sync.DefaultResponse> getSyncBlockMethod;
    if ((getSyncBlockMethod = SyncServiceGrpc.getSyncBlockMethod) == null) {
      synchronized (SyncServiceGrpc.class) {
        if ((getSyncBlockMethod = SyncServiceGrpc.getSyncBlockMethod) == null) {
          SyncServiceGrpc.getSyncBlockMethod = getSyncBlockMethod =
              io.grpc.MethodDescriptor.<com.thanos.api.proto.sync.BlockBytesObject, com.thanos.api.proto.sync.DefaultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "syncBlock"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.sync.BlockBytesObject.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.thanos.api.proto.sync.DefaultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SyncServiceMethodDescriptorSupplier("syncBlock"))
              .build();
        }
      }
    }
    return getSyncBlockMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SyncServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SyncServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SyncServiceStub>() {
        @java.lang.Override
        public SyncServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SyncServiceStub(channel, callOptions);
        }
      };
    return SyncServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SyncServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SyncServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SyncServiceBlockingStub>() {
        @java.lang.Override
        public SyncServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SyncServiceBlockingStub(channel, callOptions);
        }
      };
    return SyncServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SyncServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SyncServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SyncServiceFutureStub>() {
        @java.lang.Override
        public SyncServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SyncServiceFutureStub(channel, callOptions);
        }
      };
    return SyncServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   *服务接口定义，服务端和客户端都要遵循该接口进行通信
   * </pre>
   */
  public static abstract class SyncServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void syncBlock(com.thanos.api.proto.sync.BlockBytesObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.sync.DefaultResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSyncBlockMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSyncBlockMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.thanos.api.proto.sync.BlockBytesObject,
                com.thanos.api.proto.sync.DefaultResponse>(
                  this, METHODID_SYNC_BLOCK)))
          .build();
    }
  }

  /**
   * <pre>
   *服务接口定义，服务端和客户端都要遵循该接口进行通信
   * </pre>
   */
  public static final class SyncServiceStub extends io.grpc.stub.AbstractAsyncStub<SyncServiceStub> {
    private SyncServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SyncServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SyncServiceStub(channel, callOptions);
    }

    /**
     */
    public void syncBlock(com.thanos.api.proto.sync.BlockBytesObject request,
        io.grpc.stub.StreamObserver<com.thanos.api.proto.sync.DefaultResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSyncBlockMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   *服务接口定义，服务端和客户端都要遵循该接口进行通信
   * </pre>
   */
  public static final class SyncServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<SyncServiceBlockingStub> {
    private SyncServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SyncServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SyncServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.thanos.api.proto.sync.DefaultResponse syncBlock(com.thanos.api.proto.sync.BlockBytesObject request) {
      return blockingUnaryCall(
          getChannel(), getSyncBlockMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   *服务接口定义，服务端和客户端都要遵循该接口进行通信
   * </pre>
   */
  public static final class SyncServiceFutureStub extends io.grpc.stub.AbstractFutureStub<SyncServiceFutureStub> {
    private SyncServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SyncServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SyncServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.thanos.api.proto.sync.DefaultResponse> syncBlock(
        com.thanos.api.proto.sync.BlockBytesObject request) {
      return futureUnaryCall(
          getChannel().newCall(getSyncBlockMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SYNC_BLOCK = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SyncServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SyncServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SYNC_BLOCK:
          serviceImpl.syncBlock((com.thanos.api.proto.sync.BlockBytesObject) request,
              (io.grpc.stub.StreamObserver<com.thanos.api.proto.sync.DefaultResponse>) responseObserver);
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

  private static abstract class SyncServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SyncServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.thanos.api.proto.sync.SyncGrpcTransport.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SyncService");
    }
  }

  private static final class SyncServiceFileDescriptorSupplier
      extends SyncServiceBaseDescriptorSupplier {
    SyncServiceFileDescriptorSupplier() {}
  }

  private static final class SyncServiceMethodDescriptorSupplier
      extends SyncServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SyncServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (SyncServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SyncServiceFileDescriptorSupplier())
              .addMethod(getSyncBlockMethod())
              .build();
        }
      }
    }
    return result;
  }
}
