package com.tbw.cut.workflow.service.impl;

import com.tbw.cut.workflow.model.*;
import com.tbw.cut.workflow.service.TaskOrchestrator;
import com.tbw.cut.workflow.service.ProcessingPipeline;
import com.tbw.cut.service.VideoProcessService;
import com.tbw.cut.service.SubmissionTaskService;
import com.tbw.cut.entity.SubmissionTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务编排器实现
 */
@Slf4j
@Service
public class TaskOrchestratorImpl implements TaskOrchestrator {
    
    @Autowired
    private ProcessingPipeline processingPipeline;
    
    @Autowired
    private VideoProcessService videoProcessService;
    
    @Autowired
    private SubmissionTaskService submissionTaskService;
    
    @Override
    public void orchestrateTask(WorkflowInstance instance) throws Exception {
        log.info("开始编排工作流任务: {}, 步骤数: {}", 
                instance.getInstanceId(), instance.getSteps().size());
        
        // 启动工作流
        if (instance.getStatus() == WorkflowStatus.PENDING) {
            instance.start();
        }
        
        // 按顺序执行每个步骤
        for (WorkflowStep step : instance.getSteps()) {
            // 检查工作流是否被取消或暂停
            if (instance.getStatus() == WorkflowStatus.CANCELLED) {
                log.info("工作流已取消，停止执行: {}", instance.getInstanceId());
                return;
            }
            
            if (instance.getStatus() == WorkflowStatus.PAUSED) {
                log.info("工作流已暂停，停止执行: {}", instance.getInstanceId());
                return;
            }
            
            // 检查步骤是否可以执行
            if (!canExecuteStep(instance, step)) {
                log.info("跳过步骤: {}, 类型: {}", step.getStepId(), step.getType());
                step.markSkipped("条件不满足，跳过执行");
                continue;
            }
            
            // 检查处理步骤的前置条件
            if (isProcessingStep(step.getType()) && !processingPipeline.checkPrerequisites(instance, step.getType())) {
                log.warn("步骤前置条件不满足: {}, 类型: {}", step.getStepId(), step.getType());
                step.markSkipped("前置条件不满足，跳过执行");
                continue;
            }
            
            try {
                log.info("执行步骤: {}, 类型: {}", step.getStepId(), step.getType());
                
                // 执行步骤
                StepResult result = executeStep(instance, step);
                
                if (result.isSuccess()) {
                    step.markCompleted(result.getOutputPath());
                    log.info("步骤执行成功: {}, 输出: {}", step.getStepId(), result.getOutputPath());
                    
                    // 对于处理步骤，执行清理操作
                    if (isProcessingStep(step.getType())) {
                        processingPipeline.cleanupTempFiles(instance, step.getType());
                    }
                } else {
                    step.markFailed(result.getErrorMessage());
                    handleStepFailure(instance, step, new RuntimeException(result.getErrorMessage()));
                    throw new RuntimeException("步骤执行失败: " + result.getErrorMessage());
                }
                
            } catch (Exception e) {
                log.error("步骤执行异常: {}", step.getStepId(), e);
                step.markFailed(e.getMessage());
                handleStepFailure(instance, step, e);
                throw e;
            }
        }
        
        log.info("工作流任务编排完成: {}", instance.getInstanceId());
    }
    
    @Override
    public StepResult executeStep(WorkflowInstance instance, WorkflowStep step) throws Exception {
        log.debug("执行步骤: {}, 类型: {}", step.getStepId(), step.getType());
        
        // 标记步骤开始
        step.markStarted();
        
        try {
            // 使用ProcessingPipeline处理视频处理步骤
            if (isProcessingStep(step.getType())) {
                return executeProcessingStep(instance, step);
            } else {
                // 非处理步骤（如投稿）使用原有逻辑
                switch (step.getType()) {
                    case SUBMISSION:
                        return executeSubmissionStep(instance, step);
                    default:
                        return StepResult.failure("未知的步骤类型: " + step.getType());
                }
            }
        } catch (Exception e) {
            log.error("步骤执行异常: {}, 类型: {}", step.getStepId(), step.getType(), e);
            throw e;
        }
    }
    
    @Override
    public void handleStepFailure(WorkflowInstance instance, WorkflowStep step, Exception error) {
        log.error("处理步骤失败: {}, 类型: {}, 错误: {}", 
                step.getStepId(), step.getType(), error.getMessage());
        
        // 记录错误信息
        step.markFailed(error.getMessage());
        
        // 标记工作流失败
        instance.fail("步骤执行失败: " + step.getType().getDescription() + " - " + error.getMessage());
    }
    
    @Override
    public boolean canExecuteStep(WorkflowInstance instance, WorkflowStep step) {
        // 检查工作流状态
        if (instance.getStatus() != WorkflowStatus.RUNNING) {
            return false;
        }
        
        // 检查步骤状态
        if (step.getStatus() != StepStatus.PENDING) {
            return false;
        }
        
        // 根据步骤类型检查特定条件
        switch (step.getType()) {
            case CLIPPING:
                return canExecuteClipping(instance);
            case MERGING:
                return canExecuteMerging(instance);
            case SEGMENTATION:
                return canExecuteSegmentation(instance);
            case SUBMISSION:
                return canExecuteSubmission(instance);
            default:
                return false;
        }
    }
    
    /**
     * 判断是否为处理步骤
     */
    private boolean isProcessingStep(StepType stepType) {
        return stepType == StepType.CLIPPING || 
               stepType == StepType.MERGING || 
               stepType == StepType.SEGMENTATION;
    }
    
    /**
     * 使用ProcessingPipeline执行处理步骤
     */
    private StepResult executeProcessingStep(WorkflowInstance instance, WorkflowStep step) throws Exception {
        log.info("使用ProcessingPipeline执行处理步骤: {}", step.getStepId());
        
        try {
            // 使用ProcessingPipeline执行处理步骤
            ProcessingResult result = processingPipeline.executeProcessingStep(instance, step.getType());
            
            // 验证处理结果
            if (result.isSuccess() && !processingPipeline.validateProcessingResult(result, step.getType())) {
                return StepResult.failure("处理结果验证失败: " + step.getType().getDescription());
            }
            
            if (result.isSuccess()) {
                // 根据结果类型返回适当的输出路径
                String outputPath = result.getPrimaryOutputPath();
                if (outputPath == null && result.getOutputPaths() != null && !result.getOutputPaths().isEmpty()) {
                    // 对于多文件输出，使用分号分隔的路径列表
                    outputPath = String.join(";", result.getOutputPaths());
                }
                
                return StepResult.success(outputPath);
            } else {
                return StepResult.failure(result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("ProcessingPipeline执行异常: {}", step.getStepId(), e);
            return StepResult.failure("处理步骤异常: " + e.getMessage());
        }
    }
    
    /**
     * 执行剪辑步骤
     * @deprecated 使用ProcessingPipeline替代
     */
    @Deprecated
    private StepResult executeClippingStep(WorkflowInstance instance, WorkflowStep step) {
        log.info("执行剪辑步骤: {}", step.getStepId());
        
        try {
            // 调用现有的视频剪辑服务
            List<String> clipPaths = videoProcessService.clipVideos(instance.getTaskId());
            
            if (clipPaths == null || clipPaths.isEmpty()) {
                return StepResult.failure("剪辑步骤失败：没有生成剪辑文件");
            }
            
            log.info("剪辑步骤完成，生成 {} 个剪辑文件", clipPaths.size());
            
            // 返回剪辑文件路径列表的字符串表示
            String outputPath = String.join(";", clipPaths);
            return StepResult.success(outputPath);
            
        } catch (Exception e) {
            log.error("剪辑步骤执行异常: {}", step.getStepId(), e);
            return StepResult.failure("剪辑步骤异常: " + e.getMessage());
        }
    }
    
    /**
     * 执行合并步骤
     * @deprecated 使用ProcessingPipeline替代
     */
    @Deprecated
    private StepResult executeMergingStep(WorkflowInstance instance, WorkflowStep step) {
        log.info("执行合并步骤: {}", step.getStepId());
        
        try {
            // 调用现有的视频合并服务
            String mergedVideoPath = videoProcessService.mergeVideos(instance.getTaskId());
            
            if (mergedVideoPath == null || mergedVideoPath.isEmpty()) {
                return StepResult.failure("合并步骤失败：没有生成合并视频");
            }
            
            log.info("合并步骤完成，生成合并视频: {}", mergedVideoPath);
            return StepResult.success(mergedVideoPath);
            
        } catch (Exception e) {
            log.error("合并步骤执行异常: {}", step.getStepId(), e);
            return StepResult.failure("合并步骤异常: " + e.getMessage());
        }
    }
    
    /**
     * 执行分段步骤
     * @deprecated 使用ProcessingPipeline替代
     */
    @Deprecated
    private StepResult executeSegmentationStep(WorkflowInstance instance, WorkflowStep step) {
        log.info("执行分段步骤: {}", step.getStepId());
        
        try {
            // 调用现有的视频分段服务
            List<String> segmentPaths = videoProcessService.segmentVideo(instance.getTaskId());
            
            if (segmentPaths == null || segmentPaths.isEmpty()) {
                return StepResult.failure("分段步骤失败：没有生成分段文件");
            }
            
            // 保存分段信息到数据库
            videoProcessService.saveOutputSegments(instance.getTaskId(), segmentPaths);
            
            log.info("分段步骤完成，生成 {} 个分段文件", segmentPaths.size());
            
            // 返回分段文件路径列表的字符串表示
            String outputPath = String.join(";", segmentPaths);
            return StepResult.success(outputPath);
            
        } catch (Exception e) {
            log.error("分段步骤执行异常: {}", step.getStepId(), e);
            return StepResult.failure("分段步骤异常: " + e.getMessage());
        }
    }
    
    /**
     * 执行投稿步骤
     */
    private StepResult executeSubmissionStep(WorkflowInstance instance, WorkflowStep step) {
        log.info("执行投稿步骤: {}", step.getStepId());
        
        try {
            // 更新任务状态为上传中
            submissionTaskService.updateTaskStatus(instance.getTaskId(), SubmissionTask.TaskStatus.UPLOADING);
            
            // 投稿步骤的具体实现将由现有的投稿队列系统处理
            // 这里只是标记任务已准备好进行投稿
            log.info("投稿步骤准备完成，任务已加入投稿队列: {}", instance.getTaskId());
            
            // 返回任务ID作为投稿结果的标识
            return StepResult.success("QUEUED_FOR_SUBMISSION:" + instance.getTaskId());
            
        } catch (Exception e) {
            log.error("投稿步骤执行异常: {}", step.getStepId(), e);
            return StepResult.failure("投稿步骤异常: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否可以执行剪辑
     */
    private boolean canExecuteClipping(WorkflowInstance instance) {
        WorkflowConfig config = instance.getConfig();
        if (!config.isEnableClipping()) {
            return false;
        }
        
        // 检查是否有源视频数据
        try {
            List<com.tbw.cut.entity.TaskSourceVideo> sourceVideos = 
                submissionTaskService.getSourceVideosByTaskId(instance.getTaskId());
            return sourceVideos != null && !sourceVideos.isEmpty();
        } catch (Exception e) {
            log.error("检查剪辑条件时发生异常: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查是否可以执行合并
     */
    private boolean canExecuteMerging(WorkflowInstance instance) {
        WorkflowConfig config = instance.getConfig();
        if (!config.isEnableMerging()) {
            return false;
        }
        
        // 如果启用了剪辑，需要等剪辑完成
        if (config.isEnableClipping()) {
            // 检查剪辑步骤是否已完成
            boolean clippingCompleted = instance.getSteps().stream()
                    .filter(step -> step.getType() == StepType.CLIPPING)
                    .anyMatch(step -> step.getStatus() == StepStatus.COMPLETED);
            if (!clippingCompleted) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查是否可以执行分段
     */
    private boolean canExecuteSegmentation(WorkflowInstance instance) {
        WorkflowConfig config = instance.getConfig();
        
        // 如果启用了直接投稿，则跳过分段
        if (config.isEnableDirectSubmission()) {
            return false;
        }
        
        // 检查分段配置
        if (config.getSegmentationConfig() == null || !config.getSegmentationConfig().isEnabled()) {
            return false;
        }
        
        // 如果启用了合并，需要等合并完成
        if (config.isEnableMerging()) {
            boolean mergingCompleted = instance.getSteps().stream()
                    .filter(step -> step.getType() == StepType.MERGING)
                    .anyMatch(step -> step.getStatus() == StepStatus.COMPLETED);
            if (!mergingCompleted) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查是否可以执行投稿
     */
    private boolean canExecuteSubmission(WorkflowInstance instance) {
        // 投稿步骤总是可以执行（作为最后一步）
        // 但需要确保前面的处理步骤都已完成
        WorkflowConfig config = instance.getConfig();
        
        // 检查所有前置步骤是否完成
        for (WorkflowStep step : instance.getSteps()) {
            if (step.getType() != StepType.SUBMISSION && 
                step.getStatus() != StepStatus.COMPLETED && 
                step.getStatus() != StepStatus.SKIPPED) {
                return false;
            }
        }
        
        return true;
    }
}