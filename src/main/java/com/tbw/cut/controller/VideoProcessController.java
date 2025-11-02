package com.tbw.cut.controller;

import com.tbw.cut.dto.VideoProcessTaskDTO;
import com.tbw.cut.dto.ResponseResult;
import com.tbw.cut.entity.VideoProcessTask;
import com.tbw.cut.service.VideoProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/video/process")
public class VideoProcessController {
    
    @Autowired
    private VideoProcessService videoProcessService;
    
    /**
     * 创建视频处理任务
     */
    @PostMapping("")
    public ResponseResult<Long> createProcessTask(@RequestBody VideoProcessTaskDTO dto) {
        try {
            Long taskId = videoProcessService.createProcessTask(dto);
            if (taskId != null) {
                return ResponseResult.success("视频处理任务创建成功", taskId);
            } else {
                return ResponseResult.error("视频处理任务创建失败");
            }
        } catch (Exception e) {
            log.error("创建视频处理任务失败", e);
            return ResponseResult.error("创建视频处理任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询处理任务状态
     */
    @GetMapping("/{taskId}")
    public ResponseResult<VideoProcessTask> getProcessStatus(@PathVariable Long taskId) {
        try {
            VideoProcessTask task = videoProcessService.getById(taskId);
            if (task != null) {
                return ResponseResult.success(task);
            } else {
                return ResponseResult.error("任务不存在");
            }
        } catch (Exception e) {
            log.error("查询处理任务状态失败", e);
            return ResponseResult.error("查询处理任务状态失败: " + e.getMessage());
        }
    }
}