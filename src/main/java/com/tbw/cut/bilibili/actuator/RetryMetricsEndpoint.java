package com.tbw.cut.bilibili.actuator;

import com.tbw.cut.bilibili.retry.VideoInfoRetryHandler;
import com.tbw.cut.bilibili.retry.RetryStatistics;
import com.tbw.cut.bilibili.metrics.MetricsSummary;
import com.tbw.cut.bilibili.cache.CacheStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot Actuator endpoint for retry metrics
 * 
 * This endpoint exposes retry metrics through the Actuator framework,
 * making them available for monitoring tools like Prometheus, Grafana, etc.
 */
@RestController
@RequestMapping("/actuator/retry-metrics")
public class RetryMetricsEndpoint {
    
    @Autowired
    private VideoInfoRetryHandler retryHandler;
    
    /**
     * Get all retry metrics
     */
    @GetMapping
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Retry statistics
            RetryStatistics retryStats = retryHandler.getRetryStatistics();
            Map<String, Object> retryMetrics = new HashMap<>();
            retryMetrics.put("totalAttempts", retryStats.getTotalAttempts());
            retryMetrics.put("successfulOperations", retryStats.getSuccessfulOperations());
            retryMetrics.put("failedOperations", retryStats.getFailedOperations());
            retryMetrics.put("successRate", retryStats.getSuccessRate());
            retryMetrics.put("averageRetryTime", retryStats.getAverageRetryTime());
            retryMetrics.put("minRetryTime", retryStats.getMinRetryTime());
            retryMetrics.put("maxRetryTime", retryStats.getMaxRetryTime());
            retryMetrics.put("circuitBreakerTrips", retryStats.getCircuitBreakerTrips());
            retryMetrics.put("lastOperationTime", retryStats.getLastOperationTime());
            
            metrics.put("retry", retryMetrics);
            
            // Metrics collector summary
            MetricsSummary summary = retryHandler.getMetricsCollector().getMetricsSummary();
            Map<String, Object> collectorMetrics = new HashMap<>();
            collectorMetrics.put("totalOperations", summary.getTotalOperations());
            collectorMetrics.put("successfulOperations", summary.getSuccessfulOperations());
            collectorMetrics.put("failedOperations", summary.getFailedOperations());
            collectorMetrics.put("overallSuccessRate", summary.getOverallSuccessRate());
            collectorMetrics.put("totalRetryAttempts", summary.getTotalRetryAttempts());
            collectorMetrics.put("totalRetryTime", summary.getTotalRetryTime());
            collectorMetrics.put("averageRetryTime", summary.getAverageRetryTime());
            collectorMetrics.put("circuitBreakerTrips", summary.getCircuitBreakerTrips());
            collectorMetrics.put("collectionStartTime", summary.getCollectionStartTime());
            collectorMetrics.put("lastUpdateTime", summary.getLastUpdateTime());
            
            metrics.put("collector", collectorMetrics);
            
            // Cache statistics
            CacheStatistics cacheStats = retryHandler.getCache().getStatistics();
            Map<String, Object> cacheMetrics = new HashMap<>();
            cacheMetrics.put("hitCount", cacheStats.getHitCount());
            cacheMetrics.put("missCount", cacheStats.getMissCount());
            cacheMetrics.put("evictionCount", cacheStats.getEvictionCount());
            cacheMetrics.put("totalRequests", cacheStats.getTotalRequests());
            cacheMetrics.put("hitRate", cacheStats.getHitRate());
            cacheMetrics.put("size", retryHandler.getCache().size());
            cacheMetrics.put("creationTime", cacheStats.getCreationTime());
            cacheMetrics.put("lastAccessTime", cacheStats.getLastAccessTime());
            
            metrics.put("cache", cacheMetrics);
            
            // Circuit breaker status
            Map<String, Object> circuitBreakerMetrics = new HashMap<>();
            circuitBreakerMetrics.put("isOpen", retryHandler.isCircuitBreakerOpen());
            
            metrics.put("circuitBreaker", circuitBreakerMetrics);
            
            // System status
            Map<String, Object> systemMetrics = new HashMap<>();
            systemMetrics.put("status", "UP");
            systemMetrics.put("timestamp", System.currentTimeMillis());
            
            metrics.put("system", systemMetrics);
            
        } catch (Exception e) {
            Map<String, Object> errorMetrics = new HashMap<>();
            errorMetrics.put("status", "ERROR");
            errorMetrics.put("error", e.getMessage());
            errorMetrics.put("timestamp", System.currentTimeMillis());
            
            metrics.put("system", errorMetrics);
        }
        
        return metrics;
    }
    
    /**
     * Reset all metrics
     */
    @PostMapping("/reset")
    public Map<String, Object> resetMetrics() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            retryHandler.resetStatistics();
            retryHandler.getMetricsCollector().resetMetrics();
            
            result.put("status", "SUCCESS");
            result.put("message", "All metrics reset successfully");
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "Failed to reset metrics: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
        }
        
        return result;
    }
}