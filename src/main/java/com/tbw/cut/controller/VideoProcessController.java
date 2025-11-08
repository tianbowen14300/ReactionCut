package com.tbw.cut.controller;

import com.tbw.cut.entity.MergedVideo;
import com.tbw.cut.service.VideoProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/video-process")
public class VideoProcessController {
    
    @Autowired
    private VideoProcessService videoProcessService;
    
    /**
     * 视频剪辑
     */
    @PostMapping("/{taskId}/clip")
    public Result<List<String>> clipVideos(@PathVariable("taskId") String taskId) {
        try {
            List<String> clipPaths = videoProcessService.clipVideos(taskId);
            return Result.success(clipPaths);
        } catch (Exception e) {
            log.error("视频剪辑时发生异常，任务ID: {}", taskId, e);
            return Result.error("视频剪辑失败: " + e.getMessage());
        }
    }
    
    /**
     * 视频合并
     */
    @PostMapping("/{taskId}/merge")
    public Result<String> mergeVideos(@PathVariable("taskId") String taskId) {
        try {
            String mergedPath = videoProcessService.mergeVideos(taskId);
            return Result.success(mergedPath);
        } catch (Exception e) {
            log.error("视频合并时发生异常，任务ID: {}", taskId, e);
            return Result.error("视频合并失败: " + e.getMessage());
        }
    }
    
    /**
     * 视频分段
     */
    @PostMapping("/{taskId}/segment")
    public Result<List<String>> segmentVideo(@PathVariable("taskId") String taskId) {
        try {
            List<String> segmentPaths = videoProcessService.segmentVideo(taskId);
            videoProcessService.saveOutputSegments(taskId, segmentPaths);
            return Result.success(segmentPaths);
        } catch (Exception e) {
            log.error("视频分段时发生异常，任务ID: {}", taskId, e);
            return Result.error("视频分段失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取合并后的视频信息
     */
    @GetMapping("/{taskId}/merged-videos")
    public Result<List<MergedVideo>> getMergedVideos(@PathVariable("taskId") String taskId) {
        try {
            List<MergedVideo> mergedVideos = videoProcessService.getMergedVideos(taskId);
            return Result.success(mergedVideos);
        } catch (Exception e) {
            log.error("获取合并视频信息时发生异常，任务ID: {}", taskId, e);
            return Result.error("获取合并视频信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 统一响应结果类
     */
    public static class Result<T> {
        private int code;
        private String message;
        private T data;
        
        public static <T> Result<T> success(T data) {
            Result<T> result = new Result<>();
            result.code = 0;
            result.message = "success";
            result.data = data;
            return result;
        }
        
        public static <T> Result<T> error(String message) {
            Result<T> result = new Result<>();
            result.code = -1;
            result.message = message;
            return result;
        }
        
        // Getters and setters
        public int getCode() {
            return code;
        }
        
        public void setCode(int code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public T getData() {
            return data;
        }
        
        public void setData(T data) {
            this.data = data;
        }
    }
}