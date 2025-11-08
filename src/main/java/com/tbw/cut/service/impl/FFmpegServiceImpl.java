package com.tbw.cut.service.impl;

import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.service.FFmpegService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Slf4j
@Service
public class FFmpegServiceImpl implements FFmpegService {
    
    @Value("${ffmpeg.path:/usr/bin/ffmpeg}")
    private String ffmpegPath;
    
    @Override
    public String clipAndMergeVideos(String taskId, List<TaskSourceVideo> sourceVideos, String outputPath) {
        try {
            // 1. 剪辑每个源视频
            List<String> clippedVideoPaths = new ArrayList<>();
            for (int i = 0; i < sourceVideos.size(); i++) {
                TaskSourceVideo sourceVideo = sourceVideos.get(i);
                String clippedPath = clipVideo(sourceVideo, taskId, i);
                if (clippedPath != null) {
                    clippedVideoPaths.add(clippedPath);
                } else {
                    log.error("剪辑视频失败，任务ID: {}, 视频索引: {}", taskId, i);
                    return null;
                }
            }
            
            // 2. 合并剪辑后的视频
            String mergedPath = mergeVideos(clippedVideoPaths, outputPath);
            if (mergedPath != null) {
                log.info("剪辑和合并视频成功，任务ID: {}, 输出路径: {}", taskId, mergedPath);
                return mergedPath;
            } else {
                log.error("合并视频失败，任务ID: {}", taskId);
                return null;
            }
        } catch (Exception e) {
            log.error("剪辑和合并视频时发生异常，任务ID: {}", taskId, e);
            return null;
        }
    }
    
    @Override
    public List<String> segmentVideo(String taskId, String inputPath, int segmentTime, String outputPattern) {
        try {
            // 创建输出目录
            File outputFile = new File(outputPattern);
            File outputDir = outputFile.getParentFile();
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // 构建FFmpeg命令：切割视频
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(inputPath);
            command.add("-c");
            command.add("copy");
            command.add("-map");
            command.add("0");
            command.add("-f");
            command.add("segment");
            command.add("-segment_time");
            command.add(String.valueOf(segmentTime));
            command.add(outputPattern);
            
            log.info("执行FFmpeg分段命令: {}", String.join(" ", command));
            
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = builder.start();
            
            // 读取输出
            readProcessOutput(process);
            
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.error("FFmpeg分段超时，任务ID: {}", taskId);
                return null;
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg分段失败，退出码: {}, 任务ID: {}", exitCode, taskId);
                return null;
            }
            
            // 获取生成的分段文件列表
            List<String> segmentPaths = getSegmentFiles(outputPattern);
            log.info("视频分段完成，任务ID: {}, 分段数量: {}", taskId, segmentPaths.size());
            return segmentPaths;
        } catch (Exception e) {
            log.error("视频分段时发生异常，任务ID: {}", taskId, e);
            return null;
        }
    }
    
    /**
     * 剪辑单个视频
     */
    private String clipVideo(TaskSourceVideo sourceVideo, String taskId, int index) {
        try {
            String inputPath = sourceVideo.getSourceFilePath();
            String startTimeStr = sourceVideo.getStartTime();
            String endTimeStr = sourceVideo.getEndTime();
            
            // 构建输出路径
            File inputFile = new File(inputPath);
            String outputFileName = "clipped_" + taskId + "_" + index + "_" + inputFile.getName();
            String outputPath = inputFile.getParent() + File.separator + outputFileName;
            
            // 构建FFmpeg命令：剪辑视频
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(inputPath);
            
            if (startTimeStr != null && !startTimeStr.isEmpty() && !startTimeStr.equals("00:00:00")) {
                command.add("-ss");
                command.add(startTimeStr);
            }
            
            if (endTimeStr != null && !endTimeStr.isEmpty() && !endTimeStr.equals("00:00:00")) {
                command.add("-to");
                command.add(endTimeStr);
            }
            
            command.add("-c");
            command.add("copy");
            command.add("-y");
            command.add(outputPath);
            
            log.info("执行FFmpeg剪辑命令: {}", String.join(" ", command));
            
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = builder.start();
            
            // 读取输出
            readProcessOutput(process);
            
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.error("FFmpeg剪辑超时，任务ID: {}, 视频索引: {}", taskId, index);
                return null;
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg剪辑失败，退出码: {}, 任务ID: {}, 视频索引: {}", exitCode, taskId, index);
                return null;
            }
            
            log.info("视频剪辑完成，任务ID: {}, 视频索引: {}, 输出路径: {}", taskId, index, outputPath);
            return outputPath;
        } catch (Exception e) {
            log.error("视频剪辑时发生异常，任务ID: {}, 视频索引: {}", taskId, index, e);
            return null;
        }
    }
    
    /**
     * 合并视频
     */
    private String mergeVideos(List<String> videoPaths, String outputPath) {
        try {
            if (videoPaths.isEmpty()) {
                log.warn("没有视频需要合并");
                return null;
            }
            
            if (videoPaths.size() == 1) {
                // 只有一个视频，直接复制
                File sourceFile = new File(videoPaths.get(0));
                File targetFile = new File(outputPath);
                
                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                
                log.info("只有一个视频，直接复制完成");
                return outputPath;
            }
            
            // 创建临时的concat文件
            String concatFilePath = outputPath + ".concat";
            try (FileWriter writer = new FileWriter(concatFilePath)) {
                for (String videoPath : videoPaths) {
                    writer.write("file '" + videoPath + "'\n");
                }
            }
            
            // 构建FFmpeg命令：合并视频
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-f");
            command.add("concat");
            command.add("-safe");
            command.add("0");
            command.add("-i");
            command.add(concatFilePath);
            command.add("-c");
            command.add("copy");
            command.add("-y");
            command.add(outputPath);
            
            log.info("执行FFmpeg合并命令: {}", String.join(" ", command));
            
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = builder.start();
            
            // 读取输出
            readProcessOutput(process);
            
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.error("FFmpeg合并超时");
                return null;
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg合并失败，退出码: {}", exitCode);
                return null;
            }
            
            // 删除临时的concat文件
            new File(concatFilePath).delete();
            
            // 删除剪辑后的临时文件
            for (String videoPath : videoPaths) {
                new File(videoPath).delete();
            }
            
            log.info("视频合并完成，输出路径: {}", outputPath);
            return outputPath;
        } catch (Exception e) {
            log.error("视频合并时发生异常", e);
            return null;
        }
    }
    
    /**
     * 读取并记录进程输出
     */
    private void readProcessOutput(Process process) throws IOException {
        // 读取标准输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg stdout: {}", line);
            }
        }
        
        // 读取错误输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg stderr: {}", line);
            }
        }
    }
    
    /**
     * 获取分段文件列表
     */
    private List<String> getSegmentFiles(String outputPattern) {
        List<String> segmentFiles = new ArrayList<>();
        String basePattern = outputPattern.replace("%d", "");
        File parentDir = new File(basePattern).getParentFile();
        
        if (parentDir != null && parentDir.exists()) {
            File[] files = parentDir.listFiles((dir, name) -> name.startsWith(new File(basePattern).getName().replace("%d", "")));
            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File file : files) {
                    segmentFiles.add(file.getAbsolutePath());
                }
            }
        }
        
        return segmentFiles;
    }
}