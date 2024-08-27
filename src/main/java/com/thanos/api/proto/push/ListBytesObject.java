// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: push.proto

package com.thanos.api.proto.push;

/**
 * Protobuf type {@code com.thanos.api.proto.push.ListBytesObject}
 */
public final class ListBytesObject extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:com.thanos.api.proto.push.ListBytesObject)
    ListBytesObjectOrBuilder {
private static final long serialVersionUID = 0L;
  // Use ListBytesObject.newBuilder() to construct.
  private ListBytesObject(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private ListBytesObject() {
    value_ = java.util.Collections.emptyList();
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new ListBytesObject();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private ListBytesObject(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            if (!((mutable_bitField0_ & 0x00000001) != 0)) {
              value_ = new java.util.ArrayList<com.google.protobuf.ByteString>();
              mutable_bitField0_ |= 0x00000001;
            }
            value_.add(input.readBytes());
            break;
          }
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      if (((mutable_bitField0_ & 0x00000001) != 0)) {
        value_ = java.util.Collections.unmodifiableList(value_); // C
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.thanos.api.proto.push.PushGrpcTransport.internal_static_com_thanos_api_proto_push_ListBytesObject_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.thanos.api.proto.push.PushGrpcTransport.internal_static_com_thanos_api_proto_push_ListBytesObject_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.thanos.api.proto.push.ListBytesObject.class, com.thanos.api.proto.push.ListBytesObject.Builder.class);
  }

  public static final int VALUE_FIELD_NUMBER = 1;
  private java.util.List<com.google.protobuf.ByteString> value_;
  /**
   * <code>repeated bytes value = 1;</code>
   * @return A list containing the value.
   */
  @java.lang.Override
  public java.util.List<com.google.protobuf.ByteString>
      getValueList() {
    return value_;
  }
  /**
   * <code>repeated bytes value = 1;</code>
   * @return The count of value.
   */
  public int getValueCount() {
    return value_.size();
  }
  /**
   * <code>repeated bytes value = 1;</code>
   * @param index The index of the element to return.
   * @return The value at the given index.
   */
  public com.google.protobuf.ByteString getValue(int index) {
    return value_.get(index);
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    for (int i = 0; i < value_.size(); i++) {
      output.writeBytes(1, value_.get(i));
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    {
      int dataSize = 0;
      for (int i = 0; i < value_.size(); i++) {
        dataSize += com.google.protobuf.CodedOutputStream
          .computeBytesSizeNoTag(value_.get(i));
      }
      size += dataSize;
      size += 1 * getValueList().size();
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof com.thanos.api.proto.push.ListBytesObject)) {
      return super.equals(obj);
    }
    com.thanos.api.proto.push.ListBytesObject other = (com.thanos.api.proto.push.ListBytesObject) obj;

    if (!getValueList()
        .equals(other.getValueList())) return false;
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (getValueCount() > 0) {
      hash = (37 * hash) + VALUE_FIELD_NUMBER;
      hash = (53 * hash) + getValueList().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.thanos.api.proto.push.ListBytesObject parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.thanos.api.proto.push.ListBytesObject parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.thanos.api.proto.push.ListBytesObject parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.thanos.api.proto.push.ListBytesObject parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.thanos.api.proto.push.ListBytesObject parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.thanos.api.proto.push.ListBytesObject parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.thanos.api.proto.push.ListBytesObject parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.thanos.api.proto.push.ListBytesObject parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.thanos.api.proto.push.ListBytesObject parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.thanos.api.proto.push.ListBytesObject parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.thanos.api.proto.push.ListBytesObject parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.thanos.api.proto.push.ListBytesObject parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.thanos.api.proto.push.ListBytesObject prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code com.thanos.api.proto.push.ListBytesObject}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:com.thanos.api.proto.push.ListBytesObject)
      com.thanos.api.proto.push.ListBytesObjectOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.thanos.api.proto.push.PushGrpcTransport.internal_static_com_thanos_api_proto_push_ListBytesObject_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.thanos.api.proto.push.PushGrpcTransport.internal_static_com_thanos_api_proto_push_ListBytesObject_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.thanos.api.proto.push.ListBytesObject.class, com.thanos.api.proto.push.ListBytesObject.Builder.class);
    }

    // Construct using com.thanos.api.proto.push.ListBytesObject.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      value_ = java.util.Collections.emptyList();
      bitField0_ = (bitField0_ & ~0x00000001);
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.thanos.api.proto.push.PushGrpcTransport.internal_static_com_thanos_api_proto_push_ListBytesObject_descriptor;
    }

    @java.lang.Override
    public com.thanos.api.proto.push.ListBytesObject getDefaultInstanceForType() {
      return com.thanos.api.proto.push.ListBytesObject.getDefaultInstance();
    }

    @java.lang.Override
    public com.thanos.api.proto.push.ListBytesObject build() {
      com.thanos.api.proto.push.ListBytesObject result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.thanos.api.proto.push.ListBytesObject buildPartial() {
      com.thanos.api.proto.push.ListBytesObject result = new com.thanos.api.proto.push.ListBytesObject(this);
      int from_bitField0_ = bitField0_;
      if (((bitField0_ & 0x00000001) != 0)) {
        value_ = java.util.Collections.unmodifiableList(value_);
        bitField0_ = (bitField0_ & ~0x00000001);
      }
      result.value_ = value_;
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.thanos.api.proto.push.ListBytesObject) {
        return mergeFrom((com.thanos.api.proto.push.ListBytesObject)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.thanos.api.proto.push.ListBytesObject other) {
      if (other == com.thanos.api.proto.push.ListBytesObject.getDefaultInstance()) return this;
      if (!other.value_.isEmpty()) {
        if (value_.isEmpty()) {
          value_ = other.value_;
          bitField0_ = (bitField0_ & ~0x00000001);
        } else {
          ensureValueIsMutable();
          value_.addAll(other.value_);
        }
        onChanged();
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      com.thanos.api.proto.push.ListBytesObject parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.thanos.api.proto.push.ListBytesObject) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private java.util.List<com.google.protobuf.ByteString> value_ = java.util.Collections.emptyList();
    private void ensureValueIsMutable() {
      if (!((bitField0_ & 0x00000001) != 0)) {
        value_ = new java.util.ArrayList<com.google.protobuf.ByteString>(value_);
        bitField0_ |= 0x00000001;
       }
    }
    /**
     * <code>repeated bytes value = 1;</code>
     * @return A list containing the value.
     */
    public java.util.List<com.google.protobuf.ByteString>
        getValueList() {
      return ((bitField0_ & 0x00000001) != 0) ?
               java.util.Collections.unmodifiableList(value_) : value_;
    }
    /**
     * <code>repeated bytes value = 1;</code>
     * @return The count of value.
     */
    public int getValueCount() {
      return value_.size();
    }
    /**
     * <code>repeated bytes value = 1;</code>
     * @param index The index of the element to return.
     * @return The value at the given index.
     */
    public com.google.protobuf.ByteString getValue(int index) {
      return value_.get(index);
    }
    /**
     * <code>repeated bytes value = 1;</code>
     * @param index The index to set the value at.
     * @param value The value to set.
     * @return This builder for chaining.
     */
    public Builder setValue(
        int index, com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureValueIsMutable();
      value_.set(index, value);
      onChanged();
      return this;
    }
    /**
     * <code>repeated bytes value = 1;</code>
     * @param value The value to add.
     * @return This builder for chaining.
     */
    public Builder addValue(com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureValueIsMutable();
      value_.add(value);
      onChanged();
      return this;
    }
    /**
     * <code>repeated bytes value = 1;</code>
     * @param values The value to add.
     * @return This builder for chaining.
     */
    public Builder addAllValue(
        java.lang.Iterable<? extends com.google.protobuf.ByteString> values) {
      ensureValueIsMutable();
      com.google.protobuf.AbstractMessageLite.Builder.addAll(
          values, value_);
      onChanged();
      return this;
    }
    /**
     * <code>repeated bytes value = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearValue() {
      value_ = java.util.Collections.emptyList();
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:com.thanos.api.proto.push.ListBytesObject)
  }

  // @@protoc_insertion_point(class_scope:com.thanos.api.proto.push.ListBytesObject)
  private static final com.thanos.api.proto.push.ListBytesObject DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.thanos.api.proto.push.ListBytesObject();
  }

  public static com.thanos.api.proto.push.ListBytesObject getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ListBytesObject>
      PARSER = new com.google.protobuf.AbstractParser<ListBytesObject>() {
    @java.lang.Override
    public ListBytesObject parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new ListBytesObject(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<ListBytesObject> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<ListBytesObject> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.thanos.api.proto.push.ListBytesObject getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
