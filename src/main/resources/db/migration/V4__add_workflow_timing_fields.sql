-- 工作流时序修复 - 数据库表结构调整
-- 为task_relations表添加工作流状态相关字段

-- 添加工作流实例ID字段
ALTER TABLE task_relations ADD COLUMN workflow_instance_id VARCHAR(255) COMMENT '工作流实例ID';

-- 添加工作流状态字段，默认值为等待下载完成
ALTER TABLE task_relations ADD COLUMN workflow_status VARCHAR(50) DEFAULT 'PENDING_DOWNLOAD' COMMENT '工作流状态';

-- 添加工作流启动时间字段
ALTER TABLE task_relations ADD COLUMN workflow_started_at TIMESTAMP NULL COMMENT '工作流启动时间';

-- 添加最后错误信息字段
ALTER TABLE task_relations ADD COLUMN last_error_message TEXT NULL COMMENT '最后错误信息';

-- 添加重试计数字段
ALTER TABLE task_relations ADD COLUMN retry_count INT DEFAULT 0 COMMENT '重试次数';

-- 添加创建时间和更新时间字段（如果不存在）
ALTER TABLE task_relations ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE task_relations ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 为工作流状态字段添加索引以提高查询性能
CREATE INDEX idx_task_relations_workflow_status ON task_relations(workflow_status);

-- 为工作流实例ID字段添加索引
CREATE INDEX idx_task_relations_workflow_instance_id ON task_relations(workflow_instance_id);

-- 为下载任务ID和投稿任务ID的组合添加唯一索引（如果不存在）
CREATE UNIQUE INDEX idx_task_relations_download_submission 
ON task_relations(download_task_id, submission_task_id);

-- 添加工作流状态的检查约束
ALTER TABLE task_relations ADD CONSTRAINT chk_workflow_status 
CHECK (workflow_status IN (
    'PENDING_DOWNLOAD',      -- 等待下载完成
    'WORKFLOW_STARTED',      -- 工作流已启动
    'WORKFLOW_RUNNING',      -- 工作流运行中
    'WORKFLOW_COMPLETED',    -- 工作流已完成
    'WORKFLOW_FAILED',       -- 工作流失败
    'WORKFLOW_STARTUP_FAILED' -- 工作流启动失败
));

-- 为现有记录设置默认状态
-- 如果已有工作流实例ID，则标记为已完成
UPDATE task_relations 
SET workflow_status = 'WORKFLOW_COMPLETED' 
WHERE workflow_instance_id IS NOT NULL AND workflow_instance_id != '';

-- 如果没有工作流实例ID，则标记为等待下载完成
UPDATE task_relations 
SET workflow_status = 'PENDING_DOWNLOAD' 
WHERE workflow_instance_id IS NULL OR workflow_instance_id = '';

-- 添加注释说明
ALTER TABLE task_relations COMMENT = '任务关联表 - 包含工作流时序修复相关字段';