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
package org.apache.seata.namingserver.filter;

import jakarta.servlet.FilterChain;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.namingserver.manager.NamingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link ConsoleRemotingFilter}.
 * <p>
 * Covers the GET/HEAD body-stripping regression on upstream proxy requests
 * and other proxy-forwarding behavior.
 */
class ConsoleRemotingFilterTest {

    private NamingManager namingManager;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private ConsoleRemotingFilter filter;
    private FilterChain filterChain;

    private static final String NAMESPACE = "public";
    private static final String CLUSTER = "default";
    private static final String TARGET_HOST = "127.0.0.1";
    private static final int TARGET_PORT = 7091;

    @BeforeEach
    void setUp() {
        namingManager = mock(NamingManager.class);
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        filterChain = mock(FilterChain.class);
        filter = new ConsoleRemotingFilter(namingManager, restClientBuilder.build());

        // Set up a NamingServerNode with a control endpoint
        NamingServerNode node = new NamingServerNode();
        node.setControl(new Node.Endpoint(TARGET_HOST, TARGET_PORT, "http"));

        when(namingManager.getInstances(NAMESPACE, CLUSTER)).thenReturn(Collections.singletonList(node));
    }

    /**
     * Regression test: a GET request with a non-empty body should NOT forward
     * the body to the upstream server.
     * The body, Content-Length, and Transfer-Encoding headers must be stripped.
     */
    @Test
    void getRequestWithBodyShouldStripBody() throws Exception {
        MockHttpServletRequest request = createConsoleRequest("GET");
        request.setContent("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader(HttpHeaders.CONTENT_LENGTH, "15");

        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist(HttpHeaders.CONTENT_LENGTH))
                .andExpect(headerDoesNotExist(HttpHeaders.TRANSFER_ENCODING))
                .andExpect(this::expectEmptyBody)
                .andRespond(withSuccess("{\"result\":\"ok\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();
        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }

    /**
     * HEAD request should also strip the body, same as GET.
     */
    @Test
    void headRequestShouldStripBody() throws Exception {
        MockHttpServletRequest request = createConsoleRequest("HEAD");
        request.setContent("some body".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.HEAD))
                .andExpect(this::expectEmptyBody)
                .andRespond(withStatus(HttpStatus.OK).contentType(org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();
        verify(filterChain, never()).doFilter(any(), any());
    }

    /**
     * POST request should forward the body as-is.
     */
    @Test
    void postRequestShouldForwardBody() throws Exception {
        byte[] bodyBytes = "{\"data\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = createConsoleRequest("POST");
        request.setContent(bodyBytes);

        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(clientHttpRequest -> assertEquals(
                        new String(bodyBytes, StandardCharsets.UTF_8),
                        new String(
                                ((MockClientHttpRequest) clientHttpRequest).getBodyAsBytes(), StandardCharsets.UTF_8),
                        "POST request body should be forwarded as-is"))
                .andRespond(
                        withSuccess("{\"result\":\"created\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();
    }

    @Test
    void putRequestShouldForwardBody() throws Exception {
        byte[] bodyBytes = "{\"data\":\"test-put\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = createConsoleRequest("PUT");
        request.setContent(bodyBytes);

        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(clientHttpRequest -> assertEquals(
                        new String(bodyBytes, StandardCharsets.UTF_8),
                        new String(
                                ((MockClientHttpRequest) clientHttpRequest).getBodyAsBytes(), StandardCharsets.UTF_8),
                        "PUT request body should be forwarded as-is"))
                .andRespond(
                        withSuccess("{\"result\":\"updated\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();
    }

    @Test
    void deleteRequestShouldForwardBody() throws Exception {
        byte[] bodyBytes = "{\"data\":\"test-delete\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = createConsoleRequest("DELETE");
        request.setContent(bodyBytes);

        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(clientHttpRequest -> assertEquals(
                        new String(bodyBytes, StandardCharsets.UTF_8),
                        new String(
                                ((MockClientHttpRequest) clientHttpRequest).getBodyAsBytes(), StandardCharsets.UTF_8),
                        "DELETE request body should be forwarded as-is"))
                .andRespond(
                        withSuccess("{\"result\":\"deleted\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();
    }

    /**
     * Non-matching URL should pass through the filter chain without proxying.
     */
    @Test
    void nonConsoleUrlShouldPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/other/endpoint");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    /**
     * Upstream returning an HTML body with application/json Content-Type should
     * be replaced with a 502 error response.
     */
    @Test
    void nonJsonBodyWithJsonContentTypeShouldReturn502() throws Exception {
        MockHttpServletRequest request = createConsoleRequest("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "<html><script>alert('xss')</script></html>",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();

        assertEquals(502, response.getStatus(), "Should return 502 when upstream body is not valid JSON");
        String body = response.getContentAsString();
        assertEquals("{\"error\":\"Upstream returned invalid response body\"}", body);
    }

    @Test
    void non2xxResponseShouldStillBeProxied() throws Exception {
        MockHttpServletRequest request = createConsoleRequest("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"upstream failed\"}"));

        filter.doFilter(request, response, filterChain);
        server.verify();

        assertEquals(502, response.getStatus(), "代理模式下应保留上游非 2xx 状态码");
        assertEquals("{\"error\":\"upstream failed\"}", response.getContentAsString());
    }

    /**
     * Helper: create a MockHttpServletRequest that matches the console URL pattern
     * and includes the required namespace/cluster headers.
     */
    private MockHttpServletRequest createConsoleRequest(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/console/globalSession/query");
        request.addHeader("x-seata-namespace", NAMESPACE);
        request.addHeader("x-seata-cluster", CLUSTER);
        return request;
    }

    private void expectEmptyBody(org.springframework.http.client.ClientHttpRequest request) throws IOException {
        assertEquals(
                0,
                ((MockClientHttpRequest) request).getBodyAsBytes().length,
                "GET/HEAD request body should be stripped");
    }
}
