-- 视频下载与投稿功能集成 - 数据库迁移脚本
-- 版本: V2
-- 描述: 添加任务关联表和扩展投稿任务状态

-- 1. 创建任务关联表
CREATE TABLE `task_relations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `download_task_id` BIGINT NOT NULL COMMENT '下载任务ID',
    `submission_task_id` CHAR(36) NOT NULL COMMENT '投稿任务ID',
    `relation_type` ENUM('INTEGRATED', 'MANUAL') NOT NULL DEFAULT 'INTEGRATED' COMMENT '关联类型：INTEGRATED-集成创建，MANUAL-手动关联',
    `status` ENUM('ACTIVE', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'ACTIVE' COMMENT '关联状态：ACTIVE-活跃，COMPLETED-已完成，FAILED-失败',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`),
    
    -- 外键约束
    CONSTRAINT `fk_task_relations_download` 
        FOREIGN KEY (`download_task_id`) REFERENCES `video_download`(`id`) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_task_relations_submission` 
        FOREIGN KEY (`submission_task_id`) REFERENCES `submission_task`(`task_id`) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    
    -- 唯一约束：确保一个下载任务只能关联一个投稿任务
    UNIQUE KEY `uk_download_submission` (`download_task_id`, `submission_task_id`),
    
    -- 索引优化
    KEY `idx_task_relations_download_id` (`download_task_id`),
    KEY `idx_task_relations_submission_id` (`submission_task_id`),
    KEY `idx_task_relations_status` (`status`),
    KEY `idx_task_relations_created_at` (`created_at`)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务关联表 - 管理下载任务与投稿任务的关联关系';

-- 2. 扩展submission_task表的status枚举，添加WAITING_DOWNLOAD状态
ALTER TABLE `submission_task` 
MODIFY COLUMN `status` ENUM(
    'PENDING',           -- 待处理
    'CLIPPING',          -- 剪辑中
    'MERGING',           -- 合并中
    'SEGMENTING',        -- 分段中
    'UPLOADING',         -- 上传中
    'COMPLETED',         -- 已完成
    'FAILED',            -- 失败
    'WAITING_DOWNLOAD'   -- 等待下载完成（新增状态）
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务状态';

-- 3. 为现有数据添加索引优化（如果需要）
-- 为video_download表添加更多索引以优化关联查询
ALTER TABLE `video_download` 
ADD INDEX `idx_video_download_bvid` (`bvid`),
ADD INDEX `idx_video_download_create_time` (`create_time`);

-- 4. 添加注释说明
ALTER TABLE `video_download` COMMENT = '视频下载记录表 - 支持与投稿任务关联';
ALTER TABLE `submission_task` COMMENT = '投稿任务表 - 支持与下载任务关联';