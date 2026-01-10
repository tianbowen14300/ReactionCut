package com.tbw.cut.diagnostic.model;

/**
 * 工作流执行状态枚举
 */
public enum WorkflowExecutionStatus {
    
    /**
     * 正常执行
     */
    NORMAL("正常执行", "工作流正常执行中"),
    
    /**
     * 执行缓慢
     */
    SLOW("执行缓慢", "工作流执行速度较慢"),
    
    /**
     * 卡住
     */
    STUCK("卡住", "工作流执行卡住，长时间无响应"),
    
    /**
     * 异常
     */
    ABNORMAL("异常", "工作流执行异常"),
    
    /**
     * 资源不足
     */
    RESOURCE_CONSTRAINED("资源不足", "因资源不足导致执行受限");
    
    private final String displayName;
    private final String description;
    
    WorkflowExecutionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}