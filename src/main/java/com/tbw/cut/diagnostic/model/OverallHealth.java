package com.tbw.cut.diagnostic.model;

/**
 * 系统整体健康状态枚举
 */
public enum OverallHealth {
    
    /**
     * 健康 - 所有组件正常工作
     */
    HEALTHY("健康", "所有组件正常工作"),
    
    /**
     * 警告 - 存在一些非关键问题
     */
    WARNING("警告", "存在一些非关键问题，建议关注"),
    
    /**
     * 不健康 - 存在影响功能的问题
     */
    UNHEALTHY("不健康", "存在影响功能的问题，需要立即处理"),
    
    /**
     * 严重 - 系统功能严重受损
     */
    CRITICAL("严重", "系统功能严重受损，需要紧急处理");
    
    private final String displayName;
    private final String description;
    
    OverallHealth(String displayName, String description) {
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
     * 根据问题严重程度确定整体健康状态
     */
    public static OverallHealth fromIssues(java.util.List<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            return HEALTHY;
        }
        
        boolean hasCritical = issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.CRITICAL);
        if (hasCritical) {
            return CRITICAL;
        }
        
        boolean hasHigh = issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.HIGH);
        if (hasHigh) {
            return UNHEALTHY;
        }
        
        boolean hasMedium = issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.MEDIUM);
        if (hasMedium) {
            return WARNING;
        }
        
        return HEALTHY;
    }
}