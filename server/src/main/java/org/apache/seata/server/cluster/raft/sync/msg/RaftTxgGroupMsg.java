package org.apache.seata.server.cluster.raft.sync.msg;

import org.apache.seata.server.cluster.raft.sync.msg.dto.TxgGroupAssignmentDTO;

public class RaftTxgGroupMsg extends RaftBaseMsg {

    private TxgGroupAssignmentDTO txgAssignments;

    public RaftTxgGroupMsg(TxgGroupAssignmentDTO assignments) {
        this.msgType = RaftSyncMsgType.UPDATE_TXG_GROUP_ASSIGNMENTS;
        this.txgAssignments = assignments;
    }

    public TxgGroupAssignmentDTO getTxgAssignments() {
        return txgAssignments;
    }
}
