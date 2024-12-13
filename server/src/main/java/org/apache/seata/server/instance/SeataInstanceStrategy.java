package org.apache.seata.server.instance;

import org.apache.seata.common.metadata.namingserver.Instance;

public interface SeataInstanceStrategy {
    Instance serverInstanceInit();

    void init();

    Type type();

    enum Type {
        GENERAL, RAFT
    }

}
