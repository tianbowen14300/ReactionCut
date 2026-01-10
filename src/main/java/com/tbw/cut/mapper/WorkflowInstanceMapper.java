package com.tbw.cut.mapper;

import com.tbw.cut.entity.WorkflowInstance;
import com.tbw.cut.workflow.model.WorkflowStatus;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流实例数据访问层
 * 
 * 提供工作流实例的CRUD操作和查询功能
 */
@Mapper
public interface WorkflowInstanceMapper {
    
    /**
     * 插入新的工作流实例
     * 
     * @param workflowInstance 工作流实例
     * @return 影响的行数
     */
    @Insert("INSERT INTO workflow_instances (" +
            "instance_id, task_id, workflow_type, status, current_step, " +
            "progress, configuration_id, error_message, started_at, " +
            "completed_at, created_at, updated_at" +
            ") VALUES (" +
            "#{instanceId}, #{taskId}, #{workflowType}, #{status}, #{currentStep}, " +
            "#{progress}, #{configurationId}, #{errorMessage}, #{startedAt}, " +
            "#{completedAt}, #{createdAt}, #{updatedAt}" +
            ")")
    int insert(WorkflowInstance workflowInstance);
    
    /**
     * 根据实例ID查询工作流实例
     * 
     * @param instanceId 实例ID
     * @return 工作流实例
     */
    @Select("SELECT instance_id, task_id, workflow_type, status, current_step, " +
            "progress, configuration_id, error_message, started_at, " +
            "completed_at, created_at, updated_at " +
            "FROM workflow_instances " +
            "WHERE instance_id = #{instanceId}")
    WorkflowInstance selectByInstanceId(String instanceId);
    
    /**
     * 根据任务ID查询工作流实例
     * 
     * @param taskId 任务ID
     * @return 工作流实例
     */
    @Select("SELECT instance_id, task_id, workflow_type, status, current_step, " +
            "progress, configuration_id, error_message, started_at, " +
            "completed_at, created_at, updated_at " +
            "FROM workflow_instances " +
            "WHERE task_id = #{taskId}")
    WorkflowInstance selectByTaskId(String taskId);
    
    /**
     * 根据状态查询工作流实例列表
     * 
     * @param status 工作流状态
     * @return 工作流实例列表
     */
    @Select("SELECT instance_id, task_id, workflow_type, status, current_step, " +
            "progress, configuration_id, error_message, started_at, " +
            "completed_at, created_at, updated_at " +
            "FROM workflow_instances " +
            "WHERE status = #{status} " +
            "ORDER BY created_at DESC")
    List<WorkflowInstance> selectByStatus(WorkflowStatus status);
    
    /**
     * 根据工作流类型查询工作流实例列表
     * 
     * @param workflowType 工作流类型
     * @return 工作流实例列表
     */
    @Select("SELECT instance_id, task_id, workflow_type, status, current_step, " +
            "progress, configuration_id, error_message, started_at, " +
            "completed_at, created_at, updated_at " +
            "FROM workflow_instances " +
            "WHERE workflow_type = #{workflowType} " +
            "ORDER BY created_at DESC")
    List<WorkflowInstance> selectByWorkflowType(String workflowType);
    
    /**
     * 查询所有工作流实例
     * 
     * @return 工作流实例列表
     */
    @Select("SELECT instance_id, task_id, workflow_type, status, current_step, " +
            "progress, configuration_id, error_message, started_at, " +
            "completed_at, created_at, updated_at " +
            "FROM workflow_instances " +
            "ORDER BY created_at DESC")
    List<WorkflowInstance> selectAll();
    
    /**
     * 分页查询工作流实例
     * 
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 工作流实例列表
     */
    @Select("SELECT instance_id, task_id, workflow_type, status, current_step, " +
            "progress, configuration_id, error_message, started_at, " +
            "completed_at, created_at, updated_at " +
            "FROM workflow_instances " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<WorkflowInstance> selectWithPagination(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 统计工作流实例总数
     * 
     * @return 总数
     */
    @Select("SELECT COUNT(*) FROM workflow_instances")
    int countAll();
    
    /**
     * 根据状态统计工作流实例数量
     * 
     * @param status 工作流状态
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM workflow_instances WHERE status = #{status}")
    int countByStatus(WorkflowStatus status);
    
    /**
     * 更新工作流实例状态
     * 
     * @param instanceId 实例ID
     * @param status 新状态
     * @param currentStep 当前步骤
     * @param progress 进度
     * @return 影响的行数
     */
    @Update("UPDATE workflow_instances " +
            "SET status = #{status}, current_step = #{currentStep}, progress = #{progress}, " +
            "updated_at = NOW() " +
            "WHERE instance_id = #{instanceId}")
    int updateStatus(@Param("instanceId") String instanceId, 
                    @Param("status") WorkflowStatus status,
                    @Param("currentStep") String currentStep, 
                    @Param("progress") Double progress);
    
    /**
     * 更新工作流实例进度
     * 
     * @param instanceId 实例ID
     * @param progress 进度
     * @return 影响的行数
     */
    @Update("UPDATE workflow_instances " +
            "SET progress = #{progress}, updated_at = NOW() " +
            "WHERE instance_id = #{instanceId}")
    int updateProgress(@Param("instanceId") String instanceId, @Param("progress") Double progress);
    
    /**
     * 设置工作流开始时间
     * 
     * @param instanceId 实例ID
     * @param startedAt 开始时间
     * @return 影响的行数
     */
    @Update("UPDATE workflow_instances " +
            "SET started_at = #{startedAt}, status = 'RUNNING', updated_at = NOW() " +
            "WHERE instance_id = #{instanceId}")
    int updateStartedAt(@Param("instanceId") String instanceId, @Param("startedAt") LocalDateTime startedAt);
    
    /**
     * 设置工作流完成时间
     * 
     * @param instanceId 实例ID
     * @param completedAt 完成时间
     * @param status 最终状态
     * @return 影响的行数
     */
    @Update("UPDATE workflow_instances " +
            "SET completed_at = #{completedAt}, status = #{status}, progress = 100.00, updated_at = NOW() " +
            "WHERE instance_id = #{instanceId}")
    int updateCompletedAt(@Param("instanceId") String instanceId, 
                         @Param("completedAt") LocalDateTime completedAt,
                         @Param("status") WorkflowStatus status);
    
    /**
     * 更新错误信息
     * 
     * @param instanceId 实例ID
     * @param errorMessage 错误信息
     * @return 影响的行数
     */
    @Update("UPDATE workflow_instances " +
            "SET error_message = #{errorMessage}, status = 'FAILED', updated_at = NOW() " +
            "WHERE instance_id = #{instanceId}")
    int updateErrorMessage(@Param("instanceId") String instanceId, @Param("errorMessage") String errorMessage);
    
    /**
     * 完整更新工作流实例
     * 
     * @param workflowInstance 工作流实例
     * @return 影响的行数
     */
    @Update("UPDATE workflow_instances " +
            "SET task_id = #{taskId}, workflow_type = #{workflowType}, status = #{status}, " +
            "current_step = #{currentStep}, progress = #{progress}, " +
            "configuration_id = #{configurationId}, error_message = #{errorMessage}, " +
            "started_at = #{startedAt}, completed_at = #{completedAt}, updated_at = NOW() " +
            "WHERE instance_id = #{instanceId}")
    int update(WorkflowInstance workflowInstance);
    
    /**
     * 删除工作流实例
     * 
     * @param instanceId 实例ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM workflow_instances WHERE instance_id = #{instanceId}")
    int deleteByInstanceId(String instanceId);
    
    /**
     * 根据任务ID删除工作流实例
     * 
     * @param taskId 任务ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM workflow_instances WHERE task_id = #{taskId}")
    int deleteByTaskId(String taskId);
    
    /**
     * 查询正在运行的工作流实例
     * 
     * @return 正在运行的工作流实例列表
     */
    @Select("SELECT instance_id, task_id, workflow_type, status, current_step, " +
            "progress, configuration_id, error_message, started_at, " +
            "completed_at, created_at, updated_at " +
            "FROM workflow_instances " +
            "WHERE status IN ('RUNNING', 'PAUSED') " +
            "ORDER BY started_at ASC")
    List<WorkflowInstance> selectRunningInstances();
    
    /**
     * 查询指定时间范围内创建的工作流实例
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 工作流实例列表
     */
    @Select("SELECT instance_id, task_id, workflow_type, status, current_step, " +
            "progress, configuration_id, error_message, started_at, " +
            "completed_at, created_at, updated_at " +
            "FROM workflow_instances " +
            "WHERE created_at BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY created_at DESC")
    List<WorkflowInstance> selectByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询长时间运行的工作流实例
     * 
     * @param hours 运行小时数阈值
     * @return 长时间运行的工作流实例列表
     */
    @Select("SELECT instance_id, task_id, workflow_type, status, current_step, " +
            "progress, configuration_id, error_message, started_at, " +
            "completed_at, created_at, updated_at " +
            "FROM workflow_instances " +
            "WHERE status = 'RUNNING' " +
            "AND started_at IS NOT NULL " +
            "AND TIMESTAMPDIFF(HOUR, started_at, NOW()) >= #{hours} " +
            "ORDER BY started_at ASC")
    List<WorkflowInstance> selectLongRunningInstances(@Param("hours") int hours);
    
    /**
     * 批量更新工作流状态
     * 
     * @param instanceIds 实例ID列表
     * @param status 新状态
     * @return 影响的行数
     */
    @Update("<script>" +
            "UPDATE workflow_instances " +
            "SET status = #{status}, updated_at = NOW() " +
            "WHERE instance_id IN " +
            "<foreach collection=\"instanceIds\" item=\"instanceId\" open=\"(\" separator=\",\" close=\")\"> " +
            "#{instanceId} " +
            "</foreach>" +
            "</script>")
    int batchUpdateStatus(@Param("instanceIds") List<String> instanceIds, @Param("status") WorkflowStatus status);
}