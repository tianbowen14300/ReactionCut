package com.tbw.cut.service.impl;

import com.tbw.cut.dto.VideoDownloadDTO;
import com.tbw.cut.dto.FrontendPartInfo;
import com.tbw.cut.service.FrontendVideoDownloadService;
import com.tbw.cut.service.FrontendPartDownloadService;
import com.tbw.cut.service.VideoDownloadService;
import com.tbw.cut.bilibili.BilibiliService;
import com.tbw.cut.entity.DownloadTask;

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
     * 修复：同步创建数据库记录，返回真实的数据库ID
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
            String title = getVideoTitle(bvid, aid);
            
            // 创建主下载记录（用于集成关联）
            com.tbw.cut.entity.VideoDownload mainDownload = new com.tbw.cut.entity.VideoDownload();
            mainDownload.setBvid(bvid);
            mainDownload.setAid(aid);
            mainDownload.setTitle(title);
            mainDownload.setPartTitle("集成下载任务");
            mainDownload.setPartCount(parts != null ? parts.size() : 0);
            mainDownload.setCurrentPart(1);
            mainDownload.setDownloadUrl(videoUrl);
            
            // 设置下载配置信息
            if (config != null) {
                if (config.containsKey("resolution")) {
                    String resolutionValue = config.get("resolution").toString();
                    mainDownload.setResolution(resolutionValue);
                }
                if (config.containsKey("codec")) {
                    mainDownload.setCodec(config.get("codec").toString());
                }
                if (config.containsKey("format")) {
                    mainDownload.setFormat(config.get("format").toString());
                }
            }
            
            mainDownload.setStatus(0); // Pending
            mainDownload.setProgress(0);
            
            // 同步创建数据库记录
            mainDownload.setCreateTime(LocalDateTime.now());
            mainDownload.setUpdateTime(LocalDateTime.now());
            boolean saved = videoDownloadService.save(mainDownload);
            if (!saved || mainDownload.getId() == null) {
                log.error("创建主下载记录失败");
                return null;
            }
            
            Long mainDownloadId = mainDownload.getId();
            log.info("成功创建主下载记录，ID: {}, bvid: {}, parts: {}", mainDownloadId, bvid, parts.size());
            
            // 创建下载任务对象，设置真实的数据库ID
            DownloadTask downloadTask = new DownloadTask(mainDownloadId, videoUrl, bvid, aid, parts, config);
            
            // 将任务添加到DownloadTaskManager队列中进行异步处理
            frontendPartDownloadService.addDownloadTask(downloadTask);
            
            log.info("下载任务已提交到队列，主记录ID: {}, bvid: {}, parts: {}", mainDownloadId, bvid, parts.size());
            
            return mainDownloadId;
        } catch (Exception e) {
            log.error("处理前端part下载请求失败", e);
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
}