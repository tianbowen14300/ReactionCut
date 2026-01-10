package com.tbw.cut.workflow.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 视频处理结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 输出文件路径列表
     */
    private List<String> outputPaths;
    
    /**
     * 主要输出路径（用于单文件输出）
     */
    private String primaryOutputPath;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 处理开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 处理结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 处理耗时（毫秒）
     */
    private Long durationMs;
    
    /**
     * 额外的处理信息
     */
    private Map<String, Object> metadata;
    
    /**
     * 创建成功结果
     */
    public static ProcessingResult success(String primaryOutputPath) {
        return ProcessingResult.builder()
                .success(true)
                .primaryOutputPath(primaryOutputPath)
                .endTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建成功结果（多文件输出）
     */
    public static ProcessingResult success(List<String> outputPaths) {
        return ProcessingResult.builder()
                .success(true)
                .outputPaths(outputPaths)
                .primaryOutputPath(outputPaths != null && !outputPaths.isEmpty() ? outputPaths.get(0) : null)
                .endTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static ProcessingResult failure(String errorMessage) {
        return ProcessingResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .endTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 设置开始时间并返回自身（用于链式调用）
     */
    public ProcessingResult withStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        if (this.endTime != null && startTime != null) {
            this.durationMs = java.time.Duration.between(startTime, this.endTime).toMillis();
        }
        return this;
    }
    
    /**
     * 添加元数据
     */
    public ProcessingResult withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * 获取处理耗时（毫秒）
     */
    public Long getDurationMs() {
        if (durationMs != null) {
            return durationMs;
        }
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return null;
    }
}