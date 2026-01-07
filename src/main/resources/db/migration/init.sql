-- reaction_cut.anchor definition

CREATE TABLE `anchor`
(
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` varchar(50) NOT NULL COMMENT 'Bilibili主播UID',
  `nickname` varchar(100) DEFAULT NULL COMMENT '主播昵称',
  `live_status` tinyint DEFAULT '0' COMMENT '直播状态 0-未直播 1-直播中',
  `last_check_time` datetime DEFAULT NULL COMMENT '上次检查时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uid` (`uid`),
  KEY `idx_anchor_uid` (`uid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='主播信息表';

-- reaction_cut.login_info definition

CREATE TABLE `login_info`
(
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `nickname` varchar(100) DEFAULT NULL COMMENT '用户昵称',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `access_token` varchar(500) DEFAULT NULL COMMENT '访问令牌',
  `refresh_token` varchar(500) DEFAULT NULL COMMENT '刷新令牌',
  `cookie_info` text COMMENT 'Cookie信息（JSON格式）',
  `login_time` datetime NOT NULL COMMENT '登录时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `idx_login_info_user_id` (`user_id`),
  KEY `idx_login_info_expire_time` (`expire_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1984518865054490627 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户登录信息表';

-- reaction_cut.submission_task definition

CREATE TABLE `submission_task`
(
    `task_id` char(36) NOT NULL,
    `status` enum('PENDING','CLIPPING','MERGING','SEGMENTING','UPLOADING','COMPLETED','FAILED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    `title` varchar(255) NOT NULL,
    `description` text,
    `cover_url` varchar(255) DEFAULT NULL,
    `partition_id` int NOT NULL,
    `tags` varchar(500) DEFAULT NULL,
    `video_type` enum('ORIGINAL','REPOST') NOT NULL,
    `collection_id` bigint DEFAULT NULL,
    `bvid` varchar(50) DEFAULT NULL,
    `created_at` datetime NOT NULL,
    `updated_at` datetime NOT NULL,
    `segment_prefix` varchar(100) DEFAULT NULL COMMENT '分段前缀',
    PRIMARY KEY (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- reaction_cut.merged_video definition

CREATE TABLE `merged_video`
(
`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`task_id` varchar(36) NOT NULL COMMENT '关联的任务ID',
`file_name` varchar(255) DEFAULT NULL COMMENT '合并后视频文件名',
`video_path` text COMMENT '合并后视频存储路径',
`duration` int DEFAULT NULL COMMENT '视频时长（秒）',
`status` tinyint DEFAULT '0' COMMENT '状态 0-待处理 1-处理中 2-处理完成 3-处理失败',
`create_time` datetime NOT NULL COMMENT '创建时间',
`update_time` datetime NOT NULL COMMENT '更新时间',
PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='合并后视频表';

-- reaction_cut.task_output_segment definition

CREATE TABLE `task_output_segment`
(
`segment_id` char(36) NOT NULL,
`task_id` char(36) NOT NULL,
`part_name` varchar(100) NOT NULL,
`segment_file_path` varchar(255) NOT NULL,
`part_order` int NOT NULL,
`upload_status` enum('PENDING','UPLOADING','SUCCESS','FAILED') NOT NULL,
`cid` bigint DEFAULT NULL,
`file_name` varchar(100) DEFAULT NULL,
PRIMARY KEY (`segment_id`),
KEY `task_id` (`task_id`),
CONSTRAINT `task_output_segment_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `submission_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- reaction_cut.task_source_video definition

CREATE TABLE `task_source_video`
(
 `id` char(36) NOT NULL,
 `task_id` char(36) NOT NULL,
 `source_file_path` varchar(255) NOT NULL,
 `sort_order` int NOT NULL,
 `start_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
 `end_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
 PRIMARY KEY (`id`),
 KEY `task_id` (`task_id`),
 CONSTRAINT `task_source_video_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `submission_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- reaction_cut.video_clip definition

CREATE TABLE `video_clip`
(
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(50) NOT NULL COMMENT '关联的任务ID',
  `file_name` varchar(255) NOT NULL COMMENT '视频文件名',
  `start_time` varchar(20) DEFAULT NULL COMMENT '截取开始时间 (HH:mm:ss格式)',
  `end_time` varchar(20) DEFAULT NULL COMMENT '截取结束时间 (HH:mm:ss格式)',
  `clip_path` text COMMENT '片段输出路径',
  `sequence` int DEFAULT NULL COMMENT '序号',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待处理 1-处理中 2-处理完成 3-处理失败',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_video_clip_task_id` (`task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=315 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频片段表';

-- reaction_cut.video_download definition

CREATE TABLE `video_download`
(
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bvid` varchar(50) DEFAULT NULL COMMENT '视频BV号',
  `aid` varchar(50) DEFAULT NULL COMMENT '视频AV号',
  `title` varchar(255) DEFAULT NULL COMMENT '视频标题',
  `download_url` text COMMENT '下载链接',
  `local_path` text COMMENT '本地存储路径',
  `status` tinyint DEFAULT '0' COMMENT '下载状态 0-待下载 1-下载中 2-下载完成 3-下载失败',
  `progress` tinyint DEFAULT '0' COMMENT '下载进度百分比',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `resolution` varchar(50) DEFAULT NULL COMMENT '下载分辨率',
  `codec` varchar(50) DEFAULT NULL COMMENT '编码格式',
  `format` varchar(50) DEFAULT NULL COMMENT '流媒体格式',
  `part_title` varchar(255) DEFAULT NULL COMMENT '分p标题',
  `part_count` int DEFAULT NULL COMMENT '分p总是',
  `current_part` int DEFAULT NULL COMMENT '当前分p数',
  PRIMARY KEY (`id`),
  KEY `idx_video_download_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=276 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频下载记录表';

-- reaction_cut.video_process_task definition

CREATE TABLE `video_process_task`
(
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `status` tinyint DEFAULT '0' COMMENT '处理状态 0-待处理 1-处理中 2-处理完成 3-处理失败',
  `progress` tinyint DEFAULT '0' COMMENT '处理进度百分比',
  `input_files` text COMMENT '输入文件列表（JSON格式）',
  `output_path` text COMMENT '输出文件路径',
  `upload_status` tinyint DEFAULT '0' COMMENT 'Bilibili投稿状态 0-未投稿 1-投稿中 2-投稿成功 3-投稿失败',
  `bilibili_url` text COMMENT 'Bilibili视频链接',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_video_process_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频处理任务表';