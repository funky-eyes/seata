// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: transactionExceptionCode.proto

package org.apache.seata.core.serializer.protobuf.generated;

public final class TransactionExceptionCode {
  private TransactionExceptionCode() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\036transactionExceptionCode.proto\022\"org.ap" +
      "ache.seata.protocol.protobuf*\215\004\n\035Transac" +
      "tionExceptionCodeProto\022\013\n\007Unknown\020\000\022\023\n\017L" +
      "ockKeyConflict\020\001\022\006\n\002IO\020\002\022\"\n\036BranchRollba" +
      "ckFailed_Retriable\020\003\022$\n BranchRollbackFa" +
      "iled_Unretriable\020\004\022\030\n\024BranchRegisterFail" +
      "ed\020\005\022\026\n\022BranchReportFailed\020\006\022\027\n\023Lockable" +
      "CheckFailed\020\007\022\035\n\031BranchTransactionNotExi" +
      "st\020\010\022\035\n\031GlobalTransactionNotExist\020\t\022\036\n\032G" +
      "lobalTransactionNotActive\020\n\022\"\n\036GlobalTra" +
      "nsactionStatusInvalid\020\013\022#\n\037FailedToSendB" +
      "ranchCommitRequest\020\014\022%\n!FailedToSendBran" +
      "chRollbackRequest\020\r\022\025\n\021FailedToAddBranch" +
      "\020\016\022\037\n\033FailedLockGlobalTranscation\020\017\022\026\n\022F" +
      "ailedWriteSession\020\020\022\017\n\013FailedStore\020\021BQ\n3" +
      "org.apache.seata.core.serializer.protobu" +
      "f.generatedB\030TransactionExceptionCodeP\001b" +
      "\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
