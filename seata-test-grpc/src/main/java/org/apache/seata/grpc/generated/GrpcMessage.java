// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: grpcMessage.proto

package org.apache.seata.grpc.generated;

public final class GrpcMessage {
  private GrpcMessage() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_org_apache_seata_core_protocol_GrpcMessageProto_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_apache_seata_core_protocol_GrpcMessageProto_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_org_apache_seata_core_protocol_GrpcMessageProto_HeadMapEntry_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_apache_seata_core_protocol_GrpcMessageProto_HeadMapEntry_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\021grpcMessage.proto\022\036org.apache.seata.co" +
      "re.protocol\032\031google/protobuf/any.proto\"\327" +
      "\001\n\020GrpcMessageProto\022\n\n\002id\030\001 \001(\005\022\023\n\013messa" +
      "geType\030\002 \001(\005\022N\n\007headMap\030\003 \003(\0132=.org.apac" +
      "he.seata.core.protocol.GrpcMessageProto." +
      "HeadMapEntry\022\"\n\004body\030\004 \001(\0132\024.google.prot" +
      "obuf.Any\032.\n\014HeadMapEntry\022\013\n\003key\030\001 \001(\t\022\r\n" +
      "\005value\030\002 \001(\t:\0028\0012\205\001\n\014SeataService\022u\n\013sen" +
      "dRequest\0220.org.apache.seata.core.protoco" +
      "l.GrpcMessageProto\0320.org.apache.seata.co" +
      "re.protocol.GrpcMessageProto(\0010\001B0\n\037org." +
      "apache.seata.grpc.generatedB\013GrpcMessage" +
      "P\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.AnyProto.getDescriptor(),
        });
    internal_static_org_apache_seata_core_protocol_GrpcMessageProto_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_org_apache_seata_core_protocol_GrpcMessageProto_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_apache_seata_core_protocol_GrpcMessageProto_descriptor,
        new String[] { "Id", "MessageType", "HeadMap", "Body", });
    internal_static_org_apache_seata_core_protocol_GrpcMessageProto_HeadMapEntry_descriptor =
      internal_static_org_apache_seata_core_protocol_GrpcMessageProto_descriptor.getNestedTypes().get(0);
    internal_static_org_apache_seata_core_protocol_GrpcMessageProto_HeadMapEntry_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_apache_seata_core_protocol_GrpcMessageProto_HeadMapEntry_descriptor,
        new String[] { "Key", "Value", });
    com.google.protobuf.AnyProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
