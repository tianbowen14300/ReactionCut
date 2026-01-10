package com.tbw.cut.diagnostic.model;

/**
 * 中断点类型枚举
 */
public enum BreakpointType {
    
    /**
     * 执行失败
     */
    EXECUTION_FAILURE("执行失败", "步骤执行失败导致的中断"),
    
    /**
     * 超时
     */
    TIMEOUT("超时", "步骤执行超时导致的中断"),
    
    /**
     * 异常
     */
    EXCEPTION("异常", "执行过程中抛出异常导致的中断"),
    
    /**
     * 缺失依赖
     */
    MISSING_DEPENDENCY("缺失依赖", "缺少必要的依赖服务或资源"),
    
    /**
     * 配置错误
     */
    CONFIGURATION_ERROR("配置错误", "配置参数错误导致的中断"),
    
    /**
     * 资源不足
     */
    RESOURCE_SHORTAGE("资源不足", "系统资源不足导致的中断"),
    
    /**
     * 数据不一致
     */
    DATA_INCONSISTENCY("数据不一致", "数据状态不一致导致的中断"),
    
    /**
     * 网络错误
     */
    NETWORK_ERROR("网络错误", "网络连接问题导致的中断");
    
    private final String displayName;
    private final String description;
    
    BreakpointType(String displayName, String description) {
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