package com.tbw.cut.diagnostic.model;

/**
 * 建议优先级枚举
 */
public enum RecommendationPriority {
    
    /**
     * 低优先级
     */
    LOW("低", "可以稍后处理", 1),
    
    /**
     * 中等优先级
     */
    MEDIUM("中", "建议尽快处理", 2),
    
    /**
     * 高优先级
     */
    HIGH("高", "需要优先处理", 3),
    
    /**
     * 紧急
     */
    URGENT("紧急", "需要立即处理", 4);
    
    private final String displayName;
    private final String description;
    private final int level;
    
    RecommendationPriority(String displayName, String description, int level) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
}