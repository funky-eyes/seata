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

import org.apache.seata.common.loader.LoadLevel;

import java.util.concurrent.ThreadFactory;

/**
 * JDK 21+ SPI implementation that creates virtual-thread-backed factories.
 * <p>
 * The daemon flag is intentionally ignored because the virtual thread builder
 * does not expose daemon configuration. The total size is also only relevant
 * for the default named implementation, so virtual threads use a monotonic
 * prefix-based naming strategy instead.
 */
@LoadLevel(name = "virtual", order = Integer.MIN_VALUE)
public class VirtualThreadFactoryProvider implements ThreadFactoryProvider {

    @Override
    public ThreadFactory newThreadFactory(String threadPrefix, int totalSize, boolean daemon) {
        return Thread.ofVirtual().name(normalizePrefix(threadPrefix), 0).factory();
    }

    private String normalizePrefix(String threadPrefix) {
        return threadPrefix.endsWith("-") ? threadPrefix : threadPrefix + "-";
    }
}
