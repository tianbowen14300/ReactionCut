package com.tbw.cut.diagnostic.model;

/**
 * 事件步骤状态枚举
 */
public enum EventStepStatus {
    
    /**
     * 成功
     */
    SUCCESS("成功", "步骤执行成功"),
    
    /**
     * 失败
     */
    FAILED("失败", "步骤执行失败"),
    
    /**
     * 进行中
     */
    IN_PROGRESS("进行中", "步骤正在执行"),
    
    /**
     * 跳过
     */
    SKIPPED("跳过", "步骤被跳过"),
    
    /**
     * 超时
     */
    TIMEOUT("超时", "步骤执行超时");
    
    private final String displayName;
    private final String description;
    
    EventStepStatus(String displayName, String description) {
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