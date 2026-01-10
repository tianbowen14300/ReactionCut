package com.tbw.cut.bilibili.retry;

import com.tbw.cut.bilibili.config.RetryConfiguration;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Utility class for calculating exponential backoff delays with jitter
 * 
 * This class implements exponential backoff algorithm with configurable
 * parameters and jitter to prevent thundering herd problems.
 */
@Component
public class ExponentialBackoffCalculator {
    
    private final Random random = new Random();
    
    /**
     * Calculate the delay for a specific retry attempt using exponential backoff
     * 
     * @param attemptNumber The current attempt number (1-based)
     * @param config The retry configuration
     * @return The calculated delay in milliseconds
     */
    public long calculateDelay(int attemptNumber, RetryConfiguration config) {
        if (attemptNumber <= 0) {
            throw new IllegalArgumentException("Attempt number must be positive");
        }
        
        // Calculate base delay using exponential backoff
        // delay = initialDelay * (multiplier ^ (attempt - 1))
        double baseDelay = config.getInitialDelayMs() * 
                          Math.pow(config.getBackoffMultiplier(), attemptNumber - 1);
        
        // Cap the delay at maximum configured value
        long cappedDelay = Math.min((long) baseDelay, config.getMaxDelayMs());
        
        // Add jitter to prevent thundering herd
        long delayWithJitter = addJitter(cappedDelay, config.getJitterFactor());
        
        return Math.max(delayWithJitter, 0);
    }
    
    /**
     * Add jitter to a delay value to prevent thundering herd problems
     * 
     * @param baseDelay The base delay in milliseconds
     * @param jitterFactor The jitter factor (0.0 to 1.0)
     * @return The delay with jitter applied
     */
    public long addJitter(long baseDelay, double jitterFactor) {
        if (jitterFactor <= 0.0) {
            return baseDelay;
        }
        
        if (jitterFactor > 1.0) {
            throw new IllegalArgumentException("Jitter factor must be between 0.0 and 1.0");
        }
        
        // Calculate jitter range: ±(baseDelay * jitterFactor)
        long jitterRange = (long) (baseDelay * jitterFactor);
        
        // Generate random jitter between -jitterRange and +jitterRange
        long jitter = (long) (random.nextGaussian() * jitterRange);
        
        return baseDelay + jitter;
    }
    
    /**
     * Calculate the total time that would be spent on all retry attempts
     * 
     * @param maxAttempts The maximum number of attempts
     * @param config The retry configuration
     * @return The total estimated time in milliseconds
     */
    public long calculateTotalRetryTime(int maxAttempts, RetryConfiguration config) {
        long totalTime = 0;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            // Use base delay without jitter for estimation
            double baseDelay = config.getInitialDelayMs() * 
                              Math.pow(config.getBackoffMultiplier(), attempt - 1);
            long cappedDelay = Math.min((long) baseDelay, config.getMaxDelayMs());
            totalTime += cappedDelay;
        }
        
        return totalTime;
    }
    
    /**
     * Check if a delay value is within expected bounds for the given attempt
     * 
     * @param delay The actual delay value
     * @param attemptNumber The attempt number
     * @param config The retry configuration
     * @return true if the delay is within expected bounds
     */
    public boolean isDelayWithinBounds(long delay, int attemptNumber, RetryConfiguration config) {
        // Calculate expected delay without jitter
        double baseDelay = config.getInitialDelayMs() * 
                          Math.pow(config.getBackoffMultiplier(), attemptNumber - 1);
        long expectedDelay = Math.min((long) baseDelay, config.getMaxDelayMs());
        
        // Calculate acceptable range with jitter
        long jitterRange = (long) (expectedDelay * config.getJitterFactor());
        long minDelay = expectedDelay - jitterRange;
        long maxDelay = expectedDelay + jitterRange;
        
        return delay >= Math.max(0, minDelay) && delay <= maxDelay;
    }
    
    /**
     * Get the sequence of delays for all retry attempts
     * 
     * @param maxAttempts The maximum number of attempts
     * @param config The retry configuration
     * @return Array of delay values for each attempt
     */
    public long[] getDelaySequence(int maxAttempts, RetryConfiguration config) {
        long[] delays = new long[maxAttempts];
        
        for (int i = 0; i < maxAttempts; i++) {
            delays[i] = calculateDelay(i + 1, config);
        }
        
        return delays;
    }
}