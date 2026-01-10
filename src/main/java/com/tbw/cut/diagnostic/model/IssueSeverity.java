package com.tbw.cut.diagnostic.model;

/**
 * 问题严重程度枚举
 */
public enum IssueSeverity {
    
    /**
     * 低优先级 - 不影响核心功能
     */
    LOW("低", "不影响核心功能，可以稍后处理", 1),
    
    /**
     * 中等优先级 - 影响部分功能
     */
    MEDIUM("中", "影响部分功能，建议尽快处理", 2),
    
    /**
     * 高优先级 - 影响核心功能
     */
    HIGH("高", "影响核心功能，需要立即处理", 3),
    
    /**
     * 严重 - 系统功能严重受损
     */
    CRITICAL("严重", "系统功能严重受损，需要紧急处理", 4);
    
    private final String displayName;
    private final String description;
    private final int priority;
    
    IssueSeverity(String displayName, String description, int priority) {
        this.displayName = displayName;
        this.description = description;
        this.priority = priority;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getPriority() {
        return priority;
    }
}