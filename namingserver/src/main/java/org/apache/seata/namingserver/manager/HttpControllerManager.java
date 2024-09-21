package org.apache.seata.namingserver.manager;

import org.apache.seata.core.rpc.netty.http.HttpController;
import org.apache.seata.core.rpc.netty.http.HttpDispatchHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class HttpControllerManager {

    @Autowired
    private List<HttpController> httpControllerList;

    @PostConstruct
    private void init() {
        httpControllerList.forEach(HttpDispatchHandler::addHttpController);
    }
}
