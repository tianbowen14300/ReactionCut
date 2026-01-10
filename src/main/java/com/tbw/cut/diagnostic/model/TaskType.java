package com.tbw.cut.diagnostic.model;

/**
 * 任务类型枚举
 */
public enum TaskType {
    
    /**
     * 下载任务
     */
    DOWNLOAD_TASK("下载任务", "视频下载任务"),
    
    /**
     * 投稿任务
     */
    SUBMISSION_TASK("投稿任务", "视频投稿任务"),
    
    /**
     * 工作流任务
     */
    WORKFLOW_TASK("工作流任务", "工作流执行任务"),
    
    /**
     * 处理任务
     */
    PROCESSING_TASK("处理任务", "视频处理任务"),
    
    /**
     * 分段任务
     */
    SEGMENTATION_TASK("分段任务", "视频分段任务"),
    
    /**
     * 合并任务
     */
    MERGE_TASK("合并任务", "视频合并任务");
    
    private final String displayName;
    private final String description;
    
    TaskType(String displayName, String description) {
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