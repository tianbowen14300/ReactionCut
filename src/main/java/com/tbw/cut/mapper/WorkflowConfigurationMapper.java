package com.tbw.cut.mapper;

import com.tbw.cut.entity.WorkflowConfiguration;
import com.tbw.cut.entity.WorkflowConfiguration.ConfigType;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 工作流配置数据访问层
 * 
 * 提供工作流配置的CRUD操作和查询功能
 */
@Mapper
public interface WorkflowConfigurationMapper {
    
    /**
     * 插入新的工作流配置
     * 
     * @param workflowConfiguration 工作流配置
     * @return 影响的行数
     */
    @Insert("INSERT INTO workflow_configurations (" +
            "config_name, config_type, user_id, workflow_type, configuration_data, " +
            "description, is_active, version, created_by, created_at, updated_at" +
            ") VALUES (" +
            "#{configName}, #{configType}, #{userId}, #{workflowType}, #{configurationData}, " +
            "#{description}, #{isActive}, #{version}, #{createdBy}, #{createdAt}, #{updatedAt}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "configId")
    int insert(WorkflowConfiguration workflowConfiguration);
    
    /**
     * 根据配置ID查询工作流配置
     * 
     * @param configId 配置ID
     * @return 工作流配置
     */
    @Select("SELECT config_id, config_name, config_type, user_id, workflow_type, " +
            "configuration_data, description, is_active, version, created_by, " +
            "created_at, updated_at " +
            "FROM workflow_configurations " +
            "WHERE config_id = #{configId}")
    WorkflowConfiguration selectByConfigId(Long configId);
    
    /**
     * 根据配置名称和类型查询工作流配置
     * 
     * @param configName 配置名称
     * @param configType 配置类型
     * @return 工作流配置
     */
    @Select("SELECT config_id, config_name, config_type, user_id, workflow_type, " +
            "configuration_data, description, is_active, version, created_by, " +
            "created_at, updated_at " +
            "FROM workflow_configurations " +
            "WHERE config_name = #{configName} AND config_type = #{configType}")
    WorkflowConfiguration selectByNameAndType(@Param("configName") String configName, 
                                            @Param("configType") ConfigType configType);
    
    /**
     * 根据用户ID查询用户的配置模板
     * 
     * @param userId 用户ID
     * @return 用户配置模板列表
     */
    @Select("SELECT config_id, config_name, config_type, user_id, workflow_type, " +
            "configuration_data, description, is_active, version, created_by, " +
            "created_at, updated_at " +
            "FROM workflow_configurations " +
            "WHERE user_id = #{userId} AND config_type = 'USER_TEMPLATE' " +
            "ORDER BY created_at DESC")
    List<WorkflowConfiguration> selectUserTemplates(Long userId);
    
    /**
     * 根据工作流类型查询系统默认配置
     * 
     * @param workflowType 工作流类型
     * @return 系统默认配置列表
     */
    @Select("SELECT config_id, config_name, config_type, user_id, workflow_type, " +
            "configuration_data, description, is_active, version, created_by, " +
            "created_at, updated_at " +
            "FROM workflow_configurations " +
            "WHERE workflow_type = #{workflowType} AND config_type = 'SYSTEM_DEFAULT' AND is_active = true " +
            "ORDER BY version DESC")
    List<WorkflowConfiguration> selectSystemDefaults(String workflowType);
    
    /**
     * 根据配置类型查询工作流配置
     * 
     * @param configType 配置类型
     * @return 工作流配置列表
     */
    @Select("SELECT config_id, config_name, config_type, user_id, workflow_type, " +
            "configuration_data, description, is_active, version, created_by, " +
            "created_at, updated_at " +
            "FROM workflow_configurations " +
            "WHERE config_type = #{configType} " +
            "ORDER BY created_at DESC")
    List<WorkflowConfiguration> selectByConfigType(ConfigType configType);
    
    /**
     * 查询激活的工作流配置
     * 
     * @return 激活的工作流配置列表
     */
    @Select("SELECT config_id, config_name, config_type, user_id, workflow_type, " +
            "configuration_data, description, is_active, version, created_by, " +
            "created_at, updated_at " +
            "FROM workflow_configurations " +
            "WHERE is_active = true " +
            "ORDER BY config_type, created_at DESC")
    List<WorkflowConfiguration> selectActiveConfigurations();
    
    /**
     * 查询所有工作流配置
     * 
     * @return 工作流配置列表
     */
    @Select("SELECT config_id, config_name, config_type, user_id, workflow_type, " +
            "configuration_data, description, is_active, version, created_by, " +
            "created_at, updated_at " +
            "FROM workflow_configurations " +
            "ORDER BY config_type, created_at DESC")
    List<WorkflowConfiguration> selectAll();
    
    /**
     * 分页查询工作流配置
     * 
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 工作流配置列表
     */
    @Select("SELECT config_id, config_name, config_type, user_id, workflow_type, " +
            "configuration_data, description, is_active, version, created_by, " +
            "created_at, updated_at " +
            "FROM workflow_configurations " +
            "ORDER BY config_type, created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<WorkflowConfiguration> selectWithPagination(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 统计工作流配置总数
     * 
     * @return 总数
     */
    @Select("SELECT COUNT(*) FROM workflow_configurations")
    int countAll();
    
    /**
     * 根据配置类型统计工作流配置数量
     * 
     * @param configType 配置类型
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM workflow_configurations WHERE config_type = #{configType}")
    int countByConfigType(ConfigType configType);
    
    /**
     * 根据用户ID统计用户模板数量
     * 
     * @param userId 用户ID
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM workflow_configurations WHERE user_id = #{userId} AND config_type = 'USER_TEMPLATE'")
    int countUserTemplates(Long userId);
    
    /**
     * 更新工作流配置
     * 
     * @param workflowConfiguration 工作流配置
     * @return 影响的行数
     */
    @Update("UPDATE workflow_configurations " +
            "SET config_name = #{configName}, config_type = #{configType}, user_id = #{userId}, " +
            "workflow_type = #{workflowType}, configuration_data = #{configurationData}, " +
            "description = #{description}, is_active = #{isActive}, version = #{version}, " +
            "created_by = #{createdBy}, updated_at = NOW() " +
            "WHERE config_id = #{configId}")
    int update(WorkflowConfiguration workflowConfiguration);
    
    /**
     * 更新配置数据
     * 
     * @param configId 配置ID
     * @param configurationData 配置数据
     * @return 影响的行数
     */
    @Update("UPDATE workflow_configurations " +
            "SET configuration_data = #{configurationData}, updated_at = NOW() " +
            "WHERE config_id = #{configId}")
    int updateConfigurationData(@Param("configId") Long configId, 
                               @Param("configurationData") String configurationData);
    
    /**
     * 更新配置激活状态
     * 
     * @param configId 配置ID
     * @param isActive 是否激活
     * @return 影响的行数
     */
    @Update("UPDATE workflow_configurations " +
            "SET is_active = #{isActive}, updated_at = NOW() " +
            "WHERE config_id = #{configId}")
    int updateActiveStatus(@Param("configId") Long configId, @Param("isActive") Boolean isActive);
    
    /**
     * 更新配置版本
     * 
     * @param configId 配置ID
     * @param version 新版本
     * @return 影响的行数
     */
    @Update("UPDATE workflow_configurations " +
            "SET version = #{version}, updated_at = NOW() " +
            "WHERE config_id = #{configId}")
    int updateVersion(@Param("configId") Long configId, @Param("version") Integer version);
    
    /**
     * 删除工作流配置
     * 
     * @param configId 配置ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM workflow_configurations WHERE config_id = #{configId}")
    int deleteByConfigId(Long configId);
    
    /**
     * 根据用户ID删除用户模板
     * 
     * @param userId 用户ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM workflow_configurations WHERE user_id = #{userId} AND config_type = 'USER_TEMPLATE'")
    int deleteUserTemplates(Long userId);
    
    /**
     * 查询用户的特定工作流类型配置
     * 
     * @param userId 用户ID
     * @param workflowType 工作流类型
     * @return 用户配置列表
     */
    @Select("SELECT config_id, config_name, config_type, user_id, workflow_type, " +
            "configuration_data, description, is_active, version, created_by, " +
            "created_at, updated_at " +
            "FROM workflow_configurations " +
            "WHERE user_id = #{userId} AND workflow_type = #{workflowType} AND config_type = 'USER_TEMPLATE' " +
            "ORDER BY created_at DESC")
    List<WorkflowConfiguration> selectUserConfigsByWorkflowType(@Param("userId") Long userId, 
                                                               @Param("workflowType") String workflowType);
    
    /**
     * 查询最新版本的系统默认配置
     * 
     * @param workflowType 工作流类型
     * @return 最新版本的系统默认配置
     */
    @Select("SELECT config_id, config_name, config_type, user_id, workflow_type, " +
            "configuration_data, description, is_active, version, created_by, " +
            "created_at, updated_at " +
            "FROM workflow_configurations " +
            "WHERE workflow_type = #{workflowType} AND config_type = 'SYSTEM_DEFAULT' AND is_active = true " +
            "ORDER BY version DESC " +
            "LIMIT 1")
    WorkflowConfiguration selectLatestSystemDefault(String workflowType);
    
    /**
     * 检查配置名称是否存在（用户模板）
     * 
     * @param userId 用户ID
     * @param configName 配置名称
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) > 0 " +
            "FROM workflow_configurations " +
            "WHERE user_id = #{userId} AND config_name = #{configName} AND config_type = 'USER_TEMPLATE'")
    boolean existsUserTemplate(@Param("userId") Long userId, @Param("configName") String configName);
    
    /**
     * 检查系统默认配置是否存在
     * 
     * @param workflowType 工作流类型
     * @param configName 配置名称
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) > 0 " +
            "FROM workflow_configurations " +
            "WHERE workflow_type = #{workflowType} AND config_name = #{configName} AND config_type = 'SYSTEM_DEFAULT'")
    boolean existsSystemDefault(@Param("workflowType") String workflowType, @Param("configName") String configName);
    
    /**
     * 获取配置的下一个版本号
     * 
     * @param configName 配置名称
     * @param configType 配置类型
     * @return 下一个版本号
     */
    @Select("SELECT COALESCE(MAX(version), 0) + 1 " +
            "FROM workflow_configurations " +
            "WHERE config_name = #{configName} AND config_type = #{configType}")
    int getNextVersion(@Param("configName") String configName, @Param("configType") ConfigType configType);
    
    /**
     * 批量更新配置激活状态
     * 
     * @param configIds 配置ID列表
     * @param isActive 是否激活
     * @return 影响的行数
     */
    @Update("<script>" +
            "UPDATE workflow_configurations " +
            "SET is_active = #{isActive}, updated_at = NOW() " +
            "WHERE config_id IN " +
            "<foreach collection='configIds' item='configId' open='(' separator=',' close=')'>" +
            "#{configId}" +
            "</foreach>" +
            "</script>")
    int batchUpdateActiveStatus(@Param("configIds") List<Long> configIds, @Param("isActive") Boolean isActive);
    
    /**
     * 查询配置使用统计
     * 
     * @param configId 配置ID
     * @return 使用次数
     */
    @Select("SELECT COUNT(*) " +
            "FROM workflow_instances " +
            "WHERE configuration_id = #{configId}")
    int getUsageCount(Long configId);
    
    /**
     * 查询最近使用的用户配置
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 最近使用的配置列表
     */
    @Select("SELECT DISTINCT wc.config_id, wc.config_name, wc.config_type, wc.user_id, wc.workflow_type, " +
            "wc.configuration_data, wc.description, wc.is_active, wc.version, wc.created_by, " +
            "wc.created_at, wc.updated_at " +
            "FROM workflow_configurations wc " +
            "JOIN workflow_instances wi ON wc.config_id = wi.configuration_id " +
            "WHERE wc.user_id = #{userId} AND wc.config_type = 'USER_TEMPLATE' " +
            "ORDER BY wi.created_at DESC " +
            "LIMIT #{limit}")
    List<WorkflowConfiguration> selectRecentlyUsedConfigs(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * 搜索配置（按名称和描述）
     * 
     * @param keyword 关键词
     * @param userId 用户ID（可选，为null时搜索所有）
     * @return 匹配的配置列表
     */
    @Select("<script>" +
            "SELECT config_id, config_name, config_type, user_id, workflow_type, " +
            "configuration_data, description, is_active, version, created_by, " +
            "created_at, updated_at " +
            "FROM workflow_configurations " +
            "WHERE (config_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "<if test='userId != null'>" +
            "AND (config_type = 'SYSTEM_DEFAULT' OR user_id = #{userId}) " +
            "</if>" +
            "ORDER BY config_type, created_at DESC" +
            "</script>")
    List<WorkflowConfiguration> searchConfigurations(@Param("keyword") String keyword, 
                                                    @Param("userId") Long userId);
}