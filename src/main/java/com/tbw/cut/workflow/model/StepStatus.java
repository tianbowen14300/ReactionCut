package com.tbw.cut.workflow.model;

/**
 * 工作流步骤状态枚举
 */
public enum StepStatus {
    PENDING("等待执行"),
    RUNNING("执行中"),
    COMPLETED("已完成"),
    FAILED("执行失败"),
    SKIPPED("已跳过");
    
    private final String description;
    
    StepStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}