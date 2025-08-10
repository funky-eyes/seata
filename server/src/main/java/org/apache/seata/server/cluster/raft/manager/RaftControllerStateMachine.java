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

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.LeaderChangeContext;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.rpc.InvokeContext;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.apache.seata.common.XID;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.raft.RaftStateMachine;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.cluster.raft.execute.RaftMsgExecute;
import org.apache.seata.server.cluster.raft.processor.request.PutNodeMetadataRequest;
import org.apache.seata.server.cluster.raft.processor.response.PutNodeMetadataResponse;
import org.apache.seata.server.cluster.raft.service.RaftGroupStoreManager;
import org.apache.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.metadata.LeaderMetadataSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.txg.TransactionGroupSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.vgroup.VGroupSnapshotFile;
import org.apache.seata.server.cluster.raft.sync.RaftSyncMessageSerializer;
import org.apache.seata.server.cluster.raft.sync.msg.*;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.seata.server.cluster.raft.sync.msg.dto.TxgGroupAssignmentDTO;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_CONTROLLER_GROUP;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;

public class RaftControllerStateMachine extends RaftStateMachine {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftControllerStateMachine.class);

    private static final Map<RaftSyncMsgType, RaftMsgExecute<?>> CONTROLLER_EXECUTES = new HashMap<>();

    private final RaftGroupStoreManager raftGroupStoreManager;

    // Controller-specific cluster metadata
    private volatile RaftClusterMetadata raftClusterMetadata = new RaftClusterMetadata();

    private static final ScheduledThreadPoolExecutor CONTROLLER_METADATA_POOL =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("reSyncControllerMetadataPool", 1, true));

    private ScheduledFuture<?> scheduledFuture;

    public boolean isLeader() {
        return super.isLeader();
    }

    public RaftControllerStateMachine(String group) {
        super(group);
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext)
                        ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                .getBeanFactory();
        this.raftGroupStoreManager = beanFactory.getBean(RaftGroupStoreManager.class);

        // Register controller-specific message handlers
        CONTROLLER_EXECUTES.put(RaftSyncMsgType.REFRESH_CLUSTER_METADATA, syncMsg -> {
            refreshControllerMetadata(syncMsg);
            return null;
        });

        CONTROLLER_EXECUTES.put(RaftSyncMsgType.SAVE_OR_UPDATE_TXG_GROUP_ASSIGNMENTS, syncMsg -> {
            RaftTxgGroupMsg raftTxgGroupMsg = (RaftTxgGroupMsg) syncMsg;
            raftGroupStoreManager.saveOrUpdate(
                    raftTxgGroupMsg.getTxgAssignments().getGroupMemberMap());
            return null;
        });

        // Register controller-specific snapshot files
        registryStoreSnapshotFile(new LeaderMetadataSnapshotFile(group));
        registryStoreSnapshotFile(new VGroupSnapshotFile(group));
        registryStoreSnapshotFile(new TransactionGroupSnapshotFile(group, this.raftGroupStoreManager));

        // Start periodic controller metadata sync
        this.scheduledFuture = CONTROLLER_METADATA_POOL.scheduleAtFixedRate(
                () -> syncControllerNodeInfo(group), 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onApply(Iterator iterator) {
        while (iterator.hasNext()) {
            Closure done = iterator.done();
            if (done != null) {
                // leader does not need to be serialized, just execute the task directly
                done.run(Status.OK());
            } else {
                ByteBuffer byteBuffer = iterator.getData();
                // if data is empty, it is only a heartbeat event and can be ignored
                if (byteBuffer != null && byteBuffer.hasRemaining()) {
                    RaftBaseMsg msg = (RaftBaseMsg)
                            RaftSyncMessageSerializer.decode(byteBuffer.array()).getBody();
                    // follower executes the corresponding task
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("controller sync msg: {}", msg);
                    }
                    onExecuteControllerRaft(msg);
                }
            }
            iterator.next();
        }
    }

    @Override
    public void onSnapshotSave(final SnapshotWriter writer, final Closure done) {
        if (!StringUtils.equals(SessionMode.RAFT.getName(), mode)) {
            done.run(Status.OK());
            return;
        }
        long current = System.currentTimeMillis();
        for (StoreSnapshotFile snapshotFile : snapshotFiles) {
            Status status = snapshotFile.save(writer);
            if (!status.isOk()) {
                done.run(status);
                return;
            }
        }
        LOGGER.info("controllerGroup: {}, onSnapshotSave cost: {} ms.", group, System.currentTimeMillis() - current);
        done.run(Status.OK());
    }

    @Override
    public boolean onSnapshotLoad(final SnapshotReader reader) {
        if (!StringUtils.equals(SessionMode.RAFT.getName(), mode)) {
            return true;
        }
        if (isLeader()) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Leader is not supposed to load snapshot");
            }
            return false;
        }
        long current = System.currentTimeMillis();
        for (StoreSnapshotFile snapshotFile : snapshotFiles) {
            if (!snapshotFile.load(reader)) {
                return false;
            }
        }
        LOGGER.info("controllerGroup: {}, onSnapshotLoad cost: {} ms.", group, System.currentTimeMillis() - current);
        return true;
    }

    @Override
    public void onLeaderStart(final long term) {
        boolean leader = isLeader();
        this.leaderTerm.set(term);
        LOGGER.info("controllerGroup: {}, onLeaderStart: term={}.", group, term);
        this.currentTerm.set(term);
        syncControllerMetadata();

        if (!leader) {
            CompletableFuture.runAsync(() -> {
                int size = raftGroupStoreManager.getGroupPeersMap().size();
                if (size == 0) {
                    LOGGER.info("Begin assigning initial TXG members.");
                    SeataClusterContext.bindGroup(group);
                    try {
                        // Then, initialize TXG groups based on configuration
                        initializeTxgGroups();
                    } finally {
                        SeataClusterContext.unbindGroup();
                    }
                } else {
                    LOGGER.info("controller became leader, managing {} raft groups", size);
                }
            });
            Configuration conf = RouteTable.getInstance().getConfiguration(group);
            // A member change might trigger a leader re-election. At this point, it's necessary to filter out
            // non-existent members and synchronize again.
            changeControllerPeers(conf);
        }
    }

    private void initializeTxgGroups() {
        try {
            // 1. Read the TXG configuration
            String txgGroupsConfig = ConfigurationFactory.getInstance().getConfig(SERVER_RAFT_CONTROLLER_GROUP);

            if (StringUtils.isBlank(txgGroupsConfig)) {
                LOGGER.warn(
                        "No TXG groups configured in {}, skipping TXG initialization", SERVER_RAFT_CONTROLLER_GROUP);
                return;
            }

            String[] groupNames = txgGroupsConfig.split(",");

            // 2. Get current controller cluster members
            Configuration controllerConf = RouteTable.getInstance().getConfiguration(group);
            if (controllerConf == null) {
                LOGGER.error("Controller configuration not found for group: {}", group);
                return;
            }

            List<PeerId> controllerMembers = controllerConf.getPeers();

            // 3. Validate requirements
            if (!validateTxgRequirements(groupNames, controllerMembers)) {
                return; // Validation failed, early return
            }

            // 4. Proceed with initialization...
            LOGGER.info("Starting TXG group initialization for: {}", Arrays.toString(groupNames));

            Map<String, List<Node>> txgGroupAssignments = createTxgGroupAssignments(groupNames, controllerMembers);

            submitTxgGroupAssignments(txgGroupAssignments);

        } catch (Exception e) {
            LOGGER.error("Failed to initialize TXG groups", e);
        }
    }

    private boolean validateTxgRequirements(String[] groupNames, List<PeerId> controllerMembers) {
        // Validate groupNames is not null
        if (groupNames == null) {
            LOGGER.error("TXG initialization requires groupNames to be non-null");
            return false;
        }

        // Validate controllerMembers is not null
        if (controllerMembers == null) {
            LOGGER.error("TXG initialization requires controllerMembers to be non-null");
            return false;
        }

        // Validate group names are not empty/null
        boolean isEmpty = Arrays.stream(groupNames).anyMatch(StringUtils::isEmpty);
        if (isEmpty) {
            LOGGER.error("TXG group name cannot be null or empty. Groups: {}", Arrays.toString(groupNames));
            return false;
        }

        LOGGER.info(
                "TXG requirements validation passed: {} groups, {} nodes", groupNames.length, controllerMembers.size());
        return true;
    }

    private Map<String, List<Node>> createTxgGroupAssignments(String[] groupNames, List<PeerId> members) {
        Map<String, List<Node>> assignments = new HashMap<>();

        // Convert PeerId list to string list (IP:Port format)
        List<Node> memberEndpoints = members.stream()
                .map(peerId -> {
                    Node node = new Node();
                    node.setInternal(node.createEndpoint(peerId.getIp(), peerId.getPort(), "raft"));
                    return node;
                })
                .collect(Collectors.toList());

        // since there are only 3 nodes, assign all nodes to all groups
        // will need a better algorithm if we need to have dynamic numbers but for now lets keep it simple?
        // TXG-1: 192.168.1.100,192.168.1.101,192.168.1.102
        // TXG-2: 192.168.1.100,192.168.1.101,192.168.1.102
        // TXG-3: 192.168.1.100,192.168.1.101,192.168.1.102
        for (String groupName : groupNames) {
            String trimmedGroupName = groupName.trim();
            assignments.put(trimmedGroupName, memberEndpoints);
            LOGGER.info(
                    "Assigned all {} nodes to group {}: {}", memberEndpoints.size(), trimmedGroupName, memberEndpoints);
        }

        return assignments;
    }

    private void submitTxgGroupAssignments(Map<String, List<Node>> assignments) {
        try {
            // Create the DTO
            TxgGroupAssignmentDTO txgAssignments = new TxgGroupAssignmentDTO(assignments);

            // Create the message
            RaftTxgGroupMsg message = new RaftTxgGroupMsg(txgAssignments);
            message.setGroup(group); // Set the controller group

            // Submit to state machine so all controller followers get the assignments
            RaftTaskUtil.createTask(
                    status -> {
                        if (status.isOk()) {
                            LOGGER.info("TXG group assignments successfully replicated to all controller nodes");
                            // After successful replication, do we need to trigger something?
                            raftGroupStoreManager.saveOrUpdate(txgAssignments.getGroupMemberMap());
                        } else {
                            LOGGER.error("Failed to replicate TXG assignments: {}", status.getErrorMsg());
                        }
                    },
                    message, // The message containing group assignments
                    null);

        } catch (Exception e) {
            LOGGER.error("Failed to submit TXG group assignments", e);
        }
    }

    @Override
    public void onLeaderStop(final Status status) {
        this.leaderTerm.set(-1);
        LOGGER.info("controllerGroup: {}, onLeaderStop: status={}.", group, status);
    }

    @Override
    public void onStopFollowing(final LeaderChangeContext ctx) {
        LOGGER.info("controllerGroup: {}, onStopFollowing: {}.", group, ctx);
    }

    @Override
    public void onStartFollowing(final LeaderChangeContext ctx) {
        LOGGER.info("controllerGroup: {}, onStartFollowing: {}.", group, ctx);
        this.currentTerm.set(ctx.getTerm());
        CompletableFuture.runAsync(() -> syncControllerNodeInfo(ctx.getLeaderId()), CONTROLLER_METADATA_POOL);
    }

    @Override
    public void onConfigurationCommitted(Configuration conf) {
        LOGGER.info("controllerGroup: {}, onConfigurationCommitted: {}.", group, conf);
        RouteTable.getInstance().updateConfiguration(group, conf);
        // After a member change, the metadata needs to be synchronized again.
        initSync.compareAndSet(true, false);
        if (isLeader()) {
            changeControllerPeers(conf);
        }
    }

    private void changeControllerPeers(Configuration conf) {
        lock.lock();
        try {
            List<PeerId> newFollowers = conf.getPeers();
            Set<PeerId> newLearners = conf.getLearners();
            List<Node> currentFollowers = raftClusterMetadata.getFollowers();
            if (CollectionUtils.isNotEmpty(newFollowers)) {
                raftClusterMetadata.setFollowers(currentFollowers.stream()
                        .filter(node -> containsPeer(node, newFollowers))
                        .collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(newLearners)) {
                raftClusterMetadata.setLearner(raftClusterMetadata.getLearner().stream()
                        .filter(node -> containsPeer(node, newLearners))
                        .collect(Collectors.toList()));
            } else {
                raftClusterMetadata.setLearner(Collections.emptyList());
            }
            CompletableFuture.runAsync(this::syncControllerMetadata, CONTROLLER_METADATA_POOL);
        } finally {
            lock.unlock();
        }
    }

    private boolean containsPeer(Node node, Collection<PeerId> list) {
        // This indicates that the node is of a lower version.
        // When scaling up or down on a higher version
        // you need to ensure that the cluster is consistent first
        // otherwise, the lower version nodes may be removed.
        if (node.getInternal() == null) {
            return true;
        }
        PeerId nodePeer =
                new PeerId(node.getInternal().getHost(), node.getInternal().getPort());
        return list.contains(nodePeer);
    }

    public void syncControllerMetadata() {
        if (isLeader()) {
            SeataClusterContext.bindGroup(group);
            try {
                RaftClusterMetadataMsg controllerMetadataMsg =
                        new RaftClusterMetadataMsg(changeOrInitraftClusterMetadata());
                RaftTaskUtil.createTask(
                        status -> refreshControllerMetadata(controllerMetadataMsg), controllerMetadataMsg, null);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                SeataClusterContext.unbindGroup();
            }
        }
    }

    private void onExecuteControllerRaft(RaftBaseMsg msg) {
        RaftMsgExecute<?> execute = CONTROLLER_EXECUTES.get(msg.getMsgType());
        if (execute == null) {
            throw new RuntimeException(
                    "the controller state machine does not allow events that cannot be executed, please feedback the information to the Seata community !!! msg: "
                            + msg);
        }
        try {
            execute.execute(msg);
        } catch (Throwable e) {
            LOGGER.error(
                    "Controller message synchronization failure: {}, msgType: {}", e.getMessage(), msg.getMsgType(), e);
            throw new RuntimeException(e);
        }
    }

    public AtomicLong getCurrentTerm() {
        return super.getCurrentTerm();
    }

    public void registryStoreSnapshotFile(StoreSnapshotFile storeSnapshotFile) {
        snapshotFiles.add(storeSnapshotFile);
    }

    public RaftClusterMetadata getRaftClusterMetadata() {
        return raftClusterMetadata;
    }

    public void setRaftClusterMetadata(RaftClusterMetadata raftClusterMetadata) {
        this.raftClusterMetadata = raftClusterMetadata;
    }

    public RaftClusterMetadata changeOrInitraftClusterMetadata() {
        raftClusterMetadata.setTerm(this.currentTerm.get());
        Node leaderNode = raftClusterMetadata.getLeader();
        RaftControllerServer raftControllerServer =
                RaftControllerServerManager.getInstance().getRaftServer(group);
        PeerId currentPeerId = raftControllerServer.getServerId();

        // After the re-election, the leader information may be different from the latest leader, and you need to
        // replace the leader information
        if (leaderNode == null
                || (leaderNode.getInternal() != null
                        && !currentPeerId.equals(new PeerId(
                                leaderNode.getInternal().getHost(),
                                leaderNode.getInternal().getPort())))) {
            Node leader = raftClusterMetadata.createNode(
                    currentPeerId.getIp(),
                    XID.getPort(),
                    raftControllerServer.getServerId().getPort(),
                    Integer.parseInt(
                            ((Environment) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT))
                                    .getProperty("server.port", String.valueOf(7091))),
                    group,
                    Collections.emptyMap());
            leader.setRole(ClusterRole.LEADER);
            raftClusterMetadata.setLeader(leader);
        }
        return raftClusterMetadata;
    }

    public void refreshControllerMetadata(RaftBaseMsg syncMsg) {
        // Directly receive messages from the leader and update the controller cluster metadata
        if (syncMsg instanceof RaftClusterMetadataMsg) {
            raftClusterMetadata = ((RaftClusterMetadataMsg) syncMsg).getRaftClusterMetadata();
            ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                    .publishEvent(new ClusterChangeEvent(this, group, raftClusterMetadata.getTerm(), this.isLeader()));
            LOGGER.info("controllerGroup: {}, refresh controller cluster metadata: {}", group, raftClusterMetadata);
        }
    }

    private void syncControllerNodeInfo(String controllerGroup) {
        if (initSync.compareAndSet(false, true)) {
            try {
                RouteTable.getInstance()
                        .refreshLeader(
                                RaftControllerServerManager.getInstance().getCliClientServiceInstance(),
                                controllerGroup,
                                1000);
                PeerId peerId = RouteTable.getInstance().selectLeader(controllerGroup);
                if (peerId != null) {
                    syncControllerNodeInfo(peerId);
                } else {
                    initSync.compareAndSet(true, false);
                }
            } catch (Exception e) {
                initSync.compareAndSet(true, false);
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void syncControllerNodeInfo(PeerId leaderPeerId) {
        try {
            // Ensure that the current leader must be version 2.1 or later to synchronize the operation
            Node leader = raftClusterMetadata.getLeader();
            if (leader != null && StringUtils.isNotBlank(leader.getVersion())) {
                RaftControllerServer raftControllerServer =
                        RaftControllerServerManager.getInstance().getRaftServer(group);
                PeerId currentPeerId = raftControllerServer.getServerId();
                Node node = raftClusterMetadata.createNode(
                        currentPeerId.getIp(),
                        XID.getPort(),
                        currentPeerId.getPort(),
                        Integer.parseInt(((Environment)
                                        ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT))
                                .getProperty("server.port", String.valueOf(7091))),
                        group,
                        Collections.emptyMap());
                InvokeContext invokeContext = new InvokeContext();
                PutNodeMetadataRequest putNodeInfoRequest = new PutNodeMetadataRequest(node);
                Configuration configuration = RouteTable.getInstance().getConfiguration(group);
                node.setRole(
                        configuration.getPeers().contains(currentPeerId) ? ClusterRole.FOLLOWER : ClusterRole.LEARNER);
                invokeContext.put(
                        com.alipay.remoting.InvokeContext.BOLT_CUSTOM_SERIALIZER, SerializerType.JACKSON.getCode());
                CliClientServiceImpl cliClientService = (CliClientServiceImpl)
                        RaftControllerServerManager.getInstance().getCliClientServiceInstance();
                // The previous leader may be an old snapshot or log playback, which is not accurate, and you
                // need to get the leader again
                cliClientService
                        .getRpcClient()
                        .invokeAsync(
                                leaderPeerId.getEndpoint(),
                                putNodeInfoRequest,
                                invokeContext,
                                (result, err) -> {
                                    if (err == null) {
                                        PutNodeMetadataResponse putNodeMetadataResponse =
                                                (PutNodeMetadataResponse) result;
                                        if (putNodeMetadataResponse.isSuccess()) {
                                            scheduledFuture.cancel(true);
                                            LOGGER.info(
                                                    "sync controller node info to leader: {}, result: {}",
                                                    leaderPeerId,
                                                    result);
                                        } else {
                                            initSync.compareAndSet(true, false);
                                            LOGGER.info(
                                                    "sync controller node info to leader: {}, result: {}, retry will be made at the time of the re-election or after 10 seconds",
                                                    leaderPeerId,
                                                    result);
                                        }
                                    } else {
                                        initSync.compareAndSet(true, false);
                                        LOGGER.error(
                                                "sync controller node info to leader: {}, error: {}",
                                                leaderPeerId,
                                                err.getMessage(),
                                                err);
                                    }
                                },
                                30000);
            } else {
                initSync.compareAndSet(true, false);
            }
        } catch (Exception e) {
            initSync.compareAndSet(true, false);
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void changeControllerNodeMetadata(Node node) {
        lock.lock();
        try {
            List<Node> list = node.getRole() == ClusterRole.FOLLOWER
                    ? raftClusterMetadata.getFollowers()
                    : raftClusterMetadata.getLearner();
            // If the node currently exists, modify it
            for (Node follower : list) {
                Node.Endpoint endpoint = follower.getInternal();
                if (endpoint != null) {
                    // change old follower node metadata
                    if (endpoint.getHost().equals(node.getInternal().getHost())
                            && endpoint.getPort() == node.getInternal().getPort()) {
                        follower.setTransaction(node.getTransaction());
                        follower.setControl(node.getControl());
                        follower.setGroup(group);
                        follower.setMetadata(node.getMetadata());
                        follower.setVersion(node.getVersion());
                        follower.setRole(node.getRole());
                        return;
                    }
                }
            }
            // add new node node metadata
            list.add(node);
            syncControllerMetadata();
        } finally {
            lock.unlock();
        }
    }
}
