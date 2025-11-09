package com.tbw.cut.service.impl;

import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.entity.TaskOutputSegment;
import com.tbw.cut.mapper.TaskOutputSegmentMapper;
import com.tbw.cut.service.*;
import com.tbw.cut.utils.FileUtils;
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
                    taskOutputSegment.setPartName(FileUtils.getBaseName(segmentPaths.get(count)));
                    taskOutputSegment.setPartOrder(count);
                    taskOutputSegment.setUploadStatus(TaskOutputSegment.UploadStatus.PENDING);
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

            log.info("任务执行完成，任务ID: {}", taskId);
        } catch (Exception e) {
            log.error("执行任务时发生异常，任务ID: {}", taskId, e);
            // 检查是否是406错误（上传过快）
            if (e.getMessage().contains("406")) {
                log.warn("遇到406错误（上传过快），线程将暂停30分钟后再继续");
                try {
                    // 暂停30分钟（1800000毫秒）
                    Thread.sleep(1800000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("线程中断", ie);
                }
            }
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
        }
    }

    @Override
    public void videoUpload(String taskId) {
        try {
            // 1. 获取任务详情
            SubmissionTask task = submissionTaskService.getTaskDetail(taskId);
            if (task == null) {
                log.error("任务不存在，任务ID: {}", taskId);
                return;
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
            // 检查是否是406错误（上传过快）
            if (e.getMessage().contains("406")) {
                log.warn("遇到406错误（上传过快），线程将暂停30分钟后再继续");
                try {
                    // 暂停30分钟（1800000毫秒）
                    Thread.sleep(1800000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("线程中断", ie);
                }
            }
            submissionTaskService.updateTaskStatus(taskId, SubmissionTask.TaskStatus.FAILED);
        }
    }

}