// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: abstractBranchEndRequest.proto

package org.apache.seata.core.serializer.protobuf.generated;

public final class AbstractBranchEndRequest {
  private AbstractBranchEndRequest() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_org_apache_seata_protocol_protobuf_AbstractBranchEndRequestProto_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_apache_seata_protocol_protobuf_AbstractBranchEndRequestProto_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\036abstractBranchEndRequest.proto\022\"org.ap" +
      "ache.seata.protocol.protobuf\032 abstractTr" +
      "ansactionRequest.proto\032\020branchType.proto" +
      "\"\235\002\n\035AbstractBranchEndRequestProto\022g\n\032ab" +
      "stractTransactionRequest\030\001 \001(\0132C.org.apa" +
      "che.seata.protocol.protobuf.AbstractTran" +
      "sactionRequestProto\022\013\n\003xid\030\002 \001(\t\022\020\n\010bran" +
      "chId\030\003 \001(\003\022G\n\nbranchType\030\004 \001(\01623.org.apa" +
      "che.seata.protocol.protobuf.BranchTypePr" +
      "oto\022\022\n\nresourceId\030\005 \001(\t\022\027\n\017applicationDa" +
      "ta\030\006 \001(\tBQ\n3org.apache.seata.core.serial" +
      "izer.protobuf.generatedB\030AbstractBranchE" +
      "ndRequestP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          AbstractTransactionRequest.getDescriptor(),
          BranchType.getDescriptor(),
        });
    internal_static_org_apache_seata_protocol_protobuf_AbstractBranchEndRequestProto_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_org_apache_seata_protocol_protobuf_AbstractBranchEndRequestProto_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_apache_seata_protocol_protobuf_AbstractBranchEndRequestProto_descriptor,
        new String[] { "AbstractTransactionRequest", "Xid", "BranchId", "BranchType", "ResourceId", "ApplicationData", });
    AbstractTransactionRequest.getDescriptor();
    BranchType.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
