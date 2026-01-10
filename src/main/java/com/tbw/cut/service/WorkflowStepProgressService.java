package com.tbw.cut.service;

import com.tbw.cut.workflow.model.StepStatus;
import com.tbw.cut.workflow.model.StepType;

/**
 * 工作流步骤进度服务接口
 * 
 * 负责管理工作流步骤的状态转换和进度更新
 */
public interface WorkflowStepProgressService {
    
    /**
     * 处理下载完成事件，更新相应的工作流步骤状态
     * 
     * @param downloadTaskId 下载任务ID
     * @param downloadStatus 下载状态
     * @return 是否成功更新工作流步骤
     */
    boolean handleDownloadCompletion(Long downloadTaskId, Integer downloadStatus);
    
    /**
     * 处理视频处理完成事件，更新相应的工作流步骤状态
     * 
     * @param taskId 任务ID
     * @param stepType 步骤类型
     * @param success 是否成功
     * @param outputPath 输出路径
     * @param errorMessage 错误信息（如果失败）
     * @return 是否成功更新工作流步骤
     */
    boolean handleProcessingCompletion(String taskId, StepType stepType, boolean success, 
                                     String outputPath, String errorMessage);
    
    /**
     * 处理投稿完成事件，更新相应的工作流步骤状态
     * 
     * @param taskId 任务ID
     * @param success 是否成功
     * @param submissionResult 投稿结果
     * @param errorMessage 错误信息（如果失败）
     * @return 是否成功更新工作流步骤
     */
    boolean handleSubmissionCompletion(String taskId, boolean success, 
                                     String submissionResult, String errorMessage);
    
    /**
     * 更新工作流步骤进度
     * 
     * @param taskId 任务ID
     * @param stepType 步骤类型
     * @param progress 进度（0.0-1.0）
     * @return 是否成功更新进度
     */
    boolean updateStepProgress(String taskId, StepType stepType, double progress);
    
    /**
     * 启动下一个工作流步骤
     * 
     * @param taskId 任务ID
     * @return 是否成功启动下一个步骤
     */
    boolean startNextWorkflowStep(String taskId);
    
    /**
     * 检查并启动可执行的工作流步骤
     * 
     * @param taskId 任务ID
     * @return 启动的步骤数量
     */
    int checkAndStartPendingSteps(String taskId);
    
    /**
     * 获取任务的当前执行步骤
     * 
     * @param taskId 任务ID
     * @return 当前执行的步骤类型，如果没有则返回null
     */
    StepType getCurrentExecutingStep(String taskId);
    
    /**
     * 检查任务是否有活跃的工作流
     * 
     * @param taskId 任务ID
     * @return 是否有活跃的工作流
     */
    boolean hasActiveWorkflow(String taskId);
}