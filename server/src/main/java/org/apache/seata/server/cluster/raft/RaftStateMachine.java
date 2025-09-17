/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seata.server.cluster.raft;

import com.alipay.sofa.jraft.core.StateMachineAdapter;
import org.apache.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import org.apache.seata.server.store.StoreConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    protected final Lock lock = new ReentrantLock();

    protected final List<StoreSnapshotFile> snapshotFiles = new ArrayList<>();

    protected final String mode;

    protected final String group;

    protected RaftStateMachine(String group) {
        this.group = group;
        this.mode = StoreConfig.getSessionMode().getName();
    }

    public boolean isLeader() {
        return this.leaderTerm.get() > 0;
    }

    public AtomicLong getCurrentTerm() {
        return this.currentTerm;
    }
}
