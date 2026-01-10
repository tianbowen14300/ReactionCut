package com.tbw.cut.workflow.service.impl;

import com.tbw.cut.workflow.model.ProcessingResult;
import com.tbw.cut.workflow.model.StepType;
import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.service.ProcessingPipeline;
import com.tbw.cut.service.VideoProcessService;
import com.tbw.cut.entity.TaskSourceVideo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频处理管道实现
 * 封装现有的VideoProcessService，提供统一的处理接口
 */
@Slf4j
@Service
public class ProcessingPipelineImpl implements ProcessingPipeline {
    
    @Autowired
    private VideoProcessService videoProcessService;
    
    @Override
    public ProcessingResult executeProcessingStep(WorkflowInstance instance, StepType stepType) throws Exception {
        log.info("执行处理步骤: {}, 任务ID: {}", stepType.getDescription(), instance.getTaskId());
        
        LocalDateTime startTime = LocalDateTime.now();
        ProcessingResult result;
        
        try {
            switch (stepType) {
                case CLIPPING:
                    result = executeClipping(instance);
                    break;
                case MERGING:
                    result = executeMerging(instance);
                    break;
                case SEGMENTATION:
                    result = executeSegmentation(instance);
                    break;
                default:
                    result = ProcessingResult.failure("不支持的处理步骤类型: " + stepType);
            }
            
            // 设置处理时间
            result.withStartTime(startTime);
            
            log.info("处理步骤完成: {}, 任务ID: {}, 成功: {}, 耗时: {}ms", 
                    stepType.getDescription(), instance.getTaskId(), result.isSuccess(), result.getDurationMs());
            
            return result;
            
        } catch (Exception e) {
            log.error("处理步骤异常: {}, 任务ID: {}", stepType.getDescription(), instance.getTaskId(), e);
            ProcessingResult errorResult = ProcessingResult.failure("处理步骤异常: " + e.getMessage());
            errorResult.withStartTime(startTime);
            return errorResult;
        }
    }
    
    @Override
    public boolean validateProcessingResult(ProcessingResult result, StepType stepType) {
        if (!result.isSuccess()) {
            log.warn("处理结果验证失败: 步骤未成功完成, 类型: {}", stepType.getDescription());
            return false;
        }
        
        switch (stepType) {
            case CLIPPING:
                return validateClippingResult(result);
            case MERGING:
                return validateMergingResult(result);
            case SEGMENTATION:
                return validateSegmentationResult(result);
            default:
                log.warn("未知的步骤类型，跳过验证: {}", stepType);
                return true;
        }
    }
    
    @Override
    public boolean checkPrerequisites(WorkflowInstance instance, StepType stepType) {
        log.debug("检查处理步骤前置条件: {}, 任务ID: {}", stepType.getDescription(), instance.getTaskId());
        
        try {
            switch (stepType) {
                case CLIPPING:
                    return checkClippingPrerequisites(instance);
                case MERGING:
                    return checkMergingPrerequisites(instance);
                case SEGMENTATION:
                    return checkSegmentationPrerequisites(instance);
                default:
                    log.warn("未知的步骤类型，跳过前置条件检查: {}", stepType);
                    return true;
            }
        } catch (Exception e) {
            log.error("检查前置条件时发生异常: {}, 任务ID: {}", stepType.getDescription(), instance.getTaskId(), e);
            return false;
        }
    }
    
    @Override
    public void cleanupTempFiles(WorkflowInstance instance, StepType stepType) {
        log.info("清理临时文件: {}, 任务ID: {}", stepType.getDescription(), instance.getTaskId());
        
        try {
            // 根据步骤类型清理相应的临时文件
            switch (stepType) {
                case CLIPPING:
                    cleanupClippingTempFiles(instance);
                    break;
                case MERGING:
                    cleanupMergingTempFiles(instance);
                    break;
                case SEGMENTATION:
                    cleanupSegmentationTempFiles(instance);
                    break;
                default:
                    log.debug("无需清理临时文件: {}", stepType);
            }
        } catch (Exception e) {
            log.error("清理临时文件时发生异常: {}, 任务ID: {}", stepType.getDescription(), instance.getTaskId(), e);
        }
    }
    
    /**
     * 执行视频剪辑
     */
    private ProcessingResult executeClipping(WorkflowInstance instance) {
        try {
            List<String> clipPaths = videoProcessService.clipVideos(instance.getTaskId());
            
            if (clipPaths == null || clipPaths.isEmpty()) {
                return ProcessingResult.failure("剪辑失败：没有生成剪辑文件");
            }
            
            return ProcessingResult.success(clipPaths)
                    .withMetadata("clipCount", clipPaths.size())
                    .withMetadata("stepType", "CLIPPING");
                    
        } catch (Exception e) {
            return ProcessingResult.failure("剪辑异常: " + e.getMessage());
        }
    }
    
    /**
     * 执行视频合并
     */
    private ProcessingResult executeMerging(WorkflowInstance instance) {
        try {
            String mergedVideoPath = videoProcessService.mergeVideos(instance.getTaskId());
            
            if (mergedVideoPath == null || mergedVideoPath.isEmpty()) {
                return ProcessingResult.failure("合并失败：没有生成合并视频");
            }
            
            return ProcessingResult.success(mergedVideoPath)
                    .withMetadata("stepType", "MERGING");
                    
        } catch (Exception e) {
            return ProcessingResult.failure("合并异常: " + e.getMessage());
        }
    }
    
    /**
     * 执行视频分段
     */
    private ProcessingResult executeSegmentation(WorkflowInstance instance) {
        try {
            List<String> segmentPaths = videoProcessService.segmentVideo(instance.getTaskId());
            
            if (segmentPaths == null || segmentPaths.isEmpty()) {
                return ProcessingResult.failure("分段失败：没有生成分段文件");
            }
            
            // 保存分段信息到数据库
            videoProcessService.saveOutputSegments(instance.getTaskId(), segmentPaths);
            
            return ProcessingResult.success(segmentPaths)
                    .withMetadata("segmentCount", segmentPaths.size())
                    .withMetadata("stepType", "SEGMENTATION");
                    
        } catch (Exception e) {
            return ProcessingResult.failure("分段异常: " + e.getMessage());
        }
    }
    
    /**
     * 验证剪辑结果
     */
    private boolean validateClippingResult(ProcessingResult result) {
        if (result.getOutputPaths() == null || result.getOutputPaths().isEmpty()) {
            log.warn("剪辑结果验证失败: 没有输出文件");
            return false;
        }
        
        // 检查所有剪辑文件是否存在
        for (String clipPath : result.getOutputPaths()) {
            File clipFile = new File(clipPath);
            if (!clipFile.exists() || clipFile.length() == 0) {
                log.warn("剪辑文件不存在或为空: {}", clipPath);
                return false;
            }
        }
        
        log.debug("剪辑结果验证通过: {} 个文件", result.getOutputPaths().size());
        return true;
    }
    
    /**
     * 验证合并结果
     */
    private boolean validateMergingResult(ProcessingResult result) {
        if (result.getPrimaryOutputPath() == null || result.getPrimaryOutputPath().isEmpty()) {
            log.warn("合并结果验证失败: 没有输出文件");
            return false;
        }
        
        File mergedFile = new File(result.getPrimaryOutputPath());
        if (!mergedFile.exists() || mergedFile.length() == 0) {
            log.warn("合并文件不存在或为空: {}", result.getPrimaryOutputPath());
            return false;
        }
        
        log.debug("合并结果验证通过: {}", result.getPrimaryOutputPath());
        return true;
    }
    
    /**
     * 验证分段结果
     */
    private boolean validateSegmentationResult(ProcessingResult result) {
        if (result.getOutputPaths() == null || result.getOutputPaths().isEmpty()) {
            log.warn("分段结果验证失败: 没有输出文件");
            return false;
        }
        
        // 检查所有分段文件是否存在
        for (String segmentPath : result.getOutputPaths()) {
            File segmentFile = new File(segmentPath);
            if (!segmentFile.exists() || segmentFile.length() == 0) {
                log.warn("分段文件不存在或为空: {}", segmentPath);
                return false;
            }
        }
        
        log.debug("分段结果验证通过: {} 个文件", result.getOutputPaths().size());
        return true;
    }
    
    /**
     * 检查剪辑前置条件
     */
    private boolean checkClippingPrerequisites(WorkflowInstance instance) {
        // 检查是否有源视频数据
        List<TaskSourceVideo> sourceVideos = videoProcessService.getSourceVideos(instance.getTaskId());
        if (sourceVideos == null || sourceVideos.isEmpty()) {
            log.warn("剪辑前置条件检查失败: 没有源视频数据, 任务ID: {}", instance.getTaskId());
            return false;
        }
        
        // 检查源视频文件是否存在
        for (TaskSourceVideo sourceVideo : sourceVideos) {
            String filePath = sourceVideo.getSourceFilePath();
            if (filePath == null || filePath.isEmpty()) {
                log.warn("剪辑前置条件检查失败: 源视频路径为空, 任务ID: {}", instance.getTaskId());
                return false;
            }
            
            File sourceFile = new File(filePath);
            if (!sourceFile.exists()) {
                log.warn("剪辑前置条件检查失败: 源视频文件不存在: {}, 任务ID: {}", filePath, instance.getTaskId());
                return false;
            }
        }
        
        log.debug("剪辑前置条件检查通过: {} 个源视频, 任务ID: {}", sourceVideos.size(), instance.getTaskId());
        return true;
    }
    
    /**
     * 检查合并前置条件
     */
    private boolean checkMergingPrerequisites(WorkflowInstance instance) {
        // 检查是否有剪辑文件（如果启用了剪辑）
        // 这里简化处理，实际应该检查工作流配置
        try {
            // 尝试获取任务详情来判断是否需要合并
            if (videoProcessService.getTaskDetail(instance.getTaskId()) == null) {
                log.warn("合并前置条件检查失败: 任务不存在, 任务ID: {}", instance.getTaskId());
                return false;
            }
            
            log.debug("合并前置条件检查通过, 任务ID: {}", instance.getTaskId());
            return true;
        } catch (Exception e) {
            log.error("合并前置条件检查异常, 任务ID: {}", instance.getTaskId(), e);
            return false;
        }
    }
    
    /**
     * 检查分段前置条件
     */
    private boolean checkSegmentationPrerequisites(WorkflowInstance instance) {
        // 检查是否有合并后的视频
        String mergedVideoPath = videoProcessService.getMergedVideoPathFromDatabase(instance.getTaskId());
        if (mergedVideoPath == null || mergedVideoPath.isEmpty()) {
            log.warn("分段前置条件检查失败: 没有合并视频, 任务ID: {}", instance.getTaskId());
            return false;
        }
        
        File mergedFile = new File(mergedVideoPath);
        if (!mergedFile.exists()) {
            log.warn("分段前置条件检查失败: 合并视频文件不存在: {}, 任务ID: {}", mergedVideoPath, instance.getTaskId());
            return false;
        }
        
        log.debug("分段前置条件检查通过: {}, 任务ID: {}", mergedVideoPath, instance.getTaskId());
        return true;
    }
    
    /**
     * 清理剪辑临时文件
     */
    private void cleanupClippingTempFiles(WorkflowInstance instance) {
        // 剪辑步骤通常不产生需要清理的临时文件
        // 剪辑文件本身是需要保留的中间结果
        log.debug("剪辑步骤无需清理临时文件, 任务ID: {}", instance.getTaskId());
    }
    
    /**
     * 清理合并临时文件
     */
    private void cleanupMergingTempFiles(WorkflowInstance instance) {
        // 清理合并过程中产生的临时文件，如concat文件列表
        // 具体的清理逻辑由VideoProcessService内部处理
        log.debug("合并步骤临时文件由VideoProcessService内部清理, 任务ID: {}", instance.getTaskId());
    }
    
    /**
     * 清理分段临时文件
     */
    private void cleanupSegmentationTempFiles(WorkflowInstance instance) {
        // 分段步骤通常不产生需要清理的临时文件
        // 分段文件本身是最终输出结果
        log.debug("分段步骤无需清理临时文件, 任务ID: {}", instance.getTaskId());
    }
}