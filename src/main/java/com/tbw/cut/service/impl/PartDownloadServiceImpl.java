package com.tbw.cut.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.mapper.VideoDownloadMapper;
import com.tbw.cut.service.PartDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class PartDownloadServiceImpl extends ServiceImpl<VideoDownloadMapper, VideoDownload> implements PartDownloadService {
    
    @Override
    public Long createPartDownload(VideoDownload videoDownload) {
        try {
            // 确保设置创建和更新时间
            if (videoDownload.getCreateTime() == null) {
                videoDownload.setCreateTime(LocalDateTime.now());
            }
            if (videoDownload.getUpdateTime() == null) {
                videoDownload.setUpdateTime(LocalDateTime.now());
            }
            
            this.save(videoDownload);
            log.info("成功创建分P下载记录，任务ID: {}", videoDownload.getId());
            return videoDownload.getId();
        } catch (Exception e) {
            log.error("创建分P下载记录失败", e);
            return null;
        }
    }
    
    @Override
    public void updatePartProgress(Long taskId, Integer progress) {
        VideoDownload download = this.getById(taskId);
        if (download != null) {
            download.setProgress(progress);
            download.setUpdateTime(LocalDateTime.now());
            this.updateById(download);
            log.info("Updated part download progress, Task ID: {}, Progress: {}%", taskId, progress);
        }
    }
    
    @Override
    public void completePartDownload(Long taskId, String localPath) {
        VideoDownload download = this.getById(taskId);
        if (download != null) {
            download.setStatus(2); // Completed
            download.setLocalPath(localPath);
            download.setUpdateTime(LocalDateTime.now());
            download.setProgress(100); // 设置进度为100%
            this.updateById(download);
            log.info("Part download completed, Task ID: {}, Path: {}", taskId, localPath);
        }
    }
    
    @Override
    public void failPartDownload(Long taskId, String errorMessage) {
        VideoDownload download = this.getById(taskId);
        if (download != null) {
            download.setStatus(3); // Failed
            download.setUpdateTime(LocalDateTime.now());
            this.updateById(download);
            log.error("Part download failed, Task ID: {}, Error: {}", taskId, errorMessage);
        }
    }
}