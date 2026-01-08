package com.tbw.cut.service.download.progress;

import com.tbw.cut.config.DownloadConfig;
import com.tbw.cut.service.download.model.DetailedProgress;
import com.tbw.cut.service.PartDownloadService;
import com.tbw.cut.websocket.DownloadProgressWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进度跟踪器
 * 跟踪和计算下载进度，包括速度和剩余时间估算
 */
@Slf4j
@Service
public class ProgressTracker {
    
    @Autowired
    private PartDownloadService partDownloadService;
    
    @Autowired
    private DownloadConfig downloadConfig;
    
    private final Map<Long, ProgressCalculator> progressCalculators = new ConcurrentHashMap<>();
    private final Map<Long, DetailedProgress> currentProgress = new ConcurrentHashMap<>();
    
    /**
     * 更新下载进度
     * @param taskId 任务ID
     * @param currentBytes 当前已下载字节数
     * @param totalBytes 总字节数
     */
    public void updateProgress(Long taskId, long currentBytes, long totalBytes) {
        ProgressCalculator calculator = progressCalculators.computeIfAbsent(
            taskId, id -> new ProgressCalculator());
        
        DetailedProgress progress = calculator.calculateProgress(currentBytes, totalBytes);
        progress.setTaskId(taskId);
        
        // 更新当前进度
        currentProgress.put(taskId, progress);
        
        // 关键修复：更新数据库中的进度和状态
        try {
            int progressPercentage = progress.getProgressPercentage();
            log.info("更新任务进度到数据库: taskId={}, progress={}%", taskId, progressPercentage);
            partDownloadService.updatePartProgress(taskId, progressPercentage);
        } catch (Exception e) {
            log.error("更新数据库进度失败: taskId={}", taskId, e);
        }
        
        // 推送到前端
        broadcastProgress(taskId, progress);
        
        log.debug("Updated progress for task {}: {}% ({}/{} bytes, speed: {:.2f} KB/s)", 
            taskId, progress.getProgressPercentage(), currentBytes, totalBytes,
            progress.getDownloadSpeedBps() / 1024);
    }
    
    /**
     * 更新分P进度
     * @param taskId 任务ID
     * @param cid 分P的CID
     * @param partTitle 分P标题
     * @param currentBytes 当前已下载字节数
     * @param totalBytes 总字节数
     * @param status 下载状态
     */
    public void updatePartProgress(Long taskId, Long cid, String partTitle, 
                                 long currentBytes, long totalBytes, 
                                 DetailedProgress.DownloadStatus status) {
        
        DetailedProgress progress = currentProgress.computeIfAbsent(taskId, 
            id -> DetailedProgress.builder()
                .taskId(taskId)
                .startTime(LocalDateTime.now())
                .partProgresses(new java.util.ArrayList<>())
                .build());
        
        // 更新或添加分P进度
        DetailedProgress.PartProgress partProgress = progress.getPartProgresses().stream()
            .filter(p -> cid.equals(p.getCid()))
            .findFirst()
            .orElse(null);
        
        if (partProgress == null) {
            partProgress = DetailedProgress.PartProgress.builder()
                .cid(cid)
                .partTitle(partTitle)
                .build();
            progress.getPartProgresses().add(partProgress);
        }
        
        // 更新分P进度信息
        partProgress.setDownloadedBytes(currentBytes);
        partProgress.setTotalBytes(totalBytes);
        partProgress.setStatus(status);
        
        if (totalBytes > 0) {
            partProgress.setProgressPercentage((int) ((currentBytes * 100) / totalBytes));
        }
        
        // 重新计算总体进度
        recalculateOverallProgress(progress);
        
        // 更新时间戳
        progress.setLastUpdateTime(LocalDateTime.now());
        
        // 推送更新
        broadcastProgress(taskId, progress);
    }
    
    /**
     * 获取详细进度信息
     * @param taskId 任务ID
     * @return 详细进度信息
     */
    public DetailedProgress getDetailedProgress(Long taskId) {
        return currentProgress.get(taskId);
    }
    
    /**
     * 设置任务状态
     * @param taskId 任务ID
     * @param status 状态
     * @param errorMessage 错误消息（可选）
     */
    public void setTaskStatus(Long taskId, DetailedProgress.DownloadStatus status, String errorMessage) {
        DetailedProgress progress = currentProgress.get(taskId);
        if (progress != null) {
            progress.setStatus(status);
            progress.setErrorMessage(errorMessage);
            progress.setLastUpdateTime(LocalDateTime.now());
            
            broadcastProgress(taskId, progress);
        }
    }
    
    /**
     * 清理任务进度信息
     * @param taskId 任务ID
     */
    public void cleanupProgress(Long taskId) {
        progressCalculators.remove(taskId);
        currentProgress.remove(taskId);
        log.debug("Cleaned up progress tracking for task: {}", taskId);
    }
    
    /**
     * 重新计算总体进度
     * @param progress 进度信息
     */
    private void recalculateOverallProgress(DetailedProgress progress) {
        if (progress.getPartProgresses().isEmpty()) {
            return;
        }
        
        long totalDownloaded = 0;
        long totalSize = 0;
        
        for (DetailedProgress.PartProgress partProgress : progress.getPartProgresses()) {
            totalDownloaded += partProgress.getDownloadedBytes();
            totalSize += partProgress.getTotalBytes();
        }
        
        progress.setDownloadedBytes(totalDownloaded);
        progress.setTotalBytes(totalSize);
        
        if (totalSize > 0) {
            progress.setProgressPercentage((int) ((totalDownloaded * 100) / totalSize));
        }
        
        // 计算总体下载速度
        ProgressCalculator calculator = progressCalculators.get(progress.getTaskId());
        if (calculator != null) {
            double speed = calculator.calculateSpeed();
            progress.setDownloadSpeedBps(speed);
            
            if (speed > 0) {
                long remainingBytes = totalSize - totalDownloaded;
                progress.setEstimatedRemainingTimeMs((long) ((remainingBytes / speed) * 1000));
            }
        }
    }
    
    /**
     * 广播进度更新
     * @param taskId 任务ID
     * @param progress 进度信息
     */
    private void broadcastProgress(Long taskId, DetailedProgress progress) {
        try {
            // 推送简单进度信息（使用配置化的节流参数）
            DownloadProgressWebSocket.broadcastProgressUpdate(taskId, progress.getProgressPercentage(),
                downloadConfig.getProgressUpdateIntervalMs(),
                downloadConfig.getProgressChangeThreshold(),
                downloadConfig.isEnableProgressThrottling());
            
            // 推送详细进度信息（新增）
            broadcastDetailedProgress(taskId, progress);
            
        } catch (Exception e) {
            log.error("Failed to broadcast progress for task: {}", taskId, e);
        }
    }
    
    /**
     * 广播详细进度信息
     * @param taskId 任务ID
     * @param progress 详细进度信息
     */
    private void broadcastDetailedProgress(Long taskId, DetailedProgress progress) {
        // 构建详细进度消息
        String message = String.format(
            "{\"type\":\"detailed_progress\",\"taskId\":%d,\"progress\":%d," +
            "\"downloadedBytes\":%d,\"totalBytes\":%d,\"speedBps\":%.2f," +
            "\"remainingTimeMs\":%d,\"status\":\"%s\",\"partCount\":%d}",
            taskId, progress.getProgressPercentage(),
            progress.getDownloadedBytes(), progress.getTotalBytes(),
            progress.getDownloadSpeedBps(), progress.getEstimatedRemainingTimeMs(),
            progress.getStatus() != null ? progress.getStatus().name() : "UNKNOWN",
            progress.getPartProgresses() != null ? progress.getPartProgresses().size() : 0
        );
        
        // 使用现有的WebSocket广播机制
        // TODO: 扩展DownloadProgressWebSocket以支持详细进度消息
        log.debug("Broadcasting detailed progress: {}", message);
    }
    
    /**
     * 进度计算器
     */
    private static class ProgressCalculator {
        private final Queue<ProgressSnapshot> snapshots = new LinkedList<>();
        private static final int MAX_SNAPSHOTS = 10;
        
        public DetailedProgress calculateProgress(long currentBytes, long totalBytes) {
            long currentTime = System.currentTimeMillis();
            snapshots.offer(new ProgressSnapshot(currentTime, currentBytes));
            
            // 保持最近的快照
            while (snapshots.size() > MAX_SNAPSHOTS) {
                snapshots.poll();
            }
            
            // 计算下载速度
            double speed = calculateSpeed();
            
            // 计算剩余时间
            long remainingTime = calculateRemainingTime(currentBytes, totalBytes, speed);
            
            return DetailedProgress.builder()
                .progressPercentage(totalBytes > 0 ? (int) ((currentBytes * 100) / totalBytes) : 0)
                .downloadedBytes(currentBytes)
                .totalBytes(totalBytes)
                .downloadSpeedBps(speed)
                .estimatedRemainingTimeMs(remainingTime)
                .lastUpdateTime(LocalDateTime.now())
                .status(currentBytes >= totalBytes && totalBytes > 0 ? 
                    DetailedProgress.DownloadStatus.COMPLETED : 
                    DetailedProgress.DownloadStatus.DOWNLOADING)
                .build();
        }
        
        public double calculateSpeed() {
            if (snapshots.size() < 2) {
                return 0.0;
            }
            
            ProgressSnapshot first = snapshots.peek();
            ProgressSnapshot last = ((LinkedList<ProgressSnapshot>) snapshots).peekLast();
            
            if (first == null || last == null) {
                return 0.0;
            }
            
            long timeDiff = last.getTimestamp() - first.getTimestamp();
            long bytesDiff = last.getBytes() - first.getBytes();
            
            return timeDiff > 0 ? (bytesDiff * 1000.0) / timeDiff : 0.0;
        }
        
        private long calculateRemainingTime(long currentBytes, long totalBytes, double speedBps) {
            if (speedBps <= 0 || totalBytes <= currentBytes) {
                return 0;
            }
            
            long remainingBytes = totalBytes - currentBytes;
            return (long) ((remainingBytes / speedBps) * 1000); // 转换为毫秒
        }
    }
    
    /**
     * 进度快照
     */
    private static class ProgressSnapshot {
        private final long timestamp;
        private final long bytes;
        
        public ProgressSnapshot(long timestamp, long bytes) {
            this.timestamp = timestamp;
            this.bytes = bytes;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public long getBytes() {
            return bytes;
        }
    }
}