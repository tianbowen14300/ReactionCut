package com.tbw.cut.service;

import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.entity.TaskOutputSegment;
import java.util.List;
import java.util.UUID;

public interface FFmpegService {
    
    /**
     * 剪辑和合并视频
     * @param taskId 任务ID
     * @param sourceVideos 源视频列表
     * @param outputPath 输出路径
     * @return 合并后的视频路径
     */
    String clipAndMergeVideos(String taskId, List<TaskSourceVideo> sourceVideos, String outputPath);
    
    /**
     * 分段切割视频
     * @param taskId 任务ID
     * @param inputPath 输入视频路径
     * @param segmentTime 分段时长（秒）
     * @param outputPattern 输出文件名模式
     * @return 分段文件路径列表
     */
    List<String> segmentVideo(String taskId, String inputPath, int segmentTime, String outputPattern);
}