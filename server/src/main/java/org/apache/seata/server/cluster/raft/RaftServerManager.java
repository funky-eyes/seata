package org.apache.seata.server.cluster.raft;

import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.rpc.CliClientService;

import java.io.IOException;
import java.util.Collection;

public interface RaftServerManager<T> {

    void init(String initConfStr);

    void init();

    Collection<String> getRaftGroups();

    CliService getCliServiceInstance();

    CliClientService getCliClientServiceInstance();

    T createRaftServer(String dataPath, String group, Configuration configuration) throws IOException;
}
