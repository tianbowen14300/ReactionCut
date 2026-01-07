-- 添加重试相关字段到video_download表
ALTER TABLE video_download 
ADD COLUMN retry_count INT DEFAULT 0 COMMENT '重试次数',
ADD COLUMN last_retry_time DATETIME COMMENT '最后一次重试时间',
ADD COLUMN last_error_message TEXT COMMENT '最后一次错误信息';

-- 更新现有记录的重试次数为0
UPDATE video_download SET retry_count = 0 WHERE retry_count IS NULL;