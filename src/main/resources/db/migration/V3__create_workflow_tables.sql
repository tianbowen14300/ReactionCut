-- =====================================================
-- 工作流系统数据表创建脚本
-- Version: V3
-- Description: 创建工作流相关数据表
-- =====================================================

-- 1. 工作流实例表 (workflow_instances)
-- 存储每个工作流实例的基本信息和状态
CREATE TABLE `workflow_instances` (
    `instance_id` CHAR(36) NOT NULL COMMENT '工作流实例ID (UUID)',
    `task_id` CHAR(36) NOT NULL COMMENT '关联的任务ID',
    `workflow_type` VARCHAR(50) NOT NULL DEFAULT 'VIDEO_SUBMISSION' COMMENT '工作流类型',
    `status` ENUM('PENDING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '工作流状态',
    `current_step` VARCHAR(50) DEFAULT NULL COMMENT '当前执行步骤',
    `progress` DECIMAL(5,2) DEFAULT 0.00 COMMENT '整体进度百分比 (0.00-100.00)',
    `configuration_id` BIGINT DEFAULT NULL COMMENT '关联的配置ID',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `started_at` TIMESTAMP NULL DEFAULT NULL COMMENT '开始执行时间',
    `completed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '完成时间',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`instance_id`),
    
    -- 外键约束
    CONSTRAINT `fk_workflow_instances_task` 
        FOREIGN KEY (`task_id`) REFERENCES `submission_task`(`task_id`) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    
    -- 索引优化
    KEY `idx_workflow_instances_task_id` (`task_id`),
    KEY `idx_workflow_instances_status` (`status`),
    KEY `idx_workflow_instances_type` (`workflow_type`),
    KEY `idx_workflow_instances_created_at` (`created_at`),
    KEY `idx_workflow_instances_current_step` (`current_step`)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流实例表 - 存储工作流实例的基本信息和执行状态';

-- 2. 工作流步骤表 (workflow_steps)
-- 存储工作流中每个步骤的详细执行信息
CREATE TABLE `workflow_steps` (
    `step_id` CHAR(36) NOT NULL COMMENT '步骤ID (UUID)',
    `instance_id` CHAR(36) NOT NULL COMMENT '工作流实例ID',
    `step_name` VARCHAR(50) NOT NULL COMMENT '步骤名称',
    `step_type` ENUM('CLIPPING', 'MERGING', 'SEGMENTING', 'UPLOADING', 'VALIDATION', 'CLEANUP') NOT NULL COMMENT '步骤类型',
    `step_order` INT NOT NULL COMMENT '步骤执行顺序',
    `status` ENUM('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '步骤状态',
    `progress` DECIMAL(5,2) DEFAULT 0.00 COMMENT '步骤进度百分比 (0.00-100.00)',
    `input_data` JSON DEFAULT NULL COMMENT '步骤输入数据 (JSON格式)',
    `output_data` JSON DEFAULT NULL COMMENT '步骤输出数据 (JSON格式)',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `max_retries` INT DEFAULT 3 COMMENT '最大重试次数',
    `started_at` TIMESTAMP NULL DEFAULT NULL COMMENT '步骤开始时间',
    `completed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '步骤完成时间',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`step_id`),
    
    -- 外键约束
    CONSTRAINT `fk_workflow_steps_instance` 
        FOREIGN KEY (`instance_id`) REFERENCES `workflow_instances`(`instance_id`) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    
    -- 唯一约束：确保同一工作流实例中步骤顺序唯一
    UNIQUE KEY `uk_workflow_steps_instance_order` (`instance_id`, `step_order`),
    
    -- 索引优化
    KEY `idx_workflow_steps_instance_id` (`instance_id`),
    KEY `idx_workflow_steps_status` (`status`),
    KEY `idx_workflow_steps_type` (`step_type`),
    KEY `idx_workflow_steps_name` (`step_name`),
    KEY `idx_workflow_steps_order` (`step_order`),
    KEY `idx_workflow_steps_created_at` (`created_at`)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流步骤表 - 存储工作流中每个步骤的详细执行信息';

-- 3. 工作流配置表 (workflow_configurations)
-- 存储工作流配置模板和用户自定义配置
CREATE TABLE `workflow_configurations` (
    `config_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称',
    `config_type` ENUM('SYSTEM_DEFAULT', 'USER_TEMPLATE', 'INSTANCE_SPECIFIC') NOT NULL DEFAULT 'INSTANCE_SPECIFIC' COMMENT '配置类型',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID (用户模板时使用)',
    `workflow_type` VARCHAR(50) NOT NULL DEFAULT 'VIDEO_SUBMISSION' COMMENT '工作流类型',
    `configuration_data` JSON NOT NULL COMMENT '配置数据 (JSON格式)',
    `description` TEXT DEFAULT NULL COMMENT '配置描述',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否激活',
    `version` INT NOT NULL DEFAULT 1 COMMENT '配置版本',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建者用户ID',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`config_id`),
    
    -- 外键约束
    CONSTRAINT `fk_workflow_configurations_user` 
        FOREIGN KEY (`user_id`) REFERENCES `login_info`(`user_id`) 
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_workflow_configurations_creator` 
        FOREIGN KEY (`created_by`) REFERENCES `login_info`(`user_id`) 
        ON DELETE SET NULL ON UPDATE CASCADE,
    
    -- 唯一约束：确保用户模板名称唯一
    UNIQUE KEY `uk_workflow_configurations_user_name` (`user_id`, `config_name`, `config_type`),
    
    -- 索引优化
    KEY `idx_workflow_configurations_user_id` (`user_id`),
    KEY `idx_workflow_configurations_type` (`config_type`),
    KEY `idx_workflow_configurations_workflow_type` (`workflow_type`),
    KEY `idx_workflow_configurations_active` (`is_active`),
    KEY `idx_workflow_configurations_created_at` (`created_at`),
    KEY `idx_workflow_configurations_name` (`config_name`)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流配置表 - 存储工作流配置模板和用户自定义配置';

-- 4. 工作流执行日志表 (workflow_execution_logs)
-- 存储工作流执行过程中的详细日志信息
CREATE TABLE `workflow_execution_logs` (
    `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `instance_id` CHAR(36) NOT NULL COMMENT '工作流实例ID',
    `step_id` CHAR(36) DEFAULT NULL COMMENT '步骤ID (可选)',
    `log_level` ENUM('DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL') NOT NULL DEFAULT 'INFO' COMMENT '日志级别',
    `log_message` TEXT NOT NULL COMMENT '日志消息',
    `log_data` JSON DEFAULT NULL COMMENT '附加日志数据 (JSON格式)',
    `source_component` VARCHAR(100) DEFAULT NULL COMMENT '日志来源组件',
    `execution_context` JSON DEFAULT NULL COMMENT '执行上下文信息',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (`log_id`),
    
    -- 外键约束
    CONSTRAINT `fk_workflow_logs_instance` 
        FOREIGN KEY (`instance_id`) REFERENCES `workflow_instances`(`instance_id`) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_workflow_logs_step` 
        FOREIGN KEY (`step_id`) REFERENCES `workflow_steps`(`step_id`) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    
    -- 索引优化
    KEY `idx_workflow_logs_instance_id` (`instance_id`),
    KEY `idx_workflow_logs_step_id` (`step_id`),
    KEY `idx_workflow_logs_level` (`log_level`),
    KEY `idx_workflow_logs_created_at` (`created_at`),
    KEY `idx_workflow_logs_component` (`source_component`)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流执行日志表 - 存储工作流执行过程中的详细日志信息';

-- 5. 工作流性能指标表 (workflow_performance_metrics)
-- 存储工作流执行的性能指标数据
CREATE TABLE `workflow_performance_metrics` (
    `metric_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '指标ID',
    `instance_id` CHAR(36) NOT NULL COMMENT '工作流实例ID',
    `step_id` CHAR(36) DEFAULT NULL COMMENT '步骤ID (可选)',
    `metric_name` VARCHAR(100) NOT NULL COMMENT '指标名称',
    `metric_value` DECIMAL(15,4) NOT NULL COMMENT '指标值',
    `metric_unit` VARCHAR(20) DEFAULT NULL COMMENT '指标单位',
    `metric_type` ENUM('DURATION', 'THROUGHPUT', 'RESOURCE_USAGE', 'COUNT', 'PERCENTAGE') NOT NULL COMMENT '指标类型',
    `measurement_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '测量时间',
    `additional_data` JSON DEFAULT NULL COMMENT '附加指标数据',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (`metric_id`),
    
    -- 外键约束
    CONSTRAINT `fk_workflow_metrics_instance` 
        FOREIGN KEY (`instance_id`) REFERENCES `workflow_instances`(`instance_id`) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_workflow_metrics_step` 
        FOREIGN KEY (`step_id`) REFERENCES `workflow_steps`(`step_id`) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    
    -- 索引优化
    KEY `idx_workflow_metrics_instance_id` (`instance_id`),
    KEY `idx_workflow_metrics_step_id` (`step_id`),
    KEY `idx_workflow_metrics_name` (`metric_name`),
    KEY `idx_workflow_metrics_type` (`metric_type`),
    KEY `idx_workflow_metrics_measurement_time` (`measurement_time`),
    KEY `idx_workflow_metrics_created_at` (`created_at`)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流性能指标表 - 存储工作流执行的性能指标数据';

-- 6. 添加外键约束到现有表
-- 为workflow_instances表添加配置外键约束
ALTER TABLE `workflow_instances` 
ADD CONSTRAINT `fk_workflow_instances_config` 
    FOREIGN KEY (`configuration_id`) REFERENCES `workflow_configurations`(`config_id`) 
    ON DELETE SET NULL ON UPDATE CASCADE;

-- =====================================================
-- 初始化系统默认配置
-- =====================================================

-- 插入系统默认配置 - 下载+投稿工作流
INSERT INTO `workflow_configurations` (
    `config_name`, 
    `config_type`, 
    `workflow_type`, 
    `configuration_data`, 
    `description`, 
    `version`
) VALUES (
    'Default Download+Submission Workflow',
    'SYSTEM_DEFAULT',
    'VIDEO_SUBMISSION',
    JSON_OBJECT(
        'enableDirectSubmission', true,
        'enableClipping', true,
        'enableMerging', true,
        'enableSegmentation', false,
        'segmentationConfig', JSON_OBJECT(
            'segmentDurationSeconds', 133,
            'maxSegments', 50,
            'enableSegmentation', false
        ),
        'retainOriginalFiles', true,
        'processingPriority', 'NORMAL',
        'maxRetries', 3,
        'timeoutMinutes', 30
    ),
    '系统默认配置 - 下载+投稿工作流，默认启用直接投稿模式',
    1
);

-- 插入系统默认配置 - 投稿任务工作流
INSERT INTO `workflow_configurations` (
    `config_name`, 
    `config_type`, 
    `workflow_type`, 
    `configuration_data`, 
    `description`, 
    `version`
) VALUES (
    'Default Submission Task Workflow',
    'SYSTEM_DEFAULT',
    'VIDEO_SUBMISSION',
    JSON_OBJECT(
        'enableDirectSubmission', false,
        'enableClipping', true,
        'enableMerging', true,
        'enableSegmentation', true,
        'segmentationConfig', JSON_OBJECT(
            'segmentDurationSeconds', 133,
            'maxSegments', 50,
            'enableSegmentation', true
        ),
        'retainOriginalFiles', true,
        'processingPriority', 'NORMAL',
        'maxRetries', 3,
        'timeoutMinutes', 60
    ),
    '系统默认配置 - 投稿任务工作流，默认启用分段处理模式',
    1
);

-- =====================================================
-- 创建视图和存储过程
-- =====================================================

-- 创建工作流状态概览视图
CREATE VIEW `workflow_status_overview` AS
SELECT 
    wi.instance_id,
    wi.task_id,
    wi.workflow_type,
    wi.status as workflow_status,
    wi.current_step,
    wi.progress as workflow_progress,
    wi.started_at,
    wi.completed_at,
    TIMESTAMPDIFF(SECOND, wi.started_at, COALESCE(wi.completed_at, NOW())) as execution_duration_seconds,
    st.title as task_title,
    st.status as task_status,
    wc.config_name,
    COUNT(ws.step_id) as total_steps,
    SUM(CASE WHEN ws.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_steps,
    SUM(CASE WHEN ws.status = 'FAILED' THEN 1 ELSE 0 END) as failed_steps
FROM workflow_instances wi
LEFT JOIN submission_task st ON wi.task_id = st.task_id
LEFT JOIN workflow_configurations wc ON wi.configuration_id = wc.config_id
LEFT JOIN workflow_steps ws ON wi.instance_id = ws.instance_id
GROUP BY wi.instance_id, wi.task_id, wi.workflow_type, wi.status, wi.current_step, 
         wi.progress, wi.started_at, wi.completed_at, st.title, st.status, wc.config_name;

-- 创建工作流步骤详情视图
CREATE VIEW `workflow_step_details` AS
SELECT 
    ws.step_id,
    ws.instance_id,
    ws.step_name,
    ws.step_type,
    ws.step_order,
    ws.status as step_status,
    ws.progress as step_progress,
    ws.retry_count,
    ws.max_retries,
    ws.started_at,
    ws.completed_at,
    TIMESTAMPDIFF(SECOND, ws.started_at, COALESCE(ws.completed_at, NOW())) as step_duration_seconds,
    ws.error_message,
    wi.task_id,
    wi.workflow_type,
    wi.status as workflow_status
FROM workflow_steps ws
JOIN workflow_instances wi ON ws.instance_id = wi.instance_id
ORDER BY ws.instance_id, ws.step_order;

-- =====================================================
-- 创建索引优化查询性能
-- =====================================================

-- 复合索引优化常用查询
CREATE INDEX `idx_workflow_instances_status_type` ON `workflow_instances` (`status`, `workflow_type`);
CREATE INDEX `idx_workflow_steps_instance_status` ON `workflow_steps` (`instance_id`, `status`);
CREATE INDEX `idx_workflow_logs_instance_level_time` ON `workflow_execution_logs` (`instance_id`, `log_level`, `created_at`);
CREATE INDEX `idx_workflow_metrics_instance_name_time` ON `workflow_performance_metrics` (`instance_id`, `metric_name`, `measurement_time`);

-- =====================================================
-- 数据库表创建完成
-- =====================================================