package org.apache.seata.server.cluster.raft;

import com.alipay.sofa.jraft.core.StateMachineAdapter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * raft state machine where the lock maps will be saved
 */
public abstract class RaftStateMachine extends StateMachineAdapter {
    /**
     * Leader term
     */
    protected final AtomicLong leaderTerm = new AtomicLong(-1);

    /**
     * current term
     */
    protected final AtomicLong currentTerm = new AtomicLong(-1);

    protected final AtomicBoolean initSync = new AtomicBoolean(false);

    public boolean isLeader() {
        return this.leaderTerm.get() > 0;
    }

    public AtomicLong getCurrentTerm() {
        return this.currentTerm;
    }
}
