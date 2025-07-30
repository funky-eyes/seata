package org.apache.seata.server.cluster.raft;

import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.rpc.CliClientService;

import java.io.IOException;
import java.util.Collection;

/**
 * this is server manager that will create different raft servers (controller and transaction)
 */
public interface RaftServerManager<T> {
    void init(String initConfStr);

    void init();

    Collection<String> getRaftGroups();

    CliService getCliServiceInstance();

    CliClientService getCliClientServiceInstance();

    T createRaftServer(String dataPath, String group, Configuration configuration) throws IOException;
}
