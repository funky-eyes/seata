// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: mergedResultMessage.proto

package org.apache.seata.core.serializer.protobuf.generated;

public interface MergedResultMessageProtoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:org.apache.seata.protocol.protobuf.MergedResultMessageProto)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractMessageProto abstractMessage = 1;</code>
   * @return Whether the abstractMessage field is set.
   */
  boolean hasAbstractMessage();
  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractMessageProto abstractMessage = 1;</code>
   * @return The abstractMessage.
   */
  AbstractMessageProto getAbstractMessage();
  /**
   * <code>.org.apache.seata.protocol.protobuf.AbstractMessageProto abstractMessage = 1;</code>
   */
  AbstractMessageProtoOrBuilder getAbstractMessageOrBuilder();

  /**
   * <code>repeated .google.protobuf.Any msgs = 2;</code>
   */
  java.util.List<com.google.protobuf.Any> 
      getMsgsList();
  /**
   * <code>repeated .google.protobuf.Any msgs = 2;</code>
   */
  com.google.protobuf.Any getMsgs(int index);
  /**
   * <code>repeated .google.protobuf.Any msgs = 2;</code>
   */
  int getMsgsCount();
  /**
   * <code>repeated .google.protobuf.Any msgs = 2;</code>
   */
  java.util.List<? extends com.google.protobuf.AnyOrBuilder> 
      getMsgsOrBuilderList();
  /**
   * <code>repeated .google.protobuf.Any msgs = 2;</code>
   */
  com.google.protobuf.AnyOrBuilder getMsgsOrBuilder(
      int index);
}
