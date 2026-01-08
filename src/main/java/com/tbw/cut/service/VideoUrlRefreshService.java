package com.tbw.cut.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视频URL刷新服务
 * 处理Bilibili视频URL过期问题
 */
@Slf4j
@Service
public class VideoUrlRefreshService {
    
    @Autowired
    private BilibiliApiClient bilibiliApiClient;
    
    // URL过期时间检测正则表达式
    private static final Pattern DEADLINE_PATTERN = Pattern.compile("[&?]deadline=(\\d+)");
    
    // 视频ID提取正则表达式
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("/upgcxcode/\\d+/\\d+/(\\d+)/");
    
    /**
     * 检查URL是否已过期
     * @param videoUrl 视频URL
     * @return true如果已过期，false如果仍有效
     */
    public boolean isUrlExpired(String videoUrl) {
        try {
            Matcher matcher = DEADLINE_PATTERN.matcher(videoUrl);
            if (matcher.find()) {
                long deadline = Long.parseLong(matcher.group(1));
                long currentTime = System.currentTimeMillis() / 1000; // 转换为秒
                
                // 提前5分钟判断为过期，避免下载过程中过期
                long bufferTime = 5 * 60; // 5分钟缓冲
                boolean expired = currentTime >= (deadline - bufferTime);
                
                log.debug("URL expiration check: deadline={}, current={}, expired={}", 
                         deadline, currentTime, expired);
                return expired;
            }
        } catch (Exception e) {
            log.warn("Failed to check URL expiration: {}", e.getMessage());
        }
        return false; // 无法判断时假设未过期
    }
    
    /**
     * 从URL中提取视频信息
     * @param videoUrl 视频URL
     * @return 包含bvid, cid等信息的Map
     */
    public Map<String, String> extractVideoInfo(String videoUrl) {
        Map<String, String> info = new HashMap<>();
        
        try {
            // 从URL中提取CID
            Matcher cidMatcher = VIDEO_ID_PATTERN.matcher(videoUrl);
            if (cidMatcher.find()) {
                info.put("cid", cidMatcher.group(1));
                log.debug("Extracted CID from URL: {}", cidMatcher.group(1));
            }
            
            // 从URL参数中提取其他信息
            String[] urlParts = videoUrl.split("\\?");
            if (urlParts.length > 1) {
                String queryString = urlParts[1];
                String[] params = queryString.split("&");
                
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name());
                        
                        // 保存有用的参数
                        switch (key) {
                            case "mid":
                                info.put("mid", value);
                                break;
                            case "platform":
                                info.put("platform", value);
                                break;
                            case "qn":
                                info.put("qn", value);
                                break;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to extract video info from URL: {}", e.getMessage());
        }
        
        return info;
    }
    
    /**
     * 刷新视频URL
     * @param bvid 视频BVID
     * @param cid 视频CID
     * @param qn 清晰度代码
     * @return 新的视频URL，如果刷新失败返回null
     */
    public String refreshVideoUrl(String bvid, String cid, String qn) {
        try {
            log.info("Refreshing video URL: bvid={}, cid={}, qn={}", bvid, cid, qn);
            
            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("bvid", bvid);
            params.put("cid", cid);
            params.put("fnval", "16"); // DASH格式
            params.put("fnver", "0");
            params.put("fourk", "1");
            
            if (qn != null && !qn.isEmpty()) {
                params.put("qn", qn);
            }
            
            // 调用Bilibili API获取新的播放URL
            String apiUrl = "https://api.bilibili.com/x/player/wbi/playurl";
            String response = bilibiliApiClient.getWithWbiSign(apiUrl, params);
            
            // 解析响应
            JSONObject responseJson = JSONObject.parseObject(response);
            if (responseJson.getInteger("code") == 0) {
                JSONObject data = responseJson.getJSONObject("data");
                
                // 优先从DASH格式中获取URL
                if (data.containsKey("dash")) {
                    JSONObject dash = data.getJSONObject("dash");
                    if (dash.containsKey("video")) {
                        JSONArray videoStreams = dash.getJSONArray("video");
                        if (videoStreams.size() > 0) {
                            JSONObject firstStream = videoStreams.getJSONObject(0);
                            String newUrl = firstStream.getString("baseUrl");
                            if (newUrl == null || newUrl.isEmpty()) {
                                newUrl = firstStream.getString("base_url");
                            }
                            
                            if (newUrl != null && !newUrl.isEmpty()) {
                                log.info("Successfully refreshed video URL: {}", 
                                        newUrl.substring(0, Math.min(100, newUrl.length())) + "...");
                                return newUrl;
                            }
                        }
                    }
                }
                
                // 如果DASH格式不可用，尝试从durl中获取
                if (data.containsKey("durl")) {
                    JSONArray durls = data.getJSONArray("durl");
                    if (durls.size() > 0) {
                        JSONObject firstDurl = durls.getJSONObject(0);
                        String newUrl = firstDurl.getString("url");
                        
                        if (newUrl != null && !newUrl.isEmpty()) {
                            log.info("Successfully refreshed video URL from durl: {}", 
                                    newUrl.substring(0, Math.min(100, newUrl.length())) + "...");
                            return newUrl;
                        }
                    }
                }
                
                log.warn("No valid video URL found in API response");
            } else {
                log.error("Failed to refresh video URL, API error: code={}, message={}", 
                         responseJson.getInteger("code"), responseJson.getString("message"));
            }
            
        } catch (Exception e) {
            log.error("Exception occurred while refreshing video URL", e);
        }
        
        return null;
    }
    
    /**
     * 智能刷新视频URL
     * 尝试从原URL中提取信息并刷新
     * @param originalUrl 原始视频URL
     * @param bvid 视频BVID（如果已知）
     * @return 新的视频URL，如果刷新失败返回null
     */
    public String smartRefreshUrl(String originalUrl, String bvid) {
        try {
            // 检查URL是否真的过期
            if (!isUrlExpired(originalUrl)) {
                log.debug("URL is not expired, no need to refresh");
                return originalUrl;
            }
            
            // 从URL中提取视频信息
            Map<String, String> videoInfo = extractVideoInfo(originalUrl);
            String cid = videoInfo.get("cid");
            String qn = videoInfo.get("qn");
            
            if (bvid == null || bvid.isEmpty()) {
                log.warn("BVID is required for URL refresh but not provided");
                return null;
            }
            
            if (cid == null || cid.isEmpty()) {
                log.warn("Could not extract CID from URL: {}", originalUrl);
                return null;
            }
            
            // 刷新URL
            return refreshVideoUrl(bvid, cid, qn);
            
        } catch (Exception e) {
            log.error("Failed to smart refresh URL", e);
            return null;
        }
    }
    
    /**
     * 获取URL剩余有效时间（秒）
     * @param videoUrl 视频URL
     * @return 剩余有效时间，-1表示无法确定
     */
    public long getUrlRemainingTime(String videoUrl) {
        try {
            Matcher matcher = DEADLINE_PATTERN.matcher(videoUrl);
            if (matcher.find()) {
                long deadline = Long.parseLong(matcher.group(1));
                long currentTime = System.currentTimeMillis() / 1000;
                return Math.max(0, deadline - currentTime);
            }
        } catch (Exception e) {
            log.debug("Failed to get URL remaining time: {}", e.getMessage());
        }
        return -1;
    }
}