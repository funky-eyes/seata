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
import com.alipay.sofa.jraft.rpc.RpcServer;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.util.StringUtils;
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
import java.util.*;
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

    @Override
    public void init() {
        String dataPath;
        String controllerInitConf = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_CONTROLLER_SERVER_ADDR);
        String initConf;
        if (StringUtils.isBlank(controllerInitConf)) {
            // It is currently in single-raft mode.
            String group = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_GROUP, DEFAULT_SEATA_GROUP);
            dataPath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR) + separator
                    + "raft" + separator + serverId.getPort();
            initConf = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR);
            init(initConf);
            Configuration configuration = new Configuration();
            configuration.parse(initConf);
            try {
                RAFT_SERVER_MAP.put(group, createRaftServer(dataPath, group, configuration));
            } catch (IOException e) {
                throw new IllegalArgumentException("fail init raft cluster:" + e.getMessage(), e);
            }
        } else {
            // todo: This needs to be modified to obtain the group and address list through TXG.
            List<String> groups = Collections.emptyList();
            dataPath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR) + separator
                    + "raft" + separator + serverId.getPort();
            initConf = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR);
            init(initConf);
            Configuration configuration = new Configuration();
            configuration.parse(initConf);
            groups.forEach(group -> {
                createRaftServers(group, dataPath, configuration);
            });
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