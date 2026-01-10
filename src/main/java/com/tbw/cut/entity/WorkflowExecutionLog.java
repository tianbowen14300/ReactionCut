package com.tbw.cut.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流执行日志实体类
 * 
 * 对应数据库表：workflow_execution_logs
 */
@Data
public class WorkflowExecutionLog {
    
    /**
     * 日志ID
     */
    private Long logId;
    
    /**
     * 工作流实例ID
     */
    private String instanceId;
    
    /**
     * 步骤ID (可选)
     */
    private String stepId;
    
    /**
     * 日志级别
     */
    private LogLevel logLevel;
    
    /**
     * 日志消息
     */
    private String logMessage;
    
    /**
     * 附加日志数据 (JSON格式)
     */
    private String logData;
    
    /**
     * 日志来源组件
     */
    private String sourceComponent;
    
    /**
     * 执行上下文信息 (JSON格式)
     */
    private String executionContext;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        DEBUG("调试"),
        INFO("信息"),
        WARN("警告"),
        ERROR("错误"),
        FATAL("致命错误");
        
        private final String description;
        
        LogLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    /**
     * 检查是否为错误级别日志
     * 
     * @return true如果是错误级别，false如果不是
     */
    public boolean isErrorLevel() {
        return logLevel == LogLevel.ERROR || logLevel == LogLevel.FATAL;
    }
    
    /**
     * 检查是否为警告级别日志
     * 
     * @return true如果是警告级别，false如果不是
     */
    public boolean isWarningLevel() {
        return logLevel == LogLevel.WARN;
    }
    
    /**
     * 检查是否为信息级别日志
     * 
     * @return true如果是信息级别，false如果不是
     */
    public boolean isInfoLevel() {
        return logLevel == LogLevel.INFO;
    }
    
    /**
     * 检查是否为调试级别日志
     * 
     * @return true如果是调试级别，false如果不是
     */
    public boolean isDebugLevel() {
        return logLevel == LogLevel.DEBUG;
    }
    
    /**
     * 获取日志级别描述
     * 
     * @return 日志级别的中文描述
     */
    public String getLogLevelDescription() {
        return logLevel != null ? logLevel.getDescription() : "未知级别";
    }
    
    /**
     * 检查是否有步骤关联
     * 
     * @return true如果有步骤关联，false如果没有
     */
    public boolean hasStepAssociation() {
        return stepId != null && !stepId.trim().isEmpty();
    }
    
    /**
     * 检查是否有附加数据
     * 
     * @return true如果有附加数据，false如果没有
     */
    public boolean hasLogData() {
        return logData != null && !logData.trim().isEmpty();
    }
    
    /**
     * 检查是否有执行上下文
     * 
     * @return true如果有执行上下文，false如果没有
     */
    public boolean hasExecutionContext() {
        return executionContext != null && !executionContext.trim().isEmpty();
    }
    
    /**
     * 创建调试日志
     * 
     * @param instanceId 实例ID
     * @param message 日志消息
     * @param component 来源组件
     * @return 调试日志实例
     */
    public static WorkflowExecutionLog createDebugLog(String instanceId, String message, String component) {
        WorkflowExecutionLog execLog = new WorkflowExecutionLog();
        execLog.setInstanceId(instanceId);
        execLog.setLogLevel(LogLevel.DEBUG);
        execLog.setLogMessage(message);
        execLog.setSourceComponent(component);
        execLog.setCreatedAt(LocalDateTime.now());
        return execLog;
    }
    
    /**
     * 创建信息日志
     * 
     * @param instanceId 实例ID
     * @param message 日志消息
     * @param component 来源组件
     * @return 信息日志实例
     */
    public static WorkflowExecutionLog createInfoLog(String instanceId, String message, String component) {
        WorkflowExecutionLog execLog = new WorkflowExecutionLog();
        execLog.setInstanceId(instanceId);
        execLog.setLogLevel(LogLevel.INFO);
        execLog.setLogMessage(message);
        execLog.setSourceComponent(component);
        execLog.setCreatedAt(LocalDateTime.now());
        return execLog;
    }
    
    /**
     * 创建警告日志
     * 
     * @param instanceId 实例ID
     * @param message 日志消息
     * @param component 来源组件
     * @return 警告日志实例
     */
    public static WorkflowExecutionLog createWarnLog(String instanceId, String message, String component) {
        WorkflowExecutionLog execLog = new WorkflowExecutionLog();
        execLog.setInstanceId(instanceId);
        execLog.setLogLevel(LogLevel.WARN);
        execLog.setLogMessage(message);
        execLog.setSourceComponent(component);
        execLog.setCreatedAt(LocalDateTime.now());
        return execLog;
    }
    
    /**
     * 创建错误日志
     * 
     * @param instanceId 实例ID
     * @param message 日志消息
     * @param component 来源组件
     * @return 错误日志实例
     */
    public static WorkflowExecutionLog createErrorLog(String instanceId, String message, String component) {
        WorkflowExecutionLog execLog = new WorkflowExecutionLog();
        execLog.setInstanceId(instanceId);
        execLog.setLogLevel(LogLevel.ERROR);
        execLog.setLogMessage(message);
        execLog.setSourceComponent(component);
        execLog.setCreatedAt(LocalDateTime.now());
        return execLog;
    }
    
    /**
     * 创建步骤日志
     * 
     * @param instanceId 实例ID
     * @param stepId 步骤ID
     * @param level 日志级别
     * @param message 日志消息
     * @param component 来源组件
     * @return 步骤日志实例
     */
    public static WorkflowExecutionLog createStepLog(String instanceId, String stepId, LogLevel level, 
                                                    String message, String component) {
        WorkflowExecutionLog execLog = new WorkflowExecutionLog();
        execLog.setInstanceId(instanceId);
        execLog.setStepId(stepId);
        execLog.setLogLevel(level);
        execLog.setLogMessage(message);
        execLog.setSourceComponent(component);
        execLog.setCreatedAt(LocalDateTime.now());
        return execLog;
    }
    
    @Override
    public String toString() {
        return String.format(
            "WorkflowExecutionLog{logId=%d, instanceId='%s', stepId='%s', level=%s, component='%s', message='%s'}",
            logId, instanceId, stepId, logLevel, sourceComponent, 
            logMessage != null && logMessage.length() > 50 ? logMessage.substring(0, 50) + "..." : logMessage
        );
    }
}