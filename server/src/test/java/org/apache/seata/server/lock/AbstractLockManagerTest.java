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
package org.apache.seata.server.lock;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.LockKeyConverter;
import org.apache.seata.core.lock.Locker;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class AbstractLockManagerTest {

    @Test
    void shouldCollectEncodedLockKeysContainingSemicolon() {
        TestLockManager lockManager = new TestLockManager();
        String resourceId = "jdbc:sqlserver://127.0.0.1:1433;databaseName=demo";
        String lockKey = LockKeyConverter.encode("t_order:pk;001") + ";" + LockKeyConverter.encode("t_order:pk;002");

        List<RowLock> rowLocks = lockManager.collect(lockKey, resourceId, "127.0.0.1:8091:1");

        Assertions.assertEquals(2, rowLocks.size());
        Assertions.assertEquals("pk;001", rowLocks.get(0).getPk());
        Assertions.assertEquals("pk;002", rowLocks.get(1).getPk());
        Assertions.assertEquals(resourceId, rowLocks.get(0).getResourceId());
    }

    @Test
    void shouldCollectPlainLockKeysAsBefore() {
        TestLockManager lockManager = new TestLockManager();

        List<RowLock> rowLocks =
                lockManager.collect("t_order:1,2", "jdbc:mysql://127.0.0.1:3306/demo", "127.0.0.1:8091:1");

        Assertions.assertEquals(2, rowLocks.size());
        Assertions.assertEquals("1", rowLocks.get(0).getPk());
        Assertions.assertEquals("2", rowLocks.get(1).getPk());
    }

    private static final class TestLockManager extends AbstractLockManager {

        @Override
        protected Locker getLocker(BranchSession branchSession) {
            return new Locker() {
                @Override
                public boolean acquireLock(List<RowLock> rowLock) {
                    return false;
                }

                @Override
                public boolean acquireLock(List<RowLock> rowLock, boolean autoCommit, boolean skipCheckLock) {
                    return false;
                }

                @Override
                public boolean releaseLock(List<RowLock> rowLock) {
                    return false;
                }

                @Override
                public boolean releaseLock(String xid, Long branchId) {
                    return false;
                }

                @Override
                public boolean releaseLock(String xid) {
                    return false;
                }

                @Override
                public boolean isLockable(List<RowLock> rowLock) {
                    return false;
                }

                @Override
                public void updateLockStatus(String xid, LockStatus lockStatus) {}

                @Override
                public void cleanAllLocks() {}
            };
        }

        @Override
        public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
            return false;
        }

        private List<RowLock> collect(String lockKey, String resourceId, String xid) {
            return collectRowLocks(lockKey, resourceId, xid);
        }
    }
}
