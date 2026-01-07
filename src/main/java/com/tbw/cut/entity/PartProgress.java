package com.tbw.cut.entity;

import java.time.LocalDateTime;

/**
 * 分P上传进度实体
 */
public class PartProgress {
    /**
     * 分P索引
     */
    private int partIndex;
    
    /**
     * 分P名称
     */
    private String partName;
    
    /**
     * 总字节数
     */
    private long totalBytes;
    
    /**
     * 已上传字节数
     */
    private long uploadedBytes;
    
    /**
     * 分P状态
     */
    private PartStatus status;
    
    /**
     * 处理线程ID
     */
    private String threadId;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * CID（上传完成后获得）
     */
    private Long cid;
    
    // 构造函数
    public PartProgress() {}
    
    public PartProgress(int partIndex, String partName, long totalBytes) {
        this.partIndex = partIndex;
        this.partName = partName;
        this.totalBytes = totalBytes;
        this.uploadedBytes = 0;
        this.status = PartStatus.WAITING;
    }
    
    /**
     * 计算上传进度百分比
     */
    public double getProgressPercentage() {
        if (totalBytes == 0) {
            return 0.0;
        }
        return (double) uploadedBytes / totalBytes * 100.0;
    }
    
    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return status == PartStatus.COMPLETED;
    }
    
    /**
     * 检查是否失败
     */
    public boolean isFailed() {
        return status == PartStatus.FAILED;
    }
    
    // Getters and Setters
    public int getPartIndex() {
        return partIndex;
    }
    
    public void setPartIndex(int partIndex) {
        this.partIndex = partIndex;
    }
    
    public String getPartName() {
        return partName;
    }
    
    public void setPartName(String partName) {
        this.partName = partName;
    }
    
    public long getTotalBytes() {
        return totalBytes;
    }
    
    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }
    
    public long getUploadedBytes() {
        return uploadedBytes;
    }
    
    public void setUploadedBytes(long uploadedBytes) {
        this.uploadedBytes = uploadedBytes;
    }
    
    public PartStatus getStatus() {
        return status;
    }
    
    public void setStatus(PartStatus status) {
        this.status = status;
    }
    
    public String getThreadId() {
        return threadId;
    }
    
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Long getCid() {
        return cid;
    }
    
    public void setCid(Long cid) {
        this.cid = cid;
    }
    
    @Override
    public String toString() {
        return "PartProgress{" +
                "partIndex=" + partIndex +
                ", partName='" + partName + '\'' +
                ", status=" + status +
                ", progress=" + String.format("%.1f", getProgressPercentage()) + "%" +
                ", threadId='" + threadId + '\'' +
                '}';
    }
}