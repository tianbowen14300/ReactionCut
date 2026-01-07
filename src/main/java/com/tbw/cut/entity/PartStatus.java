package com.tbw.cut.entity;

/**
 * 分P状态枚举
 */
public enum PartStatus {
    /**
     * 等待上传
     */
    WAITING,
    
    /**
     * 正在上传
     */
    UPLOADING,
    
    /**
     * 上传完成
     */
    COMPLETED,
    
    /**
     * 上传失败
     */
    FAILED,
    
    /**
     * 已取消
     */
    CANCELLED
}