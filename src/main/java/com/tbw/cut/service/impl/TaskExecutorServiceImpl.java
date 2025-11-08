package com.tbw.cut.service.impl;

import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.entity.TaskOutputSegment;
import com.tbw.cut.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class TaskExecutorServiceImpl implements TaskExecutorService {
    
    private final SubmissionTaskService submissionTaskService;
    private final BilibiliSubmissionService bilibiliSubmissionService;
    
    // 用于生成唯一的任务目录
    private final AtomicInteger counter = new AtomicInteger(0);
    private final VideoProcessService videoProcessService;

    public TaskExecutorServiceImpl(SubmissionTaskService submissionTaskService,
                                   BilibiliSubmissionService bilibiliSubmissionService, VideoProcessService videoProcessService) {
        this.submissionTaskService = submissionTaskService;
        this.bilibiliSubmissionService = bilibiliSubmissionService;
        this.videoProcessService = videoProcessService;
    }
    
    @Override
    public void executePendingTasks() {
        try {
            // 查找所有PENDING状态的任务
            List<SubmissionTask> pendingTasks = submissionTaskService.findTasksByStatus(SubmissionTask.TaskStatus.PENDING);
            
            for (SubmissionTask task : pendingTasks) {
                // 异步执行任务
                executeTaskAsync(task.getTaskId());
            }
        } catch (Exception e) {
            log.error("执行待处理任务时发生异常", e);
        }
    }
    
    @Async
    public void executeTaskAsync(String taskId) {
        try {
            log.info("开始执行任务，任务ID: {}", taskId);
            
            // 1. 更新任务状态为PROCESSING
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.CLIPPING);

            // 3. 创建任务工作目录
            String workDir = createWorkDirectory(taskId);
            if (workDir == null) {
                log.error("创建任务工作目录失败，任务ID: {}", taskId);
                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
                return;
            }
            
            // 4. 剪辑和合并视频
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.CLIPPING);
            List<String> clipVideoPath = videoProcessService.clipVideos(taskId);
            if (clipVideoPath == null) {
                log.error("剪辑视频失败，任务ID: {}", taskId);
                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
                return;
            }
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.MERGING);
            String mergedVideoPath = videoProcessService.mergeVideos(taskId);
            if (mergedVideoPath == null) {
                log.error("合并视频失败，任务ID: {}", taskId);
                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
                return;
            }
            
            // 5. 分段切割视频
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.SEGMENTING);
            List<String> segmentPaths = videoProcessService.segmentVideo(taskId);
            if (segmentPaths == null || segmentPaths.isEmpty()) {
                log.error("分段切割视频失败，任务ID: {}", taskId);
                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
                return;
            }
            
            // 6. 保存分段信息到数据库
            List<TaskOutputSegment> segments = new ArrayList<>();
            for (int i = 0; i < segmentPaths.size(); i++) {
                TaskOutputSegment segment = new TaskOutputSegment();
                segment.setTaskId(taskId);
                segment.setPartName("P" + (i + 1));
                segment.setSegmentFilePath(segmentPaths.get(i));
                segment.setPartOrder(i + 1);
                segment.setUploadStatus(TaskOutputSegment.UploadStatus.PENDING);
                segments.add(segment);
            }
            submissionTaskService.saveOutputSegments(segments);
//            // 7. 上传分段文件到B站
//            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.UPLOADING);
//            boolean uploadSuccess = bilibiliSubmissionService.uploadSegments(taskId, segments);
//            if (!uploadSuccess) {
//                log.error("上传分段文件到B站失败，任务ID: {}", taskId);
//                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
//                return;
//            }
//
//            // 8. 提交视频到B站
//            String bvid = bilibiliSubmissionService.submitVideo(task, segments);
//            if (bvid == null) {
//                log.error("提交视频到B站失败，任务ID: {}", taskId);
//                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
//                return;
//            }
//
//            // 9. 更新任务状态为完成
//            submissionTaskService.updateTaskStatusAndBvid(taskId, SubmissionTask.TaskStatus.COMPLETED, bvid);
            
//            log.info("任务执行完成，任务ID: {}, BVID: {}", taskId, bvid);
        } catch (Exception e) {
            log.error("执行任务时发生异常，任务ID: {}", taskId, e);
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
        }
    }
    
    /**
     * 创建任务工作目录
     */
    private String createWorkDirectory(String taskId) {
        try {
            String baseDir = System.getProperty("user.home") + File.separator + "video_tasks";
            String taskDir = baseDir + File.separator + taskId.toString() + "_" + counter.incrementAndGet();
            
            File dir = new File(taskDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            return taskDir;
        } catch (Exception e) {
            log.error("创建任务工作目录时发生异常，任务ID: {}", taskId, e);
            return null;
        }
    }
}