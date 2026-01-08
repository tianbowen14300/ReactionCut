package com.tbw.cut.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 视频分P信息DTO
 * 用于在投稿配置中显示和管理视频分P信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPartInfoDTO {
    
    /**
     * 分P标题
     */
    private String title;
    
    /**
     * CID（B站分P唯一标识）
     */
    private Long cid;
    
    /**
     * 分P序号（从1开始）
     */
    private Integer partIndex;
    
    /**
     * 预期的本地文件路径
     */
    private String expectedFilePath;
    
    /**
     * 分P时长（秒）
     */
    private Long duration;
    
    /**
     * 分P大小（字节）
     */
    private Long fileSize;
    
    /**
     * 是否选中此分P进行投稿
     */
    private Boolean selected = true;
    
    /**
     * 投稿时的分P标题（可以与原标题不同）
     */
    private String submissionTitle;
    
    /**
     * 获取投稿标题（优先使用submissionTitle，否则使用原title）
     */
    public String getEffectiveTitle() {
        return submissionTitle != null && !submissionTitle.trim().isEmpty() 
               ? submissionTitle 
               : title;
    }
    
    /**
     * 生成文件路径（基于配置的下载目录和命名规则）
     */
    public String generateFilePath(String baseDir, String namingPattern) {
        if (expectedFilePath != null && !expectedFilePath.trim().isEmpty()) {
            return expectedFilePath;
        }
        
        // 使用默认命名规则：{title}_P{partIndex}.mp4
        String sanitizedTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        return String.format("%s/%s_P%d.mp4", baseDir, sanitizedTitle, partIndex);
    }
}