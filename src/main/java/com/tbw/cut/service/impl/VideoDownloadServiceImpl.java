package com.tbw.cut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.mapper.VideoDownloadMapper;
import com.tbw.cut.service.VideoDownloadService;
import com.tbw.cut.event.DownloadStatusChangeEvent;
import com.tbw.cut.dto.VideoDownloadDTO;
import com.tbw.cut.bilibili.BilibiliUtils;
import com.tbw.cut.utils.FFmpegUtil;
import com.tbw.cut.bilibili.BilibiliService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class VideoDownloadServiceImpl extends ServiceImpl<VideoDownloadMapper, VideoDownload> implements VideoDownloadService {
    
    @Autowired
    private FFmpegUtil ffmpegUtil;
    
    @Autowired
    private BilibiliService bilibiliService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Override
    public Long downloadVideo(VideoDownloadDTO dto) {
        try {
            String videoUrl = dto.getVideoUrl();
            
            // Parse video ID
            String bvid = BilibiliUtils.extractBvidFromUrl(videoUrl);
            String aid = BilibiliUtils.extractAidFromUrl(videoUrl);
            
            // If we can't parse BV or AV ID, try to get video info from Bilibili API
            if ((bvid == null || bvid.isEmpty()) && (aid == null || aid.isEmpty())) {
                log.warn("Could not parse video ID from URL: {}", videoUrl);
                // In a real implementation, you might want to handle this case differently
            }
            
            // Create download task
            VideoDownload download = new VideoDownload();
            download.setBvid(bvid);
            download.setAid(aid);
            download.setDownloadUrl(videoUrl);
            download.setStatus(0); // Pending
            download.setProgress(0);
            download.setCreateTime(LocalDateTime.now());
            download.setUpdateTime(LocalDateTime.now());
            
            this.save(download);
            
            return download.getId();
        } catch (Exception e) {
            log.error("创建下载任务失败", e);
            return null;
        }
    }
    
    @Override
    public void updateProgress(Long taskId, Integer progress) {
        VideoDownload download = this.getById(taskId);
        if (download != null) {
            download.setProgress(progress);
            download.setUpdateTime(LocalDateTime.now());
            this.updateById(download);
            log.info("Updated download progress, Task ID: {}, Progress: {}%", taskId, progress);
        }
    }
    
    @Override
    public void completeDownload(Long taskId, String localPath) {
        VideoDownload download = this.getById(taskId);
        if (download != null) {
            download.setStatus(2); // Completed
            download.setLocalPath(localPath);
            download.setUpdateTime(LocalDateTime.now());
            this.updateById(download);
            log.info("Video download completed, Task ID: {}, Path: {}", taskId, localPath);
            
            // **新增：发布下载完成事件**
            try {
                DownloadStatusChangeEvent event = DownloadStatusChangeEvent.create(taskId, null, 2);
                eventPublisher.publishEvent(event);
                log.info("Published download completion event for video task: taskId={}", taskId);
            } catch (Exception e) {
                log.error("Failed to publish download completion event for video task: taskId={}", taskId, e);
                // 不抛出异常，避免影响主流程
            }
        }
    }
    
    @Override
    public void failDownload(Long taskId, String errorMessage) {
        VideoDownload download = this.getById(taskId);
        if (download != null) {
            download.setStatus(3); // Failed
            download.setUpdateTime(LocalDateTime.now());
            this.updateById(download);
            log.error("Video download failed, Task ID: {}, Error: {}", taskId, errorMessage);
            
            // **新增：发布下载失败事件**
            try {
                DownloadStatusChangeEvent event = DownloadStatusChangeEvent.create(taskId, null, 3);
                eventPublisher.publishEvent(event);
                log.info("Published download failure event for video task: taskId={}", taskId);
            } catch (Exception e) {
                log.error("Failed to publish download failure event for video task: taskId={}", taskId, e);
                // 不抛出异常，避免影响主流程
            }
        }
    }
    
    @Override
    public List<VideoDownload> getPendingDownloads() {
        QueryWrapper<VideoDownload> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 0); // 待下载状态
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }
    
    @Override
    public List<VideoDownload> getDownloadingDownloads() {
        QueryWrapper<VideoDownload> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1); // 下载中状态
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }
    
    @Override
    public List<VideoDownload> getCompletedDownloads() {
        QueryWrapper<VideoDownload> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("status", 2, 3); // 完成或失败状态
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }
    
    @Override
    public boolean deleteDownloadRecord(Long taskId) {
        return this.removeById(taskId);
    }
    
    @Override
    public List<VideoDownload> findRecentDownloads(String bvid, String title, int limit) {
        QueryWrapper<VideoDownload> queryWrapper = new QueryWrapper<>();
        
        if (bvid != null && !bvid.isEmpty()) {
            queryWrapper.eq("bvid", bvid);
        }
        
        if (title != null && !title.isEmpty()) {
            queryWrapper.eq("title", title);
        }
        
        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("LIMIT " + limit);
        
        return this.list(queryWrapper);
    }
    
    @Override
    public List<VideoDownload> getRecentDownloads(int limit) {
        QueryWrapper<VideoDownload> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("LIMIT " + limit);
        return this.list(queryWrapper);
    }
    
    @Override
    public List<VideoDownload> findCompletedWithEmptyLocalPath() {
        QueryWrapper<VideoDownload> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 2); // 已完成
        queryWrapper.and(wrapper -> wrapper.isNull("local_path").or().eq("local_path", ""));
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }
}