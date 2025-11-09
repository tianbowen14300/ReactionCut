package com.tbw.cut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tbw.cut.entity.MergedVideo;
import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.entity.TaskOutputSegment;
import com.tbw.cut.entity.VideoClip;
import com.tbw.cut.mapper.MergedVideoMapper;
import com.tbw.cut.mapper.SubmissionTaskMapper;
import com.tbw.cut.mapper.TaskSourceVideoMapper;
import com.tbw.cut.mapper.TaskOutputSegmentMapper;
import com.tbw.cut.mapper.VideoClipMapper;
import com.tbw.cut.service.VideoProcessService;
import com.tbw.cut.utils.FFmpegUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VideoProcessServiceImpl implements VideoProcessService {
    
    @Autowired
    private SubmissionTaskMapper submissionTaskMapper;
    
    @Autowired
    private TaskSourceVideoMapper taskSourceVideoMapper;
    
    @Autowired
    private TaskOutputSegmentMapper taskOutputSegmentMapper;
    
    @Value("${ffmpeg.path:/opt/homebrew/bin/ffmpeg}")
    private String ffmpegPath;
    @Autowired
    private VideoClipMapper videoClipMapper;

    @Autowired
    private MergedVideoMapper mergedVideoMapper;

    @Override
    public List<String> clipVideos(String taskId) {
        try {
            // 获取任务源视频
            List<TaskSourceVideo> sourceVideos = getSourceVideos(taskId);
            if (sourceVideos.isEmpty()) {
                log.warn("任务没有源视频，任务ID: {}", taskId);
                return Collections.emptyList();
            }
            
            // 创建剪辑目录
            String workDir = createWorkDirectory(taskId);
            String clipsDir = workDir + File.separator + "clips";
            new File(clipsDir).mkdirs();
            
            List<String> clipPaths = new ArrayList<>();
            
            // 剪辑每个源视频
            for (int i = 0; i < sourceVideos.size(); i++) {
                TaskSourceVideo sourceVideo = sourceVideos.get(i);
                String clipPath = clipVideo(sourceVideo, clipsDir, i + 1);
                if (clipPath != null && !clipPath.isEmpty()) {
                    clipPaths.add(clipPath);
                }
                VideoClip videoClip = new VideoClip();
                videoClip.setClipPath(clipPath);
                videoClip.setTaskId(taskId); // taskId已经是String类型，无需转换
                videoClip.setSequence(sourceVideo.getSortOrder());
                videoClip.setStartTime(sourceVideo.getStartTime());
                videoClip.setEndTime(sourceVideo.getEndTime());
                videoClip.setCreateTime(LocalDateTime.now());
                videoClip.setUpdateTime(LocalDateTime.now());
                videoClip.setFileName(new File(clipPath).getName());
                videoClip.setStatus(1);
                videoClipMapper.insert(videoClip);
            }
            log.info("视频剪辑完成，任务ID: {}, 剪辑文件数量: {}", taskId, clipPaths.size());
            return clipPaths;
        } catch (Exception e) {
            log.error("视频剪辑时发生异常，任务ID: {}", taskId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public String mergeVideos(String taskId) {
        try {
            // 从数据库获取剪辑文件路径并按sequence排序
            List<String> clipPaths = getClipPathsFromDatabase(taskId);
            if (clipPaths == null || clipPaths.isEmpty()) {
                log.warn("没有找到剪辑文件，任务ID: {}", taskId);
                return null;
            }
            
            String mergedPath = performMergeVideos(taskId, clipPaths);
            
            // 保存合并视频信息
            if (mergedPath != null && !mergedPath.isEmpty()) {
                saveMergedVideo(taskId, mergedPath);
            }
            
            return mergedPath;
        } catch (Exception e) {
            log.error("视频合并时发生异常，任务ID: {}", taskId, e);
            return null;
        }
    }
    
    /**
     * 执行视频合并操作
     */
    private String performMergeVideos(String taskId, List<String> clipPaths) {
        try {
            if (clipPaths == null || clipPaths.isEmpty()) {
                log.warn("没有视频需要合并，任务ID: {}", taskId);
                return null;
            }
            
            // 创建合并目录
            String workDir = getWorkDirectory(taskId);
            String outputPath = workDir + File.separator + "merged_video.mp4";
            
            // 如果只有一个剪辑文件，直接复制
            if (clipPaths.size() == 1) {
                Files.copy(Paths.get(clipPaths.get(0)), Paths.get(outputPath));
                log.info("只有一个剪辑文件，直接复制完成，任务ID: {}", taskId);
                return outputPath;
            }
            
            // 创建临时的concat文件
            String concatFilePath = workDir + File.separator + "file_list.txt";
            try (FileWriter writer = new FileWriter(concatFilePath)) {
                for (String clipPath : clipPaths) {
                    writer.write("file '" + clipPath + "'\n");
                }
            }
            
            // 构建FFmpeg命令：合并视频
            // 使用重新编码而不是流复制，以确保生成的视频文件符合B站要求
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-f");
            command.add("concat");
            command.add("-safe");
            command.add("0");
            command.add("-i");
            command.add(concatFilePath);
            command.add("-c:v");
            command.add("libx264"); // 使用H.264编码
            command.add("-c:a");
            command.add("aac"); // 使用AAC音频编码
            command.add("-strict");
            command.add("experimental");
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
                log.error("FFmpeg合并超时，任务ID: {}", taskId);
                return null;
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg合并失败，退出码: {}, 任务ID: {}", exitCode, taskId);
                return null;
            }
            
            // 删除临时的concat文件
            new File(concatFilePath).delete();
            
            log.info("视频合并完成，任务ID: {}, 输出路径: {}", taskId, outputPath);
            return outputPath;
        } catch (Exception e) {
            log.error("视频合并时发生异常，任务ID: {}", taskId, e);
            return null;
        }
    }
    
    @Override
    public List<String> segmentVideo(String taskId) {
        try {
            // 从数据库获取合并后的视频路径
            String mergedVideoPath = getMergedVideoPathFromDatabase(taskId);
            if (mergedVideoPath == null || mergedVideoPath.isEmpty()) {
                log.warn("没有找到合并后的视频，任务ID: {}", taskId);
                return Collections.emptyList();
            }
            
            return performSegmentVideo(taskId, mergedVideoPath);
        } catch (Exception e) {
            log.error("视频分段时发生异常，任务ID: {}", taskId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 执行视频分段操作
     */
    private List<String> performSegmentVideo(String taskId, String mergedVideoPath) {
        try {
            if (mergedVideoPath == null || !new File(mergedVideoPath).exists()) {
                log.warn("合并视频文件不存在，任务ID: {}", taskId);
                return Collections.emptyList();
            }
            
            // 获取任务信息，包括分段前缀
            SubmissionTask task = getTaskDetail(taskId);
            String segmentPrefix = task.getSegmentPrefix();
            
            // 创建输出目录
            String workDir = getWorkDirectory(taskId);
            String outputDir = workDir + File.separator + "output";
            new File(outputDir).mkdirs();
            
            // 获取视频时长
            double videoDuration = getVideoDuration(mergedVideoPath);
            if (videoDuration <= 0) {
                log.error("无法获取视频时长，任务ID: {}", taskId);
                return Collections.emptyList();
            }
            
            log.info("视频时长: {} 秒，任务ID: {}", videoDuration, taskId);
            
            // 计算分段数 (每段133秒，即2分13秒)
            int segmentDuration = 133;
            int segmentCount = (int) Math.ceil(videoDuration / segmentDuration);
            
            log.info("分段数: {}, 每段时长: {}秒，任务ID: {}", segmentCount, segmentDuration, taskId);
            
            List<String> segmentPaths = new ArrayList<>();
            
            // 循环切割每一段
            for (int i = 0; i < segmentCount; i++) {
                int currentStart = i * segmentDuration;
                
                // 检查是否超过视频总时长
                if (currentStart >= videoDuration) {
                    log.info("已达到视频末尾，停止切割，任务ID: {}", taskId);
                    break;
                }
                
                // 格式化开始时间 (HH:MM:SS)
                String startTime = formatTime(currentStart);
                
                // 格式化文件编号 (001, 002, ...)
                String fileNum = String.format("%03d", i + 1);
                
                // 构建分段文件名，使用分段前缀（如果有的话）
                String fileName;
                if (segmentPrefix != null && !segmentPrefix.isEmpty()) {
                    fileName = segmentPrefix + "_part_" + fileNum + ".mp4";
                } else {
                    fileName = "part_" + fileNum + ".mp4";
                }
                
                String segmentPath = outputDir + File.separator + fileName;
                
                log.info("[{}/{}] 时间: {} → {}, 任务ID: {}", i + 1, segmentCount, startTime, segmentPath, taskId);
                
                // 构建FFmpeg命令：切割视频
                // 使用重新编码而不是流复制，以确保生成的视频文件符合B站要求
                List<String> command = new ArrayList<>();
                command.add(ffmpegPath);
                command.add("-ss");
                command.add(startTime);
                command.add("-i");
                command.add(mergedVideoPath);
                command.add("-t");
                command.add("00:02:13"); // 2分13秒
                command.add("-c:v");
                command.add("libx264"); // 使用H.264编码
                command.add("-c:a");
                command.add("aac"); // 使用AAC音频编码
                command.add("-strict");
                command.add("experimental");
                command.add("-y");
                command.add(segmentPath);
                
                log.debug("执行FFmpeg切割命令: {}", String.join(" ", command));
                
                ProcessBuilder builder = new ProcessBuilder(command);
                Process process = builder.start();
                
                // 读取输出
                readProcessOutput(process);
                
                boolean finished = process.waitFor(10, TimeUnit.MINUTES);
                if (!finished) {
                    process.destroyForcibly();
                    log.error("FFmpeg切割超时，任务ID: {}, 分段: {}", taskId, i + 1);
                    continue;
                }
                
                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    // 检查实际生成的文件
                    File generatedFile = new File(segmentPath);
                    if (generatedFile.exists()) {
                        segmentPaths.add(segmentPath);
                        log.info("✓ 成功创建分段: {}, 任务ID: {}", segmentPath, taskId);
                    } else {
                        // 如果文件不存在，尝试查找实际生成的文件
                        // FFmpeg可能会根据文件名模式生成不同的文件名
                        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                        String extension = fileName.substring(fileName.lastIndexOf('.'));
                        File outputDirFile = new File(outputDir);
                        File[] files = outputDirFile.listFiles((dir, name) -> 
                            name.startsWith(baseName) && name.endsWith(extension));
                        
                        if (files != null && files.length > 0) {
                            // 选择最新的文件（按修改时间排序）
                            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                            String actualPath = files[0].getAbsolutePath();
                            segmentPaths.add(actualPath);
                            log.info("✓ 成功创建分段: {}, 任务ID: {}", actualPath, taskId);
                        } else {
                            log.warn("未能找到生成的分段文件，预期路径: {}, 任务ID: {}", segmentPath, taskId);
                        }
                    }
                } else {
                    log.error("✗ 创建分段失败，任务ID: {}, 分段: {}, 退出码: {}", taskId, i + 1, exitCode);
                }
            }
            
            log.info("视频分段完成，任务ID: {}, 分段文件数量: {}", taskId, segmentPaths.size());
            return segmentPaths;
        } catch (Exception e) {
            log.error("视频分段时发生异常，任务ID: {}", taskId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public SubmissionTask getTaskDetail(String taskId) {
        return submissionTaskMapper.selectById(taskId);
    }
    
    @Override
    public List<TaskSourceVideo> getSourceVideos(String taskId) {
        return taskSourceVideoMapper.findByTaskIdOrderBySortOrder(taskId);
    }
    
    @Override
    public void saveOutputSegments(String taskId, List<String> segmentPaths) {
        try {
            List<TaskOutputSegment> segments = new ArrayList<>();
            
            for (int i = 0; i < segmentPaths.size(); i++) {
                TaskOutputSegment segment = new TaskOutputSegment();
                segment.setSegmentId(UUID.randomUUID().toString());
                segment.setTaskId(taskId);
                segment.setPartName("P" + (i + 1));
                segment.setSegmentFilePath(segmentPaths.get(i));
                segment.setPartOrder(i + 1);
                segment.setUploadStatus(TaskOutputSegment.UploadStatus.PENDING);
                segment.setCid(null);
                segments.add(segment);
            }
            
            for (TaskOutputSegment segment : segments) {
                taskOutputSegmentMapper.insert(segment);
            }
            
            log.info("保存输出分段成功，任务ID: {}, 分段数量: {}", taskId, segments.size());
        } catch (Exception e) {
            log.error("保存输出分段时发生异常，任务ID: {}", taskId, e);
        }
    }
    
    @Override
    public void saveMergedVideo(String taskId, String mergedVideoPath) {
        try {
            MergedVideo mergedVideo = new MergedVideo();
            mergedVideo.setTaskId(taskId);
            mergedVideo.setFileName(new File(mergedVideoPath).getName());
            mergedVideo.setVideoPath(mergedVideoPath);
            mergedVideo.setStatus(2); // 处理完成
            mergedVideo.setCreateTime(LocalDateTime.now());
            mergedVideo.setUpdateTime(LocalDateTime.now());
            
            // 获取视频时长（简化处理，实际应该通过FFmpeg获取）
            mergedVideo.setDuration(0);
            
            mergedVideoMapper.insert(mergedVideo);
            log.info("保存合并视频信息成功，任务ID: {}, 视频路径: {}", taskId, mergedVideoPath);
        } catch (Exception e) {
            log.error("保存合并视频信息时发生异常，任务ID: {}", taskId, e);
        }
    }
    
    @Override
    public List<MergedVideo> getMergedVideos(String taskId) {
        try {
            return mergedVideoMapper.findByTaskIdOrderByCreateTime(taskId);
        } catch (Exception e) {
            log.error("获取合并视频信息时发生异常，任务ID: {}", taskId, e);
            return Collections.emptyList();
        }
    }
    

    
    /**
     * 剪辑单个视频
     */
    private String clipVideo(TaskSourceVideo sourceVideo, String clipsDir, int index) {
        try {
            String inputPath = sourceVideo.getSourceFilePath();
            String startTime = sourceVideo.getStartTime();
            String endTime = sourceVideo.getEndTime();
            
            // 构建输出路径
            String fileName = new File(inputPath).getName();
            String outputFileName = "clip_" + index + "_" + fileName;
            String outputPath = clipsDir + File.separator + outputFileName;
            
            // 构建FFmpeg命令：剪辑视频
            // 使用重新编码而不是流复制，以确保生成的视频文件符合B站要求
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(inputPath);
            
            if (startTime != null && !startTime.isEmpty() && !startTime.equals("00:00:00")) {
                command.add("-ss");
                command.add(startTime);
            }
            
            if (endTime != null && !endTime.isEmpty() && !endTime.equals("00:00:00")) {
                command.add("-to");
                command.add(endTime);
            }
            
            command.add("-c:v");
            command.add("libx264"); // 使用H.264编码
            command.add("-c:a");
            command.add("aac"); // 使用AAC音频编码
            command.add("-strict");
            command.add("experimental");
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
                log.error("FFmpeg剪辑超时，视频索引: {}", index);
                return null;
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg剪辑失败，退出码: {}, 视频索引: {}", exitCode, index);
                return null;
            }
            
            log.info("视频剪辑完成，视频索引: {}, 输出路径: {}", index, outputPath);
            return outputPath;
        } catch (Exception e) {
            log.error("视频剪辑时发生异常，视频索引: {}", index, e);
            return null;
        }
    }
    
    /**
     * 获取视频时长
     */
    private double getVideoDuration(String videoPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath.replace("ffmpeg", "ffprobe"));
            command.add("-v");
            command.add("error");
            command.add("-show_entries");
            command.add("format=duration");
            command.add("-of");
            command.add("default=noprint_wrappers=1:nokey=1");
            command.add(videoPath);
            
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = builder.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            boolean finished = process.waitFor(1, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return -1;
            }
            
            if (process.exitValue() == 0) {
                return Double.parseDouble(output.toString().trim());
            }
            
            return -1;
        } catch (Exception e) {
            log.error("获取视频时长时发生异常，视频路径: {}", videoPath, e);
            return -1;
        }
    }
    
    /**
     * 格式化时间 (秒转为HH:MM:SS)
     */
    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    /**
     * 创建任务工作目录
     */
    private String createWorkDirectory(String taskId) {
        try {
            // 获取任务详情以获取源视频路径
            SubmissionTask task = getTaskDetail(taskId);
            if (task == null) {
                log.error("任务不存在，任务ID: {}", taskId);
                return null;
            }
            
            // 获取第一个源视频的目录作为工作目录基础
            List<TaskSourceVideo> sourceVideos = getSourceVideos(taskId);
            if (sourceVideos.isEmpty()) {
                log.error("任务没有源视频，任务ID: {}", taskId);
                return null;
            }
            
            String firstVideoPath = sourceVideos.get(0).getSourceFilePath();
            File firstVideoFile = new File(firstVideoPath);
            String baseDir = firstVideoFile.getParent();
            
            // 在视频所在目录下创建任务目录
            String taskDir = baseDir + File.separator + "video_task_" + taskId;
            
            File dir = new File(taskDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            log.info("创建任务工作目录: {}", taskDir);
            return taskDir;
        } catch (Exception e) {
            log.error("创建任务工作目录时发生异常，任务ID: {}", taskId, e);
            return null;
        }
    }
    
    /**
     * 获取任务工作目录
     */
    private String getWorkDirectory(String taskId) {
        try {
            // 获取任务详情以获取源视频路径
            SubmissionTask task = getTaskDetail(taskId);
            if (task == null) {
                log.error("任务不存在，任务ID: {}", taskId);
                return null;
            }
            
            // 获取第一个源视频的目录作为工作目录基础
            List<TaskSourceVideo> sourceVideos = getSourceVideos(taskId);
            if (sourceVideos.isEmpty()) {
                log.error("任务没有源视频，任务ID: {}", taskId);
                return null;
            }
            
            String firstVideoPath = sourceVideos.get(0).getSourceFilePath();
            File firstVideoFile = new File(firstVideoPath);
            String baseDir = firstVideoFile.getParent();
            
            // 查找已存在的任务目录
            String taskDirPattern = "video_task_" + taskId;
            File baseDirFile = new File(baseDir);
            
            if (baseDirFile.exists() && baseDirFile.isDirectory()) {
                File[] taskDirs = baseDirFile.listFiles((dir, name) -> name.contains(taskDirPattern));
                if (taskDirs != null && taskDirs.length > 0) {
                    return taskDirs[0].getAbsolutePath();
                }
            }
            
            // 如果没有找到已存在的任务目录，创建新的
            return createWorkDirectory(taskId);
        } catch (Exception e) {
            log.error("获取任务工作目录时发生异常，任务ID: {}", taskId, e);
            return createWorkDirectory(taskId);
        }
    }
    
    /**
     * 从数据库获取剪辑文件路径并按sequence排序
     */
    private List<String> getClipPathsFromDatabase(String taskId) {
        try {
            List<String> clipPaths = new ArrayList<>();
            
            // 查询video_clip表获取剪辑文件路径并按sequence排序
            List<VideoClip> videoClips = videoClipMapper.findByTaskIdOrderBySequence(taskId);
            for (VideoClip clip : videoClips) {
                if (clip.getClipPath() != null && !clip.getClipPath().isEmpty()) {
                    clipPaths.add(clip.getClipPath());
                }
            }
            
            log.info("从数据库获取剪辑文件路径，任务ID: {}, 文件数量: {}", taskId, clipPaths.size());
            return clipPaths;
        } catch (Exception e) {
            log.error("从数据库获取剪辑文件路径时发生异常，任务ID: {}", taskId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 从数据库获取合并后的视频路径
     */
    public String getMergedVideoPathFromDatabase(String taskId) {
        try {
            List<MergedVideo> mergedVideos = getMergedVideos(taskId);
            if (mergedVideos != null && !mergedVideos.isEmpty()) {
                // 返回最新的合并视频路径
                return mergedVideos.get(0).getVideoPath();
            }
            return null;
        } catch (Exception e) {
            log.error("从数据库获取合并视频路径时发生异常，任务ID: {}", taskId, e);
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
}