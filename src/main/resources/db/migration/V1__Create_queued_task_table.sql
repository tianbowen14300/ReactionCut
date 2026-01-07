-- 创建队列任务表
CREATE TABLE IF NOT EXISTS queued_task (
    queue_id VARCHAR(64) PRIMARY KEY COMMENT '队列ID',
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    queued_at DATETIME NOT NULL COMMENT '入队时间',
    position INT NOT NULL COMMENT '队列位置',
    status ENUM('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'QUEUED' COMMENT '队列任务状态',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    last_retry_at DATETIME NULL COMMENT '最后重试时间',
    error_message TEXT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_task_id (task_id),
    INDEX idx_status_position (status, position),
    INDEX idx_queued_at (queued_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='队列任务表';