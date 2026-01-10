package com.tbw.cut.diagnostic.model;

/**
 * 任务关联问题类型枚举
 */
public enum TaskRelationIssueType {
    
    /**
     * 孤立的下载任务
     */
    ORPHANED_DOWNLOAD_TASK("孤立的下载任务", "下载任务没有对应的投稿任务关联"),
    
    /**
     * 孤立的投稿任务
     */
    ORPHANED_SUBMISSION_TASK("孤立的投稿任务", "投稿任务没有对应的下载任务关联"),
    
    /**
     * 关联记录缺失
     */
    MISSING_RELATION_RECORD("关联记录缺失", "task_relations表中缺少关联记录"),
    
    /**
     * 无效的关联记录
     */
    INVALID_RELATION_RECORD("无效的关联记录", "关联记录指向不存在的任务"),
    
    /**
     * 重复的关联记录
     */
    DUPLICATE_RELATION_RECORD("重复的关联记录", "存在重复的任务关联记录"),
    
    /**
     * 关联状态不一致
     */
    INCONSISTENT_RELATION_STATUS("关联状态不一致", "关联任务的状态不一致"),
    
    /**
     * 多分P关联不完整
     */
    INCOMPLETE_MULTIPART_RELATION("多分P关联不完整", "多分P下载的关联记录不完整");
    
    private final String displayName;
    private final String description;
    
    TaskRelationIssueType(String displayName, String description) {
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