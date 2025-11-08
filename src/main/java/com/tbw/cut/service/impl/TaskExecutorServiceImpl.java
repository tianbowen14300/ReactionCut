package com.tbw.cut.service.impl;

import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.entity.TaskOutputSegment;
import com.tbw.cut.mapper.TaskOutputSegmentMapper;
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
    private final TaskOutputSegmentMapper taskOutputSegmentMapper;

    public TaskExecutorServiceImpl(SubmissionTaskService submissionTaskService,
                                   BilibiliSubmissionService bilibiliSubmissionService, VideoProcessService videoProcessService, TaskOutputSegmentMapper taskOutputSegmentMapper) {
        this.submissionTaskService = submissionTaskService;
        this.bilibiliSubmissionService = bilibiliSubmissionService;
        this.videoProcessService = videoProcessService;
        this.taskOutputSegmentMapper = taskOutputSegmentMapper;
    }
    
    @Override
    public void executePendingTasks() {
        try {
            // 查找所有PENDING状态的任务
            List<SubmissionTask> pendingTasks = submissionTaskService.findTasksByStatus(SubmissionTask.TaskStatus.PENDING);
            
            // 逐个执行任务
            for (SubmissionTask task : pendingTasks) {
                executeTask(task.getTaskId());
            }
        } catch (Exception e) {
            log.error("执行待处理任务时发生异常", e);
        }
    }
    
    @Async
    @Override
    public void executeTask(String taskId) {
        try {
            log.info("开始执行任务，任务ID: {}", taskId);
            
            // 1. 获取任务详情
            SubmissionTask task = submissionTaskService.getTaskDetail(taskId);
            if (task == null) {
                log.error("任务不存在，任务ID: {}", taskId);
                return;
            }
            
            // 2. 获取源视频列表
            List<TaskSourceVideo> sourceVideos = submissionTaskService.getSourceVideosByTaskId(taskId);
            if (sourceVideos.isEmpty()) {
                log.error("任务没有源视频，任务ID: {}", taskId);
                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
                return;
            }
            
            // 3. 视频剪辑
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.CLIPPING);
            List<String> clipPaths = videoProcessService.clipVideos(taskId);
            if (clipPaths.isEmpty()) {
                log.error("视频剪辑失败，任务ID: {}", taskId);
                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
                return;
            }
            
            // 4. 视频合并
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.MERGING);
            String mergedPath = videoProcessService.mergeVideos(taskId);
            if (mergedPath == null || mergedPath.isEmpty()) {
                log.error("视频合并失败，任务ID: {}", taskId);
                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
                return;
            }
            
            // 5. 视频分段
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.SEGMENTING);
            List<String> segmentPaths = videoProcessService.segmentVideo(taskId);
            if (segmentPaths.isEmpty()) {
                log.error("视频分段失败，任务ID: {}", taskId);
                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
                return;
            } else {
                for (int count = 0; count < segmentPaths.size(); count++) {
                    TaskOutputSegment taskOutputSegment = new TaskOutputSegment();
                    taskOutputSegment.setTaskId(taskId);
                    taskOutputSegment.setSegmentFilePath(segmentPaths.get(count));
                    taskOutputSegment.setPartName("P" + count);
                    taskOutputSegment.setPartOrder(count);
                    taskOutputSegment.setUploadStatus(TaskOutputSegment.UploadStatus.SUCCESS);
                    taskOutputSegmentMapper.insert(taskOutputSegment);
                }
            }
            
            // 6. 获取分段信息
            List<TaskOutputSegment> segments = submissionTaskService.getOutputSegmentsByTaskId(taskId);
            if (segments.isEmpty()) {
                log.error("没有找到分段信息，任务ID: {}", taskId);
                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
                return;
            }
            
            // 7. 上传分段文件到B站
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.UPLOADING);
            boolean uploadSuccess = bilibiliSubmissionService.uploadSegments(taskId, segments);
            if (!uploadSuccess) {
                log.error("上传分段文件到B站失败，任务ID: {}", taskId);
                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
                return;
            }
            
            // 8. 提交视频到B站
            String bvid = bilibiliSubmissionService.submitVideo(task, segments);
            if (bvid == null) {
                log.error("提交视频到B站失败，任务ID: {}", taskId);
                submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
                return;
            }
            
            // 9. 更新任务状态为完成
            submissionTaskService.updateTaskStatusAndBvid(taskId, SubmissionTask.TaskStatus.COMPLETED, bvid);
            
            log.info("任务执行完成，任务ID: {}, BVID: {}", taskId, bvid);
        } catch (Exception e) {
            log.error("执行任务时发生异常，任务ID: {}", taskId, e);
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
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