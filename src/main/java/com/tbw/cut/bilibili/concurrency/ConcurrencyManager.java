package com.tbw.cut.bilibili.concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Interface for managing concurrency in retry operations
 * 
 * This interface defines the contract for controlling concurrent retry attempts
 * to prevent API rate limiting and resource exhaustion.
 */
public interface ConcurrencyManager {
    
    /**
     * Execute a task with concurrency control
     * 
     * @param task The task to execute
     * @param timeoutMs Timeout in milliseconds for the task
     * @param <T> The return type of the task
     * @return CompletableFuture with the task result
     */
    <T> CompletableFuture<T> executeWithConcurrencyControl(Supplier<T> task, long timeoutMs);
    
    /**
     * Execute a task with concurrency control and operation name for metrics
     * 
     * @param task The task to execute
     * @param timeoutMs Timeout in milliseconds for the task
     * @param operationName Name of the operation for metrics
     * @param <T> The return type of the task
     * @return CompletableFuture with the task result
     */
    <T> CompletableFuture<T> executeWithConcurrencyControl(Supplier<T> task, long timeoutMs, String operationName);
    
    /**
     * Get the number of currently active tasks
     * 
     * @return Number of active tasks
     */
    int getActiveTasks();
    
    /**
     * Get the number of queued tasks waiting for execution
     * 
     * @return Number of queued tasks
     */
    int getQueuedTasks();
    
    /**
     * Get the maximum allowed concurrent tasks
     * 
     * @return Maximum concurrent tasks
     */
    int getMaxConcurrentTasks();
    
    /**
     * Check if the concurrency limit has been reached
     * 
     * @return true if at maximum capacity
     */
    boolean isAtCapacity();
    
    /**
     * Get concurrency statistics
     * 
     * @return Concurrency statistics object
     */
    ConcurrencyStatistics getStatistics();
    
    /**
     * Reset concurrency statistics
     */
    void resetStatistics();
    
    /**
     * Shutdown the concurrency manager and clean up resources
     */
    void shutdown();
}