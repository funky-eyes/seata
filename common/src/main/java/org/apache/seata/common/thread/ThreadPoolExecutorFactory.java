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
package org.apache.seata.common.thread;

import org.apache.seata.common.loader.EnhancedServiceLoader;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Central factory used by Seata managed thread pools.
 * <p>
 * All business modules create thread pools through this entry so that the
 * underlying {@link ThreadFactory} can be replaced by SPI. The default provider
 * keeps the historical {@link NamedThreadFactory} behavior for JDK 8 compatibility,
 * while higher-priority providers can transparently switch worker threads to
 * another implementation, such as JDK 21 virtual threads.
 */
public final class ThreadPoolExecutorFactory {

    private static final ThreadFactoryProvider THREAD_FACTORY_PROVIDER =
            EnhancedServiceLoader.load(ThreadFactoryProvider.class);

    private ThreadPoolExecutorFactory() {}

    /**
     * Create a managed thread factory with daemon threads enabled.
     *
     * @param threadPrefix the logical thread name prefix
     * @param totalSize the expected thread count for naming purposes
     * @return the managed thread factory
     */
    public static ThreadFactory newThreadFactory(String threadPrefix, int totalSize) {
        return newThreadFactory(threadPrefix, totalSize, true);
    }

    /**
     * Create a managed thread factory.
     *
     * @param threadPrefix the logical thread name prefix
     * @param totalSize the expected thread count for naming purposes
     * @param daemon whether daemon threads should be requested
     * @return the managed thread factory
     */
    public static ThreadFactory newThreadFactory(String threadPrefix, int totalSize, boolean daemon) {
        return THREAD_FACTORY_PROVIDER.newThreadFactory(threadPrefix, totalSize, daemon);
    }

    /**
     * Create a managed {@link ThreadPoolExecutor} that keeps the default abort policy.
     *
     * @param threadPrefix the logical thread name prefix
     * @param corePoolSize the core pool size
     * @param maximumPoolSize the maximum pool size
     * @param keepAliveTime the thread keep alive time
     * @param unit the keep alive time unit
     * @param workQueue the task queue
     * @return the managed thread pool executor
     */
    public static ThreadPoolExecutor newThreadPoolExecutor(
            String threadPrefix,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        return newThreadPoolExecutor(threadPrefix, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, true);
    }

    /**
     * Create a managed {@link ThreadPoolExecutor} that keeps the default abort policy.
     *
     * @param threadPrefix the logical thread name prefix
     * @param corePoolSize the core pool size
     * @param maximumPoolSize the maximum pool size
     * @param keepAliveTime the thread keep alive time
     * @param unit the keep alive time unit
     * @param workQueue the task queue
     * @param daemon whether daemon threads should be requested
     * @return the managed thread pool executor
     */
    public static ThreadPoolExecutor newThreadPoolExecutor(
            String threadPrefix,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            boolean daemon) {
        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                Objects.requireNonNull(workQueue, "workQueue must not be null"),
                newThreadFactory(threadPrefix, maximumPoolSize, daemon));
    }

    /**
     * Create a managed {@link ThreadPoolExecutor} with a custom rejection handler.
     *
     * @param threadPrefix the logical thread name prefix
     * @param corePoolSize the core pool size
     * @param maximumPoolSize the maximum pool size
     * @param keepAliveTime the thread keep alive time
     * @param unit the keep alive time unit
     * @param workQueue the task queue
     * @param rejectedHandler the rejection handler
     * @return the managed thread pool executor
     */
    public static ThreadPoolExecutor newThreadPoolExecutor(
            String threadPrefix,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            RejectedExecutionHandler rejectedHandler) {
        return newThreadPoolExecutor(
                threadPrefix, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, true, rejectedHandler);
    }

    /**
     * Create a managed {@link ThreadPoolExecutor} with a custom rejection handler.
     *
     * @param threadPrefix the logical thread name prefix
     * @param corePoolSize the core pool size
     * @param maximumPoolSize the maximum pool size
     * @param keepAliveTime the thread keep alive time
     * @param unit the keep alive time unit
     * @param workQueue the task queue
     * @param daemon whether daemon threads should be requested
     * @param rejectedHandler the rejection handler
     * @return the managed thread pool executor
     */
    public static ThreadPoolExecutor newThreadPoolExecutor(
            String threadPrefix,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            boolean daemon,
            RejectedExecutionHandler rejectedHandler) {
        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                Objects.requireNonNull(workQueue, "workQueue must not be null"),
                newThreadFactory(threadPrefix, maximumPoolSize, daemon),
                Objects.requireNonNull(rejectedHandler, "rejectedHandler must not be null"));
    }

    /**
     * Create a managed {@link ScheduledThreadPoolExecutor} with daemon threads enabled.
     *
     * @param threadPrefix the logical thread name prefix
     * @param corePoolSize the core pool size
     * @return the managed scheduled executor
     */
    public static ScheduledThreadPoolExecutor newScheduledThreadPoolExecutor(String threadPrefix, int corePoolSize) {
        return newScheduledThreadPoolExecutor(threadPrefix, corePoolSize, true);
    }

    /**
     * Create a managed {@link ScheduledThreadPoolExecutor}.
     *
     * @param threadPrefix the logical thread name prefix
     * @param corePoolSize the core pool size
     * @param daemon whether daemon threads should be requested
     * @return the managed scheduled executor
     */
    public static ScheduledThreadPoolExecutor newScheduledThreadPoolExecutor(
            String threadPrefix, int corePoolSize, boolean daemon) {
        return new ScheduledThreadPoolExecutor(corePoolSize, newThreadFactory(threadPrefix, corePoolSize, daemon));
    }

    /**
     * Create a managed {@link ScheduledThreadPoolExecutor} with a custom rejection handler.
     *
     * @param threadPrefix the logical thread name prefix
     * @param corePoolSize the core pool size
     * @param daemon whether daemon threads should be requested
     * @param rejectedHandler the rejection handler
     * @return the managed scheduled executor
     */
    public static ScheduledThreadPoolExecutor newScheduledThreadPoolExecutor(
            String threadPrefix, int corePoolSize, boolean daemon, RejectedExecutionHandler rejectedHandler) {
        return new ScheduledThreadPoolExecutor(
                corePoolSize,
                newThreadFactory(threadPrefix, corePoolSize, daemon),
                Objects.requireNonNull(rejectedHandler, "rejectedHandler must not be null"));
    }
}
