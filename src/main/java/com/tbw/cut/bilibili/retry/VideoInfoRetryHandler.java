package com.tbw.cut.bilibili.retry;

import com.alibaba.fastjson.JSONObject;

/**
 * Video information retrieval retry handler interface
 * 
 * This interface defines the contract for handling video information retrieval
 * with retry logic, exponential backoff, and error classification.
 */
public interface VideoInfoRetryHandler {
    
    /**
     * Retrieve video AID with retry logic for 404 errors
     * 
     * @param bvid The video BVID (Bilibili Video ID)
     * @return AID (Archive ID) or null if all retries failed
     */
    Long getAidWithRetry(String bvid);
    
    /**
     * Retrieve full video information with retry logic
     * 
     * @param aid The video AID (Archive ID)
     * @return Video info JSON object or null if all retries failed
     */
    JSONObject getVideoInfoWithRetry(Long aid);
    
    /**
     * Check if an error is retryable (typically 404 errors indicating video not yet available)
     * 
     * @param error The error message or exception details
     * @return true if the error should trigger a retry, false otherwise
     */
    boolean isRetryableError(String error);
    
    /**
     * Check if an exception is retryable
     * 
     * @param exception The exception that occurred
     * @return true if the exception should trigger a retry, false otherwise
     */
    boolean isRetryableError(Exception exception);
    
    /**
     * Get the current retry statistics for monitoring
     * 
     * @return Retry statistics object containing success rates, timing, etc.
     */
    RetryStatistics getRetryStatistics();
    
    /**
     * Reset retry statistics (useful for testing or periodic cleanup)
     */
    void resetStatistics();
    
    /**
     * Check if the circuit breaker is currently open (preventing retries)
     * 
     * @return true if circuit breaker is open, false otherwise
     */
    boolean isCircuitBreakerOpen();
    
    /**
     * Manually reset the circuit breaker (for administrative purposes)
     */
    void resetCircuitBreaker();
    
    /**
     * Get the metrics collector for external monitoring integration
     * 
     * @return The metrics collector instance
     */
    com.tbw.cut.bilibili.metrics.MetricsCollector getMetricsCollector();
    
    /**
     * Set the AID retriever function for dependency injection
     * 
     * @param aidRetriever Function to retrieve AID from BVID
     */
    void setAidRetriever(java.util.function.Function<String, Long> aidRetriever);
    
    /**
     * Set the video info retriever function for dependency injection
     * 
     * @param videoInfoRetriever Function to retrieve video info from AID
     */
    void setVideoInfoRetriever(java.util.function.Function<Long, com.alibaba.fastjson.JSONObject> videoInfoRetriever);
    
    /**
     * Get the video info cache for monitoring and management
     * 
     * @return The video info cache instance
     */
    com.tbw.cut.bilibili.cache.VideoInfoCache getCache();
}