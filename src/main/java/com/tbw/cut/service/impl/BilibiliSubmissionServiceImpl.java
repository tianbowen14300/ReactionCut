package com.tbw.cut.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.service.BilibiliVideoUploadService;
import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskOutputSegment;
import com.tbw.cut.service.BilibiliSubmissionService;
import com.tbw.cut.service.SubmissionTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
                    } catch (Exception e) {
                        log.error("上传分段文件时发生异常: {}", segment.getSegmentFilePath(), e);
                        // 检查是否是406错误（上传过快）
                        if (e.getMessage().contains("406")) {
                            log.warn("遇到406错误（上传过快），线程将暂停30分钟后再继续");
                            try {
                                // 暂停30分钟（1800000毫秒）
                                Thread.sleep(1800000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                log.error("线程中断", ie);
                            }
                            // 对于406错误，我们将该分段重新加入失败列表以便重试
                            currentFailedSegments.add(segment);
                        } else {
                            // 对于其他错误，也将该分段重新加入失败列表以便重试
                            currentFailedSegments.add(segment);
                        }
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
            log.info("开始提交视频到B站，任务ID: {}", task.getTaskId());
            
            // 构建投稿数据
            JSONObject submitData = new JSONObject();
            
            // 添加视频信息
            com.alibaba.fastjson.JSONArray videos = new com.alibaba.fastjson.JSONArray();
            for (int i = 0; i < segments.size(); i++) {
                TaskOutputSegment segment = segments.get(i);
                JSONObject video = new JSONObject();
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
            
            // 打印提交数据用于调试
            log.info("提交数据: {}", submitData.toJSONString());
            
            // 提交视频
            JSONObject result = videoUploadService.submitVideo(submitData);
            
            if (result.getIntValue("code") == 0) {
                String bvid = result.getJSONObject("data").getString("bvid");
                Long aid = result.getJSONObject("data").getLong("aid");
                log.info("视频投稿成功，任务ID: {}, BVID: {}, AID: {}", task.getTaskId(), bvid, aid);
                
                // 如果任务配置了合集ID，则关联合集
                if (task.getCollectionId() != null && task.getCollectionId() > 0) {
                    log.info("任务配置了合集ID，开始关联合集，任务ID: {}", task.getTaskId());
                    boolean seasonAssociated = associateWithSeason(task, aid);
                    if (seasonAssociated) {
                        log.info("视频关联合集成功，任务ID: {}", task.getTaskId());
                        
                        // 将视频添加到合集章节
                        boolean episodesAdded = addEpisodesToSection(task, aid, segments);
                        if (episodesAdded) {
                            log.info("视频添加到合集章节成功，任务ID: {}", task.getTaskId());
                        } else {
                            log.error("视频添加到合集章节失败，任务ID: {}", task.getTaskId());
                        }
                    } else {
                        log.error("视频关联合集失败，任务ID: {}，将继续处理其他任务", task.getTaskId());
                        // 即使关联合集失败，也继续完成任务，不中断整个投稿流程
                        // 这样可以确保视频能够成功投稿，只是没有关联合集
                    }
                }
                
                return bvid;
            } else {
                log.error("视频投稿失败: {}", result.toJSONString());
                return null;
            }
        } catch (Exception e) {
            log.error("提交视频到B站时发生异常，任务ID: {}", task.getTaskId(), e);
            return null;
        }
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
            
            // 调用B站API将视频添加到章节
            JSONObject result = videoUploadService.addEpisodesToSection(0L, episodes);
            
            if (result.getIntValue("code") == 0) {
                log.info("视频添加到合集章节成功，任务ID: {}, 合集ID: {}, 视频AID: {}", 
                    task.getTaskId(), task.getCollectionId(), aid);
                return true;
            } else {
                log.error("视频添加到合集章节失败，任务ID: {}, 合集ID: {}, 视频AID: {}, 错误信息: {}", 
                    task.getTaskId(), task.getCollectionId(), aid, result.toJSONString());
                return false;
            }
        } catch (Exception e) {
            log.error("添加到章节时发生异常，任务ID: {}, 合集ID: {}", task.getTaskId(), task.getCollectionId(), e);
            return false;
        }
    }
}