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
import com.alipay.sofa.jraft.conf.Configuration;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.server.cluster.raft.AbstractRaftServerManager;
import org.apache.seata.server.cluster.raft.RaftServer;
import org.apache.seata.server.cluster.raft.processor.GetTxgGroupsProcessor;
import org.apache.seata.server.cluster.raft.processor.PutNodeInfoRequestProcessor;
import org.apache.seata.server.cluster.raft.serializer.JacksonBoltSerializer;
import org.apache.seata.server.cluster.raft.service.RaftGroupStoreManager;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static java.io.File.separator;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;
import static org.apache.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;

/**
 * Controller Raft Server Manager for managing metadata across multiple raft groups
 */
public class RaftControllerServerManager extends AbstractRaftServerManager {

    public static volatile RaftControllerServerManager raftControllerServerManager;

    public static RaftControllerServerManager getInstance() {
        if (raftControllerServerManager == null) {
            synchronized (RaftControllerServerManager.class) {
                if (raftControllerServerManager == null) {
                    raftControllerServerManager = new RaftControllerServerManager();
                }
            }
        }
        return raftControllerServerManager;
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
            ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext)
                ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                .getBeanFactory();
            rpcServer.registerProcessor(new GetTxgGroupsProcessor(beanFactory.getBean(RaftGroupStoreManager.class)));
            SerializerManager.addSerializer(SerializerType.JACKSON.getCode(), new JacksonBoltSerializer());
            if (!rpcServer.init(null)) {
                throw new RuntimeException("start raft node fail!");
            }
        }
    }

    public RaftControllerServer getRaftServer(String group) {
        return (RaftControllerServer) RAFT_SERVER_MAP.get(group);
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
        String group = "controller";
        String controllerInitConf = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_CONTROLLER_SERVER_ADDR);
        String initConf;
        if (StringUtils.isBlank(controllerInitConf)) {
            return;
        } else {
            dataPath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR) + separator
                    + "raft" + separator + serverId.getPort();
            initConf = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR);
        }
        init(initConf);
        Configuration configuration = new Configuration();
        configuration.parse(initConf);
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
        return new RaftControllerServer(dataPath, group, serverId, initNodeOptions(initConf), rpcServer);
    }
}
