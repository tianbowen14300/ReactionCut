package com.tbw.cut.mapper;

import com.tbw.cut.dto.VideoPartInfoDTO;
import com.tbw.cut.entity.TaskSourceVideo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 视频分P配置数据映射器
 * 负责将前端分P配置数据映射到TaskSourceVideo实体
 */
@Slf4j
public class VideoPartsMapper {
    
    /**
     * 将前端videoParts数据映射到TaskSourceVideo实体列表
     * 
     * @param videoParts 前端分P配置列表
     * @param taskId 投稿任务ID
     * @return TaskSourceVideo实体列表
     */
    public static List<TaskSourceVideo> mapVideoPartsToSourceVideos(
            List<VideoPartInfoDTO> videoParts, String taskId) {
        
        if (videoParts == null || videoParts.isEmpty()) {
            log.info("No video parts provided for task: {}", taskId);
            return new ArrayList<>();
        }
        
        List<TaskSourceVideo> sourceVideos = new ArrayList<>();
        
        for (int i = 0; i < videoParts.size(); i++) {
            VideoPartInfoDTO videoPart = videoParts.get(i);
            
            // 跳过未选中的分P
            if (videoPart.getSelected() != null && !videoPart.getSelected()) {
                log.debug("Skipping unselected video part: {}", videoPart.getTitle());
                continue;
            }
            
            TaskSourceVideo sourceVideo = mapSingleVideoPart(videoPart, taskId, i + 1);
            sourceVideos.add(sourceVideo);
            
            log.debug("Mapped video part: {} -> {}", videoPart.getTitle(), sourceVideo.getSourceFilePath());
        }
        
        log.info("Mapped {} video parts to source videos for task: {}", sourceVideos.size(), taskId);
        return sourceVideos;
    }
    
    /**
     * 映射单个分P配置到TaskSourceVideo实体
     * 
     * @param videoPart 分P配置
     * @param taskId 投稿任务ID（可以为null，将在后续设置）
     * @param sortOrder 排序序号
     * @return TaskSourceVideo实体
     */
    public static TaskSourceVideo mapSingleVideoPart(
            VideoPartInfoDTO videoPart, String taskId, int sortOrder) {
        
        if (videoPart == null) {
            throw new IllegalArgumentException("VideoPart cannot be null");
        }
        
        TaskSourceVideo sourceVideo = new TaskSourceVideo();
        
        // 生成唯一ID（如果需要的话，SubmissionTaskService可能会重新生成）
        sourceVideo.setId(UUID.randomUUID().toString());
        
        // 设置任务ID（可能为null，将在SubmissionTaskService中设置）
        sourceVideo.setTaskId(taskId);
        
        // 设置排序序号
        sourceVideo.setSortOrder(sortOrder);
        
        // 映射文件路径
        sourceVideo.setSourceFilePath(generateSourceFilePath(videoPart));
        
        // 映射时间配置
        sourceVideo.setStartTime(videoPart.getStartTime());
        sourceVideo.setEndTime(videoPart.getEndTime());
        
        return sourceVideo;
    }
    
    /**
     * 根据分P配置生成源文件路径
     * 
     * @param videoPart 分P配置
     * @return 源文件路径
     */
    public static String generateSourceFilePath(VideoPartInfoDTO videoPart) {
        if (videoPart == null) {
            throw new IllegalArgumentException("VideoPart cannot be null");
        }
        
        // 优先使用明确指定的文件路径
        if (StringUtils.hasText(videoPart.getFilePath())) {
            return videoPart.getFilePath();
        }
        
        // 其次使用预期文件路径
        if (StringUtils.hasText(videoPart.getExpectedFilePath())) {
            return videoPart.getExpectedFilePath();
        }
        
        // 最后根据标题和分P序号生成默认路径
        if (StringUtils.hasText(videoPart.getTitle()) && videoPart.getPartIndex() != null) {
            String sanitizedTitle = sanitizeFileName(videoPart.getTitle());
            return String.format("videos/%s_P%d.mp4", sanitizedTitle, videoPart.getPartIndex());
        }
        
        // 如果都没有，使用CID生成路径
        if (videoPart.getCid() != null) {
            return String.format("videos/video_%d.mp4", videoPart.getCid());
        }
        
        throw new IllegalArgumentException("Cannot generate source file path: insufficient information in VideoPart");
    }
    
    /**
     * 清理文件名中的非法字符
     * 
     * @param fileName 原始文件名
     * @return 清理后的文件名
     */
    private static String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "unknown";
        }
        
        // 移除或替换文件名中的非法字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_")
                      .replaceAll("\\s+", "_")
                      .trim();
    }
    
    /**
     * 验证分P配置数据的完整性
     * 
     * @param videoParts 分P配置列表
     * @return 验证结果和错误信息
     */
    public static ValidationResult validateVideoParts(List<VideoPartInfoDTO> videoParts) {
        List<String> errors = new ArrayList<>();
        
        if (videoParts == null || videoParts.isEmpty()) {
            return ValidationResult.success(); // 空列表是允许的
        }
        
        for (int i = 0; i < videoParts.size(); i++) {
            VideoPartInfoDTO videoPart = videoParts.get(i);
            String prefix = String.format("VideoPart[%d]", i);
            
            // 验证基本信息
            if (videoPart == null) {
                errors.add(prefix + ": VideoPart is null");
                continue;
            }
            
            // 验证CID
            if (videoPart.getCid() == null || videoPart.getCid() <= 0) {
                errors.add(prefix + ": CID is required and must be positive");
            }
            
            // 验证时间格式
            if (StringUtils.hasText(videoPart.getStartTime()) && !isValidTimeFormat(videoPart.getStartTime())) {
                errors.add(prefix + ": StartTime format is invalid (expected HH:mm:ss)");
            }
            
            if (StringUtils.hasText(videoPart.getEndTime()) && !isValidTimeFormat(videoPart.getEndTime())) {
                errors.add(prefix + ": EndTime format is invalid (expected HH:mm:ss)");
            }
            
            // 验证时间逻辑
            if (StringUtils.hasText(videoPart.getStartTime()) && StringUtils.hasText(videoPart.getEndTime())) {
                if (compareTimeStrings(videoPart.getStartTime(), videoPart.getEndTime()) >= 0) {
                    errors.add(prefix + ": EndTime must be greater than StartTime");
                }
            }
            
            // 验证文件路径生成所需信息
            try {
                generateSourceFilePath(videoPart);
            } catch (IllegalArgumentException e) {
                errors.add(prefix + ": " + e.getMessage());
            }
        }
        
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
    
    /**
     * 验证时间格式是否为 HH:mm:ss
     * 
     * @param timeString 时间字符串
     * @return 是否为有效格式
     */
    private static boolean isValidTimeFormat(String timeString) {
        if (!StringUtils.hasText(timeString)) {
            return false;
        }
        
        return timeString.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$");
    }
    
    /**
     * 比较两个时间字符串
     * 
     * @param time1 时间1
     * @param time2 时间2
     * @return 比较结果（负数：time1 < time2，0：相等，正数：time1 > time2）
     */
    private static int compareTimeStrings(String time1, String time2) {
        try {
            String[] parts1 = time1.split(":");
            String[] parts2 = time2.split(":");
            
            int seconds1 = Integer.parseInt(parts1[0]) * 3600 + Integer.parseInt(parts1[1]) * 60 + Integer.parseInt(parts1[2]);
            int seconds2 = Integer.parseInt(parts2[0]) * 3600 + Integer.parseInt(parts2[1]) * 60 + Integer.parseInt(parts2[2]);
            
            return Integer.compare(seconds1, seconds2);
        } catch (Exception e) {
            return 0; // 如果解析失败，认为相等
        }
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}