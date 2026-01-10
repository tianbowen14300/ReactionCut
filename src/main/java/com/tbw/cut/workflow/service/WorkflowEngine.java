package com.tbw.cut.workflow.service;

import com.tbw.cut.workflow.model.WorkflowConfig;
import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.model.WorkflowStatus;

import java.util.List;

/**
 * 工作流引擎接口
 * 负责管理工作流的生命周期，协调各个组件的交互
 */
public interface WorkflowEngine {
    
    /**
     * 启动新的工作流实例
     * 
     * @param taskId 关联的任务ID
     * @param config 工作流配置
     * @return 工作流实例
     */
    WorkflowInstance startWorkflow(String taskId, WorkflowConfig config);
    
    /**
     * 查询工作流执行状态
     * 
     * @param instanceId 工作流实例ID
     * @return 工作流状态，如果不存在返回null
     */
    WorkflowStatus getWorkflowStatus(String instanceId);
    
    /**
     * 获取工作流实例详情
     * 
     * @param instanceId 工作流实例ID
     * @return 工作流实例，如果不存在返回null
     */
    WorkflowInstance getWorkflowInstance(String instanceId);
    
    /**
     * 取消正在执行的工作流
     * 
     * @param instanceId 工作流实例ID
     * @return 是否成功取消
     */
    boolean cancelWorkflow(String instanceId);
    
    /**
     * 暂停工作流执行
     * 
     * @param instanceId 工作流实例ID
     * @return 是否成功暂停
     */
    boolean pauseWorkflow(String instanceId);
    
    /**
     * 恢复工作流执行
     * 
     * @param instanceId 工作流实例ID
     * @return 是否成功恢复
     */
    boolean resumeWorkflow(String instanceId);
    
    /**
     * 获取所有活跃的工作流实例
     * 
     * @return 活跃的工作流实例列表
     */
    List<WorkflowInstance> getActiveWorkflows();
    
    /**
     * 根据任务ID获取工作流实例
     * 
     * @param taskId 任务ID
     * @return 工作流实例，如果不存在返回null
     */
    WorkflowInstance getWorkflowByTaskId(String taskId);
    
    /**
     * 清理已完成的工作流实例
     * 
     * @param olderThanHours 清理多少小时前完成的实例
     * @return 清理的实例数量
     */
    int cleanupCompletedWorkflows(int olderThanHours);
}