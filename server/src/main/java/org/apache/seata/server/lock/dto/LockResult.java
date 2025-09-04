package org.apache.seata.server.lock.dto;

/**
 * Lock operation result enumeration
 */
public enum LockResult {
    SUCCESS,
    FAILED,
    CONFLICT,
    TIMEOUT,
    NOT_LOCKABLE,
    PARTIAL_SUCCESS
}
