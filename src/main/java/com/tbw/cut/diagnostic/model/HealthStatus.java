package com.tbw.cut.diagnostic.model;

/**
 * 健康状态枚举
 */
public enum HealthStatus {
    
    /**
     * 正常运行
     */
    UP("正常", "组件正常运行"),
    
    /**
     * 部分功能异常
     */
    DEGRADED("降级", "组件部分功能异常，但仍可提供基本服务"),
    
    /**
     * 服务不可用
     */
    DOWN("停机", "组件服务不可用"),
    
    /**
     * 未知状态
     */
    UNKNOWN("未知", "无法确定组件状态");
    
    private final String displayName;
    private final String description;
    
    HealthStatus(String displayName, String description) {
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