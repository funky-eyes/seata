// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: globalLockQueryResponse.proto

package org.apache.seata.core.serializer.protobuf.generated;

/**
 * Protobuf type {@code org.apache.seata.protocol.protobuf.GlobalLockQueryResponseProto}
 */
public final class GlobalLockQueryResponseProto extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:org.apache.seata.protocol.protobuf.GlobalLockQueryResponseProto)
    GlobalLockQueryResponseProtoOrBuilder {
private static final long serialVersionUID = 0L;
  // Use GlobalLockQueryResponseProto.newBuilder() to construct.
  private GlobalLockQueryResponseProto(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private GlobalLockQueryResponseProto() {
  }

  @Override
  @SuppressWarnings({"unused"})
  protected Object newInstance(
      UnusedPrivateParameter unused) {
    return new GlobalLockQueryResponseProto();
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private GlobalLockQueryResponseProto(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new NullPointerException();
    }
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
            AbstractTransactionResponseProto.Builder subBuilder = null;
            if (abstractTransactionResponse_ != null) {
              subBuilder = abstractTransactionResponse_.toBuilder();
            }
            abstractTransactionResponse_ = input.readMessage(AbstractTransactionResponseProto.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(abstractTransactionResponse_);
              abstractTransactionResponse_ = subBuilder.buildPartial();
            }

            break;
          }
          case 16: {

            lockable_ = input.readBool();
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
    } catch (com.google.protobuf.UninitializedMessageException e) {
      throw e.asInvalidProtocolBufferException().setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return GlobalLockQueryResponse.internal_static_org_apache_seata_protocol_protobuf_GlobalLockQueryResponseProto_descriptor;
  }

  @Override
  protected FieldAccessorTable
      internalGetFieldAccessorTable() {
    return GlobalLockQueryResponse.internal_static_org_apache_seata_protocol_protobuf_GlobalLockQueryResponseProto_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            GlobalLockQueryResponseProto.class, Builder.class);
  }

  public static final int ABSTRACTTRANSACTIONRESPONSE_FIELD_NUMBER = 1;
  private AbstractTransactionResponseProto abstractTransactionResponse_;
  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
   * @return Whether the abstractTransactionResponse field is set.
   */
  @Override
  public boolean hasAbstractTransactionResponse() {
    return abstractTransactionResponse_ != null;
  }
  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
   * @return The abstractTransactionResponse.
   */
  @Override
  public AbstractTransactionResponseProto getAbstractTransactionResponse() {
    return abstractTransactionResponse_ == null ? AbstractTransactionResponseProto.getDefaultInstance() : abstractTransactionResponse_;
  }
  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
   */
  @Override
  public AbstractTransactionResponseProtoOrBuilder getAbstractTransactionResponseOrBuilder() {
    return getAbstractTransactionResponse();
  }

  public static final int LOCKABLE_FIELD_NUMBER = 2;
  private boolean lockable_;
  /**
   * <code>bool lockable = 2;</code>
   * @return The lockable.
   */
  @Override
  public boolean getLockable() {
    return lockable_;
  }

  private byte memoizedIsInitialized = -1;
  @Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (abstractTransactionResponse_ != null) {
      output.writeMessage(1, getAbstractTransactionResponse());
    }
    if (lockable_ != false) {
      output.writeBool(2, lockable_);
    }
    unknownFields.writeTo(output);
  }

  @Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (abstractTransactionResponse_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getAbstractTransactionResponse());
    }
    if (lockable_ != false) {
      size += com.google.protobuf.CodedOutputStream
        .computeBoolSize(2, lockable_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof GlobalLockQueryResponseProto)) {
      return super.equals(obj);
    }
    GlobalLockQueryResponseProto other = (GlobalLockQueryResponseProto) obj;

    if (hasAbstractTransactionResponse() != other.hasAbstractTransactionResponse()) return false;
    if (hasAbstractTransactionResponse()) {
      if (!getAbstractTransactionResponse()
          .equals(other.getAbstractTransactionResponse())) return false;
    }
    if (getLockable()
        != other.getLockable()) return false;
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (hasAbstractTransactionResponse()) {
      hash = (37 * hash) + ABSTRACTTRANSACTIONRESPONSE_FIELD_NUMBER;
      hash = (53 * hash) + getAbstractTransactionResponse().hashCode();
    }
    hash = (37 * hash) + LOCKABLE_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(
        getLockable());
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static GlobalLockQueryResponseProto parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static GlobalLockQueryResponseProto parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static GlobalLockQueryResponseProto parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static GlobalLockQueryResponseProto parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static GlobalLockQueryResponseProto parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static GlobalLockQueryResponseProto parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static GlobalLockQueryResponseProto parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static GlobalLockQueryResponseProto parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static GlobalLockQueryResponseProto parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static GlobalLockQueryResponseProto parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static GlobalLockQueryResponseProto parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static GlobalLockQueryResponseProto parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(GlobalLockQueryResponseProto prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @Override
  protected Builder newBuilderForType(
      BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code org.apache.seata.protocol.protobuf.GlobalLockQueryResponseProto}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:org.apache.seata.protocol.protobuf.GlobalLockQueryResponseProto)
      GlobalLockQueryResponseProtoOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return GlobalLockQueryResponse.internal_static_org_apache_seata_protocol_protobuf_GlobalLockQueryResponseProto_descriptor;
    }

    @Override
    protected FieldAccessorTable
        internalGetFieldAccessorTable() {
      return GlobalLockQueryResponse.internal_static_org_apache_seata_protocol_protobuf_GlobalLockQueryResponseProto_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              GlobalLockQueryResponseProto.class, Builder.class);
    }

    // Construct using org.apache.seata.core.serializer.protobuf.generated.GlobalLockQueryResponseProto.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @Override
    public Builder clear() {
      super.clear();
      if (abstractTransactionResponseBuilder_ == null) {
        abstractTransactionResponse_ = null;
      } else {
        abstractTransactionResponse_ = null;
        abstractTransactionResponseBuilder_ = null;
      }
      lockable_ = false;

      return this;
    }

    @Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return GlobalLockQueryResponse.internal_static_org_apache_seata_protocol_protobuf_GlobalLockQueryResponseProto_descriptor;
    }

    @Override
    public GlobalLockQueryResponseProto getDefaultInstanceForType() {
      return GlobalLockQueryResponseProto.getDefaultInstance();
    }

    @Override
    public GlobalLockQueryResponseProto build() {
      GlobalLockQueryResponseProto result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @Override
    public GlobalLockQueryResponseProto buildPartial() {
      GlobalLockQueryResponseProto result = new GlobalLockQueryResponseProto(this);
      if (abstractTransactionResponseBuilder_ == null) {
        result.abstractTransactionResponse_ = abstractTransactionResponse_;
      } else {
        result.abstractTransactionResponse_ = abstractTransactionResponseBuilder_.build();
      }
      result.lockable_ = lockable_;
      onBuilt();
      return result;
    }

    @Override
    public Builder clone() {
      return super.clone();
    }
    @Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        Object value) {
      return super.setField(field, value);
    }
    @Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        Object value) {
      return super.addRepeatedField(field, value);
    }
    @Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof GlobalLockQueryResponseProto) {
        return mergeFrom((GlobalLockQueryResponseProto)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(GlobalLockQueryResponseProto other) {
      if (other == GlobalLockQueryResponseProto.getDefaultInstance()) return this;
      if (other.hasAbstractTransactionResponse()) {
        mergeAbstractTransactionResponse(other.getAbstractTransactionResponse());
      }
      if (other.getLockable() != false) {
        setLockable(other.getLockable());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @Override
    public final boolean isInitialized() {
      return true;
    }

    @Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      GlobalLockQueryResponseProto parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (GlobalLockQueryResponseProto) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private AbstractTransactionResponseProto abstractTransactionResponse_;
    private com.google.protobuf.SingleFieldBuilderV3<
        AbstractTransactionResponseProto, AbstractTransactionResponseProto.Builder, AbstractTransactionResponseProtoOrBuilder> abstractTransactionResponseBuilder_;
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
     * @return Whether the abstractTransactionResponse field is set.
     */
    public boolean hasAbstractTransactionResponse() {
      return abstractTransactionResponseBuilder_ != null || abstractTransactionResponse_ != null;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
     * @return The abstractTransactionResponse.
     */
    public AbstractTransactionResponseProto getAbstractTransactionResponse() {
      if (abstractTransactionResponseBuilder_ == null) {
        return abstractTransactionResponse_ == null ? AbstractTransactionResponseProto.getDefaultInstance() : abstractTransactionResponse_;
      } else {
        return abstractTransactionResponseBuilder_.getMessage();
      }
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
     */
    public Builder setAbstractTransactionResponse(AbstractTransactionResponseProto value) {
      if (abstractTransactionResponseBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        abstractTransactionResponse_ = value;
        onChanged();
      } else {
        abstractTransactionResponseBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
     */
    public Builder setAbstractTransactionResponse(
        AbstractTransactionResponseProto.Builder builderForValue) {
      if (abstractTransactionResponseBuilder_ == null) {
        abstractTransactionResponse_ = builderForValue.build();
        onChanged();
      } else {
        abstractTransactionResponseBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
     */
    public Builder mergeAbstractTransactionResponse(AbstractTransactionResponseProto value) {
      if (abstractTransactionResponseBuilder_ == null) {
        if (abstractTransactionResponse_ != null) {
          abstractTransactionResponse_ =
            AbstractTransactionResponseProto.newBuilder(abstractTransactionResponse_).mergeFrom(value).buildPartial();
        } else {
          abstractTransactionResponse_ = value;
        }
        onChanged();
      } else {
        abstractTransactionResponseBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
     */
    public Builder clearAbstractTransactionResponse() {
      if (abstractTransactionResponseBuilder_ == null) {
        abstractTransactionResponse_ = null;
        onChanged();
      } else {
        abstractTransactionResponse_ = null;
        abstractTransactionResponseBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
     */
    public AbstractTransactionResponseProto.Builder getAbstractTransactionResponseBuilder() {
      
      onChanged();
      return getAbstractTransactionResponseFieldBuilder().getBuilder();
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
     */
    public AbstractTransactionResponseProtoOrBuilder getAbstractTransactionResponseOrBuilder() {
      if (abstractTransactionResponseBuilder_ != null) {
        return abstractTransactionResponseBuilder_.getMessageOrBuilder();
      } else {
        return abstractTransactionResponse_ == null ?
            AbstractTransactionResponseProto.getDefaultInstance() : abstractTransactionResponse_;
      }
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractTransactionResponseProto abstractTransactionResponse = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        AbstractTransactionResponseProto, AbstractTransactionResponseProto.Builder, AbstractTransactionResponseProtoOrBuilder>
        getAbstractTransactionResponseFieldBuilder() {
      if (abstractTransactionResponseBuilder_ == null) {
        abstractTransactionResponseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            AbstractTransactionResponseProto, AbstractTransactionResponseProto.Builder, AbstractTransactionResponseProtoOrBuilder>(
                getAbstractTransactionResponse(),
                getParentForChildren(),
                isClean());
        abstractTransactionResponse_ = null;
      }
      return abstractTransactionResponseBuilder_;
    }

    private boolean lockable_ ;
    /**
     * <code>bool lockable = 2;</code>
     * @return The lockable.
     */
    @Override
    public boolean getLockable() {
      return lockable_;
    }
    /**
     * <code>bool lockable = 2;</code>
     * @param value The lockable to set.
     * @return This builder for chaining.
     */
    public Builder setLockable(boolean value) {
      
      lockable_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>bool lockable = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearLockable() {
      
      lockable_ = false;
      onChanged();
      return this;
    }
    @Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:org.apache.seata.protocol.protobuf.GlobalLockQueryResponseProto)
  }

  // @@protoc_insertion_point(class_scope:org.apache.seata.protocol.protobuf.GlobalLockQueryResponseProto)
  private static final GlobalLockQueryResponseProto DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new GlobalLockQueryResponseProto();
  }

  public static GlobalLockQueryResponseProto getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<GlobalLockQueryResponseProto>
      PARSER = new com.google.protobuf.AbstractParser<GlobalLockQueryResponseProto>() {
    @Override
    public GlobalLockQueryResponseProto parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new GlobalLockQueryResponseProto(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<GlobalLockQueryResponseProto> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<GlobalLockQueryResponseProto> getParserForType() {
    return PARSER;
  }

  @Override
  public GlobalLockQueryResponseProto getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

