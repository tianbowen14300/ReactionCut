package com.tbw.cut.event;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 下载完成事件
 * 
 * 当视频下载任务完成时发布此事件，用于触发工作流启动
 */
@Data
@Builder
@Slf4j
public class DownloadCompletionEvent {
    
    /**
     * 下载任务ID
     */
    private Long downloadTaskId;
    
    /**
     * 关联的投稿任务ID
     */
    private String submissionTaskId;
    
    /**
     * 已完成下载的文件路径列表
     */
    private List<String> completedFilePaths;
    
    /**
     * 完成时间
     */
    private LocalDateTime completionTime;
    
    /**
     * 附加元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 验证事件数据完整性
     * 
     * @return true 如果事件数据有效，false 否则
     */
    public boolean isValid() {
        if (downloadTaskId == null) {
            log.warn("下载完成事件验证失败: downloadTaskId为空");
            return false;
        }
        
        if (submissionTaskId == null || submissionTaskId.trim().isEmpty()) {
            log.warn("下载完成事件验证失败: submissionTaskId为空, downloadTaskId={}", downloadTaskId);
            return false;
        }
        
        if (completedFilePaths == null || completedFilePaths.isEmpty()) {
            log.warn("下载完成事件验证失败: completedFilePaths为空, downloadTaskId={}, submissionTaskId={}", 
                    downloadTaskId, submissionTaskId);
            return false;
        }
        
        // 检查文件路径是否有效
        for (String filePath : completedFilePaths) {
            if (filePath == null || filePath.trim().isEmpty()) {
                log.warn("下载完成事件验证失败: 存在空的文件路径, downloadTaskId={}, submissionTaskId={}", 
                        downloadTaskId, submissionTaskId);
                return false;
            }
        }
        
        if (completionTime == null) {
            log.warn("下载完成事件验证失败: completionTime为空, downloadTaskId={}, submissionTaskId={}", 
                    downloadTaskId, submissionTaskId);
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取文件数量
     * 
     * @return 完成下载的文件数量
     */
    public int getFileCount() {
        return completedFilePaths != null ? completedFilePaths.size() : 0;
    }
    
    /**
     * 检查是否包含指定的元数据键
     * 
     * @param key 元数据键
     * @return true 如果包含该键，false 否则
     */
    public boolean hasMetadata(String key) {
        return metadata != null && metadata.containsKey(key);
    }
    
    /**
     * 获取元数据值
     * 
     * @param key 元数据键
     * @return 元数据值，如果不存在则返回null
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * 获取字符串类型的元数据值
     * 
     * @param key 元数据键
     * @return 字符串类型的元数据值，如果不存在或类型不匹配则返回null
     */
    public String getMetadataAsString(String key) {
        Object value = getMetadata(key);
        return value instanceof String ? (String) value : null;
    }
    
    /**
     * 获取整数类型的元数据值
     * 
     * @param key 元数据键
     * @return 整数类型的元数据值，如果不存在或类型不匹配则返回null
     */
    public Integer getMetadataAsInteger(String key) {
        Object value = getMetadata(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    @Override
    public String toString() {
        return String.format("DownloadCompletionEvent{downloadTaskId=%d, submissionTaskId='%s', fileCount=%d, completionTime=%s}", 
                downloadTaskId, submissionTaskId, getFileCount(), completionTime);
    }
}