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
package org.apache.seata.server.controller;

import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.result.Result;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.cluster.raft.service.TransactionGroupServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Raft group management operations
 */
@RestController
@RequestMapping("/api/raft")
public class RaftGroupController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftGroupController.class);

    @Resource
    private TransactionGroupServiceManager raftGroupService;

    /**
     * Get all Raft groups and their members
     */
    @GetMapping("/groups")
    public SingleResult<Map<String, List<Node>>> getAllRaftGroups() {
        try {
            Map<String, List<Node>> groups = raftGroupService.getAllRaftGroups();
            return SingleResult.success(groups);
        } catch (Exception e) {
            LOGGER.error("Error retrieving all Raft groups", e);
            return SingleResult.failure("500", "Failed to retrieve Raft groups: " + e.getMessage());
        }
    }

    /**
     * Get Raft groups by IP address
     */
    @GetMapping("/groups/by-ip")
    public SingleResult<Map<String, List<Node>>> getRaftGroupsByIp(@RequestParam String ip) {
        try {
            Map<String, List<Node>> groups = raftGroupService.getRaftGroupsByIp(ip);
            return SingleResult.success(groups);
        } catch (IllegalArgumentException e) {
            return SingleResult.failure("400", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error searching groups by IP: {}", ip, e);
            return SingleResult.failure("500", "Failed to search groups by IP: " + e.getMessage());
        }
    }

    /**
     * Add a peer to a Raft group
     */
    @PostMapping("/groups/{group}/add-peer")
    public Result<?> addPeer(@PathVariable String group,
                             @RequestParam String ip,
                             @RequestParam int port) {
        Result<?> result = new Result<>();

        try {
            raftGroupService.addPeer(group, ip, port);
            result.setMessage("Successfully added peer " + ip + ":" + port + " to group " + group);
        } catch (IllegalArgumentException e) {
            result.setCode("400");
            result.setMessage(e.getMessage());
        } catch (Exception e) {
            result.setCode("500");
            result.setMessage("Failed to add peer: " + e.getMessage());
            LOGGER.error("Failed to add peer {}:{} to group {}", ip, port, group, e);
        }

        return result;
    }

    /**
     * Remove a peer from a Raft group
     */
    @PostMapping("/groups/{group}/remove-peer")
    public Result<?> removePeer(@PathVariable String group,
                                @RequestParam String ip,
                                @RequestParam int port) {
        Result<?> result = new Result<>();

        try {
            raftGroupService.removePeer(group, ip, port);
            result.setMessage("Successfully removed peer " + ip + ":" + port + " from group " + group);
        } catch (IllegalArgumentException e) {
            result.setCode("400");
            result.setMessage(e.getMessage());
        } catch (Exception e) {
            result.setCode("500");
            result.setMessage("Failed to remove peer: " + e.getMessage());
            LOGGER.error("Failed to remove peer {}:{} from group {}", ip, port, group, e);
        }

        return result;
    }


    /**
     * Get the current leader of a specific group
     */
    @GetMapping("/groups/{group}/leader")
    public SingleResult<Map<String, Object>> getGroupLeader(@PathVariable String group) {
        try {
            Map<String, Object> leaderInfo = raftGroupService.getGroupLeader(group);

            if (leaderInfo.containsKey("error")) {
                return SingleResult.failure("404", (String) leaderInfo.get("error"));
            } else {
                return SingleResult.success(leaderInfo);
            }
        } catch (IllegalArgumentException e) {
            return SingleResult.failure("400", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error getting leader for group {}", group, e);
            return SingleResult.failure("500", "Failed to get leader info: " + e.getMessage());
        }
    }

    /**
     * Change peers for multiple Raft groups (Control Group members only)
     */
    @PostMapping("/change-peers")
    public Result<?> changePeers(@RequestBody Map<String, List<String>> groupPeersMap) {
        Result<?> result = new Result<>();

        try {
            raftGroupService.changePeers(groupPeersMap);
            result.setMessage("Successfully changed peers for " + groupPeersMap.size() + " group(s)");
        } catch (SecurityException e) {
            result.setCode("403");
            result.setMessage(e.getMessage());
        } catch (IllegalArgumentException e) {
            result.setCode("400");
            result.setMessage(e.getMessage());
        } catch (Exception e) {
            result.setCode("500");
            result.setMessage("Failed to change peers: " + e.getMessage());
            LOGGER.error("Failed to change peers for groups: {}", groupPeersMap.keySet(), e);
        }

        return result;
    }

    /**
     * Check if the current node has permission to modify group membership
     */
    @GetMapping("/permissions/change-peers")
    public SingleResult<Boolean> hasChangePeersPermission() {
        try {
            boolean hasPermission = raftGroupService.hasChangePeersPermission();
            return SingleResult.success(hasPermission);
        } catch (Exception e) {
            LOGGER.error("Error checking change peers permission", e);
            return SingleResult.failure("500", "Failed to check permissions: " + e.getMessage());
        }
    }
}