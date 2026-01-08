package com.tbw.cut.service;

import com.tbw.cut.entity.TaskRelation;
import com.tbw.cut.dto.TaskRelationInfo;

import java.util.List;
import java.util.Optional;

/**
 * 任务关联服务接口
 * 负责管理下载任务与投稿任务之间的关联关系
 */
public interface TaskRelationService {
    
    /**
     * 创建任务关联
     * @param downloadTaskId 下载任务ID
     * @param submissionTaskId 投稿任务ID
     * @param relationType 关联类型
     * @return 关联ID
     */
    Long createRelation(Long downloadTaskId, String submissionTaskId, TaskRelation.RelationType relationType);
    
    /**
     * 创建集成关联
     * @param downloadTaskId 下载任务ID
     * @param submissionTaskId 投稿任务ID
     * @return 关联ID
     */
    Long createIntegratedRelation(Long downloadTaskId, String submissionTaskId);
    
    /**
     * 创建手动关联
     * @param downloadTaskId 下载任务ID
     * @param submissionTaskId 投稿任务ID
     * @return 关联ID
     */
    Long createManualRelation(Long downloadTaskId, String submissionTaskId);
    
    /**
     * 根据下载任务ID查找关联
     * @param downloadTaskId 下载任务ID
     * @return 关联信息
     */
    Optional<TaskRelation> findByDownloadTaskId(Long downloadTaskId);
    
    /**
     * 根据投稿任务ID查找关联
     * @param submissionTaskId 投稿任务ID
     * @return 关联信息
     */
    Optional<TaskRelation> findBySubmissionTaskId(String submissionTaskId);
    
    /**
     * 查找指定任务的关联
     * @param downloadTaskId 下载任务ID
     * @param submissionTaskId 投稿任务ID
     * @return 关联信息
     */
    Optional<TaskRelation> findByTaskIds(Long downloadTaskId, String submissionTaskId);
    
    /**
     * 获取所有活跃关联
     * @return 活跃关联列表
     */
    List<TaskRelation> findAllActiveRelations();
    
    /**
     * 获取指定类型的关联
     * @param relationType 关联类型
     * @return 关联列表
     */
    List<TaskRelation> findByRelationType(TaskRelation.RelationType relationType);
    
    /**
     * 更新关联状态
     * @param relationId 关联ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateStatus(Long relationId, TaskRelation.RelationStatus status);
    
    /**
     * 根据下载任务ID更新关联状态
     * @param downloadTaskId 下载任务ID
     * @param status 新状态
     * @return 更新的记录数
     */
    int updateStatusByDownloadTaskId(Long downloadTaskId, TaskRelation.RelationStatus status);
    
    /**
     * 根据投稿任务ID更新关联状态
     * @param submissionTaskId 投稿任务ID
     * @param status 新状态
     * @return 更新的记录数
     */
    int updateStatusBySubmissionTaskId(String submissionTaskId, TaskRelation.RelationStatus status);
    
    /**
     * 标记关联为完成状态
     * @param relationId 关联ID
     * @return 是否更新成功
     */
    boolean markCompleted(Long relationId);
    
    /**
     * 标记关联为失败状态
     * @param relationId 关联ID
     * @return 是否更新成功
     */
    boolean markFailed(Long relationId);
    
    /**
     * 删除关联
     * @param relationId 关联ID
     * @return 是否删除成功
     */
    boolean deleteRelation(Long relationId);
    
    /**
     * 根据下载任务ID删除关联
     * @param downloadTaskId 下载任务ID
     * @return 删除的记录数
     */
    int deleteByDownloadTaskId(Long downloadTaskId);
    
    /**
     * 根据投稿任务ID删除关联
     * @param submissionTaskId 投稿任务ID
     * @return 删除的记录数
     */
    int deleteBySubmissionTaskId(String submissionTaskId);
    
    /**
     * 查找孤立的关联关系
     * @return 孤立关联列表
     */
    List<TaskRelation> findOrphanedRelations();
    
    /**
     * 统计指定状态的关联数量
     * @param status 关联状态
     * @return 关联数量
     */
    long countByStatus(TaskRelation.RelationStatus status);
    
    /**
     * 统计指定类型的关联数量
     * @param relationType 关联类型
     * @return 关联数量
     */
    long countByRelationType(TaskRelation.RelationType relationType);
    
    /**
     * 检查关联是否存在
     * @param downloadTaskId 下载任务ID
     * @param submissionTaskId 投稿任务ID
     * @return 是否存在关联
     */
    boolean relationExists(Long downloadTaskId, String submissionTaskId);
}