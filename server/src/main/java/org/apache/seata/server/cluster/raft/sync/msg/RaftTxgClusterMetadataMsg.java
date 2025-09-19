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
package org.apache.seata.server.cluster.raft.sync.msg;

import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;

/**
 * Message for syncing TXG cluster metadata to CG state machine.
 */
public class RaftTxgClusterMetadataMsg extends RaftBaseMsg {

    private static final long serialVersionUID = 1L;

    private String txgGroupId;

    private RaftClusterMetadata metadata;

    public RaftTxgClusterMetadataMsg() {
        this.setMsgType(RaftSyncMsgType.SAVE_OR_UPDATE_TXG_GROUP_ASSIGNMENTS);
    }

    public RaftTxgClusterMetadataMsg(String txgGroupId, RaftClusterMetadata metadata) {
        this();
        this.txgGroupId = txgGroupId;
        this.metadata = metadata;
    }

    public String getTxgGroupId() {
        return txgGroupId;
    }

    public void setTxgGroupId(String txgGroupId) {
        this.txgGroupId = txgGroupId;
    }

    public RaftClusterMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(RaftClusterMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "RaftTxgClusterMetadataMsg{" + "txgGroupId='"
                + txgGroupId + '\'' + ", metadata="
                + metadata + ", msgType="
                + getMsgType() + ", group='"
                + getGroup() + '\'' + '}';
    }
}
