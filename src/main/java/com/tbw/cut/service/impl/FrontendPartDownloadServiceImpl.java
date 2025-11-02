package com.tbw.cut.service.impl;

import com.tbw.cut.service.FrontendPartDownloadService;
import com.tbw.cut.utils.FFmpegUtil;
import com.tbw.cut.bilibili.BilibiliService;
import com.tbw.cut.bilibili.BilibiliUtils;
import com.tbw.cut.service.VideoDownloadService;
import com.tbw.cut.service.PartDownloadService;
import com.tbw.cut.service.DownloadTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class FrontendPartDownloadServiceImpl implements FrontendPartDownloadService {
    
    @Autowired
    private FFmpegUtil ffmpegUtil;
    
    @Autowired
    private BilibiliService bilibiliService;
    
    @Autowired
    private VideoDownloadService videoDownloadService;
    
    @Autowired
    private PartDownloadService partDownloadService;
    
    @Autowired
    private DownloadTaskManager downloadTaskManager;
    
    // 分辨率映射表
    private static final Map<String, String> RESOLUTION_LABEL_MAP = new java.util.HashMap<>();
    
    static {
        RESOLUTION_LABEL_MAP.put("16", "360P 流畅");
        RESOLUTION_LABEL_MAP.put("32", "480P 清晰");
        RESOLUTION_LABEL_MAP.put("64", "720P 高清");
        RESOLUTION_LABEL_MAP.put("74", "720P60 高清");
        RESOLUTION_LABEL_MAP.put("80", "1080P 高清");
        RESOLUTION_LABEL_MAP.put("112", "1080P+ 高码率");
        RESOLUTION_LABEL_MAP.put("116", "1080P60 高帧率");
        RESOLUTION_LABEL_MAP.put("120", "4K 超清");
        RESOLUTION_LABEL_MAP.put("125", "HDR 真彩色");
        RESOLUTION_LABEL_MAP.put("126", "杜比视界");
        RESOLUTION_LABEL_MAP.put("127", "8K 超高清");
    }
    
    @Override
    public void downloadParts(Long taskId, String videoUrl, String bvid, String aid, 
                             List<Map<String, Object>> parts, Map<String, Object> config) {
        new Thread(() -> {
            try {
                // 获取视频标题
                String title = getVideoTitle(bvid, aid, taskId);
                log.info("获取到视频标题: {}", title);
                
                // 获取下载路径配置
                String downloadPath = null;
                if (config.containsKey("downloadPath")) {
                    downloadPath = config.get("downloadPath").toString();
                }
                
                // 获取下载名称配置
                String downloadName = null;
                if (config.containsKey("downloadName")) {
                    downloadName = config.get("downloadName").toString();
                }
                
                // 确定主文件夹路径
                Path mainFolderPath;
                if (downloadPath != null && !downloadPath.isEmpty()) {
                    // 使用用户指定的下载路径
                    if (downloadName != null && !downloadName.isEmpty()) {
                        // 如果提供了下载名称，使用下载名称作为文件夹名
                        mainFolderPath = Paths.get(downloadPath, downloadName);
                    } else {
                        // 如果没有提供下载名称，使用视频标题
                        mainFolderPath = Paths.get(downloadPath, title);
                    }
                } else {
                    // 使用默认路径
                    String mainFolderName = (downloadName != null && !downloadName.isEmpty()) ? downloadName : title;
                    mainFolderPath = Paths.get(ffmpegUtil.getVideoStorageDir(), mainFolderName);
                }
                
                log.info("主文件夹路径: {}", mainFolderPath.toString());
                
                // 创建主文件夹
                Files.createDirectories(mainFolderPath);
                
                // 为每个part创建独立的下载记录
                List<Long> partTaskIds = new ArrayList<>();
                log.info("准备为 {} 个分P创建下载记录", parts.size());
                
                for (int i = 0; i < parts.size(); i++) {
                    Map<String, Object> part = parts.get(i);
                    Long cid = (Long) part.get("cid");
                    String partTitle = (String) part.get("title");
                    
                    log.info("处理分P: cid={}, title={}", cid, partTitle);
                    
                    if (cid != null) {
                        // 创建每个part的下载记录
                        com.tbw.cut.entity.VideoDownload partDownload = new com.tbw.cut.entity.VideoDownload();
                        partDownload.setBvid(bvid);
                        partDownload.setAid(aid);
                        partDownload.setTitle(title);
                        partDownload.setPartTitle(partTitle);
                        partDownload.setPartCount(parts.size());
                        partDownload.setCurrentPart(i + 1);
                        partDownload.setDownloadUrl(videoUrl);
                        
                        // 设置下载配置信息
                        if (config.containsKey("resolution")) {
                            String resolutionValue = config.get("resolution").toString();
                            // 转换分辨率值为标签
                            String resolutionLabel = RESOLUTION_LABEL_MAP.getOrDefault(resolutionValue, resolutionValue + "P");
                            partDownload.setResolution(resolutionLabel);
                        }
                        if (config.containsKey("codec")) {
                            partDownload.setCodec(config.get("codec").toString());
                        }
                        if (config.containsKey("format")) {
                            partDownload.setFormat(config.get("format").toString());
                        }
                        
                        partDownload.setStatus(0); // Pending
                        partDownload.setProgress(0);
                        
                        Long partTaskId = partDownloadService.createPartDownload(partDownload);
                        if (partTaskId != null) {
                            partTaskIds.add(partTaskId);
                            log.info("成功创建分P下载记录，任务ID: {}", partTaskId);
                        } else {
                            log.error("创建分P下载记录失败，cid: {}", cid);
                        }
                    } else {
                        log.warn("分P信息不完整，cid为空");
                    }
                }
                
                // 将每个part任务添加到下载队列中
                for (Long partTaskId : partTaskIds) {
                    com.tbw.cut.entity.VideoDownload partTask = partDownloadService.getById(partTaskId);
                    if (partTask != null) {
                        // 添加到下载队列
                        boolean added = downloadTaskManager.addDownloadTask(partTask);
                        if (added) {
                            log.info("成功将分P任务添加到下载队列，任务ID: {}", partTaskId);
                        } else {
                            log.error("将分P任务添加到下载队列失败，任务ID: {}", partTaskId);
                        }
                    }
                }
                
                // 下载每个part（直接下载，不使用队列）
                boolean allSuccess = true;
                
                for (int i = 0; i < parts.size(); i++) {
                    Map<String, Object> part = parts.get(i);
                    Long cid = (Long) part.get("cid");
                    String partTitle = (String) part.get("title");
                    
                    log.info("开始下载分P: cid={}, title={}", cid, partTitle);
                    
                    if (cid != null && i < partTaskIds.size()) {
                        // 获取对应的part任务ID
                        Long partTaskId = partTaskIds.get(i);
                        
                        // 更新part任务状态为下载中
                        partDownloadService.updatePartProgress(partTaskId, 0);
                        log.info("更新分P任务状态为下载中，任务ID: {}", partTaskId);
                        
                        // 直接在主文件夹下下载视频，不创建子文件夹
                        // 使用分P序号作为文件名，如1.mp4、2.mp4
                        String outputFileName = (i + 1) + ".mp4";
                        
                        log.info("下载文件名: {}", outputFileName);
                        
                        // 获取视频流URL，使用前端配置
                        Map<String, String> bilibiliParams = convertFrontendConfigToBilibiliParams(config);
                        String actualVideoUrl = getActualVideoStreamUrl(bvid, aid, String.valueOf(cid), bilibiliParams);
                        
                        if (actualVideoUrl != null) {
                            log.info("获取到视频流URL: {}", actualVideoUrl);
                            
                            // Download video using FFmpeg to main folder with progress tracking
                            String localPath = ffmpegUtil.downloadVideoToDirectoryWithProgress(actualVideoUrl, outputFileName, mainFolderPath.toString(), 
                                new com.tbw.cut.utils.FFmpegUtil.ProgressCallback() {
                                    @Override
                                    public void onProgress(int progress) {
                                        // 更新part任务进度
                                        partDownloadService.updatePartProgress(partTaskId, progress);
                                        log.debug("更新分P任务进度，任务ID: {}, 进度: {}%", partTaskId, progress);
                                    }
                                });
                            
                            if (localPath != null) {
                                // 检查文件是否真正存在且大小合理
                                java.io.File downloadedFile = new java.io.File(localPath);
                                if (downloadedFile.exists() && downloadedFile.length() > 0) {
                                    log.info("Part {} 下载成功: {}", cid, localPath);
                                    
                                    // 更新part任务为完成状态
                                    partDownloadService.completePartDownload(partTaskId, localPath);
                                } else {
                                    log.error("Part {} 下载文件验证失败，文件不存在或大小为0", cid);
                                    allSuccess = false;
                                    
                                    // 更新part任务为失败状态
                                    partDownloadService.failPartDownload(partTaskId, "下载文件验证失败");
                                }
                            } else {
                                log.error("Part {} 下载失败", cid);
                                allSuccess = false;
                                
                                // 更新part任务为失败状态
                                partDownloadService.failPartDownload(partTaskId, "下载失败");
                            }
                        } else {
                            log.error("无法获取Part {} 的视频流URL", cid);
                            allSuccess = false;
                            
                            // 更新part任务为失败状态
                            partDownloadService.failPartDownload(partTaskId, "无法获取视频流URL");
                        }
                    } else {
                        log.warn("分P信息不完整或任务ID不匹配: cid={}, index={}", cid, i);
                    }
                }
                
                log.info("所有分P下载完成，整体结果: {}", allSuccess ? "成功" : "失败");
            } catch (Exception e) {
                log.error("下载Parts时发生异常", e);
            }
        }).start();
    }
    
    /**
     * 将前端配置转换为Bilibili API参数
     */
    private Map<String, String> convertFrontendConfigToBilibiliParams(Map<String, Object> config) {
        if (config == null) {
            return new java.util.HashMap<>();
        }
        
        Map<String, String> bilibiliParams = new java.util.HashMap<>();
        
        // 处理分辨率
        if (config.containsKey("resolution")) {
            Object resolutionObj = config.get("resolution");
            if (resolutionObj != null) {
                String resolution = resolutionObj.toString();
                // 直接使用前端传递的分辨率值作为qn参数
                bilibiliParams.put("qn", resolution);
                log.info("设置分辨率参数 qn={}", resolution);
            }
        }
        
        // 处理格式
        if (config.containsKey("format")) {
            Object formatObj = config.get("format");
            if (formatObj != null) {
                String format = formatObj.toString();
                String fnval = convertFormatToFnval(format);
                if (fnval != null) {
                    bilibiliParams.put("fnval", fnval);
                    log.info("设置格式参数 fnval={}", fnval);
                }
            }
        }
        
        // 处理编码格式
        if (config.containsKey("codec")) {
            Object codecObj = config.get("codec");
            if (codecObj != null) {
                String codec = codecObj.toString();
                // 将编码格式存储在参数中，供后续筛选视频流时使用
                bilibiliParams.put("codec", codec);
                log.info("设置编码格式参数 codec={}", codec);
            }
        }
        
        // 处理内容类型
        if (config.containsKey("content")) {
            Object contentObj = config.get("content");
            if (contentObj != null) {
                String content = contentObj.toString();
                if ("audio_only".equals(content)) {
                    // 只下载音频，设置fnval为音频格式
                    bilibiliParams.put("fnval", "0"); // 这里可能需要调整，根据实际需求
                    log.info("设置音频-only模式，fnval=0");
                }
                log.info("设置内容类型 content={}", content);
            }
        }
        
        // 设置默认值
        if (!bilibiliParams.containsKey("fnver")) {
            bilibiliParams.put("fnver", "0");
            log.debug("设置默认fnver=0");
        }
        
        if (!bilibiliParams.containsKey("fourk")) {
            bilibiliParams.put("fourk", "1"); // 默认允许4K
            log.debug("设置默认fourk=1");
        }
        
        log.info("转换后的Bilibili参数: {}", bilibiliParams);
        return bilibiliParams;
    }
    
    /**
     * 将格式转换为fnval值
     */
    private String convertFormatToFnval(String format) {
        switch (format.toLowerCase()) {
            case "mp4":
                return "1";
            case "flv":
                return "0";
            case "dash":
                return "16";
            default:
                return "16"; // 默认DASH
        }
    }
    
    /**
     * 获取视频标题
     */
    private String getVideoTitle(String bvid, String aid, Long taskId) {
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
        return "download_" + taskId;
    }
    
    /**
     * 获取实际的视频流URL
     */
    private String getActualVideoStreamUrl(String bvid, String aid, String cid, Map<String, String> params) {
        try {
            String qn = params.get("qn");
            String fnval = params.get("fnval");
            String fnver = params.get("fnver");
            String fourk = params.get("fourk");
            String codec = params.get("codec");
            
            log.info("获取视频流URL，bvid: {}, aid: {}, cid: {}, qn: {}, fnval: {}, fnver: {}, fourk: {}, codec: {}", 
                    bvid, aid, cid, qn, fnval, fnver, fourk, codec);
            
            // 使用Bilibili API获取视频播放信息
            com.alibaba.fastjson.JSONObject playInfo = null;
            if (bvid != null && !bvid.isEmpty()) {
                playInfo = bilibiliService.getVideoPlayInfo(bvid, cid, qn, fnval, fnver, fourk);
            } else if (aid != null && !aid.isEmpty()) {
                playInfo = bilibiliService.getVideoPlayInfo(BilibiliUtils.avToBv("av" + aid), cid, qn, fnval, fnver, fourk);
            }
            
            log.info("Bilibili API返回的播放信息: {}", playInfo);
            
            // 从播放信息中提取实际的视频URL
            if (playInfo != null) {
                // 优先尝试DASH格式
                if (playInfo.containsKey("dash")) {
                    com.alibaba.fastjson.JSONObject dash = playInfo.getJSONObject("dash");
                    if (dash != null && dash.containsKey("video")) {
                        com.alibaba.fastjson.JSONArray videos = dash.getJSONArray("video");
                        if (videos != null && videos.size() > 0) {
                            // 打印所有可用的视频流信息
                            log.info("可用的DASH视频流数量: {}", videos.size());
                            for (int i = 0; i < videos.size(); i++) {
                                com.alibaba.fastjson.JSONObject video = videos.getJSONObject(i);
                                log.info("视频流 {}: id={}, width={}, height={}, codecs={}, baseUrl={}", 
                                        i, video.getString("id"), video.getInteger("width"), video.getInteger("height"), 
                                        video.getString("codecs"), video.getString("baseUrl"));
                            }
                            
                            // 根据qn参数和编码格式查找匹配的视频流
                            com.alibaba.fastjson.JSONObject matchedVideo = null;
                            
                            // 创建一个列表来存储所有匹配的视频流
                            java.util.List<com.alibaba.fastjson.JSONObject> matchedVideos = new java.util.ArrayList<>();
                            
                            // 首先收集所有匹配qn的视频流
                            if (qn != null && !qn.isEmpty()) {
                                for (int i = 0; i < videos.size(); i++) {
                                    com.alibaba.fastjson.JSONObject video = videos.getJSONObject(i);
                                    String videoId = video.getString("id");
                                    
                                    if (qn.equals(videoId)) {
                                        matchedVideos.add(video);
                                        log.debug("找到匹配qn={}的视频流: id={}, codecs={}", qn, videoId, video.getString("codecs"));
                                    }
                                }
                            }
                            
                            // 如果有匹配qn的视频流，进一步筛选编码格式
                            if (!matchedVideos.isEmpty() && codec != null && !codec.isEmpty()) {
                                for (com.alibaba.fastjson.JSONObject video : matchedVideos) {
                                    String videoCodec = video.getString("codecs");
                                    if (videoCodec != null && videoCodec.contains(codec)) {
                                        matchedVideo = video;
                                        log.info("找到同时匹配qn={}和编码格式={}的视频流", qn, codec);
                                        break;
                                    }
                                }
                            }
                            
                            // 如果没有同时匹配的，但有匹配qn的视频流，使用第一个匹配qn的
                            if (matchedVideo == null && !matchedVideos.isEmpty()) {
                                matchedVideo = matchedVideos.get(0);
                                log.info("找到匹配qn={}的视频流", qn);
                            }
                            
                            // 如果还没有匹配的，尝试只匹配编码格式
                            if (matchedVideo == null && codec != null && !codec.isEmpty()) {
                                for (int i = 0; i < videos.size(); i++) {
                                    com.alibaba.fastjson.JSONObject video = videos.getJSONObject(i);
                                    String videoCodec = video.getString("codecs");
                                    
                                    if (videoCodec != null && videoCodec.contains(codec)) {
                                        matchedVideo = video;
                                        log.info("找到匹配编码格式={}的视频流", codec);
                                        break;
                                    }
                                }
                            }
                            
                            // 如果仍然没有找到匹配的视频流，则使用第一个视频流
                            if (matchedVideo == null) {
                                matchedVideo = videos.getJSONObject(0);
                                log.warn("未找到匹配的视频流，使用第一个视频流");
                            }
                            
                            // 记录最终选择的视频流信息
                            log.info("最终选择的视频流: id={}, width={}, height={}, codecs={}, baseUrl={}", 
                                    matchedVideo.getString("id"), matchedVideo.getInteger("width"), 
                                    matchedVideo.getInteger("height"), matchedVideo.getString("codecs"), 
                                    matchedVideo.getString("baseUrl"));
                            
                            if (matchedVideo.containsKey("baseUrl")) {
                                String baseUrl = matchedVideo.getString("baseUrl");
                                log.info("获取到DASH视频流URL (baseUrl): {}", baseUrl);
                                return baseUrl;
                            } else if (matchedVideo.containsKey("base_url")) {
                                String baseUrl = matchedVideo.getString("base_url");
                                log.info("获取到DASH视频流URL (base_url): {}", baseUrl);
                                return baseUrl;
                            }
                        }
                    }
                }
                
                // 如果没有DASH，尝试durl格式
                if (playInfo.containsKey("durl")) {
                    com.alibaba.fastjson.JSONArray durls = playInfo.getJSONArray("durl");
                    if (durls != null && durls.size() > 0) {
                        // 打印所有可用的视频流信息
                        log.info("可用的MP4/FLV视频流数量: {}", durls.size());
                        for (int i = 0; i < durls.size(); i++) {
                            com.alibaba.fastjson.JSONObject durl = durls.getJSONObject(i);
                            log.info("视频流 {}: url={}", i, durl.getString("url"));
                        }
                        
                        com.alibaba.fastjson.JSONObject firstDurl = durls.getJSONObject(0);
                        if (firstDurl.containsKey("url")) {
                            String url = firstDurl.getString("url");
                            log.info("获取到MP4/FLV视频流URL: {}", url);
                            return url;
                        }
                    }
                }
            }
            
            log.error("无法获取视频播放信息，bvid: {}, aid: {}, cid: {}", bvid, aid, cid);
            return null;
        } catch (Exception e) {
            log.error("获取视频流URL时发生异常", e);
            return null;
        }
    }
}