package com.tbw.cut.bilibili.cache;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.config.RetryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of VideoInfoCache using in-memory storage with TTL
 * 
 * This class provides TTL-based caching for video information to reduce
 * API calls and improve performance.
 */
@Component
public class VideoInfoCacheImpl implements VideoInfoCache {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoInfoCacheImpl.class);
    
    private final RetryConfiguration config;
    private final ConcurrentHashMap<Long, CacheEntry<JSONObject>> videoInfoCache;
    private final ConcurrentHashMap<String, CacheEntry<Long>> aidCache;
    
    // Statistics
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    private final LocalDateTime creationTime = LocalDateTime.now();
    private volatile LocalDateTime lastAccessTime = LocalDateTime.now();
    
    @Autowired
    public VideoInfoCacheImpl(RetryConfiguration config) {
        this.config = config;
        this.videoInfoCache = new ConcurrentHashMap<>();
        this.aidCache = new ConcurrentHashMap<>();
        
        // Start cleanup thread for expired entries
        startCleanupThread();
    }
    
    @Override
    public JSONObject getVideoInfo(Long aid) {
        if (aid == null) {
            return null;
        }
        
        lastAccessTime = LocalDateTime.now();
        CacheEntry<JSONObject> entry = videoInfoCache.get(aid);
        
        if (entry == null || entry.isExpired(config.getCacheTtlMinutes())) {
            missCount.incrementAndGet();
            if (entry != null) {
                videoInfoCache.remove(aid);
                evictionCount.incrementAndGet();
                logger.debug("Expired video info cache entry removed for AID: {}", aid);
            }
            return null;
        }
        
        hitCount.incrementAndGet();
        logger.debug("Cache hit for video info AID: {}", aid);
        return entry.getValue();
    }
    
    @Override
    public void putVideoInfo(Long aid, JSONObject videoInfo) {
        if (aid == null || videoInfo == null) {
            return;
        }
        
        // Check cache size limit
        if (videoInfoCache.size() >= config.getCacheMaxSize()) {
            evictOldestEntries();
        }
        
        CacheEntry<JSONObject> entry = new CacheEntry<>(videoInfo);
        videoInfoCache.put(aid, entry);
        
        logger.debug("Cached video info for AID: {}", aid);
    }
    
    @Override
    public Long getAid(String bvid) {
        if (bvid == null || bvid.trim().isEmpty()) {
            return null;
        }
        
        lastAccessTime = LocalDateTime.now();
        CacheEntry<Long> entry = aidCache.get(bvid);
        
        if (entry == null || entry.isExpired(config.getCacheTtlMinutes())) {
            missCount.incrementAndGet();
            if (entry != null) {
                aidCache.remove(bvid);
                evictionCount.incrementAndGet();
                logger.debug("Expired AID cache entry removed for BVID: {}", bvid);
            }
            return null;
        }
        
        hitCount.incrementAndGet();
        logger.debug("Cache hit for AID BVID: {}", bvid);
        return entry.getValue();
    }
    
    @Override
    public void putAid(String bvid, Long aid) {
        if (bvid == null || bvid.trim().isEmpty() || aid == null) {
            return;
        }
        
        // Check cache size limit
        if (aidCache.size() >= config.getCacheMaxSize()) {
            evictOldestAidEntries();
        }
        
        CacheEntry<Long> entry = new CacheEntry<>(aid);
        aidCache.put(bvid, entry);
        
        logger.debug("Cached AID {} for BVID: {}", aid, bvid);
    }
    
    @Override
    public void invalidateVideoInfo(Long aid) {
        if (aid != null && videoInfoCache.remove(aid) != null) {
            logger.debug("Invalidated video info cache for AID: {}", aid);
        }
    }
    
    @Override
    public void invalidateAid(String bvid) {
        if (bvid != null && aidCache.remove(bvid) != null) {
            logger.debug("Invalidated AID cache for BVID: {}", bvid);
        }
    }
    
    @Override
    public void clearAll() {
        int videoInfoSize = videoInfoCache.size();
        int aidSize = aidCache.size();
        
        videoInfoCache.clear();
        aidCache.clear();
        
        logger.info("Cleared all cache entries: {} video info, {} AID entries", videoInfoSize, aidSize);
    }
    
    @Override
    public CacheStatistics getStatistics() {
        return new CacheStatistics(
            hitCount.get(),
            missCount.get(),
            evictionCount.get(),
            creationTime,
            lastAccessTime
        );
    }
    
    @Override
    public int size() {
        return videoInfoCache.size() + aidCache.size();
    }
    
    /**
     * Evict oldest entries when cache size limit is reached
     */
    private void evictOldestEntries() {
        // Simple LRU eviction - remove 10% of entries
        int toRemove = Math.max(1, config.getCacheMaxSize() / 10);
        
        videoInfoCache.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().getCreationTime().compareTo(e2.getValue().getCreationTime()))
            .limit(toRemove)
            .forEach(entry -> {
                videoInfoCache.remove(entry.getKey());
                evictionCount.incrementAndGet();
            });
        
        logger.debug("Evicted {} oldest video info cache entries", toRemove);
    }
    
    /**
     * Evict oldest AID entries when cache size limit is reached
     */
    private void evictOldestAidEntries() {
        // Simple LRU eviction - remove 10% of entries
        int toRemove = Math.max(1, config.getCacheMaxSize() / 10);
        
        aidCache.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().getCreationTime().compareTo(e2.getValue().getCreationTime()))
            .limit(toRemove)
            .forEach(entry -> {
                aidCache.remove(entry.getKey());
                evictionCount.incrementAndGet();
            });
        
        logger.debug("Evicted {} oldest AID cache entries", toRemove);
    }
    
    /**
     * Start background thread to clean up expired entries
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60000); // Run every minute
                    cleanupExpiredEntries();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "video-info-cache-cleanup");
        
        cleanupThread.setDaemon(true);
        cleanupThread.start();
        
        logger.info("Started cache cleanup thread");
    }
    
    /**
     * Remove expired entries from cache
     */
    private void cleanupExpiredEntries() {
        int ttlMinutes = config.getCacheTtlMinutes();
        int removedVideoInfo = 0;
        int removedAid = 0;
        
        // Clean video info cache
        for (Map.Entry<Long, CacheEntry<JSONObject>> entry : videoInfoCache.entrySet()) {
            if (entry.getValue().isExpired(ttlMinutes)) {
                videoInfoCache.remove(entry.getKey());
                evictionCount.incrementAndGet();
                removedVideoInfo++;
            }
        }
        
        // Clean AID cache
        for (Map.Entry<String, CacheEntry<Long>> entry : aidCache.entrySet()) {
            if (entry.getValue().isExpired(ttlMinutes)) {
                aidCache.remove(entry.getKey());
                evictionCount.incrementAndGet();
                removedAid++;
            }
        }
        
        if (removedVideoInfo > 0 || removedAid > 0) {
            logger.debug("Cleanup removed {} expired video info entries and {} expired AID entries", 
                        removedVideoInfo, removedAid);
        }
    }
    
    /**
     * Cache entry with TTL support
     */
    private static class CacheEntry<T> {
        private final T value;
        private final LocalDateTime creationTime;
        
        public CacheEntry(T value) {
            this.value = value;
            this.creationTime = LocalDateTime.now();
        }
        
        public T getValue() {
            return value;
        }
        
        public LocalDateTime getCreationTime() {
            return creationTime;
        }
        
        public boolean isExpired(int ttlMinutes) {
            return LocalDateTime.now().isAfter(creationTime.plusMinutes(ttlMinutes));
        }
    }
}