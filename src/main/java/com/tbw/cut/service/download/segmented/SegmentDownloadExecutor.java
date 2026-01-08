package com.tbw.cut.service.download.segmented;

import com.tbw.cut.service.download.model.DownloadResult;
import com.tbw.cut.service.download.logging.DownloadTimeLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分段下载执行器
 * 实现HTTP Range请求下载和分段进度跟踪
 */
@Slf4j
@Component
public class SegmentDownloadExecutor {
    
    @Value("${download.segment.timeout:30000}")
    private int connectionTimeout;
    
    @Value("${download.segment.read-timeout:300000}")
    private int readTimeout;
    
    @Value("${download.segment.buffer-size:8192}")
    private int bufferSize;
    
    @Value("${download.segment.retry-count:3}")
    private int maxRetries;
    
    @Autowired
    private DownloadTimeLogger downloadTimeLogger;
    
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "segment-download-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });
    
    /**
     * 执行分段下载
     * @param url 下载URL
     * @param outputPath 输出文件路径
     * @param fileSize 文件总大小
     * @param segmentCount 分段数量
     * @param progressCallback 进度回调
     * @return 下载结果
     */
    public CompletableFuture<DownloadResult> executeSegmentedDownload(
            String url, String outputPath, long fileSize, int segmentCount,
            SegmentProgressCallback progressCallback) {
        
        log.info("Starting segmented download: url={}, output={}, size={}, segments={}", 
            url, outputPath, fileSize, segmentCount);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 检查服务器是否支持Range请求
                if (!supportsRangeRequests(url)) {
                    log.warn("Server does not support Range requests, falling back to single download");
                    return executeSingleDownload(url, outputPath, progressCallback);
                }
                
                // 创建分段信息
                List<SegmentInfo> segments = createSegments(fileSize, segmentCount);
                
                // 创建临时文件目录
                Path tempDir = createTempDirectory(outputPath);
                
                // 并行下载所有分段
                List<CompletableFuture<SegmentResult>> segmentFutures = new ArrayList<>();
                AtomicLong totalDownloaded = new AtomicLong(0);
                
                for (int i = 0; i < segments.size(); i++) {
                    SegmentInfo segment = segments.get(i);
                    String tempFilePath = tempDir.resolve("segment_" + i + ".tmp").toString();
                    
                    CompletableFuture<SegmentResult> segmentFuture = downloadSegment(
                        url, segment, tempFilePath, totalDownloaded, fileSize, progressCallback);
                    
                    segmentFutures.add(segmentFuture);
                }
                
                // 等待所有分段下载完成
                CompletableFuture<Void> allSegments = CompletableFuture.allOf(
                    segmentFutures.toArray(new CompletableFuture[0]));
                
                allSegments.get(readTimeout * segmentCount, TimeUnit.MILLISECONDS);
                
                // 检查所有分段是否成功
                List<SegmentResult> results = new ArrayList<>();
                for (CompletableFuture<SegmentResult> future : segmentFutures) {
                    SegmentResult result = future.get();
                    if (!result.isSuccess()) {
                        throw new RuntimeException("Segment download failed: " + result.getErrorMessage());
                    }
                    results.add(result);
                }
                
                // 合并分段文件
                mergeSegments(results, outputPath);
                
                // 清理临时文件
                cleanupTempFiles(tempDir);
                
                log.info("Segmented download completed successfully: {}", outputPath);
                return DownloadResult.success("Segmented download completed");
                
            } catch (Exception e) {
                log.error("Segmented download failed", e);
                return DownloadResult.failure("Segmented download failed: " + e.getMessage());
            }
        }, executorService);
    }
    
    /**
     * 检查服务器是否支持Range请求
     * @param url URL
     * @return 是否支持Range请求
     */
    private boolean supportsRangeRequests(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            
            int responseCode = connection.getResponseCode();
            String acceptRanges = connection.getHeaderField("Accept-Ranges");
            
            connection.disconnect();
            
            return responseCode == 200 && "bytes".equalsIgnoreCase(acceptRanges);
            
        } catch (Exception e) {
            log.warn("Failed to check Range support for URL: {}", url, e);
            return false;
        }
    }
    
    /**
     * 创建分段信息
     * @param fileSize 文件大小
     * @param segmentCount 分段数量
     * @return 分段信息列表
     */
    private List<SegmentInfo> createSegments(long fileSize, int segmentCount) {
        List<SegmentInfo> segments = new ArrayList<>();
        long segmentSize = fileSize / segmentCount;
        
        for (int i = 0; i < segmentCount; i++) {
            long start = i * segmentSize;
            long end = (i == segmentCount - 1) ? fileSize - 1 : start + segmentSize - 1;
            
            segments.add(new SegmentInfo(i, start, end));
        }
        
        return segments;
    }
    
    /**
     * 创建临时文件目录
     * @param outputPath 输出文件路径
     * @return 临时目录路径
     */
    private Path createTempDirectory(String outputPath) throws IOException {
        Path outputFile = Paths.get(outputPath);
        Path tempDir = outputFile.getParent().resolve(outputFile.getFileName() + ".segments");
        
        if (Files.exists(tempDir)) {
            // 清理已存在的临时目录
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a)) // 先删除文件，再删除目录
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.warn("Failed to delete temp file: {}", path, e);
                    }
                });
        }
        
        Files.createDirectories(tempDir);
        return tempDir;
    }
    
    /**
     * 下载单个分段
     * @param url 下载URL
     * @param segment 分段信息
     * @param tempFilePath 临时文件路径
     * @param totalDownloaded 总下载字节数
     * @param totalSize 总文件大小
     * @param progressCallback 进度回调
     * @return 分段下载结果
     */
    private CompletableFuture<SegmentResult> downloadSegment(
            String url, SegmentInfo segment, String tempFilePath,
            AtomicLong totalDownloaded, long totalSize, SegmentProgressCallback progressCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            int retryCount = 0;
            Exception lastException = null;
            
            while (retryCount < maxRetries) {
                try {
                    return downloadSegmentWithRetry(url, segment, tempFilePath, 
                        totalDownloaded, totalSize, progressCallback);
                        
                } catch (Exception e) {
                    lastException = e;
                    retryCount++;
                    
                    if (retryCount < maxRetries) {
                        log.warn("Segment {} download failed (attempt {}), retrying...", 
                            segment.getIndex(), retryCount, e);
                        
                        try {
                            Thread.sleep(1000 * retryCount); // 指数退避
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            log.error("Segment {} download failed after {} retries", segment.getIndex(), maxRetries, lastException);
            return SegmentResult.failure(segment.getIndex(), tempFilePath, 
                "Download failed after " + maxRetries + " retries: " + lastException.getMessage());
                
        }, executorService);
    }
    
    /**
     * 执行分段下载（带重试）
     * @param url 下载URL
     * @param segment 分段信息
     * @param tempFilePath 临时文件路径
     * @param totalDownloaded 总下载字节数
     * @param totalSize 总文件大小
     * @param progressCallback 进度回调
     * @return 分段下载结果
     */
    private SegmentResult downloadSegmentWithRetry(
            String url, SegmentInfo segment, String tempFilePath,
            AtomicLong totalDownloaded, long totalSize, SegmentProgressCallback progressCallback) throws Exception {
        
        long segmentStartTime = System.currentTimeMillis();
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        
        try {
            // 创建HTTP连接
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            
            // 设置Range请求头
            String rangeHeader = String.format("bytes=%d-%d", segment.getStart(), segment.getEnd());
            connection.setRequestProperty("Range", rangeHeader);
            
            // 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != 206) { // 206 Partial Content
                throw new IOException("Unexpected response code: " + responseCode);
            }
            
            // 开始下载
            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(tempFilePath);
            
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            long segmentDownloaded = 0;
            long segmentSize = segment.getEnd() - segment.getStart() + 1;
            
            // 进度节流变量
            long lastProgressUpdateTime = 0;
            double lastReportedProgress = -1;
            final long PROGRESS_UPDATE_INTERVAL_MS = 500; // 500ms最小间隔
            final double MIN_PROGRESS_CHANGE = 0.01; // 最小1%变化
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                segmentDownloaded += bytesRead;
                
                // 更新进度（带节流）
                long currentTotal = totalDownloaded.addAndGet(bytesRead);
                if (progressCallback != null) {
                    double progress = totalSize > 0 ? (double) currentTotal / totalSize : 0.0;
                    long currentTime = System.currentTimeMillis();
                    
                    // 节流逻辑：检查时间间隔和进度变化
                    boolean shouldUpdate = false;
                    if (lastReportedProgress < 0) {
                        // 首次更新
                        shouldUpdate = true;
                    } else if (progress >= 1.0) {
                        // 完成时总是更新
                        shouldUpdate = true;
                    } else if (currentTime - lastProgressUpdateTime >= PROGRESS_UPDATE_INTERVAL_MS) {
                        // 时间间隔达到
                        shouldUpdate = true;
                    } else if (Math.abs(progress - lastReportedProgress) >= MIN_PROGRESS_CHANGE) {
                        // 进度变化达到阈值
                        shouldUpdate = true;
                    }
                    
                    if (shouldUpdate) {
                        lastProgressUpdateTime = currentTime;
                        lastReportedProgress = progress;
                        progressCallback.onProgress(segment.getIndex(), segmentDownloaded, segmentSize, progress);
                    }
                }
            }
            
            // 记录分段下载完成时长
            long segmentDuration = System.currentTimeMillis() - segmentStartTime;
            if (progressCallback instanceof SegmentProgressCallbackWithLogging) {
                ((SegmentProgressCallbackWithLogging) progressCallback).onSegmentComplete(
                    segment.getIndex(), segmentDownloaded, segmentDuration);
            }
            
            log.debug("Segment {} download completed: {} bytes in {} ms", 
                segment.getIndex(), segmentDownloaded, segmentDuration);
            return SegmentResult.success(segment.getIndex(), tempFilePath, segmentDownloaded);
            
        } finally {
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException e) { /* ignore */ }
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { /* ignore */ }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 执行单文件下载（降级方案）
     * @param url 下载URL
     * @param outputPath 输出路径
     * @param progressCallback 进度回调
     * @return 下载结果
     */
    private DownloadResult executeSingleDownload(String url, String outputPath, 
                                               SegmentProgressCallback progressCallback) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            
            long fileSize = connection.getContentLengthLong();
            
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
                
                byte[] buffer = new byte[bufferSize];
                int bytesRead;
                long totalDownloaded = 0;
                
                // 进度节流变量
                long lastProgressUpdateTime = 0;
                double lastReportedProgress = -1;
                final long PROGRESS_UPDATE_INTERVAL_MS = 500; // 500ms最小间隔
                final double MIN_PROGRESS_CHANGE = 0.01; // 最小1%变化
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalDownloaded += bytesRead;
                    
                    if (progressCallback != null) {
                        double progress = fileSize > 0 ? (double) totalDownloaded / fileSize : 0.0;
                        long currentTime = System.currentTimeMillis();
                        
                        // 节流逻辑：检查时间间隔和进度变化
                        boolean shouldUpdate = false;
                        if (lastReportedProgress < 0) {
                            // 首次更新
                            shouldUpdate = true;
                        } else if (progress >= 1.0) {
                            // 完成时总是更新
                            shouldUpdate = true;
                        } else if (currentTime - lastProgressUpdateTime >= PROGRESS_UPDATE_INTERVAL_MS) {
                            // 时间间隔达到
                            shouldUpdate = true;
                        } else if (Math.abs(progress - lastReportedProgress) >= MIN_PROGRESS_CHANGE) {
                            // 进度变化达到阈值
                            shouldUpdate = true;
                        }
                        
                        if (shouldUpdate) {
                            lastProgressUpdateTime = currentTime;
                            lastReportedProgress = progress;
                            progressCallback.onProgress(0, totalDownloaded, fileSize, progress);
                        }
                    }
                }
                
                log.info("Single download completed: {} bytes", totalDownloaded);
                return DownloadResult.success("Single download completed");
            }
            
        } catch (Exception e) {
            log.error("Single download failed", e);
            return DownloadResult.failure("Single download failed: " + e.getMessage());
        }
    }
    
    /**
     * 合并分段文件
     * @param segmentResults 分段结果列表
     * @param outputPath 输出文件路径
     */
    private void mergeSegments(List<SegmentResult> segmentResults, String outputPath) throws IOException {
        log.info("Merging {} segments to {}", segmentResults.size(), outputPath);
        
        // 按分段索引排序
        segmentResults.sort((a, b) -> Integer.compare(a.getSegmentIndex(), b.getSegmentIndex()));
        
        try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
            for (SegmentResult result : segmentResults) {
                try (FileInputStream inputStream = new FileInputStream(result.getTempFilePath())) {
                    byte[] buffer = new byte[bufferSize];
                    int bytesRead;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
        
        log.info("Segments merged successfully");
    }
    
    /**
     * 清理临时文件
     * @param tempDir 临时目录
     */
    private void cleanupTempFiles(Path tempDir) {
        try {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.warn("Failed to delete temp file: {}", path, e);
                    }
                });
            log.debug("Temp files cleaned up: {}", tempDir);
        } catch (IOException e) {
            log.warn("Failed to cleanup temp directory: {}", tempDir, e);
        }
    }
    
    /**
     * 关闭执行器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 分段信息内部类
     */
    private static class SegmentInfo {
        private final int index;
        private final long start;
        private final long end;
        
        public SegmentInfo(int index, long start, long end) {
            this.index = index;
            this.start = start;
            this.end = end;
        }
        
        public int getIndex() { return index; }
        public long getStart() { return start; }
        public long getEnd() { return end; }
    }
    
    /**
     * 分段下载结果内部类
     */
    private static class SegmentResult {
        private final boolean success;
        private final int segmentIndex;
        private final String tempFilePath;
        private final long downloadedBytes;
        private final String errorMessage;
        
        private SegmentResult(boolean success, int segmentIndex, String tempFilePath, 
                             long downloadedBytes, String errorMessage) {
            this.success = success;
            this.segmentIndex = segmentIndex;
            this.tempFilePath = tempFilePath;
            this.downloadedBytes = downloadedBytes;
            this.errorMessage = errorMessage;
        }
        
        public static SegmentResult success(int segmentIndex, String tempFilePath, long downloadedBytes) {
            return new SegmentResult(true, segmentIndex, tempFilePath, downloadedBytes, null);
        }
        
        public static SegmentResult failure(int segmentIndex, String tempFilePath, String errorMessage) {
            return new SegmentResult(false, segmentIndex, tempFilePath, 0, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public int getSegmentIndex() { return segmentIndex; }
        public String getTempFilePath() { return tempFilePath; }
        public long getDownloadedBytes() { return downloadedBytes; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * 分段进度回调接口
     */
    public interface SegmentProgressCallback {
        void onProgress(int segmentIndex, long segmentDownloaded, long segmentSize, double totalProgress);
    }
    
    /**
     * 带日志记录的分段进度回调接口
     */
    public interface SegmentProgressCallbackWithLogging extends SegmentProgressCallback {
        void onSegmentComplete(int segmentIndex, long segmentSize, long durationMs);
    }
}