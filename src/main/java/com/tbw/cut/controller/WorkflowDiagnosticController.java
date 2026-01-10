package com.tbw.cut.controller;

import com.tbw.cut.service.WorkflowDiagnosticService;
import com.tbw.cut.service.TaskRelationRepairer;
import com.tbw.cut.service.WorkflowStatusMonitor;
import com.tbw.cut.service.EventFlowTracker;
import com.tbw.cut.diagnostic.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流诊断控制器
 * 提供工作流诊断和修复的REST API接口
 */
@RestController
@RequestMapping("/api/workflow/diagnostic")
@CrossOrigin(origins = "*")
public class WorkflowDiagnosticController {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkflowDiagnosticController.class);
    
    @Autowired
    private WorkflowDiagnosticService workflowDiagnosticService;
    
    @Autowired
    private TaskRelationRepairer taskRelationRepairer;
    
    @Autowired
    private WorkflowStatusMonitor workflowStatusMonitor;
    
    @Autowired
    private EventFlowTracker eventFlowTracker;
    
    /**
     * 执行完整的工作流诊断
     */
    @GetMapping("/full")
    public ResponseEntity<DiagnosticReport> performFullDiagnosis() {
        logger.info("收到完整诊断请求");
        
        try {
            DiagnosticReport report = workflowDiagnosticService.performFullDiagnosis();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("执行完整诊断时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检查特定任务的工作流状态
     */
    @GetMapping("/task/{downloadTaskId}")
    public ResponseEntity<TaskWorkflowStatus> checkTaskWorkflow(@PathVariable Long downloadTaskId) {
        logger.info("收到任务工作流状态检查请求, downloadTaskId: {}", downloadTaskId);
        
        try {
            TaskWorkflowStatus status = workflowDiagnosticService.checkTaskWorkflow(downloadTaskId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("检查任务工作流状态时发生错误, downloadTaskId: {}", downloadTaskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 追踪事件流
     */
    @GetMapping("/event-flow/{taskId}")
    public ResponseEntity<EventFlowTrace> traceEventFlow(@PathVariable Long taskId) {
        logger.info("收到事件流追踪请求, taskId: {}", taskId);
        
        try {
            EventFlowTrace trace = workflowDiagnosticService.traceEventFlow(taskId);
            if (trace != null) {
                return ResponseEntity.ok(trace);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("追踪事件流时发生错误, taskId: {}", taskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 验证任务关联完整性
     */
    @GetMapping("/task-relations/validate")
    public ResponseEntity<TaskRelationValidation> validateTaskRelations() {
        logger.info("收到任务关联验证请求");
        
        try {
            TaskRelationValidation validation = workflowDiagnosticService.validateTaskRelations();
            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            logger.error("验证任务关联时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检查工作流引擎状态
     */
    @GetMapping("/workflow-engine/status")
    public ResponseEntity<WorkflowEngineStatus> checkWorkflowEngineStatus() {
        logger.info("收到工作流引擎状态检查请求");
        
        try {
            WorkflowEngineStatus status = workflowDiagnosticService.checkWorkflowEngine();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("检查工作流引擎状态时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取系统健康状态概览
     */
    @GetMapping("/health/overview")
    public ResponseEntity<SystemHealthOverview> getSystemHealthOverview() {
        logger.info("收到系统健康状态概览请求");
        
        try {
            SystemHealthOverview overview = workflowDiagnosticService.getSystemHealthOverview();
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            logger.error("获取系统健康状态概览时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 查找孤立任务
     */
    @GetMapping("/orphaned-tasks")
    public ResponseEntity<List<OrphanedTask>> findOrphanedTasks() {
        logger.info("收到查找孤立任务请求");
        
        try {
            List<OrphanedTask> orphanedTasks = workflowDiagnosticService.findOrphanedTasks();
            return ResponseEntity.ok(orphanedTasks);
        } catch (Exception e) {
            logger.error("查找孤立任务时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 查找卡住的工作流
     */
    @GetMapping("/stuck-workflows")
    public ResponseEntity<List<StuckWorkflow>> findStuckWorkflows() {
        logger.info("收到查找卡住工作流请求");
        
        try {
            List<StuckWorkflow> stuckWorkflows = workflowDiagnosticService.findStuckWorkflows();
            return ResponseEntity.ok(stuckWorkflows);
        } catch (Exception e) {
            logger.error("查找卡住工作流时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 重建任务关联
     */
    @PostMapping("/task-relations/rebuild/{downloadTaskId}")
    public ResponseEntity<RepairResult> rebuildTaskRelations(@PathVariable Long downloadTaskId) {
        logger.info("收到重建任务关联请求, downloadTaskId: {}", downloadTaskId);
        
        try {
            RepairResult result = taskRelationRepairer.rebuildTaskRelations(downloadTaskId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("重建任务关联时发生错误, downloadTaskId: {}", downloadTaskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 自动修复孤立任务
     */
    @PostMapping("/orphaned-tasks/repair/{taskId}")
    public ResponseEntity<RepairResult> autoRepairOrphanedTask(@PathVariable String taskId) {
        logger.info("收到自动修复孤立任务请求, taskId: {}", taskId);
        
        try {
            RepairResult result = taskRelationRepairer.autoRepairOrphanedTask(taskId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("自动修复孤立任务时发生错误, taskId: {}", taskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 批量修复孤立任务
     */
    @PostMapping("/orphaned-tasks/batch-repair")
    public ResponseEntity<List<RepairResult>> batchRepairOrphanedTasks(@RequestBody List<String> taskIds) {
        logger.info("收到批量修复孤立任务请求, 任务数量: {}", taskIds.size());
        
        try {
            List<RepairResult> results = taskRelationRepairer.batchRepairOrphanedTasks(taskIds);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("批量修复孤立任务时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 清理无效关联
     */
    @PostMapping("/task-relations/cleanup")
    public ResponseEntity<CleanupResult> cleanupInvalidRelations() {
        logger.info("收到清理无效关联请求");
        
        try {
            CleanupResult result = taskRelationRepairer.cleanupInvalidRelations();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("清理无效关联时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 重启卡住的工作流
     */
    @PostMapping("/workflows/restart/{workflowId}")
    public ResponseEntity<RestartResult> restartWorkflow(@PathVariable String workflowId) {
        logger.info("收到重启工作流请求, workflowId: {}", workflowId);
        
        try {
            RestartResult result = workflowStatusMonitor.restartWorkflow(workflowId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("重启工作流时发生错误, workflowId: {}", workflowId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 暂停工作流
     */
    @PostMapping("/workflows/pause/{workflowId}")
    public ResponseEntity<Map<String, Object>> pauseWorkflow(@PathVariable String workflowId) {
        logger.info("收到暂停工作流请求, workflowId: {}", workflowId);
        
        try {
            boolean success = workflowStatusMonitor.pauseWorkflow(workflowId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("workflowId", workflowId);
            response.put("message", success ? "工作流暂停成功" : "工作流暂停失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("暂停工作流时发生错误, workflowId: {}", workflowId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 恢复工作流
     */
    @PostMapping("/workflows/resume/{workflowId}")
    public ResponseEntity<Map<String, Object>> resumeWorkflow(@PathVariable String workflowId) {
        logger.info("收到恢复工作流请求, workflowId: {}", workflowId);
        
        try {
            boolean success = workflowStatusMonitor.resumeWorkflow(workflowId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("workflowId", workflowId);
            response.put("message", success ? "工作流恢复成功" : "工作流恢复失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("恢复工作流时发生错误, workflowId: {}", workflowId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 取消工作流
     */
    @PostMapping("/workflows/cancel/{workflowId}")
    public ResponseEntity<Map<String, Object>> cancelWorkflow(@PathVariable String workflowId) {
        logger.info("收到取消工作流请求, workflowId: {}", workflowId);
        
        try {
            boolean success = workflowStatusMonitor.cancelWorkflow(workflowId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("workflowId", workflowId);
            response.put("message", success ? "工作流取消成功" : "工作流取消失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("取消工作流时发生错误, workflowId: {}", workflowId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取工作流性能指标
     */
    @GetMapping("/workflows/metrics")
    public ResponseEntity<WorkflowMetrics> getWorkflowMetrics() {
        logger.info("收到获取工作流性能指标请求");
        
        try {
            WorkflowMetrics metrics = workflowStatusMonitor.getWorkflowMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("获取工作流性能指标时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取活跃工作流列表
     */
    @GetMapping("/workflows/active")
    public ResponseEntity<List<WorkflowInstanceStatus>> getActiveWorkflows() {
        logger.info("收到获取活跃工作流请求");
        
        try {
            List<WorkflowInstanceStatus> activeWorkflows = workflowStatusMonitor.getActiveWorkflows();
            return ResponseEntity.ok(activeWorkflows);
        } catch (Exception e) {
            logger.error("获取活跃工作流时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取特定工作流状态
     */
    @GetMapping("/workflows/{workflowId}/status")
    public ResponseEntity<WorkflowInstanceStatus> getWorkflowStatus(@PathVariable String workflowId) {
        logger.info("收到获取工作流状态请求, workflowId: {}", workflowId);
        
        try {
            WorkflowInstanceStatus status = workflowStatusMonitor.getWorkflowStatus(workflowId);
            if (status != null) {
                return ResponseEntity.ok(status);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("获取工作流状态时发生错误, workflowId: {}", workflowId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取工作流执行历史
     */
    @GetMapping("/workflows/{workflowId}/history")
    public ResponseEntity<List<String>> getWorkflowExecutionHistory(@PathVariable String workflowId) {
        logger.info("收到获取工作流执行历史请求, workflowId: {}", workflowId);
        
        try {
            List<String> history = workflowStatusMonitor.getWorkflowExecutionHistory(workflowId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("获取工作流执行历史时发生错误, workflowId: {}", workflowId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 开始事件流追踪
     */
    @PostMapping("/event-flow/start/{taskId}")
    public ResponseEntity<Map<String, Object>> startEventFlowTracking(@PathVariable Long taskId) {
        logger.info("收到开始事件流追踪请求, taskId: {}", taskId);
        
        try {
            eventFlowTracker.startTracking(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "事件流追踪已启动");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("开始事件流追踪时发生错误, taskId: {}", taskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 停止事件流追踪
     */
    @PostMapping("/event-flow/stop/{taskId}")
    public ResponseEntity<Map<String, Object>> stopEventFlowTracking(@PathVariable Long taskId) {
        logger.info("收到停止事件流追踪请求, taskId: {}", taskId);
        
        try {
            eventFlowTracker.stopTracking(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "事件流追踪已停止");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("停止事件流追踪时发生错误, taskId: {}", taskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取事件流历史
     */
    @GetMapping("/event-flow/{taskId}/history")
    public ResponseEntity<List<EventStep>> getEventFlowHistory(@PathVariable Long taskId) {
        logger.info("收到获取事件流历史请求, taskId: {}", taskId);
        
        try {
            List<EventStep> history = eventFlowTracker.getEventHistory(taskId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("获取事件流历史时发生错误, taskId: {}", taskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检测事件流中断点
     */
    @GetMapping("/event-flow/{taskId}/breakpoints")
    public ResponseEntity<List<EventBreakpoint>> detectEventFlowBreakpoints(@PathVariable Long taskId) {
        logger.info("收到检测事件流中断点请求, taskId: {}", taskId);
        
        try {
            List<EventBreakpoint> breakpoints = eventFlowTracker.detectBreakpoints(taskId);
            return ResponseEntity.ok(breakpoints);
        } catch (Exception e) {
            logger.error("检测事件流中断点时发生错误, taskId: {}", taskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 清理过期的事件流追踪数据
     */
    @PostMapping("/event-flow/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupExpiredEventFlowTraces(
            @RequestParam(defaultValue = "24") int retentionHours) {
        logger.info("收到清理过期事件流追踪数据请求, 保留时间: {} 小时", retentionHours);
        
        try {
            eventFlowTracker.cleanupExpiredTraces(retentionHours);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("retentionHours", retentionHours);
            response.put("message", "过期事件流追踪数据清理完成");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("清理过期事件流追踪数据时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}