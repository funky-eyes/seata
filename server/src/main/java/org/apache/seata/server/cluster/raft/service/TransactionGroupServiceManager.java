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
package org.apache.seata.server.cluster.raft.service;

import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.server.cluster.raft.RaftTransactionServer;
import org.apache.seata.server.cluster.raft.RaftTransactionServerManager;
import org.apache.seata.server.cluster.raft.RaftTransactionStateMachine;
import org.apache.seata.server.cluster.raft.manager.RaftControllerServerManager;
import org.apache.seata.server.cluster.raft.sync.msg.RaftTxgGroupMsg;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service for managing Raft groups and cluster operations
 */
@Service
public class TransactionGroupServiceManager implements RaftGroupStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionGroupServiceManager.class);

    private static final Map<String /*group*/, List<Node>> TRANSACTION_GROUPS = new ConcurrentHashMap<>();

    // regex Pattern to validate IP:PORT format? or should we remove it?
    private static final Pattern IP_PORT_PATTERN =
            Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}:[1-9]\\d{0,4}$");

    /**
     * Get all Raft groups
     */
    public Map<String, List<Node>> getAllRaftGroups() {
        return TRANSACTION_GROUPS;
    }

    /**
     * Get Raft groups that contain the specified IP address
     */
    public Map<String, List<Node>> getRaftGroupsByIp(String ip) {
        if (hasChangePeersPermission()) {
            // todo: If not a CG member, the request should be forwarded to the CG node
        }
        if (ip == null || ip.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }

        return getGroupByIp(ip);
    }

    @NotNull
    private Map<String, List<Node>> getGroupByIp(String ip) {
        Map<String, List<Node>> allGroups = getAllRaftGroups();
        Map<String, List<Node>> result = new HashMap<>();
        allGroups.forEach((group, nodes) -> {
            List<Node> matchedNodes = new ArrayList<>();
            for (Node node : nodes) {
                if (ip.equals(node.getTransaction().getHost())) {
                    matchedNodes.add(node);
                }
            }
            if (!matchedNodes.isEmpty()) {
                result.put(group, matchedNodes);
            }
        });
        return result;
    }

    /**
     * Add a peer to the specified Raft group
     * not sure if this is the correct impl, i havent grasped all the parts of raft yet
     *
     * also do we use ports or just ips? i know you sent ips but dont we need port for add peer?
     */
    public void addPeer(String group, String ip, int port) throws Exception {
        validateGroupExists(group);
        validatePeerParameters(ip, port);

        try {
            PeerId newPeer = new PeerId(ip, port);
            RouteTable routeTable = RouteTable.getInstance();
            Configuration currentConf = routeTable.getConfiguration(group);

            if (currentConf == null) {
                throw new IllegalStateException("No configuration found for group: " + group);
            }

            // Check if peer already exists
            if (currentConf.getPeers().contains(newPeer)
                    || currentConf.getLearners().contains(newPeer)) {
                throw new IllegalArgumentException("Peer " + ip + ":" + port + " already exists in group " + group);
            }

            // Create new configuration with the additional peer
            Configuration newConf = new Configuration(currentConf);
            newConf.addPeer(newPeer);

            // Apply the configuration change through CLI service
            RaftTransactionServerManager.getInstance().getCliServiceInstance().changePeers(group, currentConf, newConf);

            // Update local routing table
            routeTable.updateConfiguration(group, newConf);

            LOGGER.info("Successfully added peer {}:{} to group {}", ip, port, group);

        } catch (Exception e) {
            LOGGER.error("Failed to add peer {}:{} to group {}", ip, port, group, e);
            throw new Exception("Failed to add peer: " + e.getMessage(), e);
        }
    }

    /**
     * Remove a peer from the specified Raft group
     * same as above
     */
    public void removePeer(String group, String ip, int port) throws Exception {
        validateGroupExists(group);
        validatePeerParameters(ip, port);

        try {
            PeerId peerToRemove = new PeerId(ip, port);
            RouteTable routeTable = RouteTable.getInstance();
            Configuration currentConf = routeTable.getConfiguration(group);

            if (currentConf == null) {
                throw new IllegalStateException("No configuration found for group: " + group);
            }

            // Check if peer exists
            if (!currentConf.getPeers().contains(peerToRemove)
                    && !currentConf.getLearners().contains(peerToRemove)) {
                throw new IllegalArgumentException("Peer " + ip + ":" + port + " does not exist in group " + group);
            }

            // Don't allow removing the last peer
            if (currentConf.getPeers().size() <= 1) {
                throw new IllegalStateException("Cannot remove the last peer from group " + group);
            }

            // do we create new configuration without the peer
            // is that the correct way?
            Configuration newConf = new Configuration(currentConf);
            newConf.removePeer(peerToRemove);

            // Apply the configuration change
            RaftTransactionServerManager.getInstance().getCliServiceInstance().changePeers(group, currentConf, newConf);

            // Update local routing table
            routeTable.updateConfiguration(group, newConf);

            LOGGER.info("Successfully removed peer {}:{} from group {}", ip, port, group);

        } catch (Exception e) {
            LOGGER.error("Failed to remove peer {}:{} from group {}", ip, port, group, e);
            throw new Exception("Failed to remove peer: " + e.getMessage(), e);
        }
    }

    @Override
    public void changePeers(Map<String, List<String>> groupPeersMap) {
        validateControlGroupPermission();
        validateChangePeersParameters(groupPeersMap);

        LOGGER.info("Attempting to change peers for groups: {}", groupPeersMap.keySet());

        // Process each group
        for (Map.Entry<String, List<String>> entry : groupPeersMap.entrySet()) {
            String groupId = entry.getKey();
            List<String> newPeers = entry.getValue();

            try {
                changePeersForGroup(groupId, newPeers);
                LOGGER.info("Successfully changed peers for group {} to: {}", groupId, newPeers);
            } catch (Exception e) {
                LOGGER.error("Failed to change peers for group {} to: {}", groupId, newPeers, e);
                throw new RuntimeException("Failed to change peers for group " + groupId + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Check if the current node has permission to modify group membership
     * Only Control Group members have this permission
     * do we also check if node is leader?
     */
    public boolean hasChangePeersPermission() {
        return RaftControllerServerManager.getInstance().getRaftServer("controller") != null;
    }

    /**
     * Change peers for a specific group
     */
    private void changePeersForGroup(String groupId, List<String> newPeers) throws Exception {
        // Parse new peer addresses
        List<PeerId> newPeerIds = parsePeerAddresses(newPeers);
        List<Node> list = new ArrayList<>(newPeers.size());
        List<Node> currentList = TRANSACTION_GROUPS.get(groupId);
        for (PeerId newPeer : newPeerIds) {
            boolean exists = false;
            for (Node node : currentList) {
                if (newPeer.getIp().equals(node.getInternal().getHost())
                        && newPeer.getPort() == node.getInternal().getPort()) {
                    list.add(node);
                    exists = true;
                }
            }
            if (!exists) {
                Node node = new Node();
                node.setInternal(node.createEndpoint(newPeer.getIp(), newPeer.getPort(), "raft"));
                list.add(node);
            }
        }
        Map<String, List<Node>> groupPeersMap = new HashMap<>();
        groupPeersMap.put(groupId, list);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        RaftTxgGroupMsg raftTxgGroupMsg = RaftTxgGroupMsg.create(groupPeersMap);
        RaftTaskUtil.createTask(
                status -> {
                    if (!status.isOk()) {
                        LOGGER.error("Failed to change peers for group {}: {}", groupId, status.getErrorMsg());
                        future.completeExceptionally(new RuntimeException(
                                "Failed to change peers for group " + groupId + ": " + status.getErrorMsg()));
                        return;
                    }
                    // Get current configuration
                    RouteTable routeTable = RouteTable.getInstance();
                    Configuration currentConf = routeTable.getConfiguration(groupId);
                    if (currentConf == null) {
                        throw new IllegalArgumentException("Group not found: " + groupId);
                    }

                    // Create new configuration
                    Configuration newConf = new Configuration();
                    for (PeerId peerId : newPeerIds) {
                        newConf.addPeer(peerId);
                    }

                    // Apply the configuration change
                    CliService cliService = getCliService(groupId);
                    cliService.changePeers(groupId, currentConf, newConf);

                    // Update local routing table
                    routeTable.updateConfiguration(groupId, newConf);
                    future.complete(true);
                },
                raftTxgGroupMsg,
                future);
    }

    /**
     * Validate that the current node has Control Group permissions
     */
    private void validateControlGroupPermission() throws SecurityException {
        if (!hasChangePeersPermission()) {
            throw new SecurityException("Access denied: Only Control Group members can modify group membership");
        }
    }

    /**
     * Validate changePeers input parameters
     */
    private void validateChangePeersParameters(Map<String, List<String>> groupPeersMap) {
        if (groupPeersMap == null || groupPeersMap.isEmpty()) {
            throw new IllegalArgumentException("Group peers map cannot be null or empty");
        }

        for (Map.Entry<String, List<String>> entry : groupPeersMap.entrySet()) {
            String groupId = entry.getKey();
            List<String> peers = entry.getValue();

            if (groupId == null || groupId.trim().isEmpty()) {
                throw new IllegalArgumentException("Group ID cannot be null or empty");
            }

            if (peers == null || peers.isEmpty()) {
                throw new IllegalArgumentException("Peer list cannot be null or empty for group: " + groupId);
            }

            // Validate each peer address format
            for (String peer : peers) {
                if (peer == null || peer.trim().isEmpty()) {
                    throw new IllegalArgumentException("Peer address cannot be null or empty in group: " + groupId);
                }

                if (!IP_PORT_PATTERN.matcher(peer.trim()).matches()) {
                    throw new IllegalArgumentException("Invalid peer address format: " + peer + " in group: " + groupId
                            + ". Expected format: ip:port (e.g., 192.168.1.100:8091)");
                }
            }
        }
    }

    /**
     * Parse peer addresses from string format to PeerId objects
     */
    private List<PeerId> parsePeerAddresses(List<String> peerAddresses) {
        List<PeerId> peerIds = new ArrayList<>();

        for (String address : peerAddresses) {
            String[] parts = address.trim().split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid peer address format: " + address);
            }

            try {
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);
                peerIds.add(new PeerId(ip, port));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port in peer address: " + address, e);
            }
        }

        return peerIds;
    }

    /**
     * Get the appropriate CliService for the given group
     */
    private CliService getCliService(String groupId) {
        // Check if it's a transaction group first
        if (RaftTransactionServerManager.groups().contains(groupId)) {
            return RaftTransactionServerManager.getInstance().getCliServiceInstance();
        }

        // Check if it's a controller group
        RaftControllerServerManager controllerManager = RaftControllerServerManager.getInstance();
        if (controllerManager.getRaftGroups().contains(groupId)) {
            return controllerManager.getCliServiceInstance();
        }

        throw new IllegalArgumentException("Group not found in any manager: " + groupId);
    }

    /**
     * Get the current leader information for a specific group
     */
    public Map<String, Object> getGroupLeader(String group) {
        validateGroupExists(group);

        Map<String, Object> result = new HashMap<>();

        try {
            RaftTransactionServer raftServer =
                    RaftTransactionServerManager.getInstance().getRaftServer(group);

            if (raftServer != null) {
                RaftTransactionStateMachine stateMachine =
                        (RaftTransactionStateMachine) raftServer.getRaftStateMachine();
                RaftClusterMetadata metadata = stateMachine.getRaftLeaderMetadata();

                if (metadata.getLeader() != null) {
                    result = nodeToMap(metadata.getLeader(), "LEADER");
                    result.put("term", metadata.getTerm());
                    result.put("isCurrentNodeLeader", stateMachine.isLeader());
                } else {
                    result.put("error", "No leader found for group " + group);
                }
            } else {
                result.put("error", "Group not found: " + group);
            }
        } catch (Exception e) {
            LOGGER.error("Error getting leader for group {}", group, e);
            result.put("error", "Error getting leader info: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get detailed information for a specific group
     */
    private Map<String, Object> buildGroupInfo(String group) {
        try {
            RaftTransactionServer raftServer =
                    RaftTransactionServerManager.getInstance().getRaftServer(group);

            if (raftServer == null) {
                return null;
            }

            Map<String, Object> groupInfo = new HashMap<>();

            // Get cluster metadata
            RaftTransactionStateMachine stateMachine = raftServer.getRaftStateMachine();
            RaftClusterMetadata metadata = stateMachine.getRaftLeaderMetadata();

            // Get current configuration
            Configuration conf = RouteTable.getInstance().getConfiguration(group);

            // Collect member information
            List<Map<String, Object>> members = new ArrayList<>();

            // Add leader
            if (metadata.getLeader() != null) {
                members.add(nodeToMap(metadata.getLeader(), "LEADER"));
            }

            // Add followers
            if (metadata.getFollowers() != null) {
                for (Node follower : metadata.getFollowers()) {
                    members.add(nodeToMap(follower, "FOLLOWER"));
                }
            }

            // Add learners
            if (metadata.getLearner() != null) {
                for (Node learner : metadata.getLearner()) {
                    members.add(nodeToMap(learner, "LEARNER"));
                }
            }

            groupInfo.put("members", members);
            groupInfo.put("term", metadata.getTerm());
            groupInfo.put("isLeader", stateMachine.isLeader());
            groupInfo.put("peerCount", conf != null ? conf.getPeers().size() : 0);
            groupInfo.put("learnerCount", conf != null ? conf.getLearners().size() : 0);

            return groupInfo;

        } catch (Exception e) {
            LOGGER.error("Error building group info for group {}", group, e);
            return null;
        }
    }

    /**
     * Check if a group contains the specified IP address
     */
    private boolean groupContainsIp(Map<String, Object> groupInfo, String ip) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> members = (List<Map<String, Object>>) groupInfo.get("members");

        if (members == null) {
            return false;
        }

        return members.stream().anyMatch(member -> ip.equals(member.get("ip")));
    }

    /**
     * Convert Node to Map for consistent response format
     */
    private Map<String, Object> nodeToMap(Node node, String role) {
        Map<String, Object> nodeMap = new HashMap<>();

        if (node.getTransaction() != null) {
            nodeMap.put("ip", node.getTransaction().getHost());
            nodeMap.put("transactionPort", node.getTransaction().getPort());
        }

        if (node.getControl() != null) {
            nodeMap.put("controlPort", node.getControl().getPort());
        }

        if (node.getInternal() != null) {
            nodeMap.put("raftPort", node.getInternal().getPort());
        }

        nodeMap.put("role", role);
        nodeMap.put("group", node.getGroup());
        nodeMap.put("version", node.getVersion());

        if (node.getMetadata() != null) {
            nodeMap.put("metadata", node.getMetadata());
        }

        return nodeMap;
    }

    /**
     * Validate that a group exists
     */
    private void validateGroupExists(String group) {
        if (group == null || group.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }

        if (!RaftTransactionServerManager.groups().contains(group)) {
            throw new IllegalArgumentException("Group does not exist: " + group);
        }
    }

    /**
     * Validate peer parameters
     */
    private void validatePeerParameters(String ip, int port) {
        if (ip == null || ip.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }

        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
    }

    @Override
    public void saveOrUpdate(Map<String, List<Node>> groupPeersMap) {
        if (CollectionUtils.isEmpty(groupPeersMap)) {
            return;
        }
        TRANSACTION_GROUPS.putAll(groupPeersMap);
    }

    @Override
    public void clear() {
        TRANSACTION_GROUPS.clear();
    }

    @Override
    public Map<String, List<Node>> getGroupPeersMap() {
        return TRANSACTION_GROUPS;
    }
}
