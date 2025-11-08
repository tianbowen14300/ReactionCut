package com.tbw.cut.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("merged_video")
public class MergedVideo {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联的任务ID
     */
    private String taskId;
    
    /**
     * 合并后视频文件名
     */
    private String fileName;
    
    /**
     * 合并后视频存储路径
     */
    private String videoPath;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    
    /**
     * 状态 0-待处理 1-处理中 2-处理完成 3-处理失败
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}