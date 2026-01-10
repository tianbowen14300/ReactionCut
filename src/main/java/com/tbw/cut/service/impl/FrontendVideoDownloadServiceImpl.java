package com.tbw.cut.service.impl;

import com.tbw.cut.dto.VideoDownloadDTO;
import com.tbw.cut.dto.FrontendPartInfo;
import com.tbw.cut.service.FrontendVideoDownloadService;
import com.tbw.cut.service.FrontendPartDownloadService;
import com.tbw.cut.service.VideoDownloadService;
import com.tbw.cut.bilibili.BilibiliService;
import com.tbw.cut.entity.DownloadTask;
import com.tbw.cut.event.DownloadEventPublisher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Slf4j
@Service
public class FrontendVideoDownloadServiceImpl implements FrontendVideoDownloadService {
    
    @Autowired
    private VideoDownloadService videoDownloadService;
    
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("enhancedFrontendPartDownloadService")
    private FrontendPartDownloadService frontendPartDownloadService;
    
    @Autowired
    private BilibiliService bilibiliService;
    
    @Autowired
    private DownloadEventPublisher downloadEventPublisher;
    
    @Override
    public Long handleFrontendDownloadRequest(Map<String, Object> requestData) {
        try {
            // 检查是否是前端新格式（包含parts字段）
            if (requestData.containsKey("parts") && requestData.get("parts") instanceof List) {
                // 使用DownloadTaskManager来管理下载任务
                return handleFrontendPartDownloadWithQueue(requestData);
            } else {
                // 使用旧格式处理
                VideoDownloadDTO downloadDTO = convertToBackendDTO(requestData);
                return videoDownloadService.downloadVideo(downloadDTO);
            }
        } catch (Exception e) {
            log.error("处理前端下载请求失败", e);
            return null;
        }
    }
    
    /**
     * 处理前端part下载请求，使用队列管理
     * 修复：使用同步方式创建分P记录，确保能正确获取下载任务ID
     */
    private Long handleFrontendPartDownloadWithQueue(Map<String, Object> requestData) {
        try {
            String videoUrl = (String) requestData.get("videoUrl");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) requestData.get("parts");
            Map<String, Object> config = (Map<String, Object>) requestData.get("config");
            
            // Parse video ID
            String bvid = null;
            String aid = null;
            if (videoUrl != null) {
                bvid = com.tbw.cut.bilibili.BilibiliUtils.extractBvidFromUrl(videoUrl);
                aid = com.tbw.cut.bilibili.BilibiliUtils.extractAidFromUrl(videoUrl);
            }
            
            // 获取视频标题
            String videoTitle = getVideoTitle(bvid, aid);
            
            // 修复：先同步创建分P下载记录，获取第一个分P的ID，然后异步执行下载
            Long firstPartId = createPartDownloadRecordsSync(videoUrl, bvid, aid, videoTitle, parts, config);
            
            if (firstPartId != null) {
                // 创建下载任务对象，使用第一个分P的ID作为主ID
                DownloadTask downloadTask = new DownloadTask(firstPartId, videoUrl, bvid, aid, parts, config);
                
                // 异步执行下载任务
                frontendPartDownloadService.addDownloadTask(downloadTask);
                
                log.info("成功创建分P下载任务，返回第一个分P的ID: {}, bvid: {}, parts: {}", 
                        firstPartId, bvid, parts.size());
                return firstPartId;
            } else {
                log.error("无法创建分P下载记录");
                return null;
            }
            
        } catch (Exception e) {
            log.error("处理前端part下载请求失败", e);
            return null;
        }
    }
    
    /**
     * 同步创建分P下载记录，返回第一个分P的ID
     */
    private Long createPartDownloadRecordsSync(String videoUrl, String bvid, String aid, String videoTitle,
                                              List<Map<String, Object>> parts, Map<String, Object> config) {
        try {
            log.info("开始同步创建分P下载记录: bvid={}, videoTitle={}, parts={}", bvid, videoTitle, parts.size());
            
            Long firstPartId = null;
            
            for (int i = 0; i < parts.size(); i++) {
                Map<String, Object> part = parts.get(i);
                Long cid = (Long) part.get("cid");
                String originalTitle = (String) part.get("title");
                
                if (cid != null) {
                    // 创建分P下载记录
                    com.tbw.cut.entity.VideoDownload partDownload = new com.tbw.cut.entity.VideoDownload();
                    partDownload.setBvid(bvid);
                    partDownload.setAid(aid);
                    
                    // 修复：title字段使用originalTitle（分P标题），而不是视频总标题
                    partDownload.setTitle(originalTitle != null ? originalTitle : ("Part " + (i + 1)));
                    partDownload.setPartTitle(originalTitle != null ? originalTitle : ("Part " + (i + 1)));
                    
                    partDownload.setPartCount(parts.size());
                    partDownload.setCurrentPart(i + 1);
                    partDownload.setDownloadUrl(videoUrl);
                    
                    // 构建文件路径：downloadPath + originalTitle
                    String downloadPath = null;
                    if (config != null && config.containsKey("downloadPath")) {
                        downloadPath = config.get("downloadPath").toString();
                    }
                    
                    if (downloadPath != null && !downloadPath.isEmpty() && originalTitle != null) {
                        // 清理文件名中的非法字符
                        String sanitizedTitle = originalTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
                        String filePath = downloadPath + "/" + sanitizedTitle + ".mp4";
                        partDownload.setLocalPath(filePath);
                        log.info("设置分P下载路径: {}", filePath);
                    }
                    
                    // 设置下载配置信息
                    if (config != null) {
                        if (config.containsKey("resolution")) {
                            partDownload.setResolution(config.get("resolution").toString());
                        }
                        if (config.containsKey("codec")) {
                            partDownload.setCodec(config.get("codec").toString());
                        }
                        if (config.containsKey("format")) {
                            partDownload.setFormat(config.get("format").toString());
                        }
                    }
                    
                    partDownload.setStatus(0); // Pending
                    partDownload.setProgress(0);
                    partDownload.setCreateTime(java.time.LocalDateTime.now());
                    partDownload.setUpdateTime(java.time.LocalDateTime.now());
                    
                    // 保存到数据库
                    boolean saved = videoDownloadService.save(partDownload);
                    if (saved && partDownload.getId() != null) {
                        log.info("成功创建分P下载记录[{}]: id={}, cid={}, title={}, localPath={}", 
                                i, partDownload.getId(), cid, partDownload.getTitle(), partDownload.getLocalPath());
                        
                        // 记录第一个分P的ID
                        if (firstPartId == null) {
                            firstPartId = partDownload.getId();
                        }
                    } else {
                        log.error("创建分P下载记录失败: cid={}, title={}", cid, originalTitle);
                    }
                } else {
                    log.warn("分P信息中缺少cid: {}", part);
                }
            }
            
            log.info("同步创建分P下载记录完成，第一个分P ID: {}", firstPartId);
            return firstPartId;
            
        } catch (Exception e) {
            log.error("同步创建分P下载记录失败", e);
            return null;
        }
    }
    
    /**
     * 查找第一个分P下载记录的ID
     * 修复：使用更准确的查询条件
     */
    private Long findFirstPartDownloadId(String bvid, String aid, String videoTitle) {
        try {
            log.info("查找分P下载记录: bvid={}, videoTitle={}", bvid, videoTitle);
            
            // 修复：使用bvid和时间范围查询，而不是依赖title匹配
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.tbw.cut.entity.VideoDownload> queryWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            
            if (bvid != null && !bvid.isEmpty()) {
                queryWrapper.eq("bvid", bvid);
            } else if (aid != null && !aid.isEmpty()) {
                queryWrapper.eq("aid", aid);
            }
            
            // 查询最近5分钟内创建的记录
            java.time.LocalDateTime cutoffTime = java.time.LocalDateTime.now().minusMinutes(5);
            queryWrapper.ge("create_time", cutoffTime);
            
            queryWrapper.orderByAsc("current_part"); // 按分P顺序排序
            queryWrapper.last("LIMIT 1"); // 只取第一个
            
            List<com.tbw.cut.entity.VideoDownload> recentDownloads = videoDownloadService.list(queryWrapper);
            if (!recentDownloads.isEmpty()) {
                Long firstPartId = recentDownloads.get(0).getId();
                log.info("找到第一个分P下载记录: id={}, title={}", firstPartId, recentDownloads.get(0).getTitle());
                return firstPartId;
            } else {
                log.warn("未找到匹配的分P下载记录");
                return null;
            }
        } catch (Exception e) {
            log.error("查找分P下载记录失败", e);
            return null;
        }
    }
    
    /**
     * 获取视频标题
     */
    private String getVideoTitle(String bvid, String aid) {
        try {
            if (bvid != null && !bvid.isEmpty()) {
                com.alibaba.fastjson.JSONObject videoInfo = bilibiliService.getVideoInfo(bvid);
                if (videoInfo != null) {
                    String title = videoInfo.getString("title");
                    if (title != null) {
                        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取视频标题失败", e);
        }
        return "未知视频";
    }
    
    @Override
    public VideoDownloadDTO convertToBackendDTO(Map<String, Object> requestData) {
        VideoDownloadDTO backendDTO = new VideoDownloadDTO();
        
        // 设置视频URL
        if (requestData.containsKey("videoUrl")) {
            backendDTO.setVideoUrl((String) requestData.get("videoUrl"));
        }
        
        // 检查是否有parts字段（前端新格式）
        if (requestData.containsKey("parts") && requestData.get("parts") instanceof List) {
            // 对于新格式，我们暂时不设置pages，而是在下载服务中直接使用CID
            log.info("检测到前端新格式，包含parts字段");
        }
        
        // 检查是否有config字段（前端新格式）
        if (requestData.containsKey("config") && requestData.get("config") instanceof Map) {
            Map<String, Object> config = (Map<String, Object>) requestData.get("config");
            
            // 将resolution映射到quality
            if (config.containsKey("resolution")) {
                backendDTO.setQuality((String) config.get("resolution"));
            }
            
            // 处理content字段
            if (config.containsKey("content")) {
                String content = (String) config.get("content");
                if ("audio_only".equals(content)) {
                    backendDTO.setAudioOnly(true);
                } else if ("video_only".equals(content)) {
                    backendDTO.setVideoOnly(true);
                }
            }
        }
        
        // 保持对旧格式的兼容性
        if (requestData.containsKey("pages") && requestData.get("pages") instanceof List) {
            backendDTO.setPages((List<Integer>) requestData.get("pages"));
        }
        
        if (requestData.containsKey("quality")) {
            backendDTO.setQuality((String) requestData.get("quality"));
        }
        
        if (requestData.containsKey("audioOnly")) {
            backendDTO.setAudioOnly((Boolean) requestData.get("audioOnly"));
        }
        
        if (requestData.containsKey("videoOnly")) {
            backendDTO.setVideoOnly((Boolean) requestData.get("videoOnly"));
        }
        
        log.info("转换后的DTO: videoUrl={}, quality={}, audioOnly={}, videoOnly={}", 
                backendDTO.getVideoUrl(), backendDTO.getQuality(), 
                backendDTO.getAudioOnly(), backendDTO.getVideoOnly());
        
        return backendDTO;
    }
    
    /**
     * 处理单文件下载完成事件发布
     * 
     * @param downloadTaskId 下载任务ID
     * @param filePath 下载完成的文件路径
     */
    public void handleSingleFileDownloadCompletion(Long downloadTaskId, String filePath) {
        if (downloadTaskId == null) {
            log.error("无法处理单文件下载完成: downloadTaskId为空");
            return;
        }
        
        if (filePath == null || filePath.trim().isEmpty()) {
            log.error("无法处理单文件下载完成: filePath为空, downloadTaskId={}", downloadTaskId);
            return;
        }
        
        try {
            log.info("处理单文件下载完成: downloadTaskId={}, filePath={}", downloadTaskId, filePath);
            
            // 验证文件存在
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                log.warn("单文件下载完成但文件不存在: downloadTaskId={}, filePath={}", downloadTaskId, filePath);
                return;
            }
            
            if (file.length() == 0) {
                log.warn("单文件下载完成但文件大小为0: downloadTaskId={}, filePath={}", downloadTaskId, filePath);
                return;
            }
            
            // 发布下载完成事件
            List<String> filePaths = new ArrayList<>();
            filePaths.add(filePath);
            
            downloadEventPublisher.publishDownloadCompletionEvent(downloadTaskId, filePaths);
            log.info("✅ 单文件下载完成，已发布事件: downloadTaskId={}, filePath={}, fileSize={} bytes", 
                    downloadTaskId, filePath, file.length());
            
        } catch (Exception e) {
            log.error("❌ 处理单文件下载完成事件失败: downloadTaskId={}, filePath={}, error={}", 
                    downloadTaskId, filePath, e.getMessage(), e);
        }
    }
    
    /**
     * 检查并发布单文件下载完成事件（用于现有下载流程的集成）
     * 
     * @param downloadTaskId 下载任务ID
     */
    public void checkAndPublishSingleFileCompletion(Long downloadTaskId) {
        if (downloadTaskId == null) {
            log.error("无法检查单文件下载完成: downloadTaskId为空");
            return;
        }
        
        try {
            // 查询下载任务信息
            com.tbw.cut.entity.VideoDownload downloadTask = videoDownloadService.getById(downloadTaskId);
            if (downloadTask == null) {
                log.warn("未找到下载任务: downloadTaskId={}", downloadTaskId);
                return;
            }
            
            // 检查任务状态是否为完成
            if (downloadTask.getStatus() != 2) { // 2表示完成状态
                log.debug("下载任务尚未完成: downloadTaskId={}, status={}", downloadTaskId, downloadTask.getStatus());
                return;
            }
            
            // 检查文件路径
            String filePath = downloadTask.getLocalPath();
            if (filePath == null || filePath.trim().isEmpty()) {
                log.warn("下载任务完成但文件路径为空: downloadTaskId={}", downloadTaskId);
                return;
            }
            
            // 处理下载完成事件
            handleSingleFileDownloadCompletion(downloadTaskId, filePath);
            
        } catch (Exception e) {
            log.error("检查单文件下载完成状态失败: downloadTaskId={}", downloadTaskId, e);
        }
    }
}