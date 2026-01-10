-- V4: 为task_relations表添加工作流相关列
-- 用于支持工作流时序修复功能

-- 添加工作流实例ID列
ALTER TABLE `task_relations` 
ADD COLUMN `workflow_instance_id` VARCHAR(100) DEFAULT NULL COMMENT '工作流实例ID' AFTER `updated_at`;

-- 添加工作流状态列
ALTER TABLE `task_relations` 
ADD COLUMN `workflow_status` ENUM('PENDING_DOWNLOAD', 'WORKFLOW_STARTED', 'WORKFLOW_RUNNING', 'WORKFLOW_COMPLETED', 'WORKFLOW_FAILED', 'WORKFLOW_STARTUP_FAILED') DEFAULT 'PENDING_DOWNLOAD' COMMENT '工作流状态' AFTER `workflow_instance_id`;

-- 添加工作流启动时间列
ALTER TABLE `task_relations` 
ADD COLUMN `workflow_started_at` TIMESTAMP NULL DEFAULT NULL COMMENT '工作流启动时间' AFTER `workflow_status`;

-- 添加最后错误信息列
ALTER TABLE `task_relations` 
ADD COLUMN `last_error_message` TEXT DEFAULT NULL COMMENT '最后错误信息' AFTER `workflow_started_at`;

-- 添加重试次数列
ALTER TABLE `task_relations` 
ADD COLUMN `retry_count` INT DEFAULT 0 COMMENT '重试次数' AFTER `last_error_message`;

-- 添加工作流状态索引以优化查询
CREATE INDEX `idx_task_relations_workflow_status` ON `task_relations` (`workflow_status`);

-- 添加工作流实例ID索引
CREATE INDEX `idx_task_relations_workflow_instance_id` ON `task_relations` (`workflow_instance_id`);
