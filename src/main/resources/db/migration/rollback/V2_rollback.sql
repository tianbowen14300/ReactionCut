-- 视频下载与投稿功能集成 - 数据库回滚脚本
-- 版本: V2 回滚
-- 描述: 回滚任务关联表和投稿任务状态扩展

-- 警告：执行此脚本将删除所有任务关联数据，请谨慎操作！

-- 1. 删除task_relations表
DROP TABLE IF EXISTS `task_relations`;

-- 2. 回滚submission_task表的status枚举，移除WAITING_DOWNLOAD状态
-- 注意：如果有数据使用了WAITING_DOWNLOAD状态，需要先处理这些数据
UPDATE `submission_task` SET `status` = 'PENDING' WHERE `status` = 'WAITING_DOWNLOAD';

ALTER TABLE `submission_task` 
MODIFY COLUMN `status` ENUM(
    'PENDING',
    'CLIPPING',
    'MERGING',
    'SEGMENTING',
    'UPLOADING',
    'COMPLETED',
    'FAILED'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务状态';

-- 3. 删除新增的索引
ALTER TABLE `video_download` 
DROP INDEX IF EXISTS `idx_video_download_bvid`,
DROP INDEX IF EXISTS `idx_video_download_create_time`;

-- 4. 恢复原始表注释
ALTER TABLE `video_download` COMMENT = '视频下载记录表';
ALTER TABLE `submission_task` COMMENT = '';

-- 回滚完成提示
SELECT 'V2 migration rollback completed. All task relation data has been removed.' as message;