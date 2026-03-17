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

import java.util.concurrent.ThreadFactory;

/**
 * SPI abstraction used by Seata thread pools to create worker threads.
 * <p>
 * The common module always compiles on JDK 8, so the default implementation
 * keeps using {@link NamedThreadFactory}. A dedicated JDK 21+ module can provide
 * another implementation that returns a virtual-thread-backed {@link ThreadFactory}
 * without requiring any source-level change in business modules.
 * <p>
 * Implementations are allowed to interpret the daemon flag or naming metadata
 * differently when the underlying JDK thread model has different semantics.
 * For example, a virtual-thread provider may not be able to preserve the exact
 * daemon behavior exposed by the default platform-thread implementation.
 */
public interface ThreadFactoryProvider {

    /**
     * Create a thread factory for a managed Seata thread pool.
     *
     * @param threadPrefix the logical thread name prefix
     * @param totalSize the expected thread count for naming purposes
     * @param daemon whether created threads should be daemon threads when the provider supports it
     * @return the thread factory used by the managed thread pool
     */
    ThreadFactory newThreadFactory(String threadPrefix, int totalSize, boolean daemon);
}
