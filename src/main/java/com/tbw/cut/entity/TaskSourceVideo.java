package com.tbw.cut.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.UUID;

@Data
@TableName("task_source_video")
public class TaskSourceVideo {
    
    @TableId(type = IdType.NONE)
    private String id;
    
    @TableField(value = "task_id")
    private String taskId;
    
    @TableField(value = "source_file_path")
    private String sourceFilePath;
    
    @TableField(value = "sort_order")
    private Integer sortOrder;
    
    @TableField(value = "start_time")
    private String startTime;
    
    @TableField(value = "end_time")
    private String endTime;
}