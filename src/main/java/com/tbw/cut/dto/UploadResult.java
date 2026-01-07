package com.tbw.cut.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 上传结果DTO
 */
public class UploadResult {
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 上传的BVID
     */
    private String bvid;
    
    /**
     * 上传的AID
     */
    private Long aid;
    
    /**
     * 上传开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 上传结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 总分P数
     */
    private int totalParts;
    
    /**
     * 成功上传的分P数
     */
    private int successfulParts;
    
    /**
     * 失败的分P列表
     */
    private List<Integer> failedParts;
    
    /**
     * 重试次数
     */
    private int retryCount;
    
    // 构造函数
    public UploadResult() {}
    
    public UploadResult(String taskId, boolean success) {
        this.taskId = taskId;
        this.success = success;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 创建成功结果
     */
    public static UploadResult success(String taskId, String bvid, Long aid) {
        UploadResult result = new UploadResult(taskId, true);
        result.setBvid(bvid);
        result.setAid(aid);
        return result;
    }
    
    /**
     * 创建失败结果
     */
    public static UploadResult failure(String taskId, String errorMessage) {
        UploadResult result = new UploadResult(taskId, false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    /**
     * 计算上传耗时（秒）
     */
    public long getDurationSeconds() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).getSeconds();
    }
    
    /**
     * 检查是否部分成功
     */
    public boolean isPartialSuccess() {
        return success && failedParts != null && !failedParts.isEmpty();
    }
    
    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getBvid() {
        return bvid;
    }
    
    public void setBvid(String bvid) {
        this.bvid = bvid;
    }
    
    public Long getAid() {
        return aid;
    }
    
    public void setAid(Long aid) {
        this.aid = aid;
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
    
    public int getTotalParts() {
        return totalParts;
    }
    
    public void setTotalParts(int totalParts) {
        this.totalParts = totalParts;
    }
    
    public int getSuccessfulParts() {
        return successfulParts;
    }
    
    public void setSuccessfulParts(int successfulParts) {
        this.successfulParts = successfulParts;
    }
    
    public List<Integer> getFailedParts() {
        return failedParts;
    }
    
    public void setFailedParts(List<Integer> failedParts) {
        this.failedParts = failedParts;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    @Override
    public String toString() {
        return "UploadResult{" +
                "taskId='" + taskId + '\'' +
                ", success=" + success +
                ", bvid='" + bvid + '\'' +
                ", totalParts=" + totalParts +
                ", successfulParts=" + successfulParts +
                ", retryCount=" + retryCount +
                '}';
    }
}