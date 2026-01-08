package com.tbw.cut.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 任务关联实体类
 * 管理视频下载任务与投稿任务之间的关联关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_relations")
public class TaskRelation {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 下载任务ID
     */
    @TableField("download_task_id")
    private Long downloadTaskId;
    
    /**
     * 投稿任务ID
     */
    @TableField("submission_task_id")
    private String submissionTaskId;
    
    /**
     * 关联类型
     */
    @TableField("relation_type")
    private RelationType relationType;
    
    /**
     * 关联状态
     */
    @TableField("status")
    private RelationStatus status;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 关联类型枚举
     */
    public enum RelationType {
        /**
         * 集成创建 - 通过集成表单同时创建的关联
         */
        INTEGRATED,
        
        /**
         * 手动关联 - 用户手动建立的关联
         */
        MANUAL
    }
    
    /**
     * 关联状态枚举
     */
    public enum RelationStatus {
        /**
         * 活跃状态 - 关联正常，任务进行中
         */
        ACTIVE,
        
        /**
         * 已完成 - 两个任务都已完成
         */
        COMPLETED,
        
        /**
         * 失败状态 - 任一任务失败导致关联失效
         */
        FAILED
    }
    
    /**
     * 创建集成关联的便捷方法
     */
    public static TaskRelation createIntegratedRelation(Long downloadTaskId, String submissionTaskId) {
        return TaskRelation.builder()
                .downloadTaskId(downloadTaskId)
                .submissionTaskId(submissionTaskId)
                .relationType(RelationType.INTEGRATED)
                .status(RelationStatus.ACTIVE)
                .build();
    }
    
    /**
     * 创建手动关联的便捷方法
     */
    public static TaskRelation createManualRelation(Long downloadTaskId, String submissionTaskId) {
        return TaskRelation.builder()
                .downloadTaskId(downloadTaskId)
                .submissionTaskId(submissionTaskId)
                .relationType(RelationType.MANUAL)
                .status(RelationStatus.ACTIVE)
                .build();
    }
    
    /**
     * 检查关联是否为活跃状态
     */
    public boolean isActive() {
        return RelationStatus.ACTIVE.equals(this.status);
    }
    
    /**
     * 检查关联是否为集成类型
     */
    public boolean isIntegrated() {
        return RelationType.INTEGRATED.equals(this.relationType);
    }
    
    /**
     * 标记关联为完成状态
     */
    public void markCompleted() {
        this.status = RelationStatus.COMPLETED;
    }
    
    /**
     * 标记关联为失败状态
     */
    public void markFailed() {
        this.status = RelationStatus.FAILED;
    }
}