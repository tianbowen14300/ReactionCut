package com.tbw.cut.service;

import com.tbw.cut.entity.TaskRelation;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.entity.SubmissionTask;

import java.util.List;
import java.util.Map;

/**
 * 任务关联诊断服务接口
 * 用于诊断和修复任务关联问题
 */
public interface TaskRelationDiagnosticService {
    
    /**
     * 诊断指定下载任务的关联状态
     */
    Map<String, Object> diagnoseDownloadTask(Long downloadTaskId);
    
    /**
     * 诊断指定投稿任务的关联状态
     */
    Map<String, Object> diagnoseSubmissionTask(String submissionTaskId);
    
    /**
     * 检查所有孤立的任务关联
     */
    List<TaskRelation> findOrphanedRelations();
    
    /**
     * 修复孤立的任务关联
     */
    int repairOrphanedRelations();
    
    /**
     * 为现有的下载和投稿任务创建缺失的关联
     */
    int createMissingRelations();
    
    /**
     * 获取任务关联统计信息
     */
    Map<String, Long> getRelationStatistics();
    
    /**
     * 验证任务关联的完整性
     */
    Map<String, Object> validateRelationIntegrity();
}