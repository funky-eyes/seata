// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: globalLockQueryResponse.proto

package org.apache.seata.core.serializer.protobuf.generated;

public final class GlobalLockQueryResponse {
  private GlobalLockQueryResponse() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_org_apache_seata_protocol_protobuf_GlobalLockQueryResponseProto_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_apache_seata_protocol_protobuf_GlobalLockQueryResponseProto_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\035globalLockQueryResponse.proto\022\"org.apa" +
      "che.seata.protocol.protobuf\032!abstractTra" +
      "nsactionResponse.proto\"\233\001\n\034GlobalLockQue" +
      "ryResponseProto\022i\n\033abstractTransactionRe" +
      "sponse\030\001 \001(\0132D.org.apache.seata.protocol" +
      ".protobuf.AbstractTransactionResponsePro" +
      "to\022\020\n\010lockable\030\002 \001(\010BP\n3org.apache.seata" +
      ".core.serializer.protobuf.generatedB\027Glo" +
      "balLockQueryResponseP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          AbstractTransactionResponse.getDescriptor(),
        });
    internal_static_org_apache_seata_protocol_protobuf_GlobalLockQueryResponseProto_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_org_apache_seata_protocol_protobuf_GlobalLockQueryResponseProto_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_apache_seata_protocol_protobuf_GlobalLockQueryResponseProto_descriptor,
        new String[] { "AbstractTransactionResponse", "Lockable", });
    AbstractTransactionResponse.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
