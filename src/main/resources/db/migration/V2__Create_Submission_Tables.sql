-- 创建投稿任务表
CREATE TABLE submission_task (
    task_id CHAR(36) NOT NULL PRIMARY KEY,
    status ENUM('PENDING', 'PROCESSING', 'CLIPPING', 'SEGMENTING', 'UPLOADING', 'COMPLETED', 'FAILED') NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    cover_url VARCHAR(255),
    partition_id INT NOT NULL,
    tags VARCHAR(500),
    video_type ENUM('ORIGINAL', 'REPOST') NOT NULL,
    collection_id BIGINT,
    bvid VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- 创建任务源视频表
CREATE TABLE task_source_video (
    id CHAR(36) NOT NULL PRIMARY KEY,
    task_id CHAR(36) NOT NULL,
    source_file_path VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    start_time VARCHAR(20),
    end_time VARCHAR(20),
    FOREIGN KEY (task_id) REFERENCES submission_task(task_id)
);

-- 创建任务输出分段表
CREATE TABLE task_output_segment (
    segment_id CHAR(36) NOT NULL PRIMARY KEY,
    task_id CHAR(36) NOT NULL,
    part_name VARCHAR(100) NOT NULL,
    segment_file_path VARCHAR(255) NOT NULL,
    part_order INT NOT NULL,
    upload_status ENUM('PENDING', 'UPLOADING', 'SUCCESS', 'FAILED') NOT NULL,
    cid BIGINT,
    FOREIGN KEY (task_id) REFERENCES submission_task(task_id)
);