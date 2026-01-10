package com.tbw.cut.diagnostic.model;

/**
 * 修复类型枚举
 */
public enum RepairType {
    
    /**
     * 任务关联修复
     */
    TASK_RELATION_REPAIR("任务关联修复", "修复下载任务和投稿任务之间的关联"),
    
    /**
     * 孤立任务修复
     */
    ORPHANED_TASK_REPAIR("孤立任务修复", "修复孤立的任务"),
    
    /**
     * 工作流状态修复
     */
    WORKFLOW_STATUS_REPAIR("工作流状态修复", "修复工作流状态不一致问题"),
    
    /**
     * 数据一致性修复
     */
    DATA_CONSISTENCY_REPAIR("数据一致性修复", "修复数据库状态不一致问题"),
    
    /**
     * 文件路径修复
     */
    FILE_PATH_REPAIR("文件路径修复", "修复文件路径缺失或错误问题"),
    
    /**
     * 事件监听器修复
     */
    EVENT_LISTENER_REPAIR("事件监听器修复", "修复失效的事件监听器"),
    
    /**
     * 多分P关联修复
     */
    MULTIPART_RELATION_REPAIR("多分P关联修复", "修复多分P下载的关联问题"),
    
    /**
     * 卡住工作流修复
     */
    STUCK_WORKFLOW_REPAIR("卡住工作流修复", "修复卡住的工作流");
    
    private final String displayName;
    private final String description;
    
    RepairType(String displayName, String description) {
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