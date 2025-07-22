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
package org.apache.seata.server.cluster.raft.manager;


import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RpcServer;
import org.apache.seata.server.cluster.raft.AbstractRaftServerManager;
import org.apache.seata.server.cluster.raft.RaftServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controller Raft Server Manager for managing metadata across multiple raft groups
 */
public class RaftControllerServerManager extends AbstractRaftServerManager<RaftControllerServer> {


    private static final Logger LOGGER = LoggerFactory.getLogger(RaftControllerServerManager.class);

    // Map of controller group IDs to RaftControllerServer instances
    private static final Map<String /*controller-group*/, RaftControllerServer /*raft-controller-cluster*/> RAFT_CONTROLLER_SERVER_MAP = new HashMap<>();
    private static volatile boolean CONTROLLER_MODE;

    // Default controller group name
    private static final String DEFAULT_CONTROLLER_GROUP = "controller-group";

    @Override
    protected RaftServerConfig getRaftServerConfig() {
        return new RaftServerConfig(
                "server.raft.controller.cluster",
                "server.raftControllerPort",
                "server.raft.controller.group",
                DEFAULT_CONTROLLER_GROUP,
                "raft-controller",
                "server.raft.controller.cluster has duplicate ip, For local debugging, use -Dserver.raftControllerPort to specify the raft controller port"
        );
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    protected RaftControllerServer createRaftServer(String dataPath, String group, PeerId serverId,
                                                    NodeOptions nodeOptions, RpcServer rpcServer) throws IOException {
        return new RaftControllerServer(dataPath, group, serverId, nodeOptions, rpcServer);
    }

    @Override
    protected void storeRaftServer(String group, RaftControllerServer raftControllerServer) {
        RAFT_CONTROLLER_SERVER_MAP.put(group, raftControllerServer);
    }

    @Override
    protected boolean isModeEnabled() {
        CONTROLLER_MODE = CONFIG.getBoolean("server.raft.controller.enabled", false);
        return CONTROLLER_MODE;
    }

    @Override
    protected String getExperimentalWarningMessage() {
        return "raft controller mode is an experimental feature for managing multiple raft groups";
    }

    @Override
    protected void validateModeRequirements() {
        // Controller mode specific validations can be added here
        // For now, no specific requirements beyond the base class
    }

    public static void start() {
        RAFT_CONTROLLER_SERVER_MAP.forEach((controllerGroup, raftControllerServer) -> {
            try {
                raftControllerServer.start();
            } catch (IOException e) {
                LOGGER.error("start seata controller raft cluster error, group: {} ", controllerGroup, e);
                throw new RuntimeException(e);
            }
            LOGGER.info("started seata controller raft cluster, group: {} ", controllerGroup);
        });
        startSharedRpcServer();
    }

    public static void destroy() {
        RAFT_CONTROLLER_SERVER_MAP.forEach((controllerGroup, raftControllerServer) -> {
            raftControllerServer.close();
            LOGGER.info("closed seata controller raft cluster, group: {} ", controllerGroup);
        });
        RAFT_CONTROLLER_SERVER_MAP.clear();
        destroySharedRpcServer();
        CONTROLLER_MODE = false;
    }

    public static RaftControllerServer getRaftControllerServer(String controllerGroup) {
        return RAFT_CONTROLLER_SERVER_MAP.get(controllerGroup);
    }

    public static Collection<RaftControllerServer> getRaftControllerServers() {
        return RAFT_CONTROLLER_SERVER_MAP.values();
    }

    public static boolean isControllerLeader(String controllerGroup) {
        AtomicReference<RaftControllerStateMachine> stateMachine = new AtomicReference<>();
        Optional.ofNullable(RAFT_CONTROLLER_SERVER_MAP.get(controllerGroup)).ifPresent(raftControllerServer -> {
            stateMachine.set(raftControllerServer.getRaftControllerStateMachine());
        });
        RaftControllerStateMachine raftControllerStateMachine = stateMachine.get();
        return !isControllerMode() && RAFT_CONTROLLER_SERVER_MAP.isEmpty() ||
                (raftControllerStateMachine != null && raftControllerStateMachine.isLeader());
    }

    public static boolean isControllerMode() {
        return CONTROLLER_MODE;
    }

    /**
     * Get metadata for all managed raft groups
     */
    public static Map<String, Object> getAllGroupsMetadata() {
        Map<String, Object> allMetadata = new HashMap<>();
        RAFT_CONTROLLER_SERVER_MAP.forEach((controllerGroup, raftControllerServer) -> {
            RaftControllerStateMachine stateMachine = raftControllerServer.getRaftControllerStateMachine();
            if (stateMachine != null) {
                allMetadata.put(controllerGroup, stateMachine.getAllGroupsMetadata());
            }
        });
        return allMetadata;
    }

    /**
     * Add a new raft group to be managed by the controller
     */
    public static boolean addManagedGroup(String controllerGroup, String newGroupId, Map<String, Object> groupMetadata) {
        RaftControllerServer server = RAFT_CONTROLLER_SERVER_MAP.get(controllerGroup);
        if (server != null && server.getRaftControllerStateMachine().isLeader()) {
            return server.getRaftControllerStateMachine().addManagedGroup(newGroupId, groupMetadata);
        }
        return false;
    }

    /**
     * Remove a raft group from controller management
     */
    public static boolean removeManagedGroup(String controllerGroup, String groupId) {
        RaftControllerServer server = RAFT_CONTROLLER_SERVER_MAP.get(controllerGroup);
        if (server != null && server.getRaftControllerStateMachine().isLeader()) {
            return server.getRaftControllerStateMachine().removeManagedGroup(groupId);
        }
        return false;
    }

    public static Set<String> getControllerGroups() {
        return RAFT_CONTROLLER_SERVER_MAP.keySet();
    }
}