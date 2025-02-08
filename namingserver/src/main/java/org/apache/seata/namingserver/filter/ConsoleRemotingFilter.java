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

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.namingserver.manager.NamingManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class ConsoleRemotingFilter implements Filter {

    NamingManager namingManager;

    RestTemplate restTemplate;

    public ConsoleRemotingFilter(NamingManager namingManager, RestTemplate restTemplate) {
        this.namingManager = namingManager;
        this.restTemplate = restTemplate;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest)servletRequest;
            HttpServletResponse response = (HttpServletResponse)servletResponse;
            String namespace = request.getHeader("x-seata-namespace");
            String cluster = request.getHeader("x-seata-cluster");
            if (StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(cluster)) {
                List<Node> list = namingManager.getInstances(namespace, cluster);
                if (CollectionUtils.isNotEmpty(list)) {
                    // Randomly select a node from the list
                    Node node = list.get(ThreadLocalRandom.current().nextInt(list.size()));
                    Node.Endpoint controlEndpoint = node.getControl();

                    if (controlEndpoint != null) {
                        // Construct the target URL
                        String targetUrl = "http://" + controlEndpoint.getHost() + ":" + controlEndpoint.getPort()
                            + request.getRequestURI();

                        // Copy headers from the original request
                        HttpHeaders headers = new HttpHeaders();
                        Collections.list(request.getHeaderNames()).forEach(headerName -> {
                            headers.add(headerName, request.getHeader(headerName));
                        });

                        // Create the HttpEntity with headers and body
                        HttpEntity<byte[]> httpEntity =
                            new HttpEntity<>(IOUtils.toByteArray(request.getInputStream()), headers);

                        // Forward the request
                        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(URI.create(targetUrl),
                            HttpMethod.resolve(request.getMethod()), httpEntity, byte[].class);

                        // Copy response headers and status code
                        responseEntity.getHeaders().forEach((key, value) -> {
                            value.forEach(v -> response.addHeader(key, v));
                        });
                        response.setStatus(responseEntity.getStatusCodeValue());
                        // Write response body
                        Optional.ofNullable(responseEntity.getBody()).ifPresent(body -> {
                            try {
                                response.getOutputStream().write(body);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        return;
                    }
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

}
