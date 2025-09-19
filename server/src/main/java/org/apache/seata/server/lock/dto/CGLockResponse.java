package org.apache.seata.server.lock.dto;

/**
 * CG Lock Response DTO
 */
public class CGLockResponse {

    private String requestId;
    private LockResult result;
    private String errorMessage;
    private long processTime;

    public CGLockResponse() {}

    public CGLockResponse(String requestId, LockResult result) {
        this.requestId = requestId;
        this.result = result;
    }

    public static CGLockResponse success(String requestId) {
        return new CGLockResponse(requestId, LockResult.SUCCESS);
    }

    public static CGLockResponse failure(String requestId, String errorMessage) {
        CGLockResponse response = new CGLockResponse(requestId, LockResult.FAILED);
        response.setErrorMessage(errorMessage);
        return response;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public LockResult getResult() {
        return result;
    }

    public void setResult(LockResult result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getProcessTime() {
        return processTime;
    }

    public void setProcessTime(long processTime) {
        this.processTime = processTime;
    }
}
