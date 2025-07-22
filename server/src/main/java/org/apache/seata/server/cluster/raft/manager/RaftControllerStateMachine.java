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
import com.alipay.sofa.jraft.core.StateMachineAdapter;
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
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.cluster.raft.execute.RaftMsgExecute;
import org.apache.seata.server.cluster.raft.processor.request.PutNodeMetadataRequest;
import org.apache.seata.server.cluster.raft.processor.response.PutNodeMetadataResponse;
import org.apache.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.metadata.LeaderMetadataSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.vgroup.VGroupSnapshotFile;
import org.apache.seata.server.cluster.raft.sync.RaftSyncMessageSerializer;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBaseMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftClusterMetadataMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftGroupMetadataMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;
import org.apache.seata.server.store.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;

public class RaftControllerStateMachine extends StateMachineAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftControllerStateMachine.class);

    private final String mode;

    private final String group;

    private final List<StoreSnapshotFile> snapshotFiles = new ArrayList<>();

    private static final Map<RaftSyncMsgType, RaftMsgExecute<?>> CONTROLLER_EXECUTES = new HashMap<>();

    // Metadata for all managed raft groups
    private volatile Map<String, Object> allGroupsMetadata = new ConcurrentHashMap<>();

    // Controller-specific cluster metadata
    private volatile RaftClusterMetadata raftClusterMetadata = new RaftClusterMetadata();

    private final Lock lock = new ReentrantLock();

    private static final ScheduledThreadPoolExecutor CONTROLLER_METADATA_POOL =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("reSyncControllerMetadataPool", 1, true));

    /**
     * Leader term
     */
    private final AtomicLong leaderTerm = new AtomicLong(-1);

    /**
     * current term
     */
    private final AtomicLong currentTerm = new AtomicLong(-1);

    private final AtomicBoolean initSync = new AtomicBoolean(false);

    private ScheduledFuture<?> scheduledFuture;

    public boolean isLeader() {
        return this.leaderTerm.get() > 0;
    }

    public RaftControllerStateMachine(String group) {
        this.group = group;
        mode = StoreConfig.getSessionMode().getName();

        // Register controller-specific message handlers
        CONTROLLER_EXECUTES.put(RaftSyncMsgType.REFRESH_CLUSTER_METADATA, syncMsg -> {
            refreshControllerMetadata(syncMsg);
            return null;
        });

        // Add controller-specific message types for managing multiple groups
        CONTROLLER_EXECUTES.put(RaftSyncMsgType.ADD_RAFT_GROUP, syncMsg -> {
            addRaftGroupMetadata(syncMsg);
            return null;
        });

        CONTROLLER_EXECUTES.put(RaftSyncMsgType.REMOVE_RAFT_GROUP, syncMsg -> {
            removeRaftGroupMetadata(syncMsg);
            return null;
        });

        CONTROLLER_EXECUTES.put(RaftSyncMsgType.UPDATE_RAFT_GROUP_METADATA, syncMsg -> {
            updateRaftGroupMetadata(syncMsg);
            return null;
        });

        // Register controller-specific snapshot files
        registryStoreSnapshotFile(new LeaderMetadataSnapshotFile(group));
        registryStoreSnapshotFile(new VGroupSnapshotFile(group));

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

        if (!leader && RaftControllerServerManager.isControllerMode()) {
            CompletableFuture.runAsync(() -> {
                LOGGER.info("controller became leader, managing {} raft groups", allGroupsMetadata.size());
                SeataClusterContext.bindGroup(group);
                try {
                    // Reload controller metadata and managed groups
                    reloadControllerState();
                } finally {
                    SeataClusterContext.unbindGroup();
                }
            });
            Configuration conf = RouteTable.getInstance().getConfiguration(group);
            // A member change might trigger a leader re-election. At this point, it's necessary to filter out
            // non-existent members and synchronize again.
            changeControllerPeers(conf);
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
        PeerId nodePeer = new PeerId(node.getInternal().getHost(), node.getInternal().getPort());
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
            LOGGER.error("Controller message synchronization failure: {}, msgType: {}", e.getMessage(), msg.getMsgType(), e);
            throw new RuntimeException(e);
        }
    }

    public AtomicLong getCurrentTerm() {
        return currentTerm;
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

    public Map<String, Object> getAllGroupsMetadata() {
        return new HashMap<>(allGroupsMetadata);
    }

    public boolean addManagedGroup(String groupId, Map<String, Object> groupMetadata) {
        if (isLeader()) {
            lock.lock();
            try {
                allGroupsMetadata.put(groupId, groupMetadata);
                // Sync this change to followers
                RaftGroupMetadataMsg msg = new RaftGroupMetadataMsg(RaftSyncMsgType.ADD_RAFT_GROUP, groupId, groupMetadata);
                RaftTaskUtil.createTask(status -> addRaftGroupMetadata(msg), msg, null);
                LOGGER.info("Added managed group: {}", groupId);
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to add managed group: {}", groupId, e);
                return false;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    public boolean removeManagedGroup(String groupId) {
        if (isLeader()) {
            lock.lock();
            try {
                Object removed = allGroupsMetadata.remove(groupId);
                if (removed != null) {
                    // Sync this change to followers
                    RaftGroupMetadataMsg msg = new RaftGroupMetadataMsg(RaftSyncMsgType.REMOVE_RAFT_GROUP, groupId, null);
                    RaftTaskUtil.createTask(status -> removeRaftGroupMetadata(msg), msg, null);
                    LOGGER.info("Removed managed group: {}", groupId);
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to remove managed group: {}", groupId, e);
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    public boolean updateGroupMetadata(String groupId, Map<String, Object> newMetadata) {
        if (isLeader() && allGroupsMetadata.containsKey(groupId)) {
            lock.lock();
            try {
                allGroupsMetadata.put(groupId, newMetadata);
                // Sync this change to followers
                RaftGroupMetadataMsg msg = new RaftGroupMetadataMsg(RaftSyncMsgType.UPDATE_RAFT_GROUP_METADATA, groupId, newMetadata);
                RaftTaskUtil.createTask(status -> updateRaftGroupMetadata(msg), msg, null);
                LOGGER.info("Updated metadata for group: {}", groupId);
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to update metadata for group: {}", groupId, e);
                return false;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    private void addRaftGroupMetadata(RaftBaseMsg syncMsg) {
        RaftGroupMetadataMsg msg = (RaftGroupMetadataMsg) syncMsg;
        allGroupsMetadata.put(msg.getGroupId(), msg.getGroupMetadata());
        LOGGER.info("Follower added managed group: {}", msg.getGroupId());
    }

    private void removeRaftGroupMetadata(RaftBaseMsg syncMsg) {
        RaftGroupMetadataMsg msg = (RaftGroupMetadataMsg) syncMsg;
        allGroupsMetadata.remove(msg.getGroupId());
        LOGGER.info("Follower removed managed group: {}", msg.getGroupId());
    }

    private void updateRaftGroupMetadata(RaftBaseMsg syncMsg) {
        RaftGroupMetadataMsg msg = (RaftGroupMetadataMsg) syncMsg;
        allGroupsMetadata.put(msg.getGroupId(), msg.getGroupMetadata());
        LOGGER.info("Follower updated metadata for group: {}", msg.getGroupId());
    }

    public RaftClusterMetadata changeOrInitraftClusterMetadata() {
        raftClusterMetadata.setTerm(this.currentTerm.get());
        Node leaderNode = raftClusterMetadata.getLeader();
        RaftControllerServer raftControllerServer = RaftControllerServerManager.getRaftControllerServer(group);
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
                RouteTable.getInstance().refreshLeader(RaftControllerServerManager.getCliClientServiceInstance(), controllerGroup, 1000);
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
                RaftControllerServer raftControllerServer = RaftControllerServerManager.getRaftControllerServer(group);
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
                CliClientServiceImpl cliClientService =
                        (CliClientServiceImpl) RaftControllerServerManager.getCliClientServiceInstance();
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
                                                    "sync controller node info to leader: {}, result: {}", leaderPeerId, result);
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

    private void reloadControllerState() {
        // Reload controller-specific state
        LOGGER.info("Reloading controller state for managing {} groups", allGroupsMetadata.size());
        // Add any controller-specific reload logic here
    }










}
