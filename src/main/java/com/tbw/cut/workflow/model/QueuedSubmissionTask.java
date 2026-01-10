package com.tbw.cut.workflow.model;

import com.tbw.cut.entity.QueueTaskStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 队列中的投稿任务
 * 扩展原有QueuedTask，增加优先级和工作流集成支持
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueuedSubmissionTask {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 队列ID
     */
    private String queueId;
    
    /**
     * 工作流实例ID（如果是从工作流触发的）
     */
    private String workflowInstanceId;
    
    /**
     * 任务优先级（数值越小优先级越高）
     */
    @Builder.Default
    private int priority = Priority.NORMAL.getValue();
    
    /**
     * 是否由工作流触发
     */
    @Builder.Default
    private boolean workflowTriggered = false;
    
    /**
     * 入队时间
     */
    private LocalDateTime queuedAt;
    
    /**
     * 队列位置
     */
    private int position;
    
    /**
     * 任务状态
     */
    @Builder.Default
    private QueueTaskStatus status = QueueTaskStatus.QUEUED;
    
    /**
     * 重试次数
     */
    @Builder.Default
    private int retryCount = 0;
    
    /**
     * 最大重试次数
     */
    @Builder.Default
    private int maxRetries = 3;
    
    /**
     * 最后重试时间
     */
    private LocalDateTime lastRetryAt;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 开始处理时间
     */
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
    
    /**
     * 预估处理时间（分钟）
     */
    @Builder.Default
    private int estimatedProcessingMinutes = 10;
    
    /**
     * 任务标签（用于分类和过滤）
     */
    private String tags;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 优先级枚举
     */
    public enum Priority {
        URGENT(1, "紧急"),
        HIGH(2, "高"),
        NORMAL(5, "普通"),
        LOW(8, "低"),
        BACKGROUND(10, "后台");
        
        private final int value;
        private final String description;
        
        Priority(int value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public int getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static Priority fromValue(int value) {
            for (Priority priority : values()) {
                if (priority.value == value) {
                    return priority;
                }
            }
            return NORMAL; // 默认返回普通优先级
        }
    }
    
    /**
     * 创建工作流触发的任务
     */
    public static QueuedSubmissionTask createFromWorkflow(String taskId, String workflowInstanceId) {
        return QueuedSubmissionTask.builder()
                .taskId(taskId)
                .workflowInstanceId(workflowInstanceId)
                .workflowTriggered(true)
                .priority(Priority.NORMAL.getValue())
                .queuedAt(LocalDateTime.now())
                .description("工作流自动触发的投稿任务")
                .build();
    }
    
    /**
     * 创建高优先级任务
     */
    public static QueuedSubmissionTask createHighPriority(String taskId) {
        return QueuedSubmissionTask.builder()
                .taskId(taskId)
                .priority(Priority.HIGH.getValue())
                .queuedAt(LocalDateTime.now())
                .description("高优先级投稿任务")
                .build();
    }
    
    /**
     * 创建普通任务
     */
    public static QueuedSubmissionTask createNormal(String taskId) {
        return QueuedSubmissionTask.builder()
                .taskId(taskId)
                .priority(Priority.NORMAL.getValue())
                .queuedAt(LocalDateTime.now())
                .description("普通投稿任务")
                .build();
    }
    
    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return retryCount < maxRetries && 
               (status == QueueTaskStatus.FAILED || status == QueueTaskStatus.PAUSED);
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
        this.status = QueueTaskStatus.QUEUED; // 重新排队
    }
    
    /**
     * 标记任务开始处理
     */
    public void markStarted() {
        this.status = QueueTaskStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
    }
    
    /**
     * 标记任务完成
     */
    public void markCompleted() {
        this.status = QueueTaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 标记任务失败
     */
    public void markFailed(String errorMessage) {
        this.status = QueueTaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 获取处理时长（分钟）
     */
    public long getProcessingDurationMinutes() {
        if (startedAt == null || completedAt == null) {
            return 0;
        }
        return java.time.Duration.between(startedAt, completedAt).toMinutes();
    }
    
    /**
     * 获取等待时长（分钟）
     */
    public long getWaitingDurationMinutes() {
        if (queuedAt == null) {
            return 0;
        }
        LocalDateTime endTime = startedAt != null ? startedAt : LocalDateTime.now();
        return java.time.Duration.between(queuedAt, endTime).toMinutes();
    }
    
    /**
     * 获取优先级描述
     */
    public String getPriorityDescription() {
        return Priority.fromValue(priority).getDescription();
    }
    
    /**
     * 检查是否为高优先级任务
     */
    public boolean isHighPriority() {
        return priority <= Priority.HIGH.getValue();
    }
    
    /**
     * 检查是否为低优先级任务
     */
    public boolean isLowPriority() {
        return priority >= Priority.LOW.getValue();
    }
}