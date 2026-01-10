package com.tbw.cut.diagnostic.model;

/**
 * 工作流阶段枚举
 */
public enum WorkflowStage {
    
    /**
     * 初始化阶段
     */
    INITIALIZATION("初始化", "工作流初始化阶段"),
    
    /**
     * 下载阶段
     */
    DOWNLOADING("下载中", "视频下载阶段"),
    
    /**
     * 下载完成
     */
    DOWNLOAD_COMPLETED("下载完成", "视频下载完成"),
    
    /**
     * 视频处理阶段
     */
    VIDEO_PROCESSING("视频处理", "视频剪辑、合并、分段处理阶段"),
    
    /**
     * 准备投稿
     */
    PREPARING_SUBMISSION("准备投稿", "准备投稿材料阶段"),
    
    /**
     * 投稿中
     */
    SUBMITTING("投稿中", "视频投稿上传阶段"),
    
    /**
     * 投稿完成
     */
    SUBMISSION_COMPLETED("投稿完成", "投稿流程完成"),
    
    /**
     * 失败
     */
    FAILED("失败", "工作流执行失败"),
    
    /**
     * 已取消
     */
    CANCELLED("已取消", "工作流被取消"),
    
    /**
     * 暂停
     */
    PAUSED("暂停", "工作流暂停执行");
    
    private final String displayName;
    private final String description;
    
    WorkflowStage(String displayName, String description) {
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
     * 判断是否为终止状态
     */
    public boolean isTerminalState() {
        return this == SUBMISSION_COMPLETED || this == FAILED || this == CANCELLED;
    }
    
    /**
     * 判断是否为活跃状态
     */
    public boolean isActiveState() {
        return this == DOWNLOADING || this == VIDEO_PROCESSING || 
               this == PREPARING_SUBMISSION || this == SUBMITTING;
    }
}