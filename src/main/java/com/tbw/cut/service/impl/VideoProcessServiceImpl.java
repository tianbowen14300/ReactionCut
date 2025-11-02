package com.tbw.cut.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tbw.cut.entity.VideoProcessTask;
import com.tbw.cut.entity.VideoClip;
import com.tbw.cut.mapper.VideoProcessTaskMapper;
import com.tbw.cut.mapper.VideoClipMapper;
import com.tbw.cut.service.VideoProcessService;
import com.tbw.cut.dto.VideoProcessTaskDTO;
import com.tbw.cut.dto.VideoClipDTO;
import com.tbw.cut.utils.FFmpegUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VideoProcessServiceImpl extends ServiceImpl<VideoProcessTaskMapper, VideoProcessTask> implements VideoProcessService {
    
    @Autowired
    private VideoClipMapper videoClipMapper;
    
    @Autowired
    private FFmpegUtil ffmpegUtil;
    
    @Override
    public Long createProcessTask(VideoProcessTaskDTO dto) {
        try {
            // Create processing task
            VideoProcessTask task = new VideoProcessTask();
            task.setTaskName(dto.getTaskName());
            task.setStatus(0); // Pending
            task.setProgress(0);
            task.setCreateTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            
            this.save(task);
            
            // Save video clip information
            List<VideoClipDTO> clipDTOs = dto.getClips();
            for (int i = 0; i < clipDTOs.size(); i++) {
                VideoClipDTO clipDTO = clipDTOs.get(i);
                VideoClip clip = new VideoClip();
                clip.setTaskId(task.getId());
                clip.setFileName(clipDTO.getFileName());
                clip.setStartTime(clipDTO.getStartTime());
                clip.setEndTime(clipDTO.getEndTime());
                clip.setSequence(clipDTO.getSequence());
                clip.setStatus(0); // Pending
                clip.setCreateTime(LocalDateTime.now());
                clip.setUpdateTime(LocalDateTime.now());
                videoClipMapper.insert(clip);
            }
            
            // Execute processing task asynchronously
            Long taskId = task.getId();
            asyncExecuteProcessTask(taskId);
            
            return taskId;
        } catch (Exception e) {
            log.error("Failed to create video processing task", e);
            return null;
        }
    }
    
    @Override
    public void executeProcessTask(Long taskId) {
        try {
            VideoProcessTask task = this.getById(taskId);
            if (task == null) {
                log.error("Task not found, ID: {}", taskId);
                return;
            }
            
            // Update task status to processing
            task.setStatus(1);
            task.setUpdateTime(LocalDateTime.now());
            this.updateById(task);
            
            // Get all clips
            List<VideoClip> clips = videoClipMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VideoClip>()
                    .eq(VideoClip::getTaskId, taskId)
                    .orderByAsc(VideoClip::getSequence)
            );
            
            // Process each clip
            List<String> clipPaths = new ArrayList<>();
            for (int i = 0; i < clips.size(); i++) {
                VideoClip clip = clips.get(i);
                // Update progress
                int progress = (i * 100) / (clips.size() * 2);
                updateProgress(taskId, progress);
                
                // Extract clip using FFmpeg
                String clipPath = extractClip(clip);
                if (clipPath != null) {
                    clipPaths.add(clipPath);
                } else {
                    failProcessTask(taskId, "Failed to extract clip: " + clip.getFileName());
                    return;
                }
            }
            
            // Merge all clips
            updateProgress(taskId, 50);
            String mergedPath = mergeClips(taskId, clipPaths);
            if (mergedPath == null) {
                failProcessTask(taskId, "Failed to merge clips");
                return;
            }
            
            // Update progress to 90%
            updateProgress(taskId, 90);
            
            // Upload to Bilibili
            uploadToBilibili(taskId, mergedPath);
            
            // Complete task
            completeProcessTask(taskId, mergedPath);
        } catch (Exception e) {
            failProcessTask(taskId, e.getMessage());
        }
    }
    
    @Override
    public void updateProgress(Long taskId, Integer progress) {
        VideoProcessTask task = this.getById(taskId);
        if (task != null) {
            task.setProgress(progress);
            task.setUpdateTime(LocalDateTime.now());
            this.updateById(task);
        }
    }
    
    @Override
    public void completeProcessTask(Long taskId, String outputPath) {
        VideoProcessTask task = this.getById(taskId);
        if (task != null) {
            task.setOutputPath(outputPath);
            task.setStatus(2); // Completed
            task.setProgress(100);
            task.setUpdateTime(LocalDateTime.now());
            this.updateById(task);
        }
    }
    
    @Override
    public void failProcessTask(Long taskId, String errorMessage) {
        VideoProcessTask task = this.getById(taskId);
        if (task != null) {
            task.setStatus(3); // Failed
            task.setUpdateTime(LocalDateTime.now());
            this.updateById(task);
            log.error("Video processing failed, Task ID: {}, Error: {}", taskId, errorMessage);
        }
    }
    
    /**
     * Execute processing task asynchronously
     * @param taskId Task ID
     */
    private void asyncExecuteProcessTask(Long taskId) {
        // Use new thread to simulate async processing
        new Thread(() -> {
            executeProcessTask(taskId);
        }).start();
    }
    
    /**
     * Extract video clip using FFmpeg
     * @param clip Clip information
     * @return Clip path if successful, null otherwise
     */
    private String extractClip(VideoClip clip) {
        try {
            String outputFileName = "clip_" + clip.getId() + "_" + clip.getSequence() + ".mp4";
            String clipPath = ffmpegUtil.extractClip(
                clip.getFileName(), 
                clip.getStartTime(), 
                clip.getEndTime(), 
                outputFileName
            );
            
            if (clipPath != null) {
                // Update clip status
                clip.setStatus(2); // Completed
                clip.setClipPath(clipPath);
                clip.setUpdateTime(LocalDateTime.now());
                videoClipMapper.updateById(clip);
                return clipPath;
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to extract clip", e);
            clip.setStatus(3); // Failed
            clip.setUpdateTime(LocalDateTime.now());
            videoClipMapper.updateById(clip);
            return null;
        }
    }
    
    /**
     * Merge video clips using FFmpeg
     * @param taskId Task ID
     * @param clipPaths Clip paths
     * @return Merged file path if successful, null otherwise
     */
    private String mergeClips(Long taskId, List<String> clipPaths) {
        try {
            String outputFileName = "merged_task_" + taskId + ".mp4";
            return ffmpegUtil.mergeClips(clipPaths, outputFileName);
        } catch (Exception e) {
            log.error("Failed to merge clips", e);
            return null;
        }
    }
    
    /**
     * Upload to Bilibili
     * @param taskId Task ID
     * @param filePath File path to upload
     */
    private void uploadToBilibili(Long taskId, String filePath) {
        // This should call Bilibili's upload API
        log.info("Uploading video to Bilibili, Task ID: {}, File: {}", taskId, filePath);
        // Implementation would go here
    }
}