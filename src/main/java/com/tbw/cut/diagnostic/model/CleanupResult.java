package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 清理结果
 */
public class CleanupResult {
    
    private String cleanupId;
    private boolean success;
    private LocalDateTime cleanupTime;
    private String cleanupType;
    private String description;
    private int itemsProcessed;
    private int itemsCleaned;
    private int itemsSkipped;
    private int itemsFailed;
    private List<String> cleanedItems;
    private List<String> failedItems;
    private String errorMessage;
    private Long executionTimeMs;
    
    public CleanupResult() {
        this.cleanupTime = LocalDateTime.now();
        this.cleanupId = generateCleanupId();
    }
    
    public CleanupResult(String cleanupType, boolean success) {
        this();
        this.cleanupType = cleanupType;
        this.success = success;
    }
    
    private String generateCleanupId() {
        return "CLEANUP-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public String getCleanupId() {
        return cleanupId;
    }
    
    public void setCleanupId(String cleanupId) {
        this.cleanupId = cleanupId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public LocalDateTime getCleanupTime() {
        return cleanupTime;
    }
    
    public void setCleanupTime(LocalDateTime cleanupTime) {
        this.cleanupTime = cleanupTime;
    }
    
    public String getCleanupType() {
        return cleanupType;
    }
    
    public void setCleanupType(String cleanupType) {
        this.cleanupType = cleanupType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getItemsProcessed() {
        return itemsProcessed;
    }
    
    public void setItemsProcessed(int itemsProcessed) {
        this.itemsProcessed = itemsProcessed;
    }
    
    public int getItemsCleaned() {
        return itemsCleaned;
    }
    
    public void setItemsCleaned(int itemsCleaned) {
        this.itemsCleaned = itemsCleaned;
    }
    
    public int getItemsSkipped() {
        return itemsSkipped;
    }
    
    public void setItemsSkipped(int itemsSkipped) {
        this.itemsSkipped = itemsSkipped;
    }
    
    public int getItemsFailed() {
        return itemsFailed;
    }
    
    public void setItemsFailed(int itemsFailed) {
        this.itemsFailed = itemsFailed;
    }
    
    public List<String> getCleanedItems() {
        return cleanedItems;
    }
    
    public void setCleanedItems(List<String> cleanedItems) {
        this.cleanedItems = cleanedItems;
    }
    
    public List<String> getFailedItems() {
        return failedItems;
    }
    
    public void setFailedItems(List<String> failedItems) {
        this.failedItems = failedItems;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    @Override
    public String toString() {
        return "CleanupResult{" +
                "cleanupId='" + cleanupId + '\'' +
                ", success=" + success +
                ", cleanupType='" + cleanupType + '\'' +
                ", itemsProcessed=" + itemsProcessed +
                ", itemsCleaned=" + itemsCleaned +
                ", itemsSkipped=" + itemsSkipped +
                ", itemsFailed=" + itemsFailed +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}