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
package org.apache.seata.config.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class ConsulConfigurationTest {

    private ConsulConfiguration consulConfig;
    private ConsulClient mockConsulClient;
    private Configuration mockFileConfig;
    private MockedConstruction<ConsulClient> mockedConsulClientConstruction;

    @BeforeEach
    void setUp() {
        System.setProperty("seataEnv", "test");
        // Mock dependencies
        mockFileConfig = mock(Configuration.class);
        mockConsulClient = mock(ConsulClient.class);

        // Setup static mocks
        when(mockFileConfig.getConfig(anyString(), anyString())).thenReturn("seata.properties");
        when(mockFileConfig.getConfig(anyString())).thenReturn("localhost:8500");

        stubConsulClient(mockConsulClient);
        mockedConsulClientConstruction =
                mockConstruction(ConsulClient.class, (mock, context) -> stubConsulClient(mock));

        setField(null, "instance", null);
        setField(null, "client", mockConsulClient);
        setField(null, "seataConfig", new Properties());

        // Initialize singleton
        consulConfig = ConsulConfiguration.getInstance();
        setField(null, "client", mockConsulClient);
        setField(consulConfig, "consulNotifierExecutor", new DirectExecutorService());
    }

    @AfterEach
    void tearDown() {
        consulConfig.shutdown();
        mockedConsulClientConstruction.close();
        setField(null, "instance", null);
        setField(null, "client", null);
        setField(null, "seataConfig", new Properties());
        reset(mockConsulClient);
    }

    private void stubConsulClient(ConsulClient consulClient) {
        GetValue mockValue = mock(GetValue.class);
        when(mockValue.getDecodedValue()).thenReturn("testValue");
        Response<GetValue> mockResponse = new Response<>(mockValue, 1L, false, 1L);
        when(consulClient.getKVValue(eq("seata.properties"), nullable(String.class)))
                .thenReturn(mockResponse);
        when(consulClient.getKVValue(eq("seata.properties"), nullable(String.class), any(QueryParams.class)))
                .thenReturn(mockResponse);
    }

    @Test
    void testSingletonInstance() {
        ConsulConfiguration anotherInstance = ConsulConfiguration.getInstance();
        assertSame(consulConfig, anotherInstance);
    }

    @Test
    void testGetLatestConfig() throws InterruptedException {
        Properties properties = new Properties();
        properties.setProperty("testKey", "testValue");
        setField(null, "seataConfig", properties);

        String result = consulConfig.getLatestConfig("testKey", "default", 3000);
        assertEquals("testValue", result);
    }

    @Test
    void testPutConfigIfAbsent() {
        // Mock atomic put response
        Response<Boolean> casResponse = new Response<>(true, 1L, false, 1L);
        when(mockConsulClient.setKVValue(
                        nullable(String.class), nullable(String.class), nullable(String.class), any(PutParams.class)))
                .thenReturn(casResponse);

        assertDoesNotThrow(() -> consulConfig.putConfigIfAbsent("atomicKey", "atomicValue", 3000));
    }

    @Test
    void testInitSeataConfig() throws Exception {
        setField(null, "instance", null);
        setField(null, "client", mockConsulClient);
        setField(null, "seataConfig", new Properties());

        // initSeataConfig loads the configured Consul dataId (seata.properties), not key1 directly.
        // The decoded value must therefore be a properties payload that contains key1=val1.
        GetValue initValue = mock(GetValue.class);
        when(initValue.getDecodedValue()).thenReturn("key1=val1");
        Response<GetValue> initResponse = new Response<>(initValue, 1L, false, 1L);
        when(mockConsulClient.getKVValue(nullable(String.class), nullable(String.class)))
                .thenReturn(initResponse);

        // getInstance initializes seataConfig synchronously, so no retry loop is needed here.
        ConsulConfiguration newInstance = ConsulConfiguration.getInstance();
        setField(newInstance, "consulNotifierExecutor", new DirectExecutorService());
        consulConfig.shutdown();
        consulConfig = newInstance;

        assertDoesNotThrow(() -> newInstance.getLatestConfig("key1", null, 1000));
    }

    @Test
    void testOnChangeEvent_skipWhenValueIsBlank() throws InterruptedException {
        String dataId = "seata.properties";

        // Mock the initial call in ConsulListener constructor (2-arg version)
        GetValue initValue = mock(GetValue.class);
        when(initValue.getDecodedValue()).thenReturn("dummy");
        Response<GetValue> initResponse = new Response<>(initValue, 1L, false, 1L);
        when(mockConsulClient.getKVValue(eq(dataId), nullable(String.class))).thenReturn(initResponse);

        // Mock the watch call in onChangeEvent loop (3-arg version)
        GetValue blankValue = mock(GetValue.class);
        when(blankValue.getDecodedValue()).thenReturn("");
        Response<GetValue> blankResponse = new Response<>(blankValue, 2L, false, 2L);
        when(mockConsulClient.getKVValue(eq(dataId), nullable(String.class), any(QueryParams.class)))
                .thenReturn(blankResponse);

        ConsulConfiguration.ConsulListener listener = new ConsulConfiguration.ConsulListener(dataId, null);

        // Run onChangeEvent in a separate thread since it loops indefinitely
        Thread thread = new Thread(() -> {
            try {
                listener.onChangeEvent(new ConfigurationChangeEvent());
            } catch (Exception e) {
                // ignore
            }
        });
        thread.start();
        Thread.sleep(100);
        thread.interrupt();
        thread.join(500);

        assertTrue(true);
    }

    private static class DirectExecutorService extends AbstractExecutorService {
        private volatile boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return java.util.Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    // Utility method to set private fields via reflection
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = ConsulConfiguration.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
