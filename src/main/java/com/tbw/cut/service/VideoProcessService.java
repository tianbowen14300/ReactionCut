package com.tbw.cut.service;

import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.entity.TaskOutputSegment;
import com.tbw.cut.entity.MergedVideo;
import java.util.List;

public interface VideoProcessService {
    
    /**
     * 视频剪辑
     * @param taskId 任务ID
     * @return 剪辑后的文件路径列表
     */
    List<String> clipVideos(String taskId);
    
    /**
     * 视频合并
     * @param taskId 任务ID
     * @return 合并后的文件路径
     */
    String mergeVideos(String taskId);

    /**
     * 视频分段
     * @param taskId 任务ID
     * @return 分段后的文件路径列表
     */
    List<String> segmentVideo(String taskId);
    
    /**
     * 获取任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    SubmissionTask getTaskDetail(String taskId);
    
    /**
     * 获取任务源视频列表
     * @param taskId 任务ID
     * @return 源视频列表
     */
    List<TaskSourceVideo> getSourceVideos(String taskId);
    
    /**
     * 保存输出分段
     * @param taskId 任务ID
     * @param segmentPaths 分段文件路径列表
     */
    void saveOutputSegments(String taskId, List<String> segmentPaths);
    
    /**
     * 保存合并后的视频信息
     * @param taskId 任务ID
     * @param mergedVideoPath 合并后的视频路径
     */
    void saveMergedVideo(String taskId, String mergedVideoPath);
    
    /**
     * 获取合并后的视频信息
     * @param taskId 任务ID
     * @return 合并后的视频信息列表
     */
    List<MergedVideo> getMergedVideos(String taskId);
    
    /**
     * 从数据库获取合并后的视频路径
     * @param taskId 任务ID
     * @return 合并后的视频路径
     */
    String getMergedVideoPathFromDatabase(String taskId);
    
    // ==================== 异步处理方法 ====================
    
    /**
     * 异步视频剪辑处理
     * @param taskId 任务ID
     * @param callback 完成回调
     */
    void clipVideosAsync(String taskId, ProcessingCallback callback);
    
    /**
     * 异步视频合并处理
     * @param taskId 任务ID
     * @param callback 完成回调
     */
    void mergeVideosAsync(String taskId, MergingCallback callback);
    
    /**
     * 异步视频分段处理
     * @param taskId 任务ID
     * @param callback 完成回调
     */
    void segmentVideoAsync(String taskId, ProcessingCallback callback);
    
    // ==================== 回调接口定义 ====================
    
    /**
     * 处理完成回调接口（用于剪辑和分段）
     */
    @FunctionalInterface
    interface ProcessingCallback {
        void onComplete(boolean success, List<String> outputPaths, String errorMessage);
    }
    
    /**
     * 合并完成回调接口
     */
    @FunctionalInterface
    interface MergingCallback {
        void onComplete(boolean success, String outputPath, String errorMessage);
    }
}