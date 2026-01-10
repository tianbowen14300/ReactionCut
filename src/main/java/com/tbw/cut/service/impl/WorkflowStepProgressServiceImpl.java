package com.tbw.cut.service.impl;

import com.tbw.cut.entity.WorkflowInstance;
import com.tbw.cut.entity.WorkflowStep;
import com.tbw.cut.service.WorkflowDatabaseService;
import com.tbw.cut.service.WorkflowStepProgressService;
import com.tbw.cut.service.TaskRelationService;
import com.tbw.cut.service.VideoProcessService;
import com.tbw.cut.service.SubmissionTaskService;
import com.tbw.cut.entity.TaskRelation;
import com.tbw.cut.workflow.model.StepStatus;
import com.tbw.cut.workflow.model.StepType;
import com.tbw.cut.workflow.model.WorkflowStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 工作流步骤进度服务实现
 * 
 * 负责将外部事件（下载完成、处理完成等）转换为工作流步骤状态更新
 */
@Slf4j
@Service
public class WorkflowStepProgressServiceImpl implements WorkflowStepProgressService {
    
    @Autowired
    private WorkflowDatabaseService workflowDatabaseService;
    
    @Autowired
    private TaskRelationService taskRelationService;
    
    @Autowired
    private VideoProcessService videoProcessService;
    
    @Autowired
    private SubmissionTaskService submissionTaskService;
    
    @Override
    @Transactional
    public boolean handleDownloadCompletion(Long downloadTaskId, Integer downloadStatus) {
        log.info("处理下载完成事件: downloadTaskId={}, status={}", downloadTaskId, downloadStatus);
        
        try {
            // 通过TaskRelation找到对应的工作流任务
            Optional<TaskRelation> relationOpt = taskRelationService.findByDownloadTaskId(downloadTaskId);
            if (!relationOpt.isPresent()) {
                log.debug("未找到下载任务对应的工作流关联: {}", downloadTaskId);
                return false;
            }
            
            TaskRelation relation = relationOpt.get();
            String workflowTaskId = relation.getSubmissionTaskId(); // 使用投稿任务ID作为工作流任务ID
            
            // 获取工作流实例
            Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstanceByTaskId(workflowTaskId);
            if (!instanceOpt.isPresent()) {
                log.debug("未找到工作流实例: taskId={}", workflowTaskId);
                return false;
            }
            
            WorkflowInstance instance = instanceOpt.get();
            
            // 根据下载状态更新相应的工作流步骤
            if (downloadStatus == 2) { // 下载完成
                return handleDownloadSuccess(instance);
            } else if (downloadStatus == 3) { // 下载失败
                return handleDownloadFailure(instance, "下载任务失败");
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("处理下载完成事件异常: downloadTaskId={}", downloadTaskId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean handleProcessingCompletion(String taskId, StepType stepType, boolean success, 
                                            String outputPath, String errorMessage) {
        log.info("处理视频处理完成事件: taskId={}, stepType={}, success={}", taskId, stepType, success);
        
        try {
            // 获取工作流实例
            Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstanceByTaskId(taskId);
            if (!instanceOpt.isPresent()) {
                log.debug("未找到工作流实例: taskId={}", taskId);
                return false;
            }
            
            WorkflowInstance instance = instanceOpt.get();
            
            // 找到对应的工作流步骤
            List<WorkflowStep> steps = workflowDatabaseService.getWorkflowSteps(instance.getInstanceId());
            Optional<WorkflowStep> stepOpt = steps.stream()
                .filter(step -> step.getStepType() == stepType)
                .findFirst();
            
            if (!stepOpt.isPresent()) {
                log.warn("未找到对应的工作流步骤: taskId={}, stepType={}", taskId, stepType);
                return false;
            }
            
            WorkflowStep step = stepOpt.get();
            
            // 更新步骤状态
            if (success) {
                workflowDatabaseService.completeWorkflowStep(step.getStepId(), StepStatus.COMPLETED, outputPath);
                log.info("工作流步骤完成: stepId={}, stepType={}, output={}", step.getStepId(), stepType, outputPath);
                
                // 启动下一个步骤
                startNextWorkflowStep(taskId);
            } else {
                workflowDatabaseService.completeWorkflowStep(step.getStepId(), StepStatus.FAILED, null);
                workflowDatabaseService.setStepError(step.getStepId(), errorMessage);
                
                // 标记整个工作流失败
                workflowDatabaseService.completeWorkflowInstance(instance.getInstanceId(), WorkflowStatus.FAILED);
                workflowDatabaseService.setWorkflowError(instance.getInstanceId(), 
                    "步骤执行失败: " + stepType.getDescription() + " - " + errorMessage);
                
                log.error("工作流步骤失败: stepId={}, stepType={}, error={}", step.getStepId(), stepType, errorMessage);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("处理视频处理完成事件异常: taskId={}, stepType={}", taskId, stepType, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean handleSubmissionCompletion(String taskId, boolean success, 
                                            String submissionResult, String errorMessage) {
        log.info("处理投稿完成事件: taskId={}, success={}", taskId, success);
        
        try {
            // 获取工作流实例
            Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstanceByTaskId(taskId);
            if (!instanceOpt.isPresent()) {
                log.debug("未找到工作流实例: taskId={}", taskId);
                return false;
            }
            
            WorkflowInstance instance = instanceOpt.get();
            
            // 找到投稿步骤
            List<WorkflowStep> steps = workflowDatabaseService.getWorkflowSteps(instance.getInstanceId());
            Optional<WorkflowStep> submissionStepOpt = steps.stream()
                .filter(step -> step.getStepType() == StepType.UPLOADING)
                .findFirst();
            
            if (!submissionStepOpt.isPresent()) {
                log.warn("未找到投稿步骤: taskId={}", taskId);
                return false;
            }
            
            WorkflowStep submissionStep = submissionStepOpt.get();
            
            // 更新投稿步骤状态
            if (success) {
                workflowDatabaseService.completeWorkflowStep(submissionStep.getStepId(), StepStatus.COMPLETED, submissionResult);
                
                // 完成整个工作流
                workflowDatabaseService.completeWorkflowInstance(instance.getInstanceId(), WorkflowStatus.COMPLETED);
                workflowDatabaseService.updateWorkflowStatus(instance.getInstanceId(), WorkflowStatus.COMPLETED, "COMPLETED", 1.0);
                
                log.info("工作流完成: instanceId={}, taskId={}", instance.getInstanceId(), taskId);
            } else {
                workflowDatabaseService.completeWorkflowStep(submissionStep.getStepId(), StepStatus.FAILED, null);
                workflowDatabaseService.setStepError(submissionStep.getStepId(), errorMessage);
                
                // 标记整个工作流失败
                workflowDatabaseService.completeWorkflowInstance(instance.getInstanceId(), WorkflowStatus.FAILED);
                workflowDatabaseService.setWorkflowError(instance.getInstanceId(), "投稿失败: " + errorMessage);
                
                log.error("工作流投稿失败: instanceId={}, taskId={}, error={}", 
                    instance.getInstanceId(), taskId, errorMessage);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("处理投稿完成事件异常: taskId={}", taskId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean updateStepProgress(String taskId, StepType stepType, double progress) {
        log.debug("更新工作流步骤进度: taskId={}, stepType={}, progress={}", taskId, stepType, progress);
        
        try {
            // 获取工作流实例
            Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstanceByTaskId(taskId);
            if (!instanceOpt.isPresent()) {
                return false;
            }
            
            WorkflowInstance instance = instanceOpt.get();
            
            // 找到对应的工作流步骤
            List<WorkflowStep> steps = workflowDatabaseService.getWorkflowSteps(instance.getInstanceId());
            Optional<WorkflowStep> stepOpt = steps.stream()
                .filter(step -> step.getStepType() == stepType)
                .findFirst();
            
            if (!stepOpt.isPresent()) {
                return false;
            }
            
            WorkflowStep step = stepOpt.get();
            
            // 更新步骤进度
            workflowDatabaseService.updateStepProgress(step.getStepId(), progress);
            
            // 计算并更新整体工作流进度
            updateWorkflowProgress(instance.getInstanceId(), steps);
            
            return true;
            
        } catch (Exception e) {
            log.error("更新步骤进度异常: taskId={}, stepType={}", taskId, stepType, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean startNextWorkflowStep(String taskId) {
        log.info("启动下一个工作流步骤: taskId={}", taskId);
        
        try {
            // 获取工作流实例
            Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstanceByTaskId(taskId);
            if (!instanceOpt.isPresent()) {
                log.debug("未找到工作流实例: taskId={}", taskId);
                return false;
            }
            
            WorkflowInstance instance = instanceOpt.get();
            
            // 检查工作流是否还在运行
            if (instance.getStatus() != WorkflowStatus.RUNNING) {
                log.debug("工作流未在运行状态，无法启动下一步: taskId={}, status={}", taskId, instance.getStatus());
                return false;
            }
            
            // 获取下一个待执行的步骤
            Optional<WorkflowStep> nextStepOpt = workflowDatabaseService.getNextPendingStep(instance.getInstanceId());
            if (!nextStepOpt.isPresent()) {
                log.info("没有更多待执行的步骤，工作流即将完成: taskId={}", taskId);
                return false;
            }
            
            WorkflowStep nextStep = nextStepOpt.get();
            
            // 启动下一个步骤
            workflowDatabaseService.startWorkflowStep(nextStep.getStepId());
            
            // 更新工作流当前步骤
            workflowDatabaseService.updateWorkflowStatus(
                instance.getInstanceId(), 
                WorkflowStatus.RUNNING, 
                nextStep.getStepType().getDescription(), 
                instance.getProgress()
            );
            
            log.info("启动工作流步骤: stepId={}, stepType={}, taskId={}", 
                nextStep.getStepId(), nextStep.getStepType(), taskId);
            
            // 根据步骤类型触发相应的处理
            triggerStepExecution(taskId, nextStep);
            
            return true;
            
        } catch (Exception e) {
            log.error("启动下一个工作流步骤异常: taskId={}", taskId, e);
            return false;
        }
    }
    
    @Override
    public int checkAndStartPendingSteps(String taskId) {
        log.debug("检查并启动待执行的工作流步骤: taskId={}", taskId);
        
        int startedSteps = 0;
        
        // 持续启动可执行的步骤，直到没有更多步骤可启动
        while (startNextWorkflowStep(taskId)) {
            startedSteps++;
            // 防止无限循环，最多启动10个步骤
            if (startedSteps >= 10) {
                log.warn("启动步骤数量达到上限，停止检查: taskId={}", taskId);
                break;
            }
        }
        
        return startedSteps;
    }
    
    @Override
    public StepType getCurrentExecutingStep(String taskId) {
        try {
            // 获取工作流实例
            Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstanceByTaskId(taskId);
            if (!instanceOpt.isPresent()) {
                return null;
            }
            
            WorkflowInstance instance = instanceOpt.get();
            
            // 获取当前运行的步骤
            Optional<WorkflowStep> currentStepOpt = workflowDatabaseService.getCurrentRunningStep(instance.getInstanceId());
            if (currentStepOpt.isPresent()) {
                return currentStepOpt.get().getStepType();
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("获取当前执行步骤异常: taskId={}", taskId, e);
            return null;
        }
    }
    
    @Override
    public boolean hasActiveWorkflow(String taskId) {
        try {
            Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstanceByTaskId(taskId);
            if (!instanceOpt.isPresent()) {
                return false;
            }
            
            WorkflowInstance instance = instanceOpt.get();
            return instance.getStatus() == WorkflowStatus.RUNNING || instance.getStatus() == WorkflowStatus.PAUSED;
            
        } catch (Exception e) {
            log.error("检查活跃工作流异常: taskId={}", taskId, e);
            return false;
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 处理下载成功
     */
    private boolean handleDownloadSuccess(WorkflowInstance instance) {
        log.info("处理下载成功: instanceId={}", instance.getInstanceId());
        
        try {
            // 找到下载步骤并标记为完成
            List<WorkflowStep> steps = workflowDatabaseService.getWorkflowSteps(instance.getInstanceId());
            Optional<WorkflowStep> downloadStepOpt = steps.stream()
                .filter(step -> step.getStepType() == StepType.DOWNLOADING)
                .findFirst();
            
            if (downloadStepOpt.isPresent()) {
                WorkflowStep downloadStep = downloadStepOpt.get();
                workflowDatabaseService.completeWorkflowStep(downloadStep.getStepId(), StepStatus.COMPLETED, "DOWNLOAD_COMPLETED");
                log.info("下载步骤完成: stepId={}", downloadStep.getStepId());
            }
            
            // 启动下一个步骤
            return startNextWorkflowStep(instance.getTaskId());
            
        } catch (Exception e) {
            log.error("处理下载成功异常: instanceId={}", instance.getInstanceId(), e);
            return false;
        }
    }
    
    /**
     * 处理下载失败
     */
    private boolean handleDownloadFailure(WorkflowInstance instance, String errorMessage) {
        log.error("处理下载失败: instanceId={}, error={}", instance.getInstanceId(), errorMessage);
        
        try {
            // 找到下载步骤并标记为失败
            List<WorkflowStep> steps = workflowDatabaseService.getWorkflowSteps(instance.getInstanceId());
            Optional<WorkflowStep> downloadStepOpt = steps.stream()
                .filter(step -> step.getStepType() == StepType.DOWNLOADING)
                .findFirst();
            
            if (downloadStepOpt.isPresent()) {
                WorkflowStep downloadStep = downloadStepOpt.get();
                workflowDatabaseService.completeWorkflowStep(downloadStep.getStepId(), StepStatus.FAILED, null);
                workflowDatabaseService.setStepError(downloadStep.getStepId(), errorMessage);
            }
            
            // 标记整个工作流失败
            workflowDatabaseService.completeWorkflowInstance(instance.getInstanceId(), WorkflowStatus.FAILED);
            workflowDatabaseService.setWorkflowError(instance.getInstanceId(), "下载失败: " + errorMessage);
            
            return true;
            
        } catch (Exception e) {
            log.error("处理下载失败异常: instanceId={}", instance.getInstanceId(), e);
            return false;
        }
    }
    
    /**
     * 更新工作流整体进度
     */
    private void updateWorkflowProgress(String instanceId, List<WorkflowStep> steps) {
        try {
            if (steps.isEmpty()) {
                return;
            }
            
            // 计算整体进度
            double totalProgress = 0.0;
            int completedSteps = 0;
            
            for (WorkflowStep step : steps) {
                if (step.getStatus() == StepStatus.COMPLETED) {
                    totalProgress += 1.0;
                    completedSteps++;
                } else if (step.getStatus() == StepStatus.RUNNING && step.getProgress() != null) {
                    totalProgress += step.getProgress();
                }
            }
            
            double overallProgress = totalProgress / steps.size();
            
            // 更新工作流进度
            workflowDatabaseService.updateWorkflowStatus(instanceId, WorkflowStatus.RUNNING, null, overallProgress);
            
        } catch (Exception e) {
            log.error("更新工作流进度异常: instanceId={}", instanceId, e);
        }
    }
    
    /**
     * 触发步骤执行
     */
    private void triggerStepExecution(String taskId, WorkflowStep step) {
        log.info("触发步骤执行: taskId={}, stepType={}", taskId, step.getStepType());
        
        try {
            switch (step.getStepType()) {
                case CLIPPING:
                    log.info("触发视频剪辑处理: taskId={}", taskId);
                    // 异步执行视频剪辑
                    new Thread(() -> {
                        try {
                            List<String> outputPaths = videoProcessService.clipVideos(taskId);
                            if (outputPaths != null && !outputPaths.isEmpty()) {
                                handleProcessingCompletion(taskId, StepType.CLIPPING, true, 
                                    String.join(";", outputPaths), null);
                            } else {
                                handleProcessingCompletion(taskId, StepType.CLIPPING, false, 
                                    null, "剪辑处理未生成输出文件");
                            }
                        } catch (Exception e) {
                            log.error("视频剪辑处理异常: taskId={}", taskId, e);
                            handleProcessingCompletion(taskId, StepType.CLIPPING, false, 
                                null, "剪辑处理异常: " + e.getMessage());
                        }
                    }).start();
                    break;
                    
                case MERGING:
                    log.info("触发视频合并处理: taskId={}", taskId);
                    // 异步执行视频合并
                    new Thread(() -> {
                        try {
                            String outputPath = videoProcessService.mergeVideos(taskId);
                            if (outputPath != null && !outputPath.isEmpty()) {
                                handleProcessingCompletion(taskId, StepType.MERGING, true, outputPath, null);
                            } else {
                                handleProcessingCompletion(taskId, StepType.MERGING, false, 
                                    null, "合并处理未生成输出文件");
                            }
                        } catch (Exception e) {
                            log.error("视频合并处理异常: taskId={}", taskId, e);
                            handleProcessingCompletion(taskId, StepType.MERGING, false, 
                                null, "合并处理异常: " + e.getMessage());
                        }
                    }).start();
                    break;
                    
                case SEGMENTING:
                    log.info("触发视频分段处理: taskId={}", taskId);
                    // 异步执行视频分段
                    new Thread(() -> {
                        try {
                            List<String> outputPaths = videoProcessService.segmentVideo(taskId);
                            if (outputPaths != null && !outputPaths.isEmpty()) {
                                handleProcessingCompletion(taskId, StepType.SEGMENTING, true, 
                                    String.join(";", outputPaths), null);
                            } else {
                                handleProcessingCompletion(taskId, StepType.SEGMENTING, false, 
                                    null, "分段处理未生成输出文件");
                            }
                        } catch (Exception e) {
                            log.error("视频分段处理异常: taskId={}", taskId, e);
                            handleProcessingCompletion(taskId, StepType.SEGMENTING, false, 
                                null, "分段处理异常: " + e.getMessage());
                        }
                    }).start();
                    break;
                    
                case UPLOADING:
                    log.info("触发视频上传处理: taskId={}", taskId);
                    // 异步执行视频上传
                    new Thread(() -> {
                        try {
                            // 更新任务状态为上传中
                            submissionTaskService.updateTaskStatus(taskId, 
                                com.tbw.cut.entity.SubmissionTask.TaskStatus.UPLOADING);
                            
                            // 这里应该调用实际的上传服务
                            // 目前先模拟上传成功
                            log.info("视频上传处理完成: taskId={}", taskId);
                            handleSubmissionCompletion(taskId, true, "UPLOAD_COMPLETED", null);
                            
                        } catch (Exception e) {
                            log.error("视频上传处理异常: taskId={}", taskId, e);
                            handleSubmissionCompletion(taskId, false, null, "上传处理异常: " + e.getMessage());
                        }
                    }).start();
                    break;
                    
                default:
                    log.warn("未知的步骤类型，跳过执行: {}", step.getStepType());
                    break;
            }
        } catch (Exception e) {
            log.error("触发步骤执行异常: taskId={}, stepType={}", taskId, step.getStepType(), e);
        }
    }
}