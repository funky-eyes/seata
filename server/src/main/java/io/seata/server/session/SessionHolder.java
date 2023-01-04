/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.session;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.seata.common.ConfigurationKeys;
import io.seata.common.exception.StoreException;
import io.seata.core.model.LockStatus;
import io.seata.common.XID;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLocker;
import io.seata.server.lock.LockManager;
import io.seata.server.lock.distributed.DistributedLockerFactory;
import io.seata.server.cluster.raft.RaftServer;
import io.seata.server.cluster.raft.RaftServerFactory;
import io.seata.server.storage.file.lock.FileLockManager;
import io.seata.server.store.StoreConfig;
import io.seata.server.store.StoreConfig.SessionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static java.io.File.separator;
import static io.seata.common.DefaultValues.DEFAULT_DISTRIBUTED_LOCK_EXPIRE_TIME;
import static io.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;
import static io.seata.core.constants.ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL;

/**
 * The type Session holder.
 *
 * @author sharajava
 */
public class SessionHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionHolder.class);

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The constant ROOT_SESSION_MANAGER_NAME.
     */
    public static final String ROOT_SESSION_MANAGER_NAME = "root.data";

    /**
     * The constant ASYNC_COMMITTING_SESSION_MANAGER_NAME.
     */
    public static final String ASYNC_COMMITTING_SESSION_MANAGER_NAME = "async.commit.data";

    /**
     * The constant RETRY_COMMITTING_SESSION_MANAGER_NAME.
     */
    public static final String RETRY_COMMITTING_SESSION_MANAGER_NAME = "retry.commit.data";

    /**
     * The constant RETRY_ROLLBACKING_SESSION_MANAGER_NAME.
     */
    public static final String RETRY_ROLLBACKING_SESSION_MANAGER_NAME = "retry.rollback.data";

    /**
     * The redis distributed lock expire time
     */
    private static long DISTRIBUTED_LOCK_EXPIRE_TIME = CONFIG.getLong(ConfigurationKeys.DISTRIBUTED_LOCK_EXPIRE_TIME, DEFAULT_DISTRIBUTED_LOCK_EXPIRE_TIME);

    private static SessionManager ROOT_SESSION_MANAGER;
    private static SessionManager ASYNC_COMMITTING_SESSION_MANAGER;
    private static SessionManager RETRY_COMMITTING_SESSION_MANAGER;
    private static SessionManager RETRY_ROLLBACKING_SESSION_MANAGER;
    
    private static final Map<String, SessionManager> SESSION_MANAGER_MAP = new HashMap<>();

    private static DistributedLocker DISTRIBUTED_LOCKER;

    public static void init() {
        init(null);
    }
    /**
     * Init.
     *
     * @param sessionMode the store mode: file, db, redis
     * @throws IOException the io exception
     */
    public static void init(SessionMode sessionMode) {
        if (null == sessionMode) {
            sessionMode = StoreConfig.getSessionMode();
        }
        if (SessionMode.DB.equals(sessionMode)) {
            ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, SessionMode.DB.getName());
            ASYNC_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, SessionMode.DB.getName(),
                new Object[]{ASYNC_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, SessionMode.DB.getName(),
                new Object[]{RETRY_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_ROLLBACKING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, SessionMode.DB.getName(),
                new Object[]{RETRY_ROLLBACKING_SESSION_MANAGER_NAME});
        } else if (SessionMode.RAFT.equals(sessionMode) || SessionMode.FILE.equals(sessionMode)) {
            String sessionStorePath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR)
                + separator + System.getProperty(SERVER_SERVICE_PORT_CAMEL);
            if (StringUtils.isBlank(sessionStorePath)) {
                throw new StoreException("the {store.file.dir} is empty.");
            }
            if (SessionMode.RAFT.equals(sessionMode)) {
                ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, SessionMode.RAFT.getName(),
                        new Object[] {ROOT_SESSION_MANAGER_NAME});
            } else {
                ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, SessionMode.FILE.getName(),
                        new Object[] {ROOT_SESSION_MANAGER_NAME, sessionStorePath});
            }
            ASYNC_COMMITTING_SESSION_MANAGER = ROOT_SESSION_MANAGER;
            RETRY_COMMITTING_SESSION_MANAGER = ROOT_SESSION_MANAGER;
            RETRY_ROLLBACKING_SESSION_MANAGER = ROOT_SESSION_MANAGER;
            SESSION_MANAGER_MAP.put(DEFAULT_SEATA_GROUP, ROOT_SESSION_MANAGER);
        } else if (SessionMode.REDIS.equals(sessionMode)) {
            ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, SessionMode.REDIS.getName());
            ASYNC_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class,
                SessionMode.REDIS.getName(), new Object[]{ASYNC_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class,
                SessionMode.REDIS.getName(), new Object[]{RETRY_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_ROLLBACKING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class,
                SessionMode.REDIS.getName(), new Object[] {RETRY_ROLLBACKING_SESSION_MANAGER_NAME});

            DISTRIBUTED_LOCKER = DistributedLockerFactory.getDistributedLocker(SessionMode.REDIS.getName());
        } else {
            // unknown store
            throw new IllegalArgumentException("unknown store mode:" + sessionMode.getName());
        }
        RaftServerFactory.getInstance().init();
        if (RaftServerFactory.getInstance().getRaftServer() != null) {
            DISTRIBUTED_LOCKER = DistributedLockerFactory.getDistributedLocker(SessionMode.RAFT.getName());
        } else {
            DISTRIBUTED_LOCKER = DistributedLockerFactory.getDistributedLocker(sessionMode.getName());
        }
        if (RaftServerFactory.getInstance().isRaftMode()) {
            return;
        }
        reload(sessionMode);
    }

    /**
     * Reload.
     *
     * @param sessionMode the mode of store
     */
    protected static void reload(SessionMode sessionMode) {
        if (sessionMode == SessionMode.FILE) {
            ((Reloadable)ROOT_SESSION_MANAGER).reload();
            reload(ROOT_SESSION_MANAGER.allSessions(), sessionMode);
        } else {
            reload(null, sessionMode);
        }
    }

    public static void reload(Collection<GlobalSession> allSessions, SessionMode storeMode) {
        reload(allSessions, storeMode, true);
    }

    public static void reload(Collection<GlobalSession> allSessions, SessionMode storeMode, boolean acquireLock) {
        if ((SessionMode.FILE == storeMode || SessionMode.RAFT == storeMode)
            && CollectionUtils.isNotEmpty(allSessions)) {
            for (GlobalSession globalSession : allSessions) {
                GlobalStatus globalStatus = globalSession.getStatus();
                switch (globalStatus) {
                    case UnKnown:
                    case Committed:
                    case CommitFailed:
                    case Rollbacked:
                    case RollbackFailed:
                    case TimeoutRollbacked:
                    case TimeoutRollbackFailed:
                    case Finished:
                        removeInErrorState(globalSession);
                        break;
                    case AsyncCommitting:
                        queueToAsyncCommitting(globalSession);
                        break;
                    case Committing:
                    case CommitRetrying:
                        queueToRetryCommit(globalSession);
                        break;
                    default: {
                        if (acquireLock) {
                            lockBranchSessions(globalSession.getSortedBranches());
                            if (GlobalStatus.Rollbacking.equals(globalSession.getStatus())
                                || GlobalStatus.TimeoutRollbacking.equals(globalSession.getStatus())) {
                                globalSession.getBranchSessions().parallelStream()
                                    .forEach(branchSession -> branchSession.setLockStatus(LockStatus.Rollbacking));
                            }
                        }
                        switch (globalStatus) {
                            case Rollbacking:
                            case RollbackRetrying:
                            case TimeoutRollbacking:
                            case TimeoutRollbackRetrying:
                                globalSession.getBranchSessions().parallelStream().forEach(branchSession -> {
                                    if (storeMode == SessionMode.RAFT) {
                                        if (globalSession.getStatus() == GlobalStatus.Rollbacking) {
                                            globalSession.setStatus(GlobalStatus.RollbackRetrying);
                                            LOGGER.info("xid :{} change status RollbackRetrying",
                                                globalSession.getXid());
                                        }
                                    }
                                    branchSession.setLockStatus(LockStatus.Rollbacking);
                                });
                                queueToRetryRollback(globalSession);
                                break;
                            case Begin:
                                if (storeMode == SessionMode.RAFT) {
                                    globalSession.setStatus(GlobalStatus.RollbackRetrying);
                                    LOGGER.info("xid :{} change status RollbackRetrying", globalSession.getXid());
                                    queueToRetryRollback(globalSession);
                                } else {
                                    globalSession.setActive(true);
                                }
                                break;
                            default:
                                LOGGER.error("Could not handle the global session, xid: {}", globalSession.getXid());
                                throw new ShouldNeverHappenException("NOT properly handled " + globalStatus);
                        }
                        break;
                    }
                }
            }
        } else {
            // Redis, db and so on
            CompletableFuture.runAsync(() -> {
                SessionCondition searchCondition = new SessionCondition(GlobalStatus.UnKnown, GlobalStatus.Committed,
                        GlobalStatus.Rollbacked, GlobalStatus.TimeoutRollbacked, GlobalStatus.Finished);
                searchCondition.setLazyLoadBranch(true);

                long now = System.currentTimeMillis();
                List<GlobalSession> errorStatusGlobalSessions = ROOT_SESSION_MANAGER.findGlobalSessions(searchCondition);
                while (!CollectionUtils.isEmpty(errorStatusGlobalSessions)) {
                    for (GlobalSession errorStatusGlobalSession : errorStatusGlobalSessions) {
                        if (errorStatusGlobalSession.getBeginTime() >= now) {
                            // Exit when the global transaction begin after the instance started
                            return;
                        }

                        removeInErrorState(errorStatusGlobalSession);
                    }

                    // Load the next part
                    errorStatusGlobalSessions = ROOT_SESSION_MANAGER.findGlobalSessions(searchCondition);
                }
            });
        }
    }

    private static void removeInErrorState(GlobalSession globalSession) {
        try {
            LOGGER.warn("The global session should NOT be {}, remove it. xid = {}", globalSession.getStatus(), globalSession.getXid());
            getRootSessionManager().onSuccessEnd(globalSession);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Remove global session succeed, xid = {}, status = {}", globalSession.getXid(), globalSession.getStatus());
            }
        } catch (Exception e) {
            LOGGER.error("Remove global session failed, xid = {}, status = {}", globalSession.getXid(), globalSession.getStatus(), e);
        }
    }

    private static void queueToAsyncCommitting(GlobalSession globalSession) {
        try {
            globalSession.addSessionLifecycleListener(getAsyncCommittingSessionManager());
            getAsyncCommittingSessionManager().addGlobalSession(globalSession);
        } catch (TransactionException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    private static void lockBranchSessions(List<BranchSession> branchSessions) {
        FileLockManager fileLockManager =
                (FileLockManager)EnhancedServiceLoader.load(LockManager.class, SessionMode.FILE.getName());
        branchSessions.forEach(branchSession -> {
            try {
                if (StringUtils.isNotBlank(branchSession.getLockKey())) {
                    fileLockManager.acquireLock(branchSession);
                }
            } catch (TransactionException e) {
                throw new ShouldNeverHappenException(e);
            }
        });
    }

    private static void queueToRetryCommit(GlobalSession globalSession) {
        try {
            globalSession.addSessionLifecycleListener(getRetryCommittingSessionManager());
            getRetryCommittingSessionManager().addGlobalSession(globalSession);
        } catch (TransactionException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    private static void queueToRetryRollback(GlobalSession globalSession) {
        try {
            globalSession.addSessionLifecycleListener(getRetryRollbackingSessionManager());
            getRetryRollbackingSessionManager().addGlobalSession(globalSession);
        } catch (TransactionException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    //endregion

    //region get session manager

    /**
     * Gets root session manager.
     *
     * @return the root session manager
     */
    public static SessionManager getRootSessionManager() {
        if (ROOT_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return ROOT_SESSION_MANAGER;
    }

    public static SessionManager getRootSessionManager(String group) {
        return SESSION_MANAGER_MAP.get(group);
    }

    /**
     * Gets async committing session manager.
     *
     * @return the async committing session manager
     */
    @Deprecated
    public static SessionManager getAsyncCommittingSessionManager() {
        if (ASYNC_COMMITTING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return ASYNC_COMMITTING_SESSION_MANAGER;
    }

    /**
     * Gets retry committing session manager.
     *
     * @return the retry committing session manager
     */
    @Deprecated
    public static SessionManager getRetryCommittingSessionManager() {
        if (RETRY_COMMITTING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return RETRY_COMMITTING_SESSION_MANAGER;
    }

    /**
     * Gets retry rollbacking session manager.
     *
     * @return the retry rollbacking session manager
     */
    @Deprecated
    public static SessionManager getRetryRollbackingSessionManager() {
        if (RETRY_ROLLBACKING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return RETRY_ROLLBACKING_SESSION_MANAGER;
    }

    //endregion

    /**
     * Find global session.
     *
     * @param xid the xid
     * @return the global session
     */
    public static GlobalSession findGlobalSession(String xid) {
        return findGlobalSession(xid, true);
    }

    /**
     * Find global session.
     *
     * @param xid                the xid
     * @param withBranchSessions the withBranchSessions
     * @return the global session
     */
    public static GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
        return getRootSessionManager().findGlobalSession(xid, withBranchSessions);
    }

    /**
     * lock and execute
     *
     * @param globalSession the global session
     * @param lockCallable  the lock Callable
     * @return the value
     */
    public static <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException {
        return getRootSessionManager().lockAndExecute(globalSession, lockCallable);
    }

    /**
     * acquire lock
     *
     * @param lockKey the lock key, should be distinct for each lock
     * @return the boolean
     */
    public static boolean acquireDistributedLock(String lockKey) {
        return DISTRIBUTED_LOCKER.acquireLock(new DistributedLockDO(lockKey, XID.getIpAddressAndPort(), DISTRIBUTED_LOCK_EXPIRE_TIME));
    }

    /**
     * release lock
     *
     * @return the boolean
     */
    public static boolean releaseDistributedLock(String lockKey) {
        return DISTRIBUTED_LOCKER.releaseLock(new DistributedLockDO(lockKey, XID.getIpAddressAndPort(), DISTRIBUTED_LOCK_EXPIRE_TIME));
    }

    /**
     * Execute the function after get the distribute lock
     *
     * @param key  the distribute lock key
     * @param func the function to be call
     * @return whether the func be call
     */
    public static boolean distributedLockAndExecute(String key, NoArgsFunc func) {
        boolean lock = false;
        try {
            if (lock = acquireDistributedLock(key)) {
                func.call();
            }
        } catch (Exception e) {
            LOGGER.error("Exception running function with key = {}", key, e);
        } finally {
            if (lock) {
                try {
                    SessionHolder.releaseDistributedLock(key);
                } catch (Exception ex) {
                    LOGGER.warn("release distribute lock failure, message = {}", ex.getMessage(), ex);
                }
            }
        }
        return lock;
    }

    public static void destroy() {
        RaftServer raftServer = RaftServerFactory.getInstance().getRaftServer();
        if (raftServer != null) {
            try {
                raftServer.close();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
        if (ROOT_SESSION_MANAGER != null) {
            ROOT_SESSION_MANAGER.destroy();
        }
    }

    @FunctionalInterface
    public interface NoArgsFunc {
        void call();
    }
}
