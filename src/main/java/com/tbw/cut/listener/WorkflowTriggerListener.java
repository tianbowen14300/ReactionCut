package com.tbw.cut.listener;

import com.tbw.cut.event.DownloadCompletionEvent;
import com.tbw.cut.service.IntegrationService;
import com.tbw.cut.service.WorkflowConfigurationService;
import com.tbw.cut.workflow.model.WorkflowConfig;
import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.service.WorkflowEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * 工作流触发监听器
 * 
 * 监听下载完成事件并启动相应的工作流
 */
@Component
@Slf4j
public class WorkflowTriggerListener {
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    @Autowired
    private IntegrationService integrationService;
    
    @Autowired
    private WorkflowConfigurationService workflowConfigurationService;
    
    /**
     * 监听下载完成事件并启动工作流
     * 
     * @param event 下载完成事件
     */
    @EventListener
    @Async("workflowEventExecutor")
    public void handleDownloadCompletionEvent(DownloadCompletionEvent event) {
        if (event == null) {
            log.error("接收到空的下载完成事件");
            return;
        }
        
        log.info("接收到下载完成事件: downloadTaskId={}, submissionTaskId={}, fileCount={}", 
                event.getDownloadTaskId(), event.getSubmissionTaskId(), event.getFileCount());
        
        try {
            // 验证事件数据
            if (!validateEvent(event)) {
                return;
            }
            
            // 验证文件存在性
            if (!validateFilesExist(event.getCompletedFilePaths())) {
                log.error("下载完成事件中的文件验证失败: downloadTaskId={}, submissionTaskId={}", 
                        event.getDownloadTaskId(), event.getSubmissionTaskId());
                markWorkflowStartupFailed(event, "文件验证失败：部分文件不存在或大小为0");
                return;
            }
            
            // 获取工作流配置
            WorkflowConfig config = getWorkflowConfig(event.getSubmissionTaskId());
            if (config == null) {
                log.error("未找到工作流配置: submissionTaskId={}", event.getSubmissionTaskId());
                markWorkflowStartupFailed(event, "未找到工作流配置");
                return;
            }
            
            // 启动工作流
            WorkflowInstance instance = startWorkflow(event, config);
            if (instance != null) {
                // 更新集成任务状态
                updateWorkflowStatus(event, instance.getInstanceId(), "WORKFLOW_STARTED");
                log.info("成功启动工作流: instanceId={}, submissionTaskId={}", 
                        instance.getInstanceId(), event.getSubmissionTaskId());
            }
            
        } catch (Exception e) {
            log.error("处理下载完成事件失败: downloadTaskId={}, submissionTaskId={}", 
                    event.getDownloadTaskId(), event.getSubmissionTaskId(), e);
            
            // 标记为失败状态，支持后续重试
            markWorkflowStartupFailed(event, "处理事件异常: " + e.getMessage());
        }
    }
    
    /**
     * 验证事件数据
     * 
     * @param event 下载完成事件
     * @return true 如果事件数据有效，false 否则
     */
    private boolean validateEvent(DownloadCompletionEvent event) {
        if (!event.isValid()) {
            log.error("下载完成事件数据无效: {}", event);
            return false;
        }
        
        // 额外的业务验证
        if (event.getFileCount() == 0) {
            log.error("下载完成事件中没有文件: downloadTaskId={}, submissionTaskId={}", 
                    event.getDownloadTaskId(), event.getSubmissionTaskId());
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证文件是否存在
     * 
     * @param filePaths 文件路径列表
     * @return true 如果所有文件都存在且大小大于0，false 否则
     */
    private boolean validateFilesExist(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            log.warn("文件路径列表为空");
            return false;
        }
        
        int validFileCount = 0;
        int totalFileCount = filePaths.size();
        
        for (String filePath : filePaths) {
            if (filePath == null || filePath.trim().isEmpty()) {
                log.warn("发现空的文件路径");
                continue;
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("文件不存在: {}", filePath);
                continue;
            }
            
            if (file.length() == 0) {
                log.warn("文件大小为0: {}", filePath);
                continue;
            }
            
            validFileCount++;
            log.debug("文件验证通过: {} (大小: {} bytes)", filePath, file.length());
        }
        
        boolean allValid = validFileCount == totalFileCount;
        log.info("文件验证结果: {}/{} 个文件有效", validFileCount, totalFileCount);
        
        return allValid;
    }
    
    /**
     * 获取工作流配置
     * 
     * @param submissionTaskId 投稿任务ID
     * @return 工作流配置，如果不存在则返回null
     */
    private WorkflowConfig getWorkflowConfig(String submissionTaskId) {
        try {
            WorkflowConfig config = workflowConfigurationService.getConfigForTask(submissionTaskId);
            if (config == null) {
                log.warn("未找到工作流配置: submissionTaskId={}", submissionTaskId);
                return null;
            }
            
            // 验证配置有效性
            if (!config.isValid()) {
                log.error("工作流配置无效: submissionTaskId={}, error={}", 
                        submissionTaskId, config.getValidationError());
                return null;
            }
            
            log.debug("获取到工作流配置: submissionTaskId={}, config={}", submissionTaskId, config);
            return config;
            
        } catch (Exception e) {
            log.error("获取工作流配置时发生异常: submissionTaskId={}", submissionTaskId, e);
            return null;
        }
    }
    
    /**
     * 启动工作流
     * 
     * @param event 下载完成事件
     * @param config 工作流配置
     * @return 工作流实例，如果启动失败则返回null
     */
    private WorkflowInstance startWorkflow(DownloadCompletionEvent event, WorkflowConfig config) {
        try {
            log.info("开始启动工作流: submissionTaskId={}", event.getSubmissionTaskId());
            
            // 启动工作流
            WorkflowInstance instance = workflowEngine.startWorkflow(event.getSubmissionTaskId(), config);
            
            if (instance != null) {
                log.info("工作流启动成功: instanceId={}, submissionTaskId={}, status={}", 
                        instance.getInstanceId(), event.getSubmissionTaskId(), instance.getStatus());
                return instance;
            } else {
                log.error("工作流启动失败: 返回的实例为null, submissionTaskId={}", event.getSubmissionTaskId());
                return null;
            }
            
        } catch (Exception e) {
            log.error("启动工作流时发生异常: submissionTaskId={}, error={}", 
                    event.getSubmissionTaskId(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 更新工作流状态
     * 
     * @param event 下载完成事件
     * @param workflowInstanceId 工作流实例ID
     * @param status 状态
     */
    private void updateWorkflowStatus(DownloadCompletionEvent event, String workflowInstanceId, String status) {
        try {
            integrationService.updateWorkflowStatus(
                    event.getDownloadTaskId(), 
                    event.getSubmissionTaskId(), 
                    workflowInstanceId, 
                    status
            );
            
            log.debug("工作流状态已更新: downloadTaskId={}, submissionTaskId={}, instanceId={}, status={}", 
                    event.getDownloadTaskId(), event.getSubmissionTaskId(), workflowInstanceId, status);
            
        } catch (Exception e) {
            log.error("更新工作流状态失败: downloadTaskId={}, submissionTaskId={}, instanceId={}, status={}", 
                    event.getDownloadTaskId(), event.getSubmissionTaskId(), workflowInstanceId, status, e);
        }
    }
    
    /**
     * 标记工作流启动失败
     * 
     * @param event 下载完成事件
     * @param errorMessage 错误信息
     */
    private void markWorkflowStartupFailed(DownloadCompletionEvent event, String errorMessage) {
        try {
            integrationService.markWorkflowStartupFailed(
                    event.getDownloadTaskId(), 
                    event.getSubmissionTaskId(), 
                    errorMessage
            );
            
            log.warn("已标记工作流启动失败: downloadTaskId={}, submissionTaskId={}, error={}", 
                    event.getDownloadTaskId(), event.getSubmissionTaskId(), errorMessage);
            
        } catch (Exception e) {
            log.error("标记工作流启动失败时发生异常: downloadTaskId={}, submissionTaskId={}", 
                    event.getDownloadTaskId(), event.getSubmissionTaskId(), e);
        }
    }
    
    /**
     * 检查是否应该处理此事件
     * 
     * @param event 下载完成事件
     * @return true 如果应该处理，false 否则
     */
    private boolean shouldProcessEvent(DownloadCompletionEvent event) {
        // 可以在这里添加额外的业务逻辑来决定是否处理事件
        // 例如：检查系统负载、检查工作流引擎状态等
        
        try {
            // 检查工作流引擎是否可用
            if (workflowEngine == null) {
                log.error("工作流引擎不可用");
                return false;
            }
            
            // 检查是否已经存在该任务的工作流
            WorkflowInstance existingWorkflow = workflowEngine.getWorkflowByTaskId(event.getSubmissionTaskId());
            if (existingWorkflow != null && !existingWorkflow.isCompleted()) {
                log.warn("任务已存在未完成的工作流，跳过处理: submissionTaskId={}, existingInstanceId={}", 
                        event.getSubmissionTaskId(), existingWorkflow.getInstanceId());
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("检查事件处理条件时发生异常: submissionTaskId={}", event.getSubmissionTaskId(), e);
            return false;
        }
    }
}