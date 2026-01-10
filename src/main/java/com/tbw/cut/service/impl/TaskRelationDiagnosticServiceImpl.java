package com.tbw.cut.service.impl;

import com.tbw.cut.entity.TaskRelation;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.mapper.TaskRelationMapper;
import com.tbw.cut.service.TaskRelationDiagnosticService;
import com.tbw.cut.service.VideoDownloadService;
import com.tbw.cut.service.SubmissionTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 任务关联诊断服务实现
 */
@Slf4j
@Service
public class TaskRelationDiagnosticServiceImpl implements TaskRelationDiagnosticService {
    
    @Autowired
    private TaskRelationMapper taskRelationMapper;
    
    @Autowired
    private VideoDownloadService videoDownloadService;
    
    @Autowired
    private SubmissionTaskService submissionTaskService;
    
    @Override
    public Map<String, Object> diagnoseDownloadTask(Long downloadTaskId) {
        Map<String, Object> diagnosis = new HashMap<>();
        
        try {
            log.info("Diagnosing download task: {}", downloadTaskId);
            
            // 检查下载任务是否存在
            VideoDownload downloadTask = videoDownloadService.getById(downloadTaskId);
            diagnosis.put("downloadTaskExists", downloadTask != null);
            if (downloadTask != null) {
                diagnosis.put("downloadTaskStatus", downloadTask.getStatus());
                diagnosis.put("downloadTaskTitle", downloadTask.getTitle());
            }
            
            // 检查任务关联
            Optional<TaskRelation> relation = taskRelationMapper.findByDownloadTaskId(downloadTaskId);
            diagnosis.put("relationExists", relation.isPresent());
            
            if (relation.isPresent()) {
                TaskRelation rel = relation.get();
                diagnosis.put("relationId", rel.getId());
                diagnosis.put("relationStatus", rel.getStatus());
                diagnosis.put("relationType", rel.getRelationType());
                diagnosis.put("submissionTaskId", rel.getSubmissionTaskId());
                
                // 检查关联的投稿任务是否存在
                SubmissionTask submissionTask = submissionTaskService.getTaskDetail(rel.getSubmissionTaskId());
                diagnosis.put("submissionTaskExists", submissionTask != null);
                if (submissionTask != null) {
                    diagnosis.put("submissionTaskStatus", submissionTask.getStatus());
                    diagnosis.put("submissionTaskTitle", submissionTask.getTitle());
                }
            }
            
            // 检查是否有多个关联（不应该发生）
            List<TaskRelation> allRelations = taskRelationMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TaskRelation>()
                            .eq("download_task_id", downloadTaskId)
            );
            diagnosis.put("totalRelations", allRelations.size());
            
            diagnosis.put("success", true);
            
        } catch (Exception e) {
            log.error("Failed to diagnose download task: {}", downloadTaskId, e);
            diagnosis.put("success", false);
            diagnosis.put("error", e.getMessage());
        }
        
        return diagnosis;
    }
    
    @Override
    public Map<String, Object> diagnoseSubmissionTask(String submissionTaskId) {
        Map<String, Object> diagnosis = new HashMap<>();
        
        try {
            log.info("Diagnosing submission task: {}", submissionTaskId);
            
            // 检查投稿任务是否存在
            SubmissionTask submissionTask = submissionTaskService.getTaskDetail(submissionTaskId);
            diagnosis.put("submissionTaskExists", submissionTask != null);
            if (submissionTask != null) {
                diagnosis.put("submissionTaskStatus", submissionTask.getStatus());
                diagnosis.put("submissionTaskTitle", submissionTask.getTitle());
            }
            
            // 检查任务关联
            Optional<TaskRelation> relation = taskRelationMapper.findBySubmissionTaskId(submissionTaskId);
            diagnosis.put("relationExists", relation.isPresent());
            
            if (relation.isPresent()) {
                TaskRelation rel = relation.get();
                diagnosis.put("relationId", rel.getId());
                diagnosis.put("relationStatus", rel.getStatus());
                diagnosis.put("relationType", rel.getRelationType());
                diagnosis.put("downloadTaskId", rel.getDownloadTaskId());
                
                // 检查关联的下载任务是否存在
                VideoDownload downloadTask = videoDownloadService.getById(rel.getDownloadTaskId());
                diagnosis.put("downloadTaskExists", downloadTask != null);
                if (downloadTask != null) {
                    diagnosis.put("downloadTaskStatus", downloadTask.getStatus());
                    diagnosis.put("downloadTaskTitle", downloadTask.getTitle());
                }
            }
            
            diagnosis.put("success", true);
            
        } catch (Exception e) {
            log.error("Failed to diagnose submission task: {}", submissionTaskId, e);
            diagnosis.put("success", false);
            diagnosis.put("error", e.getMessage());
        }
        
        return diagnosis;
    }
    
    @Override
    public List<TaskRelation> findOrphanedRelations() {
        try {
            return taskRelationMapper.findOrphanedRelations();
        } catch (Exception e) {
            log.error("Failed to find orphaned relations", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public int repairOrphanedRelations() {
        try {
            List<TaskRelation> orphaned = findOrphanedRelations();
            int repairedCount = 0;
            
            for (TaskRelation relation : orphaned) {
                try {
                    // 标记为失败状态
                    taskRelationMapper.updateStatus(relation.getId(), TaskRelation.RelationStatus.FAILED);
                    repairedCount++;
                    log.info("Marked orphaned relation {} as FAILED", relation.getId());
                } catch (Exception e) {
                    log.error("Failed to repair orphaned relation: {}", relation.getId(), e);
                }
            }
            
            log.info("Repaired {} orphaned relations", repairedCount);
            return repairedCount;
            
        } catch (Exception e) {
            log.error("Failed to repair orphaned relations", e);
            return 0;
        }
    }
    
    @Override
    public int createMissingRelations() {
        // 这个方法需要根据业务逻辑来实现
        // 目前暂时返回0，因为我们不确定哪些任务应该关联
        log.warn("createMissingRelations not implemented - requires business logic");
        return 0;
    }
    
    @Override
    public Map<String, Long> getRelationStatistics() {
        Map<String, Long> stats = new HashMap<>();
        
        try {
            stats.put("totalRelations", taskRelationMapper.selectCount(null));
            stats.put("activeRelations", taskRelationMapper.countByStatus(TaskRelation.RelationStatus.ACTIVE));
            stats.put("completedRelations", taskRelationMapper.countByStatus(TaskRelation.RelationStatus.COMPLETED));
            stats.put("failedRelations", taskRelationMapper.countByStatus(TaskRelation.RelationStatus.FAILED));
            stats.put("integratedRelations", taskRelationMapper.countByRelationType(TaskRelation.RelationType.INTEGRATED));
            stats.put("manualRelations", taskRelationMapper.countByRelationType(TaskRelation.RelationType.MANUAL));
            
        } catch (Exception e) {
            log.error("Failed to get relation statistics", e);
        }
        
        return stats;
    }
    
    @Override
    public Map<String, Object> validateRelationIntegrity() {
        Map<String, Object> validation = new HashMap<>();
        
        try {
            List<String> issues = new ArrayList<>();
            
            // 检查孤立关联
            List<TaskRelation> orphaned = findOrphanedRelations();
            if (!orphaned.isEmpty()) {
                issues.add("Found " + orphaned.size() + " orphaned relations");
            }
            
            // 检查重复关联
            // TODO: 实现重复关联检查
            
            validation.put("issues", issues);
            validation.put("isValid", issues.isEmpty());
            validation.put("orphanedCount", orphaned.size());
            
        } catch (Exception e) {
            log.error("Failed to validate relation integrity", e);
            validation.put("isValid", false);
            validation.put("error", e.getMessage());
        }
        
        return validation;
    }
}