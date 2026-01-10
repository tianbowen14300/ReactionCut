package com.tbw.cut.service;

import com.tbw.cut.diagnostic.model.*;
import java.util.List;

/**
 * 工作流诊断服务接口
 * 提供全面的工作流健康检查和问题诊断功能
 */
public interface WorkflowDiagnosticService {
    
    /**
     * 执行完整的工作流健康检查
     * 检查所有组件的状态，识别问题并提供修复建议
     * 
     * @return 完整的诊断报告
     */
    DiagnosticReport performFullDiagnosis();
    
    /**
     * 检查特定下载任务的工作流状态
     * 追踪从下载完成到最终投稿的完整流程
     * 
     * @param downloadTaskId 下载任务ID
     * @return 任务工作流状态
     */
    TaskWorkflowStatus checkTaskWorkflow(Long downloadTaskId);
    
    /**
     * 追踪事件流传播路径
     * 记录事件从发布到处理的完整路径
     * 
     * @param taskId 任务ID
     * @return 事件流跟踪结果
     */
    EventFlowTrace traceEventFlow(Long taskId);
    
    /**
     * 验证任务关联完整性
     * 检查下载任务和投稿任务之间的关联关系
     * 
     * @return 任务关联验证结果
     */
    TaskRelationValidation validateTaskRelations();
    
    /**
     * 检查工作流引擎状态
     * 验证工作流引擎的健康状态和性能指标
     * 
     * @return 工作流引擎状态
     */
    WorkflowEngineStatus checkWorkflowEngine();
    
    /**
     * 检查特定工作流实例的状态
     * 
     * @param workflowInstanceId 工作流实例ID
     * @return 工作流实例状态
     */
    WorkflowInstanceStatus checkWorkflowInstance(String workflowInstanceId);
    
    /**
     * 获取系统健康状态概览
     * 
     * @return 系统健康状态
     */
    SystemHealthOverview getSystemHealthOverview();
    
    /**
     * 检查孤立的下载任务
     * 识别没有对应投稿任务关联的下载任务
     * 
     * @return 孤立任务列表
     */
    List<OrphanedTask> findOrphanedTasks();
    
    /**
     * 检查卡住的工作流
     * 识别长时间无响应或执行异常的工作流
     * 
     * @return 卡住的工作流列表
     */
    List<StuckWorkflow> findStuckWorkflows();
}