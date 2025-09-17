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

import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import org.apache.seata.server.cluster.raft.manager.RaftControllerServerManager;
import org.apache.seata.server.cluster.raft.processor.request.PutRaftClusterMetadataRequest;
import org.apache.seata.server.cluster.raft.processor.response.PutRaftClusterMetadataResponse;
import org.apache.seata.server.cluster.raft.sync.msg.RaftTxgClusterMetadataMsg;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Processor for handling TXG cluster metadata submissions to CG leader.
 * This processor ensures that TXG metadata updates go through the CG state machine
 * for proper replication to all CG followers.
 */
public class PutRaftClusterMetadataProcessor implements RpcProcessor<PutRaftClusterMetadataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutRaftClusterMetadataProcessor.class);
    final String controllerGroupType = "controller";

    @Override
    public void handleRequest(RpcContext rpcCtx, PutRaftClusterMetadataRequest request) {
        LOGGER.info("Received TXG cluster metadata update request for group: {}", request.getGroupId());

        try {
            // Check if current node is CG leader
            if (!RaftControllerServerManager.getInstance().isLeader(controllerGroupType)) {
                LOGGER.warn("Current node is not CG leader, rejecting TXG metadata update for group: {}",
                        request.getGroupId());
                rpcCtx.sendResponse(new PutRaftClusterMetadataResponse(false, "Not CG leader"));
                return;
            }

            // Create message to be processed by CG state machine
            RaftTxgClusterMetadataMsg message = new RaftTxgClusterMetadataMsg(
                    request.getGroupId(),
                    request.getMetadata()
            );

            // Use CompletableFuture to handle the async state machine processing
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            // Submit to CG state machine so all CG members get the update
            RaftTaskUtil.createTask(
                    status -> {
                        if (status.isOk()) {
                            LOGGER.info("Successfully processed TXG metadata update for group: {}",
                                    request.getGroupId());
                            future.complete(true);
                        } else {
                            LOGGER.error("Failed to process TXG metadata update for group {}: {}",
                                    request.getGroupId(), status.getErrorMsg());
                            future.completeExceptionally(new RuntimeException(
                                    "State machine processing failed: " + status.getErrorMsg()));
                        }
                    },
                    message,
                    future
            );

            // Wait for state machine processing to complete
            Boolean success = future.get(10, TimeUnit.SECONDS);
            rpcCtx.sendResponse(new PutRaftClusterMetadataResponse(success));

        } catch (Exception e) {
            LOGGER.error("Error processing TXG cluster metadata update for group: {}",
                    request.getGroupId(), e);
            rpcCtx.sendResponse(new PutRaftClusterMetadataResponse(false, e.getMessage()));
        }
    }

    @Override
    public String interest() {
        return PutRaftClusterMetadataRequest.class.getName();
    }
}