package com.tbw.cut.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tbw.cut.entity.VideoProcessTask;
import com.tbw.cut.dto.VideoProcessTaskDTO;

public interface VideoProcessService extends IService<VideoProcessTask> {
    
    /**
     * 创建视频处理任务
     * @param dto 任务信息
     * @return 任务ID
     */
    Long createProcessTask(VideoProcessTaskDTO dto);
    
    /**
     * 执行视频处理任务
     * @param taskId 任务ID
     */
    void executeProcessTask(Long taskId);
    
    /**
     * 更新处理进度
     * @param taskId 任务ID
     * @param progress 进度百分比
     */
    void updateProgress(Long taskId, Integer progress);
    
    /**
     * 完成处理任务
     * @param taskId 任务ID
     * @param outputPath 输出文件路径
     */
    void completeProcessTask(Long taskId, String outputPath);
    
    /**
     * 处理失败
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void failProcessTask(Long taskId, String errorMessage);
}