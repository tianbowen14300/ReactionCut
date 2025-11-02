package com.tbw.cut.dto;

import lombok.Data;
import java.util.List;

@Data
public class VideoDownloadDTO {
    /**
     * Bilibili视频链接
     */
    private String videoUrl;
    
    /**
     * 要下载的分P列表，如果为空则下载所有分P
     */
    private List<Integer> pages;
    
    /**
     * 视频质量
     */
    private String quality;
    
    /**
     * 是否只下载音频
     */
    private Boolean audioOnly;
    
    /**
     * 是否只下载视频
     */
    private Boolean videoOnly;
}