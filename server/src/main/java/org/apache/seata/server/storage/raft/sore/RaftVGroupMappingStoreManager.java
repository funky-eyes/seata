package org.apache.seata.server.storage.raft.sore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.store.VGroupMappingStoreManager;

@LoadLevel(name = "raft")
public class RaftVGroupMappingStoreManager implements VGroupMappingStoreManager {

    public static final String ROOT_MAPPING_MANAGER_NAME = "vgroup_mapping.json";

    private final ReadWriteLock                   lock      = new ReentrantReadWriteLock();
    private final Lock writeLock = lock.writeLock();

    private final Lock readLock = lock.readLock();

    private String storePath;

    Map<String/*cluster*/, Object> vGroupMapping = new HashMap<>();

    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Override
    public boolean addVGroup(MappingDO mappingDO) {
        writeLock.lock();
        try {
            Map<String/*unit(raft group)*/, Map<String/*vgroup*/, MappingDO>> unitVGroupMap =
                (Map<String, Map<String, MappingDO>>)vGroupMapping.get(mappingDO.getCluster());
            if (unitVGroupMap == null) {
                unitVGroupMap = new HashMap<>();
                vGroupMapping.put(mappingDO.getCluster(), unitVGroupMap);
            }
            Map<String/*vgroup*/, MappingDO> vgroup =
                unitVGroupMap.computeIfAbsent(mappingDO.getUnit(), value -> new HashMap<>());
            return vgroup.put(mappingDO.getVGroup(), mappingDO) != null;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeVGroup(String vGroup) {
        writeLock.lock();
        try {
            vGroupMapping.forEach((cluster, units) -> {
                Map<String/*unit(raft group)*/, Map<String/*vgroup*/, MappingDO>> unitVGroupMap =
                    (Map<String/*unit(raft group)*/, Map<String/*vgroup*/, MappingDO>>)units;
                unitVGroupMap.forEach((unit, mapping) -> mapping.forEach((vGroupName, mappingDO) -> {
                    if (vGroup.equalsIgnoreCase(mappingDO.getVGroup())) {
                        mapping.remove(vGroup);
                    }
                }));
            });
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Map<String, Object> loadVGroups() {
        readLock.lock();
        try {
            return vGroupMapping;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Map<String, Object> readVGroups() {
        return loadVGroups();
    }

}
