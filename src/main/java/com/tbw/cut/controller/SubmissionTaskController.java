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
    
    @Autowired
    private com.tbw.cut.workflow.service.WorkflowEngine workflowEngine;
    
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
    public Result<TaskCreationResult> createTask(@RequestBody TaskCreationRequest request) {
        try {
            SubmissionTask task = request.getTask();
            List<TaskSourceVideo> sourceVideos = request.getSourceVideos();
            
            // 设置默认状态
            task.setStatus(SubmissionTask.TaskStatus.PENDING);
            
            // 创建任务
            String taskId = submissionTaskService.createTask(task, sourceVideos);
            
            // 创建结果对象
            TaskCreationResult result = new TaskCreationResult();
            result.setTaskId(taskId);
            
            // 如果包含工作流配置，启动工作流
            if (request.hasWorkflowConfig()) {
                try {
                    com.tbw.cut.workflow.model.WorkflowConfig workflowConfig = request.getEffectiveWorkflowConfig();
                    com.tbw.cut.workflow.model.WorkflowInstance workflowInstance = 
                            workflowEngine.startWorkflow(taskId, workflowConfig);
                    
                    result.setWorkflowInstanceId(workflowInstance.getInstanceId());
                    result.setWorkflowStatus(workflowInstance.getStatus().toString());
                    
                    log.info("创建投稿任务并启动工作流成功，任务ID: {}, 工作流实例ID: {}", 
                            taskId, workflowInstance.getInstanceId());
                } catch (Exception workflowException) {
                    log.warn("启动工作流失败，但任务创建成功，任务ID: {}", taskId, workflowException);
                    result.setWorkflowError("工作流启动失败: " + workflowException.getMessage());
                }
            } else {
                log.info("创建投稿任务成功（无工作流配置），任务ID: {}", taskId);
            }
            
            return Result.success(result);
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
     * 任务创建结果DTO
     */
    public static class TaskCreationResult {
        private String taskId;
        private String workflowInstanceId;
        private String workflowStatus;
        private String workflowError;
        
        // Getters and setters
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
        
        public String getWorkflowInstanceId() {
            return workflowInstanceId;
        }
        
        public void setWorkflowInstanceId(String workflowInstanceId) {
            this.workflowInstanceId = workflowInstanceId;
        }
        
        public String getWorkflowStatus() {
            return workflowStatus;
        }
        
        public void setWorkflowStatus(String workflowStatus) {
            this.workflowStatus = workflowStatus;
        }
        
        public String getWorkflowError() {
            return workflowError;
        }
        
        public void setWorkflowError(String workflowError) {
            this.workflowError = workflowError;
        }
        
        /**
         * 检查是否有工作流实例
         */
        public boolean hasWorkflowInstance() {
            return workflowInstanceId != null && !workflowInstanceId.isEmpty();
        }
        
        /**
         * 检查工作流是否启动失败
         */
        public boolean hasWorkflowError() {
            return workflowError != null && !workflowError.isEmpty();
        }
    }
    
    /**
     * 任务创建请求DTO
     */
    public static class TaskCreationRequest {
        private SubmissionTask task;
        private List<TaskSourceVideo> sourceVideos;
        private com.tbw.cut.workflow.model.WorkflowConfig workflowConfig;
        
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
        
        public com.tbw.cut.workflow.model.WorkflowConfig getWorkflowConfig() {
            return workflowConfig;
        }
        
        public void setWorkflowConfig(com.tbw.cut.workflow.model.WorkflowConfig workflowConfig) {
            this.workflowConfig = workflowConfig;
        }
        
        /**
         * 检查是否包含工作流配置
         */
        public boolean hasWorkflowConfig() {
            return workflowConfig != null;
        }
        
        /**
         * 获取有效的工作流配置（如果没有则返回默认配置）
         */
        public com.tbw.cut.workflow.model.WorkflowConfig getEffectiveWorkflowConfig() {
            if (workflowConfig != null) {
                return workflowConfig;
            }
            // 返回投稿任务的默认配置（启用分段处理）
            return com.tbw.cut.workflow.model.WorkflowConfig.builder()
                    .userId("current_user") // TODO: 从用户会话获取真实用户ID
                    .enableDirectSubmission(false)
                    .enableClipping(true)
                    .enableMerging(true)
                    .segmentationConfig(com.tbw.cut.workflow.model.SegmentationConfig.builder()
                            .enabled(true)
                            .segmentDurationSeconds(133)
                            .maxSegmentCount(50)
                            .preserveOriginal(true)
                            .build())
                    .build();
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
    
    // ==================== 工作流状态监控 API ====================
    
    /**
     * 获取任务的工作流状态
     */
    @GetMapping("/{taskId}/workflow/status")
    public Result<WorkflowStatusResponse> getWorkflowStatus(@PathVariable("taskId") String taskId) {
        try {
            com.tbw.cut.workflow.model.WorkflowInstance workflowInstance = 
                    workflowEngine.getWorkflowByTaskId(taskId);
            
            if (workflowInstance == null) {
                return Result.error("该任务没有关联的工作流");
            }
            
            WorkflowStatusResponse response = new WorkflowStatusResponse();
            response.setInstanceId(workflowInstance.getInstanceId());
            response.setTaskId(workflowInstance.getTaskId());
            response.setStatus(workflowInstance.getStatus().name());
            response.setStatusDescription(workflowInstance.getStatus().getDescription());
            response.setProgress(workflowInstance.getProgress());
            response.setStartTime(workflowInstance.getStartTime());
            response.setEndTime(workflowInstance.getEndTime());
            response.setErrorMessage(workflowInstance.getErrorMessage());
            response.setCurrentStepIndex(workflowInstance.getCurrentStepIndex());
            
            // 转换步骤信息
            List<WorkflowStepResponse> stepResponses = workflowInstance.getSteps().stream()
                    .map(this::convertToStepResponse)
                    .collect(java.util.stream.Collectors.toList());
            response.setSteps(stepResponses);
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("获取工作流状态时发生异常，任务ID: {}", taskId, e);
            return Result.error("获取工作流状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 暂停工作流
     */
    @PostMapping("/{taskId}/workflow/pause")
    public Result<String> pauseWorkflow(@PathVariable("taskId") String taskId) {
        try {
            com.tbw.cut.workflow.model.WorkflowInstance workflowInstance = 
                    workflowEngine.getWorkflowByTaskId(taskId);
            
            if (workflowInstance == null) {
                return Result.error("该任务没有关联的工作流");
            }
            
            boolean success = workflowEngine.pauseWorkflow(workflowInstance.getInstanceId());
            
            if (success) {
                log.info("工作流暂停成功，任务ID: {}, 工作流ID: {}", taskId, workflowInstance.getInstanceId());
                return Result.success("工作流已暂停");
            } else {
                return Result.error("工作流暂停失败，可能工作流未在运行");
            }
        } catch (Exception e) {
            log.error("暂停工作流时发生异常，任务ID: {}", taskId, e);
            return Result.error("暂停工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 恢复工作流
     */
    @PostMapping("/{taskId}/workflow/resume")
    public Result<String> resumeWorkflow(@PathVariable("taskId") String taskId) {
        try {
            com.tbw.cut.workflow.model.WorkflowInstance workflowInstance = 
                    workflowEngine.getWorkflowByTaskId(taskId);
            
            if (workflowInstance == null) {
                return Result.error("该任务没有关联的工作流");
            }
            
            boolean success = workflowEngine.resumeWorkflow(workflowInstance.getInstanceId());
            
            if (success) {
                log.info("工作流恢复成功，任务ID: {}, 工作流ID: {}", taskId, workflowInstance.getInstanceId());
                return Result.success("工作流已恢复");
            } else {
                return Result.error("工作流恢复失败，可能工作流未暂停");
            }
        } catch (Exception e) {
            log.error("恢复工作流时发生异常，任务ID: {}", taskId, e);
            return Result.error("恢复工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消工作流
     */
    @PostMapping("/{taskId}/workflow/cancel")
    public Result<String> cancelWorkflow(@PathVariable("taskId") String taskId) {
        try {
            com.tbw.cut.workflow.model.WorkflowInstance workflowInstance = 
                    workflowEngine.getWorkflowByTaskId(taskId);
            
            if (workflowInstance == null) {
                return Result.error("该任务没有关联的工作流");
            }
            
            boolean success = workflowEngine.cancelWorkflow(workflowInstance.getInstanceId());
            
            if (success) {
                log.info("工作流取消成功，任务ID: {}, 工作流ID: {}", taskId, workflowInstance.getInstanceId());
                return Result.success("工作流已取消");
            } else {
                return Result.error("工作流取消失败，可能工作流已完成");
            }
        } catch (Exception e) {
            log.error("取消工作流时发生异常，任务ID: {}", taskId, e);
            return Result.error("取消工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有活跃的工作流
     */
    @GetMapping("/workflow/active")
    public Result<List<WorkflowStatusResponse>> getActiveWorkflows() {
        try {
            List<com.tbw.cut.workflow.model.WorkflowInstance> activeWorkflows = 
                    workflowEngine.getActiveWorkflows();
            
            List<WorkflowStatusResponse> responses = activeWorkflows.stream()
                    .map(this::convertToWorkflowStatusResponse)
                    .collect(java.util.stream.Collectors.toList());
            
            return Result.success(responses);
        } catch (Exception e) {
            log.error("获取活跃工作流时发生异常", e);
            return Result.error("获取活跃工作流失败: " + e.getMessage());
        }
    }
    
    // ==================== 响应DTO类 ====================
    
    /**
     * 工作流状态响应DTO
     */
    public static class WorkflowStatusResponse {
        private String instanceId;
        private String taskId;
        private String status;
        private String statusDescription;
        private int progress;
        private java.time.LocalDateTime startTime;
        private java.time.LocalDateTime endTime;
        private String errorMessage;
        private int currentStepIndex;
        private List<WorkflowStepResponse> steps;
        
        // Getters and setters
        public String getInstanceId() {
            return instanceId;
        }
        
        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }
        
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getStatusDescription() {
            return statusDescription;
        }
        
        public void setStatusDescription(String statusDescription) {
            this.statusDescription = statusDescription;
        }
        
        public int getProgress() {
            return progress;
        }
        
        public void setProgress(int progress) {
            this.progress = progress;
        }
        
        public java.time.LocalDateTime getStartTime() {
            return startTime;
        }
        
        public void setStartTime(java.time.LocalDateTime startTime) {
            this.startTime = startTime;
        }
        
        public java.time.LocalDateTime getEndTime() {
            return endTime;
        }
        
        public void setEndTime(java.time.LocalDateTime endTime) {
            this.endTime = endTime;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public int getCurrentStepIndex() {
            return currentStepIndex;
        }
        
        public void setCurrentStepIndex(int currentStepIndex) {
            this.currentStepIndex = currentStepIndex;
        }
        
        public List<WorkflowStepResponse> getSteps() {
            return steps;
        }
        
        public void setSteps(List<WorkflowStepResponse> steps) {
            this.steps = steps;
        }
    }
    
    /**
     * 工作流步骤响应DTO
     */
    public static class WorkflowStepResponse {
        private String stepId;
        private String type;
        private String typeDescription;
        private String status;
        private String statusDescription;
        private java.time.LocalDateTime startTime;
        private java.time.LocalDateTime endTime;
        private String inputPath;
        private String outputPath;
        private String errorMessage;
        private Long durationMs;
        
        // Getters and setters
        public String getStepId() {
            return stepId;
        }
        
        public void setStepId(String stepId) {
            this.stepId = stepId;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getTypeDescription() {
            return typeDescription;
        }
        
        public void setTypeDescription(String typeDescription) {
            this.typeDescription = typeDescription;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getStatusDescription() {
            return statusDescription;
        }
        
        public void setStatusDescription(String statusDescription) {
            this.statusDescription = statusDescription;
        }
        
        public java.time.LocalDateTime getStartTime() {
            return startTime;
        }
        
        public void setStartTime(java.time.LocalDateTime startTime) {
            this.startTime = startTime;
        }
        
        public java.time.LocalDateTime getEndTime() {
            return endTime;
        }
        
        public void setEndTime(java.time.LocalDateTime endTime) {
            this.endTime = endTime;
        }
        
        public String getInputPath() {
            return inputPath;
        }
        
        public void setInputPath(String inputPath) {
            this.inputPath = inputPath;
        }
        
        public String getOutputPath() {
            return outputPath;
        }
        
        public void setOutputPath(String outputPath) {
            this.outputPath = outputPath;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public Long getDurationMs() {
            return durationMs;
        }
        
        public void setDurationMs(Long durationMs) {
            this.durationMs = durationMs;
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 转换工作流实例为响应DTO
     */
    private WorkflowStatusResponse convertToWorkflowStatusResponse(com.tbw.cut.workflow.model.WorkflowInstance instance) {
        WorkflowStatusResponse response = new WorkflowStatusResponse();
        response.setInstanceId(instance.getInstanceId());
        response.setTaskId(instance.getTaskId());
        response.setStatus(instance.getStatus().name());
        response.setStatusDescription(instance.getStatus().getDescription());
        response.setProgress(instance.getProgress());
        response.setStartTime(instance.getStartTime());
        response.setEndTime(instance.getEndTime());
        response.setErrorMessage(instance.getErrorMessage());
        response.setCurrentStepIndex(instance.getCurrentStepIndex());
        
        List<WorkflowStepResponse> stepResponses = instance.getSteps().stream()
                .map(this::convertToStepResponse)
                .collect(java.util.stream.Collectors.toList());
        response.setSteps(stepResponses);
        
        return response;
    }
    
    /**
     * 转换工作流步骤为响应DTO
     */
    private WorkflowStepResponse convertToStepResponse(com.tbw.cut.workflow.model.WorkflowStep step) {
        WorkflowStepResponse response = new WorkflowStepResponse();
        response.setStepId(step.getStepId());
        response.setType(step.getType().name());
        response.setTypeDescription(getStepTypeDescription(step.getType()));
        response.setStatus(step.getStatus().name());
        response.setStatusDescription(getStepStatusDescription(step.getStatus()));
        response.setStartTime(step.getStartTime());
        response.setEndTime(step.getEndTime());
        response.setInputPath(step.getInputPath());
        response.setOutputPath(step.getOutputPath());
        response.setErrorMessage(step.getErrorMessage());
        response.setDurationMs(step.getDurationMs());
        
        return response;
    }
    
    /**
     * 获取步骤类型描述
     */
    private String getStepTypeDescription(com.tbw.cut.workflow.model.StepType stepType) {
        switch (stepType) {
            case CLIPPING:
                return "视频剪辑";
            case MERGING:
                return "视频合并";
            case SEGMENTATION:
                return "视频分段";
            case SUBMISSION:
                return "视频投稿";
            default:
                return stepType.name();
        }
    }
    
    /**
     * 获取步骤状态描述
     */
    private String getStepStatusDescription(com.tbw.cut.workflow.model.StepStatus stepStatus) {
        switch (stepStatus) {
            case PENDING:
                return "待执行";
            case RUNNING:
                return "执行中";
            case COMPLETED:
                return "已完成";
            case FAILED:
                return "执行失败";
            case SKIPPED:
                return "已跳过";
            default:
                return stepStatus.name();
        }
    }
}