// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: abstractTransactionResponse.proto

package org.apache.seata.core.serializer.protobuf.generated;

public final class AbstractTransactionResponse {
  private AbstractTransactionResponse() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_org_apache_seata_protocol_protobuf_AbstractTransactionResponseProto_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_apache_seata_protocol_protobuf_AbstractTransactionResponseProto_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n!abstractTransactionResponse.proto\022\"org" +
      ".apache.seata.protocol.protobuf\032\033abstrac" +
      "tResultMessage.proto\032\036transactionExcepti" +
      "onCode.proto\"\346\001\n AbstractTransactionResp" +
      "onseProto\022]\n\025abstractResultMessage\030\001 \001(\013" +
      "2>.org.apache.seata.protocol.protobuf.Ab" +
      "stractResultMessageProto\022c\n\030transactionE" +
      "xceptionCode\030\002 \001(\0162A.org.apache.seata.pr" +
      "otocol.protobuf.TransactionExceptionCode" +
      "ProtoBT\n3org.apache.seata.core.serialize" +
      "r.protobuf.generatedB\033AbstractTransactio" +
      "nResponseP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          AbstractResultMessage.getDescriptor(),
          TransactionExceptionCode.getDescriptor(),
        });
    internal_static_org_apache_seata_protocol_protobuf_AbstractTransactionResponseProto_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_org_apache_seata_protocol_protobuf_AbstractTransactionResponseProto_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_apache_seata_protocol_protobuf_AbstractTransactionResponseProto_descriptor,
        new String[] { "AbstractResultMessage", "TransactionExceptionCode", });
    AbstractResultMessage.getDescriptor();
    TransactionExceptionCode.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
