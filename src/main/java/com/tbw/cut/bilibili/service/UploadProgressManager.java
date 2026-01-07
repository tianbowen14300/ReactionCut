package com.tbw.cut.bilibili.service;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.entity.UploadProgress;
import com.tbw.cut.entity.UploadStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 上传进度管理器
 */
@Slf4j
@Service
public class UploadProgressManager {
    
    // 存储文件上传进度，使用文件路径作为key
    private final Map<String, UploadProgress> progressMap = new ConcurrentHashMap<>();
    
    /**
     * 开始或恢复上传
     */
    public UploadProgress startUpload(File videoFile, int totalChunks) {
        String filePath = videoFile.getAbsolutePath();
        UploadProgress progress = progressMap.get(filePath);
        
        if (progress == null) {
            // 创建新的上传进度
            progress = new UploadProgress();
            progress.setFilePath(filePath);
            progress.setTotalChunks(totalChunks);
            progress.setStatus(UploadStatus.UPLOADING);
            progress.setStartTime(LocalDateTime.now());
            progressMap.put(filePath, progress);
            log.info("开始新的上传任务，文件: {}, 总分片数: {}", filePath, totalChunks);
        } else {
            // 恢复现有上传
            progress.setStatus(UploadStatus.UPLOADING);
            log.info("恢复上传任务，文件: {}, 已完成分片: {}/{}", filePath, progress.getCompletedChunks(), totalChunks);
        }
        
        return progress;
    }
    
    /**
     * 设置上传元数据
     */
    public void setUploadMetadata(UploadProgress progress, JSONObject preUploadData, JSONObject postVideoMeta) {
        progress.setPreUploadData(preUploadData);
        progress.setPostVideoMeta(postVideoMeta);
        log.debug("设置上传元数据，文件: {}", progress.getFilePath());
    }
    
    /**
     * 标记分片完成
     */
    public void markChunkCompleted(UploadProgress progress, int chunkIndex) {
        progress.markChunkCompleted(chunkIndex);
        log.debug("分片{}完成，文件: {}, 总进度: {}%", chunkIndex, progress.getFilePath(), 
                String.format("%.1f", progress.getCompletionPercentage()));
    }
    
    /**
     * 完成上传
     */
    public void completeUpload(UploadProgress progress) {
        progress.setStatus(UploadStatus.COMPLETED);
        progress.setEndTime(LocalDateTime.now());
        // 从内存中移除已完成的上传进度
        progressMap.remove(progress.getFilePath());
        log.info("上传完成，文件: {}", progress.getFilePath());
    }
    
    /**
     * 取消上传
     */
    public void cancelUpload(UploadProgress progress) {
        progress.setStatus(UploadStatus.FAILED);
        progress.setEndTime(LocalDateTime.now());
        // 保留失败的上传进度，以便后续恢复
        log.warn("上传取消，文件: {}", progress.getFilePath());
    }
    
    /**
     * 创建上传进度跟踪
     */
    public UploadProgress createProgress(String taskId, int totalParts) {
        UploadProgress progress = new UploadProgress(taskId, totalParts);
        progressMap.put(taskId, progress);
        log.debug("创建上传进度跟踪，任务ID: {}, 总分P数: {}", taskId, totalParts);
        return progress;
    }
    
    /**
     * 获取上传进度
     */
    public UploadProgress getProgress(String taskId) {
        return progressMap.get(taskId);
    }
    
    /**
     * 更新上传进度
     */
    public void updateProgress(String taskId, UploadProgress progress) {
        progressMap.put(taskId, progress);
        log.debug("更新上传进度，任务ID: {}, 进度: {}%", taskId, progress.getOverallProgress());
    }
    
    /**
     * 移除上传进度跟踪
     */
    public void removeProgress(String taskId) {
        UploadProgress removed = progressMap.remove(taskId);
        if (removed != null) {
            log.debug("移除上传进度跟踪，任务ID: {}", taskId);
        }
    }
    
    /**
     * 清理所有进度跟踪
     */
    public void clearAll() {
        int size = progressMap.size();
        progressMap.clear();
        log.info("清理所有上传进度跟踪，共清理{}个", size);
    }
    
    /**
     * 获取当前跟踪的任务数量
     */
    public int getTrackingCount() {
        return progressMap.size();
    }
}