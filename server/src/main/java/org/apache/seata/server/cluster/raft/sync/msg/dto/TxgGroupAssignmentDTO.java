package org.apache.seata.server.cluster.raft.sync.msg.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class TxgGroupAssignmentDTO implements Serializable {

    private static final long serialVersionUID = 3327902483836983222L;

    Map<String, List<String>> groupMemberMap;

    public TxgGroupAssignmentDTO( Map<String, List<String>> groupMemberMap) {
        this.groupMemberMap = groupMemberMap;
    }

    public Map<String, List<String>> getGroupMemberMap() {
        return groupMemberMap;
    }

    public void setGroupMemberMap(Map<String, List<String>> groupMemberMap) {
        this.groupMemberMap = groupMemberMap;
    }
}
