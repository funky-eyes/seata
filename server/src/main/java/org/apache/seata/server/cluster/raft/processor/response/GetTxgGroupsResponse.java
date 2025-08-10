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

package org.apache.seata.server.cluster.raft.processor.response;

import org.apache.seata.server.cluster.raft.sync.msg.dto.TxgGroupAssignmentDTO;

import java.io.Serializable;

public class GetTxgGroupsResponse implements Serializable {
    private static final long serialVersionUID = -5693903495378729171L;

    private boolean success;
    private String errorMsg;
    private TxgGroupAssignmentDTO txgAssignments; // Reuse existing DTO!

    public GetTxgGroupsResponse() {}

    public GetTxgGroupsResponse(boolean success) {
        this.success = success;
    }

    public GetTxgGroupsResponse(boolean success, TxgGroupAssignmentDTO txgAssignments) {
        this.success = success;
        this.txgAssignments = txgAssignments;
    }

    public GetTxgGroupsResponse(boolean success, String errorMsg) {
        this.success = success;
        this.errorMsg = errorMsg;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public TxgGroupAssignmentDTO getTxgAssignments() {
        return txgAssignments;
    }

    public void setTxgAssignments(TxgGroupAssignmentDTO txgAssignments) {
        this.txgAssignments = txgAssignments;
    }
}
