package com.tbw.cut.dto;

import com.tbw.cut.entity.TaskRelation;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 任务关联信息DTO
 * 用于返回任务关联的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRelationInfo {
    
    /**
     * 关联ID
     */
    private Long relationId;
    
    /**
     * 下载任务ID
     */
    private Long downloadTaskId;
    
    /**
     * 投稿任务ID
     */
    private String submissionTaskId;
    
    /**
     * 关联类型
     */
    private TaskRelation.RelationType relationType;
    
    /**
     * 关联状态
     */
    private TaskRelation.RelationStatus relationStatus;
    
    /**
     * 下载任务信息
     */
    private DownloadTaskInfo downloadTaskInfo;
    
    /**
     * 投稿任务信息
     */
    private SubmissionTaskInfo submissionTaskInfo;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 下载任务信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadTaskInfo {
        private Long taskId;
        private String bvid;
        private String title;
        private String partTitle;
        private Integer status;
        private String statusText;
        private Integer progress;
        private String localPath;
        private LocalDateTime createTime;
    }
    
    /**
     * 投稿任务信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionTaskInfo {
        private String taskId;
        private String title;
        private String description;
        private String status;
        private String statusText;
        private String bvid;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    
    /**
     * 从TaskRelation实体创建TaskRelationInfo
     */
    public static TaskRelationInfo fromEntity(TaskRelation relation) {
        return TaskRelationInfo.builder()
                .relationId(relation.getId())
                .downloadTaskId(relation.getDownloadTaskId())
                .submissionTaskId(relation.getSubmissionTaskId())
                .relationType(relation.getRelationType())
                .relationStatus(relation.getStatus())
                .createdAt(relation.getCreatedAt())
                .updatedAt(relation.getUpdatedAt())
                .build();
    }
    
    /**
     * 检查关联是否活跃
     */
    public boolean isActive() {
        return TaskRelation.RelationStatus.ACTIVE.equals(relationStatus);
    }
    
    /**
     * 检查是否为集成类型关联
     */
    public boolean isIntegrated() {
        return TaskRelation.RelationType.INTEGRATED.equals(relationType);
    }
}