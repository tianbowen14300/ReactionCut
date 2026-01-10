package com.tbw.cut.bilibili.cache;

import com.alibaba.fastjson.JSONObject;

/**
 * Interface for video information caching
 * 
 * This interface defines the contract for caching video information
 * to reduce API calls and improve performance.
 */
public interface VideoInfoCache {
    
    /**
     * Get cached video information by AID
     * 
     * @param aid The video AID
     * @return Cached video info JSON or null if not found/expired
     */
    JSONObject getVideoInfo(Long aid);
    
    /**
     * Cache video information with TTL
     * 
     * @param aid The video AID
     * @param videoInfo The video information to cache
     */
    void putVideoInfo(Long aid, JSONObject videoInfo);
    
    /**
     * Get cached AID by BVID
     * 
     * @param bvid The video BVID
     * @return Cached AID or null if not found/expired
     */
    Long getAid(String bvid);
    
    /**
     * Cache AID with TTL
     * 
     * @param bvid The video BVID
     * @param aid The video AID
     */
    void putAid(String bvid, Long aid);
    
    /**
     * Invalidate cache entry by AID
     * 
     * @param aid The video AID
     */
    void invalidateVideoInfo(Long aid);
    
    /**
     * Invalidate cache entry by BVID
     * 
     * @param bvid The video BVID
     */
    void invalidateAid(String bvid);
    
    /**
     * Clear all cache entries
     */
    void clearAll();
    
    /**
     * Get cache statistics
     * 
     * @return Cache statistics object
     */
    CacheStatistics getStatistics();
    
    /**
     * Get current cache size
     * 
     * @return Number of entries in cache
     */
    int size();
}