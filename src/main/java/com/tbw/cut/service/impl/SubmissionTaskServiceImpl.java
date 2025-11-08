package com.tbw.cut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.entity.TaskOutputSegment;
import com.tbw.cut.mapper.SubmissionTaskMapper;
import com.tbw.cut.mapper.TaskSourceVideoMapper;
import com.tbw.cut.mapper.TaskOutputSegmentMapper;
import com.tbw.cut.service.SubmissionTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SubmissionTaskServiceImpl implements SubmissionTaskService {
    
    @Autowired
    private SubmissionTaskMapper submissionTaskMapper;
    
    @Autowired
    private TaskSourceVideoMapper taskSourceVideoMapper;
    
    @Autowired
    private TaskOutputSegmentMapper taskOutputSegmentMapper;
    
    @Override
    @Transactional
    public String createTask(SubmissionTask task, List<TaskSourceVideo> sourceVideos) {
        // 设置任务ID和时间戳
        String taskId = UUID.randomUUID().toString();
        task.setTaskId(taskId.toString());
        task.setCreatedAt(new Date());
        task.setUpdatedAt(new Date());
        
        // 设置默认状态
        task.setStatus(SubmissionTask.TaskStatus.PENDING);
        
        // 插入任务
        submissionTaskMapper.insert(task);
        log.info("创建投稿任务，任务ID: {}", taskId);
        
        // 插入源视频
        for (int i = 0; i < sourceVideos.size(); i++) {
            TaskSourceVideo sourceVideo = sourceVideos.get(i);
            sourceVideo.setId(UUID.randomUUID().toString());
            sourceVideo.setTaskId(taskId);
            sourceVideo.setSortOrder(i + 1);
            taskSourceVideoMapper.insert(sourceVideo);
            log.info("插入源视频，任务ID: {}, 视频ID: {}", taskId, sourceVideo.getId());
        }
        
        return taskId;
    }
    
    @Override
    public List<SubmissionTask> findAllTasks() {
        return submissionTaskMapper.findAllOrderByCreatedAtDesc();
    }
    
    @Override
    public List<SubmissionTask> findTasksByStatus(SubmissionTask.TaskStatus status) {
        return submissionTaskMapper.findByStatusOrderByCreatedAtDesc(status);
    }
    
    @Override
    public SubmissionTask getTaskDetail(String taskId) {
        return submissionTaskMapper.selectById(taskId);
    }
    
    @Override
    @Transactional
    public void updateTaskStatus(String taskId, SubmissionTask.TaskStatus status) {
        try {
            SubmissionTask task = new SubmissionTask();
            task.setTaskId(taskId);
            task.setStatus(status);
            submissionTaskMapper.updateById(task);
        } catch (Exception e) {
            log.error("更新任务状态时发生异常，任务ID: {}, 状态: {}", taskId, status, e);
        }
    }
    
    @Override
    @Transactional
    public void updateTaskStatusAndBvid(String taskId, SubmissionTask.TaskStatus status, String bvid) {
        SubmissionTask task = submissionTaskMapper.selectById(taskId);
        if (task != null) {
            task.setStatus(status);
            task.setBvid(bvid);
            task.setUpdatedAt(new Date());
            submissionTaskMapper.updateById(task);
            log.info("更新任务状态和BVID，任务ID: {}, 状态: {}, BVID: {}", taskId, status, bvid);
        }
    }
    
    @Override
    public List<TaskSourceVideo> getSourceVideosByTaskId(String taskId) {
        return taskSourceVideoMapper.findByTaskIdOrderBySortOrder(taskId);
    }
    
    @Override
    public List<TaskOutputSegment> getOutputSegmentsByTaskId(String taskId) {
        return taskOutputSegmentMapper.findByTaskIdOrderByPartOrder(taskId);
    }
    
    @Override
    @Transactional
    public void saveOutputSegments(List<TaskOutputSegment> segments) {
        for (TaskOutputSegment segment : segments) {
            segment.setSegmentId(UUID.randomUUID().toString());
            taskOutputSegmentMapper.insert(segment);
        }
        log.info("保存输出分段成功，分段数量: {}", segments.size());
    }
    
    @Override
    @Transactional
    public void updateSegmentUploadStatusAndCid(String segmentId, TaskOutputSegment.UploadStatus status, Long cid) {
        taskOutputSegmentMapper.updateUploadStatusAndCid(segmentId, status, cid);
        log.info("更新分段上传状态和CID，分段ID: {}, 状态: {}, CID: {}", segmentId, status, cid);
    }
    
    @Override
    public List<TaskOutputSegment> getSegmentsByTaskIdAndUploadStatus(String taskId, TaskOutputSegment.UploadStatus status) {
        return taskOutputSegmentMapper.findByTaskIdAndUploadStatus(taskId, status);
    }
}