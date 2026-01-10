package com.tbw.cut.bilibili.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.DecimalMax;

/**
 * Configuration properties for video info retrieval retry mechanism
 * 
 * This class centralizes all retry-related configuration parameters
 * with sensible defaults and validation constraints.
 */
@Component
@ConfigurationProperties(prefix = "bilibili.video.retry")
@Validated
public class RetryConfiguration {
    
    /**
     * Maximum number of retry attempts for video info retrieval
     * Default: 5 attempts
     */
    @Min(1)
    @Max(20)
    private int maxAttempts = 5;
    
    /**
     * Initial delay between retry attempts in milliseconds
     * Default: 1000ms (1 second)
     */
    @Min(100)
    @Max(10000)
    private long initialDelayMs = 1000;
    
    /**
     * Maximum delay between retry attempts in milliseconds
     * Default: 30000ms (30 seconds)
     */
    @Min(1000)
    @Max(300000)
    private long maxDelayMs = 30000;
    
    /**
     * Backoff multiplier for exponential backoff calculation
     * Default: 2.0 (doubles the delay each time)
     */
    @DecimalMin("1.1")
    @DecimalMax("5.0")
    private double backoffMultiplier = 2.0;
    
    /**
     * Jitter factor to add randomness to delays (0.0 to 1.0)
     * Default: 0.1 (±10% randomness)
     */
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double jitterFactor = 0.1;
    
    /**
     * Number of consecutive failures before circuit breaker trips
     * Default: 10 failures
     */
    @Min(3)
    @Max(100)
    private int circuitBreakerThreshold = 10;
    
    /**
     * Circuit breaker timeout in milliseconds before allowing retry
     * Default: 60000ms (1 minute)
     */
    @Min(10000)
    @Max(600000)
    private long circuitBreakerTimeoutMs = 60000;
    
    /**
     * TTL for cached video info responses in minutes
     * Default: 5 minutes
     */
    @Min(1)
    @Max(60)
    private int cacheTtlMinutes = 5;
    
    /**
     * Maximum size of the video info cache
     * Default: 1000 entries
     */
    @Min(100)
    @Max(10000)
    private int cacheMaxSize = 1000;
    
    /**
     * Maximum number of concurrent retry attempts
     * Default: 10 concurrent attempts
     */
    @Min(1)
    @Max(50)
    private int maxConcurrentAttempts = 10;
    
    // Getters and Setters
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
    
    public long getInitialDelayMs() {
        return initialDelayMs;
    }
    
    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }
    
    public long getMaxDelayMs() {
        return maxDelayMs;
    }
    
    public void setMaxDelayMs(long maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
    }
    
    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }
    
    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }
    
    public double getJitterFactor() {
        return jitterFactor;
    }
    
    public void setJitterFactor(double jitterFactor) {
        this.jitterFactor = jitterFactor;
    }
    
    public int getCircuitBreakerThreshold() {
        return circuitBreakerThreshold;
    }
    
    public void setCircuitBreakerThreshold(int circuitBreakerThreshold) {
        this.circuitBreakerThreshold = circuitBreakerThreshold;
    }
    
    public long getCircuitBreakerTimeoutMs() {
        return circuitBreakerTimeoutMs;
    }
    
    public void setCircuitBreakerTimeoutMs(long circuitBreakerTimeoutMs) {
        this.circuitBreakerTimeoutMs = circuitBreakerTimeoutMs;
    }
    
    public int getCacheTtlMinutes() {
        return cacheTtlMinutes;
    }
    
    public void setCacheTtlMinutes(int cacheTtlMinutes) {
        this.cacheTtlMinutes = cacheTtlMinutes;
    }
    
    public int getCacheMaxSize() {
        return cacheMaxSize;
    }
    
    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }
    
    public int getMaxConcurrentAttempts() {
        return maxConcurrentAttempts;
    }
    
    public void setMaxConcurrentAttempts(int maxConcurrentAttempts) {
        this.maxConcurrentAttempts = maxConcurrentAttempts;
    }
    
    @Override
    public String toString() {
        return "RetryConfiguration{" +
                "maxAttempts=" + maxAttempts +
                ", initialDelayMs=" + initialDelayMs +
                ", maxDelayMs=" + maxDelayMs +
                ", backoffMultiplier=" + backoffMultiplier +
                ", jitterFactor=" + jitterFactor +
                ", circuitBreakerThreshold=" + circuitBreakerThreshold +
                ", circuitBreakerTimeoutMs=" + circuitBreakerTimeoutMs +
                ", cacheTtlMinutes=" + cacheTtlMinutes +
                ", cacheMaxSize=" + cacheMaxSize +
                ", maxConcurrentAttempts=" + maxConcurrentAttempts +
                '}';
    }
}