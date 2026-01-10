package com.tbw.cut.workflow.service.impl;

import com.tbw.cut.workflow.service.ConfigurationManager;
import com.tbw.cut.workflow.model.WorkflowConfig;
import com.tbw.cut.workflow.model.SegmentationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 工作流配置管理器实现
 * 
 * 当前实现使用内存存储，后续可以扩展为数据库存储
 */
@Slf4j
@Service
public class ConfigurationManagerImpl implements ConfigurationManager {
    
    /**
     * 用户配置缓存
     * TODO: 后续替换为数据库存储
     */
    private final Map<String, WorkflowConfig> userConfigs = new ConcurrentHashMap<>();
    
    /**
     * 系统默认配置
     */
    private static final WorkflowConfig SYSTEM_DEFAULT_CONFIG = WorkflowConfig.builder()
            .userId("system")
            .enableDirectSubmission(false)
            .enableClipping(true)
            .enableMerging(true)
            .segmentationConfig(SegmentationConfig.builder()
                    .enabled(true)
                    .segmentDurationSeconds(133)
                    .maxSegmentCount(50)
                    .segmentNamingPattern("{title}_Part{index}")
                    .preserveOriginal(true)
                    .build())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    
    /**
     * 下载+投稿默认配置
     */
    private static final WorkflowConfig DOWNLOAD_SUBMISSION_DEFAULT_CONFIG = WorkflowConfig.builder()
            .userId("system")
            .enableDirectSubmission(true)
            .enableClipping(true)
            .enableMerging(true)
            .segmentationConfig(SegmentationConfig.builder()
                    .enabled(false)
                    .segmentDurationSeconds(133)
                    .maxSegmentCount(50)
                    .segmentNamingPattern("{title}_Part{index}")
                    .preserveOriginal(true)
                    .build())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    
    @Override
    public void saveWorkflowConfig(String userId, WorkflowConfig config) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        if (config == null) {
            throw new IllegalArgumentException("配置不能为空");
        }
        
        // 验证配置
        if (!validateConfig(config)) {
            String error = getValidationError(config);
            throw new IllegalArgumentException("配置验证失败: " + error);
        }
        
        // 设置用户ID和时间戳
        config.setUserId(userId);
        config.updateTimestamp();
        
        // 保存配置
        userConfigs.put(userId, config);
        
        log.info("保存用户配置成功: userId={}, enableDirectSubmission={}, segmentationEnabled={}", 
                userId, config.isEnableDirectSubmission(), 
                config.getSegmentationConfig() != null ? config.getSegmentationConfig().isEnabled() : false);
    }
    
    @Override
    public WorkflowConfig loadWorkflowConfig(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("用户ID为空，返回系统默认配置");
            return getDefaultConfig();
        }
        
        WorkflowConfig config = userConfigs.get(userId);
        if (config == null) {
            log.debug("用户{}没有自定义配置，返回默认配置", userId);
            config = getDefaultConfig();
            config.setUserId(userId);
        }
        
        return config;
    }
    
    @Override
    public WorkflowConfig getDefaultConfig() {
        // 返回系统默认配置的副本
        return WorkflowConfig.builder()
                .userId("system")
                .enableDirectSubmission(SYSTEM_DEFAULT_CONFIG.isEnableDirectSubmission())
                .enableClipping(SYSTEM_DEFAULT_CONFIG.isEnableClipping())
                .enableMerging(SYSTEM_DEFAULT_CONFIG.isEnableMerging())
                .segmentationConfig(copySegmentationConfig(SYSTEM_DEFAULT_CONFIG.getSegmentationConfig()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Override
    public WorkflowConfig getDownloadSubmissionDefaultConfig() {
        // 返回下载+投稿默认配置的副本
        return WorkflowConfig.builder()
                .userId("system")
                .enableDirectSubmission(DOWNLOAD_SUBMISSION_DEFAULT_CONFIG.isEnableDirectSubmission())
                .enableClipping(DOWNLOAD_SUBMISSION_DEFAULT_CONFIG.isEnableClipping())
                .enableMerging(DOWNLOAD_SUBMISSION_DEFAULT_CONFIG.isEnableMerging())
                .segmentationConfig(copySegmentationConfig(DOWNLOAD_SUBMISSION_DEFAULT_CONFIG.getSegmentationConfig()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Override
    public boolean validateConfig(WorkflowConfig config) {
        if (config == null) {
            return false;
        }
        
        return config.isValid();
    }
    
    @Override
    public String getValidationError(WorkflowConfig config) {
        if (config == null) {
            return "配置不能为空";
        }
        
        return config.getValidationError();
    }
    
    @Override
    public void resetToDefault(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        userConfigs.remove(userId);
        log.info("重置用户{}的配置为默认值", userId);
    }
    
    @Override
    public boolean hasCustomConfig(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        return userConfigs.containsKey(userId);
    }
    
    @Override
    public void deleteUserConfig(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        
        WorkflowConfig removed = userConfigs.remove(userId);
        if (removed != null) {
            log.info("删除用户{}的自定义配置", userId);
        }
    }
    
    /**
     * 复制分段配置
     */
    private SegmentationConfig copySegmentationConfig(SegmentationConfig original) {
        if (original == null) {
            return SegmentationConfig.createDefault();
        }
        
        return SegmentationConfig.builder()
                .enabled(original.isEnabled())
                .segmentDurationSeconds(original.getSegmentDurationSeconds())
                .maxSegmentCount(original.getMaxSegmentCount())
                .segmentNamingPattern(original.getSegmentNamingPattern())
                .preserveOriginal(original.isPreserveOriginal())
                .build();
    }
    
    /**
     * 获取所有用户配置（用于调试和管理）
     */
    public Map<String, WorkflowConfig> getAllUserConfigs() {
        return new ConcurrentHashMap<>(userConfigs);
    }
    
    /**
     * 清空所有用户配置（用于测试）
     */
    public void clearAllConfigs() {
        userConfigs.clear();
        log.info("清空所有用户配置");
    }
}