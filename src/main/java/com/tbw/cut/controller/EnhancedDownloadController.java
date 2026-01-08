package com.tbw.cut.controller;

import com.tbw.cut.service.download.EnhancedDownloadManager;
import com.tbw.cut.service.download.model.DetailedProgress;
import com.tbw.cut.service.download.model.SystemStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 增强下载功能控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/enhanced-download")
public class EnhancedDownloadController {
    
    @Autowired
    private EnhancedDownloadManager enhancedDownloadManager;
    
    /**
     * 获取系统状态
     */
    @GetMapping("/system-status")
    public ResponseEntity<SystemStatus> getSystemStatus() {
        try {
            SystemStatus status = enhancedDownloadManager.getSystemStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取下载进度
     */
    @GetMapping("/progress/{taskId}")
    public ResponseEntity<DetailedProgress> getDownloadProgress(@PathVariable Long taskId) {
        try {
            DetailedProgress progress = enhancedDownloadManager.getDownloadProgress(taskId);
            if (progress != null) {
                return ResponseEntity.ok(progress);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("获取下载进度失败: taskId={}", taskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 暂停下载任务
     */
    @PostMapping("/pause/{taskId}")
    public ResponseEntity<Map<String, String>> pauseDownload(@PathVariable Long taskId) {
        try {
            enhancedDownloadManager.pauseDownload(taskId);
            Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "任务已暂停");
            response.put("taskId", taskId.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("暂停下载任务失败: taskId={}", taskId, e);
            Map<String, String> error = new java.util.HashMap<>();
            error.put("error", "暂停任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 恢复下载任务
     * 注意：由于移除了断点续传功能，个别任务恢复不再支持
     * 建议使用 /resume-all 恢复所有暂停的任务
     */
    @PostMapping("/resume/{taskId}")
    public ResponseEntity<Map<String, String>> resumeDownload(@PathVariable Long taskId) {
        try {
            // 由于移除了ResumableDownloadManager，个别任务恢复功能不再可用
            // 返回适当的错误信息指导用户使用替代方案
            Map<String, String> error = new java.util.HashMap<>();
            error.put("error", "个别任务恢复功能已移除，请使用 /resume-all 恢复所有暂停的任务");
            error.put("suggestion", "使用 POST /api/enhanced-download/resume-all 恢复所有暂停的任务");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("恢复下载任务失败: taskId={}", taskId, e);
            Map<String, String> error = new java.util.HashMap<>();
            error.put("error", "恢复任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 取消下载任务
     */
    @PostMapping("/cancel/{taskId}")
    public ResponseEntity<Map<String, String>> cancelDownload(@PathVariable Long taskId) {
        try {
            enhancedDownloadManager.cancelDownload(taskId);
            Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "任务已取消");
            response.put("taskId", taskId.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("取消下载任务失败: taskId={}", taskId, e);
            Map<String, String> error = new java.util.HashMap<>();
            error.put("error", "取消任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 暂停所有下载任务
     */
    @PostMapping("/pause-all")
    public ResponseEntity<Map<String, String>> pauseAllDownloads() {
        try {
            enhancedDownloadManager.pauseAllDownloads();
            Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "所有任务已暂停");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("暂停所有下载任务失败", e);
            Map<String, String> error = new java.util.HashMap<>();
            error.put("error", "暂停所有任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 恢复所有下载任务
     */
    @PostMapping("/resume-all")
    public ResponseEntity<Map<String, String>> resumeAllDownloads() {
        try {
            enhancedDownloadManager.resumeAllDownloads();
            Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "所有任务已恢复");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("恢复所有下载任务失败", e);
            Map<String, String> error = new java.util.HashMap<>();
            error.put("error", "恢复所有任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 更新并发数量
     */
    @PostMapping("/concurrency")
    public ResponseEntity<Map<String, String>> updateConcurrency(@RequestBody Map<String, Integer> request) {
        try {
            Integer newConcurrency = request.get("concurrency");
            if (newConcurrency == null || newConcurrency <= 0 || newConcurrency > 10) {
                Map<String, String> error = new java.util.HashMap<>();
                error.put("error", "并发数必须在1-10之间");
                return ResponseEntity.badRequest().body(error);
            }
            
            enhancedDownloadManager.updateConcurrency(newConcurrency);
            Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "并发数已更新");
            response.put("concurrency", newConcurrency.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("更新并发数失败", e);
            Map<String, String> error = new java.util.HashMap<>();
            error.put("error", "更新并发数失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 检查文件是否正在使用
     */
    @GetMapping("/file-in-use")
    public ResponseEntity<Map<String, Boolean>> isFileInUse(@RequestParam String filePath) {
        try {
            boolean inUse = enhancedDownloadManager.isFileInUse(filePath);
            Map<String, Boolean> response = new java.util.HashMap<>();
            response.put("inUse", inUse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查文件使用状态失败: filePath={}", filePath, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}