package com.tbw.cut.service;

import com.tbw.cut.diagnostic.model.WorkflowInstanceStatus;
import com.tbw.cut.diagnostic.model.StuckWorkflow;
import com.tbw.cut.diagnostic.model.RestartResult;
import com.tbw.cut.diagnostic.model.WorkflowMetrics;
import java.util.List;

/**
 * 工作流状态监控器接口
 * 用于监控工作流的执行状态
 */
public interface WorkflowStatusMonitor {
    
    /**
     * 获取所有活跃工作流状态
     * 
     * @return 工作流实例状态列表
     */
    List<WorkflowInstanceStatus> getActiveWorkflows();
    
    /**
     * 检查卡住的工作流
     * 
     * @return 卡住的工作流列表
     */
    List<StuckWorkflow> detectStuckWorkflows();
    
    /**
     * 重启卡住的工作流
     * 
     * @param workflowId 工作流ID
     * @return 重启结果
     */
    RestartResult restartWorkflow(String workflowId);
    
    /**
     * 获取工作流性能指标
     * 
     * @return 工作流性能指标
     */
    WorkflowMetrics getWorkflowMetrics();
    
    /**
     * 获取特定工作流的状态
     * 
     * @param workflowId 工作流ID
     * @return 工作流实例状态
     */
    WorkflowInstanceStatus getWorkflowStatus(String workflowId);
    
    /**
     * 暂停工作流执行
     * 
     * @param workflowId 工作流ID
     * @return 操作结果
     */
    boolean pauseWorkflow(String workflowId);
    
    /**
     * 恢复工作流执行
     * 
     * @param workflowId 工作流ID
     * @return 操作结果
     */
    boolean resumeWorkflow(String workflowId);
    
    /**
     * 取消工作流执行
     * 
     * @param workflowId 工作流ID
     * @return 操作结果
     */
    boolean cancelWorkflow(String workflowId);
    
    /**
     * 获取工作流执行历史
     * 
     * @param workflowId 工作流ID
     * @return 执行历史
     */
    List<String> getWorkflowExecutionHistory(String workflowId);
}