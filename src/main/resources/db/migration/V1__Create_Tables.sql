-- 创建主播表
CREATE TABLE anchor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    uid VARCHAR(50) NOT NULL UNIQUE COMMENT 'Bilibili主播UID',
    nickname VARCHAR(100) COMMENT '主播昵称',
    live_status TINYINT DEFAULT 0 COMMENT '直播状态 0-未直播 1-直播中',
    last_check_time DATETIME COMMENT '上次检查时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
) COMMENT='主播信息表';

-- 创建视频下载表
CREATE TABLE video_download (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    bvid VARCHAR(50) COMMENT '视频BV号',
    aid VARCHAR(50) COMMENT '视频AV号',
    title VARCHAR(255) COMMENT '视频标题',
    part_title VARCHAR(255) COMMENT '分P标题',
    part_count INT COMMENT '分P总数',
    current_part INT COMMENT '当前分P序号',
    download_url TEXT COMMENT '下载链接',
    local_path TEXT COMMENT '本地存储路径',
    resolution VARCHAR(50) COMMENT '分辨率',
    codec VARCHAR(50) COMMENT '编码格式',
    format VARCHAR(50) COMMENT '流媒体格式',
    status TINYINT DEFAULT 0 COMMENT '下载状态 0-待下载 1-下载中 2-下载完成 3-下载失败',
    progress TINYINT DEFAULT 0 COMMENT '下载进度百分比',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
) COMMENT='视频下载记录表';

-- 创建视频处理任务表
CREATE TABLE video_process_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_name VARCHAR(255) COMMENT '任务名称',
    status TINYINT DEFAULT 0 COMMENT '处理状态 0-待处理 1-处理中 2-处理完成 3-处理失败',
    progress TINYINT DEFAULT 0 COMMENT '处理进度百分比',
    input_files TEXT COMMENT '输入文件列表（JSON格式）',
    output_path TEXT COMMENT '输出文件路径',
    upload_status TINYINT DEFAULT 0 COMMENT 'Bilibili投稿状态 0-未投稿 1-投稿中 2-投稿成功 3-投稿失败',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
) COMMENT='视频处理任务表';

-- 创建视频片段表
CREATE TABLE video_clip (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '处理任务ID',
    file_name VARCHAR(255) COMMENT '视频文件名',
    start_time VARCHAR(20) COMMENT '开始时间',
    end_time VARCHAR(20) COMMENT '结束时间',
    clip_path TEXT COMMENT '片段存储路径',
    sequence INT COMMENT '序号',
    status TINYINT DEFAULT 0 COMMENT '状态 0-待处理 1-处理中 2-处理完成 3-处理失败',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    FOREIGN KEY (task_id) REFERENCES video_process_task(id)
) COMMENT='视频片段表';

-- 创建合并后视频表
CREATE TABLE merged_video (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id VARCHAR(36) NOT NULL COMMENT '关联的任务ID',
    file_name VARCHAR(255) COMMENT '合并后视频文件名',
    video_path TEXT COMMENT '合并后视频存储路径',
    duration INT COMMENT '视频时长（秒）',
    status TINYINT DEFAULT 0 COMMENT '状态 0-待处理 1-处理中 2-处理完成 3-处理失败',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
) COMMENT='合并后视频表';

-- 创建投稿任务表
CREATE TABLE submission_task (
    task_id VARCHAR(36) PRIMARY KEY COMMENT '任务ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    title VARCHAR(255) NOT NULL COMMENT '标题',
    description TEXT COMMENT '描述',
    cover_url VARCHAR(500) COMMENT '封面URL',
    partition_id INT COMMENT '分区ID',
    tags VARCHAR(500) COMMENT '标签',
    video_type VARCHAR(20) COMMENT '视频类型',
    collection_id BIGINT COMMENT '合集ID',
    bvid VARCHAR(20) COMMENT 'B站视频ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间'
) COMMENT='投稿任务表';
