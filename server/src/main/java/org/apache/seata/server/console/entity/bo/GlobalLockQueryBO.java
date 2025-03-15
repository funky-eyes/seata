package org.apache.seata.server.console.entity.bo;

import org.apache.seata.core.lock.RowLock;
import org.apache.seata.server.session.GlobalSession;

public class GlobalLockQueryBO {

    private RowLock rowLock;

    private GlobalSession globalSession;

    public GlobalLockQueryBO(RowLock rowLock, GlobalSession globalSession) {
        this.rowLock = rowLock;
        this.globalSession = globalSession;
    }

    public RowLock getRowLock() {
        return rowLock;
    }

    public void setRowLock(RowLock rowLock) {
        this.rowLock = rowLock;
    }

    public GlobalSession getGlobalSession() {
        return globalSession;
    }

    public void setGlobalSession(GlobalSession globalSession) {
        this.globalSession = globalSession;
    }
}
