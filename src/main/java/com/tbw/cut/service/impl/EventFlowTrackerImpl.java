package com.tbw.cut.service.impl;

import com.tbw.cut.service.EventFlowTracker;
import com.tbw.cut.diagnostic.model.EventStep;
import com.tbw.cut.diagnostic.model.EventBreakpoint;
import com.tbw.cut.diagnostic.model.EventStepStatus;
import com.tbw.cut.diagnostic.model.BreakpointType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 事件流追踪器实现
 */
@Service
public class EventFlowTrackerImpl implements EventFlowTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(EventFlowTrackerImpl.class);
    
    // 事件追踪数据存储（实际应用中可能需要持久化）
    private final ConcurrentMap<Long, List<EventStep>> eventTraces = new ConcurrentHashMap<>();
    
    // 正在追踪的任务集合
    private final ConcurrentMap<Long, Boolean> trackingTasks = new ConcurrentHashMap<>();
    
    @Override
    public void startTracking(Long taskId) {
        logger.info("开始追踪事件流, taskId: {}", taskId);
        
        try {
            trackingTasks.put(taskId, true);
            eventTraces.put(taskId, new ArrayList<>());
            
            // 记录追踪开始事件
            recordEventStep(taskId, "TRACKING_STARTED", "开始追踪任务事件流");
            
            logger.debug("事件流追踪已启动, taskId: {}", taskId);
            
        } catch (Exception e) {
            logger.error("启动事件流追踪时发生错误, taskId: {}", taskId, e);
        }
    }
    
    @Override
    public void recordEventStep(Long taskId, String step, Object data) {
        logger.debug("记录事件步骤, taskId: {}, step: {}", taskId, step);
        
        try {
            if (!isTracking(taskId)) {
                logger.debug("任务未在追踪中，跳过记录, taskId: {}", taskId);
                return;
            }
            
            EventStep eventStep = new EventStep();
            eventStep.setStepName(step);
            eventStep.setDescription(data != null ? data.toString() : null);
            eventStep.setTimestamp(LocalDateTime.now());
            eventStep.setExecutionTimeMs(System.currentTimeMillis());
            eventStep.setStatus(EventStepStatus.SUCCESS);
            eventStep.setComponentName(determineComponentName(step));
            
            List<EventStep> steps = eventTraces.get(taskId);
            if (steps != null) {
                steps.add(eventStep);
                
                // 限制步骤数量，避免内存溢出
                if (steps.size() > 1000) {
                    steps.remove(0); // 移除最旧的步骤
                }
            }
            
            logger.debug("事件步骤记录完成, taskId: {}, step: {}", taskId, step);
            
        } catch (Exception e) {
            logger.error("记录事件步骤时发生错误, taskId: {}, step: {}", taskId, step, e);
        }
    }
    
    @Override
    public List<EventStep> getEventHistory(Long taskId) {
        logger.debug("获取事件流历史, taskId: {}", taskId);
        
        try {
            List<EventStep> steps = eventTraces.get(taskId);
            if (steps != null) {
                return new ArrayList<>(steps); // 返回副本
            }
            
            logger.debug("未找到事件流历史, taskId: {}", taskId);
            return Collections.emptyList();
            
        } catch (Exception e) {
            logger.error("获取事件流历史时发生错误, taskId: {}", taskId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<EventBreakpoint> detectBreakpoints(Long taskId) {
        logger.info("检测事件流中断点, taskId: {}", taskId);
        
        List<EventBreakpoint> breakpoints = new ArrayList<>();
        
        try {
            List<EventStep> steps = getEventHistory(taskId);
            if (steps.isEmpty()) {
                logger.debug("没有事件历史，无法检测中断点, taskId: {}", taskId);
                return breakpoints;
            }
            
            // 检测时间间隔异常的中断点
            for (int i = 1; i < steps.size(); i++) {
                EventStep prevStep = steps.get(i - 1);
                EventStep currentStep = steps.get(i);
                
                long timeDiff = currentStep.getExecutionTimeMs() - prevStep.getExecutionTimeMs();
                
                // 如果两个步骤之间的时间间隔超过5分钟，认为是中断点
                if (timeDiff > 5 * 60 * 1000) {
                    EventBreakpoint breakpoint = new EventBreakpoint();
                    breakpoint.setStepName(currentStep.getStepName());
                    breakpoint.setType(BreakpointType.TIMEOUT);
                    breakpoint.setTimestamp(currentStep.getTimestamp());
                    breakpoint.setExpectedNextStep(prevStep.getStepName());
                    breakpoint.setActualNextStep(currentStep.getStepName());
                    breakpoint.setTimeoutMs(timeDiff);
                    breakpoint.setDescription("步骤间时间间隔异常: " + (timeDiff / 1000) + "秒");
                    breakpoint.setReason("处理延迟或系统暂停");
                    
                    breakpoints.add(breakpoint);
                }
            }
            
            // 检测失败的步骤
            for (EventStep step : steps) {
                if (step.getStatus() == EventStepStatus.FAILED) {
                    EventBreakpoint breakpoint = new EventBreakpoint();
                    breakpoint.setStepName(step.getStepName());
                    breakpoint.setType(BreakpointType.EXECUTION_FAILURE);
                    breakpoint.setTimestamp(step.getTimestamp());
                    breakpoint.setExpectedNextStep(step.getStepName());
                    breakpoint.setDescription("步骤执行失败: " + step.getStepName());
                    breakpoint.setReason(step.getErrorMessage() != null ? step.getErrorMessage() : "未知错误");
                    
                    breakpoints.add(breakpoint);
                }
            }
            
            // 检测预期步骤缺失
            List<String> expectedSteps = getExpectedSteps();
            List<String> actualSteps = steps.stream()
                .map(EventStep::getStepName)
                .collect(Collectors.toList());
            
            for (String expectedStep : expectedSteps) {
                if (!actualSteps.contains(expectedStep)) {
                    EventBreakpoint breakpoint = new EventBreakpoint();
                    breakpoint.setStepName(expectedStep);
                    breakpoint.setType(BreakpointType.MISSING_DEPENDENCY);
                    breakpoint.setTimestamp(LocalDateTime.now());
                    breakpoint.setDescription("缺少预期步骤: " + expectedStep);
                    breakpoint.setReason("工作流配置错误或步骤跳过");
                    
                    breakpoints.add(breakpoint);
                }
            }
            
            logger.info("检测到 {} 个事件流中断点, taskId: {}", breakpoints.size(), taskId);
            
        } catch (Exception e) {
            logger.error("检测事件流中断点时发生错误, taskId: {}", taskId, e);
        }
        
        return breakpoints;
    }
    
    @Override
    public void stopTracking(Long taskId) {
        logger.info("停止追踪事件流, taskId: {}", taskId);
        
        try {
            if (isTracking(taskId)) {
                // 记录追踪结束事件
                recordEventStep(taskId, "TRACKING_STOPPED", "停止追踪任务事件流");
                
                trackingTasks.remove(taskId);
                
                logger.debug("事件流追踪已停止, taskId: {}", taskId);
            } else {
                logger.debug("任务未在追踪中, taskId: {}", taskId);
            }
            
        } catch (Exception e) {
            logger.error("停止事件流追踪时发生错误, taskId: {}", taskId, e);
        }
    }
    
    @Override
    public void cleanupExpiredTraces(int retentionHours) {
        logger.info("清理过期的追踪数据, 保留时间: {} 小时", retentionHours);
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(retentionHours);
            int cleanedCount = 0;
            
            List<Long> expiredTaskIds = new ArrayList<>();
            
            for (Long taskId : eventTraces.keySet()) {
                List<EventStep> steps = eventTraces.get(taskId);
                if (steps != null && !steps.isEmpty()) {
                    EventStep lastStep = steps.get(steps.size() - 1);
                    if (lastStep.getTimestamp().isBefore(cutoffTime)) {
                        expiredTaskIds.add(taskId);
                    }
                }
            }
            
            for (Long taskId : expiredTaskIds) {
                eventTraces.remove(taskId);
                trackingTasks.remove(taskId);
                cleanedCount++;
            }
            
            logger.info("清理完成，删除了 {} 个过期的追踪记录", cleanedCount);
            
        } catch (Exception e) {
            logger.error("清理过期追踪数据时发生错误", e);
        }
    }
    
    @Override
    public boolean isTracking(Long taskId) {
        return trackingTasks.getOrDefault(taskId, false);
    }
    
    // 私有辅助方法
    
    /**
     * 根据步骤名称确定组件名称
     */
    private String determineComponentName(String stepName) {
        if (stepName.contains("DOWNLOAD")) {
            return "DownloadService";
        } else if (stepName.contains("UPLOAD") || stepName.contains("SUBMIT")) {
            return "SubmissionService";
        } else if (stepName.contains("PROCESS") || stepName.contains("CLIP")) {
            return "ProcessingService";
        } else if (stepName.contains("EVENT")) {
            return "EventPublisher";
        } else if (stepName.contains("WORKFLOW")) {
            return "WorkflowEngine";
        } else {
            return "Unknown";
        }
    }
    
    /**
     * 获取预期的工作流步骤
     */
    private List<String> getExpectedSteps() {
        List<String> expectedSteps = new ArrayList<>();
        expectedSteps.add("DOWNLOAD_STARTED");
        expectedSteps.add("DOWNLOAD_COMPLETED");
        expectedSteps.add("EVENT_PUBLISHED");
        expectedSteps.add("WORKFLOW_TRIGGERED");
        expectedSteps.add("PROCESSING_STARTED");
        expectedSteps.add("PROCESSING_COMPLETED");
        expectedSteps.add("SUBMISSION_STARTED");
        expectedSteps.add("SUBMISSION_COMPLETED");
        return expectedSteps;
    }
}