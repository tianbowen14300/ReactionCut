package com.tbw.cut.service;

import com.tbw.cut.workflow.model.WorkflowConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流配置服务
 * 
 * 管理工作流配置的临时存储，用于在下载完成后启动工作流时获取配置
 * 使用内存存储（ConcurrentHashMap）替代Redis
 */
@Service
@Slf4j
public class WorkflowConfigurationService {
    
    /**
     * 配置存储容器
     * Key: taskId
     * Value: 配置包装对象（包含配置和过期时间）
     */
    private final ConcurrentHashMap<String, ConfigEntry> configStore = new ConcurrentHashMap<>();
    
    /**
     * 配置过期时间（小时）
     */
    private static final long CONFIG_EXPIRY_HOURS = 24;
    
    /**
     * 清理间隔（毫秒）- 每小时清理一次过期配置
     */
    private static final long CLEANUP_INTERVAL_MS = 60 * 60 * 1000;
    
    /**
     * 配置条目包装类
     */
    private static class ConfigEntry {
        final WorkflowConfig config;
        final LocalDateTime expiryTime;
        
        ConfigEntry(WorkflowConfig config, long expiryHours) {
            this.config = config;
            this.expiryTime = LocalDateTime.now().plusHours(expiryHours);
        }
        
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
        
        long getRemainingSeconds() {
            if (isExpired()) {
                return -1;
            }
            return java.time.Duration.between(LocalDateTime.now(), expiryTime).getSeconds();
        }
    }
    
    @PostConstruct
    public void init() {
        log.info("工作流配置服务初始化完成，使用内存存储");
    }
    
    /**
     * 保存任务的工作流配置
     * 
     * @param taskId 任务ID
     * @param config 工作流配置
     */
    public void saveConfigForTask(String taskId, WorkflowConfig config) {
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("无法保存工作流配置: taskId为空");
            return;
        }
        
        if (config == null) {
            log.error("无法保存工作流配置: config为空, taskId={}", taskId);
            return;
        }
        
        try {
            // 验证配置有效性
            if (!config.isValid()) {
                log.error("工作流配置无效，无法保存: taskId={}, error={}", taskId, config.getValidationError());
                return;
            }
            
            // 保存到内存存储
            ConfigEntry entry = new ConfigEntry(config, CONFIG_EXPIRY_HOURS);
            configStore.put(taskId, entry);
            
            log.info("成功保存工作流配置: taskId={}, expiry={}小时", taskId, CONFIG_EXPIRY_HOURS);
            log.debug("保存的工作流配置详情: taskId={}, config={}", taskId, config);
            
        } catch (Exception e) {
            log.error("保存工作流配置失败: taskId={}", taskId, e);
        }
    }
    
    /**
     * 获取任务的工作流配置
     * 
     * @param taskId 任务ID
     * @return 工作流配置，如果不存在或已过期则返回null
     */
    public WorkflowConfig getConfigForTask(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("无法获取工作流配置: taskId为空");
            return null;
        }
        
        try {
            ConfigEntry entry = configStore.get(taskId);
            
            if (entry == null) {
                log.warn("未找到工作流配置: taskId={}", taskId);
                return null;
            }
            
            // 检查是否过期
            if (entry.isExpired()) {
                log.warn("工作流配置已过期: taskId={}", taskId);
                configStore.remove(taskId);
                return null;
            }
            
            log.debug("成功获取工作流配置: taskId={}, config={}", taskId, entry.config);
            return entry.config;
            
        } catch (Exception e) {
            log.error("获取工作流配置失败: taskId={}", taskId, e);
            return null;
        }
    }
    
    /**
     * 删除任务的工作流配置
     * 
     * @param taskId 任务ID
     * @return true 如果删除成功，false 否则
     */
    public boolean removeConfigForTask(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("无法删除工作流配置: taskId为空");
            return false;
        }
        
        try {
            ConfigEntry removed = configStore.remove(taskId);
            
            if (removed != null) {
                log.info("成功删除工作流配置: taskId={}", taskId);
                return true;
            } else {
                log.warn("工作流配置不存在或删除失败: taskId={}", taskId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("删除工作流配置失败: taskId={}", taskId, e);
            return false;
        }
    }
    
    /**
     * 检查任务是否存在工作流配置
     * 
     * @param taskId 任务ID
     * @return true 如果存在有效配置，false 否则
     */
    public boolean hasConfigForTask(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return false;
        }
        
        try {
            ConfigEntry entry = configStore.get(taskId);
            
            if (entry == null) {
                return false;
            }
            
            // 检查是否过期
            if (entry.isExpired()) {
                configStore.remove(taskId);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("检查工作流配置存在性失败: taskId={}", taskId, e);
            return false;
        }
    }
    
    /**
     * 更新任务的工作流配置
     * 
     * @param taskId 任务ID
     * @param config 新的工作流配置
     * @return true 如果更新成功，false 否则
     */
    public boolean updateConfigForTask(String taskId, WorkflowConfig config) {
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("无法更新工作流配置: taskId为空");
            return false;
        }
        
        if (config == null) {
            log.error("无法更新工作流配置: config为空, taskId={}", taskId);
            return false;
        }
        
        try {
            // 检查配置是否存在
            if (!hasConfigForTask(taskId)) {
                log.warn("工作流配置不存在，无法更新: taskId={}", taskId);
                return false;
            }
            
            // 验证新配置有效性
            if (!config.isValid()) {
                log.error("新的工作流配置无效，无法更新: taskId={}, error={}", taskId, config.getValidationError());
                return false;
            }
            
            // 更新配置
            saveConfigForTask(taskId, config);
            log.info("成功更新工作流配置: taskId={}", taskId);
            return true;
            
        } catch (Exception e) {
            log.error("更新工作流配置失败: taskId={}", taskId, e);
            return false;
        }
    }
    
    /**
     * 延长任务配置的过期时间
     * 
     * @param taskId 任务ID
     * @param hours 延长的小时数
     * @return true 如果延长成功，false 否则
     */
    public boolean extendConfigExpiry(String taskId, long hours) {
        if (taskId == null || taskId.trim().isEmpty()) {
            log.error("无法延长配置过期时间: taskId为空");
            return false;
        }
        
        if (hours <= 0) {
            log.error("无法延长配置过期时间: hours必须大于0, taskId={}, hours={}", taskId, hours);
            return false;
        }
        
        try {
            ConfigEntry entry = configStore.get(taskId);
            
            if (entry == null || entry.isExpired()) {
                log.warn("工作流配置不存在或已过期，无法延长过期时间: taskId={}", taskId);
                return false;
            }
            
            // 创建新的配置条目，延长过期时间
            ConfigEntry newEntry = new ConfigEntry(entry.config, hours);
            configStore.put(taskId, newEntry);
            
            log.info("成功延长工作流配置过期时间: taskId={}, hours={}", taskId, hours);
            return true;
            
        } catch (Exception e) {
            log.error("延长工作流配置过期时间失败: taskId={}, hours={}", taskId, hours, e);
            return false;
        }
    }
    
    /**
     * 获取任务配置的剩余过期时间
     * 
     * @param taskId 任务ID
     * @return 剩余过期时间（秒），如果配置不存在或已过期则返回-1
     */
    public long getConfigTtl(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return -1;
        }
        
        try {
            ConfigEntry entry = configStore.get(taskId);
            
            if (entry == null) {
                return -1;
            }
            
            return entry.getRemainingSeconds();
            
        } catch (Exception e) {
            log.error("获取工作流配置过期时间失败: taskId={}", taskId, e);
            return -1;
        }
    }
    
    /**
     * 清理过期的配置（定时任务）
     * 每小时执行一次
     * 
     * @return 删除的配置数量
     */
    @Scheduled(fixedRate = CLEANUP_INTERVAL_MS)
    public int cleanupExpiredConfigs() {
        try {
            log.info("开始清理过期的工作流配置");
            
            int removedCount = 0;
            
            for (Map.Entry<String, ConfigEntry> entry : configStore.entrySet()) {
                if (entry.getValue().isExpired()) {
                    configStore.remove(entry.getKey());
                    removedCount++;
                    log.debug("清理过期配置: taskId={}", entry.getKey());
                }
            }
            
            log.info("工作流配置清理完成，删除了{}个过期配置，当前存储数量: {}", removedCount, configStore.size());
            return removedCount;
            
        } catch (Exception e) {
            log.error("清理过期工作流配置失败", e);
            return -1;
        }
    }
    
    /**
     * 获取当前存储的配置数量
     * 
     * @return 配置数量
     */
    public int getConfigCount() {
        return configStore.size();
    }
    
    /**
     * 清空所有配置（用于测试或重置）
     */
    public void clearAllConfigs() {
        configStore.clear();
        log.info("已清空所有工作流配置");
    }
}
