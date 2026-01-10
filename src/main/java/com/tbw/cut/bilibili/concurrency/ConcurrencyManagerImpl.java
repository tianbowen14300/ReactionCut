package com.tbw.cut.bilibili.concurrency;

import com.tbw.cut.bilibili.config.RetryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Implementation of ConcurrencyManager using semaphore-based concurrency control
 * 
 * This class manages concurrent retry attempts to prevent API rate limiting
 * and resource exhaustion.
 */
@Component
public class ConcurrencyManagerImpl implements ConcurrencyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyManagerImpl.class);
    
    private final RetryConfiguration config;
    private final Semaphore concurrencyLimiter;
    private final ExecutorService executorService;
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicInteger queuedTasks = new AtomicInteger(0);
    
    // Statistics
    private final AtomicLong totalTasksExecuted = new AtomicLong(0);
    private final AtomicLong totalTasksQueued = new AtomicLong(0);
    private final AtomicLong totalTasksTimedOut = new AtomicLong(0);
    private final AtomicLong totalTasksRejected = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicLong totalQueueTime = new AtomicLong(0);
    private final LocalDateTime collectionStartTime = LocalDateTime.now();
    private volatile LocalDateTime lastUpdateTime = LocalDateTime.now();
    
    @Autowired
    public ConcurrencyManagerImpl(RetryConfiguration config) {
        this.config = config;
        this.concurrencyLimiter = new Semaphore(config.getMaxConcurrentAttempts(), true);
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "concurrency-manager-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        logger.info("Initialized ConcurrencyManager with max concurrent attempts: {}", 
                   config.getMaxConcurrentAttempts());
    }
    
    @Override
    public <T> CompletableFuture<T> executeWithConcurrencyControl(Supplier<T> task, long timeoutMs) {
        return executeWithConcurrencyControl(task, timeoutMs, "unknown");
    }
    
    @Override
    public <T> CompletableFuture<T> executeWithConcurrencyControl(Supplier<T> task, long timeoutMs, String operationName) {
        if (task == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        long queueStartTime = System.currentTimeMillis();
        queuedTasks.incrementAndGet();
        totalTasksQueued.incrementAndGet();
        lastUpdateTime = LocalDateTime.now();
        
        CompletableFuture<T> future = new CompletableFuture<>();
        
        // Submit task to executor
        executorService.submit(() -> {
            boolean acquired = false;
            try {
                // Try to acquire semaphore permit
                acquired = concurrencyLimiter.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
                
                if (!acquired) {
                    logger.warn("Task timed out waiting for concurrency permit: {}", operationName);
                    totalTasksTimedOut.incrementAndGet();
                    future.completeExceptionally(new TimeoutException("Timed out waiting for concurrency permit"));
                    return;
                }
                
                long queueTime = System.currentTimeMillis() - queueStartTime;
                totalQueueTime.addAndGet(queueTime);
                queuedTasks.decrementAndGet();
                activeTasks.incrementAndGet();
                
                logger.debug("Executing task with concurrency control: {} (queue time: {}ms)", 
                           operationName, queueTime);
                
                long executionStartTime = System.currentTimeMillis();
                
                try {
                    T result = task.get();
                    long executionTime = System.currentTimeMillis() - executionStartTime;
                    
                    totalTasksExecuted.incrementAndGet();
                    totalExecutionTime.addAndGet(executionTime);
                    lastUpdateTime = LocalDateTime.now();
                    
                    logger.debug("Task completed successfully: {} (execution time: {}ms)", 
                               operationName, executionTime);
                    
                    future.complete(result);
                    
                } catch (Exception e) {
                    logger.error("Task execution failed: {}", operationName, e);
                    totalTasksRejected.incrementAndGet();
                    future.completeExceptionally(e);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Task interrupted while waiting for concurrency permit: {}", operationName);
                totalTasksRejected.incrementAndGet();
                future.completeExceptionally(e);
                
            } finally {
                if (acquired) {
                    concurrencyLimiter.release();
                    activeTasks.decrementAndGet();
                } else {
                    queuedTasks.decrementAndGet();
                }
            }
        });
        
        return future;
    }
    
    @Override
    public int getActiveTasks() {
        return activeTasks.get();
    }
    
    @Override
    public int getQueuedTasks() {
        return queuedTasks.get();
    }
    
    @Override
    public int getMaxConcurrentTasks() {
        return config.getMaxConcurrentAttempts();
    }
    
    @Override
    public boolean isAtCapacity() {
        return concurrencyLimiter.availablePermits() == 0;
    }
    
    @Override
    public ConcurrencyStatistics getStatistics() {
        long executed = totalTasksExecuted.get();
        long queued = totalTasksQueued.get();
        
        long avgExecutionTime = executed > 0 ? totalExecutionTime.get() / executed : 0;
        long avgQueueTime = queued > 0 ? totalQueueTime.get() / queued : 0;
        
        return new ConcurrencyStatistics(
            executed,
            queued,
            totalTasksTimedOut.get(),
            totalTasksRejected.get(),
            activeTasks.get(),
            queuedTasks.get(),
            avgExecutionTime,
            avgQueueTime,
            collectionStartTime,
            lastUpdateTime
        );
    }
    
    @Override
    public void resetStatistics() {
        totalTasksExecuted.set(0);
        totalTasksQueued.set(0);
        totalTasksTimedOut.set(0);
        totalTasksRejected.set(0);
        totalExecutionTime.set(0);
        totalQueueTime.set(0);
        lastUpdateTime = LocalDateTime.now();
        
        logger.info("Concurrency statistics have been reset");
    }
    
    @Override
    public void shutdown() {
        logger.info("Shutting down ConcurrencyManager...");
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate gracefully, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
        
        logger.info("ConcurrencyManager shutdown complete");
    }
}