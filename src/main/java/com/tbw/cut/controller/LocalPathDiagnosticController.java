package com.tbw.cut.controller;

import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.service.VideoDownloadService;
import com.tbw.cut.service.PartDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * local_path字段诊断控制器
 * 用于调试和诊断local_path字段为空的问题
 */
@Slf4j
@RestController
@RequestMapping("/api/diagnostic/local-path")
public class LocalPathDiagnosticController {
    
    @Autowired
    private VideoDownloadService videoDownloadService;
    
    @Autowired
    private PartDownloadService partDownloadService;
    
    /**
     * 获取最近的下载记录及其local_path状态
     */
    @GetMapping("/recent")
    public Map<String, Object> getRecentDownloads(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取最近的下载记录
            List<VideoDownload> recentDownloads = videoDownloadService.getRecentDownloads(limit);
            
            int totalCount = recentDownloads.size();
            int emptyLocalPathCount = 0;
            int nonEmptyLocalPathCount = 0;
            int completedWithEmptyPath = 0;
            int completedWithNonEmptyPath = 0;
            
            for (VideoDownload download : recentDownloads) {
                if (download.getLocalPath() == null || download.getLocalPath().trim().isEmpty()) {
                    emptyLocalPathCount++;
                    if (download.getStatus() == 2) { // 已完成但local_path为空
                        completedWithEmptyPath++;
                    }
                } else {
                    nonEmptyLocalPathCount++;
                    if (download.getStatus() == 2) {
                        completedWithNonEmptyPath++;
                    }
                }
            }
            
            result.put("totalCount", totalCount);
            result.put("emptyLocalPathCount", emptyLocalPathCount);
            result.put("nonEmptyLocalPathCount", nonEmptyLocalPathCount);
            result.put("completedWithEmptyPath", completedWithEmptyPath);
            result.put("completedWithNonEmptyPath", completedWithNonEmptyPath);
            result.put("downloads", recentDownloads);
            
            // 问题诊断
            if (completedWithEmptyPath > 0) {
                result.put("issue", "发现已完成但local_path为空的记录: " + completedWithEmptyPath + " 条");
                result.put("severity", "HIGH");
            } else if (emptyLocalPathCount > 0) {
                result.put("issue", "发现local_path为空的记录: " + emptyLocalPathCount + " 条（可能是未完成的下载）");
                result.put("severity", "LOW");
            } else {
                result.put("issue", "未发现local_path相关问题");
                result.put("severity", "NONE");
            }
            
        } catch (Exception e) {
            log.error("获取最近下载记录失败", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 测试local_path字段的更新功能
     */
    @PostMapping("/test-update/{taskId}")
    public Map<String, Object> testLocalPathUpdate(@PathVariable Long taskId, 
                                                   @RequestParam String testPath) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== 测试local_path字段更新 ===");
            log.info("任务ID: {}, 测试路径: {}", taskId, testPath);
            
            // 获取更新前的记录
            VideoDownload beforeUpdate = videoDownloadService.getById(taskId);
            if (beforeUpdate == null) {
                result.put("error", "找不到指定的下载记录: " + taskId);
                return result;
            }
            
            Map<String, Object> beforeUpdateMap = new HashMap<>();
            beforeUpdateMap.put("id", beforeUpdate.getId());
            beforeUpdateMap.put("status", beforeUpdate.getStatus());
            beforeUpdateMap.put("progress", beforeUpdate.getProgress());
            beforeUpdateMap.put("localPath", beforeUpdate.getLocalPath() != null ? beforeUpdate.getLocalPath() : "null");
            result.put("beforeUpdate", beforeUpdateMap);
            
            // 执行更新
            partDownloadService.completePartDownload(taskId, testPath);
            
            // 获取更新后的记录
            VideoDownload afterUpdate = videoDownloadService.getById(taskId);
            Map<String, Object> afterUpdateMap = new HashMap<>();
            afterUpdateMap.put("id", afterUpdate.getId());
            afterUpdateMap.put("status", afterUpdate.getStatus());
            afterUpdateMap.put("progress", afterUpdate.getProgress());
            afterUpdateMap.put("localPath", afterUpdate.getLocalPath() != null ? afterUpdate.getLocalPath() : "null");
            result.put("afterUpdate", afterUpdateMap);
            
            // 验证更新结果
            boolean updateSuccess = testPath.equals(afterUpdate.getLocalPath());
            result.put("updateSuccess", updateSuccess);
            
            if (updateSuccess) {
                result.put("message", "local_path字段更新成功");
            } else {
                result.put("message", "local_path字段更新失败");
                result.put("expected", testPath);
                result.put("actual", afterUpdate.getLocalPath());
            }
            
        } catch (Exception e) {
            log.error("测试local_path字段更新失败", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 修复已完成但local_path为空的记录
     */
    @PostMapping("/fix-empty-paths")
    public Map<String, Object> fixEmptyLocalPaths() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查找已完成但local_path为空的记录
            List<VideoDownload> problematicDownloads = videoDownloadService.findCompletedWithEmptyLocalPath();
            
            int fixedCount = 0;
            int failedCount = 0;
            
            for (VideoDownload download : problematicDownloads) {
                try {
                    // 尝试根据下载配置重建文件路径
                    String reconstructedPath = reconstructFilePath(download);
                    if (reconstructedPath != null) {
                        download.setLocalPath(reconstructedPath);
                        boolean updated = videoDownloadService.updateById(download);
                        if (updated) {
                            fixedCount++;
                            log.info("修复记录: id={}, 重建路径={}", download.getId(), reconstructedPath);
                        } else {
                            failedCount++;
                            log.error("更新失败: id={}", download.getId());
                        }
                    } else {
                        failedCount++;
                        log.warn("无法重建路径: id={}", download.getId());
                    }
                } catch (Exception e) {
                    failedCount++;
                    log.error("修复记录失败: id={}", download.getId(), e);
                }
            }
            
            result.put("totalProblematic", problematicDownloads.size());
            result.put("fixedCount", fixedCount);
            result.put("failedCount", failedCount);
            result.put("message", String.format("修复完成: 成功 %d 条, 失败 %d 条", fixedCount, failedCount));
            
        } catch (Exception e) {
            log.error("修复empty local_path失败", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 重建文件路径（简单实现）
     */
    private String reconstructFilePath(VideoDownload download) {
        try {
            // 基于下载配置重建可能的文件路径
            String title = download.getTitle();
            if (title != null) {
                title = title.replaceAll("[\\\\/:*?\"<>|]", "_");
            } else {
                title = "unknown";
            }
            
            String fileName = download.getCurrentPart() + ".mp4";
            String basePath = System.getProperty("user.home") + "/Downloads/" + title;
            
            return basePath + "/" + fileName;
        } catch (Exception e) {
            log.error("重建文件路径失败", e);
            return null;
        }
    }
}