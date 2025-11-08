package com.tbw.cut.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.UUID;

@Data
@TableName("task_output_segment")
public class TaskOutputSegment {
    
    @TableId(type = IdType.NONE)
    private String segmentId;
    
    @TableField(value = "task_id")
    private String taskId;
    
    @TableField(value = "part_name")
    private String partName;
    
    @TableField(value = "segment_file_path")
    private String segmentFilePath;
    
    @TableField(value = "part_order")
    private Integer partOrder;
    
    @TableField(value = "upload_status")
    private UploadStatus uploadStatus;
    
    @TableField(value = "cid")
    private Long cid;
    
    public enum UploadStatus {
        PENDING, UPLOADING, SUCCESS, FAILED
    }
}