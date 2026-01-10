package com.tbw.cut.bilibili.cache;

import java.time.LocalDateTime;

/**
 * Cache statistics for monitoring cache performance
 */
public class CacheStatistics {
    
    private final long hitCount;
    private final long missCount;
    private final long evictionCount;
    private final long totalRequests;
    private final double hitRate;
    private final LocalDateTime creationTime;
    private final LocalDateTime lastAccessTime;
    
    public CacheStatistics(long hitCount, long missCount, long evictionCount, 
                          LocalDateTime creationTime, LocalDateTime lastAccessTime) {
        this.hitCount = hitCount;
        this.missCount = missCount;
        this.evictionCount = evictionCount;
        this.totalRequests = hitCount + missCount;
        this.hitRate = totalRequests > 0 ? (double) hitCount / totalRequests * 100.0 : 0.0;
        this.creationTime = creationTime;
        this.lastAccessTime = lastAccessTime;
    }
    
    public long getHitCount() {
        return hitCount;
    }
    
    public long getMissCount() {
        return missCount;
    }
    
    public long getEvictionCount() {
        return evictionCount;
    }
    
    public long getTotalRequests() {
        return totalRequests;
    }
    
    public double getHitRate() {
        return hitRate;
    }
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
    }
    
    @Override
    public String toString() {
        return "CacheStatistics{" +
                "hitCount=" + hitCount +
                ", missCount=" + missCount +
                ", evictionCount=" + evictionCount +
                ", totalRequests=" + totalRequests +
                ", hitRate=" + String.format("%.2f", hitRate) + "%" +
                ", creationTime=" + creationTime +
                ", lastAccessTime=" + lastAccessTime +
                '}';
    }
}