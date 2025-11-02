package com.tbw.cut.dto;

import lombok.Data;
import java.util.List;

@Data
public class VideoProcessTaskDTO {
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 视频片段列表
     */
    private List<VideoClipDTO> clips;
}