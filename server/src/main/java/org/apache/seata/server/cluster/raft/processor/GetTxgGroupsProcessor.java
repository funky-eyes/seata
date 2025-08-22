package org.apache.seata.server.cluster.raft.processor;

import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import org.apache.seata.server.cluster.raft.processor.request.GetTxgGroupsRequest;
import org.apache.seata.server.cluster.raft.processor.response.GetTxgGroupsResponse;
import org.apache.seata.server.cluster.raft.service.RaftGroupStoreManager;
import org.apache.seata.server.cluster.raft.sync.msg.dto.TxgGroupAssignmentDTO;

public class GetTxgGroupsProcessor implements RpcProcessor<GetTxgGroupsRequest> {

    private final RaftGroupStoreManager transactionGroupStoreManager;

    public GetTxgGroupsProcessor(RaftGroupStoreManager transactionGroupStoreManager) {
        super();
        this.transactionGroupStoreManager = transactionGroupStoreManager;
    }

    @Override
    public void handleRequest(RpcContext rpcCtx, GetTxgGroupsRequest request) {
        TxgGroupAssignmentDTO txgGroupAssignmentDTO =
                new TxgGroupAssignmentDTO(transactionGroupStoreManager.getRaftGroupsByIp(request.getNodeIp()));
        rpcCtx.sendResponse(new GetTxgGroupsResponse(true, txgGroupAssignmentDTO));
    }

    @Override
    public String interest() {
        return GetTxgGroupsRequest.class.getName();
    }
}