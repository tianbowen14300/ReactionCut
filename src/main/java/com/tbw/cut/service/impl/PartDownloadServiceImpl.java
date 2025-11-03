package com.tbw.cut.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.mapper.VideoDownloadMapper;
import com.tbw.cut.service.PartDownloadService;
import com.tbw.cut.websocket.DownloadProgressWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class PartDownloadServiceImpl extends ServiceImpl<VideoDownloadMapper, VideoDownload> implements PartDownloadService {
    
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
                this.updateById(download);
                log.info("Updated part download status from pending to downloading, Task ID: {}, Progress: {}%", taskId, progress);
                // 广播状态和进度更新
                DownloadProgressWebSocket.broadcastStatusUpdate(taskId, 1);
                DownloadProgressWebSocket.broadcastProgressUpdate(taskId, progress);
            } else if (progress >= 100) {
                // 下载完成时更新数据库并广播
                this.updateById(download);
                log.debug("Updated part download progress on completion, Task ID: {}, Progress: {}%", taskId, progress);
                // 广播进度更新
                DownloadProgressWebSocket.broadcastProgressUpdate(taskId, progress);
            } else {
                // 对于其他进度更新，更新数据库并广播进度
                // 确保进度是递增的才更新（除非是重置为0）
                if (progress >= download.getProgress() || progress == 0) {
                    this.updateById(download); // 更新数据库
                    log.debug("Updated part download progress, Task ID: {}, Progress: {}%", taskId, progress);
                }
                // 总是广播进度更新，确保前端能收到实时进度
                log.info("Broadcasting progress update to frontend: taskId={}, progress={}", taskId, progress);
                DownloadProgressWebSocket.broadcastProgressUpdate(taskId, progress);
            }
        } else {
            log.warn("VideoDownload not found for taskId: {}", taskId);
        }
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
            log.info("Part download completed, Task ID: {}, Path: {}", taskId, localPath);
            // 广播状态和进度更新
            DownloadProgressWebSocket.broadcastStatusUpdate(taskId, 2);
            DownloadProgressWebSocket.broadcastProgressUpdate(taskId, 100);
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
        }
    }
}