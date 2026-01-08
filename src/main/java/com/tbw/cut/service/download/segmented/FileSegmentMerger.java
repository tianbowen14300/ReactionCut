package com.tbw.cut.service.download.segmented;

import com.tbw.cut.service.download.logging.DownloadTimeLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 文件分段合并器
 * 实现分段文件合并逻辑和临时文件清理机制
 */
@Slf4j
@Component
public class FileSegmentMerger {
    
    @Value("${download.merge.buffer-size:65536}") // 64KB
    private int bufferSize;
    
    @Value("${download.merge.verify-checksum:true}")
    private boolean verifyChecksum;
    
    @Value("${download.merge.temp-cleanup-delay:5000}") // 5秒延迟清理
    private long tempCleanupDelayMs;
    
    @Autowired
    private DownloadTimeLogger downloadTimeLogger;
    
    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "file-merger-cleanup");
        t.setDaemon(true);
        return t;
    });
    
    /**
     * 合并分段文件
     * @param segmentFiles 分段文件路径列表（按顺序）
     * @param outputPath 输出文件路径
     * @param expectedSize 预期文件大小（用于验证）
     * @param progressCallback 进度回调
     * @return 合并结果
     */
    public CompletableFuture<MergeResult> mergeSegments(
            List<String> segmentFiles, String outputPath, long expectedSize,
            MergeProgressCallback progressCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            log.info("Starting merge of {} segments to {}", segmentFiles.size(), outputPath);
            
            // 生成任务ID用于日志记录
            Long taskId = System.currentTimeMillis();
            
            try {
                // 验证分段文件存在性
                validateSegmentFiles(segmentFiles);
                
                // 创建输出目录
                createOutputDirectory(outputPath);
                
                // 执行合并
                MergeResult result = performMerge(segmentFiles, outputPath, expectedSize, progressCallback, taskId);
                
                if (result.isSuccess()) {
                    // 记录文件合并时长
                    downloadTimeLogger.logFileMergeTime(taskId, segmentFiles.size(), 
                        result.getMergedBytes(), result.getDurationMs());
                    
                    // 异步清理临时文件
                    scheduleCleanup(segmentFiles);
                }
                
                return result;
                
            } catch (Exception e) {
                log.error("Failed to merge segments", e);
                return MergeResult.failure("Merge failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * 验证分段文件存在性和可读性
     * @param segmentFiles 分段文件列表
     */
    private void validateSegmentFiles(List<String> segmentFiles) throws IOException {
        for (int i = 0; i < segmentFiles.size(); i++) {
            String filePath = segmentFiles.get(i);
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                throw new IOException("Segment file does not exist: " + filePath);
            }
            
            if (!Files.isReadable(path)) {
                throw new IOException("Segment file is not readable: " + filePath);
            }
            
            if (Files.size(path) == 0) {
                log.warn("Segment file {} is empty", filePath);
            }
        }
        
        log.debug("All {} segment files validated", segmentFiles.size());
    }
    
    /**
     * 创建输出目录
     * @param outputPath 输出文件路径
     */
    private void createOutputDirectory(String outputPath) throws IOException {
        Path outputFile = Paths.get(outputPath);
        Path parentDir = outputFile.getParent();
        
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            log.debug("Created output directory: {}", parentDir);
        }
    }
    
    /**
     * 执行文件合并
     * @param segmentFiles 分段文件列表
     * @param outputPath 输出路径
     * @param expectedSize 预期大小
     * @param progressCallback 进度回调
     * @param taskId 任务ID
     * @return 合并结果
     */
    private MergeResult performMerge(List<String> segmentFiles, String outputPath, 
                                   long expectedSize, MergeProgressCallback progressCallback, Long taskId) throws Exception {
        
        long startTime = System.currentTimeMillis();
        long totalMerged = 0;
        MessageDigest md5Digest = verifyChecksum ? MessageDigest.getInstance("MD5") : null;
        
        // 创建临时输出文件
        String tempOutputPath = outputPath + ".merging";
        
        try (FileOutputStream outputStream = new FileOutputStream(tempOutputPath);
             BufferedOutputStream bufferedOutput = new BufferedOutputStream(outputStream, bufferSize)) {
            
            for (int i = 0; i < segmentFiles.size(); i++) {
                String segmentFile = segmentFiles.get(i);
                long segmentSize = Files.size(Paths.get(segmentFile));
                long segmentMerged = 0;
                
                log.debug("Merging segment {}/{}: {} ({} bytes)", 
                    i + 1, segmentFiles.size(), segmentFile, segmentSize);
                
                try (FileInputStream inputStream = new FileInputStream(segmentFile);
                     BufferedInputStream bufferedInput = new BufferedInputStream(inputStream, bufferSize)) {
                    
                    byte[] buffer = new byte[bufferSize];
                    int bytesRead;
                    
                    while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                        bufferedOutput.write(buffer, 0, bytesRead);
                        
                        if (md5Digest != null) {
                            md5Digest.update(buffer, 0, bytesRead);
                        }
                        
                        segmentMerged += bytesRead;
                        totalMerged += bytesRead;
                        
                        // 更新进度
                        if (progressCallback != null) {
                            double segmentProgress = segmentSize > 0 ? (double) segmentMerged / segmentSize : 1.0;
                            double totalProgress = expectedSize > 0 ? (double) totalMerged / expectedSize : 0.0;
                            
                            progressCallback.onProgress(i, segmentProgress, totalProgress, totalMerged);
                        }
                    }
                }
                
                log.debug("Segment {} merged: {} bytes", i + 1, segmentMerged);
            }
            
            // 确保数据写入磁盘
            bufferedOutput.flush();
            outputStream.getFD().sync();
        }
        
        // 验证合并结果
        MergeResult validationResult = validateMergedFile(tempOutputPath, expectedSize, totalMerged);
        if (!validationResult.isSuccess()) {
            // 删除失败的临时文件
            try {
                Files.deleteIfExists(Paths.get(tempOutputPath));
            } catch (IOException e) {
                log.warn("Failed to delete failed merge file: {}", tempOutputPath, e);
            }
            return validationResult;
        }
        
        // 原子性地移动临时文件到最终位置
        try {
            Files.move(Paths.get(tempOutputPath), Paths.get(outputPath), 
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // 如果原子移动失败，尝试普通复制
            log.warn("Atomic move failed, trying copy: {}", e.getMessage());
            Files.copy(Paths.get(tempOutputPath), Paths.get(outputPath), 
                StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(Paths.get(tempOutputPath));
        }
        
        long duration = System.currentTimeMillis() - startTime;
        String checksum = md5Digest != null ? bytesToHex(md5Digest.digest()) : null;
        
        log.info("Merge completed successfully: {} bytes in {} ms (checksum: {})", 
            totalMerged, duration, checksum);
        
        return MergeResult.success(totalMerged, duration, checksum);
    }
    
    /**
     * 验证合并后的文件
     * @param filePath 文件路径
     * @param expectedSize 预期大小
     * @param actualSize 实际大小
     * @return 验证结果
     */
    private MergeResult validateMergedFile(String filePath, long expectedSize, long actualSize) {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return MergeResult.failure("Merged file does not exist");
            }
            
            long fileSize = Files.size(path);
            if (fileSize != actualSize) {
                return MergeResult.failure(
                    String.format("File size mismatch: expected %d, actual %d", actualSize, fileSize));
            }
            
            if (expectedSize > 0 && fileSize != expectedSize) {
                log.warn("File size differs from expected: expected {}, actual {}", expectedSize, fileSize);
                // 不作为错误处理，因为预期大小可能不准确
            }
            
            return MergeResult.success(fileSize, 0, null);
            
        } catch (IOException e) {
            return MergeResult.failure("Failed to validate merged file: " + e.getMessage());
        }
    }
    
    /**
     * 调度临时文件清理
     * @param segmentFiles 要清理的分段文件列表
     */
    private void scheduleCleanup(List<String> segmentFiles) {
        cleanupExecutor.submit(() -> {
            try {
                // 延迟清理，确保文件不再被使用
                Thread.sleep(tempCleanupDelayMs);
                
                cleanupSegmentFiles(segmentFiles);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Cleanup task interrupted");
            }
        });
    }
    
    /**
     * 清理分段文件和临时目录
     * @param segmentFiles 分段文件列表
     */
    public void cleanupSegmentFiles(List<String> segmentFiles) {
        int deletedFiles = 0;
        int failedDeletes = 0;
        
        for (String segmentFile : segmentFiles) {
            try {
                Path path = Paths.get(segmentFile);
                if (Files.deleteIfExists(path)) {
                    deletedFiles++;
                    log.debug("Deleted segment file: {}", segmentFile);
                }
            } catch (IOException e) {
                failedDeletes++;
                log.warn("Failed to delete segment file: {}", segmentFile, e);
            }
        }
        
        // 尝试删除临时目录
        if (!segmentFiles.isEmpty()) {
            try {
                Path firstSegment = Paths.get(segmentFiles.get(0));
                Path tempDir = firstSegment.getParent();
                
                if (tempDir != null && isDirEmpty(tempDir)) {
                    Files.deleteIfExists(tempDir);
                    log.debug("Deleted empty temp directory: {}", tempDir);
                }
            } catch (IOException e) {
                log.warn("Failed to delete temp directory", e);
            }
        }
        
        log.info("Cleanup completed: {} files deleted, {} failed", deletedFiles, failedDeletes);
    }
    
    /**
     * 检查目录是否为空
     * @param dir 目录路径
     * @return 是否为空
     */
    private boolean isDirEmpty(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            return !stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 字节数组转十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * 关闭合并器
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 合并结果类
     */
    public static class MergeResult {
        private final boolean success;
        private final long mergedBytes;
        private final long durationMs;
        private final String checksum;
        private final String errorMessage;
        
        private MergeResult(boolean success, long mergedBytes, long durationMs, 
                           String checksum, String errorMessage) {
            this.success = success;
            this.mergedBytes = mergedBytes;
            this.durationMs = durationMs;
            this.checksum = checksum;
            this.errorMessage = errorMessage;
        }
        
        public static MergeResult success(long mergedBytes, long durationMs, String checksum) {
            return new MergeResult(true, mergedBytes, durationMs, checksum, null);
        }
        
        public static MergeResult failure(String errorMessage) {
            return new MergeResult(false, 0, 0, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public long getMergedBytes() { return mergedBytes; }
        public long getDurationMs() { return durationMs; }
        public String getChecksum() { return checksum; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * 合并进度回调接口
     */
    public interface MergeProgressCallback {
        void onProgress(int segmentIndex, double segmentProgress, double totalProgress, long totalMerged);
    }
}