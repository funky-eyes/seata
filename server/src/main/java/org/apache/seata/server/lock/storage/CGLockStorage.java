package org.apache.seata.server.lock.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Multi-raft lock storage structure
 */
public class CGLockStorage {

    private static final int BUCKET_PER_TABLE = 128;

    /**
     * Updated storage structure:
     * resourceId -> instance -> bucketId -> BucketLockMap
     */
    private static final ConcurrentMap<
                    String /* resource */,
                    ConcurrentMap<String /* instance */, ConcurrentMap<Integer /* bucketId */, BucketLockMap>>>
            LOCK_MAP = new ConcurrentHashMap<>();

    public static ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>>> getLockMap() {
        return LOCK_MAP;
    }

    public static int getBucketPerTable() {
        return BUCKET_PER_TABLE;
    }

    public static void clearAllLocks() {
        LOCK_MAP.clear();
    }
}
