package com.tbw.cut.service.impl;

import com.tbw.cut.service.EventPublishChecker;
import com.tbw.cut.diagnostic.model.*;
import com.tbw.cut.event.DownloadStatusChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件发布检查器实现
 */
@Service
public class EventPublishCheckerImpl implements EventPublishChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(EventPublishCheckerImpl.class);
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    // 统计信息（实际应用中可能需要持久化或使用专门的监控系统）
    private long totalEventsPublished = 0;
    private long successfulEvents = 0;
    private long failedEvents = 0;
    private final Map<String, Long> eventTypeStats = new HashMap<>();
    
    @Override
    public ComponentStatus checkEventPublisherStatus() {
        logger.info("检查ApplicationEventPublisher状态");
        
        ComponentStatus status = new ComponentStatus("ApplicationEventPublisher", 
            ComponentType.EVENT_PUBLISHER, HealthStatus.UNKNOWN);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 检查ApplicationEventPublisher是否可用
            if (eventPublisher == null) {
                status.setStatus(HealthStatus.DOWN);
                status.setStatusMessage("ApplicationEventPublisher未注入");
                logger.error("ApplicationEventPublisher未注入");
                return status;
            }
            
            // 检查ApplicationContext是否可用
            if (applicationContext == null) {
                status.setStatus(HealthStatus.DOWN);
                status.setStatusMessage("ApplicationContext未注入");
                logger.error("ApplicationContext未注入");
                return status;
            }
            
            // 尝试发布测试事件
            boolean testResult = testEventPublishing();
            
            if (testResult) {
                status.setStatus(HealthStatus.UP);
                status.setStatusMessage("事件发布器运行正常");
                logger.info("ApplicationEventPublisher状态检查通过");
            } else {
                status.setStatus(HealthStatus.DEGRADED);
                status.setStatusMessage("事件发布测试失败");
                logger.warn("ApplicationEventPublisher测试失败");
            }
            
            // 设置响应时间
            long responseTime = System.currentTimeMillis() - startTime;
            status.setResponseTimeMs(responseTime);
            
            // 添加统计信息
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalEventsPublished", totalEventsPublished);
            metrics.put("successfulEvents", successfulEvents);
            metrics.put("failedEvents", failedEvents);
            metrics.put("successRate", calculateSuccessRate());
            status.setMetrics(metrics);
            
        } catch (Exception e) {
            logger.error("检查ApplicationEventPublisher状态时发生错误", e);
            status.setStatus(HealthStatus.DOWN);
            status.setStatusMessage("状态检查异常: " + e.getMessage());
        }
        
        return status;
    }
    
    @Override
    public List<Issue> checkDownloadStatusEventPublishing(Long downloadTaskId) {
        logger.info("检查DownloadStatusChangeEvent发布状态, downloadTaskId: {}", downloadTaskId);
        
        List<Issue> issues = new ArrayList<>();
        
        try {
            // 检查事件发布器是否可用
            if (eventPublisher == null) {
                Issue issue = new Issue("事件发布器不可用", 
                    "ApplicationEventPublisher未正确注入", 
                    IssueSeverity.CRITICAL, IssueCategory.EVENT_PROCESSING);
                issue.setComponentName("ApplicationEventPublisher");
                issue.setAutoFixable(false);
                issue.setSuggestedAction("检查Spring配置，确保ApplicationEventPublisher正确注入");
                issues.add(issue);
                return issues;
            }
            
            // 检查是否有相关的事件监听器
            String[] listenerBeans = applicationContext.getBeanNamesForType(Object.class);
            boolean hasWorkflowEventListener = false;
            
            for (String beanName : listenerBeans) {
                if (beanName.contains("WorkflowEventListener") || beanName.contains("workflowEventListener")) {
                    hasWorkflowEventListener = true;
                    break;
                }
            }
            
            if (!hasWorkflowEventListener) {
                Issue issue = new Issue("缺少工作流事件监听器", 
                    "未找到WorkflowEventListener，DownloadStatusChangeEvent可能无法被处理", 
                    IssueSeverity.HIGH, IssueCategory.EVENT_PROCESSING);
                issue.setComponentName("WorkflowEventListener");
                issue.setAutoFixable(false);
                issue.setSuggestedAction("确保WorkflowEventListener已正确配置并注册");
                issues.add(issue);
            }
            
            // 尝试发布测试事件
            try {
                DownloadStatusChangeEvent testEvent = DownloadStatusChangeEvent.create(
                    downloadTaskId, 1, 2);
                eventPublisher.publishEvent(testEvent);
                
                totalEventsPublished++;
                successfulEvents++;
                updateEventTypeStats("DownloadStatusChangeEvent");
                
                logger.debug("DownloadStatusChangeEvent测试发布成功");
                
            } catch (Exception e) {
                failedEvents++;
                Issue issue = new Issue("事件发布失败", 
                    "发布DownloadStatusChangeEvent时发生异常: " + e.getMessage(), 
                    IssueSeverity.HIGH, IssueCategory.EVENT_PROCESSING);
                issue.setComponentName("ApplicationEventPublisher");
                issue.setAutoFixable(false);
                issue.setSuggestedAction("检查事件发布逻辑和异常处理");
                issues.add(issue);
                
                logger.error("DownloadStatusChangeEvent发布失败", e);
            }
            
        } catch (Exception e) {
            logger.error("检查DownloadStatusChangeEvent发布状态时发生错误", e);
            
            Issue issue = new Issue("事件发布检查异常", 
                "检查过程中发生异常: " + e.getMessage(), 
                IssueSeverity.MEDIUM, IssueCategory.EVENT_PROCESSING);
            issue.setAutoFixable(false);
            issue.setSuggestedAction("检查系统日志，排查异常原因");
            issues.add(issue);
        }
        
        return issues;
    }
    
    @Override
    public List<Issue> validateEventPublishingConfiguration() {
        logger.info("验证事件发布配置");
        
        List<Issue> issues = new ArrayList<>();
        
        try {
            // 检查Spring事件配置
            if (applicationContext == null) {
                Issue issue = new Issue("Spring上下文不可用", 
                    "ApplicationContext未正确初始化", 
                    IssueSeverity.CRITICAL, IssueCategory.CONFIGURATION);
                issue.setAutoFixable(false);
                issue.setSuggestedAction("检查Spring Boot应用启动配置");
                issues.add(issue);
                return issues;
            }
            
            // 检查事件监听器配置
            String[] eventListenerBeans = applicationContext.getBeanNamesForAnnotation(
                org.springframework.context.event.EventListener.class);
            
            if (eventListenerBeans.length == 0) {
                Issue issue = new Issue("缺少事件监听器", 
                    "未找到任何@EventListener注解的Bean", 
                    IssueSeverity.MEDIUM, IssueCategory.CONFIGURATION);
                issue.setAutoFixable(false);
                issue.setSuggestedAction("确保事件监听器类已正确配置@EventListener注解");
                issues.add(issue);
            }
            
            // 检查异步事件处理配置
            try {
                Object asyncConfigurer = applicationContext.getBean("taskExecutor");
                if (asyncConfigurer == null) {
                    Issue issue = new Issue("异步执行器未配置", 
                        "未找到taskExecutor Bean，异步事件处理可能不可用", 
                        IssueSeverity.LOW, IssueCategory.CONFIGURATION);
                    issue.setAutoFixable(false);
                    issue.setSuggestedAction("考虑配置TaskExecutor以支持异步事件处理");
                    issues.add(issue);
                }
            } catch (Exception e) {
                // taskExecutor不是必需的，忽略异常
                logger.debug("未找到taskExecutor配置，这是正常的");
            }
            
        } catch (Exception e) {
            logger.error("验证事件发布配置时发生错误", e);
            
            Issue issue = new Issue("配置验证异常", 
                "验证过程中发生异常: " + e.getMessage(), 
                IssueSeverity.MEDIUM, IssueCategory.CONFIGURATION);
            issue.setAutoFixable(false);
            issue.setSuggestedAction("检查Spring配置和应用上下文");
            issues.add(issue);
        }
        
        return issues;
    }
    
    @Override
    public boolean testEventPublishing() {
        logger.debug("测试事件发布功能");
        
        try {
            if (eventPublisher == null) {
                logger.error("事件发布器不可用");
                return false;
            }
            
            // 创建测试事件
            TestEvent testEvent = new TestEvent(this, "诊断系统测试事件");
            
            // 发布测试事件
            eventPublisher.publishEvent(testEvent);
            
            totalEventsPublished++;
            successfulEvents++;
            updateEventTypeStats("TestEvent");
            
            logger.debug("测试事件发布成功");
            return true;
            
        } catch (Exception e) {
            logger.error("测试事件发布失败", e);
            failedEvents++;
            return false;
        }
    }
    
    @Override
    public EventPublishingStats getEventPublishingStats() {
        logger.debug("获取事件发布统计信息");
        
        EventPublishingStats stats = new EventPublishingStats();
        
        stats.setTotalEventsPublished(totalEventsPublished);
        stats.setSuccessfulEvents(successfulEvents);
        stats.setFailedEvents(failedEvents);
        stats.setSuccessRate(calculateSuccessRate());
        stats.setEventTypeDistribution(new HashMap<>(eventTypeStats));
        
        // 模拟性能数据（实际应用中应该从监控系统获取）
        stats.setAveragePublishTimeMs(5L);
        stats.setMaxPublishTimeMs(50L);
        stats.setMinPublishTimeMs(1L);
        
        // 获取监听器数量
        if (applicationContext != null) {
            String[] listenerBeans = applicationContext.getBeanNamesForAnnotation(
                org.springframework.context.event.EventListener.class);
            stats.setTotalListeners(listenerBeans.length);
            stats.setActiveListeners(listenerBeans.length); // 假设所有监听器都是活跃的
        }
        
        return stats;
    }
    
    // 私有辅助方法
    
    private double calculateSuccessRate() {
        if (totalEventsPublished == 0) {
            return 0.0;
        }
        return (double) successfulEvents / totalEventsPublished * 100.0;
    }
    
    private void updateEventTypeStats(String eventType) {
        eventTypeStats.put(eventType, eventTypeStats.getOrDefault(eventType, 0L) + 1);
    }
    
    /**
     * 测试事件类
     */
    private static class TestEvent {
        private final Object source;
        private final String message;
        
        public TestEvent(Object source, String message) {
            this.source = source;
            this.message = message;
        }
        
        public Object getSource() {
            return source;
        }
        
        public String getMessage() {
            return message;
        }
    }
}