package com.tbw.cut.service;

import com.tbw.cut.entity.DownloadTask;
import com.tbw.cut.service.impl.EnhancedFrontendPartDownloadServiceImpl;
import com.tbw.cut.service.impl.FrontendPartDownloadServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 下载服务选择器
 * 根据配置选择使用原始下载服务还是增强下载服务
 */
@Slf4j
@Service
public class DownloadServiceSelector {
    
    @Value("${download.enable-enhanced:true}")
    private boolean enableEnhanced;
    
    @Autowired
    private FrontendPartDownloadServiceImpl originalDownloadService;
    
    @Autowired
    private EnhancedFrontendPartDownloadServiceImpl enhancedDownloadService;
    
    /**
     * 选择合适的下载服务执行下载任务
     * @param downloadTask 下载任务
     */
    public void executeDownload(DownloadTask downloadTask) {
        if (enableEnhanced) {
            log.info("使用增强下载服务执行任务: taskId={}", downloadTask.getId());
            try {
                enhancedDownloadService.executeDownload(downloadTask);
            } catch (Exception e) {
                log.error("增强下载服务执行失败，回退到原始服务: taskId={}", downloadTask.getId(), e);
                // 回退到原始服务
                originalDownloadService.executeDownload(downloadTask);
            }
        } else {
            log.info("使用原始下载服务执行任务: taskId={}", downloadTask.getId());
            originalDownloadService.executeDownload(downloadTask);
        }
    }
    
    /**
     * 添加下载任务
     * @param downloadTask 下载任务
     */
    public void addDownloadTask(DownloadTask downloadTask) {
        if (enableEnhanced) {
            log.info("使用增强下载服务添加任务: taskId={}", downloadTask.getId());
            enhancedDownloadService.addDownloadTask(downloadTask);
        } else {
            log.info("使用原始下载服务添加任务: taskId={}", downloadTask.getId());
            originalDownloadService.addDownloadTask(downloadTask);
        }
    }
    
    /**
     * 检查是否启用了增强功能
     * @return 是否启用增强功能
     */
    public boolean isEnhancedEnabled() {
        return enableEnhanced;
    }
    
    /**
     * 动态切换下载服务
     * @param enhanced 是否启用增强功能
     */
    public void setEnhanced(boolean enhanced) {
        this.enableEnhanced = enhanced;
        log.info("下载服务切换为: {}", enhanced ? "增强模式" : "原始模式");
    }
}