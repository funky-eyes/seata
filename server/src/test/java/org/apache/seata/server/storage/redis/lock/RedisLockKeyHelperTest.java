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
package org.apache.seata.server.storage.redis.lock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class RedisLockKeyHelperTest {

    @Test
    void shouldSplitLegacyStoredKeys() {
        List<String> lockKeys = Arrays.asList(
                "SEATA_ROW_LOCK_jdbc:mysql://127.0.0.1:3306/db^^^t_order^^^1",
                "SEATA_ROW_LOCK_jdbc:mysql://127.0.0.1:3306/db^^^t_order^^^2");

        Assertions.assertEquals(lockKeys, RedisLockKeyHelper.splitStoredLockKeys(String.join(";", lockKeys)));
    }

    @Test
    void shouldKeepSqlServerResourceIdSemicolonInsideSingleKey() {
        String lockKey = "SEATA_ROW_LOCK_jdbc:sqlserver://127.0.0.1:1433;databaseName=lxk^^^BPM_ACT_RU_TASK^^^123";

        Assertions.assertEquals(Collections.singletonList(lockKey), RedisLockKeyHelper.splitStoredLockKeys(lockKey));
    }

    @Test
    void shouldSplitMultipleKeysWhenResourceIdContainsSemicolon() {
        List<String> lockKeys = Arrays.asList(
                "SEATA_ROW_LOCK_jdbc:sqlserver://127.0.0.1:1433;databaseName=lxk^^^BPM_ACT_RU_TASK^^^123",
                "SEATA_ROW_LOCK_jdbc:sqlserver://127.0.0.1:1433;databaseName=lxk^^^BPM_ACT_RU_TASK^^^456");

        String storedLockKeys = RedisLockKeyHelper.joinStoredLockKeys(lockKeys);

        Assertions.assertEquals(lockKeys, RedisLockKeyHelper.splitStoredLockKeys(storedLockKeys));
    }

    @Test
    void shouldReturnEmptyListWhenStoredKeysBlank() {
        Assertions.assertTrue(RedisLockKeyHelper.splitStoredLockKeys(null).isEmpty());
        Assertions.assertTrue(RedisLockKeyHelper.splitStoredLockKeys("").isEmpty());
    }
}
