package com.tbw.cut.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 投稿请求DTO
 * 包含投稿任务的所有配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRequestDTO {
    
    /**
     * 视频标题
     */
    private String title;
    
    /**
     * 视频描述
     */
    private String description;
    
    /**
     * 封面图片URL
     */
    private String coverUrl;
    
    /**
     * 分区ID
     */
    private Integer partitionId;
    
    /**
     * 子分区ID
     */
    private Integer subPartitionId;
    
    /**
     * 标签（逗号分隔）
     */
    private String tags;
    
    /**
     * 视频类型
     */
    private VideoType videoType;
    
    /**
     * 合集ID（可选）
     */
    private Long collectionId;
    
    /**
     * 是否允许转载
     */
    private Boolean allowRepost = false;
    
    /**
     * 是否自制
     */
    private Boolean isOriginal = true;
    
    /**
     * 视频分P信息列表（自动从下载配置回显）
     */
    private List<VideoPartInfoDTO> videoParts;
    
    /**
     * 视频类型枚举
     */
    public enum VideoType {
        /**
         * 自制原创
         */
        ORIGINAL,
        
        /**
         * 转载
         */
        REPOST
    }
}