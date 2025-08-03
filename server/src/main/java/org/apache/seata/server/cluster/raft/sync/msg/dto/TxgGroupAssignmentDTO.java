package org.apache.seata.server.cluster.raft.sync.msg.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class TxgGroupAssignmentDTO implements Serializable {

    //do we need this term?
    private static final long serialVersionUID = 6208583637662412658L;

    public TxgGroupAssignmentDTO( Map<String, List<String>> groupMemberMap) {
    }
}
