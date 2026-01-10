package com.tbw.cut.bilibili.retry;

import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for calculating jitter to prevent thundering herd problems
 * 
 * This class provides various jitter calculation strategies to add randomness
 * to retry delays and prevent multiple clients from retrying simultaneously.
 */
@Component
public class JitterCalculator {
    
    /**
     * Jitter calculation strategy enumeration
     */
    public enum JitterStrategy {
        /**
         * Full jitter: random value between 0 and base delay
         */
        FULL,
        
        /**
         * Equal jitter: base delay / 2 + random(0, base delay / 2)
         */
        EQUAL,
        
        /**
         * Decorrelated jitter: random value between base delay and previous delay * 3
         */
        DECORRELATED,
        
        /**
         * Gaussian jitter: base delay + gaussian random * jitter factor
         */
        GAUSSIAN
    }
    
    /**
     * Calculate jitter using the specified strategy
     * 
     * @param baseDelay The base delay in milliseconds
     * @param jitterFactor The jitter factor (interpretation depends on strategy)
     * @param strategy The jitter calculation strategy
     * @return The jittered delay value
     */
    public long calculateJitter(long baseDelay, double jitterFactor, JitterStrategy strategy) {
        return calculateJitter(baseDelay, jitterFactor, strategy, baseDelay);
    }
    
    /**
     * Calculate jitter using the specified strategy with previous delay context
     * 
     * @param baseDelay The base delay in milliseconds
     * @param jitterFactor The jitter factor (interpretation depends on strategy)
     * @param strategy The jitter calculation strategy
     * @param previousDelay The previous delay (used for decorrelated jitter)
     * @return The jittered delay value
     */
    public long calculateJitter(long baseDelay, double jitterFactor, JitterStrategy strategy, long previousDelay) {
        if (baseDelay < 0) {
            throw new IllegalArgumentException("Base delay must be non-negative");
        }
        
        if (jitterFactor < 0.0 || jitterFactor > 1.0) {
            throw new IllegalArgumentException("Jitter factor must be between 0.0 and 1.0");
        }
        
        switch (strategy) {
            case FULL:
                return calculateFullJitter(baseDelay);
                
            case EQUAL:
                return calculateEqualJitter(baseDelay);
                
            case DECORRELATED:
                return calculateDecorrelatedJitter(baseDelay, previousDelay);
                
            case GAUSSIAN:
                return calculateGaussianJitter(baseDelay, jitterFactor);
                
            default:
                throw new IllegalArgumentException("Unknown jitter strategy: " + strategy);
        }
    }
    
    /**
     * Full jitter: returns random value between 0 and base delay
     * This provides maximum randomness but can result in very short delays
     */
    private long calculateFullJitter(long baseDelay) {
        if (baseDelay == 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextLong(0, baseDelay + 1);
    }
    
    /**
     * Equal jitter: returns base delay / 2 + random(0, base delay / 2)
     * This ensures at least half the base delay while adding randomness
     */
    private long calculateEqualJitter(long baseDelay) {
        if (baseDelay == 0) {
            return 0;
        }
        
        long halfDelay = baseDelay / 2;
        long randomPart = ThreadLocalRandom.current().nextLong(0, halfDelay + 1);
        return halfDelay + randomPart;
    }
    
    /**
     * Decorrelated jitter: returns random value between base delay and previous delay * 3
     * This creates a decorrelated sequence that prevents synchronization
     */
    private long calculateDecorrelatedJitter(long baseDelay, long previousDelay) {
        if (baseDelay == 0) {
            return 0;
        }
        
        long minDelay = baseDelay;
        long maxDelay = Math.max(baseDelay, previousDelay * 3);
        
        if (minDelay >= maxDelay) {
            return minDelay;
        }
        
        return ThreadLocalRandom.current().nextLong(minDelay, maxDelay + 1);
    }
    
    /**
     * Gaussian jitter: returns base delay + gaussian random * jitter factor * base delay
     * This creates a normal distribution around the base delay
     */
    private long calculateGaussianJitter(long baseDelay, double jitterFactor) {
        if (baseDelay == 0 || jitterFactor == 0.0) {
            return baseDelay;
        }
        
        // Generate gaussian random value (mean=0, stddev=1)
        double gaussianRandom = ThreadLocalRandom.current().nextGaussian();
        
        // Scale by jitter factor and base delay
        long jitterAmount = (long) (gaussianRandom * jitterFactor * baseDelay);
        
        // Ensure result is non-negative
        return Math.max(0, baseDelay + jitterAmount);
    }
    
    /**
     * Calculate jitter bounds for validation purposes
     * 
     * @param baseDelay The base delay
     * @param jitterFactor The jitter factor
     * @param strategy The jitter strategy
     * @return Array containing [minPossibleDelay, maxPossibleDelay]
     */
    public long[] calculateJitterBounds(long baseDelay, double jitterFactor, JitterStrategy strategy) {
        switch (strategy) {
            case FULL:
                return new long[]{0, baseDelay};
                
            case EQUAL:
                return new long[]{baseDelay / 2, baseDelay};
                
            case DECORRELATED:
                return new long[]{baseDelay, baseDelay * 3};
                
            case GAUSSIAN:
                // For gaussian, use 3 standard deviations as bounds (99.7% of values)
                long jitterRange = (long) (3 * jitterFactor * baseDelay);
                return new long[]{Math.max(0, baseDelay - jitterRange), baseDelay + jitterRange};
                
            default:
                throw new IllegalArgumentException("Unknown jitter strategy: " + strategy);
        }
    }
    
    /**
     * Check if a jittered delay is within expected bounds for the given strategy
     * 
     * @param jitteredDelay The actual jittered delay
     * @param baseDelay The base delay
     * @param jitterFactor The jitter factor
     * @param strategy The jitter strategy
     * @return true if the delay is within expected bounds
     */
    public boolean isWithinBounds(long jitteredDelay, long baseDelay, double jitterFactor, JitterStrategy strategy) {
        long[] bounds = calculateJitterBounds(baseDelay, jitterFactor, strategy);
        return jitteredDelay >= bounds[0] && jitteredDelay <= bounds[1];
    }
    
    /**
     * Get the default jitter strategy for retry operations
     * 
     * @return The recommended jitter strategy
     */
    public JitterStrategy getDefaultStrategy() {
        return JitterStrategy.GAUSSIAN;
    }
}