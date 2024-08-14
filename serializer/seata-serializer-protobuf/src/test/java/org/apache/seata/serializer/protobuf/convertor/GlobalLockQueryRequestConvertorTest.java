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
package org.apache.seata.core.serializer.protobuf.convertor;

import org.apache.seata.core.serializer.protobuf.generated.GlobalLockQueryRequestProto;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class GlobalLockQueryRequestConvertorTest {

    @Test
    public void convert2Proto() {

        GlobalLockQueryRequest globalLockQueryRequest = new GlobalLockQueryRequest();
        globalLockQueryRequest.setApplicationData("data");
        globalLockQueryRequest.setBranchType(BranchType.AT);
        globalLockQueryRequest.setLockKey("localKey");
        globalLockQueryRequest.setResourceId("resourceId");
        globalLockQueryRequest.setXid("xid");

        GlobalLockQueryRequestConvertor convertor = new GlobalLockQueryRequestConvertor();
        GlobalLockQueryRequestProto proto = convertor.convert2Proto(
            globalLockQueryRequest);
        GlobalLockQueryRequest real = convertor.convert2Model(proto);

        assertThat(real.getTypeCode()).isEqualTo(globalLockQueryRequest.getTypeCode());
        assertThat(real.getApplicationData()).isEqualTo(globalLockQueryRequest.getApplicationData());
        assertThat(real.getXid()).isEqualTo(globalLockQueryRequest.getXid());
        assertThat(real.getBranchType()).isEqualTo(globalLockQueryRequest.getBranchType());
        assertThat(real.getLockKey()).isEqualTo(globalLockQueryRequest.getLockKey());
        assertThat(real.getResourceId()).isEqualTo(globalLockQueryRequest.getResourceId());
    }
}
