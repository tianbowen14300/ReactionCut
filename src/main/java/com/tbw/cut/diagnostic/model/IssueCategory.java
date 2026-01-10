package com.tbw.cut.diagnostic.model;

/**
 * 问题分类枚举
 */
public enum IssueCategory {
    
    /**
     * 事件处理问题
     */
    EVENT_PROCESSING("事件处理", "事件发布、监听、处理相关问题"),
    
    /**
     * 工作流执行问题
     */
    WORKFLOW_EXECUTION("工作流执行", "工作流步骤执行、状态转换相关问题"),
    
    /**
     * 任务关联问题
     */
    TASK_RELATION("任务关联", "下载任务和投稿任务关联相关问题"),
    
    /**
     * 数据一致性问题
     */
    DATA_CONSISTENCY("数据一致性", "数据库状态不一致相关问题"),
    
    /**
     * 性能问题
     */
    PERFORMANCE("性能", "系统性能、资源使用相关问题"),
    
    /**
     * 配置问题
     */
    CONFIGURATION("配置", "系统配置、参数设置相关问题"),
    
    /**
     * 外部依赖问题
     */
    EXTERNAL_DEPENDENCY("外部依赖", "外部服务、API调用相关问题"),
    
    /**
     * 文件系统问题
     */
    FILE_SYSTEM("文件系统", "文件路径、存储相关问题"),
    
    /**
     * 网络连接问题
     */
    NETWORK("网络连接", "网络连接、通信相关问题"),
    
    /**
     * 资源不足问题
     */
    RESOURCE_SHORTAGE("资源不足", "CPU、内存、磁盘空间不足相关问题");
    
    private final String displayName;
    private final String description;
    
    IssueCategory(String displayName, String description) {
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