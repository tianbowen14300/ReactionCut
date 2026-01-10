package com.tbw.cut.diagnostic.model;

/**
 * 事件步骤类型枚举
 */
public enum EventStepType {
    
    /**
     * 事件发布
     */
    EVENT_PUBLISH("事件发布", "发布事件到事件总线"),
    
    /**
     * 事件接收
     */
    EVENT_RECEIVE("事件接收", "从事件总线接收事件"),
    
    /**
     * 事件处理
     */
    EVENT_PROCESS("事件处理", "处理接收到的事件"),
    
    /**
     * 状态更新
     */
    STATUS_UPDATE("状态更新", "更新任务或工作流状态"),
    
    /**
     * 数据库操作
     */
    DATABASE_OPERATION("数据库操作", "执行数据库读写操作"),
    
    /**
     * 服务调用
     */
    SERVICE_CALL("服务调用", "调用其他服务接口"),
    
    /**
     * 工作流触发
     */
    WORKFLOW_TRIGGER("工作流触发", "触发工作流执行"),
    
    /**
     * 错误处理
     */
    ERROR_HANDLING("错误处理", "处理执行过程中的错误");
    
    private final String displayName;
    private final String description;
    
    EventStepType(String displayName, String description) {
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