package com.tbw.cut.controller;

import com.tbw.cut.dto.IntegrationRequest;
import com.tbw.cut.dto.IntegrationResult;
import com.tbw.cut.dto.ResponseResult;
import com.tbw.cut.dto.TaskRelationInfo;
import com.tbw.cut.dto.VideoDownloadDTO;
import com.tbw.cut.dto.SubmissionRequestDTO;
import com.tbw.cut.dto.VideoPartInfoDTO;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.service.IntegrationService;
import com.tbw.cut.service.VideoDownloadService;
import com.tbw.cut.service.FrontendVideoDownloadService;
import com.tbw.cut.workflow.model.WorkflowConfig;
import com.tbw.cut.workflow.model.SegmentationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    
    @Autowired
    private IntegrationService integrationService;
    
    /**
     * 下载Bilibili视频（支持集成投稿功能）
     */
    @PostMapping("")
    public ResponseResult<?> downloadVideo(@RequestBody Map<String, Object> requestData) {
        try {
            log.info("收到下载请求: {}", requestData);
            
            // 检查是否为集成请求
            if (isIntegrationRequest(requestData)) {
                return handleIntegrationRequest(requestData);
            } else {
                return handleRegularDownloadRequest(requestData);
            }
            
        } catch (Exception e) {
            log.error("处理下载请求失败", e);
            return ResponseResult.error("处理下载请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理集成请求（下载+投稿）
     */
    private ResponseResult<IntegrationResult> handleIntegrationRequest(Map<String, Object> requestData) {
        try {
            // 将Map转换为IntegrationRequest对象
            IntegrationRequest integrationRequest = convertToIntegrationRequest(requestData);
            
            // 验证集成请求
            String validationError = validateIntegrationRequest(integrationRequest);
            if (validationError != null) {
                return ResponseResult.error(validationError);
            }
            
            // 处理集成请求
            IntegrationResult result = integrationService.processIntegratedRequest(integrationRequest);
            
            if (result.isSuccess()) {
                return ResponseResult.success("集成任务创建成功", result);
            } else {
                return ResponseResult.error(result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("处理集成请求失败", e);
            return ResponseResult.error("处理集成请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理常规下载请求
     */
    private ResponseResult<Long> handleRegularDownloadRequest(Map<String, Object> requestData) {
        try {
            Long taskId = frontendVideoDownloadService.handleFrontendDownloadRequest(requestData);
            
            if (taskId != null) {
                return ResponseResult.success("视频下载任务创建成功", taskId);
            } else {
                return ResponseResult.error("视频下载任务创建失败");
            }
            
        } catch (Exception e) {
            log.error("处理常规下载请求失败", e);
            return ResponseResult.error("处理常规下载请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询下载任务状态
     */
    @GetMapping("/{taskId}")
    public ResponseResult<VideoDownload> getDownloadStatus(@PathVariable("taskId") Long taskId) {
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
    public ResponseResult<Boolean> deleteDownloadRecord(@PathVariable("taskId") Long taskId) {
        try {
            log.info("删除下载记录请求: taskId={}", taskId);
            boolean result = videoDownloadService.deleteDownloadRecord(taskId);
            if (result) {
                return ResponseResult.success("删除成功", true);
            } else {
                return ResponseResult.error("删除失败，未找到记录");
            }
        } catch (Exception e) {
            log.error("删除下载记录失败: taskId={}", taskId, e);
            return ResponseResult.error("删除下载记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取任务关联信息
     */
    @GetMapping("/{taskId}/relation")
    public ResponseResult<TaskRelationInfo> getTaskRelation(@PathVariable("taskId") Long taskId) {
        try {
            TaskRelationInfo relationInfo = integrationService.getTaskRelation(taskId, IntegrationService.TaskRelationType.DOWNLOAD);
            if (relationInfo != null) {
                return ResponseResult.success(relationInfo);
            } else {
                return ResponseResult.error("未找到任务关联信息");
            }
        } catch (Exception e) {
            log.error("获取任务关联信息失败: taskId={}", taskId, e);
            return ResponseResult.error("获取任务关联信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否为集成请求
     */
    private boolean isIntegrationRequest(Map<String, Object> requestData) {
        return requestData.containsKey("enableSubmission") && 
               Boolean.TRUE.equals(requestData.get("enableSubmission")) &&
               requestData.containsKey("submissionRequest");
    }
    
    /**
     * 将Map转换为IntegrationRequest对象
     */
    @SuppressWarnings("unchecked")
    private IntegrationRequest convertToIntegrationRequest(Map<String, Object> requestData) {
        try {
            IntegrationRequest request = new IntegrationRequest();
            request.setEnableSubmission((Boolean) requestData.get("enableSubmission"));
            
            // 处理工作流配置
            if (requestData.containsKey("workflowConfig")) {
                Map<String, Object> workflowConfigMap = (Map<String, Object>) requestData.get("workflowConfig");
                WorkflowConfig workflowConfig = convertToWorkflowConfig(workflowConfigMap);
                request.setWorkflowConfig(workflowConfig);
            }
            
            // 直接保存原始下载请求数据
            Map<String, Object> downloadRequestMap = (Map<String, Object>) requestData.get("downloadRequest");
            if (downloadRequestMap != null) {
                request.setDownloadRequestRaw(downloadRequestMap);
            }
            
            // 转换投稿请求数据
            Map<String, Object> submissionRequestMap = (Map<String, Object>) requestData.get("submissionRequest");
            if (submissionRequestMap != null) {
                SubmissionRequestDTO submissionRequest = convertToSubmissionRequestDTO(submissionRequestMap, downloadRequestMap);
                request.setSubmissionRequest(submissionRequest);
            }
            
            // 设置用户ID（从工作流配置或默认值获取）
            if (request.getWorkflowConfig() != null && request.getWorkflowConfig().getUserId() != null) {
                request.setUserId(request.getWorkflowConfig().getUserId());
            } else {
                request.setUserId("current_user"); // TODO: 从会话获取真实用户ID
            }
            
            return request;
        } catch (Exception e) {
            log.error("转换IntegrationRequest失败", e);
            throw new RuntimeException("请求数据格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 将Map转换为WorkflowConfig对象
     */
    @SuppressWarnings("unchecked")
    private WorkflowConfig convertToWorkflowConfig(Map<String, Object> workflowConfigMap) {
        WorkflowConfig.WorkflowConfigBuilder builder = WorkflowConfig.builder();
        
        // 基本配置
        if (workflowConfigMap.containsKey("userId")) {
            builder.userId((String) workflowConfigMap.get("userId"));
        }
        
        if (workflowConfigMap.containsKey("enableDirectSubmission")) {
            builder.enableDirectSubmission((Boolean) workflowConfigMap.get("enableDirectSubmission"));
        }
        
        if (workflowConfigMap.containsKey("enableClipping")) {
            builder.enableClipping((Boolean) workflowConfigMap.get("enableClipping"));
        }
        
        if (workflowConfigMap.containsKey("enableMerging")) {
            builder.enableMerging((Boolean) workflowConfigMap.get("enableMerging"));
        }
        
        // 分段配置
        if (workflowConfigMap.containsKey("segmentationConfig")) {
            Map<String, Object> segmentationMap = (Map<String, Object>) workflowConfigMap.get("segmentationConfig");
            SegmentationConfig segmentationConfig = convertToSegmentationConfig(segmentationMap);
            builder.segmentationConfig(segmentationConfig);
        }
        
        // 设置时间戳
        builder.createdAt(LocalDateTime.now());
        builder.updatedAt(LocalDateTime.now());
        
        return builder.build();
    }
    
    /**
     * 将Map转换为SegmentationConfig对象
     */
    private SegmentationConfig convertToSegmentationConfig(Map<String, Object> segmentationMap) {
        SegmentationConfig.SegmentationConfigBuilder builder = SegmentationConfig.builder();
        
        if (segmentationMap.containsKey("enabled")) {
            builder.enabled((Boolean) segmentationMap.get("enabled"));
        }
        
        if (segmentationMap.containsKey("segmentDurationSeconds")) {
            Object duration = segmentationMap.get("segmentDurationSeconds");
            if (duration instanceof Integer) {
                builder.segmentDurationSeconds((Integer) duration);
            } else if (duration instanceof Double) {
                builder.segmentDurationSeconds(((Double) duration).intValue());
            }
        }
        
        if (segmentationMap.containsKey("maxSegmentCount")) {
            Object maxCount = segmentationMap.get("maxSegmentCount");
            if (maxCount instanceof Integer) {
                builder.maxSegmentCount((Integer) maxCount);
            } else if (maxCount instanceof Double) {
                builder.maxSegmentCount(((Double) maxCount).intValue());
            }
        }
        
        if (segmentationMap.containsKey("segmentNamingPattern")) {
            builder.segmentNamingPattern((String) segmentationMap.get("segmentNamingPattern"));
        }
        
        if (segmentationMap.containsKey("preserveOriginal")) {
            builder.preserveOriginal((Boolean) segmentationMap.get("preserveOriginal"));
        }
        
        return builder.build();
    }
    
    /**
     * 转换投稿请求数据
     */
    @SuppressWarnings("unchecked")
    private SubmissionRequestDTO convertToSubmissionRequestDTO(Map<String, Object> submissionRequestMap, 
                                                              Map<String, Object> downloadRequestMap) {
        SubmissionRequestDTO submissionRequest = new SubmissionRequestDTO();
        
        // 基本信息
        submissionRequest.setTitle((String) submissionRequestMap.get("title"));
        submissionRequest.setDescription((String) submissionRequestMap.get("description"));
        submissionRequest.setTags((String) submissionRequestMap.get("tags"));
        
        // 分区信息
        Object partitionId = submissionRequestMap.get("partitionId");
        if (partitionId instanceof Number) {
            submissionRequest.setPartitionId(((Number) partitionId).intValue());
        }
        
        // 视频类型
        String videoType = (String) submissionRequestMap.get("videoType");
        if ("ORIGINAL".equals(videoType)) {
            submissionRequest.setVideoType(SubmissionRequestDTO.VideoType.ORIGINAL);
            submissionRequest.setIsOriginal(true);
        } else if ("REPOST".equals(videoType)) {
            submissionRequest.setVideoType(SubmissionRequestDTO.VideoType.REPOST);
            submissionRequest.setIsOriginal(false);
        }
        
        // 转换视频分P信息
        List<Map<String, Object>> videoPartsList = (List<Map<String, Object>>) submissionRequestMap.get("videoParts");
        if (videoPartsList != null && !videoPartsList.isEmpty()) {
            List<VideoPartInfoDTO> videoParts = videoPartsList.stream()
                .map(partMap -> convertToVideoPartInfoDTO(partMap, downloadRequestMap))
                .collect(java.util.stream.Collectors.toList());
            submissionRequest.setVideoParts(videoParts);
        }
        
        return submissionRequest;
    }
    
    /**
     * 转换视频分P信息
     */
    private VideoPartInfoDTO convertToVideoPartInfoDTO(Map<String, Object> partMap, Map<String, Object> downloadRequestMap) {
        VideoPartInfoDTO partInfo = new VideoPartInfoDTO();
        
        String originalTitle = (String) partMap.get("originalTitle");
        partInfo.setTitle(originalTitle);
        partInfo.setSubmissionTitle(originalTitle); // 使用原标题作为投稿标题
        
        // 修复：根据downloadPath和originalTitle构建正确的文件路径
        String correctFilePath = buildCorrectFilePath(partMap, downloadRequestMap);
        partInfo.setExpectedFilePath(correctFilePath);
        partInfo.setFilePath(correctFilePath); // 设置实际文件路径
        
        // CID
        Object cid = partMap.get("cid");
        if (cid instanceof Number) {
            partInfo.setCid(((Number) cid).longValue());
        }
        
        // 时间配置
        partInfo.setStartTime((String) partMap.get("startTime"));
        partInfo.setEndTime((String) partMap.get("endTime"));
        
        // 其他字段设置默认值
        partInfo.setSelected(true);
        partInfo.setPartIndex(1); // 默认值，实际使用时可能需要调整
        
        return partInfo;
    }
    
    /**
     * 根据downloadPath和originalTitle构建正确的文件路径
     */
    private String buildCorrectFilePath(Map<String, Object> partMap, Map<String, Object> downloadRequestMap) {
        String originalTitle = (String) partMap.get("originalTitle");
        
        // 从downloadRequest中获取downloadPath
        String downloadPath = null;
        if (downloadRequestMap != null) {
            downloadPath = (String) downloadRequestMap.get("downloadPath");
        }
        
        if (downloadPath != null && originalTitle != null) {
            // 清理文件名中的非法字符
            String sanitizedTitle = originalTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
            
            // 构建完整路径：downloadPath + originalTitle
            String filePath;
            if (downloadPath.endsWith("/") || downloadPath.endsWith("\\")) {
                filePath = downloadPath + sanitizedTitle;
            } else {
                filePath = downloadPath + "/" + sanitizedTitle;
            }
            
            log.info("构建文件路径: downloadPath={}, originalTitle={}, filePath={}", 
                    downloadPath, originalTitle, filePath);
            
            return filePath;
        }
        
        // 如果无法构建，回退到原有逻辑
        String fallbackPath = (String) partMap.get("filePath");
        log.warn("无法构建正确的文件路径，使用回退路径: downloadPath={}, originalTitle={}, fallbackPath={}", 
                downloadPath, originalTitle, fallbackPath);
        return fallbackPath;
    }
    
    /**
     * 获取当前请求的downloadPath
     * 这是一个临时解决方案，理想情况下应该通过参数传递
     */
    private String getCurrentDownloadPath() {
        // TODO: 这里需要从当前请求上下文中获取downloadPath
        // 由于方法签名限制，这里使用ThreadLocal或其他方式获取
        return null; // 暂时返回null，需要进一步实现
    }
    
    /**
     * 验证集成请求
     */
    private String validateIntegrationRequest(IntegrationRequest request) {
        if (request == null) {
            return "请求数据不能为空";
        }
        
        if (request.getDownloadRequestRaw() == null && request.getDownloadRequest() == null) {
            return "下载请求数据不能为空";
        }
        
        if (request.getEnableSubmission() && request.getSubmissionRequest() == null) {
            return "启用投稿功能时，投稿请求数据不能为空";
        }
        
        if (request.getSubmissionRequest() != null) {
            if (request.getSubmissionRequest().getTitle() == null || 
                request.getSubmissionRequest().getTitle().trim().isEmpty()) {
                return "视频标题不能为空";
            }
            
            if (request.getSubmissionRequest().getPartitionId() == null) {
                return "视频分区不能为空";
            }
            
            if (request.getSubmissionRequest().getVideoType() == null) {
                return "视频类型不能为空";
            }
        }
        
        return null; // 验证通过
    }
}