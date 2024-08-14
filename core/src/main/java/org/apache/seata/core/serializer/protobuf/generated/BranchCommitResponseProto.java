// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: branchCommitResponse.proto

package org.apache.seata.core.serializer.protobuf.generated;

/**
 * <pre>
 * PublishRequest is a publish request.
 * </pre>
 *
 * Protobuf type {@code org.apache.seata.protocol.protobuf.BranchCommitResponseProto}
 */
public final class BranchCommitResponseProto extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:org.apache.seata.protocol.protobuf.BranchCommitResponseProto)
    BranchCommitResponseProtoOrBuilder {
private static final long serialVersionUID = 0L;
  // Use BranchCommitResponseProto.newBuilder() to construct.
  private BranchCommitResponseProto(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private BranchCommitResponseProto() {
  }

  @Override
  @SuppressWarnings({"unused"})
  protected Object newInstance(
      UnusedPrivateParameter unused) {
    return new BranchCommitResponseProto();
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private BranchCommitResponseProto(
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
            AbstractBranchEndResponseProto.Builder subBuilder = null;
            if (abstractBranchEndResponse_ != null) {
              subBuilder = abstractBranchEndResponse_.toBuilder();
            }
            abstractBranchEndResponse_ = input.readMessage(AbstractBranchEndResponseProto.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(abstractBranchEndResponse_);
              abstractBranchEndResponse_ = subBuilder.buildPartial();
            }

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
    return BranchCommitResponse.internal_static_org_apache_seata_protocol_protobuf_BranchCommitResponseProto_descriptor;
  }

  @Override
  protected FieldAccessorTable
      internalGetFieldAccessorTable() {
    return BranchCommitResponse.internal_static_org_apache_seata_protocol_protobuf_BranchCommitResponseProto_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            BranchCommitResponseProto.class, Builder.class);
  }

  public static final int ABSTRACTBRANCHENDRESPONSE_FIELD_NUMBER = 1;
  private AbstractBranchEndResponseProto abstractBranchEndResponse_;
  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
   * @return Whether the abstractBranchEndResponse field is set.
   */
  @Override
  public boolean hasAbstractBranchEndResponse() {
    return abstractBranchEndResponse_ != null;
  }
  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
   * @return The abstractBranchEndResponse.
   */
  @Override
  public AbstractBranchEndResponseProto getAbstractBranchEndResponse() {
    return abstractBranchEndResponse_ == null ? AbstractBranchEndResponseProto.getDefaultInstance() : abstractBranchEndResponse_;
  }
  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
   */
  @Override
  public AbstractBranchEndResponseProtoOrBuilder getAbstractBranchEndResponseOrBuilder() {
    return getAbstractBranchEndResponse();
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
    if (abstractBranchEndResponse_ != null) {
      output.writeMessage(1, getAbstractBranchEndResponse());
    }
    unknownFields.writeTo(output);
  }

  @Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (abstractBranchEndResponse_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getAbstractBranchEndResponse());
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
    if (!(obj instanceof BranchCommitResponseProto)) {
      return super.equals(obj);
    }
    BranchCommitResponseProto other = (BranchCommitResponseProto) obj;

    if (hasAbstractBranchEndResponse() != other.hasAbstractBranchEndResponse()) return false;
    if (hasAbstractBranchEndResponse()) {
      if (!getAbstractBranchEndResponse()
          .equals(other.getAbstractBranchEndResponse())) return false;
    }
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
    if (hasAbstractBranchEndResponse()) {
      hash = (37 * hash) + ABSTRACTBRANCHENDRESPONSE_FIELD_NUMBER;
      hash = (53 * hash) + getAbstractBranchEndResponse().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static BranchCommitResponseProto parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static BranchCommitResponseProto parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static BranchCommitResponseProto parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static BranchCommitResponseProto parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static BranchCommitResponseProto parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static BranchCommitResponseProto parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static BranchCommitResponseProto parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static BranchCommitResponseProto parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static BranchCommitResponseProto parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static BranchCommitResponseProto parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static BranchCommitResponseProto parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static BranchCommitResponseProto parseFrom(
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
  public static Builder newBuilder(BranchCommitResponseProto prototype) {
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
   * <pre>
   * PublishRequest is a publish request.
   * </pre>
   *
   * Protobuf type {@code org.apache.seata.protocol.protobuf.BranchCommitResponseProto}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:org.apache.seata.protocol.protobuf.BranchCommitResponseProto)
      BranchCommitResponseProtoOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return BranchCommitResponse.internal_static_org_apache_seata_protocol_protobuf_BranchCommitResponseProto_descriptor;
    }

    @Override
    protected FieldAccessorTable
        internalGetFieldAccessorTable() {
      return BranchCommitResponse.internal_static_org_apache_seata_protocol_protobuf_BranchCommitResponseProto_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              BranchCommitResponseProto.class, Builder.class);
    }

    // Construct using org.apache.seata.core.serializer.protobuf.generated.BranchCommitResponseProto.newBuilder()
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
      if (abstractBranchEndResponseBuilder_ == null) {
        abstractBranchEndResponse_ = null;
      } else {
        abstractBranchEndResponse_ = null;
        abstractBranchEndResponseBuilder_ = null;
      }
      return this;
    }

    @Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return BranchCommitResponse.internal_static_org_apache_seata_protocol_protobuf_BranchCommitResponseProto_descriptor;
    }

    @Override
    public BranchCommitResponseProto getDefaultInstanceForType() {
      return BranchCommitResponseProto.getDefaultInstance();
    }

    @Override
    public BranchCommitResponseProto build() {
      BranchCommitResponseProto result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @Override
    public BranchCommitResponseProto buildPartial() {
      BranchCommitResponseProto result = new BranchCommitResponseProto(this);
      if (abstractBranchEndResponseBuilder_ == null) {
        result.abstractBranchEndResponse_ = abstractBranchEndResponse_;
      } else {
        result.abstractBranchEndResponse_ = abstractBranchEndResponseBuilder_.build();
      }
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
      if (other instanceof BranchCommitResponseProto) {
        return mergeFrom((BranchCommitResponseProto)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(BranchCommitResponseProto other) {
      if (other == BranchCommitResponseProto.getDefaultInstance()) return this;
      if (other.hasAbstractBranchEndResponse()) {
        mergeAbstractBranchEndResponse(other.getAbstractBranchEndResponse());
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
      BranchCommitResponseProto parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (BranchCommitResponseProto) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private AbstractBranchEndResponseProto abstractBranchEndResponse_;
    private com.google.protobuf.SingleFieldBuilderV3<
        AbstractBranchEndResponseProto, AbstractBranchEndResponseProto.Builder, AbstractBranchEndResponseProtoOrBuilder> abstractBranchEndResponseBuilder_;
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     * @return Whether the abstractBranchEndResponse field is set.
     */
    public boolean hasAbstractBranchEndResponse() {
      return abstractBranchEndResponseBuilder_ != null || abstractBranchEndResponse_ != null;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     * @return The abstractBranchEndResponse.
     */
    public AbstractBranchEndResponseProto getAbstractBranchEndResponse() {
      if (abstractBranchEndResponseBuilder_ == null) {
        return abstractBranchEndResponse_ == null ? AbstractBranchEndResponseProto.getDefaultInstance() : abstractBranchEndResponse_;
      } else {
        return abstractBranchEndResponseBuilder_.getMessage();
      }
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public Builder setAbstractBranchEndResponse(AbstractBranchEndResponseProto value) {
      if (abstractBranchEndResponseBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        abstractBranchEndResponse_ = value;
        onChanged();
      } else {
        abstractBranchEndResponseBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public Builder setAbstractBranchEndResponse(
        AbstractBranchEndResponseProto.Builder builderForValue) {
      if (abstractBranchEndResponseBuilder_ == null) {
        abstractBranchEndResponse_ = builderForValue.build();
        onChanged();
      } else {
        abstractBranchEndResponseBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public Builder mergeAbstractBranchEndResponse(AbstractBranchEndResponseProto value) {
      if (abstractBranchEndResponseBuilder_ == null) {
        if (abstractBranchEndResponse_ != null) {
          abstractBranchEndResponse_ =
            AbstractBranchEndResponseProto.newBuilder(abstractBranchEndResponse_).mergeFrom(value).buildPartial();
        } else {
          abstractBranchEndResponse_ = value;
        }
        onChanged();
      } else {
        abstractBranchEndResponseBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public Builder clearAbstractBranchEndResponse() {
      if (abstractBranchEndResponseBuilder_ == null) {
        abstractBranchEndResponse_ = null;
        onChanged();
      } else {
        abstractBranchEndResponse_ = null;
        abstractBranchEndResponseBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public AbstractBranchEndResponseProto.Builder getAbstractBranchEndResponseBuilder() {
      
      onChanged();
      return getAbstractBranchEndResponseFieldBuilder().getBuilder();
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public AbstractBranchEndResponseProtoOrBuilder getAbstractBranchEndResponseOrBuilder() {
      if (abstractBranchEndResponseBuilder_ != null) {
        return abstractBranchEndResponseBuilder_.getMessageOrBuilder();
      } else {
        return abstractBranchEndResponse_ == null ?
            AbstractBranchEndResponseProto.getDefaultInstance() : abstractBranchEndResponse_;
      }
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        AbstractBranchEndResponseProto, AbstractBranchEndResponseProto.Builder, AbstractBranchEndResponseProtoOrBuilder>
        getAbstractBranchEndResponseFieldBuilder() {
      if (abstractBranchEndResponseBuilder_ == null) {
        abstractBranchEndResponseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            AbstractBranchEndResponseProto, AbstractBranchEndResponseProto.Builder, AbstractBranchEndResponseProtoOrBuilder>(
                getAbstractBranchEndResponse(),
                getParentForChildren(),
                isClean());
        abstractBranchEndResponse_ = null;
      }
      return abstractBranchEndResponseBuilder_;
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


    // @@protoc_insertion_point(builder_scope:org.apache.seata.protocol.protobuf.BranchCommitResponseProto)
  }

  // @@protoc_insertion_point(class_scope:org.apache.seata.protocol.protobuf.BranchCommitResponseProto)
  private static final BranchCommitResponseProto DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new BranchCommitResponseProto();
  }

  public static BranchCommitResponseProto getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<BranchCommitResponseProto>
      PARSER = new com.google.protobuf.AbstractParser<BranchCommitResponseProto>() {
    @Override
    public BranchCommitResponseProto parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new BranchCommitResponseProto(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<BranchCommitResponseProto> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<BranchCommitResponseProto> getParserForType() {
    return PARSER;
  }

  @Override
  public BranchCommitResponseProto getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

