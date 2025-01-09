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
package org.apache.seata.server.instance;

import javax.annotation.Resource;
import org.apache.seata.common.XID;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.listener.ClusterChangeListener;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.spring.boot.autoconfigure.properties.server.ServerRaftProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


import static org.apache.seata.common.ConfigurationKeys.META_PREFIX;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;

@Component
@ConditionalOnProperty(name = "seata.store.type", havingValue = "raft")
public class RaftServerInstanceStrategy extends AbstractSeataInstanceStrategy
    implements ClusterChangeListener, Ordered {

    @Resource
    ServerRaftProperties raftProperties;

    @Override
    public Instance serverInstanceInit() {
        ConfigurableEnvironment environment =
                (ConfigurableEnvironment) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);

        // load node properties
        Instance instance = Instance.getInstance();
        // load namespace
        String namespace = registryNamingServerProperties.getNamespace();
        instance.setNamespace(namespace);
        // load cluster name
        String clusterName = registryNamingServerProperties.getCluster();
        instance.setClusterName(clusterName);
        String unit = raftProperties.getGroup();
        instance.setUnit(unit);
        // load cluster type
        String clusterType = String.valueOf(StoreConfig.getSessionMode());
        instance.addMetadata("cluster-type", "raft".equals(clusterType) ? clusterType : "default");
        long term = RaftServerManager.getRaftServer(unit).getRaftStateMachine().getCurrentTerm().get();
        instance.setTerm(term);

        // load node Endpoint
        instance.setControl(new Node.Endpoint(XID.getIpAddress(), serverProperties.getPort(), "http"));

        // load metadata
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> enumerablePropertySource = (EnumerablePropertySource<?>)propertySource;
                for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                    if (propertyName.startsWith(META_PREFIX)) {
                        instance.addMetadata(propertyName.substring(META_PREFIX.length()),
                            enumerablePropertySource.getProperty(propertyName));
                    }
                }
            }
        }
        return instance;
    }

    @Override
    public Type type() {
        return Type.RAFT;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    @Override
    @EventListener
    @Async
    public void onChangeEvent(ClusterChangeEvent event) {
        Instance.getInstance().setTerm(event.getTerm());
        SessionHolder.getRootVGroupMappingManager().notifyMapping();
    }

}
