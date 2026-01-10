package com.tbw.cut.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.dto.VideoDownloadDTO;

import java.util.List;

public interface VideoDownloadService extends IService<VideoDownload> {
    
    /**
     * 下载Bilibili视频
     * @param dto 视频下载信息
     * @return 任务ID
     */
    Long downloadVideo(VideoDownloadDTO dto);
    
    /**
     * 更新下载进度
     * @param taskId 任务ID
     * @param progress 进度百分比
     */
    void updateProgress(Long taskId, Integer progress);
    
    /**
     * 完成下载任务
     * @param taskId 任务ID
     * @param localPath 本地存储路径
     */
    void completeDownload(Long taskId, String localPath);
    
    /**
     * 下载失败
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void failDownload(Long taskId, String errorMessage);
    
    /**
     * 获取待下载任务列表
     * @return 待下载任务列表
     */
    List<VideoDownload> getPendingDownloads();
    
    /**
     * 获取下载中任务列表
     * @return 下载中任务列表
     */
    List<VideoDownload> getDownloadingDownloads();
    
    /**
     * 获取已完成下载任务列表
     * @return 已完成下载任务列表
     */
    List<VideoDownload> getCompletedDownloads();
    
    /**
     * 删除下载记录
     * @param taskId 任务ID
     * @return 是否删除成功
     */
    boolean deleteDownloadRecord(Long taskId);
    
    /**
     * 查找最近创建的下载记录
     * @param bvid 视频BVID
     * @param title 视频标题
     * @param limit 限制数量
     * @return 最近的下载记录列表
     */
    List<VideoDownload> findRecentDownloads(String bvid, String title, int limit);
    
    /**
     * 获取最近的下载记录
     * @param limit 限制数量
     * @return 下载记录列表
     */
    List<VideoDownload> getRecentDownloads(int limit);
    
    /**
     * 查找已完成但local_path为空的记录
     * @return 问题记录列表
     */
    List<VideoDownload> findCompletedWithEmptyLocalPath();
}