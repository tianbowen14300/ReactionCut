package com.tbw.cut.service.download.segmented;

import com.tbw.cut.service.download.model.*;
import com.tbw.cut.service.download.EnhancedDownloadManager;
import com.tbw.cut.service.download.logging.DownloadTimeLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 分段下载管理器
 * 决定是否使用分段下载，并协调分段下载过程
 */
@Slf4j
@Service
public class SegmentedDownloadManager {
    
    @Autowired
    private EnhancedDownloadManager enhancedDownloadManager;
    
    @Autowired
    private OptimalThreadCalculator threadCalculator;
    
    @Autowired
    private SegmentDownloadExecutor segmentExecutor;
    
    @Autowired
    private FileSegmentMerger fileMerger;
    
    @Autowired
    private DownloadTimeLogger downloadTimeLogger;
    
    @Autowired
    private com.tbw.cut.service.PartDownloadService partDownloadService;
    
    @Value("${download.segmented.enabled:true}")
    private boolean segmentedDownloadEnabled;
    
    @Value("${download.segmented.min-file-size:50485760}") // 50MB
    private long minFileSizeForSegmentation;
    
    @Value("${download.segmented.max-segments:8}")
    private int maxSegmentsPerFile;
    
    @Value("${download.segmented.segment-size:10485760}") // 10MB
    private long defaultSegmentSize;
    
    /**
     * 下载视频
     * @param request 下载请求
     * @return 下载结果
     */
    public CompletableFuture<DownloadResult> downloadVideo(VideoDownloadRequest request) {
        log.info("Starting video download: {} (parts: {})", 
            request.getVideoTitle(), request.getPartCount());
        
        try {
            // 分析是否应该使用分段下载
            SegmentationStrategy strategy = analyzeSegmentationStrategy(request);
            
            if (strategy.shouldUseSegmentation()) {
                log.info("Using segmented download for video: {} (strategy: {})", 
                    request.getVideoTitle(), strategy.getReason());
                return executeSegmentedDownload(request, strategy);
            } else {
                log.info("Using standard download for video: {} (reason: {})", 
                    request.getVideoTitle(), strategy.getReason());
                return executeStandardDownload(request);
            }
            
        } catch (Exception e) {
            log.error("Failed to start video download: {}", request.getVideoTitle(), e);
            CompletableFuture<DownloadResult> future = new CompletableFuture<>();
            future.complete(DownloadResult.failure("Failed to start download: " + e.getMessage()));
            return future;
        }
    }
    
    /**
     * 分析分段下载策略
     * @param request 下载请求
     * @return 分段策略
     */
    private SegmentationStrategy analyzeSegmentationStrategy(VideoDownloadRequest request) {
        // 检查是否启用分段下载
        if (!segmentedDownloadEnabled) {
            return SegmentationStrategy.noSegmentation("Segmented download is disabled");
        }
        
        // 检查用户是否明确禁用分段下载
        if (!request.isEnableSegmentedDownload()) {
            return SegmentationStrategy.noSegmentation("User disabled segmented download");
        }
        
        // 检查文件大小 - 降低分段下载的最小文件大小阈值
        long adjustedMinSize = minFileSizeForSegmentation / 5; // 降低到10MB
        long totalEstimatedSize = request.getTotalEstimatedSize();
        if (totalEstimatedSize > 0 && totalEstimatedSize < adjustedMinSize) {
            return SegmentationStrategy.noSegmentation(
                String.format("File size (%d bytes) below threshold (%d bytes)", 
                    totalEstimatedSize, adjustedMinSize));
        }
        
        // 检查分P数量 - 调整策略，允许多分P视频使用分段下载
        if (request.getPartCount() > 8) {
            // 只有当分P数量非常多时才禁用分段下载
            return SegmentationStrategy.noSegmentation(
                String.format("Too many parts (%d), segmentation may not be beneficial", 
                    request.getPartCount()));
        }
        
        // 计算最优分段数
        long requestEstimatedSize = request.getTotalEstimatedSize();
        int optimalSegments = calculateOptimalSegments(requestEstimatedSize);
        
        return SegmentationStrategy.useSegmentation(
            String.format("File size %d bytes, using %d segments", requestEstimatedSize, optimalSegments),
            optimalSegments);
    }
    
    /**
     * 计算最优分段数
     * @param fileSize 文件大小
     * @return 最优分段数
     */
    private int calculateOptimalSegments(long fileSize) {
        if (fileSize <= 0) {
            return 1;
        }
        
        // 基于文件大小计算分段数
        int segments = (int) Math.ceil((double) fileSize / defaultSegmentSize);
        
        // 限制在合理范围内
        segments = Math.max(1, Math.min(segments, maxSegmentsPerFile));
        
        // 考虑系统资源 - 这里可以后续集成OptimalThreadCalculator
        // 暂时使用简单的启发式算法
        int availableThreads = Runtime.getRuntime().availableProcessors();
        segments = Math.min(segments, availableThreads * 2);
        
        return segments;
    }
    
    /**
     * 执行分段下载
     * @param request 下载请求
     * @param strategy 分段策略
     * @return 下载结果
     */
    private CompletableFuture<DownloadResult> executeSegmentedDownload(
            VideoDownloadRequest request, SegmentationStrategy strategy) {
        
        log.info("Executing segmented download with {} segments", strategy.getSegmentCount());
        
        // 对于多分P视频，对每个分P使用分段下载
        if (request.isMultiPart()) {
            log.info("Multi-part video detected, using segmented download for each part");
            return executeMultiPartSegmentedDownload(request, strategy);
        }
        
        // 单个文件的分段下载
        if (request.getParts().size() == 1) {
            VideoPart part = request.getParts().get(0);
            String outputPath = generateOutputPath(request, part);
            long fileSize = request.getTotalEstimatedSize();
            
            // 获取数据库任务ID
            Long databaseTaskId = part.getDatabaseId();
            
            return segmentExecutor.executeSegmentedDownload(
                part.getUrl(), 
                outputPath, 
                fileSize, 
                strategy.getSegmentCount(),
                new SegmentProgressCallbackImpl(request.getVideoTitle(), databaseTaskId, partDownloadService)
            );
        }
        
        // 降级到标准下载
        log.warn("Segmented download conditions not met, falling back to standard download");
        return executeStandardDownload(request);
    }
    
    /**
     * 执行多分P分段下载
     * @param request 下载请求
     * @param strategy 分段策略
     * @return 下载结果
     */
    private CompletableFuture<DownloadResult> executeMultiPartSegmentedDownload(
            VideoDownloadRequest request, SegmentationStrategy strategy) {
        
        log.info("Starting multi-part segmented download for {} parts", request.getPartCount());
        
        // 为每个分P创建分段下载任务
        List<CompletableFuture<String>> partFutures = new ArrayList<>();
        
        for (int i = 0; i < request.getParts().size(); i++) {
            VideoPart part = request.getParts().get(i);
            String outputPath = generateOutputPath(request, part);
            
            // 估算每个分P的大小（平均分配）
            long estimatedPartSize = request.getTotalEstimatedSize() / request.getPartCount();
            
            // 为每个分P使用分段下载
            CompletableFuture<String> partFuture = segmentExecutor.executeSegmentedDownload(
                part.getUrl(),
                outputPath,
                estimatedPartSize,
                strategy.getSegmentCount(),
                new SegmentProgressCallbackImpl(request.getVideoTitle() + " - Part " + (i + 1), 
                                              part.getDatabaseId(), partDownloadService)
            ).thenApply(result -> {
                if (result.isSuccess()) {
                    return outputPath;
                } else {
                    throw new RuntimeException("Part download failed: " + result.getErrorMessage());
                }
            });
            
            partFutures.add(partFuture);
        }
        
        // 等待所有分P下载完成
        return CompletableFuture.allOf(partFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<String> downloadedPaths = partFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(java.util.stream.Collectors.toList());
                
                log.info("Multi-part segmented download completed: {} parts", downloadedPaths.size());
                return DownloadResult.success(downloadedPaths);
            })
            .exceptionally(throwable -> {
                log.error("Multi-part segmented download failed", throwable);
                return DownloadResult.failure("Multi-part segmented download failed: " + throwable.getMessage());
            });
    }
    
    /**
     * 执行标准下载
     * @param request 下载请求
     * @return 下载结果
     */
    private CompletableFuture<DownloadResult> executeStandardDownload(VideoDownloadRequest request) {
        return enhancedDownloadManager.startDownload(
            request.getVideoUrl(), 
            request.getParts(), 
            request.getConfig() != null ? request.getConfig() : createDefaultConfig()
        );
    }
    
    /**
     * 创建默认下载配置
     * @return 默认配置
     */
    private DownloadConfig createDefaultConfig() {
        return DownloadConfig.builder()
            .connectionTimeout(30000L)
            .readTimeout(1800000L) // 30分钟
            .progressUpdateInterval(1000L)
            .build();
    }
    
    /**
     * 生成输出文件路径
     * @param request 下载请求
     * @param part 视频分P
     * @return 输出路径
     */
    private String generateOutputPath(VideoDownloadRequest request, VideoPart part) {
        String outputDir = request.getOutputDirectory();
        if (outputDir == null) {
            outputDir = System.getProperty("java.io.tmpdir");
        }
        
        String fileName = request.getVideoTitle() + "_" + part.getTitle() + ".mp4";
        // 清理文件名中的非法字符
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        return outputDir + "/" + fileName;
    }
    
    /**
     * 分段进度回调实现
     */
    private class SegmentProgressCallbackImpl implements SegmentDownloadExecutor.SegmentProgressCallbackWithLogging {
        private final String videoTitle;
        private final Long databaseTaskId;
        private final com.tbw.cut.service.PartDownloadService partDownloadService;
        
        public SegmentProgressCallbackImpl(String videoTitle, Long databaseTaskId, 
                                         com.tbw.cut.service.PartDownloadService partDownloadService) {
            this.videoTitle = videoTitle;
            this.databaseTaskId = databaseTaskId;
            this.partDownloadService = partDownloadService;
        }
        
        @Override
        public void onProgress(int segmentIndex, long segmentDownloaded, long segmentSize, double totalProgress) {
            // 将分段进度转换为百分比并更新到数据库
            int progressPercentage = (int) (totalProgress * 100);
            
            if (databaseTaskId != null && partDownloadService != null) {
                // 关键修复：使用实际的数据库任务ID更新进度
                partDownloadService.updatePartProgress(databaseTaskId, progressPercentage);
            }
            
            if (segmentIndex == 0 || segmentIndex % 2 == 0) { // 减少日志频率
                log.debug("Segmented download progress for {} (taskId={}): segment {} - {}/{} bytes, total: {:.1f}%", 
                    videoTitle, databaseTaskId, segmentIndex, segmentDownloaded, segmentSize, totalProgress * 100);
            }
        }
        
        @Override
        public void onSegmentComplete(int segmentIndex, long segmentSize, long durationMs) {
            // 记录分段下载完成时长
            if (databaseTaskId != null) {
                downloadTimeLogger.logSegmentDownloadTime(databaseTaskId, segmentIndex, segmentSize, durationMs);
            }
        }
    }
    private static class SegmentationStrategy {
        private final boolean useSegmentation;
        private final String reason;
        private final int segmentCount;
        
        private SegmentationStrategy(boolean useSegmentation, String reason, int segmentCount) {
            this.useSegmentation = useSegmentation;
            this.reason = reason;
            this.segmentCount = segmentCount;
        }
        
        public static SegmentationStrategy useSegmentation(String reason, int segmentCount) {
            return new SegmentationStrategy(true, reason, segmentCount);
        }
        
        public static SegmentationStrategy noSegmentation(String reason) {
            return new SegmentationStrategy(false, reason, 1);
        }
        
        public boolean shouldUseSegmentation() {
            return useSegmentation;
        }
        
        public String getReason() {
            return reason;
        }
        
        public int getSegmentCount() {
            return segmentCount;
        }
    }
}