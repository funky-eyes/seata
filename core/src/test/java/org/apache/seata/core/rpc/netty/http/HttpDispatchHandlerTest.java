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
package org.apache.seata.core.rpc.netty.http;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpDispatchHandlerTest {

    private HttpDispatchHandler handler;
    private EmbeddedChannel channel;
    private TestController testController = new TestController();

    class TestController {
        public String handleRequest(String param) {
            return "Processed: " + param;
        }
    }

    @BeforeEach
    void setUp() {
        handler = new HttpDispatchHandler();
        channel = new EmbeddedChannel(handler);
    }

    @Test
    void testGetRequestWithParameters() throws Exception {
        try (MockedStatic<ControllerManager> mocked = Mockito.mockStatic(ControllerManager.class)) {
            // 准备测试方法和调用信息
            Method method = TestController.class.getMethod("handleRequest", String.class);
            ParamMetaData paramMetaData = new ParamMetaData();
            paramMetaData.setParamConvertType(ParamMetaData.ParamConvertType.REQUEST_PARAM);
            ParamMetaData[] paramMetaDatas = new ParamMetaData[]{paramMetaData};
            HttpInvocation invocation = new HttpInvocation();
            invocation.setController(testController);
            invocation.setMethod(method);
            invocation.setParamMetaData(paramMetaDatas);

            // 模拟ControllerManager返回
            mocked.when(() -> ControllerManager.getHttpInvocation("/test"))
                    .thenReturn(invocation);

            // 构建GET请求
            HttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.GET,
                    "/test?param=testValue"
            );

            // 处理请求
            channel.writeInbound(request);

            // 验证响应
            FullHttpResponse response = channel.readOutbound();
            assertEquals(HttpResponseStatus.OK, response.status());
            String content = response.content().toString(StandardCharsets.UTF_8);
            assertTrue(content.contains("Processed: testValue"));
        }
    }

    @Test
    void testRequestToNonexistentPath() {
        try (MockedStatic<ControllerManager> mocked = Mockito.mockStatic(ControllerManager.class)) {
            // 模拟找不到路径
            mocked.when(() -> ControllerManager.getHttpInvocation("/notfound"))
                    .thenReturn(null);

            // 构建请求
            HttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.GET,
                    "/notfound"
            );

            // 处理请求
            channel.writeInbound(request);

            // 验证404响应
            FullHttpResponse response = channel.readOutbound();
            assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
        }
    }

    @Test
    void testHttpHeadMethod() {
        try (MockedStatic<ControllerManager> mocked = Mockito.mockStatic(ControllerManager.class)) {
            // 模拟HEAD请求处理
            mocked.when(() -> ControllerManager.getHttpInvocation("/head"))
                    .thenReturn(null);

            // 构建HEAD请求
            HttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.HEAD,
                    "/head"
            );

            // 处理请求
            channel.writeInbound(request);

            // 验证响应
            FullHttpResponse response = channel.readOutbound();
            assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
            assertEquals(0, response.content().readableBytes());
        }
    }
}