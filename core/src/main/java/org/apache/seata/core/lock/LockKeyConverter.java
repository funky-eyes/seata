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
package org.apache.seata.core.lock;

import org.apache.seata.common.Constants;
import org.apache.seata.common.util.StringUtils;

import java.util.Base64;

/**
 * Converter for lock key segments transported between RM and TC.
 */
public final class LockKeyConverter {

    public static final String BASE64_PREFIX = "!B64!";

    private LockKeyConverter() {}

    public static String encode(String lockKey) {
        if (StringUtils.isBlank(lockKey) || isEncoded(lockKey)) {
            return lockKey;
        }
        return BASE64_PREFIX
                + Base64.getUrlEncoder().withoutPadding().encodeToString(lockKey.getBytes(Constants.DEFAULT_CHARSET));
    }

    public static String decodeIfNecessary(String lockKey) {
        if (!isEncoded(lockKey)) {
            return lockKey;
        }
        String encodedValue = lockKey.substring(BASE64_PREFIX.length());
        if (StringUtils.isBlank(encodedValue)) {
            return lockKey;
        }
        try {
            return new String(Base64.getUrlDecoder().decode(encodedValue), Constants.DEFAULT_CHARSET);
        } catch (IllegalArgumentException ignored) {
            return lockKey;
        }
    }

    public static boolean isEncoded(String lockKey) {
        return StringUtils.isNotBlank(lockKey) && lockKey.startsWith(BASE64_PREFIX);
    }
}
