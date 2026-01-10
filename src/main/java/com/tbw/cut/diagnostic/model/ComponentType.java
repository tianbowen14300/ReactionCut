package com.tbw.cut.diagnostic.model;

/**
 * 组件类型枚举
 */
public enum ComponentType {
    
    /**
     * 事件发布器
     */
    EVENT_PUBLISHER("事件发布器"),
    
    /**
     * 事件监听器
     */
    EVENT_LISTENER("事件监听器"),
    
    /**
     * 工作流引擎
     */
    WORKFLOW_ENGINE("工作流引擎"),
    
    /**
     * 状态同步服务
     */
    STATUS_SYNC_SERVICE("状态同步服务"),
    
    /**
     * 任务关联服务
     */
    TASK_RELATION_SERVICE("任务关联服务"),
    
    /**
     * 下载服务
     */
    DOWNLOAD_SERVICE("下载服务"),
    
    /**
     * 视频处理服务
     */
    VIDEO_PROCESS_SERVICE("视频处理服务"),
    
    /**
     * 投稿服务
     */
    SUBMISSION_SERVICE("投稿服务"),
    
    /**
     * 数据库连接
     */
    DATABASE("数据库连接"),
    
    /**
     * 文件系统
     */
    FILE_SYSTEM("文件系统"),
    
    /**
     * 外部API
     */
    EXTERNAL_API("外部API"),
    
    /**
     * 消息队列
     */
    MESSAGE_QUEUE("消息队列"),
    
    /**
     * 缓存系统
     */
    CACHE_SYSTEM("缓存系统");
    
    private final String displayName;
    
    ComponentType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}