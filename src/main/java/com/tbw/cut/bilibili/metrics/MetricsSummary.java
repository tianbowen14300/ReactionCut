package com.tbw.cut.bilibili.metrics;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Summary of retry metrics across all operations
 * 
 * This class provides a high-level overview of retry performance
 * suitable for monitoring dashboards and alerts.
 */
public class MetricsSummary {
    
    private final long totalOperations;
    private final long successfulOperations;
    private final long failedOperations;
    private final double overallSuccessRate;
    private final long totalRetryAttempts;
    private final long totalRetryTime;
    private final double averageRetryTime;
    private final long circuitBreakerTrips;
    private final LocalDateTime collectionStartTime;
    private final LocalDateTime lastUpdateTime;
    private final Map<String, OperationMetrics> operationBreakdown;
    
    public MetricsSummary(long totalOperations,
                         long successfulOperations,
                         long failedOperations,
                         double overallSuccessRate,
                         long totalRetryAttempts,
                         long totalRetryTime,
                         double averageRetryTime,
                         long circuitBreakerTrips,
                         LocalDateTime collectionStartTime,
                         LocalDateTime lastUpdateTime,
                         Map<String, OperationMetrics> operationBreakdown) {
        this.totalOperations = totalOperations;
        this.successfulOperations = successfulOperations;
        this.failedOperations = failedOperations;
        this.overallSuccessRate = overallSuccessRate;
        this.totalRetryAttempts = totalRetryAttempts;
        this.totalRetryTime = totalRetryTime;
        this.averageRetryTime = averageRetryTime;
        this.circuitBreakerTrips = circuitBreakerTrips;
        this.collectionStartTime = collectionStartTime;
        this.lastUpdateTime = lastUpdateTime;
        this.operationBreakdown = operationBreakdown;
    }
    
    public long getTotalOperations() {
        return totalOperations;
    }
    
    public long getSuccessfulOperations() {
        return successfulOperations;
    }
    
    public long getFailedOperations() {
        return failedOperations;
    }
    
    public double getOverallSuccessRate() {
        return overallSuccessRate;
    }
    
    public long getTotalRetryAttempts() {
        return totalRetryAttempts;
    }
    
    public long getTotalRetryTime() {
        return totalRetryTime;
    }
    
    public double getAverageRetryTime() {
        return averageRetryTime;
    }
    
    public long getCircuitBreakerTrips() {
        return circuitBreakerTrips;
    }
    
    public LocalDateTime getCollectionStartTime() {
        return collectionStartTime;
    }
    
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public Map<String, OperationMetrics> getOperationBreakdown() {
        return operationBreakdown;
    }
    
    /**
     * Check if the system is performing well based on success rate
     * 
     * @param threshold Success rate threshold (0.0 to 1.0)
     * @return true if success rate is above threshold
     */
    public boolean isHealthy(double threshold) {
        return overallSuccessRate >= (threshold * 100.0);
    }
    
    /**
     * Get the failure rate as a percentage
     * 
     * @return Failure rate (0.0 to 100.0)
     */
    public double getFailureRate() {
        return 100.0 - overallSuccessRate;
    }
    
    /**
     * Check if circuit breaker activity is concerning
     * 
     * @param maxTrips Maximum acceptable circuit breaker trips
     * @return true if trips exceed threshold
     */
    public boolean hasExcessiveCircuitBreakerActivity(long maxTrips) {
        return circuitBreakerTrips > maxTrips;
    }
    
    @Override
    public String toString() {
        return "MetricsSummary{" +
                "totalOperations=" + totalOperations +
                ", successfulOperations=" + successfulOperations +
                ", failedOperations=" + failedOperations +
                ", overallSuccessRate=" + String.format("%.2f%%", overallSuccessRate) +
                ", totalRetryAttempts=" + totalRetryAttempts +
                ", averageRetryTime=" + String.format("%.2fms", averageRetryTime) +
                ", circuitBreakerTrips=" + circuitBreakerTrips +
                ", collectionStartTime=" + collectionStartTime +
                ", lastUpdateTime=" + lastUpdateTime +
                '}';
    }
}