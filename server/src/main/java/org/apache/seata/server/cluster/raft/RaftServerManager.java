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
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.discovery.registry.FileRegistryServiceImpl;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.seata.discovery.registry.namingserver.NamingserverRegistryServiceImpl;
import org.apache.seata.server.cluster.raft.processor.PutNodeInfoRequestProcessor;
import org.apache.seata.server.cluster.raft.serializer.JacksonBoltSerializer;
import org.apache.seata.server.store.StoreConfig;
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
 */
public class RaftServerManager extends AbstractRaftServerManager<RaftServer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftServerManager.class);
    private static final Map<String /*group*/, RaftServer /*raft-group-cluster*/> RAFT_SERVER_MAP = new HashMap<>();
    private static volatile boolean RAFT_MODE;

    @Override
    protected RaftServerConfig getRaftServerConfig() {
        return new RaftServerConfig(
                ConfigurationKeys.SERVER_RAFT_SERVER_ADDR,
                SERVER_RAFT_PORT_CAMEL,
                ConfigurationKeys.SERVER_RAFT_GROUP,
                DEFAULT_SEATA_GROUP,
                "raft",
                "server.raft.cluster has duplicate ip, For local debugging, use -Dserver.raftPort to specify the raft port"
        );
    }

    @Override
    protected RaftServer createRaftServer(String dataPath, String group, PeerId serverId,
                                          NodeOptions nodeOptions, RpcServer rpcServer) throws IOException {
        return new RaftServer(dataPath, group, serverId, nodeOptions, rpcServer);
    }

    @Override
    protected void storeRaftServer(String group, RaftServer raftServer) {
        RAFT_SERVER_MAP.put(group, raftServer);
    }

    @Override
    protected boolean isModeEnabled() {
        RAFT_MODE = StoreConfig.getSessionMode().equals(SessionMode.RAFT);
        return RAFT_MODE;
    }

    @Override
    protected String getExperimentalWarningMessage() {
        return "raft mode and raft cluster is an experimental feature";
    }

    @Override
    protected void validateModeRequirements() {
        for (RegistryService<?> instance : MultiRegistryFactory.getInstances()) {
            if (!(instance instanceof FileRegistryServiceImpl)
                    && !(instance instanceof NamingserverRegistryServiceImpl)) {
                throw new IllegalArgumentException("Raft store mode not support other Registration Center");
            }
        }
    }

    public static void start() {
        RAFT_SERVER_MAP.forEach((group, raftServer) -> {
            try {
                raftServer.start();
            } catch (IOException e) {
                LOGGER.error("start seata server raft cluster error, group: {} ", group, e);
                throw new RuntimeException(e);
            }
            LOGGER.info("started seata server raft cluster, group: {} ", group);
        });
        startSharedRpcServer();
    }

    public static void destroy() {
        RAFT_SERVER_MAP.forEach((group, raftServer) -> {
            raftServer.close();
            LOGGER.info("closed seata server raft cluster, group: {} ", group);
        });
        RAFT_SERVER_MAP.clear();
        destroySharedRpcServer();
        RAFT_MODE = false;
    }

    public static RaftServer getRaftServer(String group) {
        return RAFT_SERVER_MAP.get(group);
    }

    public static Collection<RaftServer> getRaftServers() {
        return RAFT_SERVER_MAP.values();
    }

    public static boolean isLeader(String group) {
        AtomicReference<RaftStateMachine> stateMachine = new AtomicReference<>();
        Optional.ofNullable(RAFT_SERVER_MAP.get(group)).ifPresent(raftServer -> {
            stateMachine.set(raftServer.getRaftStateMachine());
        });
        RaftStateMachine raftStateMachine = stateMachine.get();
        return !isRaftMode() && RAFT_SERVER_MAP.isEmpty() || (raftStateMachine != null && raftStateMachine.isLeader());
    }

    public static boolean isRaftMode() {
        return RAFT_MODE;
    }

    public static Set<String> groups() {
        return RAFT_SERVER_MAP.keySet();
    }

}
