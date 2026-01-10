package com.tbw.cut.diagnostic.model;

/**
 * 卡住类型枚举
 */
public enum StuckType {
    
    /**
     * 长时间无响应
     */
    NO_RESPONSE("长时间无响应", "工作流长时间没有任何活动"),
    
    /**
     * 步骤执行超时
     */
    STEP_TIMEOUT("步骤执行超时", "当前步骤执行时间超过预期"),
    
    /**
     * 资源等待
     */
    RESOURCE_WAITING("资源等待", "等待系统资源释放"),
    
    /**
     * 依赖服务不可用
     */
    DEPENDENCY_UNAVAILABLE("依赖服务不可用", "依赖的外部服务不可用"),
    
    /**
     * 死锁
     */
    DEADLOCK("死锁", "工作流陷入死锁状态"),
    
    /**
     * 循环等待
     */
    CIRCULAR_WAITING("循环等待", "工作流陷入循环等待状态"),
    
    /**
     * 异常阻塞
     */
    EXCEPTION_BLOCKED("异常阻塞", "因异常导致的阻塞状态"),
    
    /**
     * 配置错误
     */
    CONFIGURATION_ERROR("配置错误", "配置错误导致的卡住状态");
    
    private final String displayName;
    private final String description;
    
    StuckType(String displayName, String description) {
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