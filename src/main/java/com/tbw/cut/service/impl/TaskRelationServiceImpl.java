package com.tbw.cut.service.impl;

import com.tbw.cut.entity.TaskRelation;
import com.tbw.cut.mapper.TaskRelationMapper;
import com.tbw.cut.service.TaskRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 任务关联服务实现
 */
@Slf4j
@Service
public class TaskRelationServiceImpl implements TaskRelationService {
    
    @Autowired
    private TaskRelationMapper taskRelationMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRelation(Long downloadTaskId, String submissionTaskId, TaskRelation.RelationType relationType) {
        log.info("Creating task relation: downloadTaskId={}, submissionTaskId={}, relationType={}", 
                downloadTaskId, submissionTaskId, relationType);
        
        try {
            // 检查是否已存在关联
            if (relationExists(downloadTaskId, submissionTaskId)) {
                throw new IllegalArgumentException("Task relation already exists");
            }
            
            TaskRelation relation = TaskRelation.builder()
                    .downloadTaskId(downloadTaskId)
                    .submissionTaskId(submissionTaskId)
                    .relationType(relationType)
                    .status(TaskRelation.RelationStatus.ACTIVE)
                    .build();
            
            taskRelationMapper.insert(relation);
            log.info("Created task relation with ID: {}", relation.getId());
            return relation.getId();
            
        } catch (Exception e) {
            log.error("Failed to create task relation", e);
            throw new RuntimeException("Failed to create task relation", e);
        }
    }
    
    @Override
    public Long createIntegratedRelation(Long downloadTaskId, String submissionTaskId) {
        return createRelation(downloadTaskId, submissionTaskId, TaskRelation.RelationType.INTEGRATED);
    }
    
    @Override
    public Long createManualRelation(Long downloadTaskId, String submissionTaskId) {
        return createRelation(downloadTaskId, submissionTaskId, TaskRelation.RelationType.MANUAL);
    }
    
    @Override
    public Optional<TaskRelation> findByDownloadTaskId(Long downloadTaskId) {
        try {
            return taskRelationMapper.findByDownloadTaskId(downloadTaskId);
        } catch (Exception e) {
            log.error("Failed to find relation by download task ID: {}", downloadTaskId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<TaskRelation> findBySubmissionTaskId(String submissionTaskId) {
        try {
            return taskRelationMapper.findBySubmissionTaskId(submissionTaskId);
        } catch (Exception e) {
            log.error("Failed to find relation by submission task ID: {}", submissionTaskId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<TaskRelation> findByTaskIds(Long downloadTaskId, String submissionTaskId) {
        try {
            return taskRelationMapper.findByDownloadAndSubmissionTaskId(downloadTaskId, submissionTaskId);
        } catch (Exception e) {
            log.error("Failed to find relation by task IDs: downloadTaskId={}, submissionTaskId={}", 
                    downloadTaskId, submissionTaskId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<TaskRelation> findAllActiveRelations() {
        try {
            return taskRelationMapper.findAllActiveRelations();
        } catch (Exception e) {
            log.error("Failed to find all active relations", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<TaskRelation> findByRelationType(TaskRelation.RelationType relationType) {
        try {
            return taskRelationMapper.findByRelationType(relationType);
        } catch (Exception e) {
            log.error("Failed to find relations by type: {}", relationType, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long relationId, TaskRelation.RelationStatus status) {
        try {
            int updated = taskRelationMapper.updateStatus(relationId, status);
            boolean success = updated > 0;
            
            if (success) {
                log.info("Updated relation {} status to {}", relationId, status);
            } else {
                log.warn("No relation found with ID: {}", relationId);
            }
            
            return success;
        } catch (Exception e) {
            log.error("Failed to update relation status: relationId={}, status={}", relationId, status, e);
            return false;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateStatusByDownloadTaskId(Long downloadTaskId, TaskRelation.RelationStatus status) {
        try {
            int updated = taskRelationMapper.updateStatusByDownloadTaskId(downloadTaskId, status);
            log.info("Updated {} relations for download task {} to status {}", updated, downloadTaskId, status);
            return updated;
        } catch (Exception e) {
            log.error("Failed to update relations by download task ID: downloadTaskId={}, status={}", 
                    downloadTaskId, status, e);
            return 0;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateStatusBySubmissionTaskId(String submissionTaskId, TaskRelation.RelationStatus status) {
        try {
            int updated = taskRelationMapper.updateStatusBySubmissionTaskId(submissionTaskId, status);
            log.info("Updated {} relations for submission task {} to status {}", updated, submissionTaskId, status);
            return updated;
        } catch (Exception e) {
            log.error("Failed to update relations by submission task ID: submissionTaskId={}, status={}", 
                    submissionTaskId, status, e);
            return 0;
        }
    }
    
    @Override
    public boolean markCompleted(Long relationId) {
        return updateStatus(relationId, TaskRelation.RelationStatus.COMPLETED);
    }
    
    @Override
    public boolean markFailed(Long relationId) {
        return updateStatus(relationId, TaskRelation.RelationStatus.FAILED);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRelation(Long relationId) {
        try {
            int deleted = taskRelationMapper.deleteById(relationId);
            boolean success = deleted > 0;
            
            if (success) {
                log.info("Deleted relation with ID: {}", relationId);
            } else {
                log.warn("No relation found with ID: {}", relationId);
            }
            
            return success;
        } catch (Exception e) {
            log.error("Failed to delete relation: {}", relationId, e);
            return false;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByDownloadTaskId(Long downloadTaskId) {
        try {
            int deleted = taskRelationMapper.deleteByDownloadTaskId(downloadTaskId);
            log.info("Deleted {} relations for download task: {}", deleted, downloadTaskId);
            return deleted;
        } catch (Exception e) {
            log.error("Failed to delete relations by download task ID: {}", downloadTaskId, e);
            return 0;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBySubmissionTaskId(String submissionTaskId) {
        try {
            int deleted = taskRelationMapper.deleteBySubmissionTaskId(submissionTaskId);
            log.info("Deleted {} relations for submission task: {}", deleted, submissionTaskId);
            return deleted;
        } catch (Exception e) {
            log.error("Failed to delete relations by submission task ID: {}", submissionTaskId, e);
            return 0;
        }
    }
    
    @Override
    public List<TaskRelation> findOrphanedRelations() {
        try {
            List<TaskRelation> orphaned = taskRelationMapper.findOrphanedRelations();
            log.info("Found {} orphaned relations", orphaned.size());
            return orphaned;
        } catch (Exception e) {
            log.error("Failed to find orphaned relations", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public long countByStatus(TaskRelation.RelationStatus status) {
        try {
            return taskRelationMapper.countByStatus(status);
        } catch (Exception e) {
            log.error("Failed to count relations by status: {}", status, e);
            return 0;
        }
    }
    
    @Override
    public long countByRelationType(TaskRelation.RelationType relationType) {
        try {
            return taskRelationMapper.countByRelationType(relationType);
        } catch (Exception e) {
            log.error("Failed to count relations by type: {}", relationType, e);
            return 0;
        }
    }
    
    @Override
    public boolean relationExists(Long downloadTaskId, String submissionTaskId) {
        try {
            Optional<TaskRelation> relation = taskRelationMapper.findByDownloadAndSubmissionTaskId(
                    downloadTaskId, submissionTaskId);
            return relation.isPresent();
        } catch (Exception e) {
            log.error("Failed to check relation existence: downloadTaskId={}, submissionTaskId={}", 
                    downloadTaskId, submissionTaskId, e);
            return false;
        }
    }
}