package com.tbw.cut.mapper;

import com.tbw.cut.entity.WorkflowStep;
import com.tbw.cut.workflow.model.StepStatus;
import com.tbw.cut.workflow.model.StepType;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流步骤数据访问层
 * 
 * 提供工作流步骤的CRUD操作和查询功能
 */
@Mapper
public interface WorkflowStepMapper {
    
    /**
     * 插入新的工作流步骤
     * 
     * @param workflowStep 工作流步骤
     * @return 影响的行数
     */
    @Insert("INSERT INTO workflow_steps (" +
            "step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at" +
            ") VALUES (" +
            "#{stepId}, #{instanceId}, #{stepName}, #{stepType}, #{stepOrder}, #{status}, " +
            "#{progress}, #{inputData}, #{outputData}, #{errorMessage}, #{retryCount}, " +
            "#{maxRetries}, #{startedAt}, #{completedAt}, #{createdAt}, #{updatedAt}" +
            ")")
    int insert(WorkflowStep workflowStep);
    
    /**
     * 根据步骤ID查询工作流步骤
     * 
     * @param stepId 步骤ID
     * @return 工作流步骤
     */
    @Select("SELECT step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at " +
            "FROM workflow_steps " +
            "WHERE step_id = #{stepId}")
    WorkflowStep selectByStepId(String stepId);
    
    /**
     * 根据实例ID查询所有步骤
     * 
     * @param instanceId 实例ID
     * @return 工作流步骤列表
     */
    @Select("SELECT step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at " +
            "FROM workflow_steps " +
            "WHERE instance_id = #{instanceId} " +
            "ORDER BY step_order ASC")
    List<WorkflowStep> selectByInstanceId(String instanceId);
    
    /**
     * 根据实例ID和步骤顺序查询步骤
     * 
     * @param instanceId 实例ID
     * @param stepOrder 步骤顺序
     * @return 工作流步骤
     */
    @Select("SELECT step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at " +
            "FROM workflow_steps " +
            "WHERE instance_id = #{instanceId} AND step_order = #{stepOrder}")
    WorkflowStep selectByInstanceIdAndOrder(@Param("instanceId") String instanceId, 
                                          @Param("stepOrder") Integer stepOrder);
    
    /**
     * 根据状态查询工作流步骤列表
     * 
     * @param status 步骤状态
     * @return 工作流步骤列表
     */
    @Select("SELECT step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at " +
            "FROM workflow_steps " +
            "WHERE status = #{status} " +
            "ORDER BY created_at DESC")
    List<WorkflowStep> selectByStatus(StepStatus status);
    
    /**
     * 根据步骤类型查询工作流步骤列表
     * 
     * @param stepType 步骤类型
     * @return 工作流步骤列表
     */
    @Select("SELECT step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at " +
            "FROM workflow_steps " +
            "WHERE step_type = #{stepType} " +
            "ORDER BY created_at DESC")
    List<WorkflowStep> selectByStepType(StepType stepType);
    
    /**
     * 查询实例的当前执行步骤
     * 
     * @param instanceId 实例ID
     * @return 当前执行的步骤
     */
    @Select("SELECT step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at " +
            "FROM workflow_steps " +
            "WHERE instance_id = #{instanceId} AND status = 'RUNNING' " +
            "ORDER BY step_order ASC " +
            "LIMIT 1")
    WorkflowStep selectCurrentRunningStep(String instanceId);
    
    /**
     * 查询实例的下一个待执行步骤
     * 
     * @param instanceId 实例ID
     * @return 下一个待执行的步骤
     */
    @Select("SELECT step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at " +
            "FROM workflow_steps " +
            "WHERE instance_id = #{instanceId} AND status = 'PENDING' " +
            "ORDER BY step_order ASC " +
            "LIMIT 1")
    WorkflowStep selectNextPendingStep(String instanceId);
    
    /**
     * 查询实例的失败步骤
     * 
     * @param instanceId 实例ID
     * @return 失败的步骤列表
     */
    @Select("SELECT step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at " +
            "FROM workflow_steps " +
            "WHERE instance_id = #{instanceId} AND status = 'FAILED' " +
            "ORDER BY step_order ASC")
    List<WorkflowStep> selectFailedSteps(String instanceId);
    
    /**
     * 统计实例的步骤数量
     * 
     * @param instanceId 实例ID
     * @return 步骤总数
     */
    @Select("SELECT COUNT(*) FROM workflow_steps WHERE instance_id = #{instanceId}")
    int countByInstanceId(String instanceId);
    
    /**
     * 统计实例的已完成步骤数量
     * 
     * @param instanceId 实例ID
     * @return 已完成步骤数量
     */
    @Select("SELECT COUNT(*) FROM workflow_steps WHERE instance_id = #{instanceId} AND status = 'COMPLETED'")
    int countCompletedSteps(String instanceId);
    
    /**
     * 统计实例的失败步骤数量
     * 
     * @param instanceId 实例ID
     * @return 失败步骤数量
     */
    @Select("SELECT COUNT(*) FROM workflow_steps WHERE instance_id = #{instanceId} AND status = 'FAILED'")
    int countFailedSteps(String instanceId);
    
    /**
     * 更新步骤状态
     * 
     * @param stepId 步骤ID
     * @param status 新状态
     * @param progress 进度
     * @return 影响的行数
     */
    @Update("UPDATE workflow_steps " +
            "SET status = #{status}, progress = #{progress}, updated_at = NOW() " +
            "WHERE step_id = #{stepId}")
    int updateStatus(@Param("stepId") String stepId, 
                    @Param("status") StepStatus status, 
                    @Param("progress") Double progress);
    
    /**
     * 更新步骤进度
     * 
     * @param stepId 步骤ID
     * @param progress 进度
     * @return 影响的行数
     */
    @Update("UPDATE workflow_steps " +
            "SET progress = #{progress}, updated_at = NOW() " +
            "WHERE step_id = #{stepId}")
    int updateProgress(@Param("stepId") String stepId, @Param("progress") Double progress);
    
    /**
     * 设置步骤开始时间
     * 
     * @param stepId 步骤ID
     * @param startedAt 开始时间
     * @return 影响的行数
     */
    @Update("UPDATE workflow_steps " +
            "SET started_at = #{startedAt}, status = 'RUNNING', updated_at = NOW() " +
            "WHERE step_id = #{stepId}")
    int updateStartedAt(@Param("stepId") String stepId, @Param("startedAt") LocalDateTime startedAt);
    
    /**
     * 设置步骤完成时间
     * 
     * @param stepId 步骤ID
     * @param completedAt 完成时间
     * @param status 最终状态
     * @return 影响的行数
     */
    @Update("UPDATE workflow_steps " +
            "SET completed_at = #{completedAt}, status = #{status}, progress = 100.00, updated_at = NOW() " +
            "WHERE step_id = #{stepId}")
    int updateCompletedAt(@Param("stepId") String stepId, 
                         @Param("completedAt") LocalDateTime completedAt,
                         @Param("status") StepStatus status);
    
    /**
     * 更新步骤输入数据
     * 
     * @param stepId 步骤ID
     * @param inputData 输入数据
     * @return 影响的行数
     */
    @Update("UPDATE workflow_steps " +
            "SET input_data = #{inputData}, updated_at = NOW() " +
            "WHERE step_id = #{stepId}")
    int updateInputData(@Param("stepId") String stepId, @Param("inputData") String inputData);
    
    /**
     * 更新步骤输出数据
     * 
     * @param stepId 步骤ID
     * @param outputData 输出数据
     * @return 影响的行数
     */
    @Update("UPDATE workflow_steps " +
            "SET output_data = #{outputData}, updated_at = NOW() " +
            "WHERE step_id = #{stepId}")
    int updateOutputData(@Param("stepId") String stepId, @Param("outputData") String outputData);
    
    /**
     * 更新错误信息
     * 
     * @param stepId 步骤ID
     * @param errorMessage 错误信息
     * @return 影响的行数
     */
    @Update("UPDATE workflow_steps " +
            "SET error_message = #{errorMessage}, status = 'FAILED', updated_at = NOW() " +
            "WHERE step_id = #{stepId}")
    int updateErrorMessage(@Param("stepId") String stepId, @Param("errorMessage") String errorMessage);
    
    /**
     * 增加重试次数
     * 
     * @param stepId 步骤ID
     * @return 影响的行数
     */
    @Update("UPDATE workflow_steps " +
            "SET retry_count = retry_count + 1, status = 'PENDING', updated_at = NOW() " +
            "WHERE step_id = #{stepId}")
    int incrementRetryCount(String stepId);
    
    /**
     * 完整更新工作流步骤
     * 
     * @param workflowStep 工作流步骤
     * @return 影响的行数
     */
    @Update("UPDATE workflow_steps " +
            "SET instance_id = #{instanceId}, step_name = #{stepName}, step_type = #{stepType}, " +
            "step_order = #{stepOrder}, status = #{status}, progress = #{progress}, " +
            "input_data = #{inputData}, output_data = #{outputData}, error_message = #{errorMessage}, " +
            "retry_count = #{retryCount}, max_retries = #{maxRetries}, started_at = #{startedAt}, " +
            "completed_at = #{completedAt}, updated_at = NOW() " +
            "WHERE step_id = #{stepId}")
    int update(WorkflowStep workflowStep);
    
    /**
     * 删除工作流步骤
     * 
     * @param stepId 步骤ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM workflow_steps WHERE step_id = #{stepId}")
    int deleteByStepId(String stepId);
    
    /**
     * 根据实例ID删除所有步骤
     * 
     * @param instanceId 实例ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM workflow_steps WHERE instance_id = #{instanceId}")
    int deleteByInstanceId(String instanceId);
    
    /**
     * 查询可重试的失败步骤
     * 
     * @return 可重试的失败步骤列表
     */
    @Select("SELECT step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at " +
            "FROM workflow_steps " +
            "WHERE status = 'FAILED' AND retry_count < max_retries " +
            "ORDER BY created_at ASC")
    List<WorkflowStep> selectRetryableFailedSteps();
    
    /**
     * 查询长时间运行的步骤
     * 
     * @param hours 运行小时数阈值
     * @return 长时间运行的步骤列表
     */
    @Select("SELECT step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at " +
            "FROM workflow_steps " +
            "WHERE status = 'RUNNING' " +
            "AND started_at IS NOT NULL " +
            "AND TIMESTAMPDIFF(HOUR, started_at, NOW()) >= #{hours} " +
            "ORDER BY started_at ASC")
    List<WorkflowStep> selectLongRunningSteps(@Param("hours") int hours);
    
    /**
     * 批量插入工作流步骤
     * 
     * @param workflowSteps 工作流步骤列表
     * @return 影响的行数
     */
    @Insert("<script>" +
            "INSERT INTO workflow_steps (" +
            "step_id, instance_id, step_name, step_type, step_order, status, " +
            "progress, input_data, output_data, error_message, retry_count, " +
            "max_retries, started_at, completed_at, created_at, updated_at" +
            ") VALUES " +
            "<foreach collection='workflowSteps' item='step' separator=','>" +
            "(#{step.stepId}, #{step.instanceId}, #{step.stepName}, #{step.stepType}, " +
            "#{step.stepOrder}, #{step.status}, #{step.progress}, #{step.inputData}, " +
            "#{step.outputData}, #{step.errorMessage}, #{step.retryCount}, " +
            "#{step.maxRetries}, #{step.startedAt}, #{step.completedAt}, " +
            "#{step.createdAt}, #{step.updatedAt})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("workflowSteps") List<WorkflowStep> workflowSteps);
    
    /**
     * 批量更新步骤状态
     * 
     * @param stepIds 步骤ID列表
     * @param status 新状态
     * @return 影响的行数
     */
    @Update("<script>" +
            "UPDATE workflow_steps " +
            "SET status = #{status}, updated_at = NOW() " +
            "WHERE step_id IN " +
            "<foreach collection='stepIds' item='stepId' open='(' separator=',' close=')'>" +
            "#{stepId}" +
            "</foreach>" +
            "</script>")
    int batchUpdateStatus(@Param("stepIds") List<String> stepIds, @Param("status") StepStatus status);
}