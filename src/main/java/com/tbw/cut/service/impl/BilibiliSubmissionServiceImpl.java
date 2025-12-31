package com.tbw.cut.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.service.BilibiliVideoUploadService;
import com.tbw.cut.bilibili.service.RateLimitHandler;
import com.tbw.cut.bilibili.service.UploadProgressManager;
import com.tbw.cut.config.BilibiliSubmissionConfig;
import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskOutputSegment;
import com.tbw.cut.service.BilibiliSubmissionService;
import com.tbw.cut.service.SubmissionTaskService;
import com.tbw.cut.service.VideoSubmissionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class BilibiliSubmissionServiceImpl implements BilibiliSubmissionService {
    
    @Autowired
    private BilibiliVideoUploadService videoUploadService;
    
    @Autowired
    private SubmissionTaskService submissionTaskService;
    
    @Autowired
    private UploadProgressManager uploadProgressManager;
    
    @Autowired
    private RateLimitHandler rateLimitHandler;
    
    @Autowired
    private BilibiliSubmissionConfig submissionConfig;
    
    @Override
    public boolean uploadSegments(String taskId, List<TaskOutputSegment> segments) {
        try {
            log.info("开始上传分段文件到B站，任务ID: {}, 分段数量: {}", taskId, segments.size());
            
            // 记录上传失败的分段，用于重试
            List<TaskOutputSegment> failedSegments = new ArrayList<>(segments);
            int maxRetries = 3; // 最大重试次数
            int retryCount = 0;
            
            // 循环重试直到所有分段上传成功或达到最大重试次数
            while (!failedSegments.isEmpty() && retryCount < maxRetries) {
                if (retryCount > 0) {
                    log.info("第{}次重试上传分段文件，剩余分段数量: {}", retryCount, failedSegments.size());
                }
                
                // 临时存储本次尝试中失败的分段
                List<TaskOutputSegment> currentFailedSegments = new ArrayList<>();
                
                // 遍历需要上传的分段
                for (TaskOutputSegment segment : failedSegments) {
                    try {
                        // 获取视频文件
                        File videoFile = new File(segment.getSegmentFilePath());
                        if (!videoFile.exists()) {
                            log.error("视频文件不存在: {}", segment.getSegmentFilePath());
                            currentFailedSegments.add(segment);
                            continue;
                        }
                        
                        // 1. 预上传
                        JSONObject preUploadData = videoUploadService.preUploadVideo(
                            videoFile.getName(), videoFile.length());
                        
                        if (preUploadData.getIntValue("OK") != 1) {
                            log.error("预上传失败: {}", preUploadData.toJSONString());
                            currentFailedSegments.add(segment);
                            continue;
                        }
                        
                        // 2. 上传元数据
                        JSONObject postVideoMeta = videoUploadService.postVideoMeta(
                            preUploadData, videoFile.length());
                        
                        if (postVideoMeta.getIntValue("OK") != 1) {
                            log.error("上传元数据失败: {}", postVideoMeta.toJSONString());
                            currentFailedSegments.add(segment);
                            continue;
                        }
                        
                        // 3. 分片上传文件
                        int chunks = videoUploadService.uploadVideo(preUploadData, postVideoMeta, videoFile);
                        
                        // 4. 结束上传
                        JSONObject endUploadResult = videoUploadService.endUpload(preUploadData, postVideoMeta, chunks);
                        
                        if (endUploadResult.getIntValue("OK") != 1) {
                            log.error("结束上传失败: {}", endUploadResult.toJSONString());
                            currentFailedSegments.add(segment);
                            continue;
                        }
                        
                        // 获取上传后的CID
                        // 根据biliup-rs项目的实现，CID应该从endUploadResult中获取，而不是preUploadData
                        Long cid = null;
                        if (endUploadResult.containsKey("data") && endUploadResult.getJSONObject("data").containsKey("cid")) {
                            cid = endUploadResult.getJSONObject("data").getLong("cid");
                        }
                        if (cid == null) {
                            // 如果endUploadResult中没有cid，则从preUploadData中获取biz_id作为备用方案
                            cid = preUploadData.getLong("biz_id");
                        }
                        
                        // 获取上传后的filename（key字段）
                        String filename = null;
                        if (endUploadResult.containsKey("key")) {
                            filename = endUploadResult.getString("key");
                            // 移除开头的斜杠
                            if (filename.startsWith("/")) {
                                filename = filename.substring(1);
                            }
                            // 移除文件扩展名
                            int dotIndex = filename.lastIndexOf('.');
                            if (dotIndex > 0) {
                                filename = filename.substring(0, dotIndex);
                            }
                        }
                        
                        // 更新分段状态和CID
                        submissionTaskService.updateSegmentUploadStatusAndCid(
                            segment.getSegmentId(), 
                            TaskOutputSegment.UploadStatus.SUCCESS, 
                            cid);
                        
                        // 如果获取到了filename，则更新到数据库
                        if (filename != null && !filename.isEmpty()) {
                            submissionTaskService.updateSegmentFilename(segment.getSegmentId(), filename);
                        }
                        
                        log.info("分段文件上传成功: {}, CID: {}, Filename: {}", segment.getSegmentFilePath(), cid, filename);
                        
                        // 清理上传进度（上传成功后）
                        // 注意：这里我们无法直接获取UploadProgress对象，但UploadProgressManager会在内部管理
                        // 实际的清理会在uploadVideo方法完成时自动进行
                    } catch (Exception e) {
                        log.error("上传分段文件时发生异常: {}", segment.getSegmentFilePath(), e);
                        
                        // 使用统一的406错误处理
                        long waitTime = rateLimitHandler.handle406Error(e.getMessage());
                        if (waitTime > 0) {
                            log.warn("遇到406错误，需要等待{}，当前连续406错误次数: {}", 
                                rateLimitHandler.getWaitTimeDescription(waitTime), 
                                rateLimitHandler.getCurrent406Count());
                            try {
                                rateLimitHandler.smartWait(waitTime);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                log.error("线程中断", ie);
                            }
                        }
                        
                        // 无论是否406错误，都将该分段重新加入失败列表以便重试
                        currentFailedSegments.add(segment);
                    }
                }
                
                // 更新失败分段列表
                failedSegments = currentFailedSegments;
                retryCount++;
                
                // 如果还有失败的分段并且不是最后一次重试，等待一段时间再重试
                if (!failedSegments.isEmpty() && retryCount < maxRetries) {
                    try {
                        // 等待5秒再重试
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("线程中断", ie);
                    }
                }
            }
            
            if (failedSegments.isEmpty()) {
                log.info("所有分段文件上传完成，任务ID: {}", taskId);
                return true;
            } else {
                log.error("仍有{}个分段上传失败，任务ID: {}", failedSegments.size(), taskId);
                return false;
            }
        } catch (Exception e) {
            log.error("上传分段文件到B站时发生异常，任务ID: {}", taskId, e);
            return false;
        }
    }
    
    @Override
    public String submitVideo(SubmissionTask task, List<TaskOutputSegment> segments) {
        try {
            log.info("开始提交视频到B站，任务ID: {}, 总分段数: {}", task.getTaskId(), segments.size());
            
            // 检查分P数量上限
            int maxTotalParts = submissionConfig.getMaxTotalParts();
            if (segments.size() > maxTotalParts) {
                String errorMsg = String.format("分P数大于%d，无法进行投稿。当前分P数: %d", maxTotalParts, segments.size());
                log.error("投稿失败，任务ID: {}, {}", task.getTaskId(), errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            // 从配置中获取单次投稿分P数量限制
            int maxPartsPerSubmission = submissionConfig.getMaxPartsPerSubmission();
            
            if (!submissionConfig.isEnableBatchSubmission() || segments.size() <= maxPartsPerSubmission) {
                // 分段数量在限制内或未启用分批投稿，直接投稿
                return submitSingleVideo(task, segments);
            } else {
                // 分段数量超出限制，需要分批投稿
                return submitVideosInBatches(task, segments, maxPartsPerSubmission);
            }
        } catch (Exception e) {
            log.error("提交视频到B站时发生异常，任务ID: {}", task.getTaskId(), e);
            return null;
        }
    }
    
    /**
     * 单次投稿（分段数量在限制内）
     */
    private VideoSubmissionResult submitSingleVideoWithResult(SubmissionTask task, List<TaskOutputSegment> segments) {
        try {
            log.info("单次投稿，任务ID: {}, 分段数: {}", task.getTaskId(), segments.size());
            
            // 构建投稿数据
            JSONObject submitData = buildSubmitData(task, segments);
            
            // 打印提交数据用于调试
            log.info("提交数据: {}", submitData.toJSONString());
            
            // 提交视频
            JSONObject result = videoUploadService.submitVideo(submitData);
            
            if (result.getIntValue("code") == 0) {
                String bvid = result.getJSONObject("data").getString("bvid");
                Long aid = result.getJSONObject("data").getLong("aid");
                log.info("视频投稿成功，任务ID: {}, BVID: {}, AID: {}", task.getTaskId(), bvid, aid);
                
                // 如果投稿时已经指定了合集ID，通常不需要额外的操作
                if (task.getCollectionId() != null && task.getCollectionId() > 0) {
                    log.info("视频已通过投稿时的season_id自动添加到合集，任务ID: {}, 合集ID: {}", 
                        task.getTaskId(), task.getCollectionId());
                }
                
                return VideoSubmissionResult.success(bvid, aid);
            } else {
                String errorMsg = "视频投稿失败: " + result.toJSONString();
                log.error(errorMsg);
                return VideoSubmissionResult.failure(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "单次投稿失败，任务ID: " + task.getTaskId() + ", 错误: " + e.getMessage();
            log.error(errorMsg, e);
            return VideoSubmissionResult.failure(errorMsg);
        }
    }

    /**
     * 单次投稿（分段数量在限制内）- 保持向后兼容
     */
    private String submitSingleVideo(SubmissionTask task, List<TaskOutputSegment> segments) {
        VideoSubmissionResult result = submitSingleVideoWithResult(task, segments);
        return result.isSuccess() ? result.getBvid() : null;
    }
    
    /**
     * 分批投稿（分段数量超出限制）
     * 策略：先投稿第一批创建视频，然后使用编辑API将后续批次添加到同一个视频中
     */
    private String submitVideosInBatches(SubmissionTask task, List<TaskOutputSegment> segments, int maxPartsPerSubmission) {
        try {
            log.info("开始分批投稿到同一视频（使用编辑API），任务ID: {}, 总分段数: {}, 每批最大分段数: {}", 
                task.getTaskId(), segments.size(), maxPartsPerSubmission);
            
            int totalBatches = (int) Math.ceil((double) segments.size() / maxPartsPerSubmission);
            String mainBvid = null;
            Long mainAid = null;
            
            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                int startIndex = batchIndex * maxPartsPerSubmission;
                int endIndex = Math.min(startIndex + maxPartsPerSubmission, segments.size());
                
                List<TaskOutputSegment> batchSegments = segments.subList(startIndex, endIndex);
                
                log.info("处理第 {}/{} 批，分段范围: P{}-P{}, 分段数: {}", 
                    batchIndex + 1, totalBatches, startIndex + 1, endIndex, batchSegments.size());
                
                if (batchIndex == 0) {
                    // 第一批：创建新视频，直接使用投稿API返回的AID
                    VideoSubmissionResult submissionResult = submitSingleVideoWithResult(task, batchSegments);
                    if (!submissionResult.isSuccess()) {
                        log.error("第一批投稿失败，无法创建视频，任务ID: {}, 错误: {}", 
                            task.getTaskId(), submissionResult.getErrorMessage());
                        return null;
                    }
                    
                    mainBvid = submissionResult.getBvid();
                    mainAid = submissionResult.getAid();  // 直接使用投稿API返回的AID，无需查询！
                    
                    log.info("第一批投稿成功，创建视频 BVID: {}, AID: {} (直接从投稿响应获取)", mainBvid, mainAid);
                } else {
                    // 后续批次：使用编辑API添加分P到现有视频
                    boolean editSuccess = editVideoToAddParts(task, mainAid, segments, startIndex, endIndex);
                    if (!editSuccess) {
                        log.error("第 {}/{} 批编辑视频失败，任务ID: {}", batchIndex + 1, totalBatches, task.getTaskId());
                        return null;
                    }
                    
                    log.info("第 {}/{} 批编辑视频成功", batchIndex + 1, totalBatches);
                }
                
                // 批次间等待，避免请求过快
                if (batchIndex < totalBatches - 1) {
                    try {
                        long intervalMs = submissionConfig.getBatchIntervalMs();
                        log.info("批次间等待 {} 毫秒", intervalMs);
                        Thread.sleep(intervalMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("批次间等待被中断");
                    }
                }
            }
            
            log.info("所有批次处理完成，任务ID: {}, 总批次数: {}, 最终视频BVID: {}", 
                task.getTaskId(), totalBatches, mainBvid);
            
            return mainBvid;
        } catch (Exception e) {
            log.error("分批投稿失败，任务ID: {}", task.getTaskId(), e);
            return null;
        }
    }
    

    
    /**
     * 使用编辑API添加分P到现有视频
     */
    private boolean editVideoToAddParts(SubmissionTask task, Long aid, List<TaskOutputSegment> allSegments, 
                                       int newPartsStartIndex, int newPartsEndIndex) {
        try {
            log.info("使用编辑API添加分P，AID: {}, 新增分段范围: P{}-P{}", 
                aid, newPartsStartIndex + 1, newPartsEndIndex);
            
            // 构建完整的视频数据（包含现有分P + 新增分P）
            JSONObject editData = buildEditData(task, allSegments, newPartsEndIndex);
            
            // 调用编辑API
            JSONObject result = videoUploadService.editVideo(aid, editData);
            
            if (result != null && result.getIntValue("code") == 0) {
                log.info("成功编辑视频添加分P，AID: {}, 总分P数: {}", aid, newPartsEndIndex);
                return true;
            } else {
                log.error("编辑视频失败，AID: {}, 错误信息: {}", aid, result != null ? result.toJSONString() : "null");
                return false;
            }
        } catch (Exception e) {
            log.error("编辑视频添加分P失败，AID: {}", aid, e);
            return false;
        }
    }
    
    /**
     * 构建编辑视频的数据（包含所有分P）
     */
    private JSONObject buildEditData(SubmissionTask task, List<TaskOutputSegment> segments, int totalParts) {
        JSONObject editData = new JSONObject();
        
        // 添加所有视频分P信息（从P1到P{totalParts}）
        com.alibaba.fastjson.JSONArray videos = new com.alibaba.fastjson.JSONArray();
        for (int i = 0; i < totalParts; i++) {
            TaskOutputSegment segment = segments.get(i);
            JSONObject video = new JSONObject();
            
            // 验证CID是否存在
            if (segment.getCid() == null) {
                throw new RuntimeException("分段缺少CID，无法编辑视频，分段ID: " + segment.getSegmentId());
            }
            
            // 使用正确的filename
            String fileName = segment.getFilename();
            if (fileName == null || fileName.isEmpty()) {
                fileName = new File(segment.getSegmentFilePath()).getName();
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    fileName = fileName.substring(0, dotIndex);
                }
            }
            
            video.put("filename", fileName);
            video.put("title", "P" + (i + 1));
            video.put("desc", "");
            video.put("cid", segment.getCid());
            
            videos.add(video);
        }
        editData.put("videos", videos);
        
        // 保留原始视频元数据
        editData.put("title", task.getTitle());
        editData.put("copyright", task.getVideoType() == com.tbw.cut.entity.SubmissionTask.VideoType.ORIGINAL ? 1 : 2);
        editData.put("tid", task.getPartitionId());
        editData.put("tag", task.getTags());
        editData.put("desc_format_id", 9999);
        editData.put("desc", task.getDescription());
        editData.put("cover", task.getCoverUrl());
        editData.put("recreate", -1);
        editData.put("dynamic", "");
        editData.put("interactive", 0);
        editData.put("act_reserve_create", 0);
        editData.put("no_disturbance", 0);
        editData.put("no_reprint", 1);
        editData.put("web_os", 1); // 编辑API使用web_os=1
        
        // 添加合集信息（如果存在）
        if (task.getCollectionId() != null && task.getCollectionId() > 0) {
            editData.put("season_id", task.getCollectionId());
        }
        
        JSONObject subtitle = new JSONObject();
        subtitle.put("open", 0);
        subtitle.put("lan", "");
        editData.put("subtitle", subtitle);
        
        editData.put("dolby", 0);
        editData.put("lossless_music", 0);
        editData.put("up_selection_reply", false);
        editData.put("up_close_reply", false);
        editData.put("up_close_danmu", false);
        
        return editData;
    }
    
    /**
     * 创建批次任务
     */
    private SubmissionTask createBatchTask(SubmissionTask originalTask, int batchNumber, int totalBatches) {
        SubmissionTask batchTask = new SubmissionTask();
        
        // 复制原任务的基本信息
        batchTask.setTaskId(originalTask.getTaskId());
        batchTask.setVideoType(originalTask.getVideoType());
        batchTask.setPartitionId(originalTask.getPartitionId());
        batchTask.setTags(originalTask.getTags());
        batchTask.setDescription(originalTask.getDescription());
        batchTask.setCoverUrl(originalTask.getCoverUrl());
        batchTask.setCollectionId(originalTask.getCollectionId());
        
        // 修改标题以区分批次
        String originalTitle = originalTask.getTitle();
        String batchTitle;
        
        if (totalBatches > 1) {
            batchTitle = String.format("%s（第%d部分，共%d部分）", originalTitle, batchNumber, totalBatches);
        } else {
            batchTitle = originalTitle;
        }
        
        batchTask.setTitle(batchTitle);
        
        return batchTask;
    }
    
    /**
     * 构建投稿数据
     */
    private JSONObject buildSubmitData(SubmissionTask task, List<TaskOutputSegment> segments) {
        JSONObject submitData = new JSONObject();
        
        // 添加视频信息
        com.alibaba.fastjson.JSONArray videos = new com.alibaba.fastjson.JSONArray();
        for (int i = 0; i < segments.size(); i++) {
            TaskOutputSegment segment = segments.get(i);
            JSONObject video = new JSONObject();
            
            // 验证CID是否存在
            if (segment.getCid() == null) {
                throw new RuntimeException("分段缺少CID，无法提交视频，分段ID: " + segment.getSegmentId());
            }
            
            // 使用从endUploadResult中获取的正确filename
            String fileName = segment.getFilename();
            if (fileName == null || fileName.isEmpty()) {
                // 如果没有从endUploadResult获取到filename，则使用原来的处理方式
                fileName = new File(segment.getSegmentFilePath()).getName();
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    fileName = fileName.substring(0, dotIndex);
                }
            }
            
            video.put("filename", fileName);
            video.put("title", "P" + (i + 1));
            video.put("desc", "");
            video.put("cid", segment.getCid());
            
            log.info("添加视频分段到提交数据，索引: {}, filename: {}, cid: {}", i, fileName, segment.getCid());
            videos.add(video);
        }
        submitData.put("videos", videos);
        
        // 添加其他必要信息
        submitData.put("cover", task.getCoverUrl());
        submitData.put("cover43", "");
        submitData.put("title", task.getTitle());
        submitData.put("copyright", task.getVideoType() == com.tbw.cut.entity.SubmissionTask.VideoType.ORIGINAL ? 1 : 2);
        submitData.put("human_type2", task.getPartitionId());
        submitData.put("tag", task.getTags());
        submitData.put("desc_format_id", 9999);
        submitData.put("desc", task.getDescription());
        submitData.put("recreate", -1);
        submitData.put("dynamic", "");
        submitData.put("interactive", 0);
        submitData.put("act_reserve_create", 0);
        submitData.put("no_disturbance", 0);
        submitData.put("no_reprint", 1);
        
        // 添加合集信息（如果存在）
        if (task.getCollectionId() != null && task.getCollectionId() > 0) {
            submitData.put("season_id", task.getCollectionId());
        }
        
        JSONObject subtitle = new JSONObject();
        subtitle.put("open", 0);
        subtitle.put("lan", "");
        submitData.put("subtitle", subtitle);
        
        submitData.put("dolby", 0);
        submitData.put("lossless_music", 0);
        submitData.put("up_selection_reply", false);
        submitData.put("up_close_reply", false);
        submitData.put("up_close_danmu", false);
        submitData.put("web_os", 3);
        
        return submitData;
    }
    
    @Override
    public boolean associateWithSeason(SubmissionTask task, Long aid) {
        // 检查任务是否配置了合集ID
        if (task.getCollectionId() == null || task.getCollectionId() <= 0) {
            log.info("任务未配置合集ID，跳过关联合集操作，任务ID: {}", task.getTaskId());
            return true;
        }
        
        try {
            log.info("开始关联合集，任务ID: {}, 合集ID: {}, 视频AID: {}", 
                task.getTaskId(), task.getCollectionId(), aid);
            
            // 获取合集章节信息，使用默认章节ID 0
            Long sectionId = 0L;
            
            // 检查合集ID是否有效
            if (task.getCollectionId() == null || task.getCollectionId() <= 0) {
                log.error("关联合集失败，合集ID无效，任务ID: {}, 合集ID: {}", task.getTaskId(), task.getCollectionId());
                return false;
            }
            
            // 调用B站API关联合集
            JSONObject result = videoUploadService.associateWithSeason(
                task.getCollectionId(), sectionId, task.getTitle(), aid);
            
            if (result.getIntValue("code") == 0) {
                log.info("视频关联合集成功，任务ID: {}, 合集ID: {}, 视频AID: {}", 
                    task.getTaskId(), task.getCollectionId(), aid);
                return true;
            } else {
                // 检查是否是season_id不存在的错误
                if (result.getIntValue("code") == -404) {
                    log.error("视频关联合集失败，合集ID不存在或已被删除，任务ID: {}, 合集ID: {}, 视频AID: {}, 错误信息: {}", 
                        task.getTaskId(), task.getCollectionId(), aid, result.toJSONString());
                } else {
                    log.error("视频关联合集失败，任务ID: {}, 合集ID: {}, 视频AID: {}, 错误信息: {}", 
                        task.getTaskId(), task.getCollectionId(), aid, result.toJSONString());
                }
                return false;
            }
        } catch (RuntimeException re) {
            // 捕获RuntimeException并记录详细错误信息
            log.error("关联合集时发生运行时异常，任务ID: {}, 合集ID: {}", task.getTaskId(), task.getCollectionId(), re);
            return false;
        } catch (Exception e) {
            log.error("关联合集时发生异常，任务ID: {}, 合集ID: {}", task.getTaskId(), task.getCollectionId(), e);
            return false;
        }
    }
    
    @Override
    public boolean addEpisodesToSection(SubmissionTask task, Long aid, List<TaskOutputSegment> segments) {
        // 检查任务是否配置了合集ID
        if (task.getCollectionId() == null || task.getCollectionId() <= 0) {
            log.info("任务未配置合集ID，跳过添加到章节操作，任务ID: {}", task.getTaskId());
            return true;
        }
        
        try {
            log.info("开始将视频添加到合集章节，任务ID: {}, 合集ID: {}, 视频AID: {}", 
                task.getTaskId(), task.getCollectionId(), aid);
            
            // 首先获取合集的章节信息
            JSONObject seasonInfo = videoUploadService.getSeasonInfo(task.getCollectionId());
            if (seasonInfo.getIntValue("code") != 0) {
                log.error("获取合集信息失败，任务ID: {}, 合集ID: {}, 错误信息: {}", 
                    task.getTaskId(), task.getCollectionId(), seasonInfo.toJSONString());
                return false;
            }
            
            // 获取默认章节ID（通常是第一个章节）
            Long sectionId = null;
            JSONObject data = seasonInfo.getJSONObject("data");
            if (data != null && data.containsKey("sections")) {
                com.alibaba.fastjson.JSONArray sections = data.getJSONArray("sections");
                if (sections != null && !sections.isEmpty()) {
                    JSONObject firstSection = sections.getJSONObject(0);
                    sectionId = firstSection.getLong("id");
                }
            }
            
            // 如果没有找到章节，使用默认章节ID 0
            if (sectionId == null) {
                sectionId = 0L;
                log.warn("未找到合集章节，使用默认章节ID 0，任务ID: {}, 合集ID: {}", 
                    task.getTaskId(), task.getCollectionId());
            }
            
            log.info("使用章节ID: {}, 任务ID: {}, 合集ID: {}", sectionId, task.getTaskId(), task.getCollectionId());
            
            // 构建视频列表
            List<JSONObject> episodes = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                TaskOutputSegment segment = segments.get(i);
                JSONObject episode = new JSONObject();
                episode.put("title", segment.getPartName());
                episode.put("cid", segment.getCid());
                episode.put("aid", aid);
                episodes.add(episode);
            }
            
            // 调用B站API将视频添加到章节（使用正确的sectionId）
            JSONObject result = videoUploadService.addEpisodesToSection(sectionId, episodes);
            if (result.getIntValue("code") == 0) {
                log.info("视频添加到合集章节成功，任务ID: {}, 合集ID: {}, 章节ID: {}, 视频AID: {}",
                        task.getTaskId(), task.getCollectionId(), sectionId, aid);
                return true;
            } else {
                log.error("视频添加到合集章节失败，任务ID: {}, 合集ID: {}, 章节ID: {}, 视频AID: {}, 错误信息: {}",
                        task.getTaskId(), task.getCollectionId(), sectionId, aid, result.toJSONString());
                return false;
            }
        } catch (Exception e) {
            log.error("添加到章节时发生异常，任务ID: {}, 合集ID: {}", task.getTaskId(), task.getCollectionId(), e);
            return false;
        }
    }
}