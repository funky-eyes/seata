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

import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.rpc.CliClientService;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.XID;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.server.cluster.raft.processor.PutNodeInfoRequestProcessor;
import org.apache.seata.server.cluster.raft.serializer.JacksonBoltSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.io.File.separator;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_APPLY_BATCH;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_DISRUPTOR_BUFFER_SIZE;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_ELECTION_TIMEOUT_MS;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_MAX_APPEND_BUFFER_SIZE;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_MAX_REPLICATOR_INFLIGHT_MSGS;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_PORT_CAMEL;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SNAPSHOT_INTERVAL;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SYNC;
import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static org.apache.seata.common.DefaultValues.DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS;
import static org.apache.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;

/**
 * Controller Raft Server Manager for managing metadata across multiple raft groups
 */
public class RaftControllerServerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftControllerServerManager.class);

    // Map of controller group IDs to RaftControllerServer instances
    private static final Map<String /*controller-group*/, RaftControllerServer /*raft-controller-cluster*/> RAFT_CONTROLLER_SERVER_MAP = new HashMap<>();
    private static final AtomicBoolean INIT = new AtomicBoolean(false);

    private static final org.apache.seata.config.Configuration CONFIG = ConfigurationFactory.getInstance();
    private static volatile boolean CONTROLLER_MODE;
    private static RpcServer controllerRpcServer;

    // Default controller group name
    private static final String DEFAULT_CONTROLLER_GROUP = "controller-group";

    public static CliService getCliServiceInstance() {
        return SingletonHandler.CLI_SERVICE;
    }

    public static CliClientService getCliClientServiceInstance() {
        return SingletonHandler.CLI_CLIENT_SERVICE;
    }

    public static void init() {
        if (INIT.compareAndSet(false, true)) {
            // Controller-specific configuration key
            String initConfStr = CONFIG.getConfig("server.raft.controller.cluster");
            CONTROLLER_MODE = CONFIG.getBoolean("server.raft.controller.enabled", false);

            if (StringUtils.isBlank(initConfStr)) {
                if (CONTROLLER_MODE) {
                    throw new IllegalArgumentException(
                            "Controller mode must config: server.raft.controller.cluster");
                }
                return;
            } else {
                LOGGER.warn("raft controller mode is an experimental feature for managing multiple raft groups");
            }

            final Configuration initConf = new Configuration();
            if (!initConf.parse(initConfStr)) {
                throw new IllegalArgumentException("fail to parse controller initConf:" + initConfStr);
            }

            // Controller uses a different port range
            int port = Integer.parseInt(System.getProperty("server.raftControllerPort", "0"));
            PeerId serverId = null;
            String host = XID.getIpAddress();

            if (port <= 0) {
                // Highly available deployments require different nodes
                for (PeerId peer : initConf.getPeers()) {
                    List<String> peerIps = NetUtil.getHostByName(peer.getIp());
                    for (String peerIp : peerIps) {
                        if (StringUtils.equals(peerIp, host)) {
                            if (serverId != null) {
                                throw new IllegalArgumentException(
                                        "server.raft.controller.cluster has duplicate ip, For local debugging, use -Dserver.raftControllerPort to specify the raft controller port");
                            }
                            serverId = peer;
                            break;
                        }
                    }
                }
            } else {
                // Local debugging use
                serverId = new PeerId(host, port);
            }

            final String dataPath = CONFIG.getConfig("store.controller.file.dir", DEFAULT_SESSION_STORE_FILE_DIR)
                    + separator + "raft-controller" + separator + serverId.getPort();
            String controllerGroup = CONFIG.getConfig("server.raft.controller.group", DEFAULT_CONTROLLER_GROUP);

            try {
                // Controller RPC server for metadata management
                controllerRpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
                RaftControllerServer raftControllerServer = new RaftControllerServer(
                        dataPath, controllerGroup, serverId, initNodeOptions(initConf), controllerRpcServer);
                // Store the controller server
                RAFT_CONTROLLER_SERVER_MAP.put(controllerGroup, raftControllerServer);
            } catch (IOException e) {
                throw new IllegalArgumentException("fail init raft controller cluster:" + e.getMessage(), e);
            }
        }
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

        if (controllerRpcServer != null) {
            controllerRpcServer.registerProcessor(new PutNodeInfoRequestProcessor());
            SerializerManager.addSerializer(SerializerType.JACKSON.getCode(), new JacksonBoltSerializer());
            if (!controllerRpcServer.init(null)) {
                throw new RuntimeException("start raft controller node fail!");
            }
        }
    }

    public static void destroy() {
        RAFT_CONTROLLER_SERVER_MAP.forEach((controllerGroup, raftControllerServer) -> {
            raftControllerServer.close();
            LOGGER.info("closed seata controller raft cluster, group: {} ", controllerGroup);
        });
        Optional.ofNullable(controllerRpcServer).ifPresent(RpcServer::shutdown);
        RAFT_CONTROLLER_SERVER_MAP.clear();
        controllerRpcServer = null;
        CONTROLLER_MODE = false;
        INIT.set(false);
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

    private static RaftOptions initRaftOptions() {
        RaftOptions raftOptions = new RaftOptions();
        raftOptions.setApplyBatch(CONFIG.getInt(SERVER_RAFT_APPLY_BATCH, raftOptions.getApplyBatch()));
        raftOptions.setMaxAppendBufferSize(
                CONFIG.getInt(SERVER_RAFT_MAX_APPEND_BUFFER_SIZE, raftOptions.getMaxAppendBufferSize()));
        raftOptions.setDisruptorBufferSize(
                CONFIG.getInt(SERVER_RAFT_DISRUPTOR_BUFFER_SIZE, raftOptions.getDisruptorBufferSize()));
        raftOptions.setMaxReplicatorInflightMsgs(
                CONFIG.getInt(SERVER_RAFT_MAX_REPLICATOR_INFLIGHT_MSGS, raftOptions.getMaxReplicatorInflightMsgs()));
        raftOptions.setSync(CONFIG.getBoolean(SERVER_RAFT_SYNC, raftOptions.isSync()));
        return raftOptions;
    }

    private static NodeOptions initNodeOptions(Configuration initConf) {
        NodeOptions nodeOptions = new NodeOptions();
        // enable the CLI service.
        nodeOptions.setDisableCli(false);
        // snapshot should be made every 600 seconds (can be configured differently for controller)
        int snapshotInterval = CONFIG.getInt("server.raft.controller.snapshot.interval", 60 * 10);
        nodeOptions.setSnapshotIntervalSecs(snapshotInterval);
        nodeOptions.setRaftOptions(initRaftOptions());
        // set the election timeout to 1 second (can be configured differently for controller)
        nodeOptions.setElectionTimeoutMs(
                CONFIG.getInt("server.raft.controller.election.timeout.ms", DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS));
        // set up the initial cluster configuration
        nodeOptions.setInitialConf(initConf);
        return nodeOptions;
    }

    public static Set<String> getControllerGroups() {
        return RAFT_CONTROLLER_SERVER_MAP.keySet();
    }

    private static class SingletonHandler {
        private static final CliService CLI_SERVICE = RaftServiceFactory.createAndInitCliService(new CliOptions());
        private static final CliClientService CLI_CLIENT_SERVICE = new CliClientServiceImpl();

        static {
            CLI_CLIENT_SERVICE.init(new CliOptions());
        }
    }
}