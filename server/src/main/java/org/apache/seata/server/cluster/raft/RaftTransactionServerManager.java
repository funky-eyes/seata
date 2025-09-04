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

import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.CliClientService;
import com.alipay.sofa.jraft.rpc.InvokeContext;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.XID;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.discovery.registry.FileRegistryServiceImpl;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.seata.discovery.registry.namingserver.NamingserverRegistryServiceImpl;
import org.apache.seata.server.cluster.raft.processor.PutNodeInfoRequestProcessor;
import org.apache.seata.server.cluster.raft.processor.request.GetTxgGroupsRequest;
import org.apache.seata.server.cluster.raft.processor.response.GetTxgGroupsResponse;
import org.apache.seata.server.cluster.raft.serializer.JacksonBoltSerializer;
import org.apache.seata.server.cluster.raft.sync.msg.dto.TxgGroupAssignmentDTO;
import org.apache.seata.server.store.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.io.File.separator;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_PORT_CAMEL;
import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static org.apache.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;

/**
 */
public class RaftTransactionServerManager extends AbstractRaftServerManager {

    public static volatile RaftTransactionServerManager raftTransactionServerManager;

    public static RaftTransactionServerManager getInstance() {
        if (raftTransactionServerManager == null) {
            synchronized (RaftTransactionServerManager.class) {
                if (raftTransactionServerManager == null) {
                    raftTransactionServerManager = new RaftTransactionServerManager();
                }
            }
        }
        return raftTransactionServerManager;
    }

    public void start() {
        RAFT_SERVER_MAP.forEach((group, raftServer) -> {
            try {
                raftServer.start();
            } catch (IOException e) {
                logger.error("start seata server raft cluster error, group: {} ", group, e);
                throw new RuntimeException(e);
            }
            logger.info("started seata server raft cluster, group: {} ", group);
        });
        if (rpcServer != null) {
            rpcServer.registerProcessor(new PutNodeInfoRequestProcessor());
            SerializerManager.addSerializer(SerializerType.JACKSON.getCode(), new JacksonBoltSerializer());
            if (!rpcServer.init(null)) {
                throw new RuntimeException("start raft node fail!");
            }
        }
    }

    public RaftTransactionServer getRaftServer(String group) {
        return (RaftTransactionServer) RAFT_SERVER_MAP.get(group);
    }

    public Collection<RaftServer> getRaftServers() {
        return RAFT_SERVER_MAP.values();
    }

    public static Set<String> groups() {
        return RAFT_SERVER_MAP.keySet();
    }

    /**
     * Request Flow:
     *
     * Transaction server reads controller addresses from config
     * Makes RPC call to controller asking for TXG groups assigned to its IP
     * Controller responds with list of groups this node should participate in
     * Transaction server manager creates raft servers for each assigned group
     */
    @Override
    public void init() {
        String dataPath;
        String controllerInitConf = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_CONTROLLER_SERVER_ADDR);
        if (StringUtils.isBlank(controllerInitConf)) {
            // It is currently in single-raft mode.
            String group = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_GROUP, DEFAULT_SEATA_GROUP);
            dataPath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR) + separator
                    + "raft" + separator + serverId.getPort();
            String initConf = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR);
            init(initConf);
            Configuration configuration = new Configuration();
            configuration.parse(initConf);
            try {
                RAFT_SERVER_MAP.put(group, createRaftServer(dataPath, group, configuration));
            } catch (IOException e) {
                throw new IllegalArgumentException("fail init raft cluster:" + e.getMessage(), e);
            }
        } else {
            // Retrieve TXG information from CG interface
            dataPath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR) + separator
                    + "raft" + separator + serverId.getPort();
            // Retrieve TXG assignments from CG via RPC
            TxgGroupAssignmentDTO txgAssignments = retrieveTxgAssignmentsFromCG(controllerInitConf);
            // Create raft servers for each TXG group assigned to this node
            if (txgAssignments != null && txgAssignments.getGroupMemberMap() != null) {
                txgAssignments.getGroupMemberMap().forEach((groupName, members) -> {
                    logger.info("TXG group: {} assigned members: {}", groupName, members);
                    if (members.stream()
                            .anyMatch(node -> node.getInternal().getHost().equals(XID.getIpAddress())
                                    && node.getInternal().getPort() == serverId.getPort())) {
                        Configuration configuration = new Configuration();
                        StringJoiner sb = new StringJoiner(",");
                        for (Node member : members) {
                            Node.Endpoint endpoint = member.getInternal();
                            sb.add(endpoint.getHost() + ":" + endpoint.getPort());
                        }
                        String initConf = sb.toString();
                        logger.info("Creating raft server for TXG group: {} with members: {}", groupName, initConf);
                        configuration.parse(initConf);
                        createRaftServers(groupName, dataPath, configuration);
                    }
                });
            } else {
                logger.error("No TXG assignments received from controller, no raft groups will be created");
                throw new RuntimeException("Failed to retrieve TXG assignments from controller: " + controllerInitConf);
            }
        }
    }

    private TxgGroupAssignmentDTO retrieveTxgAssignmentsFromCG(String controllerInitConf) {
        try {
            // Parse controller addresses
            Configuration controllerConfiguration = new Configuration();
            controllerConfiguration.parse(controllerInitConf);
            List<PeerId> controllerPeers = controllerConfiguration.getPeers();

            if (CollectionUtils.isEmpty(controllerPeers)) {
                logger.warn("No controller peers found in configuration: {}", controllerInitConf);
                return null;
            }

            // Try to get TXG assignments from each controller peer
            for (int i = 0; i < 10; i++) {
                for (PeerId controllerPeer : controllerPeers) {
                    try {
                        GetTxgGroupsRequest request = new GetTxgGroupsRequest(XID.getIpAddress());
                        GetTxgGroupsResponse response = makeRpcCallToController(controllerPeer, request);

                        if (response != null && response.isSuccess()) {
                            logger.info("Successfully retrieved TXG assignments from controller {}", controllerPeer);
                            return response.getTxgAssignments();
                        }
                    } catch (Exception e) {
                        logger.warn("Error calling controller {}: {}", controllerPeer, e.getMessage());
                    }
                }
                // Wait before retrying
                Thread.sleep(1000);
            }
            logger.error("Failed to retrieve TXG assignments from any controller");
            return null;

        } catch (Exception e) {
            logger.error("Failed to retrieve TXG assignments from CG", e);
            return null;
        }
    }

    private GetTxgGroupsResponse makeRpcCallToController(PeerId controllerPeer, GetTxgGroupsRequest request)
            throws Exception {

        InvokeContext invokeContext = new InvokeContext();
        invokeContext.put(com.alipay.remoting.InvokeContext.BOLT_CUSTOM_SERIALIZER,
                SerializerType.JACKSON.getCode());

        CliClientService cliClientService = getCliClientServiceInstance();

        CompletableFuture<GetTxgGroupsResponse> future = new CompletableFuture<>();

        ((CliClientServiceImpl) cliClientService)
                .getRpcClient()
                .invokeAsync(
                        controllerPeer.getEndpoint(),
                        request,
                        invokeContext,
                        (result, err) -> {
                            if (err == null) {
                                future.complete((GetTxgGroupsResponse) result);
                            } else {
                                future.completeExceptionally(new Exception("RPC error: " + err.getMessage(), err));
                            }
                        },
                        5000
                );

        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new TimeoutException("Timeout waiting for TXG assignments from " + controllerPeer);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new Exception("Unexpected error", cause);
            }
        }
    }



    private void createRaftServers(String group, String dataPath, Configuration configuration) {
        try {
            RAFT_SERVER_MAP.put(group, createRaftServer(dataPath, group, configuration));
        } catch (IOException e) {
            throw new IllegalArgumentException("fail init raft cluster:" + e.getMessage(), e);
        }
    }

    @Override
    public Collection<String> getRaftGroups() {
        return RAFT_SERVER_MAP.keySet();
    }

    @Override
    public RaftServer createRaftServer(String dataPath, String group, Configuration initConf) throws IOException {
        return new RaftTransactionServer(dataPath, group, serverId, initNodeOptions(initConf), rpcServer);
    }
}