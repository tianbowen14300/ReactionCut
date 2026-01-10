package com.tbw.cut.diagnostic.model;

/**
 * 事件流状态枚举
 */
public enum EventFlowStatus {
    
    /**
     * 正在追踪
     */
    TRACKING("正在追踪", "事件流正在被追踪"),
    
    /**
     * 追踪完成
     */
    COMPLETED("追踪完成", "事件流追踪完成"),
    
    /**
     * 追踪中断
     */
    INTERRUPTED("追踪中断", "事件流追踪被中断"),
    
    /**
     * 追踪失败
     */
    FAILED("追踪失败", "事件流追踪失败"),
    
    /**
     * 超时
     */
    TIMEOUT("超时", "事件流追踪超时");
    
    private final String displayName;
    private final String description;
    
    EventFlowStatus(String displayName, String description) {
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