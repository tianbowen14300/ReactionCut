package com.tbw.cut.workflow.service.impl;

import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.model.WorkflowStep;
import com.tbw.cut.workflow.model.WorkflowStatus;
import com.tbw.cut.workflow.model.StepStatus;
import com.tbw.cut.workflow.service.WorkflowEngine;
import com.tbw.cut.workflow.service.WorkflowRecoveryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 工作流恢复管理器实现
 * 
 * 提供完整的工作流错误处理和恢复机制，包括错误分类、恢复策略确定、
 * 自动重试、手动干预支持等功能。
 */
@Service
public class WorkflowRecoveryManagerImpl implements WorkflowRecoveryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkflowRecoveryManagerImpl.class);
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    // 用于测试的setter方法
    public void setWorkflowEngine(WorkflowEngine workflowEngine) {
        this.workflowEngine = workflowEngine;
    }
    
    // 恢复历史记录存储
    private final Map<String, List<RecoveryRecord>> recoveryHistory = new ConcurrentHashMap<>();
    
    // 重试计数器
    private final Map<String, Integer> retryCounters = new ConcurrentHashMap<>();
    
    // 暂停的工作流
    private final Set<String> pausedWorkflows = ConcurrentHashMap.newKeySet();
    
    // 等待资源的工作流
    private final Map<String, String> waitingForResource = new ConcurrentHashMap<>();
    
    // 定时任务执行器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // 配置参数
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_SECONDS = 30;
    private static final int RESOURCE_CHECK_INTERVAL_SECONDS = 60;
    
    @Override
    public boolean recoverFromFailure(WorkflowInstance instance, Exception error) {
        logger.info("开始恢复工作流失败: instanceId={}, error={}", 
                   instance.getInstanceId(), error.getMessage());
        
        try {
            // 分类错误类型
            ErrorType errorType = classifyError(error);
            
            // 确定恢复策略
            RecoveryStrategy strategy = determineRecoveryStrategy(errorType, instance, null);
            
            // 记录恢复尝试
            RecoveryRecord record = new RecoveryRecord(
                instance.getInstanceId(), null, errorType, strategy, error.getMessage()
            );
            addRecoveryRecord(instance.getInstanceId(), record);
            
            // 执行恢复策略
            boolean success = executeRecoveryStrategy(strategy, instance, null, error.getMessage());
            record.setSuccessful(success);
            
            logger.info("工作流恢复完成: instanceId={}, strategy={}, success={}", 
                       instance.getInstanceId(), strategy, success);
            
            return success;
            
        } catch (Exception e) {
            logger.error("工作流恢复过程中发生异常: instanceId={}", instance.getInstanceId(), e);
            return false;
        }
    }
    
    @Override
    public boolean recoverFromStepFailure(WorkflowInstance instance, WorkflowStep step, Exception error) {
        logger.info("开始恢复步骤失败: instanceId={}, stepId={}, error={}", 
                   instance.getInstanceId(), step.getStepId(), error.getMessage());
        
        try {
            // 分类错误类型
            ErrorType errorType = classifyError(error);
            
            // 确定恢复策略
            RecoveryStrategy strategy = determineRecoveryStrategy(errorType, instance, step);
            
            // 记录恢复尝试
            RecoveryRecord record = new RecoveryRecord(
                instance.getInstanceId(), step.getStepId(), errorType, strategy, error.getMessage()
            );
            addRecoveryRecord(instance.getInstanceId(), record);
            
            // 执行恢复策略
            boolean success = executeRecoveryStrategy(strategy, instance, step, error.getMessage());
            record.setSuccessful(success);
            
            logger.info("步骤恢复完成: instanceId={}, stepId={}, strategy={}, success={}", 
                       instance.getInstanceId(), step.getStepId(), strategy, success);
            
            return success;
            
        } catch (Exception e) {
            logger.error("步骤恢复过程中发生异常: instanceId={}, stepId={}", 
                        instance.getInstanceId(), step.getStepId(), e);
            return false;
        }
    }
    
    @Override
    public ErrorType classifyError(Exception error) {
        if (error == null) {
            return ErrorType.FATAL_ERROR;
        }
        
        String errorMessage = error.getMessage();
        String errorClass = error.getClass().getSimpleName();
        
        // 临时错误
        if (error instanceof IOException && !(error instanceof NoSuchFileException)) {
            return ErrorType.TEMPORARY_ERROR;
        }
        
        if (errorMessage != null) {
            String lowerMessage = errorMessage.toLowerCase();
            
            // 网络相关临时错误
            if (lowerMessage.contains("connection") || 
                lowerMessage.contains("timeout") || 
                lowerMessage.contains("network") ||
                lowerMessage.contains("socket")) {
                return ErrorType.TEMPORARY_ERROR;
            }
            
            // 资源相关错误
            if (lowerMessage.contains("disk space") || 
                lowerMessage.contains("memory") || 
                lowerMessage.contains("cpu") ||
                lowerMessage.contains("resource")) {
                return ErrorType.RESOURCE_ERROR;
            }
            
            // 配置相关错误
            if (lowerMessage.contains("config") || 
                lowerMessage.contains("parameter") || 
                lowerMessage.contains("invalid") ||
                lowerMessage.contains("missing")) {
                return ErrorType.CONFIGURATION_ERROR;
            }
            
            // 文件不存在等致命错误
            if (lowerMessage.contains("not found") || 
                lowerMessage.contains("no such file") ||
                lowerMessage.contains("access denied")) {
                return ErrorType.FATAL_ERROR;
            }
        }
        
        // 运行时异常通常是临时错误
        if (error instanceof RuntimeException) {
            return ErrorType.TEMPORARY_ERROR;
        }
        
        // 默认为致命错误
        return ErrorType.FATAL_ERROR;
    }
    
    @Override
    public RecoveryStrategy determineRecoveryStrategy(ErrorType errorType, WorkflowInstance instance, WorkflowStep step) {
        switch (errorType) {
            case TEMPORARY_ERROR:
                // 检查重试次数
                String key = instance.getInstanceId() + (step != null ? ":" + step.getStepId() : "");
                int retryCount = retryCounters.getOrDefault(key, 0);
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    return RecoveryStrategy.RETRY;
                } else {
                    return RecoveryStrategy.PAUSE_FOR_INTERVENTION;
                }
                
            case CONFIGURATION_ERROR:
                return RecoveryStrategy.PAUSE_FOR_INTERVENTION;
                
            case RESOURCE_ERROR:
                return RecoveryStrategy.WAIT_FOR_RESOURCE;
                
            case FATAL_ERROR:
                // 对于某些步骤，可以考虑跳过
                if (step != null && isSkippableStep(step)) {
                    return RecoveryStrategy.SKIP_STEP;
                } else {
                    return RecoveryStrategy.MARK_AS_FAILED;
                }
                
            default:
                return RecoveryStrategy.MARK_AS_FAILED;
        }
    }
    
    @Override
    public boolean scheduleRetry(WorkflowInstance instance, WorkflowStep step) {
        String key = instance.getInstanceId() + (step != null ? ":" + step.getStepId() : "");
        int currentRetryCount = retryCounters.getOrDefault(key, 0);
        
        if (currentRetryCount >= MAX_RETRY_ATTEMPTS) {
            logger.warn("已达到最大重试次数: instanceId={}, stepId={}, retryCount={}", 
                       instance.getInstanceId(), step != null ? step.getStepId() : "null", currentRetryCount);
            return false;
        }
        
        // 增加重试计数
        retryCounters.put(key, currentRetryCount + 1);
        
        // 安排延迟重试
        scheduler.schedule(() -> {
            try {
                logger.info("执行重试: instanceId={}, stepId={}, attempt={}", 
                           instance.getInstanceId(), step != null ? step.getStepId() : "null", currentRetryCount + 1);
                
                if (step != null) {
                    // 重试特定步骤
                    step.setStatus(StepStatus.PENDING);
                    step.setErrorMessage(null);
                } else {
                    // 重试整个工作流
                    instance.setStatus(WorkflowStatus.PENDING);
                    instance.setErrorMessage(null);
                }
                
                // 重新启动工作流
                workflowEngine.resumeWorkflow(instance.getInstanceId());
                
            } catch (Exception e) {
                logger.error("重试执行失败: instanceId={}, stepId={}", 
                            instance.getInstanceId(), step != null ? step.getStepId() : "null", e);
            }
        }, RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
        
        logger.info("已安排重试: instanceId={}, stepId={}, delay={}秒, attempt={}", 
                   instance.getInstanceId(), step != null ? step.getStepId() : "null", 
                   RETRY_DELAY_SECONDS, currentRetryCount + 1);
        
        return true;
    }
    
    @Override
    public boolean pauseWorkflowForUserIntervention(WorkflowInstance instance, String reason) {
        try {
            // 暂停工作流
            boolean paused = workflowEngine.pauseWorkflow(instance.getInstanceId());
            if (paused) {
                pausedWorkflows.add(instance.getInstanceId());
                instance.setErrorMessage("等待用户干预: " + reason);
                
                logger.info("工作流已暂停等待用户干预: instanceId={}, reason={}", 
                           instance.getInstanceId(), reason);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            logger.error("暂停工作流失败: instanceId={}", instance.getInstanceId(), e);
            return false;
        }
    }
    
    @Override
    public boolean waitForResourceAvailability(WorkflowInstance instance, String resourceType) {
        try {
            // 暂停工作流
            boolean paused = workflowEngine.pauseWorkflow(instance.getInstanceId());
            if (paused) {
                waitingForResource.put(instance.getInstanceId(), resourceType);
                instance.setErrorMessage("等待资源可用: " + resourceType);
                
                // 安排定期检查资源可用性
                scheduleResourceCheck(instance.getInstanceId(), resourceType);
                
                logger.info("工作流等待资源可用: instanceId={}, resourceType={}", 
                           instance.getInstanceId(), resourceType);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            logger.error("设置资源等待失败: instanceId={}", instance.getInstanceId(), e);
            return false;
        }
    }
    
    @Override
    public boolean markWorkflowAsFailed(WorkflowInstance instance, String reason) {
        try {
            instance.setStatus(WorkflowStatus.FAILED);
            instance.setErrorMessage(reason);
            instance.setEndTime(LocalDateTime.now());
            
            // 清理相关状态
            String instanceId = instance.getInstanceId();
            retryCounters.entrySet().removeIf(entry -> entry.getKey().startsWith(instanceId));
            pausedWorkflows.remove(instanceId);
            waitingForResource.remove(instanceId);
            
            logger.info("工作流已标记为失败: instanceId={}, reason={}", instanceId, reason);
            return true;
            
        } catch (Exception e) {
            logger.error("标记工作流失败时发生异常: instanceId={}", instance.getInstanceId(), e);
            return false;
        }
    }
    
    @Override
    public boolean skipFailedStep(WorkflowInstance instance, WorkflowStep step, String reason) {
        try {
            step.setStatus(StepStatus.SKIPPED);
            step.setErrorMessage("已跳过: " + reason);
            step.setEndTime(LocalDateTime.now());
            
            logger.info("步骤已跳过: instanceId={}, stepId={}, reason={}", 
                       instance.getInstanceId(), step.getStepId(), reason);
            
            // 继续执行下一个步骤
            workflowEngine.resumeWorkflow(instance.getInstanceId());
            
            return true;
            
        } catch (Exception e) {
            logger.error("跳过步骤失败: instanceId={}, stepId={}", 
                        instance.getInstanceId(), step.getStepId(), e);
            return false;
        }
    }
    
    @Override
    public boolean manualRecover(String instanceId, String fromStepId) {
        try {
            logger.info("开始手动恢复工作流: instanceId={}, fromStepId={}", instanceId, fromStepId);
            
            // 清理暂停状态
            pausedWorkflows.remove(instanceId);
            waitingForResource.remove(instanceId);
            
            // 重置重试计数器
            if (fromStepId != null) {
                String key = instanceId + ":" + fromStepId;
                retryCounters.remove(key);
            } else {
                retryCounters.entrySet().removeIf(entry -> entry.getKey().startsWith(instanceId));
            }
            
            // 恢复工作流
            boolean resumed = workflowEngine.resumeWorkflow(instanceId);
            
            if (resumed) {
                // 记录手动恢复
                RecoveryRecord record = new RecoveryRecord(instanceId, fromStepId, 
                    null, RecoveryStrategy.RETRY, "手动恢复");
                record.setRecoveryReason("用户手动干预恢复");
                record.setSuccessful(true);
                addRecoveryRecord(instanceId, record);
                
                logger.info("手动恢复成功: instanceId={}, fromStepId={}", instanceId, fromStepId);
            }
            
            return resumed;
            
        } catch (Exception e) {
            logger.error("手动恢复失败: instanceId={}, fromStepId={}", instanceId, fromStepId, e);
            return false;
        }
    }
    
    @Override
    public List<RecoveryRecord> getRecoveryHistory(String instanceId) {
        return recoveryHistory.getOrDefault(instanceId, new ArrayList<>());
    }
    
    // 私有辅助方法
    
    private boolean executeRecoveryStrategy(RecoveryStrategy strategy, WorkflowInstance instance, 
                                          WorkflowStep step, String errorMessage) {
        switch (strategy) {
            case RETRY:
                return scheduleRetry(instance, step);
                
            case PAUSE_FOR_INTERVENTION:
                return pauseWorkflowForUserIntervention(instance, errorMessage);
                
            case WAIT_FOR_RESOURCE:
                String resourceType = extractResourceType(errorMessage);
                return waitForResourceAvailability(instance, resourceType);
                
            case MARK_AS_FAILED:
                return markWorkflowAsFailed(instance, errorMessage);
                
            case SKIP_STEP:
                if (step != null) {
                    return skipFailedStep(instance, step, errorMessage);
                } else {
                    return markWorkflowAsFailed(instance, "无法跳过整个工作流");
                }
                
            default:
                return false;
        }
    }
    
    private boolean isSkippableStep(WorkflowStep step) {
        // 某些步骤可以跳过，比如分段步骤
        return step.getType().name().equals("SEGMENTATION");
    }
    
    private String extractResourceType(String errorMessage) {
        if (errorMessage == null) {
            return "unknown";
        }
        
        String lowerMessage = errorMessage.toLowerCase();
        if (lowerMessage.contains("disk") || lowerMessage.contains("space")) {
            return "disk_space";
        } else if (lowerMessage.contains("memory")) {
            return "memory";
        } else if (lowerMessage.contains("cpu")) {
            return "cpu";
        } else {
            return "system_resource";
        }
    }
    
    private void scheduleResourceCheck(String instanceId, String resourceType) {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (!waitingForResource.containsKey(instanceId)) {
                    return; // 已经不再等待资源
                }
                
                // 检查资源是否可用（简化实现）
                boolean resourceAvailable = checkResourceAvailability(resourceType);
                
                if (resourceAvailable) {
                    logger.info("资源已可用，恢复工作流: instanceId={}, resourceType={}", 
                               instanceId, resourceType);
                    
                    waitingForResource.remove(instanceId);
                    workflowEngine.resumeWorkflow(instanceId);
                }
                
            } catch (Exception e) {
                logger.error("资源检查失败: instanceId={}, resourceType={}", instanceId, resourceType, e);
            }
        }, RESOURCE_CHECK_INTERVAL_SECONDS, RESOURCE_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
    
    private boolean checkResourceAvailability(String resourceType) {
        // 简化的资源检查实现
        // 实际实现应该检查真实的系统资源状态
        switch (resourceType) {
            case "disk_space":
                // 检查磁盘空间
                return Runtime.getRuntime().freeMemory() > 1024 * 1024 * 100; // 100MB
                
            case "memory":
                // 检查内存
                return Runtime.getRuntime().freeMemory() > 1024 * 1024 * 500; // 500MB
                
            case "cpu":
                // 检查CPU负载（简化）
                return true;
                
            default:
                return true;
        }
    }
    
    private void addRecoveryRecord(String instanceId, RecoveryRecord record) {
        recoveryHistory.computeIfAbsent(instanceId, k -> new ArrayList<>()).add(record);
    }
}