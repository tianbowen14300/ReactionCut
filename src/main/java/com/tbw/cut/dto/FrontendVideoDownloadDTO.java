package com.tbw.cut.dto;

import lombok.Data;
import java.util.List;

@Data
public class FrontendVideoDownloadDTO {
    /**
     * Bilibili视频链接
     */
    private String videoUrl;
    
    /**
     * 要下载的分P列表
     */
    private List<PartInfo> parts;
    
    /**
     * 下载配置
     */
    private DownloadConfig config;
    
    @Data
    public static class PartInfo {
        private Long cid;
        private String title;
    }
    
    @Data
    public static class DownloadConfig {
        private String resolution;
        private String codec;
        private String bitrate;
        private String format;
        private String content;
    }
}