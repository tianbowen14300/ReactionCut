package com.tbw.cut.controller;

import com.tbw.cut.dto.QueuePosition;
import com.tbw.cut.dto.QueueStatus;
import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.entity.TaskOutputSegment;
import com.tbw.cut.entity.MergedVideo;
import com.tbw.cut.service.SubmissionTaskService;
import com.tbw.cut.service.SubmissionQueueService;
import com.tbw.cut.service.TaskExecutorService;
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
    private SubmissionQueueService submissionQueueService;
    
    @Autowired
    private TaskExecutorService taskExecutorService;
    
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
    
    /**
     * 执行指定任务（加入队列）
     */
    @PostMapping("/{taskId}/execute")
    public Result<QueuePosition> executeTask(@PathVariable("taskId") String taskId) {
        try {
            // 检查任务是否存在
            SubmissionTask task = submissionTaskService.getTaskDetail(taskId);
            if (task == null) {
                return Result.error("任务不存在");
            }
            
            // 将任务加入队列
            QueuePosition queuePosition = submissionQueueService.enqueueTask(taskId);
            
            if (queuePosition != null) {
                log.info("任务已加入队列，任务ID: {}, 队列位置: {}", taskId, queuePosition.getPosition());
                return Result.success(queuePosition);
            } else {
                return Result.error("任务加入队列失败");
            }
        } catch (Exception e) {
            log.error("执行投稿任务时发生异常，任务ID: {}", taskId, e);
            return Result.error("执行投稿任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取队列状态
     */
    @GetMapping("/queue/status")
    public Result<QueueStatus> getQueueStatus() {
        try {
            QueueStatus queueStatus = submissionQueueService.getQueueStatus();
            return Result.success(queueStatus);
        } catch (Exception e) {
            log.error("获取队列状态时发生异常", e);
            return Result.error("获取队列状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取任务在队列中的位置
     */
    @GetMapping("/{taskId}/queue-position")
    public Result<QueuePosition> getTaskQueuePosition(@PathVariable("taskId") String taskId) {
        try {
            QueuePosition queuePosition = submissionQueueService.getTaskPosition(taskId);
            
            if (queuePosition != null) {
                return Result.success(queuePosition);
            } else {
                return Result.error("任务不在队列中");
            }
        } catch (Exception e) {
            log.error("获取任务队列位置时发生异常，任务ID: {}", taskId, e);
            return Result.error("获取任务队列位置失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消排队的任务
     */
    @PostMapping("/{taskId}/cancel")
    public Result<String> cancelQueuedTask(@PathVariable("taskId") String taskId) {
        try {
            boolean success = submissionQueueService.cancelQueuedTask(taskId);
            
            if (success) {
                log.info("任务取消成功，任务ID: {}", taskId);
                return Result.success("任务已取消");
            } else {
                return Result.error("任务取消失败，可能任务不在队列中或正在处理");
            }
        } catch (Exception e) {
            log.error("取消任务时发生异常，任务ID: {}", taskId, e);
            return Result.error("取消任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 队列管理 - 启动队列
     */
    @PostMapping("/queue/start")
    public Result<String> startQueue() {
        try {
            submissionQueueService.startQueue();
            return Result.success("队列已启动");
        } catch (Exception e) {
            log.error("启动队列时发生异常", e);
            return Result.error("启动队列失败: " + e.getMessage());
        }
    }
    
    /**
     * 队列管理 - 停止队列
     */
    @PostMapping("/queue/stop")
    public Result<String> stopQueue() {
        try {
            submissionQueueService.stopQueue();
            return Result.success("队列已停止");
        } catch (Exception e) {
            log.error("停止队列时发生异常", e);
            return Result.error("停止队列失败: " + e.getMessage());
        }
    }
    
    /**
     * 队列管理 - 暂停队列
     */
    @PostMapping("/queue/pause")
    public Result<String> pauseQueue() {
        try {
            submissionQueueService.pauseQueue();
            return Result.success("队列已暂停");
        } catch (Exception e) {
            log.error("暂停队列时发生异常", e);
            return Result.error("暂停队列失败: " + e.getMessage());
        }
    }
    
    /**
     * 队列管理 - 恢复队列
     */
    @PostMapping("/queue/resume")
    public Result<String> resumeQueue() {
        try {
            submissionQueueService.resumeQueue();
            return Result.success("队列已恢复");
        } catch (Exception e) {
            log.error("恢复队列时发生异常", e);
            return Result.error("恢复队列失败: " + e.getMessage());
        }
    }
    
    /**
     * 队列管理 - 清空队列
     */
    @PostMapping("/queue/clear")
    public Result<String> clearQueue() {
        try {
            boolean success = submissionQueueService.clearQueue();
            
            if (success) {
                return Result.success("队列已清空");
            } else {
                return Result.error("队列清空失败，可能有任务正在处理");
            }
        } catch (Exception e) {
            log.error("清空队列时发生异常", e);
            return Result.error("清空队列失败: " + e.getMessage());
        }
    }
}