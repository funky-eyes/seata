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
package org.apache.seata.namingserver.controller;

import io.netty.channel.Channel;
import org.apache.seata.namingserver.listener.Watcher;
import org.apache.seata.namingserver.manager.ClusterWatcherManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/naming/v2")
public class NamingControllerV2 {

    @Resource
    private ClusterWatcherManager clusterWatcherManager;

    /**
     * @param clientTerm The timestamp of the subscription saved on the client side
     * @param vGroup     The name of the transaction group
     * @param timeout    The timeout duration
     */
    @PostMapping("/watch")
    public void watch(@RequestParam String clientTerm,
                      @RequestParam String vGroup,
                      @RequestParam String timeout,
                      Channel channel) {
        Watcher<Channel> watcher = new Watcher<>(vGroup, channel, Integer.parseInt(timeout), Long.parseLong(clientTerm), channel.remoteAddress().toString());
        clusterWatcherManager.registryWatcher(watcher);
    }
}
