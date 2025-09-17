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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.seata.core.exception.TransactionExceptionCode.LockKeyConflictFailFast;

/**
 * Default implementation of CGLockManager for distributed lock management
 */
@Service
public class CGLockManagerImpl implements CGLockManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CGLockManagerImpl.class);

    // Track lock holders by ObjectHolder for efficient cleanup
    private final Map<ObjectHolder, Set<String>> lockHolderMap = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<LockResult> acquireLock(CGLockRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (CollectionUtils.isEmpty(request.getRowLocks())) {
                    return LockResult.SUCCESS;
                }

                ObjectHolder holder = new ObjectHolder(request.getParent(), request.getOwner(), request.getInstance());
                Set<String> keysInHolder = lockHolderMap.computeIfAbsent(holder, k -> ConcurrentHashMap.newKeySet());

                boolean canLock = true;
                boolean failFast = false;

                for (RowLock lock : request.getRowLocks()) {
                    String resourceId = lock.getResourceId();
                    String tableName = lock.getTableName();
                    String pk = lock.getPk();

                    ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>> resourceLockMap =
                            CollectionUtils.computeIfAbsent(CGLockStorage.getLockMap(), resourceId,
                                    key -> new ConcurrentHashMap<>());

                    String instance = request.getInstance() != null ? request.getInstance() : "default";
                    ConcurrentMap<Integer, BucketLockMap> instanceLockMap =
                            CollectionUtils.computeIfAbsent(resourceLockMap, instance,
                                    key -> new ConcurrentHashMap<>());

                    int bucketId = pk.hashCode() % CGLockStorage.getBucketPerTable();
                    BucketLockMap bucketLockMap =
                            CollectionUtils.computeIfAbsent(instanceLockMap, bucketId,
                                    key -> new BucketLockMap());

                    ObjectHolder previousLockHolder = bucketLockMap.get().putIfAbsent(pk, holder);

                    if (previousLockHolder == null) {
                        // Successfully acquired lock
                        keysInHolder.add(resourceId + ":" + tableName + ":" + pk);
                        LOGGER.debug("Lock acquired for key: {}", pk);
                    } else if (previousLockHolder.equals(holder)) {
                        // Already locked by same holder
                        LOGGER.debug("Lock already held by same holder for key: {}", pk);
                    } else {
                        // Lock conflict
                        LOGGER.info("Global lock on [{}:{}] is holding by parent {} owner {}",
                                tableName, pk, previousLockHolder.getParent(), previousLockHolder.getOwner());

                        // Release partially acquired locks
                        releaseLockInternal(holder, keysInHolder);

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
        });
    }

    @Override
    public CompletableFuture<LockResult> releaseLock(CGLockRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ObjectHolder holder = new ObjectHolder(request.getParent(), request.getOwner(), request.getInstance());
                Set<String> keysInHolder = lockHolderMap.get(holder);

                if (keysInHolder != null) {
                    releaseLockInternal(holder, keysInHolder);
                }

                return LockResult.SUCCESS;
            } catch (Exception e) {
                LOGGER.error("Failed to release lock", e);
                return LockResult.FAILED;
            }
        });
    }

    @Override
    public CompletableFuture<LockResult> isLockable(CGLockRequest request) {
        return CompletableFuture.supplyAsync(() -> {
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
        });
    }

    @Override
    public CompletableFuture<LockResult> cleanAllLocks() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CGLockStorage.clearAllLocks();
                lockHolderMap.clear();
                LOGGER.info("All locks cleaned successfully");
                return LockResult.SUCCESS;
            } catch (Exception e) {
                LOGGER.error("Failed to clean all locks", e);
                return LockResult.FAILED;
            }
        });
    }

    /**
     * Internal method to release locks for a specific holder
     */
    private void releaseLockInternal(ObjectHolder holder, Set<String> keysInHolder) {
        for (String key : keysInHolder) {
            String[] parts = key.split(":");
            if (parts.length >= 3) {
                String resourceId = parts[0];
                String tableName = parts[1];
                String pk = parts[2];

                ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>> resourceLockMap =
                        CGLockStorage.getLockMap().get(resourceId);
                if (resourceLockMap != null) {
                    String instance = holder.getInstance() != null ? holder.getInstance() : "default";
                    ConcurrentMap<Integer, BucketLockMap> instanceLockMap = resourceLockMap.get(instance);
                    if (instanceLockMap != null) {
                        int bucketId = pk.hashCode() % CGLockStorage.getBucketPerTable();
                        BucketLockMap bucketLockMap = instanceLockMap.get(bucketId);
                        if (bucketLockMap != null) {
                            // Remove lock only if it's held by this holder
                            bucketLockMap.get().remove(pk, holder);
                        }
                    }
                }
            }
        }
        keysInHolder.clear();
        lockHolderMap.remove(holder);
    }
}
