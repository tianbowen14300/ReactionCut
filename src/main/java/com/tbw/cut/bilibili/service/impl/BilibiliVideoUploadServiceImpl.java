package com.tbw.cut.bilibili.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import com.tbw.cut.bilibili.service.BilibiliVideoUploadService;
import com.tbw.cut.bilibili.service.RateLimitHandler;
import com.tbw.cut.bilibili.service.UploadProgressManager;
import com.tbw.cut.entity.UploadProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;

/**
 * Bilibili视频上传服务实现
 */
@Slf4j
@Service
public class BilibiliVideoUploadServiceImpl implements BilibiliVideoUploadService {
    
    @Autowired
    private BilibiliApiClient apiClient;
    
    @Autowired
    private RateLimitHandler rateLimitHandler;
    
    @Autowired
    private UploadProgressManager uploadProgressManager;
    
    @Value("${bilibili.member.base-url:https://member.bilibili.com}")
    private String memberBaseUrl;
    
    @Override
    public JSONObject preUploadVideo(String fileName, long fileSize) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // 添加空值检查
                if (fileName == null || fileName.isEmpty()) {
                    throw new IllegalArgumentException("fileName cannot be null or empty");
                }
                
                String url = memberBaseUrl + "/preupload";
                StringJoiner params = new StringJoiner("&");
                params.add("name=" + URLEncoder.encode(fileName, "UTF-8"));
                params.add("r=upos");
                params.add("profile=ugcfx/bup");
                params.add("version=2.14.0.0");
                params.add("size=" + fileSize);
                
                String fullUrl = url + "?" + params.toString();
                log.info("预上传视频: {}", fullUrl);
                
                // 这里需要直接调用API，因为需要特殊的认证方式
                String response = callPreUploadApi(fullUrl);
                JSONObject result = JSONObject.parseObject(response);
                
                // 成功时重置406错误计数
                rateLimitHandler.reset406Count();
                return result;
            } catch (Exception e) {
                retryCount++;
                log.error("预上传视频失败，第{}次尝试: {}", retryCount, e.getMessage(), e);
                
                // 使用统一的406错误处理
                long waitTime = rateLimitHandler.handle406Error(e.getMessage());
                if (waitTime > 0) {
                    log.warn("遇到406错误，需要等待{}，当前连续406错误次数: {}", 
                        rateLimitHandler.getWaitTimeDescription(waitTime), 
                        rateLimitHandler.getCurrent406Count());
                    try {
                        rateLimitHandler.smartWait(waitTime);
                        // 406错误不计入重试次数
                        retryCount--;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else if (retryCount < maxRetries) {
                    // 对于其他错误，如果还有重试机会，等待一段时间再重试
                    try {
                        // 等待2^retryCount秒再重试（指数退避）
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，抛出异常
                    throw new RuntimeException("预上传视频失败，已达到最大重试次数: " + e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("预上传视频失败，已达到最大重试次数");
    }
    
    @Override
    public JSONObject postVideoMeta(JSONObject preUploadData, long fileSize) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // 添加空值检查
                if (preUploadData == null) {
                    throw new IllegalArgumentException("preUploadData cannot be null");
                }
                
                String schemeAndHost = "https:" + preUploadData.getString("endpoint");
                String path = preUploadData.getString("upos_uri").replaceFirst("upos:/", "");
                String url = schemeAndHost + path;
                
                StringJoiner params = new StringJoiner("&");
                params.add("uploads="); // 留空是必须的
                params.add("output=json");
                params.add("profile=ugcfx/bup");
                params.add("filesize=" + fileSize);
                params.add("partsize=" + preUploadData.getLong("chunk_size"));
                params.add("biz_id=" + preUploadData.getLong("biz_id"));
                
                String fullUrl = url + "?" + params.toString();
                log.info("上传视频元数据: {}", fullUrl);
                
                String auth = preUploadData.getString("auth");
                String response = callPostVideoMetaApi(fullUrl, auth);
                return JSONObject.parseObject(response);
            } catch (Exception e) {
                retryCount++;
                log.error("上传视频元数据失败，第{}次尝试: {}", retryCount, e.getMessage(), e);
                
                if (retryCount < maxRetries) {
                    // 等待2^retryCount秒再重试（指数退避）
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，抛出异常
                    throw new RuntimeException("上传视频元数据失败，已达到最大重试次数: " + e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("上传视频元数据失败，已达到最大重试次数");
    }
    
    @Override
    public int uploadVideo(JSONObject preUploadData, JSONObject postVideoMeta, File videoFile) {
        // 添加空值检查
        if (preUploadData == null) {
            throw new IllegalArgumentException("preUploadData cannot be null");
        }
        if (postVideoMeta == null) {
            throw new IllegalArgumentException("postVideoMeta cannot be null");
        }
        if (videoFile == null) {
            throw new IllegalArgumentException("videoFile cannot be null");
        }
        
        long length = videoFile.length();
        int chunkSize = preUploadData.getIntValue("chunk_size");
        int totalChunks = (int) Math.ceil((double) length / chunkSize);
        
        // 开始或恢复上传进度
        UploadProgress progress = uploadProgressManager.startUpload(videoFile, totalChunks);
        uploadProgressManager.setUploadMetadata(progress, preUploadData, postVideoMeta);
        
        // 如果已经完成，直接返回
        if (progress.isCompleted()) {
            log.info("文件已完全上传，跳过上传步骤");
            // 清理已完成的上传进度
            uploadProgressManager.completeUpload(progress);
            return totalChunks;
        }
        
        int result = uploadVideoWithResume(progress, videoFile, preUploadData, postVideoMeta);
        
        // 上传完成后清理进度
        if (result == totalChunks) {
            uploadProgressManager.completeUpload(progress);
            log.info("上传完成，已清理进度信息");
        }
        
        return result;
    }
    
    /**
     * 支持断点续传的视频上传
     */
    private int uploadVideoWithResume(UploadProgress progress, File videoFile, 
                                    JSONObject preUploadData, JSONObject postVideoMeta) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                long startTs = System.currentTimeMillis() - 1;
                String schemeAndHost = "https:" + preUploadData.getString("endpoint");
                String path = preUploadData.getString("upos_uri").replaceFirst("upos:/", "");
                String urlBase = schemeAndHost + path;
                String auth = preUploadData.getString("auth"); // 提取auth参数
                
                long length = videoFile.length();
                int chunkSize = preUploadData.getIntValue("chunk_size");
                int totalChunks = progress.getTotalChunks();
                byte[] buffer = new byte[chunkSize];
                
                try (InputStream in = new FileInputStream(videoFile)) {
                    String uploadId = postVideoMeta.getString("upload_id");
                    
                    // 从下一个未完成的分片开始上传
                    int startChunk = progress.getNextChunkIndex();
                    if (startChunk == -1) {
                        log.info("所有分片已完成上传");
                        return totalChunks;
                    }
                    
                    log.info("从分片 {} 开始上传，已完成 {}/{} ({}%)", 
                        startChunk, progress.getCompletedChunks(), totalChunks,
                        String.format("%.1f", progress.getCompletionPercentage()));
                    
                    // 跳过已完成的分片
                    long skipBytes = (long) startChunk * chunkSize;
                    if (skipBytes > 0) {
                        long skipped = in.skip(skipBytes);
                        if (skipped != skipBytes) {
                            throw new IOException("无法跳过到指定位置: " + skipBytes);
                        }
                    }
                    
                    for (int chunk = startChunk; chunk < totalChunks; chunk++) {
                        // 检查分片是否已完成
                        if (progress.isChunkCompleted(chunk)) {
                            // 跳过已完成的分片
                            in.skip(chunkSize);
                            continue;
                        }
                        
                        log.info("上传分片 {}/{} (总进度: {}%)", chunk + 1, totalChunks,
                                String.format("%.1f", ((double)(chunk + 1) / totalChunks * 100)));
                        
                        int size = in.read(buffer, 0, chunkSize);
                        if (size == -1) {
                            break;
                        }
                        
                        // 上传单个分片，支持重试
                        boolean chunkSuccess = uploadSingleChunk(urlBase, uploadId, chunk, totalChunks, 
                                                               chunkSize, length, buffer, size, auth);
                        
                        if (chunkSuccess) {
                            // 标记分片完成
                            uploadProgressManager.markChunkCompleted(progress, chunk);
                            // 成功上传后重置406错误计数
                            rateLimitHandler.reset406Count();
                        } else {
                            throw new RuntimeException("分片 " + chunk + " 上传失败");
                        }
                    }
                }
                
                log.info("视频文件上传完成，总分片数: {}", totalChunks);
                return totalChunks;
                
            } catch (Exception e) {
                retryCount++;
                log.error("上传视频文件失败，第{}次尝试: {}", retryCount, e.getMessage(), e);
                
                // 使用统一的406错误处理
                long waitTime = rateLimitHandler.handle406Error(e.getMessage());
                if (waitTime > 0) {
                    log.warn("遇到406错误，需要等待{}，当前连续406错误次数: {}，断点续传将保留已上传进度", 
                        rateLimitHandler.getWaitTimeDescription(waitTime), 
                        rateLimitHandler.getCurrent406Count());
                    try {
                        rateLimitHandler.smartWait(waitTime);
                        // 406错误不计入重试次数，因为我们有断点续传
                        retryCount--;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else if (retryCount < maxRetries) {
                    // 等待2^retryCount秒再重试（指数退避）
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，取消上传
                    uploadProgressManager.cancelUpload(progress);
                    throw new RuntimeException("上传视频文件失败，已达到最大重试次数: " + e.getMessage(), e);
                }
            }
        }
        
        uploadProgressManager.cancelUpload(progress);
        throw new RuntimeException("上传视频文件失败，已达到最大重试次数");
    }
    
    /**
     * 上传单个分片，支持重试
     */
    private boolean uploadSingleChunk(String urlBase, String uploadId, int chunk, int totalChunks,
                                    int chunkSize, long totalLength, byte[] buffer, int size, String auth) {
        int maxChunkRetries = 3;
        
        for (int retry = 0; retry < maxChunkRetries; retry++) {
            try {
                StringJoiner params = new StringJoiner("&");
                params.add("partNumber=" + (chunk + 1));
                params.add("uploadId=" + uploadId);
                params.add("chunk=" + chunk);
                params.add("chunks=" + totalChunks);
                params.add("size=" + size);
                params.add("start=" + (chunk * chunkSize));
                params.add("end=" + (chunk * chunkSize + size));
                params.add("total=" + totalLength);
                
                String fullUrl = urlBase + "?" + params.toString();
                
                String response = callUploadChunkApi(fullUrl, auth, buffer, size);
                if ("MULTIPART_PUT_SUCCESS".equals(response.trim())) {
                    return true;
                }
                
                log.warn("分片{}上传失败，响应: {}，第{}次重试", chunk, response, retry + 1);
                
            } catch (Exception e) {
                log.warn("分片{}上传异常，第{}次重试: {}", chunk, retry + 1, e.getMessage());
                
                // 检查是否是406错误
                long waitTime = rateLimitHandler.handle406Error(e.getMessage());
                if (waitTime > 0) {
                    try {
                        rateLimitHandler.smartWait(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            
            // 分片重试间隔
            if (retry < maxChunkRetries - 1) {
                try {
                    Thread.sleep(1000 * (retry + 1)); // 1s, 2s, 3s
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public JSONObject endUpload(JSONObject preUploadData, JSONObject postVideoMeta, int chunks) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // 添加空值检查
                if (preUploadData == null) {
                    throw new IllegalArgumentException("preUploadData cannot be null");
                }
                if (postVideoMeta == null) {
                    throw new IllegalArgumentException("postVideoMeta cannot be null");
                }
                
                String schemeAndHost = "https:" + preUploadData.getString("endpoint");
                String path = preUploadData.getString("upos_uri").replaceFirst("upos:/", "");
                String url = schemeAndHost + path;
                
                StringJoiner params = new StringJoiner("&");
                params.add("output=json");
                
                // 添加空值检查
                String filename = preUploadData.getString("filename");
                if (filename != null) {
                    params.add("name=" + URLEncoder.encode(filename, "UTF-8"));
                }
                
                params.add("profile=ugcfx/bup");
                
                // 添加空值检查
                String uploadId = postVideoMeta.getString("upload_id");
                if (uploadId != null) {
                    params.add("uploadId=" + uploadId);
                }
                
                params.add("biz_id=" + preUploadData.getLong("biz_id"));
                
                String fullUrl = url + "?" + params.toString();
                String auth = preUploadData.getString("auth");
                
                // 构建parts数组
                JSONObject body = new JSONObject();
                com.alibaba.fastjson.JSONArray parts = new com.alibaba.fastjson.JSONArray();
                for (int i = 1; i <= chunks; i++) {
                    JSONObject part = new JSONObject();
                    part.put("partNumber", i);
                    part.put("eTag", "etag");
                    parts.add(part);
                }
                body.put("parts", parts);
                
                String response = callEndUploadApi(fullUrl, auth, body.toJSONString());
                JSONObject result = JSONObject.parseObject(response);
                
                // 成功完成上传后重置406错误计数
                rateLimitHandler.reset406Count();
                return result;
            } catch (Exception e) {
                retryCount++;
                log.error("结束上传失败，第{}次尝试: {}", retryCount, e.getMessage(), e);
                
                // 使用统一的406错误处理
                long waitTime = rateLimitHandler.handle406Error(e.getMessage());
                if (waitTime > 0) {
                    log.warn("遇到406错误，需要等待{}，当前连续406错误次数: {}", 
                        rateLimitHandler.getWaitTimeDescription(waitTime), 
                        rateLimitHandler.getCurrent406Count());
                    try {
                        rateLimitHandler.smartWait(waitTime);
                        // 406错误不计入重试次数
                        retryCount--;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else if (retryCount < maxRetries) {
                    // 等待2^retryCount秒再重试（指数退避）
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，抛出异常
                    throw new RuntimeException("结束上传失败，已达到最大重试次数: " + e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("结束上传失败，已达到最大重试次数");
    }
    
    @Override
    public String uploadCover(MultipartFile coverImage) {
        try {
            String url = memberBaseUrl + "/x/vu/web/cover/up";
            
            // 将图片文件转换为base64编码
            byte[] bytes = coverImage.getBytes();
            String base64Image = java.util.Base64.getEncoder().encodeToString(bytes);
            String imageData = "data:image/jpeg;base64," + base64Image;
            
            // 构建请求参数
            JSONObject params = new JSONObject();
            params.put("cover", imageData);
            params.put("csrf", apiClient.extractCsrfTokenFromLoginInfo());
            
            // 发送请求
            String response = callUploadCoverApi(url, params.toJSONString());
            JSONObject result = JSONObject.parseObject(response);
            
            if (result.getIntValue("code") == 0) {
                return result.getJSONObject("data").getString("url");
            } else {
                log.error("上传封面失败: {}", result.toJSONString());
                throw new RuntimeException("上传封面失败: " + result.getString("message"));
            }
        } catch (Exception e) {
            log.error("上传封面失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传封面失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject submitVideo(JSONObject submitData) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                String url = memberBaseUrl + "/x/vu/web/add/v3";
                String csrf = apiClient.extractCsrfTokenFromLoginInfo();
                
                String fullUrl = url + "?csrf=" + csrf;
                log.info("提交视频投稿: {}", fullUrl);
                
                // 发送投稿请求
                String response = callSubmitVideoApi(fullUrl, submitData.toJSONString());
                return JSONObject.parseObject(response);
            } catch (Exception e) {
                retryCount++;
                log.error("提交视频投稿失败，第{}次尝试: {}", retryCount, e.getMessage(), e);
                
                if (retryCount < maxRetries) {
                    // 等待2^retryCount秒再重试（指数退避）
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，抛出异常
                    throw new RuntimeException("提交视频投稿失败，已达到最大重试次数: " + e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("提交视频投稿失败，已达到最大重试次数");
    }
    
    @Override
    public JSONObject associateWithSeason(Long seasonId, Long sectionId, String title, Long aid) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                String url = memberBaseUrl + "/x2/creative/web/season/switch";
                String csrf = apiClient.extractCsrfTokenFromLoginInfo();
                
                String fullUrl = url + "?t=" + System.currentTimeMillis() + "&csrf=" + csrf;
                log.info("关联视频到合集: {}", fullUrl);
                
                // 构建请求体
                JSONObject requestData = new JSONObject();
                requestData.put("season_id", seasonId);
                requestData.put("section_id", sectionId);
                requestData.put("title", title);
                requestData.put("aid", aid);
                requestData.put("csrf", csrf);
                
                // 发送请求
                String response = callAssociateWithSeasonApi(fullUrl, requestData.toJSONString());
                JSONObject result = JSONObject.parseObject(response);
                
                // 检查响应结果，如果season_id不存在则记录详细错误信息
                if (result.getIntValue("code") == -404) {
                    log.error("关联合集失败，season_id不存在或已被删除，season_id: {}, aid: {}", seasonId, aid);
                    // 对于-404错误，不需要重试，直接抛出异常
                    throw new RuntimeException("关联合集失败，指定的合集不存在或已被删除");
                }
                
                return result;
            } catch (RuntimeException re) {
                // 对于RuntimeException（如-404错误），直接抛出，不进行重试
                if (re.getMessage().contains("关联合集失败，指定的合集不存在或已被删除")) {
                    throw re;
                }
                // 其他RuntimeException继续重试逻辑
                retryCount++;
                log.error("关联视频到合集失败，第{}次尝试: {}", retryCount, re.getMessage(), re);
                
                if (retryCount < maxRetries) {
                    // 等待2^retryCount秒再重试（指数退避）
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，抛出异常
                    throw new RuntimeException("关联视频到合集失败，已达到最大重试次数: " + re.getMessage(), re);
                }
            } catch (Exception e) {
                retryCount++;
                log.error("关联视频到合集失败，第{}次尝试: {}", retryCount, e.getMessage(), e);
                
                if (retryCount < maxRetries) {
                    // 等待2^retryCount秒再重试（指数退避）
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，抛出异常
                    throw new RuntimeException("关联视频到合集失败，已达到最大重试次数: " + e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("关联视频到合集失败，已达到最大重试次数");
    }
    
    // 以下为辅助方法，用于直接调用HTTP API
    
    private String callPreUploadApi(String url) throws IOException {
        HttpURLConnection conn = createConnection(url, "GET");
        return readResponse(conn);
    }
    
    private String callPostVideoMetaApi(String url, String auth) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("X-Upos-Auth", auth);
        return readResponse(conn);
    }
    
    private String callUploadChunkApi(String url, String auth, byte[] data, int size) throws IOException {
        HttpURLConnection conn = createConnection(url, "PUT");
        conn.setRequestProperty("X-Upos-Auth", auth);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setDoOutput(true);
        
        try (OutputStream out = conn.getOutputStream()) {
            out.write(data, 0, size);
        }
        
        return readResponse(conn);
    }
    
    private String callEndUploadApi(String url, String auth, String body) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("X-Upos-Auth", auth);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        return readResponse(conn);
    }
    
    private HttpURLConnection createConnection(String url, String method) throws IOException {
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) new URI(url).toURL().openConnection();
        } catch (Exception e) {
            throw new IOException(e);
        }
        conn.setRequestMethod(method);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        // 添加认证信息
        String cookie = apiClient.extractCookieFromLoginInfo();
        if (cookie != null && !cookie.isEmpty()) {
            conn.setRequestProperty("Cookie", cookie);
        }
        
        return conn;
    }
    
    private String readResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            // 如果响应码不是200，读取错误流
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                // 抛出包含响应码和错误信息的异常
                throw new IOException("Server returned HTTP response code: " + responseCode + " for URL: " + conn.getURL() + " with message: " + response.toString());
            }
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    private String callUploadCoverApi(String url, String body) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        return readResponse(conn);
    }
    
    private String callSubmitVideoApi(String url, String body) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        return readResponse(conn);
    }
    
    private String callAssociateWithSeasonApi(String url, String body) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        return readResponse(conn);
    }
    
    @Override
    public JSONObject addEpisodesToSection(Long sectionId, List<JSONObject> episodes) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                String url = memberBaseUrl + "/x2/creative/web/season/section/episodes/add";
                String csrf = apiClient.extractCsrfTokenFromLoginInfo();
                
                String fullUrl = url + "?t=" + System.currentTimeMillis() + "&csrf=" + csrf;
                log.info("将视频添加到合集章节: {}", fullUrl);
                
                // 构建请求体
                JSONObject requestData = new JSONObject();
                requestData.put("sectionId", sectionId);
                requestData.put("episodes", episodes);
                requestData.put("csrf", csrf);
                
                // 发送请求
                String response = callAddEpisodesToSectionApi(fullUrl, requestData.toJSONString());
                return JSONObject.parseObject(response);
            } catch (Exception e) {
                retryCount++;
                log.error("将视频添加到合集章节失败，第{}次尝试: {}", retryCount, e.getMessage(), e);
                
                if (retryCount < maxRetries) {
                    // 等待2^retryCount秒再重试（指数退避）
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，抛出异常
                    throw new RuntimeException("将视频添加到合集章节失败，已达到最大重试次数: " + e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("将视频添加到合集章节失败，已达到最大重试次数");
    }
    
    private String callAddEpisodesToSectionApi(String url, String body) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        return readResponse(conn);
    }
    
    @Override
    public JSONObject getSeasonInfo(Long seasonId) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // 使用新的合集列表接口
                String url = memberBaseUrl + "/x2/creative/web/seasons";
                String csrf = apiClient.extractCsrfTokenFromLoginInfo();
                
                String fullUrl = url + "?pn=1&ps=100&order=desc&sort=mtime&filter=1&t=" + System.currentTimeMillis();
                log.info("获取合集列表: {}", fullUrl);
                
                // 发送GET请求获取合集列表
                String response = callGetSeasonInfoApi(fullUrl);
                JSONObject result = JSONObject.parseObject(response);
                
                if (result.getIntValue("code") == 0) {
                    // 从合集列表中查找指定的合集
                    JSONObject data = result.getJSONObject("data");
                    if (data != null && data.containsKey("seasons")) {
                        com.alibaba.fastjson.JSONArray seasons = data.getJSONArray("seasons");
                        if (seasons != null) {
                            for (int i = 0; i < seasons.size(); i++) {
                                JSONObject season = seasons.getJSONObject(i);
                                if (season.getLong("id").equals(seasonId)) {
                                    // 找到了指定的合集，构造返回结果
                                    JSONObject seasonInfo = new JSONObject();
                                    seasonInfo.put("code", 0);
                                    JSONObject seasonData = new JSONObject();
                                    seasonData.put("id", season.getLong("id"));
                                    seasonData.put("title", season.getString("title"));
                                    
                                    // 构造默认章节信息
                                    com.alibaba.fastjson.JSONArray sections = new com.alibaba.fastjson.JSONArray();
                                    JSONObject defaultSection = new JSONObject();
                                    defaultSection.put("id", 0L); // 默认章节ID
                                    defaultSection.put("title", "默认章节");
                                    sections.add(defaultSection);
                                    seasonData.put("sections", sections);
                                    
                                    seasonInfo.put("data", seasonData);
                                    return seasonInfo;
                                }
                            }
                        }
                    }
                    
                    // 如果没有找到指定的合集，返回404错误
                    JSONObject notFoundResult = new JSONObject();
                    notFoundResult.put("code", -404);
                    notFoundResult.put("message", "合集不存在或已被删除");
                    return notFoundResult;
                } else {
                    return result;
                }
            } catch (Exception e) {
                retryCount++;
                log.error("获取合集信息失败，第{}次尝试: {}", retryCount, e.getMessage(), e);
                
                if (retryCount < maxRetries) {
                    // 等待2^retryCount秒再重试（指数退避）
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，抛出异常
                    throw new RuntimeException("获取合集信息失败，已达到最大重试次数: " + e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("获取合集信息失败，已达到最大重试次数");
    }
    
    private String callGetSeasonInfoApi(String url) throws IOException {
        HttpURLConnection conn = createConnection(url, "GET");
        return readResponse(conn);
    }
    
    @Override
    public Long getAidFromBvid(String bvid) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // 使用B站API获取视频信息
                String url = "https://api.bilibili.com/x/web-interface/view";
                String fullUrl = url + "?bvid=" + bvid;
                log.info("获取视频信息: {}", fullUrl);
                
                // 发送GET请求
                String response = callGetVideoInfoApi(fullUrl);
                JSONObject result = JSONObject.parseObject(response);
                
                if (result.getIntValue("code") == 0) {
                    JSONObject data = result.getJSONObject("data");
                    if (data != null && data.containsKey("aid")) {
                        Long aid = data.getLong("aid");
                        log.info("成功获取AID: {}, BVID: {}", aid, bvid);
                        return aid;
                    }
                }
                
                log.error("获取AID失败，响应: {}", result.toJSONString());
                return null;
            } catch (Exception e) {
                retryCount++;
                log.error("获取AID失败，第{}次尝试: {}", retryCount, e.getMessage(), e);
                
                if (retryCount < maxRetries) {
                    // 等待2^retryCount秒再重试（指数退避）
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，返回null
                    log.error("获取AID失败，已达到最大重试次数: {}", e.getMessage(), e);
                    return null;
                }
            }
        }
        
        return null;
    }
    
    private String callGetVideoInfoApi(String url) throws IOException {
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) new URI(url).toURL().openConnection();
        } catch (Exception e) {
            throw new IOException(e);
        }
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        return readResponse(conn);
    }
    
    @Override
    public JSONObject editVideo(Long aid, JSONObject editData) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                String url = memberBaseUrl + "/x/vu/web/edit";
                String csrf = apiClient.extractCsrfTokenFromLoginInfo();
                
                String fullUrl = url + "?t=" + System.currentTimeMillis() + "&csrf=" + csrf;
                log.info("编辑视频: {}, AID: {}", fullUrl, aid);
                
                // 添加AID到编辑数据中
                editData.put("aid", aid);
                editData.put("csrf", csrf);
                
                // 发送请求
                String response = callEditVideoApi(fullUrl, editData.toJSONString());
                JSONObject result = JSONObject.parseObject(response);
                
                // 成功时重置406错误计数
                if (result.getIntValue("code") == 0) {
                    rateLimitHandler.reset406Count();
                }
                
                return result;
            } catch (Exception e) {
                retryCount++;
                log.error("编辑视频失败，第{}次尝试: {}", retryCount, e.getMessage(), e);
                
                // 使用统一的406错误处理
                long waitTime = rateLimitHandler.handle406Error(e.getMessage());
                if (waitTime > 0) {
                    log.warn("遇到406错误，需要等待{}，当前连续406错误次数: {}", 
                        rateLimitHandler.getWaitTimeDescription(waitTime), 
                        rateLimitHandler.getCurrent406Count());
                    try {
                        rateLimitHandler.smartWait(waitTime);
                        // 406错误不计入重试次数
                        retryCount--;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else if (retryCount < maxRetries) {
                    // 等待2^retryCount秒再重试（指数退避）
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，抛出异常
                    throw new RuntimeException("编辑视频失败，已达到最大重试次数: " + e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("编辑视频失败，已达到最大重试次数");
    }
    
    private String callEditVideoApi(String url, String body) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        return readResponse(conn);
    }
    
    @Override
    public JSONObject getVideoInfo(Long aid) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // 使用B站API获取视频详细信息
                String url = "https://api.bilibili.com/x/web-interface/view";
                String fullUrl = url + "?aid=" + aid;
                log.info("获取视频详细信息: {}", fullUrl);
                
                // 发送GET请求
                String response = callGetVideoInfoApi(fullUrl);
                JSONObject result = JSONObject.parseObject(response);
                
                if (result.getIntValue("code") == 0) {
                    log.info("成功获取视频信息，AID: {}", aid);
                    return result;
                }
                
                log.error("获取视频信息失败，响应: {}", result.toJSONString());
                return result;
            } catch (Exception e) {
                retryCount++;
                log.error("获取视频信息失败，第{}次尝试: {}", retryCount, e.getMessage(), e);
                
                if (retryCount < maxRetries) {
                    // 等待2^retryCount秒再重试（指数退避）
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("线程中断", ie);
                    }
                } else {
                    // 达到最大重试次数，返回错误结果
                    log.error("获取视频信息失败，已达到最大重试次数: {}", e.getMessage(), e);
                    JSONObject errorResult = new JSONObject();
                    errorResult.put("code", -1);
                    errorResult.put("message", "获取视频信息失败: " + e.getMessage());
                    return errorResult;
                }
            }
        }
        
        JSONObject errorResult = new JSONObject();
        errorResult.put("code", -1);
        errorResult.put("message", "获取视频信息失败，已达到最大重试次数");
        return errorResult;
    }
}