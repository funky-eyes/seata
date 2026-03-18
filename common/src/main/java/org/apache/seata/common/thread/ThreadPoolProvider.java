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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * SPI abstraction used by Seata managed business thread pools.
 */
public interface ThreadPoolProvider {

    /**
     * Create a managed thread pool executor.
     *
     * @param threadPrefix the logical thread name prefix
     * @param corePoolSize the core pool size
     * @param maximumPoolSize the maximum pool size
     * @param keepAliveTime the keep alive time
     * @param unit the keep alive time unit
     * @param workQueue the work queue
     * @param daemon whether daemon threads should be requested when supported
     * @param rejectedHandler the rejection handler
     * @return the managed executor
     */
    ThreadPoolExecutor newThreadPoolExecutor(
            String threadPrefix,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            boolean daemon,
            RejectedExecutionHandler rejectedHandler);
}
