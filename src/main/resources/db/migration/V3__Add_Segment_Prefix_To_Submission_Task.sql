-- 为投稿任务表添加分段前缀字段
ALTER TABLE submission_task 
ADD COLUMN segment_prefix VARCHAR(100) COMMENT '分段前缀';