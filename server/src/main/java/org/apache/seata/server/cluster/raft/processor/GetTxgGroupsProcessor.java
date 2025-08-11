/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.cluster.raft.processor;

import org.apache.seata.server.cluster.raft.processor.request.GetTxgGroupsRequest;
import org.apache.seata.server.cluster.raft.processor.response.GetTxgGroupsResponse;

import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
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
