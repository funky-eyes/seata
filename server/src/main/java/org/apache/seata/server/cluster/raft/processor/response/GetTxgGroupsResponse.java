package org.apache.seata.server.cluster.raft.processor.response;

import org.apache.seata.server.cluster.raft.sync.msg.dto.TxgGroupAssignmentDTO;

import java.io.Serializable;

public class GetTxgGroupsResponse implements Serializable {
    private static final long serialVersionUID = -5693903495378729171L;

    private boolean success;
    private String errorMsg;
    private TxgGroupAssignmentDTO txgAssignments; // Reuse existing DTO!

    public GetTxgGroupsResponse() {}

    public GetTxgGroupsResponse(boolean success) {
        this.success = success;
    }

    public GetTxgGroupsResponse(boolean success, TxgGroupAssignmentDTO txgAssignments) {
        this.success = success;
        this.txgAssignments = txgAssignments;
    }

    public GetTxgGroupsResponse(boolean success, String errorMsg) {
        this.success = success;
        this.errorMsg = errorMsg;
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public TxgGroupAssignmentDTO getTxgAssignments() { return txgAssignments; }
    public void setTxgAssignments(TxgGroupAssignmentDTO txgAssignments) { this.txgAssignments = txgAssignments; }
}
