package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 事件发布统计信息
 */
public class EventPublishingStats {
    
    private LocalDateTime timestamp;
    private long totalEventsPublished;
    private long successfulEvents;
    private long failedEvents;
    private double successRate;
    private Map<String, Long> eventTypeDistribution;
    private long averagePublishTimeMs;
    private long maxPublishTimeMs;
    private long minPublishTimeMs;
    private int activeListeners;
    private int totalListeners;
    
    public EventPublishingStats() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getTotalEventsPublished() {
        return totalEventsPublished;
    }
    
    public void setTotalEventsPublished(long totalEventsPublished) {
        this.totalEventsPublished = totalEventsPublished;
    }
    
    public long getSuccessfulEvents() {
        return successfulEvents;
    }
    
    public void setSuccessfulEvents(long successfulEvents) {
        this.successfulEvents = successfulEvents;
    }
    
    public long getFailedEvents() {
        return failedEvents;
    }
    
    public void setFailedEvents(long failedEvents) {
        this.failedEvents = failedEvents;
    }
    
    public double getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
    
    public Map<String, Long> getEventTypeDistribution() {
        return eventTypeDistribution;
    }
    
    public void setEventTypeDistribution(Map<String, Long> eventTypeDistribution) {
        this.eventTypeDistribution = eventTypeDistribution;
    }
    
    public long getAveragePublishTimeMs() {
        return averagePublishTimeMs;
    }
    
    public void setAveragePublishTimeMs(long averagePublishTimeMs) {
        this.averagePublishTimeMs = averagePublishTimeMs;
    }
    
    public long getMaxPublishTimeMs() {
        return maxPublishTimeMs;
    }
    
    public void setMaxPublishTimeMs(long maxPublishTimeMs) {
        this.maxPublishTimeMs = maxPublishTimeMs;
    }
    
    public long getMinPublishTimeMs() {
        return minPublishTimeMs;
    }
    
    public void setMinPublishTimeMs(long minPublishTimeMs) {
        this.minPublishTimeMs = minPublishTimeMs;
    }
    
    public int getActiveListeners() {
        return activeListeners;
    }
    
    public void setActiveListeners(int activeListeners) {
        this.activeListeners = activeListeners;
    }
    
    public int getTotalListeners() {
        return totalListeners;
    }
    
    public void setTotalListeners(int totalListeners) {
        this.totalListeners = totalListeners;
    }
    
    @Override
    public String toString() {
        return "EventPublishingStats{" +
                "timestamp=" + timestamp +
                ", totalEventsPublished=" + totalEventsPublished +
                ", successfulEvents=" + successfulEvents +
                ", failedEvents=" + failedEvents +
                ", successRate=" + successRate +
                ", averagePublishTimeMs=" + averagePublishTimeMs +
                ", activeListeners=" + activeListeners +
                ", totalListeners=" + totalListeners +
                '}';
    }
}