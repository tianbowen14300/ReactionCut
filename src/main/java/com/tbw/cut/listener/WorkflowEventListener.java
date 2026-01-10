package com.tbw.cut.listener;

import com.tbw.cut.event.DownloadStatusChangeEvent;
import com.tbw.cut.service.WorkflowStepProgressService;
import com.tbw.cut.service.TaskRelationService;
import com.tbw.cut.entity.TaskRelation;
import com.tbw.cut.workflow.model.StepType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 工作流事件监听器
 * 
 * 监听各种业务事件，触发相应的工作流步骤处理
 */
@Slf4j
@Component
public class WorkflowEventListener {
    
    @Autowired
    private WorkflowStepProgressService workflowStepProgressService;
    
    @Autowired
    private TaskRelationService taskRelationService;
    
    /**
     * 监听下载状态变化事件，触发工作流步骤更新
     */
    @EventListener
    @Async
    public void handleDownloadStatusChangeForWorkflow(DownloadStatusChangeEvent event) {
        if (!event.isStatusChanged()) {
            return;
        }
        
        log.info("处理下载状态变化的工作流事件: taskId={}, oldStatus={}, newStatus={}", 
                event.getTaskId(), event.getOldStatus(), event.getNewStatus());
        
        try {
            // 检查是否为下载完成或失败事件
            if (event.isCompletedEvent() || event.isFailedEvent()) {
                boolean updated = workflowStepProgressService.handleDownloadCompletion(
                    event.getTaskId(), event.getNewStatus());
                
                if (updated) {
                    log.info("工作流步骤已更新，下载任务: {}, 状态: {}", 
                            event.getTaskId(), event.getNewStatus());
                    
                    // 如果下载成功，启动后续步骤
                    if (event.isCompletedEvent()) {
                        triggerPostDownloadProcessing(event.getTaskId());
                    }
                } else {
                    log.debug("未找到相关工作流或更新失败，下载任务: {}", event.getTaskId());
                }
            }
            
        } catch (Exception e) {
            log.error("处理下载状态变化的工作流事件异常: taskId={}", event.getTaskId(), e);
        }
    }
    
    /**
     * 触发下载后的处理流程
     */
    private void triggerPostDownloadProcessing(Long downloadTaskId) {
        log.info("触发下载后处理流程: downloadTaskId={}", downloadTaskId);
        
        try {
            // 通过TaskRelation找到对应的工作流任务ID
            Optional<TaskRelation> relationOpt = taskRelationService.findByDownloadTaskId(downloadTaskId);
            if (!relationOpt.isPresent()) {
                log.debug("未找到下载任务对应的工作流关联: {}", downloadTaskId);
                return;
            }
            
            TaskRelation relation = relationOpt.get();
            String workflowTaskId = relation.getSubmissionTaskId(); // 使用投稿任务ID作为工作流任务ID
            
            // 检查并启动待执行的工作流步骤
            int startedSteps = workflowStepProgressService.checkAndStartPendingSteps(workflowTaskId);
            
            if (startedSteps > 0) {
                log.info("成功启动 {} 个工作流步骤，任务: {}", startedSteps, workflowTaskId);
            } else {
                log.debug("没有待启动的工作流步骤，任务: {}", workflowTaskId);
            }
            
        } catch (Exception e) {
            log.error("触发下载后处理流程异常: downloadTaskId={}", downloadTaskId, e);
        }
    }
}