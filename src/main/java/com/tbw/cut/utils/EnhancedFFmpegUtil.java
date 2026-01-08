package com.tbw.cut.utils;

import com.tbw.cut.service.download.model.DownloadConfig;
import com.tbw.cut.service.download.retry.RetryManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 增强的FFmpeg工具类
 * 提供更好的错误处理、重试机制和进度跟踪
 */
@Slf4j
@Component
public class EnhancedFFmpegUtil {
    
    @Value("${app.ffmpeg-path:/opt/homebrew/bin/ffmpeg}")
    private String ffmpegPath;
    
    @Value("${app.ffprobe-path:/opt/homebrew/bin/ffprobe}")
    private String ffprobePath;
    
    @Value("${app.video-storage-dir:./videos}")
    private String videoStorageDir;
    
    @Value("${app.temp-dir:./temp}")
    private String tempDir;
    
    @Autowired
    private RetryManager retryManager;
    
    @PostConstruct
    public void init() {
        createDirectories();
        verifyFFmpegInstallation();
    }
    
    /**
     * 创建必要的目录
     */
    private void createDirectories() {
        createDirectory(videoStorageDir);
        createDirectory(tempDir);
        createDirectory(tempDir + "/clips");
        createDirectory(tempDir + "/partial");
    }
    
    /**
     * 验证FFmpeg安装
     */
    private void verifyFFmpegInstallation() {
        try {
            ProcessResult result = executeCommand(
                java.util.Arrays.asList(ffmpegPath, "-version"), 
                10, TimeUnit.SECONDS
            );
            
            if (result.isSuccess() && result.getOutput().contains("ffmpeg")) {
                log.info("FFmpeg installation verified");
            } else {
                log.warn("FFmpeg verification failed. Please check FFmpeg installation.");
            }
        } catch (Exception e) {
            log.error("FFmpeg verification failed", e);
        }
    }
    
    /**
     * 获取视频时长（带重试机制）
     * @param videoUrl 视频URL
     * @return 视频时长（微秒）
     */
    public long getVideoDuration(String videoUrl) {
        return retryManager.executeWithRetry(() -> {
            return executeGetDuration(videoUrl);
        }, "get-duration-" + videoUrl.hashCode()).join();
    }
    
    /**
     * 执行获取视频时长
     * @param videoUrl 视频URL
     * @return 视频时长（微秒）
     */
    private long executeGetDuration(String videoUrl) {
        List<String> command = buildDurationCommand(videoUrl);
        
        try {
            ProcessResult result = executeCommand(command, 60, TimeUnit.SECONDS);
            
            if (result.isSuccess() && !result.getOutput().trim().isEmpty()) {
                double durationInSeconds = Double.parseDouble(result.getOutput().trim());
                
                // 验证时长合理性
                if (durationInSeconds > 0 && durationInSeconds <= 86400) { // 24小时内
                    long durationInMicroseconds = (long) (durationInSeconds * 1_000_000);
                    log.debug("Got video duration: {} seconds ({} microseconds)", 
                        durationInSeconds, durationInMicroseconds);
                    return durationInMicroseconds;
                } else {
                    log.warn("Invalid duration: {} seconds", durationInSeconds);
                }
            }
        } catch (NumberFormatException e) {
            log.error("Failed to parse duration from output: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get video duration", e);
            throw new RuntimeException("Duration detection failed", e);
        }
        
        return 0;
    }
    
    /**
     * 下载视频（带进度跟踪和断点续传支持）
     * @param videoUrl 视频URL
     * @param outputPath 输出路径
     * @param config 下载配置
     * @param progressCallback 进度回调
     * @return 下载结果
     */
    public DownloadResult downloadVideo(String videoUrl, String outputPath, 
                                      DownloadConfig config, ProgressCallback progressCallback) {
        
        return retryManager.executeWithRetry(() -> {
            return executeDownload(videoUrl, outputPath, config, progressCallback);
        }, "download-" + outputPath.hashCode()).join();
    }
    
    /**
     * 支持断点续传的下载
     * @param videoUrl 视频URL
     * @param outputPath 输出路径
     * @param resumeFromBytes 从指定字节位置开始下载
     * @param config 下载配置
     * @param progressCallback 进度回调
     * @return 下载结果
     */
    public DownloadResult downloadVideoWithResume(String videoUrl, String outputPath, 
                                                long resumeFromBytes, DownloadConfig config, 
                                                ProgressCallback progressCallback) {
        
        return retryManager.executeWithRetry(() -> {
            return executeDownloadWithResume(videoUrl, outputPath, resumeFromBytes, config, progressCallback);
        }, "resume-download-" + outputPath.hashCode()).join();
    }
    
    /**
     * 执行视频下载
     * @param videoUrl 视频URL
     * @param outputPath 输出路径
     * @param config 下载配置
     * @param progressCallback 进度回调
     * @return 下载结果
     */
    private DownloadResult executeDownload(String videoUrl, String outputPath, 
                                         DownloadConfig config, ProgressCallback progressCallback) {
        
        List<String> command = buildDownloadCommand(videoUrl, outputPath, config, 0);
        
        try {
            ProcessResult result = executeCommandWithProgress(command, config, progressCallback);
            
            if (result.isSuccess()) {
                File outputFile = new File(outputPath);
                if (outputFile.exists() && outputFile.length() > 0) {
                    return DownloadResult.success(outputPath, outputFile.length());
                } else {
                    return DownloadResult.failure("Output file not found or empty");
                }
            } else {
                return DownloadResult.failure("Download failed with exit code: " + result.getExitCode());
            }
            
        } catch (Exception e) {
            log.error("Download execution failed", e);
            return DownloadResult.failure("Download execution failed: " + e.getMessage());
        }
    }
    
    /**
     * 执行断点续传下载
     * @param videoUrl 视频URL
     * @param outputPath 输出路径
     * @param resumeFromBytes 从指定字节位置开始
     * @param config 下载配置
     * @param progressCallback 进度回调
     * @return 下载结果
     */
    private DownloadResult executeDownloadWithResume(String videoUrl, String outputPath, 
                                                   long resumeFromBytes, DownloadConfig config, 
                                                   ProgressCallback progressCallback) {
        
        List<String> command = buildDownloadCommand(videoUrl, outputPath, config, resumeFromBytes);
        
        try {
            ProcessResult result = executeCommandWithProgress(command, config, progressCallback);
            
            if (result.isSuccess()) {
                File outputFile = new File(outputPath);
                if (outputFile.exists() && outputFile.length() > resumeFromBytes) {
                    return DownloadResult.success(outputPath, outputFile.length());
                } else {
                    return DownloadResult.failure("Resume download failed");
                }
            } else {
                return DownloadResult.failure("Resume download failed with exit code: " + result.getExitCode());
            }
            
        } catch (Exception e) {
            log.error("Resume download execution failed", e);
            return DownloadResult.failure("Resume download execution failed: " + e.getMessage());
        }
    }
    
    /**
     * 构建获取时长的命令
     * @param videoUrl 视频URL
     * @return 命令列表
     */
    private List<String> buildDurationCommand(String videoUrl) {
        List<String> command = new ArrayList<>();
        command.add(ffprobePath);
        command.add("-user_agent");
        command.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        command.add("-headers");
        command.add("Referer: https://www.bilibili.com/");
        command.add("-timeout");
        command.add("30000000"); // 30秒超时
        command.add("-v");
        command.add("error");
        command.add("-show_entries");
        command.add("format=duration");
        command.add("-of");
        command.add("default=noprint_wrappers=1:nokey=1");
        command.add("-i");
        command.add(videoUrl);
        
        return command;
    }
    
    /**
     * 构建下载命令
     * @param videoUrl 视频URL
     * @param outputPath 输出路径
     * @param config 下载配置
     * @param resumeFromBytes 断点续传起始位置
     * @return 命令列表
     */
    private List<String> buildDownloadCommand(String videoUrl, String outputPath, 
                                            DownloadConfig config, long resumeFromBytes) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        
        // 网络参数
        command.add("-user_agent");
        command.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        command.add("-headers");
        command.add("Referer: https://www.bilibili.com/");
        
        // 超时和重连参数
        if (config.getConnectionTimeout() != null) {
            command.add("-timeout");
            command.add(String.valueOf(config.getConnectionTimeout() * 1000)); // 转换为微秒
        }
        command.add("-reconnect");
        command.add("1");
        command.add("-reconnect_streamed");
        command.add("1");
        command.add("-reconnect_delay_max");
        command.add("5");
        
        // 断点续传支持
        if (resumeFromBytes > 0) {
            command.add("-ss");
            command.add(String.valueOf(resumeFromBytes / 1000000.0)); // 转换为秒
        }
        
        command.add("-i");
        command.add(videoUrl);
        command.add("-c");
        command.add("copy"); // 流复制，提高速度
        command.add("-y"); // 覆盖输出文件
        command.add("-progress");
        command.add("pipe:1"); // 进度输出到标准输出
        command.add(outputPath);
        
        return command;
    }
    
    /**
     * 执行带进度跟踪的命令
     * @param command 命令列表
     * @param config 下载配置
     * @param progressCallback 进度回调
     * @return 执行结果
     */
    private ProcessResult executeCommandWithProgress(List<String> command, DownloadConfig config, 
                                                   ProgressCallback progressCallback) throws Exception {
        
        log.info("Executing FFmpeg command: {}", String.join(" ", command));
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // 进度跟踪
        ProgressTracker tracker = new ProgressTracker(progressCallback, config.getProgressUpdateInterval());
        
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                
                // 解析进度信息
                tracker.parseLine(line);
                
                log.debug("FFmpeg output: {}", line);
            }
        }
        
        // 等待进程完成
        long timeoutMs = config.getReadTimeout() != null ? config.getReadTimeout() : 1800000; // 默认30分钟
        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        
        if (!finished) {
            log.warn("Process timed out, terminating...");
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            throw new InterruptedException("Process timed out");
        }
        
        int exitCode = process.exitValue();
        
        // 确保最终进度为100%
        if (exitCode == 0 && progressCallback != null) {
            progressCallback.onProgress(100, 0, 0);
        }
        
        return new ProcessResult(exitCode == 0, exitCode, output.toString());
    }
    
    /**
     * 执行简单命令
     * @param command 命令列表
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @return 执行结果
     */
    private ProcessResult executeCommand(List<String> command, long timeout, TimeUnit timeUnit) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);
        
        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();
        
        // 读取标准输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // 读取错误输出
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }
        }
        
        boolean finished = process.waitFor(timeout, timeUnit);
        
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            throw new InterruptedException("Command timed out");
        }
        
        int exitCode = process.exitValue();
        String result = output.length() > 0 ? output.toString() : error.toString();
        
        return new ProcessResult(exitCode == 0, exitCode, result);
    }
    
    /**
     * 创建目录
     * @param path 目录路径
     */
    private void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.debug("Created directory: {}", path);
            } else {
                log.warn("Failed to create directory: {}", path);
            }
        }
    }
    
    // Getter methods
    public String getVideoStorageDir() {
        return videoStorageDir;
    }
    
    public String getTempDir() {
        return tempDir;
    }
    
    /**
     * 兼容方法：获取视频时长（使用FFprobe）
     * @param videoUrl 视频URL
     * @return 视频时长（微秒）
     */
    public long getDurationByFFprobe(String videoUrl) {
        return getVideoDuration(videoUrl);
    }
    
    /**
     * 兼容方法：下载视频到指定目录（带进度跟踪）
     * @param videoUrl 视频URL
     * @param outputFileName 输出文件名
     * @param outputDirectory 输出目录
     * @param totalDuration 总时长（微秒）
     * @param progressCallback 进度回调
     * @return 输出文件路径，失败返回null
     */
    public String downloadVideoToDirectoryWithProgress(String videoUrl, String outputFileName, 
                                                     String outputDirectory, long totalDuration, 
                                                     FFmpegUtil.ProgressCallback progressCallback) {
        
        // 确保输出目录存在
        createDirectory(outputDirectory);
        
        String outputPath = outputDirectory + "/" + outputFileName;
        
        // 创建配置
        DownloadConfig config = DownloadConfig.builder()
            .connectionTimeout(30000L)
            .readTimeout(1800000L) // 30分钟
            .progressUpdateInterval(1000L)
            .build();
        
        // 转换进度回调
        ProgressCallback enhancedCallback = null;
        if (progressCallback != null) {
            enhancedCallback = (percentage, currentBytes, totalBytes) -> {
                // 调用原始的进度回调接口
                progressCallback.onProgress(percentage);
            };
        }
        
        DownloadResult result = downloadVideo(videoUrl, outputPath, config, enhancedCallback);
        
        return result.isSuccess() ? result.getFilePath() : null;
    }
    
    /**
     * 进度回调接口
     */
    public interface ProgressCallback {
        void onProgress(int percentage, long currentBytes, long totalBytes);
    }
    
    /**
     * 下载结果
     */
    public static class DownloadResult {
        private final boolean success;
        private final String message;
        private final String filePath;
        private final long fileSize;
        
        private DownloadResult(boolean success, String message, String filePath, long fileSize) {
            this.success = success;
            this.message = message;
            this.filePath = filePath;
            this.fileSize = fileSize;
        }
        
        public static DownloadResult success(String filePath, long fileSize) {
            return new DownloadResult(true, "Download completed", filePath, fileSize);
        }
        
        public static DownloadResult failure(String message) {
            return new DownloadResult(false, message, null, 0);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getFilePath() { return filePath; }
        public long getFileSize() { return fileSize; }
    }
    
    /**
     * 进程执行结果
     */
    private static class ProcessResult {
        private final boolean success;
        private final int exitCode;
        private final String output;
        
        public ProcessResult(boolean success, int exitCode, String output) {
            this.success = success;
            this.exitCode = exitCode;
            this.output = output;
        }
        
        public boolean isSuccess() { return success; }
        public int getExitCode() { return exitCode; }
        public String getOutput() { return output; }
    }
    
    /**
     * 进度跟踪器
     */
    private static class ProgressTracker {
        private final ProgressCallback callback;
        private final long updateInterval;
        private long lastUpdateTime = 0;
        private int lastProgress = 0;
        private long totalDuration = 0; // 总时长（微秒）
        
        public ProgressTracker(ProgressCallback callback, Long updateInterval) {
            this.callback = callback;
            this.updateInterval = updateInterval != null ? updateInterval : 1000;
        }
        
        public void parseLine(String line) {
            if (callback == null) {
                return;
            }
            
            try {
                // 解析总时长
                if (line.contains("Duration:") && totalDuration == 0) {
                    // 格式: Duration: 00:01:23.45, start: 0.000000, bitrate: 1234 kb/s
                    String[] parts = line.split("Duration: ");
                    if (parts.length > 1) {
                        String durationStr = parts[1].split(",")[0].trim();
                        totalDuration = parseDurationToMicroseconds(durationStr);
                        log.debug("解析到总时长: {} 微秒 ({})", totalDuration, durationStr);
                    }
                }
                
                // 解析当前进度
                if (line.startsWith("out_time_us=") && totalDuration > 0) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdateTime < updateInterval) {
                        return; // 限制更新频率
                    }
                    
                    long currentDuration = Long.parseLong(line.substring(12));
                    int progress = (int) ((currentDuration * 100) / totalDuration);
                    
                    // 确保进度在0-100之间，且不会倒退
                    progress = Math.max(0, Math.min(100, progress));
                    
                    if (progress > lastProgress) {
                        lastProgress = progress;
                        lastUpdateTime = currentTime;
                        
                        // 调用进度回调
                        callback.onProgress(progress, currentDuration, totalDuration);
                        log.debug("FFmpeg进度更新: {}% ({}/{})", progress, currentDuration, totalDuration);
                    }
                }
                
                // 解析其他进度格式（备用）
                if (line.contains("time=") && totalDuration > 0) {
                    // 格式: frame= 1234 fps= 25 q=28.0 size= 1234kB time=00:01:23.45 bitrate= 123.4kbits/s speed=1.23x
                    String[] parts = line.split("time=");
                    if (parts.length > 1) {
                        String timeStr = parts[1].split(" ")[0].trim();
                        long currentDuration = parseDurationToMicroseconds(timeStr);
                        
                        if (currentDuration > 0) {
                            int progress = (int) ((currentDuration * 100) / totalDuration);
                            progress = Math.max(0, Math.min(100, progress));
                            
                            if (progress > lastProgress) {
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastUpdateTime >= updateInterval) {
                                    lastProgress = progress;
                                    lastUpdateTime = currentTime;
                                    
                                    callback.onProgress(progress, currentDuration, totalDuration);
                                    log.debug("FFmpeg进度更新(time格式): {}% ({})", progress, timeStr);
                                }
                            }
                        }
                    }
                }
                
            } catch (Exception e) {
                log.debug("Failed to parse progress line: {}", line, e);
            }
        }
        
        /**
         * 解析时长字符串为微秒
         * @param durationStr 时长字符串，格式如 "00:01:23.45"
         * @return 微秒数
         */
        private long parseDurationToMicroseconds(String durationStr) {
            try {
                String[] parts = durationStr.split(":");
                if (parts.length >= 3) {
                    int hours = Integer.parseInt(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    double seconds = Double.parseDouble(parts[2]);
                    
                    return (long) ((hours * 3600 + minutes * 60 + seconds) * 1_000_000);
                }
            } catch (Exception e) {
                log.debug("Failed to parse duration: {}", durationStr, e);
            }
            return 0;
        }
    }
}