package com.tbw.cut.bilibili.metrics;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Detailed metrics for a specific operation
 * 
 * This class tracks comprehensive metrics for individual operations
 * like "getAidFromBvid" or "getVideoInfo".
 */
public class OperationMetrics {
    
    private final String operationName;
    private final AtomicLong totalAttempts = new AtomicLong(0);
    private final AtomicLong successfulOperations = new AtomicLong(0);
    private final AtomicLong failedOperations = new AtomicLong(0);
    private final AtomicLong totalRetryTime = new AtomicLong(0);
    private final AtomicLong circuitBreakerTrips = new AtomicLong(0);
    private volatile LocalDateTime firstOperationTime;
    private volatile LocalDateTime lastOperationTime;
    private volatile long minRetryTime = Long.MAX_VALUE;
    private volatile long maxRetryTime = 0;
    
    // Attempt distribution tracking
    private final AtomicLong[] attemptCounts = new AtomicLong[10]; // Track up to 10 attempts
    
    public OperationMetrics(String operationName) {
        this.operationName = operationName;
        for (int i = 0; i < attemptCounts.length; i++) {
            attemptCounts[i] = new AtomicLong(0);
        }
    }
    
    /**
     * Record a retry attempt
     * 
     * @param attemptNumber The attempt number (1-based)
     */
    public void recordAttempt(int attemptNumber) {
        totalAttempts.incrementAndGet();
        
        // Track attempt distribution
        int index = Math.min(attemptNumber - 1, attemptCounts.length - 1);
        attemptCounts[index].incrementAndGet();
        
        updateTimestamps();
    }
    
    /**
     * Record a successful operation
     * 
     * @param totalTimeMs Total time spent on all retry attempts
     */
    public void recordSuccess(long totalTimeMs) {
        successfulOperations.incrementAndGet();
        recordTiming(totalTimeMs);
    }
    
    /**
     * Record a failed operation
     * 
     * @param totalTimeMs Total time spent on all retry attempts
     */
    public void recordFailure(long totalTimeMs) {
        failedOperations.incrementAndGet();
        recordTiming(totalTimeMs);
    }
    
    /**
     * Record a circuit breaker trip
     */
    public void recordCircuitBreakerTrip() {
        circuitBreakerTrips.incrementAndGet();
    }
    
    private void recordTiming(long timeMs) {
        totalRetryTime.addAndGet(timeMs);
        
        // Update min/max timing
        synchronized (this) {
            if (timeMs < minRetryTime) {
                minRetryTime = timeMs;
            }
            if (timeMs > maxRetryTime) {
                maxRetryTime = timeMs;
            }
        }
    }
    
    private void updateTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        if (firstOperationTime == null) {
            firstOperationTime = now;
        }
        lastOperationTime = now;
    }
    
    // Getters
    
    public String getOperationName() {
        return operationName;
    }
    
    public long getTotalAttempts() {
        return totalAttempts.get();
    }
    
    public long getSuccessfulOperations() {
        return successfulOperations.get();
    }
    
    public long getFailedOperations() {
        return failedOperations.get();
    }
    
    public long getTotalOperations() {
        return getSuccessfulOperations() + getFailedOperations();
    }
    
    public double getSuccessRate() {
        long total = getTotalOperations();
        if (total == 0) {
            return 0.0;
        }
        return (double) getSuccessfulOperations() / total * 100.0;
    }
    
    public long getTotalRetryTime() {
        return totalRetryTime.get();
    }
    
    public double getAverageRetryTime() {
        long total = getTotalOperations();
        if (total == 0) {
            return 0.0;
        }
        return (double) getTotalRetryTime() / total;
    }
    
    public long getMinRetryTime() {
        return minRetryTime == Long.MAX_VALUE ? 0 : minRetryTime;
    }
    
    public long getMaxRetryTime() {
        return maxRetryTime;
    }
    
    public long getCircuitBreakerTrips() {
        return circuitBreakerTrips.get();
    }
    
    public LocalDateTime getFirstOperationTime() {
        return firstOperationTime;
    }
    
    public LocalDateTime getLastOperationTime() {
        return lastOperationTime;
    }
    
    /**
     * Get the distribution of attempts (how many operations required N attempts)
     * 
     * @return Array where index i contains count of operations that required (i+1) attempts
     */
    public long[] getAttemptDistribution() {
        long[] distribution = new long[attemptCounts.length];
        for (int i = 0; i < attemptCounts.length; i++) {
            distribution[i] = attemptCounts[i].get();
        }
        return distribution;
    }
    
    /**
     * Get the average number of attempts per operation
     * 
     * @return Average attempts per operation
     */
    public double getAverageAttempts() {
        long totalOps = getTotalOperations();
        if (totalOps == 0) {
            return 0.0;
        }
        return (double) getTotalAttempts() / totalOps;
    }
    
    /**
     * Reset all metrics for this operation
     */
    public void reset() {
        totalAttempts.set(0);
        successfulOperations.set(0);
        failedOperations.set(0);
        totalRetryTime.set(0);
        circuitBreakerTrips.set(0);
        firstOperationTime = null;
        lastOperationTime = null;
        minRetryTime = Long.MAX_VALUE;
        maxRetryTime = 0;
        
        for (AtomicLong count : attemptCounts) {
            count.set(0);
        }
    }
    
    @Override
    public String toString() {
        return "OperationMetrics{" +
                "operationName='" + operationName + '\'' +
                ", totalOperations=" + getTotalOperations() +
                ", successfulOperations=" + getSuccessfulOperations() +
                ", failedOperations=" + getFailedOperations() +
                ", successRate=" + String.format("%.2f%%", getSuccessRate()) +
                ", averageRetryTime=" + String.format("%.2fms", getAverageRetryTime()) +
                ", averageAttempts=" + String.format("%.2f", getAverageAttempts()) +
                ", circuitBreakerTrips=" + getCircuitBreakerTrips() +
                '}';
    }
}