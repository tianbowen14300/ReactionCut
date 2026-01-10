package com.tbw.cut.workflow.service;

import com.tbw.cut.workflow.model.ProcessingResult;
import com.tbw.cut.workflow.model.StepType;
import com.tbw.cut.workflow.model.WorkflowInstance;

/**
 * 视频处理管道接口
 * 提供统一的视频处理接口，封装现有的VideoProcessService
 */
public interface ProcessingPipeline {
    
    /**
     * 执行视频处理步骤
     * 
     * @param instance 工作流实例
     * @param stepType 处理步骤类型
     * @return 处理结果
     * @throws Exception 处理过程中的异常
     */
    ProcessingResult executeProcessingStep(WorkflowInstance instance, StepType stepType) throws Exception;
    
    /**
     * 验证处理结果
     * 
     * @param result 处理结果
     * @param stepType 处理步骤类型
     * @return 验证是否通过
     */
    boolean validateProcessingResult(ProcessingResult result, StepType stepType);
    
    /**
     * 检查处理步骤的前置条件
     * 
     * @param instance 工作流实例
     * @param stepType 处理步骤类型
     * @return 前置条件是否满足
     */
    boolean checkPrerequisites(WorkflowInstance instance, StepType stepType);
    
    /**
     * 清理处理过程中的临时文件
     * 
     * @param instance 工作流实例
     * @param stepType 处理步骤类型
     */
    void cleanupTempFiles(WorkflowInstance instance, StepType stepType);
}