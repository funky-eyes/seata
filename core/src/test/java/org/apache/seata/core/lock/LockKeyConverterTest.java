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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LockKeyConverterTest {

    @Test
    void shouldEncodeAndDecodeLockKeyWithSemicolon() {
        String lockKey = "t_order:pk;001";

        String encoded = LockKeyConverter.encode(lockKey);

        Assertions.assertTrue(LockKeyConverter.isEncoded(encoded));
        Assertions.assertFalse(encoded.contains(";"));
        Assertions.assertEquals(lockKey, LockKeyConverter.decodeIfNecessary(encoded));
    }

    @Test
    void shouldKeepPlainLockKeyUnchanged() {
        String lockKey = "t_order:1";

        Assertions.assertEquals(lockKey, LockKeyConverter.decodeIfNecessary(lockKey));
    }

    @Test
    void shouldKeepInvalidEncodedPrefixUnchanged() {
        String lockKey = LockKeyConverter.BASE64_PREFIX + "not*base64";

        Assertions.assertEquals(lockKey, LockKeyConverter.decodeIfNecessary(lockKey));
    }
}
