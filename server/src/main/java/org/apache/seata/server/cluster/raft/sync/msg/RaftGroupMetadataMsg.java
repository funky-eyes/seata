package org.apache.seata.server.cluster.raft.sync.msg;

import java.util.Map;

public class RaftGroupMetadataMsg extends RaftBaseMsg {
    private String groupId;
    private RaftSyncMsgType msgType;
    private Map<String, Object> groupMetadata;

    public RaftGroupMetadataMsg(RaftSyncMsgType msgType,String groupId, Map<String, Object> groupMetadata) {
        this.msgType = msgType;
        this.groupId = groupId;
        this.groupMetadata = groupMetadata;
    }

    public String getGroupId() {
        return groupId;
    }

    public Map<String, Object> getGroupMetadata() {
        return groupMetadata;
    }
}

