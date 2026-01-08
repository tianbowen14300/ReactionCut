package com.tbw.cut.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tbw.cut.config.DownloadConfig;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.mapper.VideoDownloadMapper;
import com.tbw.cut.service.PartDownloadService;
import com.tbw.cut.websocket.DownloadProgressWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PartDownloadServiceImpl extends ServiceImpl<VideoDownloadMapper, VideoDownload> implements PartDownloadService {
    
    @Autowired
    private DownloadConfig downloadConfig;
    
    // 数据库更新节流控制
    private final Map<Long, Long> lastDbUpdateTimes = new ConcurrentHashMap<>();
    private final Map<Long, Integer> lastDbProgressValues = new ConcurrentHashMap<>();
    private static final long DB_UPDATE_INTERVAL_MS = 5000; // 5秒数据库更新间隔
    private static final int DB_PROGRESS_THRESHOLD = 10; // 10%进度变化阈值
    
    @Override
    public Long createPartDownload(VideoDownload download) {
        try {
            download.setCreateTime(LocalDateTime.now());
            download.setUpdateTime(LocalDateTime.now());
            this.save(download);
            return download.getId();
        } catch (Exception e) {
            log.error("创建分P下载记录失败", e);
            return null;
        }
    }
    
    @Override
    public void updatePartProgress(Long taskId, Integer progress) {
        VideoDownload download = this.getById(taskId);
        if (download != null) {
            log.info("Updating part progress: taskId={}, progress={}, currentProgress={}", taskId, progress, download.getProgress());
            
            // 确保进度不会回退（除非是重置进度）
            if (progress < download.getProgress() && progress > 0) {
                log.debug("Skipping progress update to prevent rollback, Task ID: {}, Current: {}%, New: {}%", 
                         taskId, download.getProgress(), progress);
                // 仍然广播最新的进度值给前端
                DownloadProgressWebSocket.broadcastProgressUpdate(taskId, download.getProgress());
                return;
            }
            
            // 更新进度和时间
            download.setProgress(progress);
            download.setUpdateTime(LocalDateTime.now());
            
            // 只有当当前状态是待下载(0)时，才更新为下载中(1)
            if (download.getStatus() == 0) {
                download.setStatus(1); // 下载中
                this.updateById(download); // 状态变更时总是更新数据库
                lastDbUpdateTimes.put(taskId, System.currentTimeMillis());
                lastDbProgressValues.put(taskId, progress);
                log.info("Updated part download status from pending to downloading, Task ID: {}, Progress: {}%", taskId, progress);
                // 广播状态和进度更新
                DownloadProgressWebSocket.broadcastStatusUpdate(taskId, 1);
                // 状态变更时强制发送进度更新
                DownloadProgressWebSocket.forceProgressUpdate(taskId, progress);
            } else if (progress >= 100) {
                // 下载完成时更新数据库并广播
                this.updateById(download); // 完成时总是更新数据库
                lastDbUpdateTimes.put(taskId, System.currentTimeMillis());
                lastDbProgressValues.put(taskId, progress);
                log.debug("Updated part download progress on completion, Task ID: {}, Progress: {}%", taskId, progress);
                // 完成时强制发送进度更新
                DownloadProgressWebSocket.forceProgressUpdate(taskId, progress);
            } else {
                // 对于其他进度更新，使用节流逻辑决定是否更新数据库
                boolean shouldUpdateDb = shouldUpdateDatabase(taskId, progress);
                
                if (shouldUpdateDb && (progress >= download.getProgress() || progress == 0)) {
                    this.updateById(download); // 节流更新数据库
                    lastDbUpdateTimes.put(taskId, System.currentTimeMillis());
                    lastDbProgressValues.put(taskId, progress);
                    log.debug("Updated part download progress in database, Task ID: {}, Progress: {}%", taskId, progress);
                } else {
                    log.trace("Database update throttled for Task ID: {}, Progress: {}%", taskId, progress);
                }
                
                // 使用配置化的节流进度更新（WebSocket总是尝试发送，内部有节流）
                log.debug("Sending throttled progress update: taskId={}, progress={}", taskId, progress);
                DownloadProgressWebSocket.broadcastProgressUpdate(taskId, progress, 
                    downloadConfig.getProgressUpdateIntervalMs(), 
                    downloadConfig.getProgressChangeThreshold(), 
                    downloadConfig.isEnableProgressThrottling());
            }
        } else {
            log.warn("VideoDownload not found for taskId: {}", taskId);
        }
    }
    
    /**
     * 判断是否应该更新数据库
     * @param taskId 任务ID
     * @param progress 当前进度
     * @return 是否应该更新数据库
     */
    private boolean shouldUpdateDatabase(Long taskId, Integer progress) {
        long currentTime = System.currentTimeMillis();
        Long lastUpdateTime = lastDbUpdateTimes.get(taskId);
        Integer lastProgress = lastDbProgressValues.get(taskId);
        
        // 首次更新
        if (lastUpdateTime == null || lastProgress == null) {
            return true;
        }
        
        // 完成时总是更新
        if (progress >= 100) {
            return true;
        }
        
        // 重置时总是更新
        if (progress == 0) {
            return true;
        }
        
        // 时间间隔达到
        if (currentTime - lastUpdateTime >= DB_UPDATE_INTERVAL_MS) {
            return true;
        }
        
        // 进度变化达到阈值
        if (Math.abs(progress - lastProgress) >= DB_PROGRESS_THRESHOLD) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 清理任务的节流数据
     * @param taskId 任务ID
     */
    public void clearThrottlingData(Long taskId) {
        lastDbUpdateTimes.remove(taskId);
        lastDbProgressValues.remove(taskId);
        log.debug("Cleared database throttling data for task: {}", taskId);
    }
    
    @Override
    public void completePartDownload(Long taskId, String localPath) {
        VideoDownload download = this.getById(taskId);
        if (download != null) {
            download.setStatus(2); // Completed
            download.setLocalPath(localPath);
            download.setUpdateTime(LocalDateTime.now());
            download.setProgress(100); // 设置进度为100%
            this.updateById(download);
            
            // 更新节流数据
            lastDbUpdateTimes.put(taskId, System.currentTimeMillis());
            lastDbProgressValues.put(taskId, 100);
            
            log.info("Part download completed, Task ID: {}, Path: {}", taskId, localPath);
            // 广播状态和进度更新
            DownloadProgressWebSocket.broadcastStatusUpdate(taskId, 2);
            // 完成时强制发送进度更新
            DownloadProgressWebSocket.forceProgressUpdate(taskId, 100);
            
            // 清理节流数据
            clearThrottlingData(taskId);
        }
    }
    
    @Override
    public void failPartDownload(Long taskId, String errorMessage) {
        VideoDownload download = this.getById(taskId);
        if (download != null) {
            download.setStatus(3); // Failed
            download.setUpdateTime(LocalDateTime.now());
            this.updateById(download);
            log.error("Part download failed, Task ID: {}, Error: {}", taskId, errorMessage);
            // 广播状态更新
            DownloadProgressWebSocket.broadcastStatusUpdate(taskId, 3);
            
            // 清理节流数据
            clearThrottlingData(taskId);
        }
    }
}