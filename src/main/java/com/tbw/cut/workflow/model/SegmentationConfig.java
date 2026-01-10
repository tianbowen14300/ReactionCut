package com.tbw.cut.workflow.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 分段配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentationConfig {
    
    /**
     * 是否启用分段
     */
    private boolean enabled;
    
    /**
     * 分段时长（秒）
     */
    @Builder.Default
    private int segmentDurationSeconds = 133; // 默认2分13秒
    
    /**
     * 最大分段数量
     */
    @Builder.Default
    private int maxSegmentCount = 50;
    
    /**
     * 分段命名模式
     */
    @Builder.Default
    private String segmentNamingPattern = "{title}_Part{index}";
    
    /**
     * 是否保留原始文件
     */
    @Builder.Default
    private boolean preserveOriginal = true;
    
    /**
     * 创建默认配置
     */
    public static SegmentationConfig createDefault() {
        return SegmentationConfig.builder()
                .enabled(false)
                .build();
    }
    
    /**
     * 创建启用分段的配置
     */
    public static SegmentationConfig createEnabled(int segmentDurationSeconds) {
        return SegmentationConfig.builder()
                .enabled(true)
                .segmentDurationSeconds(segmentDurationSeconds)
                .build();
    }
    
    /**
     * 验证配置有效性
     */
    public boolean isValid() {
        if (!enabled) {
            return true; // 未启用时总是有效
        }
        
        return isValidSegmentDuration() && isValidMaxSegmentCount() && isValidNamingPattern();
    }
    
    /**
     * 获取验证错误信息
     */
    public String getValidationError() {
        if (!enabled) {
            return null;
        }
        
        if (!isValidSegmentDuration()) {
            return "分段时长必须在30-600秒之间，当前值: " + segmentDurationSeconds + "秒";
        }
        
        if (!isValidMaxSegmentCount()) {
            return "最大分段数量必须在1-100之间，当前值: " + maxSegmentCount;
        }
        
        if (!isValidNamingPattern()) {
            return "分段命名模式不能为空且必须包含{index}占位符";
        }
        
        return null;
    }
    
    /**
     * 验证分段时长是否有效
     */
    public boolean isValidSegmentDuration() {
        return segmentDurationSeconds >= 30 && segmentDurationSeconds <= 600;
    }
    
    /**
     * 验证最大分段数量是否有效
     */
    public boolean isValidMaxSegmentCount() {
        return maxSegmentCount > 0 && maxSegmentCount <= 100;
    }
    
    /**
     * 验证命名模式是否有效
     */
    public boolean isValidNamingPattern() {
        return segmentNamingPattern != null && 
               !segmentNamingPattern.trim().isEmpty() && 
               segmentNamingPattern.contains("{index}");
    }
    
    /**
     * 应用默认值到无效字段
     */
    public void applyDefaults() {
        if (!isValidSegmentDuration()) {
            segmentDurationSeconds = 133;
        }
        
        if (!isValidMaxSegmentCount()) {
            maxSegmentCount = 50;
        }
        
        if (!isValidNamingPattern()) {
            segmentNamingPattern = "{title}_Part{index}";
        }
    }
    
    /**
     * 获取分段时长的有效范围描述
     */
    public static String getSegmentDurationRange() {
        return "30-600秒";
    }
    
    /**
     * 获取最大分段数量的有效范围描述
     */
    public static String getMaxSegmentCountRange() {
        return "1-100个";
    }
    
    /**
     * 获取推荐的分段时长
     */
    public static int getRecommendedSegmentDuration() {
        return 133; // 2分13秒
    }
    
    /**
     * 创建用于视频投稿的分段配置
     * 
     * @param segmentDurationSeconds 分段时长（秒）
     * @return 分段配置
     */
    public static SegmentationConfig createForSubmission(int segmentDurationSeconds) {
        SegmentationConfig config = SegmentationConfig.builder()
                .enabled(true)
                .segmentDurationSeconds(segmentDurationSeconds)
                .maxSegmentCount(50)
                .segmentNamingPattern("{title}_Part{index}")
                .preserveOriginal(true)
                .build();
        
        // 应用默认值到无效字段
        config.applyDefaults();
        
        return config;
    }
}