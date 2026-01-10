package com.tbw.cut.service;

import com.tbw.cut.diagnostic.model.ComponentStatus;
import com.tbw.cut.diagnostic.model.Issue;
import com.tbw.cut.diagnostic.model.EventPublishingStats;
import java.util.List;

/**
 * 事件发布检查器接口
 * 用于检查事件发布机制的健康状态
 */
public interface EventPublishChecker {
    
    /**
     * 检查ApplicationEventPublisher状态
     * 
     * @return 组件状态
     */
    ComponentStatus checkEventPublisherStatus();
    
    /**
     * 检查DownloadStatusChangeEvent是否正确发布
     * 
     * @param downloadTaskId 下载任务ID
     * @return 检查结果问题列表
     */
    List<Issue> checkDownloadStatusEventPublishing(Long downloadTaskId);
    
    /**
     * 验证事件发布配置
     * 
     * @return 配置验证问题列表
     */
    List<Issue> validateEventPublishingConfiguration();
    
    /**
     * 测试事件发布功能
     * 
     * @return 测试结果
     */
    boolean testEventPublishing();
    
    /**
     * 获取事件发布统计信息
     * 
     * @return 统计信息
     */
    EventPublishingStats getEventPublishingStats();
}