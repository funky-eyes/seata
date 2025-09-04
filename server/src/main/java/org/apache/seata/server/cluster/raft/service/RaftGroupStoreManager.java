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
package org.apache.seata.server.cluster.raft.service;

import org.apache.seata.common.metadata.Node;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;

import java.util.List;
import java.util.Map;

/**
 * Interface for changing peers in Raft groups
 * Only accessible to Control Group members
 * Updated to include RaftClusterMetadata support.
 */
public interface RaftGroupStoreManager {



    /**
     * Change peers for multiple Raft groups
     * This operation is restricted to Control Group members only validation required
     */
    void saveOrUpdate(Map<String, List<Node>> groupPeersMap);

    void clear();

    Map<String, List<Node>> getGroupPeersMap();

    void changePeers(Map<String, List<String>> groupPeersMap);

    Map<String, List<Node>> getRaftGroupsByIp(String ip);

    /**
     * Update TXG cluster metadata (new method for enhanced metadata support)
     * This method is called by the CG state machine to store complete cluster metadata
     */
    default void updateTxgClusterMetadata(String groupId, RaftClusterMetadata metadata) {
        // Default implementation for backward compatibility
        // Implementations should override this method to provide full metadata support
    }

    /**
     * Get all Raft groups metadata (new method for enhanced metadata support)
     */
    default Map<String, RaftClusterMetadata> getAllRaftGroupsMetadata() {
        // Default implementation for backward compatibility
        return Map.of();
    }

    /**
     * Get TXG cluster metadata for a specific group (new method)
     */
    default RaftClusterMetadata getTxgClusterMetadata(String groupId) {
        // Default implementation for backward compatibility
        return null;
    }


}