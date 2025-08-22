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
package org.apache.seata.server.cluster.raft.snapshot.txg;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.server.cluster.raft.service.RaftGroupStoreManager;
import org.apache.seata.server.cluster.raft.snapshot.RaftSnapshot;
import org.apache.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 */
public class TransactionGroupSnapshotFile implements Serializable, StoreSnapshotFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionGroupSnapshotFile.class);

    private static final long serialVersionUID = 7942307427240595916L;

    String group;

    String fileName = "txg";
    private final RaftGroupStoreManager raftGroupStoreManager;

    public TransactionGroupSnapshotFile(String group, RaftGroupStoreManager raftGroupStoreManager) {
        this.group = group;
        this.raftGroupStoreManager = raftGroupStoreManager;
    }

    @Override
    public Status save(SnapshotWriter writer) {
        Map<String, List<Node>> groups = raftGroupStoreManager.getGroupPeersMap();
        RaftSnapshot raftSnapshot = new RaftSnapshot();
        raftSnapshot.setBody(groups);
        raftSnapshot.setType(RaftSnapshot.SnapshotType.session);
        LOGGER.info("groupId: {}, txg size: {}", group, groups.size());
        String path = writer.getPath() + File.separator + fileName;
        try {
            if (save(raftSnapshot, path)) {
                if (writer.addFile(fileName)) {
                    return Status.OK();
                } else {
                    return new Status(RaftError.EIO, "Fail to add file to writer");
                }
            }
        } catch (IOException e) {
            LOGGER.error("Fail to save groupId: {} snapshot {}", group, path, e);
        }
        return new Status(RaftError.EIO, "Fail to save groupId: " + group + " snapshot %s", path);
    }

    @Override
    public boolean load(SnapshotReader reader) {
        if (reader.getFileMeta(fileName) == null) {
            LOGGER.error("Fail to find data file in {}", reader.getPath());
            return false;
        }
        String path = reader.getPath() + File.separator + fileName;
        try {
            LOGGER.info(
                    "group: {}, on snapshot load start index: {}",
                    group,
                    reader.load().getLastIncludedIndex());
            raftGroupStoreManager.clear();
            raftGroupStoreManager.saveOrUpdate((Map<String, List<Node>>) load(path));
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "group: {},on snapshot load end index: {}",
                        group,
                        reader.load().getLastIncludedIndex());
            }
            return true;
        } catch (final Exception e) {
            LOGGER.error("group: {}, fail to load snapshot from {}", group, path, e);
            return false;
        }
    }
}

