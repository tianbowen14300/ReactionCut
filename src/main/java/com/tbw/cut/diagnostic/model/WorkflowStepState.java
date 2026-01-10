package com.tbw.cut.diagnostic.model;

/**
 * 工作流步骤状态枚举
 */
public enum WorkflowStepState {
    
    /**
     * 等待执行
     */
    PENDING("等待执行", "步骤等待执行"),
    
    /**
     * 正在执行
     */
    RUNNING("正在执行", "步骤正在执行"),
    
    /**
     * 执行成功
     */
    COMPLETED("执行成功", "步骤执行成功"),
    
    /**
     * 执行失败
     */
    FAILED("执行失败", "步骤执行失败"),
    
    /**
     * 已跳过
     */
    SKIPPED("已跳过", "步骤被跳过"),
    
    /**
     * 已取消
     */
    CANCELLED("已取消", "步骤被取消"),
    
    /**
     * 重试中
     */
    RETRYING("重试中", "步骤正在重试"),
    
    /**
     * 暂停
     */
    PAUSED("暂停", "步骤暂停执行");
    
    private final String displayName;
    private final String description;
    
    WorkflowStepState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为终止状态
     */
    public boolean isTerminalState() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == SKIPPED;
    }
    
    /**
     * 判断是否为活跃状态
     */
    public boolean isActiveState() {
        return this == RUNNING || this == RETRYING;
    }
}