package com.tbw.cut.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FFmpegUtil {
    
    @Value("${app.ffmpeg-path:/usr/local/bin/ffmpeg}")
    private String ffmpegPath;
    
    @Value("${app.video-storage-dir:./videos}")
    private String videoStorageDir;
    
    @Value("${app.temp-dir:./temp}")
    private String tempDir;
    
    // Getter methods
    public String getVideoStorageDir() {
        return videoStorageDir;
    }
    
    public String getTempDir() {
        return tempDir;
    }
    
    @PostConstruct
    public void init() {
        // Ensure directories exist
        createDirectory(videoStorageDir);
        createDirectory(tempDir);
        createDirectory(tempDir + "/clips");
        
        // Verify FFmpeg installation
        verifyFFmpegInstallation();
    }
    
    /**
     * Verify FFmpeg installation
     */
    private void verifyFFmpegInstallation() {
        try {
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-version");
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            
            if (line != null && line.contains("ffmpeg")) {
                log.info("FFmpeg installation verified: {}", line);
            } else {
                log.warn("FFmpeg verification failed. Please check FFmpeg installation.");
            }
        } catch (Exception e) {
            log.error("FFmpeg verification failed", e);
        }
    }
    
    /**
     * Create directory if not exists
     */
    private void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Record Bilibili live stream
     * @param streamUrl Live stream URL
     * @param outputFileName Output file name
     * @return Output file path if successful, null otherwise
     */
    public String recordLiveStream(String streamUrl, String outputFileName) {
        try {
            String outputPath = videoStorageDir + "/" + outputFileName;
            
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(streamUrl);
            command.add("-c");
            command.add("copy");
            command.add("-f");
            command.add("flv");
            command.add("-y");
            command.add(outputPath);
            
            log.info("Recording live stream: {}", String.join(" ", command));
            
            Process process = executeCommand(command, 30, TimeUnit.MINUTES);
            
            if (process.exitValue() == 0) {
                log.info("Live stream recording completed: {}", outputPath);
                return outputPath;
            } else {
                log.error("Live stream recording failed with exit code: {}", process.exitValue());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to record live stream", e);
            return null;
        }
    }
    
    /**
     * Download Bilibili video
     * @param videoUrl Video URL
     * @param outputFileName Output file name
     * @return Output file path if successful, null otherwise
     */
    public String downloadVideo(String videoUrl, String outputFileName) {
        return downloadVideoToDirectory(videoUrl, outputFileName, videoStorageDir);
    }
    
    /**
     * Download Bilibili video to specific directory
     * @param videoUrl Video URL
     * @param outputFileName Output file name
     * @param outputDirectory Output directory
     * @return Output file path if successful, null otherwise
     */
    public String downloadVideoToDirectory(String videoUrl, String outputFileName, String outputDirectory) {
        try {
            // 确保输出文件名是唯一的，避免文件已存在错误
            String uniqueOutputFileName = generateUniqueFileName(outputFileName);
            String outputPath = outputDirectory + "/" + uniqueOutputFileName;
            
            // 确保输出目录存在
            File outputDir = new File(outputDirectory);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // 检查文件是否已存在，如果存在则删除
            File outputFile = new File(outputPath);
            if (outputFile.exists()) {
                log.info("输出文件已存在，删除旧文件: {}", outputPath);
                outputFile.delete();
            }
            
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-user_agent");
            command.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            command.add("-referer");
            command.add("https://www.bilibili.com");
            command.add("-i");
            command.add(videoUrl);
            command.add("-c");
            command.add("copy");
            command.add("-y"); // 覆盖输出文件
            command.add(outputPath);
            
            log.info("Downloading video: {}", String.join(" ", command));
            
            Process process = executeCommand(command, 30, TimeUnit.MINUTES);
            
            if (process.exitValue() == 0) {
                log.info("Video download completed: {}", outputPath);
                return outputPath;
            } else {
                log.error("Video download failed with exit code: {}", process.exitValue());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to download video", e);
            return null;
        }
    }
    
    /**
     * 生成唯一的文件名，避免文件冲突
     * @param originalFileName 原始文件名
     * @return 唯一的文件名
     */
    private String generateUniqueFileName(String originalFileName) {
        File file = new File(videoStorageDir, originalFileName);
        if (!file.exists()) {
            return originalFileName;
        }
        
        // 分离文件名和扩展名
        int lastDotIndex = originalFileName.lastIndexOf('.');
        String namePart = originalFileName;
        String extensionPart = "";
        if (lastDotIndex > 0) {
            namePart = originalFileName.substring(0, lastDotIndex);
            extensionPart = originalFileName.substring(lastDotIndex);
        }
        
        // 尝试添加时间戳或序号
        String uniqueFileName = namePart + "_" + System.currentTimeMillis() + extensionPart;
        File uniqueFile = new File(videoStorageDir, uniqueFileName);
        if (!uniqueFile.exists()) {
            return uniqueFileName;
        }
        
        // 如果仍然存在，添加序号
        int counter = 1;
        while (true) {
            uniqueFileName = namePart + "_" + counter + extensionPart;
            uniqueFile = new File(videoStorageDir, uniqueFileName);
            if (!uniqueFile.exists()) {
                return uniqueFileName;
            }
            counter++;
        }
    }
    
    /**
     * Extract video clip
     * @param inputPath Input video file path
     * @param startTime Start time (HH:mm:ss format)
     * @param endTime End time (HH:mm:ss format)
     * @param outputFileName Output file name
     * @return Output file path if successful, null otherwise
     */
    public String extractClip(String inputPath, String startTime, String endTime, String outputFileName) {
        try {
            String outputPath = tempDir + "/clips/" + outputFileName;
            
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(inputPath);
            command.add("-ss");
            command.add(startTime);
            command.add("-to");
            command.add(endTime);
            command.add("-c");
            command.add("copy");
            command.add("-y");
            command.add(outputPath);
            
            log.info("Extracting clip: {}", String.join(" ", command));
            
            Process process = executeCommand(command, 10, TimeUnit.MINUTES);
            
            if (process.exitValue() == 0) {
                log.info("Clip extraction completed: {}", outputPath);
                return outputPath;
            } else {
                log.error("Clip extraction failed with exit code: {}", process.exitValue());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to extract clip", e);
            return null;
        }
    }
    
    /**
     * Merge video clips
     * @param clipPaths List of clip file paths
     * @param outputFileName Output file name
     * @return Output file path if successful, null otherwise
     */
    public String mergeClips(List<String> clipPaths, String outputFileName) {
        try {
            // Create file list for concat
            String fileListPath = tempDir + "/file_list.txt";
            createFileList(clipPaths, fileListPath);
            
            String outputPath = videoStorageDir + "/" + outputFileName;
            
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-f");
            command.add("concat");
            command.add("-safe");
            command.add("0");
            command.add("-i");
            command.add(fileListPath);
            command.add("-c");
            command.add("copy");
            command.add("-y");
            command.add(outputPath);
            
            log.info("Merging clips: {}", String.join(" ", command));
            
            Process process = executeCommand(command, 30, TimeUnit.MINUTES);
            
            // Clean up file list
            new File(fileListPath).delete();
            
            if (process.exitValue() == 0) {
                log.info("Clip merging completed: {}", outputPath);
                return outputPath;
            } else {
                log.error("Clip merging failed with exit code: {}", process.exitValue());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to merge clips", e);
            return null;
        }
    }
    
    /**
     * Get video information
     * @param videoPath Video file path
     * @return Video information as JSON string
     */
    public String getVideoInfo(String videoPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(videoPath);
            command.add("-v");
            command.add("quiet");
            command.add("-print_format");
            command.add("json");
            command.add("-show_format");
            command.add("-show_streams");
            
            log.info("Getting video info: {}", String.join(" ", command));
            
            Process process = executeCommand(command, 5, TimeUnit.MINUTES);
            
            if (process.exitValue() == 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                log.info("Video info retrieved successfully");
                return result.toString();
            } else {
                log.error("Failed to get video info with exit code: {}", process.exitValue());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to get video info", e);
            return null;
        }
    }
    
    /**
     * Create file list for concat
     */
    private void createFileList(List<String> clipPaths, String fileListPath) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(fileListPath));
        for (String clipPath : clipPaths) {
            writer.println("file '" + clipPath + "'");
        }
        writer.close();
    }
    
    /**
     * Execute FFmpeg command
     * @param command Command list
     * @param timeout Timeout value
     * @param timeUnit Timeout unit
     * @return Process object
     * @throws IOException
     * @throws InterruptedException
     */
    private Process executeCommand(List<String> command, long timeout, TimeUnit timeUnit) 
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // Log output in a separate thread
        logProcessOutput(process);
        
        // Wait for completion with timeout
        boolean finished = process.waitFor(timeout, timeUnit);
        
        if (!finished) {
            log.warn("Process timed out, destroying...");
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
        
        return process;
    }
    
    /**
     * Log process output
     */
    private void logProcessOutput(Process process) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg output: {}", line);
                }
            } catch (IOException e) {
                log.error("Error reading process output", e);
            }
        }).start();
    }
}