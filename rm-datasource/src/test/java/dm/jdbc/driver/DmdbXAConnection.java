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
package dm.jdbc.driver;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal Dameng XAConnection stub for unit tests.
 */
public class DmdbXAConnection implements XAConnection {

    private final DmdbConnection connection;
    private XAResource xaResource;
    private final List<ConnectionEventListener> connectionEventListeners = new CopyOnWriteArrayList<>();

    public DmdbXAConnection(DmdbConnection connection) {
        this.connection = connection;
    }

    @Override
    public XAResource getXAResource() {
        return xaResource;
    }

    public void setXaResource(XAResource xaResource) {
        this.xaResource = xaResource;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        // no-op for test stub
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.remove(listener);
    }

    @Override
    public void addStatementEventListener(StatementEventListener listener) {
        // no-op for test stub
    }

    @Override
    public void removeStatementEventListener(StatementEventListener listener) {
        // no-op for test stub
    }
}
