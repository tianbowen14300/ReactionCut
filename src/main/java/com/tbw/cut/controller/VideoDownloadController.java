package com.tbw.cut.controller;

import com.tbw.cut.dto.ResponseResult;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.service.VideoDownloadService;
import com.tbw.cut.service.FrontendVideoDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/video/download")
public class VideoDownloadController {
    
    @Autowired
    private VideoDownloadService videoDownloadService;
    
    @Autowired
    private FrontendVideoDownloadService frontendVideoDownloadService;
    
    /**
     * 下载Bilibili视频（兼容前端新格式）
     */
    @PostMapping("")
    public ResponseResult<Long> downloadVideo(@RequestBody Map<String, Object> requestData) {
        try {
            log.info("收到下载请求: {}", requestData);
            
            Long taskId = frontendVideoDownloadService.handleFrontendDownloadRequest(requestData);
            
            if (taskId != null) {
                return ResponseResult.success("视频下载任务创建成功", taskId);
            } else {
                return ResponseResult.error("视频下载任务创建失败");
            }
        } catch (Exception e) {
            log.error("下载视频失败", e);
            return ResponseResult.error("下载视频失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询下载任务状态
     */
    @GetMapping("/{taskId}")
    public ResponseResult<VideoDownload> getDownloadStatus(@PathVariable Long taskId) {
        try {
            VideoDownload download = videoDownloadService.getById(taskId);
            if (download != null) {
                return ResponseResult.success(download);
            } else {
                return ResponseResult.error("未找到下载任务");
            }
        } catch (Exception e) {
            log.error("查询下载任务状态失败", e);
            return ResponseResult.error("查询下载任务状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取待下载任务列表
     */
    @GetMapping("/pending")
    public ResponseResult<List<VideoDownload>> getPendingDownloads() {
        try {
            List<VideoDownload> downloads = videoDownloadService.getPendingDownloads();
            return ResponseResult.success(downloads);
        } catch (Exception e) {
            log.error("获取待下载任务列表失败", e);
            return ResponseResult.error("获取待下载任务列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取下载中任务列表
     */
    @GetMapping("/downloading")
    public ResponseResult<List<VideoDownload>> getDownloadingDownloads() {
        try {
            List<VideoDownload> downloads = videoDownloadService.getDownloadingDownloads();
            return ResponseResult.success(downloads);
        } catch (Exception e) {
            log.error("获取下载中任务列表失败", e);
            return ResponseResult.error("获取下载中任务列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取已完成下载任务列表
     */
    @GetMapping("/completed")
    public ResponseResult<List<VideoDownload>> getCompletedDownloads() {
        try {
            List<VideoDownload> downloads = videoDownloadService.getCompletedDownloads();
            return ResponseResult.success(downloads);
        } catch (Exception e) {
            log.error("获取已完成下载任务列表失败", e);
            return ResponseResult.error("获取已完成下载任务列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除下载记录
     */
    @DeleteMapping("/{taskId}")
    public ResponseResult<Boolean> deleteDownloadRecord(@PathVariable Long taskId) {
        try {
            boolean result = videoDownloadService.deleteDownloadRecord(taskId);
            if (result) {
                return ResponseResult.success("删除成功", true);
            } else {
                return ResponseResult.error("删除失败，未找到记录");
            }
        } catch (Exception e) {
            log.error("删除下载记录失败", e);
            return ResponseResult.error("删除下载记录失败: " + e.getMessage());
        }
    }
}