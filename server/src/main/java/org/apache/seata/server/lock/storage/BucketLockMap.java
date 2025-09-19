package org.apache.seata.server.lock.storage;

import org.apache.seata.server.lock.holder.ObjectHolder;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Updated BucketLockMap to use ObjectHolder instead of BranchSession
 */
public class BucketLockMap {

    private final ConcurrentHashMap<String /* pk */, ObjectHolder /* holder */> bucketLockMap =
            new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, ObjectHolder> get() {
        return bucketLockMap;
    }
}
