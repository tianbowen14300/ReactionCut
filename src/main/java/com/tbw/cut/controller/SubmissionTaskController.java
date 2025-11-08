package com.tbw.cut.controller;

import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.entity.TaskOutputSegment;
import com.tbw.cut.entity.MergedVideo;
import com.tbw.cut.service.SubmissionTaskService;
import com.tbw.cut.service.VideoProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/submission-tasks")
public class SubmissionTaskController {
    
    @Autowired
    private SubmissionTaskService submissionTaskService;
    
    @Autowired
    private VideoProcessService videoProcessService;
    
    /**
     * 统一响应结果类
     */
    public static class Result<T> {
        private int code;
        private String message;
        private T data;
        
        public static <T> Result<T> success(T data) {
            Result<T> result = new Result<>();
            result.code = 0;
            result.message = "success";
            result.data = data;
            return result;
        }
        
        public static <T> Result<T> error(String message) {
            Result<T> result = new Result<>();
            result.code = -1;
            result.message = message;
            return result;
        }
        
        // Getters and setters
        public int getCode() {
            return code;
        }
        
        public void setCode(int code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public T getData() {
            return data;
        }
        
        public void setData(T data) {
            this.data = data;
        }
    }
    
    /**
     * 创建投稿任务
     */
    @PostMapping
    public Result<String> createTask(@RequestBody TaskCreationRequest request) {
        try {
            SubmissionTask task = request.getTask();
            List<TaskSourceVideo> sourceVideos = request.getSourceVideos();
            
            // 设置默认状态
            task.setStatus(SubmissionTask.TaskStatus.PENDING);
            
            String taskId = submissionTaskService.createTask(task, sourceVideos);
            
            log.info("创建投稿任务成功，任务ID: {}", taskId);
            return Result.success(taskId);
        } catch (Exception e) {
            log.error("创建投稿任务时发生异常", e);
            return Result.error("创建投稿任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询所有任务（按创建时间倒序）
     */
    @GetMapping
    public Result<List<SubmissionTask>> getAllTasks() {
        try {
            List<SubmissionTask> tasks = submissionTaskService.findAllTasks();
            return Result.success(tasks);
        } catch (Exception e) {
            log.error("查询任务时发生异常", e);
            return Result.error("查询任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据状态查询任务（按创建时间倒序）
     */
    @GetMapping("/status/{status}")
    public Result<List<SubmissionTask>> getTasksByStatus(@PathVariable("status") SubmissionTask.TaskStatus status) {
        try {
            List<SubmissionTask> tasks = submissionTaskService.findTasksByStatus(status);
            return Result.success(tasks);
        } catch (Exception e) {
            log.error("查询任务时发生异常", e);
            return Result.error("查询任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据任务ID获取任务详情
     */
    @GetMapping("/{taskId}")
    public Result<TaskDetailResponse> getTaskById(@PathVariable("taskId") String taskId) {
        try {
            // 获取任务基本信息
            SubmissionTask task = submissionTaskService.getTaskDetail(taskId);
            if (task == null) {
                return Result.error("任务不存在");
            }
            
            // 获取源视频列表
            List<TaskSourceVideo> sourceVideos = submissionTaskService.getSourceVideosByTaskId(taskId);
            
            // 获取输出分段列表
            List<TaskOutputSegment> outputSegments = submissionTaskService.getOutputSegmentsByTaskId(taskId);
            
            // 获取合并视频列表
            List<MergedVideo> mergedVideos = videoProcessService.getMergedVideos(taskId);
            
            TaskDetailResponse response = new TaskDetailResponse();
            response.setTask(task);
            response.setSourceVideos(sourceVideos);
            response.setOutputSegments(outputSegments);
            response.setMergedVideos(mergedVideos);
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("查询任务详情时发生异常，任务ID: {}", taskId, e);
            return Result.error("查询任务详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 任务详情响应DTO
     */
    public static class TaskDetailResponse {
        private SubmissionTask task;
        private List<TaskSourceVideo> sourceVideos;
        private List<TaskOutputSegment> outputSegments;
        private List<MergedVideo> mergedVideos;
        
        // Getters and setters
        public SubmissionTask getTask() {
            return task;
        }
        
        public void setTask(SubmissionTask task) {
            this.task = task;
        }
        
        public List<TaskSourceVideo> getSourceVideos() {
            return sourceVideos;
        }
        
        public void setSourceVideos(List<TaskSourceVideo> sourceVideos) {
            this.sourceVideos = sourceVideos;
        }
        
        public List<TaskOutputSegment> getOutputSegments() {
            return outputSegments;
        }
        
        public void setOutputSegments(List<TaskOutputSegment> outputSegments) {
            this.outputSegments = outputSegments;
        }
        
        public List<MergedVideo> getMergedVideos() {
            return mergedVideos;
        }
        
        public void setMergedVideos(List<MergedVideo> mergedVideos) {
            this.mergedVideos = mergedVideos;
        }
    }
    
    /**
     * 任务创建请求DTO
     */
    public static class TaskCreationRequest {
        private SubmissionTask task;
        private List<TaskSourceVideo> sourceVideos;
        
        // Getters and setters
        public SubmissionTask getTask() {
            return task;
        }
        
        public void setTask(SubmissionTask task) {
            this.task = task;
        }
        
        public List<TaskSourceVideo> getSourceVideos() {
            return sourceVideos;
        }
        
        public void setSourceVideos(List<TaskSourceVideo> sourceVideos) {
            this.sourceVideos = sourceVideos;
        }
    }
}