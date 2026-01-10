package com.tbw.cut.bilibili.metrics;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Implementation of MetricsCollector for retry operations
 * 
 * This class collects and aggregates metrics for retry operations,
 * providing detailed statistics for monitoring and analysis.
 */
@Component
public class MetricsCollectorImpl implements MetricsCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollectorImpl.class);
    
    private final Map<String, OperationMetrics> operationMetrics = new ConcurrentHashMap<>();
    private final LocalDateTime collectionStartTime = LocalDateTime.now();
    private volatile LocalDateTime lastUpdateTime = LocalDateTime.now();
    
    @Override
    public void recordRetryAttempt(String operation, int attemptNumber) {
        if (operation == null || operation.trim().isEmpty()) {
            logger.warn("Invalid operation name provided for retry attempt: {}", operation);
            return;
        }
        
        OperationMetrics metrics = getOrCreateOperationMetrics(operation);
        metrics.recordAttempt(attemptNumber);
        updateLastUpdateTime();
        
        logger.debug("Recorded retry attempt {} for operation: {}", attemptNumber, operation);
    }
    
    @Override
    public void recordRetrySuccess(String operation, int totalAttempts, long totalTimeMs) {
        if (operation == null || operation.trim().isEmpty()) {
            logger.warn("Invalid operation name provided for retry success: {}", operation);
            return;
        }
        
        OperationMetrics metrics = getOrCreateOperationMetrics(operation);
        metrics.recordSuccess(totalTimeMs);
        updateLastUpdateTime();
        
        logger.info("Recorded retry success for operation: {} (attempts: {}, time: {}ms)", 
                   operation, totalAttempts, totalTimeMs);
    }
    
    @Override
    public void recordRetryFailure(String operation, int totalAttempts, String reason) {
        if (operation == null || operation.trim().isEmpty()) {
            logger.warn("Invalid operation name provided for retry failure: {}", operation);
            return;
        }
        
        OperationMetrics metrics = getOrCreateOperationMetrics(operation);
        metrics.recordFailure(0); // We don't have timing info in this method
        updateLastUpdateTime();
        
        logger.warn("Recorded retry failure for operation: {} (attempts: {}, reason: {})", 
                   operation, totalAttempts, reason);
    }
    
    @Override
    public void recordCircuitBreakerTrip(String operation) {
        if (operation == null || operation.trim().isEmpty()) {
            logger.warn("Invalid operation name provided for circuit breaker trip: {}", operation);
            return;
        }
        
        OperationMetrics metrics = getOrCreateOperationMetrics(operation);
        metrics.recordCircuitBreakerTrip();
        updateLastUpdateTime();
        
        logger.warn("Recorded circuit breaker trip for operation: {}", operation);
    }
    
    @Override
    public void recordAttemptTiming(String operation, int attemptNumber, long durationMs, boolean success) {
        if (operation == null || operation.trim().isEmpty()) {
            logger.warn("Invalid operation name provided for attempt timing: {}", operation);
            return;
        }
        
        // This is recorded as part of other methods, but we can log it for detailed analysis
        logger.debug("Attempt {} for operation {} took {}ms (success: {})", 
                    attemptNumber, operation, durationMs, success);
    }
    
    @Override
    public void recordRetryDelay(String operation, int attemptNumber, long delayMs) {
        if (operation == null || operation.trim().isEmpty()) {
            logger.warn("Invalid operation name provided for retry delay: {}", operation);
            return;
        }
        
        logger.debug("Retry delay for operation {} attempt {}: {}ms", 
                    operation, attemptNumber, delayMs);
    }
    
    @Override
    public MetricsSummary getMetricsSummary() {
        long totalOperations = 0;
        long successfulOperations = 0;
        long failedOperations = 0;
        long totalRetryAttempts = 0;
        long totalRetryTime = 0;
        long circuitBreakerTrips = 0;
        
        Map<String, OperationMetrics> operationBreakdown = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, OperationMetrics> entry : operationMetrics.entrySet()) {
            OperationMetrics metrics = entry.getValue();
            
            totalOperations += metrics.getTotalOperations();
            successfulOperations += metrics.getSuccessfulOperations();
            failedOperations += metrics.getFailedOperations();
            totalRetryAttempts += metrics.getTotalAttempts();
            totalRetryTime += metrics.getTotalRetryTime();
            circuitBreakerTrips += metrics.getCircuitBreakerTrips();
            
            operationBreakdown.put(entry.getKey(), metrics);
        }
        
        double overallSuccessRate = totalOperations > 0 ? 
            (double) successfulOperations / totalOperations * 100.0 : 0.0;
        
        double averageRetryTime = totalOperations > 0 ? 
            (double) totalRetryTime / totalOperations : 0.0;
        
        return new MetricsSummary(
            totalOperations,
            successfulOperations,
            failedOperations,
            overallSuccessRate,
            totalRetryAttempts,
            totalRetryTime,
            averageRetryTime,
            circuitBreakerTrips,
            collectionStartTime,
            lastUpdateTime,
            operationBreakdown
        );
    }
    
    @Override
    public OperationMetrics getOperationMetrics(String operation) {
        if (operation == null || operation.trim().isEmpty()) {
            return null;
        }
        
        return operationMetrics.get(operation);
    }
    
    @Override
    public void resetMetrics() {
        operationMetrics.clear();
        lastUpdateTime = LocalDateTime.now();
        logger.info("All retry metrics have been reset");
    }
    
    @Override
    public String exportMetrics(String format) {
        if (format == null) {
            format = "json";
        }
        
        switch (format.toLowerCase()) {
            case "json":
                return exportAsJson();
            case "prometheus":
                return exportAsPrometheus();
            default:
                logger.warn("Unsupported export format: {}. Using JSON.", format);
                return exportAsJson();
        }
    }
    
    private OperationMetrics getOrCreateOperationMetrics(String operation) {
        return operationMetrics.computeIfAbsent(operation, OperationMetrics::new);
    }
    
    private void updateLastUpdateTime() {
        lastUpdateTime = LocalDateTime.now();
    }
    
    private String exportAsJson() {
        try {
            MetricsSummary summary = getMetricsSummary();
            JSONObject json = new JSONObject();
            
            json.put("collectionStartTime", summary.getCollectionStartTime().toString());
            json.put("lastUpdateTime", summary.getLastUpdateTime().toString());
            json.put("totalOperations", summary.getTotalOperations());
            json.put("successfulOperations", summary.getSuccessfulOperations());
            json.put("failedOperations", summary.getFailedOperations());
            json.put("overallSuccessRate", summary.getOverallSuccessRate());
            json.put("totalRetryAttempts", summary.getTotalRetryAttempts());
            json.put("totalRetryTime", summary.getTotalRetryTime());
            json.put("averageRetryTime", summary.getAverageRetryTime());
            json.put("circuitBreakerTrips", summary.getCircuitBreakerTrips());
            
            JSONObject operations = new JSONObject();
            for (Map.Entry<String, OperationMetrics> entry : summary.getOperationBreakdown().entrySet()) {
                OperationMetrics metrics = entry.getValue();
                JSONObject opJson = new JSONObject();
                
                opJson.put("totalOperations", metrics.getTotalOperations());
                opJson.put("successfulOperations", metrics.getSuccessfulOperations());
                opJson.put("failedOperations", metrics.getFailedOperations());
                opJson.put("successRate", metrics.getSuccessRate());
                opJson.put("totalAttempts", metrics.getTotalAttempts());
                opJson.put("averageAttempts", metrics.getAverageAttempts());
                opJson.put("averageRetryTime", metrics.getAverageRetryTime());
                opJson.put("minRetryTime", metrics.getMinRetryTime());
                opJson.put("maxRetryTime", metrics.getMaxRetryTime());
                opJson.put("circuitBreakerTrips", metrics.getCircuitBreakerTrips());
                
                operations.put(entry.getKey(), opJson);
            }
            json.put("operations", operations);
            
            return json.toJSONString();
            
        } catch (Exception e) {
            logger.error("Error exporting metrics as JSON", e);
            return "{\"error\":\"Failed to export metrics\"}";
        }
    }
    
    private String exportAsPrometheus() {
        StringBuilder sb = new StringBuilder();
        
        try {
            MetricsSummary summary = getMetricsSummary();
            
            // Overall metrics
            sb.append("# HELP bilibili_retry_operations_total Total number of retry operations\n");
            sb.append("# TYPE bilibili_retry_operations_total counter\n");
            sb.append("bilibili_retry_operations_total ").append(summary.getTotalOperations()).append("\n");
            
            sb.append("# HELP bilibili_retry_success_rate Overall success rate percentage\n");
            sb.append("# TYPE bilibili_retry_success_rate gauge\n");
            sb.append("bilibili_retry_success_rate ").append(summary.getOverallSuccessRate()).append("\n");
            
            sb.append("# HELP bilibili_retry_circuit_breaker_trips_total Total circuit breaker trips\n");
            sb.append("# TYPE bilibili_retry_circuit_breaker_trips_total counter\n");
            sb.append("bilibili_retry_circuit_breaker_trips_total ").append(summary.getCircuitBreakerTrips()).append("\n");
            
            // Per-operation metrics
            for (Map.Entry<String, OperationMetrics> entry : summary.getOperationBreakdown().entrySet()) {
                String operation = entry.getKey();
                OperationMetrics metrics = entry.getValue();
                
                sb.append("# HELP bilibili_retry_operation_success_rate Success rate for operation\n");
                sb.append("# TYPE bilibili_retry_operation_success_rate gauge\n");
                sb.append("bilibili_retry_operation_success_rate{operation=\"").append(operation).append("\"} ")
                  .append(metrics.getSuccessRate()).append("\n");
                
                sb.append("# HELP bilibili_retry_operation_average_time Average retry time for operation\n");
                sb.append("# TYPE bilibili_retry_operation_average_time gauge\n");
                sb.append("bilibili_retry_operation_average_time{operation=\"").append(operation).append("\"} ")
                  .append(metrics.getAverageRetryTime()).append("\n");
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            logger.error("Error exporting metrics as Prometheus format", e);
            return "# Error exporting metrics\n";
        }
    }
}