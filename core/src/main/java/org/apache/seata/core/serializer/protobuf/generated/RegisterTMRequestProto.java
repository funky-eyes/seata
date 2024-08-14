// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: registerTMRequest.proto

package org.apache.seata.core.serializer.protobuf.generated;

/**
 * <pre>
 * PublishRequest is a publish request.
 * </pre>
 *
 * Protobuf type {@code org.apache.seata.protocol.protobuf.RegisterTMRequestProto}
 */
public final class RegisterTMRequestProto extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:org.apache.seata.protocol.protobuf.RegisterTMRequestProto)
    RegisterTMRequestProtoOrBuilder {
private static final long serialVersionUID = 0L;
  // Use RegisterTMRequestProto.newBuilder() to construct.
  private RegisterTMRequestProto(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private RegisterTMRequestProto() {
  }

  @Override
  @SuppressWarnings({"unused"})
  protected Object newInstance(
      UnusedPrivateParameter unused) {
    return new RegisterTMRequestProto();
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private RegisterTMRequestProto(
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
            AbstractIdentifyRequestProto.Builder subBuilder = null;
            if (abstractIdentifyRequest_ != null) {
              subBuilder = abstractIdentifyRequest_.toBuilder();
            }
            abstractIdentifyRequest_ = input.readMessage(AbstractIdentifyRequestProto.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(abstractIdentifyRequest_);
              abstractIdentifyRequest_ = subBuilder.buildPartial();
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
    return RegisterTMRequest.internal_static_org_apache_seata_protocol_protobuf_RegisterTMRequestProto_descriptor;
  }

  @Override
  protected FieldAccessorTable
      internalGetFieldAccessorTable() {
    return RegisterTMRequest.internal_static_org_apache_seata_protocol_protobuf_RegisterTMRequestProto_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            RegisterTMRequestProto.class, Builder.class);
  }

  public static final int ABSTRACTIDENTIFYREQUEST_FIELD_NUMBER = 1;
  private AbstractIdentifyRequestProto abstractIdentifyRequest_;
  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
   * @return Whether the abstractIdentifyRequest field is set.
   */
  @Override
  public boolean hasAbstractIdentifyRequest() {
    return abstractIdentifyRequest_ != null;
  }
  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
   * @return The abstractIdentifyRequest.
   */
  @Override
  public AbstractIdentifyRequestProto getAbstractIdentifyRequest() {
    return abstractIdentifyRequest_ == null ? AbstractIdentifyRequestProto.getDefaultInstance() : abstractIdentifyRequest_;
  }
  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
   */
  @Override
  public AbstractIdentifyRequestProtoOrBuilder getAbstractIdentifyRequestOrBuilder() {
    return getAbstractIdentifyRequest();
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
    if (abstractIdentifyRequest_ != null) {
      output.writeMessage(1, getAbstractIdentifyRequest());
    }
    unknownFields.writeTo(output);
  }

  @Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (abstractIdentifyRequest_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getAbstractIdentifyRequest());
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
    if (!(obj instanceof RegisterTMRequestProto)) {
      return super.equals(obj);
    }
    RegisterTMRequestProto other = (RegisterTMRequestProto) obj;

    if (hasAbstractIdentifyRequest() != other.hasAbstractIdentifyRequest()) return false;
    if (hasAbstractIdentifyRequest()) {
      if (!getAbstractIdentifyRequest()
          .equals(other.getAbstractIdentifyRequest())) return false;
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
    if (hasAbstractIdentifyRequest()) {
      hash = (37 * hash) + ABSTRACTIDENTIFYREQUEST_FIELD_NUMBER;
      hash = (53 * hash) + getAbstractIdentifyRequest().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static RegisterTMRequestProto parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static RegisterTMRequestProto parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static RegisterTMRequestProto parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static RegisterTMRequestProto parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static RegisterTMRequestProto parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static RegisterTMRequestProto parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static RegisterTMRequestProto parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static RegisterTMRequestProto parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static RegisterTMRequestProto parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static RegisterTMRequestProto parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static RegisterTMRequestProto parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static RegisterTMRequestProto parseFrom(
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
  public static Builder newBuilder(RegisterTMRequestProto prototype) {
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
   * Protobuf type {@code org.apache.seata.protocol.protobuf.RegisterTMRequestProto}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:org.apache.seata.protocol.protobuf.RegisterTMRequestProto)
      RegisterTMRequestProtoOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return RegisterTMRequest.internal_static_org_apache_seata_protocol_protobuf_RegisterTMRequestProto_descriptor;
    }

    @Override
    protected FieldAccessorTable
        internalGetFieldAccessorTable() {
      return RegisterTMRequest.internal_static_org_apache_seata_protocol_protobuf_RegisterTMRequestProto_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              RegisterTMRequestProto.class, Builder.class);
    }

    // Construct using org.apache.seata.core.serializer.protobuf.generated.RegisterTMRequestProto.newBuilder()
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
      if (abstractIdentifyRequestBuilder_ == null) {
        abstractIdentifyRequest_ = null;
      } else {
        abstractIdentifyRequest_ = null;
        abstractIdentifyRequestBuilder_ = null;
      }
      return this;
    }

    @Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return RegisterTMRequest.internal_static_org_apache_seata_protocol_protobuf_RegisterTMRequestProto_descriptor;
    }

    @Override
    public RegisterTMRequestProto getDefaultInstanceForType() {
      return RegisterTMRequestProto.getDefaultInstance();
    }

    @Override
    public RegisterTMRequestProto build() {
      RegisterTMRequestProto result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @Override
    public RegisterTMRequestProto buildPartial() {
      RegisterTMRequestProto result = new RegisterTMRequestProto(this);
      if (abstractIdentifyRequestBuilder_ == null) {
        result.abstractIdentifyRequest_ = abstractIdentifyRequest_;
      } else {
        result.abstractIdentifyRequest_ = abstractIdentifyRequestBuilder_.build();
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
      if (other instanceof RegisterTMRequestProto) {
        return mergeFrom((RegisterTMRequestProto)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(RegisterTMRequestProto other) {
      if (other == RegisterTMRequestProto.getDefaultInstance()) return this;
      if (other.hasAbstractIdentifyRequest()) {
        mergeAbstractIdentifyRequest(other.getAbstractIdentifyRequest());
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
      RegisterTMRequestProto parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (RegisterTMRequestProto) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private AbstractIdentifyRequestProto abstractIdentifyRequest_;
    private com.google.protobuf.SingleFieldBuilderV3<
        AbstractIdentifyRequestProto, AbstractIdentifyRequestProto.Builder, AbstractIdentifyRequestProtoOrBuilder> abstractIdentifyRequestBuilder_;
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
     * @return Whether the abstractIdentifyRequest field is set.
     */
    public boolean hasAbstractIdentifyRequest() {
      return abstractIdentifyRequestBuilder_ != null || abstractIdentifyRequest_ != null;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
     * @return The abstractIdentifyRequest.
     */
    public AbstractIdentifyRequestProto getAbstractIdentifyRequest() {
      if (abstractIdentifyRequestBuilder_ == null) {
        return abstractIdentifyRequest_ == null ? AbstractIdentifyRequestProto.getDefaultInstance() : abstractIdentifyRequest_;
      } else {
        return abstractIdentifyRequestBuilder_.getMessage();
      }
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
     */
    public Builder setAbstractIdentifyRequest(AbstractIdentifyRequestProto value) {
      if (abstractIdentifyRequestBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        abstractIdentifyRequest_ = value;
        onChanged();
      } else {
        abstractIdentifyRequestBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
     */
    public Builder setAbstractIdentifyRequest(
        AbstractIdentifyRequestProto.Builder builderForValue) {
      if (abstractIdentifyRequestBuilder_ == null) {
        abstractIdentifyRequest_ = builderForValue.build();
        onChanged();
      } else {
        abstractIdentifyRequestBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
     */
    public Builder mergeAbstractIdentifyRequest(AbstractIdentifyRequestProto value) {
      if (abstractIdentifyRequestBuilder_ == null) {
        if (abstractIdentifyRequest_ != null) {
          abstractIdentifyRequest_ =
            AbstractIdentifyRequestProto.newBuilder(abstractIdentifyRequest_).mergeFrom(value).buildPartial();
        } else {
          abstractIdentifyRequest_ = value;
        }
        onChanged();
      } else {
        abstractIdentifyRequestBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
     */
    public Builder clearAbstractIdentifyRequest() {
      if (abstractIdentifyRequestBuilder_ == null) {
        abstractIdentifyRequest_ = null;
        onChanged();
      } else {
        abstractIdentifyRequest_ = null;
        abstractIdentifyRequestBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
     */
    public AbstractIdentifyRequestProto.Builder getAbstractIdentifyRequestBuilder() {
      
      onChanged();
      return getAbstractIdentifyRequestFieldBuilder().getBuilder();
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
     */
    public AbstractIdentifyRequestProtoOrBuilder getAbstractIdentifyRequestOrBuilder() {
      if (abstractIdentifyRequestBuilder_ != null) {
        return abstractIdentifyRequestBuilder_.getMessageOrBuilder();
      } else {
        return abstractIdentifyRequest_ == null ?
            AbstractIdentifyRequestProto.getDefaultInstance() : abstractIdentifyRequest_;
      }
    }
    /**
     * <code>.org.apache.seata.protocol.protobuf.AbstractIdentifyRequestProto abstractIdentifyRequest = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        AbstractIdentifyRequestProto, AbstractIdentifyRequestProto.Builder, AbstractIdentifyRequestProtoOrBuilder>
        getAbstractIdentifyRequestFieldBuilder() {
      if (abstractIdentifyRequestBuilder_ == null) {
        abstractIdentifyRequestBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            AbstractIdentifyRequestProto, AbstractIdentifyRequestProto.Builder, AbstractIdentifyRequestProtoOrBuilder>(
                getAbstractIdentifyRequest(),
                getParentForChildren(),
                isClean());
        abstractIdentifyRequest_ = null;
      }
      return abstractIdentifyRequestBuilder_;
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


    // @@protoc_insertion_point(builder_scope:org.apache.seata.protocol.protobuf.RegisterTMRequestProto)
  }

  // @@protoc_insertion_point(class_scope:org.apache.seata.protocol.protobuf.RegisterTMRequestProto)
  private static final RegisterTMRequestProto DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new RegisterTMRequestProto();
  }

  public static RegisterTMRequestProto getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<RegisterTMRequestProto>
      PARSER = new com.google.protobuf.AbstractParser<RegisterTMRequestProto>() {
    @Override
    public RegisterTMRequestProto parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new RegisterTMRequestProto(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<RegisterTMRequestProto> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<RegisterTMRequestProto> getParserForType() {
    return PARSER;
  }

  @Override
  public RegisterTMRequestProto getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

