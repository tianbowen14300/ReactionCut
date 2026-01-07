package com.tbw.cut.entity;

import com.alibaba.fastjson.JSONObject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * 上传进度实体
 */
public class UploadProgress {
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 总分片数
     */
    private int totalChunks;
    
    /**
     * 已完成分片集合
     */
    private Set<Integer> completedChunks;
    
    /**
     * 总分P数
     */
    private int totalParts;
    
    /**
     * 已完成分P数
     */
    private int completedParts;
    
    /**
     * 失败分P数
     */
    private int failedParts;
    
    /**
     * 各分P的进度详情
     */
    private Map<Integer, PartProgress> partProgress;
    
    /**
     * 整体进度百分比
     */
    private double overallProgress;
    
    /**
     * 上传状态
     */
    private UploadStatus status;
    
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
     * 预上传数据
     */
    private JSONObject preUploadData;
    
    /**
     * 视频元数据
     */
    private JSONObject postVideoMeta;
    
    // 构造函数
    public UploadProgress() {
        this.partProgress = new ConcurrentHashMap<>();
        this.completedChunks = new HashSet<>();
    }
    
    public UploadProgress(String taskId, int totalParts) {
        this.taskId = taskId;
        this.totalParts = totalParts;
        this.completedParts = 0;
        this.failedParts = 0;
        this.overallProgress = 0.0;
        this.status = UploadStatus.NOT_STARTED;
        this.partProgress = new ConcurrentHashMap<>();
        this.completedChunks = new HashSet<>();
    }
    
    /**
     * 获取总分片数
     */
    public int getTotalChunks() {
        return totalChunks;
    }
    
    /**
     * 获取下一个未完成的分片索引
     */
    public int getNextChunkIndex() {
        for (int i = 0; i < totalChunks; i++) {
            if (!completedChunks.contains(i)) {
                return i;
            }
        }
        return -1; // 所有分片都已完成
    }
    
    /**
     * 获取已完成分片数
     */
    public int getCompletedChunks() {
        return completedChunks.size();
    }
    
    /**
     * 获取完成百分比
     */
    public double getCompletionPercentage() {
        if (totalChunks == 0) {
            return 0.0;
        }
        return (double) completedChunks.size() / totalChunks * 100.0;
    }
    
    /**
     * 检查分片是否已完成
     */
    public boolean isChunkCompleted(int chunkIndex) {
        return completedChunks.contains(chunkIndex);
    }
    
    /**
     * 标记分片完成
     */
    public void markChunkCompleted(int chunkIndex) {
        completedChunks.add(chunkIndex);
    }
    
    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return completedChunks.size() == totalChunks;
    }
    
    /**
     * 更新整体进度
     */
    public void updateOverallProgress() {
        if (totalParts == 0) {
            this.overallProgress = 0.0;
            return;
        }
        
        // 计算已完成分P的百分比
        this.overallProgress = (double) completedParts / totalParts * 100.0;
        
        // 如果有正在上传的分P，加上其部分进度
        double partialProgress = 0.0;
        int uploadingParts = 0;
        
        for (PartProgress part : partProgress.values()) {
            if (part.getStatus() == PartStatus.UPLOADING) {
                partialProgress += part.getProgressPercentage();
                uploadingParts++;
            }
        }
        
        if (uploadingParts > 0) {
            // 将正在上传的分P的部分进度加入总进度
            this.overallProgress += (partialProgress / uploadingParts) / totalParts;
        }
        
        // 确保进度不超过100%
        this.overallProgress = Math.min(this.overallProgress, 100.0);
    }
    
    /**
     * 添加分P进度
     */
    public void addPartProgress(PartProgress part) {
        this.partProgress.put(part.getPartIndex(), part);
    }
    
    /**
     * 更新分P状态
     */
    public void updatePartStatus(int partIndex, PartStatus status) {
        PartProgress part = partProgress.get(partIndex);
        if (part != null) {
            PartStatus oldStatus = part.getStatus();
            part.setStatus(status);
            
            // 更新计数器
            if (oldStatus != PartStatus.COMPLETED && status == PartStatus.COMPLETED) {
                completedParts++;
            } else if (oldStatus == PartStatus.COMPLETED && status != PartStatus.COMPLETED) {
                completedParts--;
            }
            
            if (oldStatus != PartStatus.FAILED && status == PartStatus.FAILED) {
                failedParts++;
            } else if (oldStatus == PartStatus.FAILED && status != PartStatus.FAILED) {
                failedParts--;
            }
            
            // 更新整体进度
            updateOverallProgress();
        }
    }
    
    /**
     * 检查是否所有分P都已完成
     */
    public boolean isAllPartsCompleted() {
        return completedParts == totalParts;
    }
    
    /**
     * 检查是否有失败的分P
     */
    public boolean hasFailedParts() {
        return failedParts > 0;
    }
    
    /**
     * 获取正在上传的分P数量
     */
    public int getUploadingPartsCount() {
        return (int) partProgress.values().stream()
                .filter(part -> part.getStatus() == PartStatus.UPLOADING)
                .count();
    }
    
    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }
    
    public int getTotalParts() {
        return totalParts;
    }
    
    public void setTotalParts(int totalParts) {
        this.totalParts = totalParts;
    }
    
    public int getCompletedParts() {
        return completedParts;
    }
    
    public void setCompletedParts(int completedParts) {
        this.completedParts = completedParts;
    }
    
    public int getFailedParts() {
        return failedParts;
    }
    
    public void setFailedParts(int failedParts) {
        this.failedParts = failedParts;
    }
    
    public Map<Integer, PartProgress> getPartProgress() {
        return partProgress;
    }
    
    public void setPartProgress(Map<Integer, PartProgress> partProgress) {
        this.partProgress = partProgress;
    }
    
    public double getOverallProgress() {
        return overallProgress;
    }
    
    public void setOverallProgress(double overallProgress) {
        this.overallProgress = overallProgress;
    }
    
    public UploadStatus getStatus() {
        return status;
    }
    
    public void setStatus(UploadStatus status) {
        this.status = status;
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
    
    public JSONObject getPreUploadData() {
        return preUploadData;
    }
    
    public void setPreUploadData(JSONObject preUploadData) {
        this.preUploadData = preUploadData;
    }
    
    public JSONObject getPostVideoMeta() {
        return postVideoMeta;
    }
    
    public void setPostVideoMeta(JSONObject postVideoMeta) {
        this.postVideoMeta = postVideoMeta;
    }
    
    @Override
    public String toString() {
        return "UploadProgress{" +
                "taskId='" + taskId + '\'' +
                ", filePath='" + filePath + '\'' +
                ", totalChunks=" + totalChunks +
                ", completedChunks=" + completedChunks.size() +
                ", totalParts=" + totalParts +
                ", completedParts=" + completedParts +
                ", failedParts=" + failedParts +
                ", overallProgress=" + String.format("%.1f", overallProgress) + "%" +
                ", status=" + status +
                '}';
    }
}