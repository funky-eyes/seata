package org.apache.seata.server.lock;

import org.apache.seata.server.lock.dto.CGLockRequest;
import org.apache.seata.server.lock.dto.LockResult;

import java.util.concurrent.CompletableFuture;

/**
 * Cluster Group Lock Manager Interface for multi-raft distributed lock management.
 * Provides distributed lock operations across multiple raft groups.
 * LOCK_MAP: resourceId -> instance -> bucketId -> BucketLockMap
 */
public interface CGLockManager {

    /**
     * Acquire locks for the specified rows
     *
     * @param request the lock acquisition request
     * @return CompletableFuture containing lock result
     */
    CompletableFuture<LockResult> acquireLock(CGLockRequest request);

    /**
     * Release locks for the specified rows
     *
     * @param request the lock release request
     * @return CompletableFuture containing release result
     */
    CompletableFuture<LockResult> releaseLock(CGLockRequest request);

    /**
     * Check if the specified locks are acquirable without actually acquiring them
     *
     * @param request the lock query request
     * @return CompletableFuture containing lockable status
     */
    CompletableFuture<LockResult> isLockable(CGLockRequest request);

    /**
     * Clean all locks - used when follower falls behind and needs to apply leader's snapshot
     *
     * @return CompletableFuture containing clean result
     */
    CompletableFuture<LockResult> cleanAllLocks();
}
