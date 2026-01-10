package com.tbw.cut.controller;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.retry.VideoInfoRetryHandler;
import com.tbw.cut.bilibili.retry.RetryStatistics;
import com.tbw.cut.bilibili.metrics.MetricsCollector;
import com.tbw.cut.bilibili.metrics.MetricsSummary;
import com.tbw.cut.bilibili.cache.VideoInfoCache;
import com.tbw.cut.bilibili.cache.CacheStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for exposing retry metrics and monitoring endpoints
 * 
 * This controller provides REST endpoints for monitoring the performance
 * and health of the video info retry system.
 */
@RestController
@RequestMapping("/api/retry-metrics")
public class RetryMetricsController {
    
    @Autowired
    private VideoInfoRetryHandler retryHandler;
    
    /**
     * Get comprehensive retry metrics summary
     */
    @GetMapping("/summary")
    public ResponseEntity<MetricsSummary> getMetricsSummary() {
        try {
            MetricsCollector metricsCollector = retryHandler.getMetricsCollector();
            MetricsSummary summary = metricsCollector.getMetricsSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get retry statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<JSONObject> getRetryStatistics() {
        try {
            RetryStatistics statistics = retryHandler.getRetryStatistics();
            
            JSONObject response = new JSONObject();
            response.put("totalAttempts", statistics.getTotalAttempts());
            response.put("successfulOperations", statistics.getSuccessfulOperations());
            response.put("failedOperations", statistics.getFailedOperations());
            response.put("successRate", statistics.getSuccessRate());
            response.put("averageRetryTime", statistics.getAverageRetryTime());
            response.put("minRetryTime", statistics.getMinRetryTime());
            response.put("maxRetryTime", statistics.getMaxRetryTime());
            response.put("circuitBreakerTrips", statistics.getCircuitBreakerTrips());
            response.put("lastOperationTime", statistics.getLastOperationTime());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get cache statistics
     */
    @GetMapping("/cache")
    public ResponseEntity<CacheStatistics> getCacheStatistics() {
        try {
            VideoInfoCache cache = retryHandler.getCache();
            CacheStatistics stats = cache.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get circuit breaker status
     */
    @GetMapping("/circuit-breaker")
    public ResponseEntity<JSONObject> getCircuitBreakerStatus() {
        try {
            JSONObject status = new JSONObject();
            status.put("isOpen", retryHandler.isCircuitBreakerOpen());
            status.put("statistics", retryHandler.getRetryStatistics());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Export metrics in different formats
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportMetrics(@RequestParam(defaultValue = "json") String format) {
        try {
            MetricsCollector metricsCollector = retryHandler.getMetricsCollector();
            String exportedMetrics = metricsCollector.exportMetrics(format);
            
            String contentType = "json".equalsIgnoreCase(format) ? 
                "application/json" : "text/plain";
            
            return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .body(exportedMetrics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Reset retry statistics (for administrative purposes)
     */
    @PostMapping("/reset")
    public ResponseEntity<JSONObject> resetStatistics() {
        try {
            retryHandler.resetStatistics();
            retryHandler.getMetricsCollector().resetMetrics();
            
            JSONObject response = new JSONObject();
            response.put("message", "Statistics reset successfully");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("error", "Failed to reset statistics");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Reset circuit breaker (for administrative purposes)
     */
    @PostMapping("/circuit-breaker/reset")
    public ResponseEntity<JSONObject> resetCircuitBreaker() {
        try {
            retryHandler.resetCircuitBreaker();
            
            JSONObject response = new JSONObject();
            response.put("message", "Circuit breaker reset successfully");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("error", "Failed to reset circuit breaker");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Clear cache (for administrative purposes)
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<JSONObject> clearCache() {
        try {
            VideoInfoCache cache = retryHandler.getCache();
            int sizeBefore = cache.size();
            cache.clearAll();
            
            JSONObject response = new JSONObject();
            response.put("message", "Cache cleared successfully");
            response.put("entriesCleared", sizeBefore);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("error", "Failed to clear cache");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<JSONObject> healthCheck() {
        try {
            JSONObject health = new JSONObject();
            health.put("status", "UP");
            health.put("circuitBreakerOpen", retryHandler.isCircuitBreakerOpen());
            health.put("cacheSize", retryHandler.getCache().size());
            health.put("timestamp", System.currentTimeMillis());
            
            // Add basic statistics
            RetryStatistics stats = retryHandler.getRetryStatistics();
            health.put("totalOperations", stats.getTotalAttempts());
            health.put("successRate", stats.getSuccessRate());
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("status", "DOWN");
            error.put("error", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(503).body(error);
        }
    }
}