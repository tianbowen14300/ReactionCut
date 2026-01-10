package com.tbw.cut.workflow.model;

/**
 * 工作流状态枚举
 */
public enum WorkflowStatus {
    PENDING("待开始"),
    RUNNING("执行中"),
    PAUSED("已暂停"),
    COMPLETED("已完成"),
    FAILED("执行失败"),
    CANCELLED("已取消");
    
    private final String description;
    
    WorkflowStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}