package com.tbw.cut.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_clip")
public class VideoClip {
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
     * 视频文件名
     */
    private String fileName;
    
    /**
     * 截取开始时间 (HH:mm:ss格式)
     */
    private String startTime;
    
    /**
     * 截取结束时间 (HH:mm:ss格式)
     */
    private String endTime;
    
    /**
     * 片段输出路径
     */
    private String clipPath;
    
    /**
     * 序号
     */
    private Integer sequence;
    
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