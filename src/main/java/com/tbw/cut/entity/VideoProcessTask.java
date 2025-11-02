package com.tbw.cut.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_process_task")
public class VideoProcessTask {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 处理状态 0-待处理 1-处理中 2-处理完成 3-处理失败
     */
    private Integer status;
    
    /**
     * 处理进度百分比
     */
    private Integer progress;
    
    /**
     * 输入文件列表（JSON格式）
     */
    private String inputFiles;
    
    /**
     * 输出文件路径
     */
    private String outputPath;
    
    /**
     * Bilibili投稿状态 0-未投稿 1-投稿中 2-投稿成功 3-投稿失败
     */
    private Integer uploadStatus;
    
    /**
     * Bilibili视频链接
     */
    private String bilibiliUrl;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}