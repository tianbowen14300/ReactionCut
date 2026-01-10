package com.tbw.cut.bilibili.retry;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.config.RetryConfiguration;
import com.tbw.cut.bilibili.metrics.MetricsCollector;
import com.tbw.cut.bilibili.cache.VideoInfoCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Implementation of VideoInfoRetryHandler with exponential backoff and circuit breaker
 * 
 * This class provides robust retry logic for video information retrieval,
 * handling timing issues when videos are not immediately available after submission.
 */
@Service
public class VideoInfoRetryHandlerImpl implements VideoInfoRetryHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoInfoRetryHandlerImpl.class);
    
    private final RetryConfiguration config;
    private final ExponentialBackoffCalculator backoffCalculator;
    private final RetryStatistics statistics;
    private final MetricsCollector metricsCollector;
    private final VideoInfoCache cache;
    private final ScheduledExecutorService retryExecutor;
    
    // Circuit breaker state
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile LocalDateTime circuitBreakerOpenTime;
    private volatile boolean circuitBreakerOpen = false;
    
    // Function interfaces for external service calls
    private Function<String, Long> aidRetriever;
    private Function<Long, JSONObject> videoInfoRetriever;
    
    @Autowired
    public VideoInfoRetryHandlerImpl(RetryConfiguration config,
                                   ExponentialBackoffCalculator backoffCalculator,
                                   MetricsCollector metricsCollector,
                                   VideoInfoCache cache) {
        this.config = config;
        this.backoffCalculator = backoffCalculator;
        this.metricsCollector = metricsCollector;
        this.cache = cache;
        this.statistics = new RetryStatistics();
        this.retryExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "video-info-retry-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Set the AID retriever function (to be called by service that uses this handler)
     */
    public void setAidRetriever(Function<String, Long> aidRetriever) {
        this.aidRetriever = aidRetriever;
    }
    
    /**
     * Set the video info retriever function (to be called by service that uses this handler)
     */
    public void setVideoInfoRetriever(Function<Long, JSONObject> videoInfoRetriever) {
        this.videoInfoRetriever = videoInfoRetriever;
    }
    
    @Override
    public Long getAidWithRetry(String bvid) {
        if (bvid == null || bvid.trim().isEmpty()) {
            logger.warn("Invalid BVID provided: {}", bvid);
            return null;
        }
        
        // Check cache first
        Long cachedAid = cache.getAid(bvid);
        if (cachedAid != null) {
            logger.debug("Cache hit for AID retrieval, BVID: {}, AID: {}", bvid, cachedAid);
            return cachedAid;
        }
        
        if (isCircuitBreakerOpen()) {
            logger.warn("Circuit breaker is open, skipping retry for BVID: {}", bvid);
            return null;
        }
        
        logger.info("Starting AID retrieval with retry for BVID: {}", bvid);
        long startTime = System.currentTimeMillis();
        String operation = "getAidWithRetry";
        
        for (int attempt = 1; attempt <= config.getMaxAttempts(); attempt++) {
            statistics.recordAttempt();
            metricsCollector.recordRetryAttempt(operation, attempt);
            
            long attemptStartTime = System.currentTimeMillis();
            
            try {
                logger.debug("Attempt {} of {} for BVID: {}", attempt, config.getMaxAttempts(), bvid);
                
                Long aid = aidRetriever != null ? aidRetriever.apply(bvid) : null;
                
                if (aid != null) {
                    long totalTime = System.currentTimeMillis() - startTime;
                    long attemptTime = System.currentTimeMillis() - attemptStartTime;
                    
                    logger.info("Successfully retrieved AID {} for BVID {} after {} attempts in {}ms", 
                              aid, bvid, attempt, totalTime);
                    
                    // Cache the successful result
                    cache.putAid(bvid, aid);
                    
                    statistics.recordSuccess(totalTime);
                    metricsCollector.recordRetrySuccess(operation, attempt, totalTime);
                    metricsCollector.recordAttemptTiming(operation, attempt, attemptTime, true);
                    resetCircuitBreakerOnSuccess();
                    return aid;
                }
                
                // If we get null, it might be a 404 - check if we should retry
                if (attempt < config.getMaxAttempts()) {
                    long delay = backoffCalculator.calculateDelay(attempt, config);
                    long attemptTime = System.currentTimeMillis() - attemptStartTime;
                    
                    logger.info("AID not found for BVID {}, attempt {} failed. Retrying in {}ms", 
                              bvid, attempt, delay);
                    
                    metricsCollector.recordAttemptTiming(operation, attempt, attemptTime, false);
                    metricsCollector.recordRetryDelay(operation, attempt, delay);
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Retry interrupted for BVID: {}", bvid);
                        break;
                    }
                } else {
                    long attemptTime = System.currentTimeMillis() - attemptStartTime;
                    metricsCollector.recordAttemptTiming(operation, attempt, attemptTime, false);
                    logger.warn("Max retry attempts ({}) reached for BVID: {}", config.getMaxAttempts(), bvid);
                }
                
            } catch (Exception e) {
                long attemptTime = System.currentTimeMillis() - attemptStartTime;
                logger.error("Error during AID retrieval attempt {} for BVID {}: {}", 
                           attempt, bvid, e.getMessage());
                
                metricsCollector.recordAttemptTiming(operation, attempt, attemptTime, false);
                
                if (!isRetryableError(e)) {
                    logger.info("Non-retryable error encountered for BVID {}: {}", bvid, e.getMessage());
                    long totalTime = System.currentTimeMillis() - startTime;
                    statistics.recordFailure(totalTime);
                    metricsCollector.recordRetryFailure(operation, attempt, "Non-retryable error: " + e.getMessage());
                    recordFailureForCircuitBreaker();
                    return null;
                }
                
                if (attempt < config.getMaxAttempts()) {
                    long delay = backoffCalculator.calculateDelay(attempt, config);
                    logger.info("Retryable error for BVID {}, attempt {} failed. Retrying in {}ms", 
                              bvid, attempt, delay);
                    
                    metricsCollector.recordRetryDelay(operation, attempt, delay);
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.warn("Retry interrupted for BVID: {}", bvid);
                        break;
                    }
                }
            }
        }
        
        // All retries exhausted
        long totalTime = System.currentTimeMillis() - startTime;
        logger.error("Failed to retrieve AID for BVID {} after {} attempts in {}ms", 
                   bvid, config.getMaxAttempts(), totalTime);
        
        statistics.recordFailure(totalTime);
        metricsCollector.recordRetryFailure(operation, config.getMaxAttempts(), "Max retries exhausted");
        recordFailureForCircuitBreaker();
        return null;
    }
    
    @Override
    public JSONObject getVideoInfoWithRetry(Long aid) {
        if (aid == null) {
            logger.warn("Invalid AID provided: null");
            return null;
        }
        
        // Check cache first
        JSONObject cachedVideoInfo = cache.getVideoInfo(aid);
        if (cachedVideoInfo != null) {
            logger.debug("Cache hit for video info retrieval, AID: {}", aid);
            return cachedVideoInfo;
        }
        
        if (isCircuitBreakerOpen()) {
            logger.warn("Circuit breaker is open, skipping retry for AID: {}", aid);
            return null;
        }
        
        logger.info("Starting video info retrieval with retry for AID: {}", aid);
        long startTime = System.currentTimeMillis();
        String operation = "getVideoInfoWithRetry";
        
        for (int attempt = 1; attempt <= config.getMaxAttempts(); attempt++) {
            statistics.recordAttempt();
            metricsCollector.recordRetryAttempt(operation, attempt);
            
            long attemptStartTime = System.currentTimeMillis();
            
            try {
                logger.debug("Attempt {} of {} for AID: {}", attempt, config.getMaxAttempts(), aid);
                
                // Use the video info retriever function if available
                JSONObject videoInfo = videoInfoRetriever != null ? videoInfoRetriever.apply(aid) : null;
                
                if (videoInfo != null) {
                    long totalTime = System.currentTimeMillis() - startTime;
                    long attemptTime = System.currentTimeMillis() - attemptStartTime;
                    
                    logger.info("Successfully retrieved video info for AID {} after {} attempts in {}ms", 
                              aid, attempt, totalTime);
                    
                    // Cache the successful result
                    cache.putVideoInfo(aid, videoInfo);
                    
                    statistics.recordSuccess(totalTime);
                    metricsCollector.recordRetrySuccess(operation, attempt, totalTime);
                    metricsCollector.recordAttemptTiming(operation, attempt, attemptTime, true);
                    resetCircuitBreakerOnSuccess();
                    return videoInfo;
                }
                
                if (attempt < config.getMaxAttempts()) {
                    long delay = backoffCalculator.calculateDelay(attempt, config);
                    long attemptTime = System.currentTimeMillis() - attemptStartTime;
                    
                    logger.info("Video info not found for AID {}, attempt {} failed. Retrying in {}ms", 
                              aid, attempt, delay);
                    
                    metricsCollector.recordAttemptTiming(operation, attempt, attemptTime, false);
                    metricsCollector.recordRetryDelay(operation, attempt, delay);
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Retry interrupted for AID: {}", aid);
                        break;
                    }
                } else {
                    long attemptTime = System.currentTimeMillis() - attemptStartTime;
                    metricsCollector.recordAttemptTiming(operation, attempt, attemptTime, false);
                }
                
            } catch (Exception e) {
                long attemptTime = System.currentTimeMillis() - attemptStartTime;
                logger.error("Error during video info retrieval attempt {} for AID {}: {}", 
                           attempt, aid, e.getMessage());
                
                metricsCollector.recordAttemptTiming(operation, attempt, attemptTime, false);
                
                if (!isRetryableError(e)) {
                    logger.info("Non-retryable error encountered for AID {}: {}", aid, e.getMessage());
                    long totalTime = System.currentTimeMillis() - startTime;
                    statistics.recordFailure(totalTime);
                    metricsCollector.recordRetryFailure(operation, attempt, "Non-retryable error: " + e.getMessage());
                    recordFailureForCircuitBreaker();
                    return null;
                }
                
                if (attempt < config.getMaxAttempts()) {
                    long delay = backoffCalculator.calculateDelay(attempt, config);
                    logger.info("Retryable error for AID {}, attempt {} failed. Retrying in {}ms", 
                              aid, attempt, delay);
                    
                    metricsCollector.recordRetryDelay(operation, attempt, delay);
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.warn("Retry interrupted for AID: {}", aid);
                        break;
                    }
                }
            }
        }
        
        // All retries exhausted
        long totalTime = System.currentTimeMillis() - startTime;
        logger.error("Failed to retrieve video info for AID {} after {} attempts in {}ms", 
                   aid, config.getMaxAttempts(), totalTime);
        
        statistics.recordFailure(totalTime);
        metricsCollector.recordRetryFailure(operation, config.getMaxAttempts(), "Max retries exhausted");
        recordFailureForCircuitBreaker();
        return null;
    }
    
    @Override
    public boolean isRetryableError(String error) {
        if (error == null) {
            return false;
        }
        
        String lowerError = error.toLowerCase();
        
        // Check for 404-like errors that indicate video not yet available
        return lowerError.contains("404") ||
               lowerError.contains("not found") ||
               lowerError.contains("啥都木有") ||
               lowerError.contains("视频不存在") ||
               lowerError.contains("video not found");
    }
    
    @Override
    public boolean isRetryableError(Exception exception) {
        if (exception == null) {
            return false;
        }
        
        String message = exception.getMessage();
        if (message != null && isRetryableError(message)) {
            return true;
        }
        
        // Check exception type for specific retryable exceptions
        String className = exception.getClass().getSimpleName().toLowerCase();
        
        // Don't retry on authentication or permission errors
        if (className.contains("auth") || className.contains("permission") || 
            className.contains("forbidden") || className.contains("unauthorized")) {
            return false;
        }
        
        // Don't retry on client errors (4xx except 404)
        if (className.contains("client") && !className.contains("notfound")) {
            return false;
        }
        
        // Retry on network-related issues that might be temporary
        return className.contains("timeout") ||
               className.contains("connection") ||
               className.contains("socket") ||
               className.contains("network") ||
               className.contains("io");
    }
    
    @Override
    public RetryStatistics getRetryStatistics() {
        return statistics;
    }
    
    @Override
    public void resetStatistics() {
        statistics.reset();
        logger.info("Retry statistics have been reset");
    }
    
    @Override
    public boolean isCircuitBreakerOpen() {
        if (!circuitBreakerOpen) {
            return false;
        }
        
        // Check if circuit breaker timeout has elapsed
        if (circuitBreakerOpenTime != null) {
            LocalDateTime now = LocalDateTime.now();
            long elapsedMs = java.time.Duration.between(circuitBreakerOpenTime, now).toMillis();
            
            if (elapsedMs >= config.getCircuitBreakerTimeoutMs()) {
                logger.info("Circuit breaker timeout elapsed, transitioning to half-open state");
                circuitBreakerOpen = false;
                circuitBreakerOpenTime = null;
                consecutiveFailures.set(0);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public void resetCircuitBreaker() {
        circuitBreakerOpen = false;
        circuitBreakerOpenTime = null;
        consecutiveFailures.set(0);
        logger.info("Circuit breaker has been manually reset");
    }
    
    @Override
    public com.tbw.cut.bilibili.metrics.MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }
    
    /**
     * Get the video info cache for external monitoring
     * 
     * @return The cache instance
     */
    public VideoInfoCache getCache() {
        return cache;
    }
    
    /**
     * Record a failure for circuit breaker logic
     */
    private void recordFailureForCircuitBreaker() {
        int failures = consecutiveFailures.incrementAndGet();
        
        if (failures >= config.getCircuitBreakerThreshold() && !circuitBreakerOpen) {
            circuitBreakerOpen = true;
            circuitBreakerOpenTime = LocalDateTime.now();
            statistics.recordCircuitBreakerTrip();
            metricsCollector.recordCircuitBreakerTrip("video-info-retry");
            
            logger.warn("Circuit breaker tripped after {} consecutive failures. " +
                       "Will remain open for {}ms", failures, config.getCircuitBreakerTimeoutMs());
        }
    }
    
    /**
     * Reset circuit breaker state on successful operation
     */
    private void resetCircuitBreakerOnSuccess() {
        if (consecutiveFailures.get() > 0) {
            logger.debug("Resetting consecutive failure count after successful operation");
            consecutiveFailures.set(0);
        }
        
        if (circuitBreakerOpen) {
            logger.info("Circuit breaker reset after successful operation");
            circuitBreakerOpen = false;
            circuitBreakerOpenTime = null;
        }
    }
}