package com.tbw.cut.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流配置实体类
 * 
 * 对应数据库表：workflow_configurations
 */
@Data
public class WorkflowConfiguration {
    
    /**
     * 配置ID
     */
    private Long configId;
    
    /**
     * 配置名称
     */
    private String configName;
    
    /**
     * 配置类型
     */
    private ConfigType configType;
    
    /**
     * 用户ID (用户模板时使用)
     */
    private Long userId;
    
    /**
     * 工作流类型
     */
    private String workflowType;
    
    /**
     * 配置数据 (JSON格式)
     */
    private String configurationData;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 是否激活
     */
    private Boolean isActive;
    
    /**
     * 配置版本
     */
    private Integer version;
    
    /**
     * 创建者用户ID
     */
    private Long createdBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 配置类型枚举
     */
    public enum ConfigType {
        SYSTEM_DEFAULT("系统默认"),
        USER_TEMPLATE("用户模板"),
        INSTANCE_SPECIFIC("实例专用");
        
        private final String description;
        
        ConfigType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    /**
     * 检查是否为系统默认配置
     * 
     * @return true如果是系统默认配置，false如果不是
     */
    public boolean isSystemDefault() {
        return configType == ConfigType.SYSTEM_DEFAULT;
    }
    
    /**
     * 检查是否为用户模板
     * 
     * @return true如果是用户模板，false如果不是
     */
    public boolean isUserTemplate() {
        return configType == ConfigType.USER_TEMPLATE;
    }
    
    /**
     * 检查是否为实例专用配置
     * 
     * @return true如果是实例专用配置，false如果不是
     */
    public boolean isInstanceSpecific() {
        return configType == ConfigType.INSTANCE_SPECIFIC;
    }
    
    /**
     * 检查配置是否激活
     * 
     * @return true如果激活，false如果未激活
     */
    public boolean isActive() {
        return isActive != null && isActive;
    }
    
    /**
     * 检查配置是否属于指定用户
     * 
     * @param userId 用户ID
     * @return true如果属于指定用户，false如果不属于
     */
    public boolean belongsToUser(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }
    
    /**
     * 检查配置是否可以被指定用户编辑
     * 
     * @param userId 用户ID
     * @return true如果可以编辑，false如果不能编辑
     */
    public boolean canBeEditedBy(Long userId) {
        if (isSystemDefault()) {
            return false; // 系统默认配置不能编辑
        }
        
        if (isUserTemplate()) {
            return belongsToUser(userId); // 用户模板只能由所有者编辑
        }
        
        return true; // 实例专用配置可以编辑
    }
    
    /**
     * 检查配置是否可以被删除
     * 
     * @return true如果可以删除，false如果不能删除
     */
    public boolean canBeDeleted() {
        return !isSystemDefault(); // 系统默认配置不能删除
    }
    
    /**
     * 获取配置类型描述
     * 
     * @return 配置类型的中文描述
     */
    public String getConfigTypeDescription() {
        return configType != null ? configType.getDescription() : "未知类型";
    }
    
    /**
     * 获取版本描述
     * 
     * @return 版本的格式化字符串
     */
    public String getVersionDescription() {
        return version != null ? "v" + version : "v1";
    }
    
    /**
     * 获取状态描述
     * 
     * @return 状态的中文描述
     */
    public String getStatusDescription() {
        return isActive() ? "激活" : "未激活";
    }
    
    /**
     * 创建系统默认配置
     * 
     * @param configName 配置名称
     * @param workflowType 工作流类型
     * @param configurationData 配置数据
     * @param description 配置描述
     * @return 系统默认配置实例
     */
    public static WorkflowConfiguration createSystemDefault(String configName, String workflowType, 
                                                           String configurationData, String description) {
        WorkflowConfiguration config = new WorkflowConfiguration();
        config.setConfigName(configName);
        config.setConfigType(ConfigType.SYSTEM_DEFAULT);
        config.setWorkflowType(workflowType);
        config.setConfigurationData(configurationData);
        config.setDescription(description);
        config.setIsActive(true);
        config.setVersion(1);
        return config;
    }
    
    /**
     * 创建用户模板配置
     * 
     * @param configName 配置名称
     * @param userId 用户ID
     * @param workflowType 工作流类型
     * @param configurationData 配置数据
     * @param description 配置描述
     * @return 用户模板配置实例
     */
    public static WorkflowConfiguration createUserTemplate(String configName, Long userId, String workflowType, 
                                                          String configurationData, String description) {
        WorkflowConfiguration config = new WorkflowConfiguration();
        config.setConfigName(configName);
        config.setConfigType(ConfigType.USER_TEMPLATE);
        config.setUserId(userId);
        config.setWorkflowType(workflowType);
        config.setConfigurationData(configurationData);
        config.setDescription(description);
        config.setIsActive(true);
        config.setVersion(1);
        config.setCreatedBy(userId);
        return config;
    }
    
    /**
     * 创建实例专用配置
     * 
     * @param configName 配置名称
     * @param workflowType 工作流类型
     * @param configurationData 配置数据
     * @param createdBy 创建者用户ID
     * @return 实例专用配置实例
     */
    public static WorkflowConfiguration createInstanceSpecific(String configName, String workflowType, 
                                                              String configurationData, Long createdBy) {
        WorkflowConfiguration config = new WorkflowConfiguration();
        config.setConfigName(configName);
        config.setConfigType(ConfigType.INSTANCE_SPECIFIC);
        config.setWorkflowType(workflowType);
        config.setConfigurationData(configurationData);
        config.setIsActive(true);
        config.setVersion(1);
        config.setCreatedBy(createdBy);
        return config;
    }
    
    @Override
    public String toString() {
        return String.format(
            "WorkflowConfiguration{configId=%d, name='%s', type=%s, workflowType='%s', version=%d, active=%s}",
            configId, configName, configType, workflowType, version, isActive()
        );
    }
}