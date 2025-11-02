package com.tbw.cut.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tbw.cut.entity.VideoDownload;

public interface PartDownloadService extends IService<VideoDownload> {
    
    /**
     * 创建分P下载记录
     * @param videoDownload 分P下载信息
     * @return 任务ID
     */
    Long createPartDownload(VideoDownload videoDownload);
    
    /**
     * 更新分P下载进度
     * @param taskId 任务ID
     * @param progress 进度百分比
     */
    void updatePartProgress(Long taskId, Integer progress);
    
    /**
     * 完成分P下载
     * @param taskId 任务ID
     * @param localPath 本地存储路径
     */
    void completePartDownload(Long taskId, String localPath);
    
    /**
     * 分P下载失败
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void failPartDownload(Long taskId, String errorMessage);
}