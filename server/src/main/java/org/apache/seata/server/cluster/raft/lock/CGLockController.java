package org.apache.seata.server.cluster.raft.lock;

import org.apache.seata.server.lock.CGLockManager;
import org.apache.seata.server.lock.dto.CGLockRequest;
import org.apache.seata.server.lock.dto.CGLockResponse;
import org.apache.seata.server.lock.dto.LockResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for CGLockManager HTTP/2 communication
 * Handles incoming lock requests from other CG nodes or clients
 */
@RestController
@RequestMapping("/api/v1/cg-lock")
public class CGLockController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CGLockController.class);

    @Autowired
    private CGLockManager cgLockManager;

    /**
     * Acquire locks endpoint
     */
    @PostMapping("/acquire")
    public CompletableFuture<ResponseEntity<CGLockResponse>> acquireLock(@RequestBody CGLockRequest request) {
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Received acquire lock request: {}", request.getRequestId());

        return cgLockManager
                .acquireLock(request)
                .thenApply(result -> {
                    long processTime = System.currentTimeMillis() - startTime;
                    CGLockResponse response;

                    if (result == LockResult.SUCCESS) {
                        response = CGLockResponse.success(request.getRequestId());
                    } else {
                        response = CGLockResponse.failure(request.getRequestId(), "Lock acquisition failed: " + result);
                    }

                    response.setProcessTime(processTime);
                    LOGGER.debug(
                            "Lock acquire request {} processed in {}ms with result: {}",
                            request.getRequestId(),
                            processTime,
                            result);

                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    long processTime = System.currentTimeMillis() - startTime;
                    LOGGER.error("Failed to process acquire lock request: {}", request.getRequestId(), throwable);

                    CGLockResponse response =
                            CGLockResponse.failure(request.getRequestId(), "Internal error: " + throwable.getMessage());
                    response.setProcessTime(processTime);

                    return ResponseEntity.internalServerError().body(response);
                });
    }

    /**
     * Release locks endpoint
     */
    @PostMapping("/release")
    public CompletableFuture<ResponseEntity<CGLockResponse>> releaseLock(@RequestBody CGLockRequest request) {
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Received release lock request: {}", request.getRequestId());

        return cgLockManager
                .releaseLock(request)
                .thenApply(result -> {
                    long processTime = System.currentTimeMillis() - startTime;
                    CGLockResponse response;

                    if (result == LockResult.SUCCESS) {
                        response = CGLockResponse.success(request.getRequestId());
                    } else {
                        response = CGLockResponse.failure(request.getRequestId(), "Lock release failed: " + result);
                    }

                    response.setProcessTime(processTime);
                    LOGGER.debug(
                            "Lock release request {} processed in {}ms with result: {}",
                            request.getRequestId(),
                            processTime,
                            result);

                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    long processTime = System.currentTimeMillis() - startTime;
                    LOGGER.error("Failed to process release lock request: {}", request.getRequestId(), throwable);

                    CGLockResponse response =
                            CGLockResponse.failure(request.getRequestId(), "Internal error: " + throwable.getMessage());
                    response.setProcessTime(processTime);

                    return ResponseEntity.internalServerError().body(response);
                });
    }

    /**
     * Check if locks are acquirable endpoint
     */
    @PostMapping("/check")
    public CompletableFuture<ResponseEntity<CGLockResponse>> isLockable(@RequestBody CGLockRequest request) {
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Received lockable check request: {}", request.getRequestId());

        return cgLockManager
                .isLockable(request)
                .thenApply(result -> {
                    long processTime = System.currentTimeMillis() - startTime;
                    CGLockResponse response;

                    if (result == LockResult.SUCCESS) {
                        response = CGLockResponse.success(request.getRequestId());
                    } else if (result == LockResult.NOT_LOCKABLE) {
                        response = CGLockResponse.failure(request.getRequestId(), "Locks are not acquirable");
                    } else {
                        response = CGLockResponse.failure(request.getRequestId(), "Lock check failed: " + result);
                    }

                    response.setProcessTime(processTime);
                    LOGGER.debug(
                            "Lockable check request {} processed in {}ms with result: {}",
                            request.getRequestId(),
                            processTime,
                            result);

                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    long processTime = System.currentTimeMillis() - startTime;
                    LOGGER.error("Failed to process lockable check request: {}", request.getRequestId(), throwable);

                    CGLockResponse response =
                            CGLockResponse.failure(request.getRequestId(), "Internal error: " + throwable.getMessage());
                    response.setProcessTime(processTime);

                    return ResponseEntity.internalServerError().body(response);
                });
    }

    /**
     * Clean all locks endpoint - for snapshot recovery scenarios
     */
    @PostMapping("/clean")
    public CompletableFuture<ResponseEntity<CGLockResponse>> cleanAllLocks() {
        long startTime = System.currentTimeMillis();
        String requestId = "clean_" + System.currentTimeMillis();
        LOGGER.info("Received clean all locks request: {}", requestId);

        return cgLockManager
                .cleanAllLocks()
                .thenApply(result -> {
                    long processTime = System.currentTimeMillis() - startTime;
                    CGLockResponse response;

                    if (result == LockResult.SUCCESS) {
                        response = CGLockResponse.success(requestId);
                        LOGGER.info("All locks cleaned successfully in {}ms", processTime);
                    } else {
                        response = CGLockResponse.failure(requestId, "Failed to clean locks: " + result);
                        LOGGER.error("Failed to clean all locks: {}", result);
                    }

                    response.setProcessTime(processTime);
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    long processTime = System.currentTimeMillis() - startTime;
                    LOGGER.error("Failed to process clean all locks request", throwable);

                    CGLockResponse response =
                            CGLockResponse.failure(requestId, "Internal error: " + throwable.getMessage());
                    response.setProcessTime(processTime);

                    return ResponseEntity.internalServerError().body(response);
                });
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("CG Lock Manager is healthy");
    }
}
