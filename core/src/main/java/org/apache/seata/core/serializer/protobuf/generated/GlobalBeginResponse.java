// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: globalBeginResponse.proto

package org.apache.seata.core.serializer.protobuf.generated;

public final class GlobalBeginResponse {
  private GlobalBeginResponse() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_org_apache_seata_protocol_protobuf_GlobalBeginResponseProto_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_apache_seata_protocol_protobuf_GlobalBeginResponseProto_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\031globalBeginResponse.proto\022\"org.apache." +
      "seata.protocol.protobuf\032!abstractTransac" +
      "tionResponse.proto\"\245\001\n\030GlobalBeginRespon" +
      "seProto\022i\n\033abstractTransactionResponse\030\001" +
      " \001(\0132D.org.apache.seata.protocol.protobu" +
      "f.AbstractTransactionResponseProto\022\013\n\003xi" +
      "d\030\002 \001(\t\022\021\n\textraData\030\003 \001(\tBL\n3org.apache" +
      ".seata.core.serializer.protobuf.generate" +
      "dB\023GlobalBeginResponseP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          AbstractTransactionResponse.getDescriptor(),
        });
    internal_static_org_apache_seata_protocol_protobuf_GlobalBeginResponseProto_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_org_apache_seata_protocol_protobuf_GlobalBeginResponseProto_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_apache_seata_protocol_protobuf_GlobalBeginResponseProto_descriptor,
        new String[] { "AbstractTransactionResponse", "Xid", "ExtraData", });
    AbstractTransactionResponse.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
