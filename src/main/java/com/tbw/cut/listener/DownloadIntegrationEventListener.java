package com.tbw.cut.listener;

import com.tbw.cut.event.DownloadStatusChangeEvent;
import com.tbw.cut.service.IntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 下载集成事件监听器
 * 监听下载状态变化事件，调用集成服务进行状态同步
 * 
 * 这个监听器的作用是打破循环依赖：
 * 下载服务 -> 发布事件 -> 此监听器 -> 集成服务
 * 而不是：下载服务 -> 直接依赖集成服务（会造成循环依赖）
 */
@Slf4j
@Component
public class DownloadIntegrationEventListener {
    
    @Autowired
    private IntegrationService integrationService;
    
    /**
     * 监听下载状态变化事件，调用集成服务同步状态
     */
    @EventListener
    @Async
    public void handleDownloadStatusChangeForIntegration(DownloadStatusChangeEvent event) {
        log.debug("Received download status change event for integration: taskId={}, newStatus={}", 
                event.getTaskId(), event.getNewStatus());
        
        try {
            // 调用集成服务同步状态，这会触发后续的工作流步骤
            integrationService.syncTaskStatus(event.getTaskId(), event.getNewStatus());
            log.info("Successfully synced download status to integration service: taskId={}, status={}", 
                    event.getTaskId(), event.getNewStatus());
        } catch (Exception e) {
            log.error("Failed to sync download status to integration service: taskId={}, status={}", 
                    event.getTaskId(), event.getNewStatus(), e);
            // 不抛出异常，避免影响其他事件监听器
        }
    }
}