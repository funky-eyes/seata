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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.DefaultValues;

import java.lang.reflect.Method;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Runtime helper used to resolve the thread pool mode without adding a hard dependency
 * from the common module to the config module.
 */
final class ThreadPoolRuntimeEnvironment {

    private static final Supplier<String> DEFAULT_THREAD_POOL_TYPE_SUPPLIER =
            ThreadPoolRuntimeEnvironment::loadConfiguredThreadPoolType;
    private static final IntSupplier DEFAULT_JDK_FEATURE_SUPPLIER =
            () -> Runtime.version().feature();

    private static volatile Supplier<String> threadPoolTypeSupplier = DEFAULT_THREAD_POOL_TYPE_SUPPLIER;
    private static volatile IntSupplier jdkFeatureSupplier = DEFAULT_JDK_FEATURE_SUPPLIER;

    private ThreadPoolRuntimeEnvironment() {}

    static ThreadPoolType resolveThreadPoolType() {
        ThreadPoolType configuredType = ThreadPoolType.from(threadPoolTypeSupplier.get());
        if (configuredType == ThreadPoolType.PLATFORM) {
            return ThreadPoolType.PLATFORM;
        }
        int jdkFeature = jdkFeatureSupplier.getAsInt();
        if (configuredType == ThreadPoolType.VIRTUAL) {
            return jdkFeature >= 21 ? ThreadPoolType.VIRTUAL : ThreadPoolType.PLATFORM;
        }
        return jdkFeature >= 25 ? ThreadPoolType.VIRTUAL : ThreadPoolType.PLATFORM;
    }

    static void setThreadPoolTypeSupplier(Supplier<String> supplier) {
        threadPoolTypeSupplier = supplier == null ? DEFAULT_THREAD_POOL_TYPE_SUPPLIER : supplier;
    }

    static void setJdkFeatureSupplier(IntSupplier supplier) {
        jdkFeatureSupplier = supplier == null ? DEFAULT_JDK_FEATURE_SUPPLIER : supplier;
    }

    static void reset() {
        threadPoolTypeSupplier = DEFAULT_THREAD_POOL_TYPE_SUPPLIER;
        jdkFeatureSupplier = DEFAULT_JDK_FEATURE_SUPPLIER;
    }

    private static String loadConfiguredThreadPoolType() {
        try {
            Class<?> configurationFactoryClass = Class.forName("org.apache.seata.config.ConfigurationFactory");
            Object configuration =
                    configurationFactoryClass.getMethod("getInstance").invoke(null);
            Method getConfigMethod = configuration.getClass().getMethod("getConfig", String.class, String.class);
            return (String) getConfigMethod.invoke(
                    configuration, ConfigurationKeys.TRANSPORT_THREADPOOL, DefaultValues.DEFAULT_TRANSPORT_THREADPOOL);
        } catch (Exception ignored) {
            return System.getProperty(
                    ConfigurationKeys.TRANSPORT_THREADPOOL, DefaultValues.DEFAULT_TRANSPORT_THREADPOOL);
        }
    }
}
