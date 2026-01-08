package com.tbw.cut.service.download.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 下载结果模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 下载的文件路径列表
     */
    private List<String> filePaths;
    
    /**
     * 下载开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 下载结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 总下载大小（字节）
     */
    private long totalSize;
    
    /**
     * 下载耗时（毫秒）
     */
    private long duration;
    
    /**
     * 创建成功结果
     * @param filePaths 文件路径列表
     * @return 成功结果
     */
    public static DownloadResult success(List<String> filePaths) {
        return DownloadResult.builder()
            .success(true)
            .filePaths(filePaths)
            .endTime(LocalDateTime.now())
            .build();
    }
    
    /**
     * 创建成功结果
     * @param filePath 单个文件路径
     * @return 成功结果
     */
    public static DownloadResult success(String filePath) {
        return success(java.util.Collections.singletonList(filePath));
    }
    
    /**
     * 创建失败结果
     * @param errorMessage 错误消息
     * @return 失败结果
     */
    public static DownloadResult failure(String errorMessage) {
        return DownloadResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .endTime(LocalDateTime.now())
            .build();
    }
    
    /**
     * 计算下载耗时
     * @return 耗时毫秒数
     */
    public long calculateDuration() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }
}