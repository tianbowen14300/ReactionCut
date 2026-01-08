-- 视频下载与投稿功能集成 - 数据库迁移验证脚本
-- 版本: V2 验证
-- 描述: 验证任务关联表和投稿任务状态扩展是否成功

-- 1. 验证task_relations表是否创建成功
SELECT 
    'task_relations table exists' as check_name,
    CASE 
        WHEN COUNT(*) > 0 THEN 'PASS' 
        ELSE 'FAIL' 
    END as result
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
AND table_name = 'task_relations';

-- 2. 验证task_relations表的列结构
SELECT 
    'task_relations columns check' as check_name,
    CASE 
        WHEN COUNT(*) = 7 THEN 'PASS' 
        ELSE CONCAT('FAIL - Expected 7 columns, found ', COUNT(*)) 
    END as result
FROM information_schema.columns 
WHERE table_schema = DATABASE() 
AND table_name = 'task_relations'
AND column_name IN ('id', 'download_task_id', 'submission_task_id', 'relation_type', 'status', 'created_at', 'updated_at');

-- 3. 验证外键约束是否创建成功
SELECT 
    'task_relations foreign keys check' as check_name,
    CASE 
        WHEN COUNT(*) = 2 THEN 'PASS' 
        ELSE CONCAT('FAIL - Expected 2 foreign keys, found ', COUNT(*)) 
    END as result
FROM information_schema.key_column_usage 
WHERE table_schema = DATABASE() 
AND table_name = 'task_relations' 
AND referenced_table_name IS NOT NULL;

-- 4. 验证唯一约束是否创建成功
SELECT 
    'task_relations unique constraint check' as check_name,
    CASE 
        WHEN COUNT(*) > 0 THEN 'PASS' 
        ELSE 'FAIL' 
    END as result
FROM information_schema.statistics 
WHERE table_schema = DATABASE() 
AND table_name = 'task_relations' 
AND index_name = 'uk_download_submission';

-- 5. 验证submission_task表的status枚举是否包含WAITING_DOWNLOAD
SELECT 
    'submission_task status enum check' as check_name,
    CASE 
        WHEN column_type LIKE '%WAITING_DOWNLOAD%' THEN 'PASS' 
        ELSE 'FAIL - WAITING_DOWNLOAD not found in enum' 
    END as result
FROM information_schema.columns 
WHERE table_schema = DATABASE() 
AND table_name = 'submission_task' 
AND column_name = 'status';

-- 6. 验证video_download表的新索引是否创建成功
SELECT 
    'video_download indexes check' as check_name,
    CASE 
        WHEN COUNT(*) >= 2 THEN 'PASS' 
        ELSE CONCAT('FAIL - Expected at least 2 new indexes, found ', COUNT(*)) 
    END as result
FROM information_schema.statistics 
WHERE table_schema = DATABASE() 
AND table_name = 'video_download' 
AND index_name IN ('idx_video_download_bvid', 'idx_video_download_create_time');

-- 7. 测试task_relations表的基本操作
-- 插入测试数据（需要确保引用的记录存在）
-- 注意：这个测试需要在有实际数据的环境中运行

-- 8. 显示迁移摘要
SELECT 
    'Migration V2 Summary' as summary,
    'Task relations table created with proper constraints and indexes' as description
UNION ALL
SELECT 
    'Status Extension',
    'submission_task.status enum extended with WAITING_DOWNLOAD'
UNION ALL
SELECT 
    'Index Optimization',
    'Added indexes to video_download table for better query performance';

-- 9. 显示表统计信息
SELECT 
    table_name,
    table_rows,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) as size_mb
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
AND table_name IN ('task_relations', 'video_download', 'submission_task')
ORDER BY table_name;