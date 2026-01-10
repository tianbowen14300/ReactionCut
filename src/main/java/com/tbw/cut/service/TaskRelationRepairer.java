package com.tbw.cut.service;

import com.tbw.cut.diagnostic.model.OrphanedTask;
import com.tbw.cut.diagnostic.model.RepairResult;
import com.tbw.cut.diagnostic.model.ValidationResult;
import com.tbw.cut.diagnostic.model.CleanupResult;
import java.util.List;

/**
 * 任务关联修复器接口
 * 用于修复任务关联问题
 */
public interface TaskRelationRepairer {
    
    /**
     * 扫描孤立的下载任务
     * 
     * @return 孤立任务列表
     */
    List<OrphanedTask> scanOrphanedTasks();
    
    /**
     * 重建任务关联
     * 
     * @param downloadTaskId 下载任务ID
     * @return 修复结果
     */
    RepairResult rebuildTaskRelations(Long downloadTaskId);
    
    /**
     * 验证关联完整性
     * 
     * @return 验证结果
     */
    ValidationResult validateRelationIntegrity();
    
    /**
     * 清理无效关联
     * 
     * @return 清理结果
     */
    CleanupResult cleanupInvalidRelations();
    
    /**
     * 自动修复孤立任务
     * 
     * @param taskId 任务ID
     * @return 修复结果
     */
    RepairResult autoRepairOrphanedTask(String taskId);
    
    /**
     * 批量修复孤立任务
     * 
     * @param taskIds 任务ID列表
     * @return 修复结果列表
     */
    List<RepairResult> batchRepairOrphanedTasks(List<String> taskIds);
    
    /**
     * 检查多分P下载的关联完整性
     * 
     * @param downloadTaskId 主下载任务ID
     * @return 验证结果
     */
    ValidationResult validateMultiPartRelations(Long downloadTaskId);
}