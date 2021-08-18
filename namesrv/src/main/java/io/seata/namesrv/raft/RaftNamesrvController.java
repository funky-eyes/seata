/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.namesrv.raft;

import java.io.File;
import java.util.List;
import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.RaftGroupService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import io.seata.common.XID;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationCache;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.namesrv.NamesrvController;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;
import static io.seata.common.DefaultValues.SEATA_RAFT_GROUP;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_AUTO_JOIN;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_CLUSTER;
import static java.io.File.separator;

/**
 * @author funkye
 */
public class RaftNamesrvController implements NamesrvController<PeerId>, ConfigurationChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftNamesrvController.class);

    private StateMachineAdapter raftStateMachine;

    private RaftGroupService raftGroupService;

    private Node node;

    private CliService cliService;

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Override
    public NamesrvController start() throws Exception {
        if (node != null) {
            return this;
        }
        // analytic parameter
        final PeerId serverId = new PeerId();
        String colon = ":";
        String serverIdStr = new StringBuilder(XID.getIpAddress()).append(":").append(XID.getPort() - 1000).toString();
        if (!serverId.parse(serverIdStr)) {
            throw new IllegalArgumentException("fail to parse serverId:" + serverIdStr);
        }
        final String dataPath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR)
            + separator + serverIdStr.split(colon)[1];
        // Initialization path
        FileUtils.forceMkdir(new File(dataPath));

        // Here you have raft RPC and business RPC using the same RPC server, and you can usually do this separately
        final RpcServer rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
        // Initialize the state machine
        this.raftStateMachine = new RaftStateMachine();
        final NodeOptions nodeOptions = new NodeOptions();
        // Set the state machine to startup parameters
        nodeOptions.setFsm(this.raftStateMachine);
        // Set the storage path
        // Log, must
        nodeOptions.setLogUri(dataPath + File.separator + "log");
        // Meta information, must
        nodeOptions.setRaftMetaUri(dataPath + File.separator + "raft_meta");
        // Snapshot, optional, is generally recommended
        nodeOptions.setSnapshotUri(dataPath + File.separator + "snapshot");
        // Initialize the raft Group service framework
        this.raftGroupService = new RaftGroupService(SEATA_RAFT_GROUP, serverId, nodeOptions, rpcServer);
        ConfigurationCache.addConfigListener(SERVER_RAFT_CLUSTER, this);
        this.node = this.raftGroupService.start();
        this.cliService = SingletonHandler.CLI_SERVICE;
        String initConfStr = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_CLUSTER);
        if (StringUtils.isBlank(initConfStr)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("initialize SofaJRaft fail , server.raft.cluster is null");
            }
            return null;
        }
        final com.alipay.sofa.jraft.conf.Configuration initConf = new com.alipay.sofa.jraft.conf.Configuration();
        if (!initConf.parse(initConfStr)) {
            throw new IllegalArgumentException("fail to parse initConf:" + initConfStr);
        }
        // whether to join an existing cluster
        if (CONFIG.getBoolean(SERVER_RAFT_AUTO_JOIN, false)) {
            List<PeerId> currentPeers = null;
            try {
                currentPeers = cliService.getPeers(SEATA_RAFT_GROUP, initConf);
            } catch (Exception e) {
                // In the first deployment, the leader cannot be found
            }
            if (CollectionUtils.isNotEmpty(currentPeers)) {
                if (!currentPeers.contains(serverId)) {
                    Status status = cliService.addPeer(SEATA_RAFT_GROUP, initConf, serverId);
                    if (!status.isOk()) {
                        LOGGER.error("failed to join the RAFT cluster: {}. Please check the status of the cluster",
                            initConfStr);
                    }
                }
            }
        }
        return this;
    }

    @Override
    public List<PeerId> nodes() {
        if (cliService != null) {
            String initConfStr = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_CLUSTER);
            final com.alipay.sofa.jraft.conf.Configuration initConf = new com.alipay.sofa.jraft.conf.Configuration();
            initConf.parse(initConfStr);
            return cliService.getPeers(SEATA_RAFT_GROUP, initConf);
        }
        return null;
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        if (SERVER_RAFT_CLUSTER.equals(event.getDataId())) {
            final com.alipay.sofa.jraft.conf.Configuration newConf = new com.alipay.sofa.jraft.conf.Configuration();
            if (newConf.parse(event.getNewValue())) {
                if (node != null && node.isLeader()) {
                    cliService.changePeers(SEATA_RAFT_GROUP, node.getOptions().getInitialConf(), newConf);
                }
            }
        }
    }

    private static class SingletonHandler {
        private static final CliService CLI_SERVICE = RaftServiceFactory.createAndInitCliService(new CliOptions());
    }

    @Override
    public void close() {
        cliService.shutdown();
        raftGroupService.shutdown();
        raftStateMachine.onShutdown();
    }

}
