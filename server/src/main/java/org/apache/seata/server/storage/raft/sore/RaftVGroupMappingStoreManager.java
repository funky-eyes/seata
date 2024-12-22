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
package org.apache.seata.server.storage.raft.sore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.alipay.sofa.jraft.Closure;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.metadata.namingserver.Instance;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType;
import org.apache.seata.server.cluster.raft.sync.msg.RaftVGroupSyncMsg;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;
import org.apache.seata.server.store.VGroupMappingStoreManager;

@LoadLevel(name = "raft")
public class RaftVGroupMappingStoreManager implements VGroupMappingStoreManager {

    private final static Map<String/*unit(raft group)*/, Map<String/*vgroup*/, MappingDO>> VGROUP_MAPPING =
        new HashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();
    
    private final Lock writeLock = lock.writeLock();


    public boolean localAddVGroup(MappingDO mappingDO) {
        writeLock.lock();
        try {
            return VGROUP_MAPPING.computeIfAbsent(mappingDO.getUnit(), k -> new HashMap<>()).put(mappingDO.getVGroup(),
                mappingDO) != null;
        } finally {
            writeLock.unlock();
        }
    }

    public void localAddVGroups(Map<String/*vgroup*/, MappingDO> vGroups, String unit) {
        writeLock.lock();
        try {
            VGROUP_MAPPING.computeIfAbsent(unit, k -> new HashMap<>()).putAll(vGroups);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean addVGroup(MappingDO mappingDO) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = status -> {
            if (status.isOk()) {
                completableFuture.complete(localAddVGroup(mappingDO));
            } else {
                completableFuture.complete(false);
            }
        };
        RaftVGroupSyncMsg raftVGroupSyncMsg = new RaftVGroupSyncMsg(mappingDO, RaftSyncMsgType.ADD_VGROUP_MAPPING);
        try {
            RaftTaskUtil.createTask(closure, raftVGroupSyncMsg, completableFuture);
            return completableFuture.get();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean removeVGroup(String vGroup) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = status -> {
            if (status.isOk()) {
                completableFuture.complete(localRemoveVGroup(vGroup));
            } else {
                completableFuture.complete(false);
            }
        };
        MappingDO mappingDO = new MappingDO();
        mappingDO.setVGroup(vGroup);
        RaftVGroupSyncMsg raftVGroupSyncMsg = new RaftVGroupSyncMsg(mappingDO, RaftSyncMsgType.REMOVE_VGROUP_MAPPING);
        try {
            RaftTaskUtil.createTask(closure, raftVGroupSyncMsg, completableFuture);
            return completableFuture.get();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RuntimeException(e);
        }
    }

    public boolean localRemoveVGroup(String vGroup) {
        writeLock.lock();
        try {
            VGROUP_MAPPING.forEach((unit, vgroup) -> vgroup.remove(vGroup));
        } finally {
            writeLock.unlock();
        }
        return true;
    }

    @Override
    public Map<String, Object> loadVGroups() {
        Map<String, Object> result = new HashMap<>();
        String clusterName = Instance.getInstance().getClusterName();
        readLock.lock();
        try {
            result.put(clusterName, VGROUP_MAPPING);
        } finally {
            readLock.unlock();
        }
        return result;
    }

    public Map<String/*vgroup*/, MappingDO> loadVGroupsByUnit(String unit) {
        return VGROUP_MAPPING.getOrDefault(unit, Collections.emptyMap());
    }

    @Override
    public Map<String, Object> readVGroups() {
        return loadVGroups();
    }

}
