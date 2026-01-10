package com.tbw.cut.diagnostic.model;

/**
 * 建议类型枚举
 */
public enum RecommendationType {
    
    /**
     * 立即修复
     */
    IMMEDIATE_FIX("立即修复", "需要立即执行的修复操作"),
    
    /**
     * 配置调整
     */
    CONFIGURATION_ADJUSTMENT("配置调整", "调整系统配置参数"),
    
    /**
     * 性能优化
     */
    PERFORMANCE_OPTIMIZATION("性能优化", "提升系统性能的建议"),
    
    /**
     * 预防措施
     */
    PREVENTIVE_MEASURE("预防措施", "防止问题再次发生的措施"),
    
    /**
     * 监控增强
     */
    MONITORING_ENHANCEMENT("监控增强", "增强系统监控能力"),
    
    /**
     * 资源扩容
     */
    RESOURCE_SCALING("资源扩容", "增加系统资源配置"),
    
    /**
     * 代码修复
     */
    CODE_FIX("代码修复", "修复代码逻辑问题"),
    
    /**
     * 数据修复
     */
    DATA_REPAIR("数据修复", "修复数据一致性问题");
    
    private final String displayName;
    private final String description;
    
    RecommendationType(String displayName, String description) {
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