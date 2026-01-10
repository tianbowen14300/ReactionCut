package com.tbw.cut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tbw.cut.entity.TaskRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

/**
 * 任务关联Mapper接口
 */
@Mapper
public interface TaskRelationMapper extends BaseMapper<TaskRelation> {
    
    /**
     * 根据下载任务ID查找关联关系
     */
    @Select("SELECT * FROM task_relations WHERE download_task_id = #{downloadTaskId} AND status = 'ACTIVE'")
    Optional<TaskRelation> findByDownloadTaskId(@Param("downloadTaskId") Long downloadTaskId);
    
    /**
     * 根据投稿任务ID查找关联关系
     */
    @Select("SELECT * FROM task_relations WHERE submission_task_id = #{submissionTaskId} AND status = 'ACTIVE'")
    Optional<TaskRelation> findBySubmissionTaskId(@Param("submissionTaskId") String submissionTaskId);
    
    /**
     * 查找指定下载任务和投稿任务的关联关系
     */
    @Select("SELECT * FROM task_relations WHERE download_task_id = #{downloadTaskId} AND submission_task_id = #{submissionTaskId}")
    Optional<TaskRelation> findByDownloadAndSubmissionTaskId(
            @Param("downloadTaskId") Long downloadTaskId, 
            @Param("submissionTaskId") String submissionTaskId);
    
    /**
     * 查找所有活跃的关联关系
     */
    @Select("SELECT * FROM task_relations WHERE status = 'ACTIVE' ORDER BY created_at DESC")
    List<TaskRelation> findAllActiveRelations();
    
    /**
     * 查找指定类型的关联关系
     */
    @Select("SELECT * FROM task_relations WHERE relation_type = #{relationType} ORDER BY created_at DESC")
    List<TaskRelation> findByRelationType(@Param("relationType") TaskRelation.RelationType relationType);
    
    /**
     * 查找孤立的关联关系（下载任务或投稿任务不存在）
     */
    @Select("SELECT tr.* FROM task_relations tr " +
            "LEFT JOIN video_download vd ON tr.download_task_id = vd.id " +
            "LEFT JOIN submission_task st ON tr.submission_task_id = st.task_id " +
            "WHERE (vd.id IS NULL OR st.task_id IS NULL) AND tr.status = 'ACTIVE'")
    List<TaskRelation> findOrphanedRelations();
    
    /**
     * 更新关联状态
     */
    @Update("UPDATE task_relations SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") TaskRelation.RelationStatus status);
    
    /**
     * 根据下载任务ID更新关联状态
     */
    @Update("UPDATE task_relations SET status = #{status}, updated_at = NOW() WHERE download_task_id = #{downloadTaskId}")
    int updateStatusByDownloadTaskId(@Param("downloadTaskId") Long downloadTaskId, 
                                   @Param("status") TaskRelation.RelationStatus status);
    
    /**
     * 根据投稿任务ID更新关联状态
     */
    @Update("UPDATE task_relations SET status = #{status}, updated_at = NOW() WHERE submission_task_id = #{submissionTaskId}")
    int updateStatusBySubmissionTaskId(@Param("submissionTaskId") String submissionTaskId, 
                                     @Param("status") TaskRelation.RelationStatus status);
    
    /**
     * 统计指定状态的关联数量
     */
    @Select("SELECT COUNT(*) FROM task_relations WHERE status = #{status}")
    long countByStatus(@Param("status") TaskRelation.RelationStatus status);
    
    /**
     * 统计指定类型的关联数量
     */
    @Select("SELECT COUNT(*) FROM task_relations WHERE relation_type = #{relationType}")
    long countByRelationType(@Param("relationType") TaskRelation.RelationType relationType);
    
    /**
     * 删除指定下载任务的所有关联关系
     */
    @Update("DELETE FROM task_relations WHERE download_task_id = #{downloadTaskId}")
    int deleteByDownloadTaskId(@Param("downloadTaskId") Long downloadTaskId);
    
    /**
     * 删除指定投稿任务的所有关联关系
     */
    @Update("DELETE FROM task_relations WHERE submission_task_id = #{submissionTaskId}")
    int deleteBySubmissionTaskId(@Param("submissionTaskId") String submissionTaskId);
    
    /**
     * 根据工作流状态查找关联关系
     */
    @Select("SELECT * FROM task_relations WHERE workflow_status = #{workflowStatus} ORDER BY created_at DESC")
    List<TaskRelation> findByWorkflowStatus(@Param("workflowStatus") TaskRelation.WorkflowStatus workflowStatus);
    
    /**
     * 统计指定工作流状态的关联数量
     */
    @Select("SELECT COUNT(*) FROM task_relations WHERE workflow_status = #{workflowStatus}")
    long countByWorkflowStatus(@Param("workflowStatus") TaskRelation.WorkflowStatus workflowStatus);
    
    /**
     * 查找可重试的工作流任务
     */
    @Select("SELECT * FROM task_relations WHERE workflow_status IN ('WORKFLOW_STARTUP_FAILED', 'WORKFLOW_FAILED') " +
            "AND (retry_count IS NULL OR retry_count < #{maxRetryCount}) " +
            "ORDER BY updated_at ASC")
    List<TaskRelation> findRetryableWorkflowTasks(@Param("maxRetryCount") int maxRetryCount);
    
    /**
     * 更新工作流信息
     */
    @Update("UPDATE task_relations SET " +
            "workflow_instance_id = #{workflowInstanceId}, " +
            "workflow_status = #{workflowStatus}, " +
            "workflow_started_at = NOW(), " +
            "updated_at = NOW() " +
            "WHERE download_task_id = #{downloadTaskId} AND submission_task_id = #{submissionTaskId}")
    int updateWorkflowInfo(@Param("downloadTaskId") Long downloadTaskId,
                          @Param("submissionTaskId") String submissionTaskId,
                          @Param("workflowInstanceId") String workflowInstanceId,
                          @Param("workflowStatus") TaskRelation.WorkflowStatus workflowStatus);
    
    /**
     * 根据投稿任务ID更新工作流信息
     */
    @Update("UPDATE task_relations SET " +
            "workflow_instance_id = #{workflowInstanceId}, " +
            "workflow_status = #{workflowStatus}, " +
            "workflow_started_at = NOW(), " +
            "updated_at = NOW() " +
            "WHERE submission_task_id = #{submissionTaskId}")
    int updateWorkflowInfoBySubmissionId(@Param("submissionTaskId") String submissionTaskId,
                                        @Param("workflowInstanceId") String workflowInstanceId,
                                        @Param("workflowStatus") TaskRelation.WorkflowStatus workflowStatus);
    
    /**
     * 记录工作流错误信息
     */
    @Update("UPDATE task_relations SET " +
            "last_error_message = #{errorMessage}, " +
            "retry_count = COALESCE(retry_count, 0) + 1, " +
            "updated_at = NOW() " +
            "WHERE download_task_id = #{downloadTaskId} AND submission_task_id = #{submissionTaskId}")
    int recordWorkflowError(@Param("downloadTaskId") Long downloadTaskId,
                           @Param("submissionTaskId") String submissionTaskId,
                           @Param("errorMessage") String errorMessage);
}