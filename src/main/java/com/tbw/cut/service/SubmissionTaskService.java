package com.tbw.cut.service;

import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.entity.TaskOutputSegment;
import java.util.List;
import java.util.UUID;

public interface SubmissionTaskService {
    
    /**
     * 创建投稿任务
     * @param task 任务信息
     * @param sourceVideos 源视频列表
     * @return 任务ID
     */
    String createTask(SubmissionTask task, List<TaskSourceVideo> sourceVideos);
    
    /**
     * 查找所有任务并按创建时间倒序排列
     * @return 任务列表
     */
    List<SubmissionTask> findAllTasks();
    
    /**
     * 根据状态查找任务并按创建时间倒序排列
     * @param status 任务状态
     * @return 任务列表
     */
    List<SubmissionTask> findTasksByStatus(SubmissionTask.TaskStatus status);
    
    /**
     * 根据任务ID获取任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    SubmissionTask getTaskDetail(String taskId);
    
    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 新状态
     */
    void updateTaskStatus(String taskId, SubmissionTask.TaskStatus status);
    
    /**
     * 更新任务状态和BVID
     * @param taskId 任务ID
     * @param status 新状态
     * @param bvid B站视频ID
     */
    void updateTaskStatusAndBvid(String taskId, SubmissionTask.TaskStatus status, String bvid);
    
    /**
     * 根据任务ID获取源视频列表
     * @param taskId 任务ID
     * @return 源视频列表
     */
    List<TaskSourceVideo> getSourceVideosByTaskId(String taskId);
    
    /**
     * 根据任务ID获取输出分段列表
     * @param taskId 任务ID
     * @return 输出分段列表
     */
    List<TaskOutputSegment> getOutputSegmentsByTaskId(String taskId);
    
    /**
     * 保存输出分段
     * @param segments 分段列表
     */
    void saveOutputSegments(List<TaskOutputSegment> segments);
    
    /**
     * 更新分段上传状态和CID
     * @param segmentId 分段ID
     * @param status 上传状态
     * @param cid B站CID
     */
    void updateSegmentUploadStatusAndCid(String segmentId, TaskOutputSegment.UploadStatus status, Long cid);
    
    /**
     * 根据任务ID和上传状态查找分段
     * @param taskId 任务ID
     * @param status 上传状态
     * @return 分段列表
     */
    List<TaskOutputSegment> getSegmentsByTaskIdAndUploadStatus(String taskId, TaskOutputSegment.UploadStatus status);
}