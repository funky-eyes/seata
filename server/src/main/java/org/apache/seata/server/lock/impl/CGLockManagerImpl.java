package org.apache.seata.server.lock.impl;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.core.exception.BranchTransactionException;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.server.lock.CGLockManager;
import org.apache.seata.server.lock.dto.CGLockRequest;
import org.apache.seata.server.lock.dto.LockResult;
import org.apache.seata.server.lock.holder.ObjectHolder;
import org.apache.seata.server.lock.storage.BucketLockMap;
import org.apache.seata.server.lock.storage.CGLockStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

import static org.apache.seata.core.exception.TransactionExceptionCode.LockKeyConflictFailFast;

/**
 * Default implementation of CGLockManager for distributed lock management
 */
@Service
public class CGLockManagerImpl implements CGLockManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CGLockManagerImpl.class);

    @Override
    public CompletableFuture<LockResult> acquireLock(CGLockRequest request) {
        // Return completed future to maintain interface compatibility while avoiding async overhead
        return CompletableFuture.completedFuture(acquireLockSync(request));
    }

    @Override
    public CompletableFuture<LockResult> releaseLock(CGLockRequest request) {
        return CompletableFuture.completedFuture(releaseLockSync(request));
    }

    @Override
    public CompletableFuture<LockResult> isLockable(CGLockRequest request) {
        return CompletableFuture.completedFuture(isLockableSync(request));
    }

    @Override
    public CompletableFuture<LockResult> cleanAllLocks() {
        return CompletableFuture.completedFuture(cleanAllLocksSync());
    }

    /**
     * Synchronous lock acquisition logic
     */
    private LockResult acquireLockSync(CGLockRequest request) {
        try {
            if (CollectionUtils.isEmpty(request.getRowLocks())) {
                return LockResult.SUCCESS;
            }

            ObjectHolder holder = new ObjectHolder(request.getParent(), request.getOwner(), request.getInstance());
            boolean canLock = true;
            boolean failFast = false;

            // Track successfully acquired locks for potential rollback
            java.util.List<LockKey> acquiredLocks = new java.util.ArrayList<>();

            for (RowLock lock : request.getRowLocks()) {
                String resourceId = lock.getResourceId();
                String tableName = lock.getTableName();
                String pk = lock.getPk();

                ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>> resourceLockMap =
                        CollectionUtils.computeIfAbsent(CGLockStorage.getLockMap(), resourceId,
                                key -> new java.util.concurrent.ConcurrentHashMap<>());

                String instance = request.getInstance() != null ? request.getInstance() : "default";
                ConcurrentMap<Integer, BucketLockMap> instanceLockMap =
                        CollectionUtils.computeIfAbsent(resourceLockMap, instance,
                                key -> new java.util.concurrent.ConcurrentHashMap<>());

                int bucketId = pk.hashCode() % CGLockStorage.getBucketPerTable();
                BucketLockMap bucketLockMap =
                        CollectionUtils.computeIfAbsent(instanceLockMap, bucketId,
                                key -> new BucketLockMap());

                ObjectHolder previousLockHolder = bucketLockMap.get().putIfAbsent(pk, holder);

                if (previousLockHolder == null) {
                    // Successfully acquired lock
                    acquiredLocks.add(new LockKey(resourceId, instance, pk));
                    LOGGER.debug("Lock acquired for key: {}", pk);
                } else if (previousLockHolder.equals(holder)) {
                    // Already locked by same holder
                    LOGGER.debug("Lock already held by same holder for key: {}", pk);
                } else {
                    // Lock conflict
                    LOGGER.info("Global lock on [{}:{}] is holding by parent {} owner {}",
                            tableName, pk, previousLockHolder.getParent(), previousLockHolder.getOwner());

                    // Release partially acquired locks
                    releaseSpecificLocks(holder, acquiredLocks);

                    if (!request.isAutoCommit()) {
                        failFast = true;
                        break;
                    }
                    canLock = false;
                    break;
                }
            }

            if (failFast) {
                throw new StoreException(new BranchTransactionException(LockKeyConflictFailFast));
            }

            return canLock ? LockResult.SUCCESS : LockResult.CONFLICT;

        } catch (Exception e) {
            LOGGER.error("Failed to acquire lock", e);
            return LockResult.FAILED;
        }
    }

    /**
     * Synchronous lock release logic
     */
    private LockResult releaseLockSync(CGLockRequest request) {
        try {
            ObjectHolder holder = new ObjectHolder(request.getParent(), request.getOwner(), request.getInstance());
            releaseLockInternal(holder);
            return LockResult.SUCCESS;
        } catch (Exception e) {
            LOGGER.error("Failed to release lock", e);
            return LockResult.FAILED;
        }
    }

    /**
     * Synchronous lockability check logic
     */
    private LockResult isLockableSync(CGLockRequest request) {
        try {
            if (CollectionUtils.isEmpty(request.getRowLocks())) {
                return LockResult.SUCCESS;
            }

            ObjectHolder holder = new ObjectHolder(request.getParent(), request.getOwner(), request.getInstance());

            for (RowLock rowLock : request.getRowLocks()) {
                String resourceId = rowLock.getResourceId();
                String tableName = rowLock.getTableName();
                String pk = rowLock.getPk();

                ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>> resourceLockMap =
                        CGLockStorage.getLockMap().get(resourceId);
                if (resourceLockMap == null) {
                    continue;
                }

                String instance = request.getInstance() != null ? request.getInstance() : "default";
                ConcurrentMap<Integer, BucketLockMap> instanceLockMap = resourceLockMap.get(instance);
                if (instanceLockMap == null) {
                    continue;
                }

                int bucketId = pk.hashCode() % CGLockStorage.getBucketPerTable();
                BucketLockMap bucketLockMap = instanceLockMap.get(bucketId);
                if (bucketLockMap == null) {
                    continue;
                }

                ObjectHolder lockingHolder = bucketLockMap.get().get(pk);
                if (lockingHolder != null && !lockingHolder.equals(holder)) {
                    LOGGER.info("Global lock on [{}:{}] is holding by parent {} owner {}",
                            tableName, pk, lockingHolder.getParent(), lockingHolder.getOwner());
                    return LockResult.NOT_LOCKABLE;
                }
            }

            return LockResult.SUCCESS;
        } catch (Exception e) {
            LOGGER.error("Failed to check lock status", e);
            return LockResult.FAILED;
        }
    }

    /**
     * Synchronous clean all locks logic
     */
    private LockResult cleanAllLocksSync() {
        try {
            CGLockStorage.clearAllLocks();
            LOGGER.info("All locks cleaned successfully");
            return LockResult.SUCCESS;
        } catch (Exception e) {
            LOGGER.error("Failed to clean all locks", e);
            return LockResult.FAILED;
        }
    }

    private void releaseLockInternal(ObjectHolder holder) {
        CGLockStorage.getLockMap().forEach((resourceId, resourceMap) -> {
            resourceMap.forEach((instance, instanceMap) -> {
                instanceMap.forEach((bucketId, bucketLockMap) -> {
                    // Remove all locks held by this holder
                    bucketLockMap.get().entrySet().removeIf(entry ->
                            entry.getValue().equals(holder));
                });
            });
        });
    }

    /**
     * Release specific locks (used during partial lock acquisition failure)
     */
    private void releaseSpecificLocks(ObjectHolder holder, java.util.List<LockKey> locksToRelease) {
        for (LockKey lockKey : locksToRelease) {
            ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>> resourceLockMap =
                    CGLockStorage.getLockMap().get(lockKey.resourceId);
            if (resourceLockMap != null) {
                ConcurrentMap<Integer, BucketLockMap> instanceLockMap = resourceLockMap.get(lockKey.instance);
                if (instanceLockMap != null) {
                    int bucketId = lockKey.pk.hashCode() % CGLockStorage.getBucketPerTable();
                    BucketLockMap bucketLockMap = instanceLockMap.get(bucketId);
                    if (bucketLockMap != null) {
                        // Remove lock only if it's held by this holder
                        bucketLockMap.get().remove(lockKey.pk, holder);
                    }
                }
            }
        }
    }

    /**
     * Helper class to track lock keys for cleanup
     */
    private static class LockKey {
        final String resourceId;
        final String instance;
        final String pk;

        LockKey(String resourceId, String instance, String pk) {
            this.resourceId = resourceId;
            this.instance = instance;
            this.pk = pk;
        }
    }
}