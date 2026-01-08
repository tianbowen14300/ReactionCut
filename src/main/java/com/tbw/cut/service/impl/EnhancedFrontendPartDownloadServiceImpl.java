package com.tbw.cut.service.impl;

import com.tbw.cut.entity.DownloadTask;
import com.tbw.cut.service.FrontendPartDownloadService;
import com.tbw.cut.service.download.EnhancedDownloadManager;
import com.tbw.cut.service.download.model.*;
import com.tbw.cut.bilibili.BilibiliService;
import com.tbw.cut.bilibili.BilibiliUtils;
import com.tbw.cut.service.PartDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 增强的前端分P下载服务实现
 * 使用新的EnhancedDownloadManager进行并发下载
 */
@Slf4j
@Service("enhancedFrontendPartDownloadService")
@Primary
public class EnhancedFrontendPartDownloadServiceImpl implements FrontendPartDownloadService {
    
    @Autowired
    private EnhancedDownloadManager enhancedDownloadManager;
    
    @Autowired
    private BilibiliService bilibiliService;
    
    @Autowired
    private PartDownloadService partDownloadService;
    
    // 分辨率映射表
    private static final Map<String, String> RESOLUTION_LABEL_MAP;
    
    static {
        Map<String, String> map = new java.util.HashMap<>();
        map.put("16", "360P 流畅");
        map.put("32", "480P 清晰");
        map.put("64", "720P 高清");
        map.put("74", "720P60 高清");
        map.put("80", "1080P 高清");
        map.put("112", "1080P+ 高码率");
        map.put("116", "1080P60 高帧率");
        map.put("120", "4K 超清");
        map.put("125", "HDR 真彩色");
        map.put("126", "杜比视界");
        map.put("127", "8K 超高清");
        RESOLUTION_LABEL_MAP = java.util.Collections.unmodifiableMap(map);
    }
    
    @Override
    public void addDownloadTask(DownloadTask downloadTask) {
        // 异步执行下载任务
        CompletableFuture.runAsync(() -> executeDownload(downloadTask));
    }
    
    @Override
    public void executeDownload(DownloadTask downloadTask) {
        executeEnhancedDownload(downloadTask.getId(), downloadTask.getVideoUrl(), 
                               downloadTask.getBvid(), downloadTask.getAid(), 
                               downloadTask.getParts(), downloadTask.getConfig());
    }
    
    /**
     * 执行增强的下载逻辑
     */
    private void executeEnhancedDownload(Long taskId, String videoUrl, String bvid, String aid, 
                                        List<Map<String, Object>> parts, Map<String, Object> config) {
        try {
            log.info("开始增强下载任务: taskId={}, bvid={}, parts={}", taskId, bvid, parts.size());
            
            // 获取视频标题
            String title = getVideoTitle(bvid, aid, taskId);
            log.info("获取到视频标题: {}", title);
            
            // 创建下载配置
            DownloadConfig downloadConfig = createDownloadConfig(config);
            
            // 确定下载路径
            Path downloadPath = determineDownloadPath(config, title);
            Files.createDirectories(downloadPath);
            log.info("下载路径: {}", downloadPath);
            
            // 转换分P信息为VideoPart对象
            List<VideoPart> videoParts = convertToVideoParts(parts, bvid, aid, downloadPath, config);
            
            // 创建分P下载记录
            List<Long> partTaskIds = createPartDownloadRecords(videoParts, title, bvid, aid, config);
            
            // 使用增强下载管理器执行并发下载
            CompletableFuture<DownloadResult> downloadFuture = enhancedDownloadManager
                .startDownload(videoUrl, videoParts, downloadConfig);
            
            // 处理下载结果
            downloadFuture.thenAccept(result -> {
                handleDownloadResult(result, partTaskIds, videoParts);
            }).exceptionally(throwable -> {
                log.error("下载任务执行失败: taskId={}", taskId, throwable);
                handleDownloadFailure(throwable, partTaskIds);
                return null;
            });
            
        } catch (Exception e) {
            log.error("执行增强下载时发生异常: taskId={}", taskId, e);
        }
    }
    
    /**
     * 创建下载配置
     */
    private DownloadConfig createDownloadConfig(Map<String, Object> config) {
        DownloadConfig.DownloadConfigBuilder builder = DownloadConfig.builder()
            .maxConcurrentTasks(3)
            .maxRetryAttempts(3)
            .timeoutSeconds(300)
            .enableResume(true);
        
        if (config != null) {
            // 设置分辨率
            if (config.containsKey("resolution")) {
                builder.resolution(config.get("resolution").toString());
            }
            
            // 设置格式
            if (config.containsKey("format")) {
                builder.format(config.get("format").toString());
            }
            
            // 设置编码
            if (config.containsKey("codec")) {
                builder.codec(config.get("codec").toString());
            }
            
            // 设置并发数
            if (config.containsKey("maxConcurrent")) {
                try {
                    int maxConcurrent = Integer.parseInt(config.get("maxConcurrent").toString());
                    builder.maxConcurrentTasks(maxConcurrent);
                } catch (NumberFormatException e) {
                    log.warn("无效的并发数配置: {}", config.get("maxConcurrent"));
                }
            }
        }
        
        return builder.build();
    }
    
    /**
     * 确定下载路径
     */
    private Path determineDownloadPath(Map<String, Object> config, String title) {
        String downloadPath = null;
        String downloadName = null;
        
        if (config != null) {
            if (config.containsKey("downloadPath")) {
                downloadPath = config.get("downloadPath").toString();
            }
            if (config.containsKey("downloadName")) {
                downloadName = config.get("downloadName").toString();
            }
        }
        
        if (downloadPath != null && !downloadPath.isEmpty()) {
            String folderName = (downloadName != null && !downloadName.isEmpty()) ? downloadName : title;
            return Paths.get(downloadPath, folderName);
        } else {
            String folderName = (downloadName != null && !downloadName.isEmpty()) ? downloadName : title;
            return Paths.get(System.getProperty("user.home"), "Downloads", folderName);
        }
    }
    
    /**
     * 转换分P信息为VideoPart对象
     */
    private List<VideoPart> convertToVideoParts(List<Map<String, Object>> parts, String bvid, String aid, 
                                               Path downloadPath, Map<String, Object> config) {
        List<VideoPart> videoParts = new ArrayList<>();
        
        for (int i = 0; i < parts.size(); i++) {
            Map<String, Object> part = parts.get(i);
            Long cid = (Long) part.get("cid");
            String partTitle = (String) part.get("title");
            
            if (cid != null) {
                // 获取视频流URL
                String streamUrl = getActualVideoStreamUrl(bvid, aid, String.valueOf(cid), 
                                                         convertFrontendConfigToBilibiliParams(config));
                
                if (streamUrl != null) {
                    String outputFileName = (i + 1) + ".mp4";
                    String outputPath = downloadPath.resolve(outputFileName).toString();
                    
                    VideoPart videoPart = VideoPart.builder()
                        .cid(cid)
                        .title(partTitle != null ? partTitle : "Part " + (i + 1))
                        .url(streamUrl)
                        .outputPath(outputPath)
                        .outputFileName(outputFileName)
                        .partIndex(i + 1)
                        .totalParts(parts.size())
                        .build();
                    
                    videoParts.add(videoPart);
                    log.info("创建VideoPart: cid={}, title={}, url={}", cid, partTitle, streamUrl);
                } else {
                    log.error("无法获取分P视频流URL: cid={}", cid);
                }
            }
        }
        
        return videoParts;
    }
    
    /**
     * 创建分P下载记录
     */
    private List<Long> createPartDownloadRecords(List<VideoPart> videoParts, String title, 
                                                String bvid, String aid, Map<String, Object> config) {
        List<Long> partTaskIds = new ArrayList<>();
        
        for (int i = 0; i < videoParts.size(); i++) {
            VideoPart part = videoParts.get(i);
            
            com.tbw.cut.entity.VideoDownload partDownload = new com.tbw.cut.entity.VideoDownload();
            partDownload.setBvid(bvid);
            partDownload.setAid(aid);
            partDownload.setTitle(title);
            partDownload.setPartTitle(part.getTitle());
            partDownload.setPartCount(videoParts.size());
            partDownload.setCurrentPart(part.getPartIndex());
            partDownload.setDownloadUrl(part.getUrl());
            
            // 设置下载配置信息
            if (config != null) {
                if (config.containsKey("resolution")) {
                    String resolutionValue = config.get("resolution").toString();
                    String resolutionLabel = RESOLUTION_LABEL_MAP.getOrDefault(resolutionValue, resolutionValue + "P");
                    partDownload.setResolution(resolutionLabel);
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
            
            Long partTaskId = partDownloadService.createPartDownload(partDownload);
            if (partTaskId != null) {
                partTaskIds.add(partTaskId);
                // 关键修复：将数据库ID存储到VideoPart中，用于后续进度更新
                part.setDatabaseId(partTaskId);
                log.info("成功创建分P下载记录，数据库ID: {}, CID: {}", partTaskId, part.getCid());
            } else {
                log.error("创建分P下载记录失败，cid: {}", part.getCid());
            }
        }
        
        return partTaskIds;
    }
    
    /**
     * 处理下载结果
     */
    private void handleDownloadResult(DownloadResult result, List<Long> partTaskIds, List<VideoPart> videoParts) {
        if (result.isSuccess()) {
            log.info("所有分P下载成功，文件数量: {}", result.getFilePaths().size());
            
            // 更新所有分P任务为完成状态
            for (int i = 0; i < partTaskIds.size() && i < result.getFilePaths().size(); i++) {
                Long partTaskId = partTaskIds.get(i);
                String filePath = result.getFilePaths().get(i);
                
                if (filePath != null) {
                    partDownloadService.completePartDownload(partTaskId, filePath);
                    log.info("分P任务完成: taskId={}, filePath={}", partTaskId, filePath);
                } else {
                    partDownloadService.failPartDownload(partTaskId, "下载文件路径为空");
                    log.error("分P任务失败: taskId={}, 文件路径为空", partTaskId);
                }
            }
        } else {
            log.error("下载失败: {}", result.getErrorMessage());
            handleDownloadFailure(new RuntimeException(result.getErrorMessage()), partTaskIds);
        }
    }
    
    /**
     * 处理下载失败
     */
    private void handleDownloadFailure(Throwable throwable, List<Long> partTaskIds) {
        String errorMessage = throwable.getMessage();
        
        // 更新所有分P任务为失败状态
        for (Long partTaskId : partTaskIds) {
            partDownloadService.failPartDownload(partTaskId, errorMessage);
            log.error("分P任务失败: taskId={}, error={}", partTaskId, errorMessage);
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
                bilibiliParams.put("codec", codec);
                log.info("设置编码格式参数 codec={}", codec);
            }
        }
        
        // 设置默认值
        bilibiliParams.putIfAbsent("fnver", "0");
        bilibiliParams.putIfAbsent("fourk", "1");
        
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
     * 获取实际的视频流URL
     */
    private String getActualVideoStreamUrl(String bvid, String aid, String cid, Map<String, String> params) {
        try {
            String qn = params.get("qn");
            String fnval = params.get("fnval");
            String fnver = params.get("fnver");
            String fourk = params.get("fourk");
            String codec = params.get("codec");
            
            log.info("获取视频流URL，bvid: {}, aid: {}, cid: {}, qn: {}, fnval: {}", 
                    bvid, aid, cid, qn, fnval);
            
            // 使用Bilibili API获取视频播放信息
            com.alibaba.fastjson.JSONObject playInfo = null;
            if (bvid != null && !bvid.isEmpty()) {
                playInfo = bilibiliService.getVideoPlayInfo(bvid, cid, qn, fnval, fnver, fourk);
            } else if (aid != null && !aid.isEmpty()) {
                playInfo = bilibiliService.getVideoPlayInfo(BilibiliUtils.avToBv("av" + aid), cid, qn, fnval, fnver, fourk);
            }
            
            if (playInfo != null) {
                // 优先尝试包含音频的durl格式（MP4/FLV）
                if (playInfo.containsKey("durl")) {
                    com.alibaba.fastjson.JSONArray durls = playInfo.getJSONArray("durl");
                    if (durls != null && durls.size() > 0) {
                        com.alibaba.fastjson.JSONObject firstDurl = durls.getJSONObject(0);
                        if (firstDurl.containsKey("url")) {
                            String url = firstDurl.getString("url");
                            log.info("获取到MP4/FLV视频流URL: {}", url);
                            return url;
                        }
                    }
                }
                
                // 如果没有durl，再尝试DASH格式
                if (playInfo.containsKey("dash")) {
                    com.alibaba.fastjson.JSONObject dash = playInfo.getJSONObject("dash");
                    if (dash != null && dash.containsKey("video")) {
                        com.alibaba.fastjson.JSONArray videos = dash.getJSONArray("video");
                        if (videos != null && videos.size() > 0) {
                            // 根据参数选择合适的视频流
                            com.alibaba.fastjson.JSONObject selectedVideo = selectBestVideoStream(videos, qn, codec);
                            
                            if (selectedVideo != null) {
                                String baseUrl = selectedVideo.getString("baseUrl");
                                if (baseUrl == null) {
                                    baseUrl = selectedVideo.getString("base_url");
                                }
                                
                                if (baseUrl != null) {
                                    log.info("获取到DASH视频流URL: {}", baseUrl);
                                    return baseUrl;
                                }
                            }
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
    
    /**
     * 选择最佳视频流
     */
    private com.alibaba.fastjson.JSONObject selectBestVideoStream(com.alibaba.fastjson.JSONArray videos, 
                                                                 String qn, String codec) {
        // 首先尝试匹配qn和codec
        if (qn != null && codec != null) {
            for (int i = 0; i < videos.size(); i++) {
                com.alibaba.fastjson.JSONObject video = videos.getJSONObject(i);
                String videoId = video.getString("id");
                String videoCodec = video.getString("codecs");
                
                if (qn.equals(videoId) && videoCodec != null && videoCodec.contains(codec)) {
                    return video;
                }
            }
        }
        
        // 然后尝试只匹配qn
        if (qn != null) {
            for (int i = 0; i < videos.size(); i++) {
                com.alibaba.fastjson.JSONObject video = videos.getJSONObject(i);
                String videoId = video.getString("id");
                
                if (qn.equals(videoId)) {
                    return video;
                }
            }
        }
        
        // 最后尝试只匹配codec
        if (codec != null) {
            for (int i = 0; i < videos.size(); i++) {
                com.alibaba.fastjson.JSONObject video = videos.getJSONObject(i);
                String videoCodec = video.getString("codecs");
                
                if (videoCodec != null && videoCodec.contains(codec)) {
                    return video;
                }
            }
        }
        
        // 如果都没有匹配，返回第一个
        return videos.getJSONObject(0);
    }
}