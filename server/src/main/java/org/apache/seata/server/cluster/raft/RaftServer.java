package org.apache.seata.server.cluster.raft;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.entity.PeerId;

import java.io.Closeable;
import java.io.IOException;

/**
 * smallest unit of raft group used to extend both controller and transaction raft servers from
 */
public interface RaftServer extends Closeable {
    void start() throws IOException;

    Node getNode();

    RaftStateMachine getRaftStateMachine();

    PeerId getServerId();

    boolean isController();
}
