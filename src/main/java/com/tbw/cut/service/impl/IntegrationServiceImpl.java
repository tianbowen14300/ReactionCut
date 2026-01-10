package com.tbw.cut.service.impl;

import com.tbw.cut.dto.*;
import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskRelation;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.entity.TaskSourceVideo;
import com.tbw.cut.event.DownloadStatusChangeEvent;
import com.tbw.cut.mapper.TaskRelationMapper;
import com.tbw.cut.mapper.VideoPartsMapper;
import com.tbw.cut.service.IntegrationService;
import com.tbw.cut.service.StatusSyncService;
import com.tbw.cut.service.SubmissionTaskService;
import com.tbw.cut.service.VideoDownloadService;
import com.tbw.cut.service.FrontendVideoDownloadService;
import com.tbw.cut.service.WorkflowConfigurationService;
import com.tbw.cut.service.TaskRelationService;
import com.tbw.cut.workflow.service.WorkflowEngine;
import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.model.WorkflowConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.List;

/**
 * 视频下载与投稿集成服务实现
 */
@Slf4j
@Service
public class IntegrationServiceImpl implements IntegrationService {
    
    @Autowired
    private VideoDownloadService videoDownloadService;
    
    @Autowired
    private FrontendVideoDownloadService frontendVideoDownloadService;
    
    @Autowired
    private SubmissionTaskService submissionTaskService;
    
    @Autowired
    private TaskRelationMapper taskRelationMapper;
    
    @Autowired
    private StatusSyncService statusSyncService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    @Autowired
    private WorkflowConfigurationService workflowConfigurationService;
    
    @Autowired
    private TaskRelationService taskRelationService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public IntegrationResult processIntegratedRequest(IntegrationRequest request) {
        log.info("Processing integration request: enableSubmission={}, userId={}, hasWorkflowConfig={}", 
                request.getEnableSubmission(), request.getUserId(), request.hasWorkflowConfig());
        
        try {
            // 验证请求
            if (request.getDownloadRequestRaw() == null && request.getDownloadRequest() == null) {
                return IntegrationResult.failure("下载请求不能为空", "INVALID_DOWNLOAD_REQUEST");
            }
            
            // 检查是否使用工作流引擎
            if (request.hasWorkflowConfig()) {
                return processWithWorkflowEngine(request);
            } else {
                return processWithLegacyMethod(request);
            }
            
        } catch (DownloadTaskCreationException e) {
            log.error("Failed to create download task", e);
            throw new IntegrationException("下载任务创建失败: " + e.getMessage(), "DOWNLOAD_TASK_CREATION_FAILED", e);
        } catch (SubmissionTaskCreationException e) {
            log.error("Failed to create submission task", e);
            throw new IntegrationException("投稿任务创建失败: " + e.getMessage(), "SUBMISSION_TASK_CREATION_FAILED", e);
        } catch (TaskSourceVideoCreationException e) {
            log.error("Failed to create task source video data: {}", e.getDetailedMessage(), e);
            throw new IntegrationException("分P配置数据处理失败: " + e.getDetailedMessage(), "TASK_SOURCE_VIDEO_CREATION_FAILED", e);
        } catch (TaskRelationCreationException e) {
            log.error("Failed to create task relation", e);
            // 检查是否是外键约束违反
            if (e.getCause() != null && e.getCause().getMessage().contains("foreign key constraint")) {
                throw new IntegrationException("任务关联创建失败，可能是下载任务或投稿任务不存在: " + e.getMessage(), "FOREIGN_KEY_CONSTRAINT_VIOLATION", e);
            } else {
                throw new IntegrationException("任务关联创建失败: " + e.getMessage(), "TASK_RELATION_CREATION_FAILED", e);
            }
        } catch (Exception e) {
            log.error("Integration processing failed", e);
            throw new IntegrationException("集成处理失败: " + e.getMessage(), "INTEGRATION_PROCESSING_FAILED", e);
        }
    }
    
    /**
     * 使用工作流引擎处理集成请求
     */
    private IntegrationResult processWithWorkflowEngine(IntegrationRequest request) {
        log.info("Processing integration request with workflow engine");
        
        // 验证工作流配置
        if (request.hasWorkflowConfig()) {
            WorkflowConfig workflowConfig = request.getWorkflowConfig();
            if (!workflowConfig.isValid()) {
                String errorMessage = "工作流配置验证失败: " + workflowConfig.getValidationError();
                log.error(errorMessage);
                throw new IntegrationException(errorMessage, "INVALID_WORKFLOW_CONFIG");
            }
        }
        
        // 创建下载任务 - 优先使用原始前端数据
        Long downloadTaskId;
        if (request.getDownloadRequestRaw() != null) {
            downloadTaskId = frontendVideoDownloadService.handleFrontendDownloadRequest(request.getDownloadRequestRaw());
        } else {
            downloadTaskId = createDownloadTaskUsingFrontendService(request.getDownloadRequest());
        }
        log.info("Created download task with ID: {}", downloadTaskId);
        
        // 如果不启用投稿功能，只返回下载任务结果
        if (!request.isValidIntegrationRequest()) {
            return IntegrationResult.downloadOnlySuccess(downloadTaskId);
        }
        
        // 创建投稿任务
        String submissionTaskId = createSubmissionTask(request.getSubmissionRequest(), downloadTaskId);
        log.info("Created submission task with ID: {}", submissionTaskId);
        
        // 创建任务关联
        Long relationId = createTaskRelation(downloadTaskId, submissionTaskId);
        log.info("Created task relation with ID: {}", relationId);
        
        // 保存工作流配置，等待下载完成后启动
        workflowConfigurationService.saveConfigForTask(submissionTaskId, request.getEffectiveWorkflowConfig());
        
        // 修改：不立即启动工作流，而是返回等待状态
        log.info("工作流配置已保存，等待下载完成后启动: submissionTaskId={}", submissionTaskId);
        return IntegrationResult.successWithPendingWorkflow(downloadTaskId, submissionTaskId, relationId);
    }
    
    /**
     * 使用传统方法处理集成请求（向后兼容）
     */
    private IntegrationResult processWithLegacyMethod(IntegrationRequest request) {
        log.info("Processing integration request with legacy method");
        
        // 创建下载任务 - 优先使用原始前端数据
        Long downloadTaskId;
        if (request.getDownloadRequestRaw() != null) {
            downloadTaskId = frontendVideoDownloadService.handleFrontendDownloadRequest(request.getDownloadRequestRaw());
        } else {
            downloadTaskId = createDownloadTaskUsingFrontendService(request.getDownloadRequest());
        }
        log.info("Created download task with ID: {}", downloadTaskId);
        
        // 如果不启用投稿功能，只返回下载任务结果
        if (!request.isValidIntegrationRequest()) {
            return IntegrationResult.downloadOnlySuccess(downloadTaskId);
        }
        
        // 创建投稿任务
        String submissionTaskId = createSubmissionTask(request.getSubmissionRequest(), downloadTaskId);
        log.info("Created submission task with ID: {}", submissionTaskId);
        
        // 创建任务关联
        Long relationId = createTaskRelation(downloadTaskId, submissionTaskId);
        log.info("Created task relation with ID: {}", relationId);
        
        return IntegrationResult.success(downloadTaskId, submissionTaskId, relationId);
    }
    
    @Override
    public void syncTaskStatus(Long downloadTaskId, Integer downloadStatus) {
        log.debug("Syncing task status for download task: {}, status: {}", downloadTaskId, downloadStatus);
        
        try {
            // 使用状态同步服务处理状态变化
            statusSyncService.syncDownloadStatusToSubmission(downloadTaskId, downloadStatus);
            
            // 注意：不在这里发布事件，因为事件已经由下载服务发布，
            // 我们现在正在处理该事件，再次发布会造成无限循环
            log.debug("Successfully synced task status for download task: {}", downloadTaskId);
            
        } catch (Exception e) {
            log.error("Failed to sync status for download task: {}", downloadTaskId, e);
            // 不抛出异常，避免影响主流程
        }
    }
    
    @Override
    public TaskRelationInfo getTaskRelation(Long taskId, TaskRelationType taskType) {
        try {
            Optional<TaskRelation> relationOpt;
            
            if (taskType == TaskRelationType.DOWNLOAD) {
                relationOpt = taskRelationMapper.findByDownloadTaskId(taskId);
            } else {
                // 对于SUBMISSION类型，需要先转换taskId为String
                relationOpt = taskRelationMapper.findBySubmissionTaskId(String.valueOf(taskId));
            }
            
            if (!relationOpt.isPresent()) {
                return null;
            }
            
            return buildTaskRelationInfo(relationOpt.get());
            
        } catch (Exception e) {
            log.error("Failed to get task relation for taskId: {}, taskType: {}", taskId, taskType, e);
            return null;
        }
    }
    
    @Override
    public TaskRelationInfo getTaskRelationBySubmissionId(String submissionTaskId) {
        try {
            Optional<TaskRelation> relationOpt = taskRelationMapper.findBySubmissionTaskId(submissionTaskId);
            
            if (!relationOpt.isPresent()) {
                return null;
            }
            
            return buildTaskRelationInfo(relationOpt.get());
            
        } catch (Exception e) {
            log.error("Failed to get task relation for submission task: {}", submissionTaskId, e);
            return null;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTaskRelation(Long downloadTaskId, String submissionTaskId) {
        try {
            Optional<TaskRelation> relationOpt = taskRelationMapper.findByDownloadAndSubmissionTaskId(
                    downloadTaskId, submissionTaskId);
            
            if (!relationOpt.isPresent()) {
                log.warn("Task relation not found: downloadTaskId={}, submissionTaskId={}", 
                        downloadTaskId, submissionTaskId);
                return false;
            }
            
            TaskRelation relation = relationOpt.get();
            relation.markFailed();
            taskRelationMapper.updateById(relation);
            
            log.info("Removed task relation: downloadTaskId={}, submissionTaskId={}", 
                    downloadTaskId, submissionTaskId);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to remove task relation: downloadTaskId={}, submissionTaskId={}", 
                    downloadTaskId, submissionTaskId, e);
            return false;
        }
    }
    
    /**
     * 使用前端服务创建下载任务
     */
    private Long createDownloadTaskUsingFrontendService(VideoDownloadDTO downloadRequest) {
        try {
            // 将VideoDownloadDTO转换为前端格式的Map
            Map<String, Object> frontendRequest = convertToFrontendFormat(downloadRequest);
            return frontendVideoDownloadService.handleFrontendDownloadRequest(frontendRequest);
        } catch (Exception e) {
            throw new DownloadTaskCreationException("Failed to create download task using frontend service", e);
        }
    }
    
    /**
     * 将VideoDownloadDTO转换为前端格式
     */
    private Map<String, Object> convertToFrontendFormat(VideoDownloadDTO downloadRequest) {
        Map<String, Object> frontendRequest = new java.util.HashMap<>();
        
        // 设置视频URL
        frontendRequest.put("videoUrl", downloadRequest.getVideoUrl());
        
        // 转换分P列表
        if (downloadRequest.getPages() != null && !downloadRequest.getPages().isEmpty()) {
            java.util.List<Map<String, Object>> parts = downloadRequest.getPages().stream()
                .map(cid -> {
                    Map<String, Object> part = new java.util.HashMap<>();
                    part.put("cid", cid);
                    part.put("title", "Part " + cid); // 默认标题
                    return part;
                })
                .collect(java.util.stream.Collectors.toList());
            frontendRequest.put("parts", parts);
        }
        
        // 设置配置信息
        Map<String, Object> config = new java.util.HashMap<>();
        config.put("resolution", downloadRequest.getQuality());
        
        // 根据音视频选项设置content
        if (Boolean.TRUE.equals(downloadRequest.getAudioOnly())) {
            config.put("content", "audio_only");
        } else if (Boolean.TRUE.equals(downloadRequest.getVideoOnly())) {
            config.put("content", "video_only");
        } else {
            config.put("content", "audio_video");
        }
        
        frontendRequest.put("config", config);
        
        return frontendRequest;
    }
    
    /**
     * 创建投稿任务
     */
    private String createSubmissionTask(SubmissionRequestDTO submissionRequest, Long downloadTaskId) {
        try {
            log.info("开始创建投稿任务，title: {}", submissionRequest != null ? submissionRequest.getTitle() : "null");
            
            // 转换SubmissionRequestDTO为SubmissionTask实体
            SubmissionTask task = convertToSubmissionTask(submissionRequest);
            log.info("转换后的SubmissionTask title: {}, partitionId: {}", 
                    task != null ? task.getTitle() : "null",
                    task != null ? task.getPartitionId() : "null");
            
            // 设置待处理状态（等待下载完成后再处理）
            task.setStatus(SubmissionTask.TaskStatus.PENDING);
            
            // 处理分P配置数据
            List<TaskSourceVideo> sourceVideos = processVideoPartsConfiguration(submissionRequest);
            log.info("处理分P配置完成，源视频数量: {}", sourceVideos.size());
            
            // 创建任务（传入实际的源视频列表）
            String taskId = submissionTaskService.createTask(task, sourceVideos);
            log.info("成功创建投稿任务，taskId: {}", taskId);
            
            return taskId;
            
        } catch (Exception e) {
            log.error("创建投稿任务失败", e);
            throw new SubmissionTaskCreationException("Failed to create submission task", e);
        }
    }
    
    /**
     * 处理分P配置数据，转换为TaskSourceVideo列表
     */
    private List<TaskSourceVideo> processVideoPartsConfiguration(SubmissionRequestDTO submissionRequest) {
        if (submissionRequest == null || submissionRequest.getVideoParts() == null) {
            log.info("没有分P配置数据，返回空列表");
            return new java.util.ArrayList<>();
        }
        
        List<VideoPartInfoDTO> videoParts = submissionRequest.getVideoParts();
        log.info("开始处理分P配置，分P数量: {}", videoParts.size());
        
        // 验证分P配置数据
        VideoPartsMapper.ValidationResult validation = VideoPartsMapper.validateVideoParts(videoParts);
        if (!validation.isValid()) {
            String errorMessage = "分P配置数据验证失败: " + validation.getErrorMessage();
            log.error(errorMessage);
            throw new TaskSourceVideoCreationException(errorMessage, validation.getErrors());
        }
        
        // 映射分P配置到TaskSourceVideo实体（taskId将在SubmissionTaskService中设置）
        List<TaskSourceVideo> sourceVideos = VideoPartsMapper.mapVideoPartsToSourceVideos(videoParts, null);
        
        log.info("分P配置映射完成，映射后的源视频数量: {}", sourceVideos.size());
        return sourceVideos;
    }
    
    /**
     * 创建任务关联
     * 修复：为所有相关的分P下载记录创建关联
     */
    private Long createTaskRelation(Long downloadTaskId, String submissionTaskId) {
        try {
            log.info("开始创建任务关联: downloadTaskId={}, submissionTaskId={}", downloadTaskId, submissionTaskId);
            
            // 验证下载任务是否存在
            VideoDownload downloadTask = videoDownloadService.getById(downloadTaskId);
            if (downloadTask == null) {
                log.error("下载任务不存在，无法创建关联: downloadTaskId={}", downloadTaskId);
                throw new TaskRelationCreationException("下载任务不存在: " + downloadTaskId, null);
            }
            log.debug("下载任务验证通过: downloadTaskId={}, title={}", downloadTaskId, downloadTask.getTitle());
            
            // 验证投稿任务是否存在
            SubmissionTask submissionTask = submissionTaskService.getTaskDetail(submissionTaskId);
            if (submissionTask == null) {
                log.error("投稿任务不存在，无法创建关联: submissionTaskId={}", submissionTaskId);
                throw new TaskRelationCreationException("投稿任务不存在: " + submissionTaskId, null);
            }
            log.debug("投稿任务验证通过: submissionTaskId={}, title={}", submissionTaskId, submissionTask.getTitle());
            
            // 查找所有相关的分P下载记录
            List<VideoDownload> relatedDownloads = findRelatedDownloads(downloadTask);
            log.info("找到相关下载记录数量: {}", relatedDownloads.size());
            
            Long firstRelationId = null;
            int createdCount = 0;
            
            // 为每个相关的下载记录创建任务关联
            for (VideoDownload relatedDownload : relatedDownloads) {
                try {
                    // 检查是否已存在关联
                    Optional<TaskRelation> existingRelation = taskRelationMapper.findByDownloadAndSubmissionTaskId(
                            relatedDownload.getId(), submissionTaskId);
                    if (existingRelation.isPresent()) {
                        log.warn("任务关联已存在: downloadTaskId={}, submissionTaskId={}, relationId={}", 
                                relatedDownload.getId(), submissionTaskId, existingRelation.get().getId());
                        if (firstRelationId == null) {
                            firstRelationId = existingRelation.get().getId();
                        }
                        continue;
                    }
                    
                    // 创建关联关系
                    TaskRelation relation = TaskRelation.createIntegratedRelation(relatedDownload.getId(), submissionTaskId);
                    
                    // 插入数据库
                    int insertResult = taskRelationMapper.insert(relation);
                    if (insertResult <= 0) {
                        log.error("插入任务关联失败: downloadTaskId={}, submissionTaskId={}", 
                                relatedDownload.getId(), submissionTaskId);
                        continue;
                    }
                    
                    log.info("成功创建任务关联: relationId={}, downloadTaskId={}, submissionTaskId={}", 
                            relation.getId(), relatedDownload.getId(), submissionTaskId);
                    
                    if (firstRelationId == null) {
                        firstRelationId = relation.getId();
                    }
                    createdCount++;
                    
                } catch (Exception e) {
                    log.error("创建单个任务关联失败: downloadTaskId={}, submissionTaskId={}", 
                            relatedDownload.getId(), submissionTaskId, e);
                    // 继续处理其他关联，不中断整个流程
                }
            }
            
            if (createdCount == 0 && firstRelationId == null) {
                throw new TaskRelationCreationException("未能创建任何任务关联", null);
            }
            
            log.info("任务关联创建完成: 总共创建了{}个关联，返回第一个关联ID: {}", createdCount, firstRelationId);
            return firstRelationId;
            
        } catch (TaskRelationCreationException e) {
            log.error("任务关联创建异常: {}", e.getMessage(), e);
            throw e; // 重新抛出自定义异常
        } catch (Exception e) {
            log.error("创建任务关联时发生未知异常: downloadTaskId={}, submissionTaskId={}", 
                    downloadTaskId, submissionTaskId, e);
            throw new TaskRelationCreationException("Failed to create task relation: " + e.getMessage(), e);
        }
    }
    
    /**
     * 查找所有相关的下载记录
     * 包括主下载记录和所有同一视频的分P记录
     */
    private List<VideoDownload> findRelatedDownloads(VideoDownload mainDownload) {
        try {
            // 查找同一视频的所有下载记录
            List<VideoDownload> relatedDownloads = videoDownloadService.findRecentDownloads(
                    mainDownload.getBvid(), mainDownload.getTitle(), 50);
            
            // 过滤出真正相关的记录（同一时间段内创建的）
            LocalDateTime cutoffTime = mainDownload.getCreateTime().minusMinutes(5); // 5分钟内创建的记录
            List<VideoDownload> filteredDownloads = relatedDownloads.stream()
                    .filter(download -> download.getCreateTime().isAfter(cutoffTime))
                    .collect(java.util.stream.Collectors.toList());
            
            log.debug("找到相关下载记录: 总数={}, 过滤后={}", relatedDownloads.size(), filteredDownloads.size());
            
            return filteredDownloads;
            
        } catch (Exception e) {
            log.error("查找相关下载记录失败", e);
            // 如果查找失败，至少返回主下载记录
            return java.util.Arrays.asList(mainDownload);
        }
    }
    
    /**
     * 转换SubmissionRequestDTO为SubmissionTask实体
     */
    private SubmissionTask convertToSubmissionTask(SubmissionRequestDTO request) {
        SubmissionTask task = new SubmissionTask();
        // 不在这里设置taskId，让SubmissionTaskService来设置
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(SubmissionTask.TaskStatus.PENDING);
        
        // 映射所有必要字段
        task.setPartitionId(request.getPartitionId());
        task.setCoverUrl(request.getCoverUrl());
        task.setTags(request.getTags());
        
        // 映射视频类型
        if (request.getVideoType() != null) {
            task.setVideoType(SubmissionTask.VideoType.valueOf(request.getVideoType().name()));
        }
        
        task.setCollectionId(request.getCollectionId());
        
        return task;
    }
    
    /**
     * 映射下载状态到投稿状态
     */
    private SubmissionTask.TaskStatus mapDownloadStatusToSubmissionStatus(Integer downloadStatus) {
        // 根据VideoDownload的状态枚举进行映射
        // 这里需要根据实际的状态定义进行调整
        switch (downloadStatus) {
            case 0: // PENDING
                return SubmissionTask.TaskStatus.PENDING;
            case 1: // DOWNLOADING
                return SubmissionTask.TaskStatus.PENDING;
            case 2: // COMPLETED
                return SubmissionTask.TaskStatus.PENDING; // 下载完成后，投稿任务变为待处理状态
            case 3: // FAILED
                return SubmissionTask.TaskStatus.FAILED;
            default:
                return null;
        }
    }
    
    /**
     * 更新关联状态
     */
    private void updateRelationStatus(TaskRelation relation, Integer downloadStatus) {
        TaskRelation.RelationStatus newStatus;
        
        switch (downloadStatus) {
            case 2: // COMPLETED
                newStatus = TaskRelation.RelationStatus.ACTIVE; // 保持活跃，等待投稿完成
                break;
            case 3: // FAILED
                newStatus = TaskRelation.RelationStatus.FAILED;
                break;
            default:
                return; // 其他状态不更新关联状态
        }
        
        taskRelationMapper.updateStatus(relation.getId(), newStatus);
    }
    
    /**
     * 构建任务关联信息
     */
    private TaskRelationInfo buildTaskRelationInfo(TaskRelation relation) {
        TaskRelationInfo info = TaskRelationInfo.fromEntity(relation);
        
        // 获取下载任务信息
        VideoDownload downloadTask = videoDownloadService.getById(relation.getDownloadTaskId());
        if (downloadTask != null) {
            TaskRelationInfo.DownloadTaskInfo downloadInfo = TaskRelationInfo.DownloadTaskInfo.builder()
                    .taskId(downloadTask.getId())
                    .bvid(downloadTask.getBvid())
                    .title(downloadTask.getTitle())
                    .partTitle(downloadTask.getPartTitle())
                    .status(downloadTask.getStatus())
                    .statusText(getDownloadStatusText(downloadTask.getStatus()))
                    .progress(downloadTask.getProgress())
                    .localPath(downloadTask.getLocalPath())
                    .createTime(downloadTask.getCreateTime())
                    .build();
            info.setDownloadTaskInfo(downloadInfo);
        }
        
        // 获取投稿任务信息
        SubmissionTask submissionTask = submissionTaskService.getTaskDetail(relation.getSubmissionTaskId());
        if (submissionTask != null) {
            TaskRelationInfo.SubmissionTaskInfo submissionInfo = TaskRelationInfo.SubmissionTaskInfo.builder()
                    .taskId(submissionTask.getTaskId())
                    .title(submissionTask.getTitle())
                    .description(submissionTask.getDescription())
                    .status(submissionTask.getStatus().name())
                    .statusText(getSubmissionStatusText(submissionTask.getStatus()))
                    .bvid(submissionTask.getBvid())
                    .createdAt(submissionTask.getCreatedAt() != null ? 
                            submissionTask.getCreatedAt().toInstant()
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime() : null)
                    .updatedAt(submissionTask.getUpdatedAt() != null ? 
                            submissionTask.getUpdatedAt().toInstant()
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime() : null)
                    .build();
            info.setSubmissionTaskInfo(submissionInfo);
        }
        
        return info;
    }
    
    /**
     * 获取下载状态文本
     */
    private String getDownloadStatusText(Integer status) {
        switch (status) {
            case 0: return "待下载";
            case 1: return "下载中";
            case 2: return "已完成";
            case 3: return "失败";
            default: return "未知";
        }
    }
    
    /**
     * 获取投稿状态文本
     */
    private String getSubmissionStatusText(SubmissionTask.TaskStatus status) {
        switch (status) {
            case PENDING: return "待处理";
            case CLIPPING: return "剪辑中";
            case MERGING: return "合并中";
            case SEGMENTING: return "分段中";
            case UPLOADING: return "上传中";
            case COMPLETED: return "已完成";
            case FAILED: return "失败";
            case WAITING_DOWNLOAD: return "等待下载完成"; // 保留这个case以防万一
            default: return "未知";
        }
    }
    
    /**
     * 自定义异常类
     */
    public static class IntegrationException extends RuntimeException {
        private final String errorCode;
        
        public IntegrationException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
        
        public IntegrationException(String message, String errorCode, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
    
    public static class DownloadTaskCreationException extends RuntimeException {
        public DownloadTaskCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class SubmissionTaskCreationException extends RuntimeException {
        public SubmissionTaskCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class TaskRelationCreationException extends RuntimeException {
        public TaskRelationCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class TaskSourceVideoCreationException extends RuntimeException {
        private final List<String> validationErrors;
        
        public TaskSourceVideoCreationException(String message, List<String> validationErrors) {
            super(message);
            this.validationErrors = validationErrors != null ? validationErrors : new java.util.ArrayList<>();
        }
        
        public TaskSourceVideoCreationException(String message, Throwable cause) {
            super(message, cause);
            this.validationErrors = new java.util.ArrayList<>();
        }
        
        public List<String> getValidationErrors() {
            return new java.util.ArrayList<>(validationErrors);
        }
        
        public String getDetailedMessage() {
            if (validationErrors.isEmpty()) {
                return getMessage();
            }
            return getMessage() + " Details: " + String.join("; ", validationErrors);
        }
    }
    
    /**
     * 更新工作流状态
     * 
     * @param downloadTaskId 下载任务ID
     * @param submissionTaskId 投稿任务ID
     * @param workflowInstanceId 工作流实例ID
     * @param status 状态
     */
    public void updateWorkflowStatus(Long downloadTaskId, String submissionTaskId, 
                                    String workflowInstanceId, String status) {
        try {
            // 更新任务关联表中的工作流信息
            boolean updated = taskRelationService.updateWorkflowInfo(downloadTaskId, submissionTaskId, workflowInstanceId, status);
            
            if (updated) {
                log.info("工作流状态已更新: downloadTaskId={}, submissionTaskId={}, instanceId={}, status={}", 
                        downloadTaskId, submissionTaskId, workflowInstanceId, status);
            } else {
                log.warn("工作流状态更新失败，可能任务关联不存在: downloadTaskId={}, submissionTaskId={}", 
                        downloadTaskId, submissionTaskId);
            }
        } catch (Exception e) {
            log.error("更新工作流状态失败: downloadTaskId={}, submissionTaskId={}, instanceId={}, status={}", 
                    downloadTaskId, submissionTaskId, workflowInstanceId, status, e);
        }
    }
    
    /**
     * 标记工作流启动失败
     * 
     * @param downloadTaskId 下载任务ID
     * @param submissionTaskId 投稿任务ID
     * @param errorMessage 错误信息
     */
    public void markWorkflowStartupFailed(Long downloadTaskId, String submissionTaskId, String errorMessage) {
        try {
            // 更新任务关联表中的工作流信息
            boolean updated = taskRelationService.updateWorkflowInfo(downloadTaskId, submissionTaskId, null, "WORKFLOW_STARTUP_FAILED");
            
            if (updated) {
                log.error("工作流启动失败已标记: downloadTaskId={}, submissionTaskId={}, error={}", 
                        downloadTaskId, submissionTaskId, errorMessage);
            } else {
                log.warn("标记工作流启动失败时更新失败，可能任务关联不存在: downloadTaskId={}, submissionTaskId={}", 
                        downloadTaskId, submissionTaskId);
            }
        } catch (Exception e) {
            log.error("标记工作流启动失败时出错: downloadTaskId={}, submissionTaskId={}, originalError={}", 
                    downloadTaskId, submissionTaskId, errorMessage, e);
        }
    }
}