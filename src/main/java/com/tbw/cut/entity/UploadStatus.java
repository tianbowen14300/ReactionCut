package com.tbw.cut.entity;

/**
 * 上传状态枚举
 */
public enum UploadStatus {
    /**
     * 未开始
     */
    NOT_STARTED,
    
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
    CANCELLED,
    
    /**
     * 暂停中
     */
    PAUSED
}