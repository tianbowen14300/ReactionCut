package com.tbw.cut.workflow.service;

import com.tbw.cut.workflow.model.WorkflowConfig;

/**
 * 工作流配置管理器接口
 * 负责工作流配置的持久化、验证和管理
 */
public interface ConfigurationManager {
    
    /**
     * 保存工作流配置
     * 
     * @param userId 用户ID
     * @param config 工作流配置
     * @throws IllegalArgumentException 当配置无效时
     */
    void saveWorkflowConfig(String userId, WorkflowConfig config);
    
    /**
     * 加载用户的工作流配置
     * 如果用户没有配置，返回默认配置
     * 
     * @param userId 用户ID
     * @return 工作流配置
     */
    WorkflowConfig loadWorkflowConfig(String userId);
    
    /**
     * 获取系统默认配置
     * 
     * @return 默认工作流配置
     */
    WorkflowConfig getDefaultConfig();
    
    /**
     * 获取下载+投稿场景的默认配置
     * 
     * @return 下载+投稿默认配置
     */
    WorkflowConfig getDownloadSubmissionDefaultConfig();
    
    /**
     * 验证工作流配置
     * 
     * @param config 要验证的配置
     * @return 配置是否有效
     */
    boolean validateConfig(WorkflowConfig config);
    
    /**
     * 获取配置验证错误信息
     * 
     * @param config 要验证的配置
     * @return 验证错误信息，如果配置有效则返回null
     */
    String getValidationError(WorkflowConfig config);
    
    /**
     * 重置用户配置为默认值
     * 
     * @param userId 用户ID
     */
    void resetToDefault(String userId);
    
    /**
     * 检查用户是否有自定义配置
     * 
     * @param userId 用户ID
     * @return 是否有自定义配置
     */
    boolean hasCustomConfig(String userId);
    
    /**
     * 删除用户的自定义配置
     * 
     * @param userId 用户ID
     */
    void deleteUserConfig(String userId);
}