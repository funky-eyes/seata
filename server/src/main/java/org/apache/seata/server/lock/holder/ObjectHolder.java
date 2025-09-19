package org.apache.seata.server.lock.holder;

/**
 * ObjectHolder represents the unique owner of a lock, replacing the original BranchSession-based ownership.
 */
public class ObjectHolder {

    /**
     * Parent identifier - replaces the xid field in the original BranchSession
     */
    private String parent;

    /**
     * Owner identifier - replaces the branch id in the original BranchSession
     */
    private String owner;

    /**
     * Lock timestamp for ordering and timeout management
     */
    private long lockTime;

    /**
     * Optional instance identifier for multi-instance scenarios
     */
    private String instance;

    public ObjectHolder() {}

    public ObjectHolder(String parent, String owner) {
        this.parent = parent;
        this.owner = owner;
        this.lockTime = System.currentTimeMillis();
    }

    public ObjectHolder(String parent, String owner, String instance) {
        this.parent = parent;
        this.owner = owner;
        this.instance = instance;
        this.lockTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getLockTime() {
        return lockTime;
    }

    public void setLockTime(long lockTime) {
        this.lockTime = lockTime;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectHolder that = (ObjectHolder) o;
        return parent.equals(that.parent) && owner.equals(that.owner);
    }

    @Override
    public int hashCode() {
        return parent.hashCode() * 31 + owner.hashCode();
    }

    @Override
    public String toString() {
        return "ObjectHolder{" + "parent='"
                + parent + '\'' + ", owner='"
                + owner + '\'' + ", instance='"
                + instance + '\'' + ", lockTime="
                + lockTime + '}';
    }
}
