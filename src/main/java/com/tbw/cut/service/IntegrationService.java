package com.tbw.cut.service;

import com.tbw.cut.dto.IntegrationRequest;
import com.tbw.cut.dto.IntegrationResult;
import com.tbw.cut.dto.TaskRelationInfo;
import com.tbw.cut.entity.TaskRelation;

/**
 * 视频下载与投稿集成服务接口
 * 负责处理下载任务和投稿任务的集成创建和管理
 */
public interface IntegrationService {
    
    /**
     * 处理集成请求，同时创建下载任务和投稿任务
     * @param request 集成请求数据
     * @return 集成结果，包含两个任务的ID
     */
    IntegrationResult processIntegratedRequest(IntegrationRequest request);
    
    /**
     * 同步任务状态
     * 当下载任务状态发生变化时，同步更新投稿任务状态
     * @param downloadTaskId 下载任务ID
     * @param downloadStatus 下载任务状态
     */
    void syncTaskStatus(Long downloadTaskId, Integer downloadStatus);
    
    /**
     * 获取任务关联信息
     * @param taskId 任务ID
     * @param taskType 任务类型（DOWNLOAD 或 SUBMISSION）
     * @return 任务关联信息
     */
    TaskRelationInfo getTaskRelation(Long taskId, TaskRelationType taskType);
    
    /**
     * 获取任务关联信息（通过投稿任务ID）
     * @param submissionTaskId 投稿任务ID
     * @return 任务关联信息
     */
    TaskRelationInfo getTaskRelationBySubmissionId(String submissionTaskId);
    
    /**
     * 删除任务关联
     * @param downloadTaskId 下载任务ID
     * @param submissionTaskId 投稿任务ID
     * @return 是否删除成功
     */
    boolean removeTaskRelation(Long downloadTaskId, String submissionTaskId);
    
    /**
     * 任务类型枚举
     */
    enum TaskRelationType {
        DOWNLOAD,
        SUBMISSION
    }
}