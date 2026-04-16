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

import org.apache.seata.common.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.seata.common.Constants.ROW_LOCK_KEY_SPLIT_CHAR;
import static org.apache.seata.core.constants.RedisKeyConstants.DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX;

/**
 * Utility for Redis global lock row key serialization.
 * Splits stored lock keys on {@code ;SEATA_ROW_LOCK_} boundaries so semicolons inside resourceIds
 * (for example SQL Server JDBC URLs) are preserved.
 */
public final class RedisLockKeyHelper {

    private static final String NEXT_ROW_LOCK_KEY_BOUNDARY =
            ROW_LOCK_KEY_SPLIT_CHAR + DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX;

    private RedisLockKeyHelper() {}

    public static String joinStoredLockKeys(List<String> lockKeys) {
        return String.join(ROW_LOCK_KEY_SPLIT_CHAR, lockKeys);
    }

    public static List<String> splitStoredLockKeys(String storedLockKeys) {
        if (StringUtils.isBlank(storedLockKeys)) {
            return Collections.emptyList();
        }
        List<String> lockKeys = new ArrayList<>();
        int start = 0;
        while (start < storedLockKeys.length()) {
            int next = storedLockKeys.indexOf(NEXT_ROW_LOCK_KEY_BOUNDARY, start);
            if (next < 0) {
                lockKeys.add(storedLockKeys.substring(start));
                break;
            }
            lockKeys.add(storedLockKeys.substring(start, next));
            start = next + ROW_LOCK_KEY_SPLIT_CHAR.length();
        }
        return lockKeys;
    }
}
