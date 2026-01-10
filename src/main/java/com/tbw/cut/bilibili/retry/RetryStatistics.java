package com.tbw.cut.bilibili.retry;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for retry operations
 * 
 * This class tracks various metrics related to retry operations
 * for monitoring and performance analysis.
 */
public class RetryStatistics {
    
    private final AtomicLong totalAttempts = new AtomicLong(0);
    private final AtomicLong successfulRetries = new AtomicLong(0);
    private final AtomicLong failedRetries = new AtomicLong(0);
    private final AtomicLong circuitBreakerTrips = new AtomicLong(0);
    private final AtomicLong totalRetryTime = new AtomicLong(0);
    private volatile LocalDateTime lastResetTime = LocalDateTime.now();
    private volatile LocalDateTime lastSuccessTime;
    private volatile LocalDateTime lastFailureTime;
    
    /**
     * Record a retry attempt
     */
    public void recordAttempt() {
        totalAttempts.incrementAndGet();
    }
    
    /**
     * Record a successful retry
     * 
     * @param retryTimeMs Total time spent on retries in milliseconds
     */
    public void recordSuccess(long retryTimeMs) {
        successfulRetries.incrementAndGet();
        totalRetryTime.addAndGet(retryTimeMs);
        lastSuccessTime = LocalDateTime.now();
    }
    
    /**
     * Record a failed retry (after all attempts exhausted)
     * 
     * @param retryTimeMs Total time spent on retries in milliseconds
     */
    public void recordFailure(long retryTimeMs) {
        failedRetries.incrementAndGet();
        totalRetryTime.addAndGet(retryTimeMs);
        lastFailureTime = LocalDateTime.now();
    }
    
    /**
     * Record a circuit breaker trip
     */
    public void recordCircuitBreakerTrip() {
        circuitBreakerTrips.incrementAndGet();
    }
    
    /**
     * Get total number of retry attempts
     * 
     * @return Total attempts
     */
    public long getTotalAttempts() {
        return totalAttempts.get();
    }
    
    /**
     * Get number of successful retries
     * 
     * @return Successful retries
     */
    public long getSuccessfulRetries() {
        return successfulRetries.get();
    }
    
    /**
     * Get number of failed retries
     * 
     * @return Failed retries
     */
    public long getFailedRetries() {
        return failedRetries.get();
    }
    
    /**
     * Get number of circuit breaker trips
     * 
     * @return Circuit breaker trips
     */
    public long getCircuitBreakerTrips() {
        return circuitBreakerTrips.get();
    }
    
    /**
     * Get total time spent on retries
     * 
     * @return Total retry time in milliseconds
     */
    public long getTotalRetryTime() {
        return totalRetryTime.get();
    }
    
    /**
     * Calculate success rate as a percentage
     * 
     * @return Success rate (0.0 to 100.0)
     */
    public double getSuccessRate() {
        long total = getTotalAttempts();
        if (total == 0) {
            return 0.0;
        }
        return (double) getSuccessfulRetries() / total * 100.0;
    }
    
    /**
     * Calculate average retry time per attempt
     * 
     * @return Average retry time in milliseconds
     */
    public double getAverageRetryTime() {
        long total = getTotalAttempts();
        if (total == 0) {
            return 0.0;
        }
        return (double) getTotalRetryTime() / total;
    }
    
    /**
     * Get the time when statistics were last reset
     * 
     * @return Last reset time
     */
    public LocalDateTime getLastResetTime() {
        return lastResetTime;
    }
    
    /**
     * Get the time of the last successful retry
     * 
     * @return Last success time, or null if no successes recorded
     */
    public LocalDateTime getLastSuccessTime() {
        return lastSuccessTime;
    }
    
    /**
     * Get the time of the last failed retry
     * 
     * @return Last failure time, or null if no failures recorded
     */
    public LocalDateTime getLastFailureTime() {
        return lastFailureTime;
    }
    
    /**
     * Get number of successful operations (alias for successful retries)
     * 
     * @return Successful operations count
     */
    public long getSuccessfulOperations() {
        return getSuccessfulRetries();
    }
    
    /**
     * Get number of failed operations (alias for failed retries)
     * 
     * @return Failed operations count
     */
    public long getFailedOperations() {
        return getFailedRetries();
    }
    
    /**
     * Get minimum retry time (placeholder - returns 0 for now)
     * 
     * @return Minimum retry time in milliseconds
     */
    public long getMinRetryTime() {
        // TODO: Implement actual min retry time tracking
        return 0L;
    }
    
    /**
     * Get maximum retry time (placeholder - returns total time for now)
     * 
     * @return Maximum retry time in milliseconds
     */
    public long getMaxRetryTime() {
        // TODO: Implement actual max retry time tracking
        return getTotalRetryTime();
    }
    
    /**
     * Get last operation time (returns last success or failure time)
     * 
     * @return Last operation time, or null if no operations recorded
     */
    public LocalDateTime getLastOperationTime() {
        if (lastSuccessTime == null && lastFailureTime == null) {
            return null;
        }
        if (lastSuccessTime == null) {
            return lastFailureTime;
        }
        if (lastFailureTime == null) {
            return lastSuccessTime;
        }
        return lastSuccessTime.isAfter(lastFailureTime) ? lastSuccessTime : lastFailureTime;
    }
    
    /**
     * Reset all statistics
     */
    public void reset() {
        totalAttempts.set(0);
        successfulRetries.set(0);
        failedRetries.set(0);
        circuitBreakerTrips.set(0);
        totalRetryTime.set(0);
        lastResetTime = LocalDateTime.now();
        lastSuccessTime = null;
        lastFailureTime = null;
    }
    
    @Override
    public String toString() {
        return "RetryStatistics{" +
                "totalAttempts=" + getTotalAttempts() +
                ", successfulRetries=" + getSuccessfulRetries() +
                ", failedRetries=" + getFailedRetries() +
                ", circuitBreakerTrips=" + getCircuitBreakerTrips() +
                ", successRate=" + String.format("%.2f%%", getSuccessRate()) +
                ", averageRetryTime=" + String.format("%.2fms", getAverageRetryTime()) +
                ", lastResetTime=" + lastResetTime +
                '}';
    }
}