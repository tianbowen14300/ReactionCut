package com.tbw.cut.bilibili.metrics;

/**
 * Interface for collecting and reporting retry metrics
 * 
 * This interface provides methods to record various retry-related metrics
 * for monitoring and performance analysis.
 */
public interface MetricsCollector {
    
    /**
     * Record a retry attempt for a specific operation
     * 
     * @param operation The operation being retried (e.g., "getAidFromBvid", "getVideoInfo")
     * @param attemptNumber The current attempt number (1-based)
     */
    void recordRetryAttempt(String operation, int attemptNumber);
    
    /**
     * Record a successful retry operation
     * 
     * @param operation The operation that succeeded
     * @param totalAttempts Total number of attempts made
     * @param totalTimeMs Total time spent on all attempts in milliseconds
     */
    void recordRetrySuccess(String operation, int totalAttempts, long totalTimeMs);
    
    /**
     * Record a failed retry operation (after all attempts exhausted)
     * 
     * @param operation The operation that failed
     * @param totalAttempts Total number of attempts made
     * @param reason The reason for failure
     */
    void recordRetryFailure(String operation, int totalAttempts, String reason);
    
    /**
     * Record a circuit breaker trip event
     * 
     * @param operation The operation that caused the circuit breaker to trip
     */
    void recordCircuitBreakerTrip(String operation);
    
    /**
     * Record timing information for an individual retry attempt
     * 
     * @param operation The operation being timed
     * @param attemptNumber The attempt number
     * @param durationMs Duration of this specific attempt in milliseconds
     * @param success Whether this attempt was successful
     */
    void recordAttemptTiming(String operation, int attemptNumber, long durationMs, boolean success);
    
    /**
     * Record delay information between retry attempts
     * 
     * @param operation The operation being retried
     * @param attemptNumber The attempt number that will follow this delay
     * @param delayMs The delay duration in milliseconds
     */
    void recordRetryDelay(String operation, int attemptNumber, long delayMs);
    
    /**
     * Get current metrics summary for monitoring dashboards
     * 
     * @return Metrics summary object
     */
    MetricsSummary getMetricsSummary();
    
    /**
     * Get detailed metrics for a specific operation
     * 
     * @param operation The operation to get metrics for
     * @return Detailed metrics for the operation, or null if no data available
     */
    OperationMetrics getOperationMetrics(String operation);
    
    /**
     * Reset all collected metrics (useful for testing or periodic cleanup)
     */
    void resetMetrics();
    
    /**
     * Export metrics in a format suitable for external monitoring systems
     * 
     * @param format The export format (e.g., "prometheus", "json")
     * @return Formatted metrics string
     */
    String exportMetrics(String format);
}