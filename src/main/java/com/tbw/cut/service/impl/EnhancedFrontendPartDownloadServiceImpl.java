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
    
    @Autowired
    private com.tbw.cut.service.download.queue.VideoDownloadQueueManager videoDownloadQueueManager;
    
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
        log.info("=== 增强下载服务收到任务: taskId={}, bvid={} ===", 
            downloadTask.getId(), downloadTask.getBvid());
        // 直接执行下载任务，使用VideoDownloadQueueManager进行并发控制
        executeDownload(downloadTask);
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
            
            // 创建视频下载请求
            com.tbw.cut.service.download.model.VideoDownloadRequest videoRequest = 
                com.tbw.cut.service.download.model.VideoDownloadRequest.builder()
                    .videoUrl(videoUrl)
                    .videoTitle(title)
                    .parts(videoParts)
                    .config(downloadConfig)
                    .estimatedFileSize(estimateTotalSize(videoParts))
                    .enableSegmentedDownload(downloadConfig.getEnableSegmentedDownload())
                    .outputDirectory(downloadPath.toString())
                    .extraParams(createExtraParams(bvid, aid))
                    .build();
            
            // 使用VideoDownloadQueueManager提交下载任务，实现视频级别并发控制
            log.info("=== 通过VideoDownloadQueueManager提交下载任务: {} ===", title);
            CompletableFuture<DownloadResult> downloadFuture = videoDownloadQueueManager
                .submitVideoDownload(videoRequest);
            
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
            .connectionTimeout(30000L)
            .readTimeout(300000L)
            .maxRetryAttempts(3)
            .enableSegmentedDownload(true);
        
        if (config != null) {
            // 设置超时时间
            if (config.containsKey("timeout")) {
                try {
                    int timeout = Integer.parseInt(config.get("timeout").toString());
                    builder.connectionTimeout((long) timeout * 1000);
                    builder.readTimeout((long) timeout * 1000);
                } catch (NumberFormatException e) {
                    log.warn("无效的超时配置: {}", config.get("timeout"));
                }
            }
            
            // 设置重试次数
            if (config.containsKey("maxRetry")) {
                try {
                    int maxRetry = Integer.parseInt(config.get("maxRetry").toString());
                    builder.maxRetryAttempts(maxRetry);
                } catch (NumberFormatException e) {
                    log.warn("无效的重试次数配置: {}", config.get("maxRetry"));
                }
            }
            
            // 设置分段下载
            if (config.containsKey("enableSegmented")) {
                try {
                    boolean enableSegmented = Boolean.parseBoolean(config.get("enableSegmented").toString());
                    builder.enableSegmentedDownload(enableSegmented);
                } catch (Exception e) {
                    log.warn("无效的分段下载配置: {}", config.get("enableSegmented"));
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
                    
                    VideoPart.VideoPartBuilder videoPartBuilder = VideoPart.builder()
                        .cid(cid)
                        .bvid(bvid)
                        .title(partTitle != null ? partTitle : "Part " + (i + 1))
                        .streamUrl(streamUrl)
                        .outputPath(outputPath)
                        .outputFileName(outputFileName)
                        .partNumber(i + 1);
                    
                    // 检查是否是DASH格式需要合并
                    if (streamUrl.startsWith("DASH_MERGE:")) {
                        // 解析DASH URL
                        String[] urlParts = streamUrl.substring("DASH_MERGE:".length()).split("\\|AUDIO:");
                        if (urlParts.length == 2) {
                            String videoUrl = urlParts[0];
                            String audioUrl = urlParts[1];
                            
                            // 设置DASH相关信息
                            videoPartBuilder.streamUrl(videoUrl); // 主URL设为视频流
                            
                            // 使用extraParams存储音频URL和合并标记
                            Map<String, Object> extraParams = new java.util.HashMap<>();
                            extraParams.put("isDashMerge", true);
                            extraParams.put("audioUrl", audioUrl);
                            extraParams.put("videoUrl", videoUrl);
                            videoPartBuilder.extraParams(extraParams);
                            
                            log.info("创建DASH合并VideoPart: cid={}, videoUrl={}, audioUrl={}", 
                                    cid, videoUrl.substring(0, Math.min(100, videoUrl.length())) + "...",
                                    audioUrl.substring(0, Math.min(100, audioUrl.length())) + "...");
                        } else {
                            log.error("DASH URL格式错误: {}", streamUrl);
                            continue;
                        }
                    } else {
                        log.info("创建普通VideoPart: cid={}, title={}, url={}", cid, partTitle, 
                                streamUrl.substring(0, Math.min(100, streamUrl.length())) + "...");
                    }
                    
                    VideoPart videoPart = videoPartBuilder.build();
                    videoParts.add(videoPart);
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
     * 创建额外参数
     */
    private Map<String, Object> createExtraParams(String bvid, String aid) {
        Map<String, Object> extraParams = new java.util.HashMap<>();
        if (bvid != null) {
            extraParams.put("bvid", bvid);
        }
        if (aid != null) {
            extraParams.put("aid", aid);
        }
        return extraParams;
    }
    
    /**
     * 估算视频总大小
     */
    private long estimateTotalSize(List<VideoPart> videoParts) {
        // 简单估算：每个分P大约100MB
        return videoParts.size() * 100L * 1024 * 1024;
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
        
        // 处理格式 - 优先使用包含音频的格式
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
        } else {
            // 默认优先使用MP4格式（包含音频），如果不可用再使用DASH
            bilibiliParams.put("fnval", "1"); // MP4格式
            log.info("使用默认MP4格式 fnval=1");
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
                return "1";  // MP4格式，包含音频
            case "flv":
                return "0";  // FLV格式（已下线）
            case "dash":
                return "16"; // DASH格式，需要合并音视频
            default:
                return "1";  // 默认MP4格式，确保有音频
        }
    }
    
    /**
     * 获取实际的视频流URL（支持DASH格式的音视频合并）
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
                            log.info("获取到包含音频的MP4/FLV视频流URL: {}", url);
                            return url;
                        }
                    }
                }
                
                // 如果没有durl，处理DASH格式（需要合并音视频流）
                if (playInfo.containsKey("dash")) {
                    return handleDashFormat(playInfo, qn, codec, bvid, cid);
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
     * 处理DASH格式，合并音视频流
     */
    private String handleDashFormat(com.alibaba.fastjson.JSONObject playInfo, String qn, String codec, String bvid, String cid) {
        try {
            com.alibaba.fastjson.JSONObject dash = playInfo.getJSONObject("dash");
            if (dash == null) {
                log.error("DASH对象为空");
                return null;
            }
            
            // 获取视频流
            String videoUrl = null;
            if (dash.containsKey("video")) {
                com.alibaba.fastjson.JSONArray videos = dash.getJSONArray("video");
                if (videos != null && videos.size() > 0) {
                    com.alibaba.fastjson.JSONObject selectedVideo = selectBestVideoStream(videos, qn, codec);
                    if (selectedVideo != null) {
                        videoUrl = selectedVideo.getString("baseUrl");
                        if (videoUrl == null) {
                            videoUrl = selectedVideo.getString("base_url");
                        }
                        log.info("获取到DASH视频流URL: {}", videoUrl);
                    }
                }
            }
            
            // 获取音频流
            String audioUrl = null;
            if (dash.containsKey("audio")) {
                com.alibaba.fastjson.JSONArray audios = dash.getJSONArray("audio");
                if (audios != null && audios.size() > 0) {
                    // 选择最佳音频流（通常选择第一个或最高质量的）
                    com.alibaba.fastjson.JSONObject selectedAudio = selectBestAudioStream(audios);
                    if (selectedAudio != null) {
                        audioUrl = selectedAudio.getString("baseUrl");
                        if (audioUrl == null) {
                            audioUrl = selectedAudio.getString("base_url");
                        }
                        log.info("获取到DASH音频流URL: {}", audioUrl);
                    }
                }
            }
            
            // 如果同时有音视频流，返回特殊格式的URL用于后续处理
            if (videoUrl != null && audioUrl != null) {
                // 使用特殊格式标记这是需要合并的DASH流
                String dashUrl = "DASH_MERGE:" + videoUrl + "|AUDIO:" + audioUrl;
                log.info("获取到DASH音视频流，需要合并处理");
                return dashUrl;
            } else if (videoUrl != null) {
                // 只有视频流，没有音频（可能是无音轨视频）
                log.warn("只获取到视频流，没有音频流，视频可能无音轨");
                return videoUrl;
            } else {
                log.error("无法获取DASH视频流");
                return null;
            }
            
        } catch (Exception e) {
            log.error("处理DASH格式时发生异常", e);
            return null;
        }
    }
    
    /**
     * 选择最佳音频流
     */
    private com.alibaba.fastjson.JSONObject selectBestAudioStream(com.alibaba.fastjson.JSONArray audios) {
        if (audios == null || audios.size() == 0) {
            return null;
        }
        
        // 优先选择高质量音频流
        com.alibaba.fastjson.JSONObject bestAudio = null;
        int bestBandwidth = 0;
        
        for (int i = 0; i < audios.size(); i++) {
            com.alibaba.fastjson.JSONObject audio = audios.getJSONObject(i);
            Integer bandwidth = audio.getInteger("bandwidth");
            
            if (bandwidth != null && bandwidth > bestBandwidth) {
                bestBandwidth = bandwidth;
                bestAudio = audio;
            }
        }
        
        // 如果没有找到带宽信息，使用第一个
        if (bestAudio == null) {
            bestAudio = audios.getJSONObject(0);
        }
        
        log.info("选择音频流: id={}, bandwidth={}", 
                bestAudio.getString("id"), bestAudio.getInteger("bandwidth"));
        
        return bestAudio;
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