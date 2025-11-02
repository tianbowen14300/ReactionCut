package com.tbw.cut.dto;

import lombok.Data;

@Data
public class VideoClipDTO {
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
     * 序号
     */
    private Integer sequence;
}