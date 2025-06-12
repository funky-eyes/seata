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
package org.apache.seata.server.controller;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;

import static org.apache.seata.common.ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL;

public class ClearServerServicePortInitializer implements GenericApplicationListener, Ordered {

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        Class<?> type = eventType.getRawClass();
        if (type == null) {
            return false;
        }
        return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(type)
                || ContextRefreshedEvent.class.isAssignableFrom(type)
                || ContextClosedEvent.class.isAssignableFrom(type);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent ||
            event instanceof ContextRefreshedEvent ||
            event instanceof ContextClosedEvent) {
            System.clearProperty(SERVER_SERVICE_PORT_CAMEL);
        }
    }

    @Override
    public int getOrder() {
        // Ensure this listener runs before ServerApplicationListener for ApplicationEnvironmentPreparedEvent
        // ServerApplicationListener order is LoggingApplicationListener.DEFAULT_ORDER - 1
        // LoggingApplicationListener.DEFAULT_ORDER is Ordered.HIGHEST_PRECEDENCE + 20
        // So, we set our order to LoggingApplicationListener.DEFAULT_ORDER - 2, which is Ordered.HIGHEST_PRECEDENCE + 18
        return LoggingApplicationListener.DEFAULT_ORDER - 2;
    }
}

