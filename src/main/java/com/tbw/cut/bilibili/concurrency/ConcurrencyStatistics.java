package com.tbw.cut.bilibili.concurrency;

import java.time.LocalDateTime;

/**
 * Statistics for concurrency management
 */
public class ConcurrencyStatistics {
    
    private final long totalTasksExecuted;
    private final long totalTasksQueued;
    private final long totalTasksTimedOut;
    private final long totalTasksRejected;
    private final int currentActiveTasks;
    private final int currentQueuedTasks;
    private final long averageExecutionTimeMs;
    private final long averageQueueTimeMs;
    private final LocalDateTime collectionStartTime;
    private final LocalDateTime lastUpdateTime;
    
    public ConcurrencyStatistics(long totalTasksExecuted, long totalTasksQueued, 
                               long totalTasksTimedOut, long totalTasksRejected,
                               int currentActiveTasks, int currentQueuedTasks,
                               long averageExecutionTimeMs, long averageQueueTimeMs,
                               LocalDateTime collectionStartTime, LocalDateTime lastUpdateTime) {
        this.totalTasksExecuted = totalTasksExecuted;
        this.totalTasksQueued = totalTasksQueued;
        this.totalTasksTimedOut = totalTasksTimedOut;
        this.totalTasksRejected = totalTasksRejected;
        this.currentActiveTasks = currentActiveTasks;
        this.currentQueuedTasks = currentQueuedTasks;
        this.averageExecutionTimeMs = averageExecutionTimeMs;
        this.averageQueueTimeMs = averageQueueTimeMs;
        this.collectionStartTime = collectionStartTime;
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public long getTotalTasksExecuted() {
        return totalTasksExecuted;
    }
    
    public long getTotalTasksQueued() {
        return totalTasksQueued;
    }
    
    public long getTotalTasksTimedOut() {
        return totalTasksTimedOut;
    }
    
    public long getTotalTasksRejected() {
        return totalTasksRejected;
    }
    
    public int getCurrentActiveTasks() {
        return currentActiveTasks;
    }
    
    public int getCurrentQueuedTasks() {
        return currentQueuedTasks;
    }
    
    public long getAverageExecutionTimeMs() {
        return averageExecutionTimeMs;
    }
    
    public long getAverageQueueTimeMs() {
        return averageQueueTimeMs;
    }
    
    public LocalDateTime getCollectionStartTime() {
        return collectionStartTime;
    }
    
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public double getSuccessRate() {
        long totalTasks = totalTasksExecuted + totalTasksTimedOut + totalTasksRejected;
        return totalTasks > 0 ? (double) totalTasksExecuted / totalTasks * 100.0 : 0.0;
    }
    
    @Override
    public String toString() {
        return "ConcurrencyStatistics{" +
                "totalTasksExecuted=" + totalTasksExecuted +
                ", totalTasksQueued=" + totalTasksQueued +
                ", totalTasksTimedOut=" + totalTasksTimedOut +
                ", totalTasksRejected=" + totalTasksRejected +
                ", currentActiveTasks=" + currentActiveTasks +
                ", currentQueuedTasks=" + currentQueuedTasks +
                ", averageExecutionTimeMs=" + averageExecutionTimeMs +
                ", averageQueueTimeMs=" + averageQueueTimeMs +
                ", successRate=" + String.format("%.2f", getSuccessRate()) + "%" +
                ", collectionStartTime=" + collectionStartTime +
                ", lastUpdateTime=" + lastUpdateTime +
                '}';
    }
}