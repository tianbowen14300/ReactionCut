package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 修复结果
 */
public class RepairResult {
    
    private String repairId;
    private boolean success;
    private String taskId;
    private RepairType repairType;
    private LocalDateTime repairTime;
    private String description;
    private List<String> actionsPerformed;
    private String errorMessage;
    private int itemsRepaired;
    private int itemsFailed;
    private Long executionTimeMs;
    
    public RepairResult() {
        this.repairTime = LocalDateTime.now();
        this.repairId = generateRepairId();
    }
    
    public RepairResult(String taskId, RepairType repairType, boolean success) {
        this();
        this.taskId = taskId;
        this.repairType = repairType;
        this.success = success;
    }
    
    private String generateRepairId() {
        return "REPAIR-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public String getRepairId() {
        return repairId;
    }
    
    public void setRepairId(String repairId) {
        this.repairId = repairId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public RepairType getRepairType() {
        return repairType;
    }
    
    public void setRepairType(RepairType repairType) {
        this.repairType = repairType;
    }
    
    public LocalDateTime getRepairTime() {
        return repairTime;
    }
    
    public void setRepairTime(LocalDateTime repairTime) {
        this.repairTime = repairTime;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getActionsPerformed() {
        return actionsPerformed;
    }
    
    public void setActionsPerformed(List<String> actionsPerformed) {
        this.actionsPerformed = actionsPerformed;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public int getItemsRepaired() {
        return itemsRepaired;
    }
    
    public void setItemsRepaired(int itemsRepaired) {
        this.itemsRepaired = itemsRepaired;
    }
    
    public int getItemsFailed() {
        return itemsFailed;
    }
    
    public void setItemsFailed(int itemsFailed) {
        this.itemsFailed = itemsFailed;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    @Override
    public String toString() {
        return "RepairResult{" +
                "repairId='" + repairId + '\'' +
                ", success=" + success +
                ", taskId='" + taskId + '\'' +
                ", repairType=" + repairType +
                ", itemsRepaired=" + itemsRepaired +
                ", itemsFailed=" + itemsFailed +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}