package com.tbw.cut.service.download.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 视频下载请求模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDownloadRequest {
    
    /**
     * 视频URL
     */
    private String videoUrl;
    
    /**
     * 视频标题
     */
    private String videoTitle;
    
    /**
     * 视频分P列表
     */
    private List<VideoPart> parts;
    
    /**
     * 输出目录
     */
    private String outputDirectory;
    
    /**
     * 下载配置
     */
    private DownloadConfig config;
    
    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
    
    /**
     * 是否启用分段下载
     */
    private boolean enableSegmentedDownload;
    
    /**
     * 预估文件大小（字节）
     */
    private Long estimatedFileSize;
    
    /**
     * 视频质量
     */
    private String quality;
    
    /**
     * 视频格式
     */
    private String format;
    
    /**
     * 用户ID（用于权限控制）
     */
    private String userId;
    
    /**
     * 回调URL（下载完成后通知）
     */
    private String callbackUrl;
    
    /**
     * 获取视频唯一标识
     * @return 视频标识
     */
    public String getVideoId() {
        if (extraParams != null && extraParams.containsKey("bvid")) {
            return (String) extraParams.get("bvid");
        }
        return String.valueOf(videoUrl.hashCode());
    }
    
    /**
     * 获取总分P数量
     * @return 分P数量
     */
    public int getPartCount() {
        return parts != null ? parts.size() : 0;
    }
    
    /**
     * 是否为多分P视频
     * @return 是否多分P
     */
    public boolean isMultiPart() {
        return getPartCount() > 1;
    }
    
    /**
     * 获取预估总文件大小
     * @return 总文件大小（字节）
     */
    public long getTotalEstimatedSize() {
        if (estimatedFileSize != null) {
            return estimatedFileSize;
        }
        
        if (parts != null) {
            return parts.stream()
                .mapToLong(part -> part.getEstimatedSize() != null ? part.getEstimatedSize() : 0)
                .sum();
        }
        
        return 0;
    }
}