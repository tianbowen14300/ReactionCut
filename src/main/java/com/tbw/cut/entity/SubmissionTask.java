package com.tbw.cut.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("submission_task")
public class SubmissionTask {
    
    @TableId(type = IdType.NONE)
    private String taskId;
    
    private TaskStatus status;
    
    private String title;
    private String description;
    private String coverUrl;
    private Integer partitionId;
    private String tags;
    private VideoType videoType;
    private Long collectionId;
    private String bvid;
    
    // 添加分段前缀字段
    @TableField(value = "segment_prefix")
    private String segmentPrefix;
    
    @TableField(value = "created_at")
    private Date createdAt;
    
    @TableField(value = "updated_at")
    private Date updatedAt;
    
    public enum TaskStatus {
        PENDING, CLIPPING, MERGING, SEGMENTING, UPLOADING, COMPLETED, FAILED, WAITING_DOWNLOAD
    }
    
    public enum VideoType {
        ORIGINAL, REPOST
    }
}