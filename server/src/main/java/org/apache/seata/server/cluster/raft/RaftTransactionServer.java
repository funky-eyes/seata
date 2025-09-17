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
package org.apache.seata.server.cluster.raft;

import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RpcServer;

/**
 */
public class RaftTransactionServer extends AbstractRaftServer {

    public RaftTransactionServer(
            final String dataPath,
            final String groupId,
            final PeerId serverId,
            final NodeOptions nodeOptions,
            final RpcServer rpcServer) {
        super(dataPath, groupId, serverId, nodeOptions, rpcServer);
        this.raftStateMachine = new RaftTransactionStateMachine(groupId);
    }

    @Override
    public RaftTransactionStateMachine getRaftStateMachine() {
        return (RaftTransactionStateMachine) this.raftStateMachine;
    }

    @Override
    public boolean isController() {
        return false;
    }

}
