package org.apache.seata.server.cluster.raft.processor.request;

import java.io.Serializable;

public class GetTxgGroupsRequest implements Serializable {

    private static final long serialVersionUID = 8666784892146170675L;
    private String nodeIp;

    public GetTxgGroupsRequest() {}

    public GetTxgGroupsRequest(String nodeIp) {
        this.nodeIp = nodeIp;
    }

    public String getNodeIp() {
        return nodeIp;
    }

    public void setNodeIp(String nodeIp) {
        this.nodeIp = nodeIp;
    }
}
