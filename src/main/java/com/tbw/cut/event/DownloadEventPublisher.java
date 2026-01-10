package com.tbw.cut.event;

import com.tbw.cut.service.TaskRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 下载事件发布器
 * 
 * 负责发布下载完成事件，触发工作流启动
 */
@Component
@Slf4j
public class DownloadEventPublisher {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private TaskRelationService taskRelationService;
    
    /**
     * 发布下载完成事件
     * 
     * @param downloadTaskId 下载任务ID
     * @param filePaths 完成下载的文件路径列表
     */
    public void publishDownloadCompletionEvent(Long downloadTaskId, List<String> filePaths) {
        if (downloadTaskId == null) {
            log.error("无法发布下载完成事件: downloadTaskId为空");
            return;
        }
        
        if (filePaths == null || filePaths.isEmpty()) {
            log.error("无法发布下载完成事件: filePaths为空, downloadTaskId={}", downloadTaskId);
            return;
        }
        
        try {
            log.info("开始发布下载完成事件: downloadTaskId={}, fileCount={}", downloadTaskId, filePaths.size());
            
            // 查找关联的投稿任务
            String submissionTaskId = taskRelationService.findSubmissionTaskByDownloadId(downloadTaskId);
            
            if (submissionTaskId == null || submissionTaskId.trim().isEmpty()) {
                log.warn("未找到关联的投稿任务，跳过工作流启动: downloadTaskId={}", downloadTaskId);
                return;
            }
            
            // 创建事件
            DownloadCompletionEvent event = DownloadCompletionEvent.builder()
                    .downloadTaskId(downloadTaskId)
                    .submissionTaskId(submissionTaskId)
                    .completedFilePaths(filePaths)
                    .completionTime(LocalDateTime.now())
                    .metadata(createEventMetadata(downloadTaskId, filePaths))
                    .build();
            
            // 验证事件数据
            if (!event.isValid()) {
                log.error("下载完成事件数据无效，无法发布: {}", event);
                return;
            }
            
            // 发布事件
            eventPublisher.publishEvent(event);
            
            log.info("成功发布下载完成事件: downloadTaskId={}, submissionTaskId={}, fileCount={}", 
                    downloadTaskId, submissionTaskId, filePaths.size());
            
        } catch (Exception e) {
            log.error("发布下载完成事件失败: downloadTaskId={}, error={}", downloadTaskId, e.getMessage(), e);
            
            // 记录失败事件以便后续重试
            recordFailedEvent(downloadTaskId, filePaths, e);
        }
    }
    
    /**
     * 发布下载完成事件（带元数据）
     * 
     * @param downloadTaskId 下载任务ID
     * @param filePaths 完成下载的文件路径列表
     * @param metadata 附加元数据
     */
    public void publishDownloadCompletionEvent(Long downloadTaskId, List<String> filePaths, Map<String, Object> metadata) {
        if (downloadTaskId == null) {
            log.error("无法发布下载完成事件: downloadTaskId为空");
            return;
        }
        
        if (filePaths == null || filePaths.isEmpty()) {
            log.error("无法发布下载完成事件: filePaths为空, downloadTaskId={}", downloadTaskId);
            return;
        }
        
        try {
            log.info("开始发布下载完成事件(带元数据): downloadTaskId={}, fileCount={}", downloadTaskId, filePaths.size());
            
            // 查找关联的投稿任务
            String submissionTaskId = taskRelationService.findSubmissionTaskByDownloadId(downloadTaskId);
            
            if (submissionTaskId == null || submissionTaskId.trim().isEmpty()) {
                log.warn("未找到关联的投稿任务，跳过工作流启动: downloadTaskId={}", downloadTaskId);
                return;
            }
            
            // 合并元数据
            Map<String, Object> combinedMetadata = createEventMetadata(downloadTaskId, filePaths);
            if (metadata != null) {
                combinedMetadata.putAll(metadata);
            }
            
            // 创建事件
            DownloadCompletionEvent event = DownloadCompletionEvent.builder()
                    .downloadTaskId(downloadTaskId)
                    .submissionTaskId(submissionTaskId)
                    .completedFilePaths(filePaths)
                    .completionTime(LocalDateTime.now())
                    .metadata(combinedMetadata)
                    .build();
            
            // 验证事件数据
            if (!event.isValid()) {
                log.error("下载完成事件数据无效，无法发布: {}", event);
                return;
            }
            
            // 发布事件
            eventPublisher.publishEvent(event);
            
            log.info("成功发布下载完成事件(带元数据): downloadTaskId={}, submissionTaskId={}, fileCount={}", 
                    downloadTaskId, submissionTaskId, filePaths.size());
            
        } catch (Exception e) {
            log.error("发布下载完成事件失败: downloadTaskId={}, error={}", downloadTaskId, e.getMessage(), e);
            
            // 记录失败事件以便后续重试
            recordFailedEvent(downloadTaskId, filePaths, e);
        }
    }
    
    /**
     * 创建事件元数据
     * 
     * @param downloadTaskId 下载任务ID
     * @param filePaths 文件路径列表
     * @return 元数据Map
     */
    private Map<String, Object> createEventMetadata(Long downloadTaskId, List<String> filePaths) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("downloadTaskId", downloadTaskId);
        metadata.put("fileCount", filePaths.size());
        metadata.put("publishTime", LocalDateTime.now());
        metadata.put("eventSource", "DownloadEventPublisher");
        
        // 计算文件总大小（如果可能）
        long totalSize = 0;
        for (String filePath : filePaths) {
            try {
                java.io.File file = new java.io.File(filePath);
                if (file.exists()) {
                    totalSize += file.length();
                }
            } catch (Exception e) {
                log.debug("无法获取文件大小: {}", filePath);
            }
        }
        
        if (totalSize > 0) {
            metadata.put("totalFileSize", totalSize);
        }
        
        return metadata;
    }
    
    /**
     * 记录失败的事件发布
     * 
     * @param downloadTaskId 下载任务ID
     * @param filePaths 文件路径列表
     * @param error 错误信息
     */
    private void recordFailedEvent(Long downloadTaskId, List<String> filePaths, Exception error) {
        try {
            // 这里可以将失败的事件记录到数据库或其他持久化存储中
            // 以便后续重试或手动处理
            log.warn("记录失败的下载完成事件: downloadTaskId={}, fileCount={}, error={}", 
                    downloadTaskId, filePaths.size(), error.getMessage());
            
            // TODO: 实现失败事件的持久化存储
            // failedEventService.recordFailedEvent(downloadTaskId, filePaths, error.getMessage());
            
        } catch (Exception e) {
            log.error("记录失败事件时发生异常: downloadTaskId={}", downloadTaskId, e);
        }
    }
    
    /**
     * 检查是否可以发布事件
     * 
     * @param downloadTaskId 下载任务ID
     * @return true 如果可以发布事件，false 否则
     */
    public boolean canPublishEvent(Long downloadTaskId) {
        if (downloadTaskId == null) {
            return false;
        }
        
        try {
            // 检查是否存在关联的投稿任务
            String submissionTaskId = taskRelationService.findSubmissionTaskByDownloadId(downloadTaskId);
            return submissionTaskId != null && !submissionTaskId.trim().isEmpty();
        } catch (Exception e) {
            log.error("检查事件发布条件时发生异常: downloadTaskId={}", downloadTaskId, e);
            return false;
        }
    }
}